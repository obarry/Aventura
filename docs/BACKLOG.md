# Aventura backlog

Items identified during the September 2026 clean-up phase (Rasterizer removal, contexts audit,
configurable shadow maps, GUI-independent view) and the October 2026 spot and point light shadows,
and deliberately **not** handled yet.
See also the roadmap in [DESIGN.md](DESIGN.md#11-history-limitations-and-roadmap).

## 1. Perspective / Viewport split

`PerspectiveContext` mixes the lens (the `Perspective`: view volume and projection, in world units)
and the raster target (pixel size), linked by a pixels-per-unit ratio.

- Pixel dimensions do not follow later `Perspective` changes; `ppu` only reflects the ratio of the
  construction (or of the last `resize()`).
  **Done (October 2026): resizing.** The application resizes the view (`GUIView.resize(w, h)`); at the next
  `render()`, the `RenderEngine` follows it: `PerspectiveContext.resize()` changes the pixel dimensions and
  adapts the `Perspective` according to the context's `ResizeMode` (`setResizeMode()`): `KEEP_HEIGHT`
  (default, vertical field of view kept, the usual 3D convention) or `KEEP_SCALE` (`ppu` kept, a larger image
  shows more of the scene); the main ZBuffer is reallocated. The view is the reference for the pixel size,
  which the future `Viewport` should hold.
  `FractalLandscape` uses it to make the image fill its window (`KEEP_SCALE`).
- Overload trap: `(int, int, float, float, type, int)` is pixel-based while
  `(float, float, float, float, type, int)` is unit-based, so `(10, 10, ...)` means 10 x 10 **pixels**.
- The six-bounds constructor takes `(top, bottom, right, left, far, near)`, unlike `Perspective`
  and the `Projection` classes, which use `(left, right, bottom, top, near, far)`.
- ZBuffer clear value: always `far`, whereas the stored depth is `w` (eye distance) for a frustum and
  NDC z in [0, 1] for an orthographic projection. It should come from the perspective type.

Target: a `Perspective` (lens) and a `Viewport` (pixels), `PerspectiveContext` kept as a thin
compatibility façade.

## 2. Generalized maps (for BumpMap and others)

`MapView` (a 2D grid of floats) is used by `ZBuffer` and shadow maps, and it lives in the `view`
package only to be displayable. Bump maps (and later height, normal or light maps) will need the
same data structure.

Proposal: extract a general map class (for example `FloatMap`, with bilinear sampling, min/max, and
normalization) into a non-view package. Keep maps displayable through **composition** rather than
inheritance: `GUIView.initView(map)` or a small map-to-image adapter can show any map, so a view
does not need to *be* a map. The bilinear filtering duplicated between `MapView` and `Texture` can
be factored at the same time.

**Done (October 2026): the storage.** `MapView` now stores its values in a single `float[]`, row by
row (index `y * width + x`), instead of a `float[x][y]` array: the rasterizer walks the pixels row by
row, so they are now consecutive in memory (performance audit D7: about −10 % per frame, −15 to
−20 % with point or spot light shadows). The accessors keep the `(x, y)` convention; `getMap()` now
returns a copy. The extraction of `FloatMap` out of the `view` package remains to be done.

## 3. Shadows

- **Done (October 2026): spot and point light shadows.** A `SpotLight` has one perspective (frustum)
  shadow map built from its position, direction and outer angle (1000 pixels by default); a `PointLight`
  has a cube map of six frustum faces (512 pixels per face by default) with exact 90° faces, a face
  chosen from the largest component of the light-to-point vector, and an exact skip of the faces no
  object can reach. See [DESIGN.md §7](DESIGN.md#7-shadow-mapping). Follow-ups:
  - The shadow bias of a frustum map is sized from a texel at the **near plane** and does not grow with
    the distance to the light, while a texel covers more and more world space farther away: acne may
    appear on distant surfaces. A bias growing with the depth of the fragment would fix it.
  - The cube faces are not culled by distance: a face is skipped only when the world bounding box does
    not intersect its pyramid, not when everything in it is beyond `maxDistance`.
  - `SpotLight extends PointLight` forces `SpotLight` to re-assert the single-map behavior
    (`generateShadowMap`, `shadowFactorAt`, default and current map size). A shared abstract parent
    (point-like light) with two siblings would remove this trap.
- `SHADOWING_BOX_ELEMENT` and `SHADOWING_BOX_SPECIFIC` are not implemented (they fall back to
  `SHADOWING_BOX_WORLD`). The `SHADOWING_BOX_*` int constants could become an enum, like
  `RenderingType`. Also, `ShadowingLight(int)` (box type) and `ShadowingLight(float)` (intensity) are
  easy to confuse.
- Tuning constants are hard-coded: `SHADOW_BIAS_WORLD`, `DOT_NL_FLOOR`, `SHADOW_BIAS_TEXEL_FACTOR`.
- **Done (October 2026): soft shadows**, `ShadowFilter.PCF_3X3` (per light, off by default). Follow-ups:
  - A wider penumbra far from the object that casts the shadow (PCSS: search of the occluder, then a
    filter size that depends on the distance), and bigger kernels (`PCF_5X5`: the filter is already
    written for any radius, only the enum constant and a re-check of the bias are missing).
  - A switch in `RenderContext` that sets the filter of every light at once, and the choice of making
    `PCF_3X3` the default once the extra cost is measured (see the performance audit).
  - The filtered lookup uses a receiver plane depth bias (the depth gradient of the surface is measured
    per pixel, so the bias of the single test is enough). A wide constant bias detached the shadows of the
    window sills and floor lines of UrbanScape. On the top of the sills, next to the glass, a faint grain
    remains where the hard shadows already show a few specks (very thin gaps): a smaller or smarter
    clamp of the plane correction, or a gradient computed from the geometry instead of two projections,
    could be tried.
- **Performance of the shadows** (cache of the shadow maps with a change flag, a `castShadows` switch per
  light, reuse of the buffers, timing before and after on UrbanScape) is not handled here: it belongs to the
  performance audit, see [PERFORMANCE_AUDIT.md](PERFORMANCE_AUDIT.md) (§R9, R10). A point light costs up to six
  shadow passes per frame, and a soft shadow filter more texel reads per pixel.

## 3b. Visible lights

- **Done (October 2026): the foundation, the halo of point and spot lights, and the sun.** `RenderContext.setLightGlow()`
  (master switch, off by default), `LightGlowMode` (`NONE`, `EMISSIVE`, `HALO`, `SUN`), `LightAppearance`
  (per light: mode, color, core and glow radius, gain), `LightGlowRenderer` (post-process on the finished
  frame, visibility measured in the Z-buffer), `GUIView.addPixel()`, a "Light glow" option in the SceneViewer
  and the demos `TestLightingGlow` and `TestLightingSun`. See [DESIGN.md, Visible lights](DESIGN.md#visible-lights).
  Follow-ups, one effect at a time:
  - **Done (October 2026): the sun** (`LightGlowMode.SUN`, directional lights): disc and halo at the
    projection of the direction towards the sun (`w = 0` point), visibility read in the Z-buffer where the
    sky is (depth = far), size as an angle (`LightAppearance.setSunDiscAngle()`, `setSunGlowAngle()`).
    `TestLightingSun` is the scene to check it (the sun crosses the edges of the buildings). Follow-ups: the
    visibility is measured on the disc only (an edge that covers the disc makes the whole halo vanish, where a
    real sun would keep a glow around the building). The color of the halo is the one of the light or of its
    appearance: it does not turn orange near the horizon.
  - **Done (October 2026): lens flare** of the sun: four ghosts (discs and rings) along the axis from the sun
    through the center of the screen, faded with the visibility and with the distance of the sun to the
    center, `LightAppearance.setFlareGain()`. Follow-ups: the ghosts have fixed colors, positions and sizes
    (no per-light shapes, no dirt or starburst), and they are on by default for the sun; a flare for point
    and spot lights, and for a sun just out of the image (it fades before the edge), could be added.
  - **Done (October 2026): light shafts** of the sun: radial blur of the visible sky towards the sun (64
    steps, mask on a grid four times smaller), added to the image, off by default
    (`LightAppearance.setShaftsGain()`), about 15 ms on a 1000×562 image. Follow-ups: the cost is mostly the
    last pass that spreads the grid over the whole image (a lower resolution of the result, or a pass
    restricted to the pixels the beams reach, could halve it); the beams are computed from the sky only (no
    clouds or fog density), and the fixed constants (decay, weight, exposure, radius of the bright sky) are not
    adjustable per light.
  - **Emissive geometry** beyond the core of the light (a lamp mesh that glows by itself).
  - The glow of a point light is not scaled by its `intensity` or its range (only by `LightAppearance.gain`);
    a halo that fades with the distance to the camera, and a visibility measured on the whole halo (an edge
    that covers only the core makes the whole halo vanish) could be tried.
  - Not available in `LINE` rendering and under an orthographic perspective; a light behind the near plane
    is not drawn even if its halo would reach the image.

## 4. Rasterization

**Done (September 2026).** `TriangleRasterizer` follows a pixel-center convention: a pixel is drawn
when its center (integer coordinates) is inside the triangle, with half-open bounds, and depth and
attributes are sampled at that center. Column and row 0 now have the same size as the others, and
`ScreenLineRenderer` rounds to the nearest pixel center. Shadow map sampling is aligned exactly
for every sign of x and y. `RasterizerStats` counts the triangles rendered with lines (rows inside
the screen, `TriangleRasterizer.getRasterizedLines()`), for the main pass and, now triangle by
triangle, for the shadow maps. See [DESIGN.md §5](DESIGN.md#5-rasterization-and-fragments).

## 5. Rendering options

**Done (September 2026).** `RenderingType` is now `LINE`, `MONOCHROME` (hidden-line wireframe, fill
color set by `RenderContext.setMonochromeColor()`, default the `World` background), `UNLIT` (no
lighting), `FLAT` (faceted shading) and `INTERPOLATE`. `PLAIN` was removed (its uses moved to `FLAT`),
and textures stay a separate option, honored by `UNLIT`, `FLAT` and `INTERPOLATE`. See
[DESIGN.md §8](DESIGN.md#8-configuration-the-two-contexts).

Follow-ups identified while doing it:

- `Sphere` triangles are wound inwards: their face normal (`Triangle.calculateNormal()`) points
  inside while the vertex normals point outside (all other shapes are consistent). `FLAT` now
  re-orients the face normal along the vertex normals (`RenderEngine.orientLikeVertexNormals()`),
  but the root cause is the vertex order of the mesh in `Sphere.generateVertices()`; fixing it
  there changes the texture mapping, so it must be checked with the textured sphere tests.
  Its vertex normals are not unit either (`Vector4.normalize()` applied to a point, `w = 1`, so their
  length depends on the radius): harmless today since lighting renormalizes per pixel, but to fix
  with the winding. Both are reported by the cookbook's `ElementContractChecker`.
- `Triangle.setRectoVerso()` is set by the meshes but not used by the renderer, and
  `Vertex.setColor()` is ignored (colors resolve from the triangle, then the elements): implement
  or remove.
- `ElementContractChecker` (cookbook, test code) could move to the main code as a public validation
  tool for user-defined shapes.
- Depth-tested edges (`MONOCHROME`, `UNLIT` with lines): the tolerances
  `ScreenLineRenderer.EDGE_DEPTH_BIAS_FRUSTUM` (1 %) and `EDGE_DEPTH_BIAS_ORTHOGRAPHIC` are empirical.
  Edges on faces seen at a grazing angle may look dashed.
- The overlay lines of `FLAT` and `INTERPOLATE` (`setRenderingLines(true)`) are still drawn without
  depth test, as before: they could use the depth-tested edges too.

## 5b. Atmospheric perspective (distance fog)

**Done (November 2026).** The haze of the distance: `World.setAtmosphere(Atmosphere)` (visibility, model
`EXPONENTIAL` / `EXPONENTIAL_SQUARED` / `LINEAR`, start distance, maximum factor, color of the haze, by default the
background of the `World`) and `RenderContext.setAtmosphere(true)` (off by default). `AtmosphereRenderer` is a
post-process after the main pass (before the landmarks and the visible lights), from the linear depth of the main
Z-buffer, with the real distance to the camera, computed by rows on several threads (about 2 ms for 1000×562 pixels).
Demos: `TestAtmosphere` (an avenue), the scenes `haze_avenue` and `haze_landscape` of the `SceneViewer` (Atmosphere
option), and, since the fogs, the Haze and Fog items of the Effects menu of `FractalLandscape`.

**Done (November 2026), second step: the atmosphere as a tree, and the fogs.** `Atmosphere` is now the container of the
air: a `Haze` (the former `Atmosphere`) and a list of `Fog`, all `AtmosphericEffect` (own on/off flag), with the
switches `Atmosphere.setHaze(boolean)` and `setFog(boolean)` under the global switch of the `RenderContext`, like the
`Lighting` has an ambient light and several lights. `SphericalFog` (center, radius of action, visibility, color) is a
volume in the coordinates of the `World` with a parabolic density, so with no visible edge, whose optical depth along
the ray of each pixel is exact; it hides the sky too, and a camera can cross it. Demo `TestFog` (the avenue with four
banks), scene `fog_avenue` and the Haze and Fog options of the `SceneViewer`. `FractalLandscape` has the Haze and Fog
items in its Effects menu (no fog by default) and `Landscape` / `Generate fog`, which draws one to three banks at random
places and sizes on the hills, to generate again until it pleases (`FractalLandscape.generateFogs()`, reproducible with a
seed). The [Atmosphere Cookbook](ATMOSPHERE_COOKBOOK.md) and a step-by-step tutorial of the README document it all, with
tested examples (`AtmosphereCookbook`, `GroundMist`). See
[DESIGN.md, Atmosphere](DESIGN.md#atmosphere-the-haze-of-the-distance-the-fogs). Follow-ups, one at a time:

- Fogs with a uniform density (a visible volume with a soft edge only at its surface), and other shapes: a box, a
  horizontal layer (a mist of the valleys that depends on the altitude), a cylinder. Each only implements
  `Fog.opticalDepth()` and a bounding sphere. A horizontal layer already exists as the `GroundMist` of the cookbook (in
  the tests: a layer between two altitudes, with a density that falls linearly, an exact integral); making it a built-in
  class of `com.aventura.model.atmosphere` is a small step.
- The sun and the halos are not attenuated by a fog or by the haze (they are drawn after the atmosphere): a sun seen
  through a thick fog should be dimmer and larger. The optical depth to the light of the sun (or of the lamp) would
  also dim the light that reaches the objects inside the fog (a cheap form of its scattering).
- Other phenomena as new `AtmosphericEffect`: clouds, rain or snow, a layer of smoke. A fog that moves (drifts, grows)
  only needs its center and radius to be changed from one image to the next, with no other cost.

- A haze tinted towards the sun (the color of the haze depends on the angle between the view direction and the
  direction of the sun, warmer towards it), which would suit `sun_street`.
- A mist that depends on the altitude (valleys filled with mist): the factor would also depend on the height of
  the point, which needs its world position, not only its depth (a pass over the pixels with the inverse of the
  view projection, or the position interpolated by the rasterizer).
- A different extinction for each color channel (the red is lost first, so that the distance turns blue), without
  changing the haze color.
- The halos of the point and spot lights could be attenuated by
  the haze, according to the distance of the light: a far lamp would be dimmer in the fog.
- The haze is not drawn in `LINE` rendering (no Z-buffer) nor under an orthographic perspective; the overlay lines
  and the landmarks are never faded.
- The pass reads the Z-buffer through `ZBuffer.get()` for each pixel; a direct access to the rows of the map
  (like `getBackPixels()` for the colors) would make it faster if it ever shows in a profile.
- `GUIView.getBackPixels()` is only implemented by `ImageView` (so `SwingView`): other views are skipped.

## 6. Views

- `ImageView` allocates a new `BufferedImage` for each frame, which keeps the front image immutable
  and thread-safe. A pool (triple buffering) would avoid the allocation.
- Dedicated views for other toolkits (JavaFX, SWT) or an image-sequence writer, on top of `ImageView`.

## 7. Repository housekeeping

- Remove the `Claude outputs` folders (at the project root and under `src/`), left over by earlier
  working sessions. They are not tracked: both are ignored by `.gitignore` and `src/.gitignore`, so
  they only exist in local working copies (the one under `src/` holds old shape sources and zip files).
- Run `mvn test` locally: during these sessions, the code was compiled and tested with `javac` and
  JUnit directly, because Maven could not download its plugins.

## 8. Transformations and numerical accuracy

**Done (November 2026): wrong backface culling of translated elements in orthographic view.** Seen with
`TestMultiElementsTextureOrtho`: after about two turns of accumulated rotations, two faces of the farthest cube
disappeared. When the 3x3 part of the Model matrix is no longer orthogonal (within `Constants.EPSILON`),
`ElementTransform.computeNormalMatrix()` switches from `model` to `transpose(inverse(model))`, whose bottom
row carried the translation: the transformed normals got a non-zero `w`, which the View and Projection
translations then added to the clip-space normal Z used by the orthographic backface test. The translation is
now removed before the inverse transpose. Follow-ups:

- **Drift of accumulated transformations.** `World.expandTransformation()` / `Element.combineTransformation()`
  multiply the Model matrix by a small rotation at every frame, in `float`: the rounding errors accumulate and
  the matrix slowly stops being a rotation (in the test above, the orthogonality test fails after 2692 frames).
  The elements are then slightly sheared and scaled. Options: re-orthonormalize the 3x3 part from time to time
  (Gram-Schmidt), store a rotation (angle, axis or quaternion) and rebuild the matrix from it, or let the
  callers compute the transformation from the total angle rather than accumulating increments.
- **Two code paths for the normal matrix.** The switch on an epsilon orthogonality test means a matrix can
  jump from one path to the other between two frames, which is what made this bug appear only after a while.
  Always using the inverse transpose of the 3x3 part (cached per Element when its transformation changes)
  would give a single, continuous behavior.

## 9. Reading and writing 3D model files

Saving a scene to a file and loading it back is not available yet: scenes exist only as Java code. The analysis,
the file format, the architecture and the step-by-step plan are in [MODEL_FILE_IO.md](MODEL_FILE_IO.md).

- **Aventura JSON format first**, since it describes the whole element tree with its local model matrices,
  colors and textures, which flat formats such as OBJ cannot. Phases: (1) geometry tree with raw geometry,
  (2) built-in shapes written as type + parameters, (3) camera, lights and atmosphere, (4) other formats
  (OBJ, already listed as an idea in [DESIGN.md §11](DESIGN.md#11-history-limitations-and-roadmap)).
- **Separate Maven target.** The reader and writer, and the JSON library they need, live in their own module
  (`aventura-io`, built and released separately) so that the rendering engine keeps no dependency other than
  the JDK. The engine never references the module. Layout decided: a sibling
  folder `aventura-io/` next to the root `pom.xml` (the engine build and the Eclipse configuration do not change); a
  multi-module reactor remains possible later, see [MODEL_FILE_IO.md §6](MODEL_FILE_IO.md#6-maven-structure-a-separate-build-target).
- **Small engine changes needed first** ([§7](MODEL_FILE_IO.md#7-changes-required-in-the-engine)): a getter for the
  local model matrix of an `Element` (`getTransformation()` returns the accumulated one), and a `Texture` that
  remembers its source file and orientation arguments (it keeps only its texels today). Phase 2 also needs getters
  for the constructor parameters of the built-in shapes (only `Trellis` has some).
- Decided: the file extension is `.av3d` (JSON text), every file starts with the signature `AVENTURA3D` and a
  `major.minor` format version (1.0 first, see the versioning policy of MODEL_FILE_IO.md §4); older versions are read by default, and rewriting
  one over itself in the current version needs a confirmation from the application (never silent); textures are not embedded, they are referenced by a path
  relative to the model file. The product is called "Aventura 3D", but the Maven coordinates and the packages keep the name
  Aventura. Still open: optional compression, and where the demo that opens a file lives.
