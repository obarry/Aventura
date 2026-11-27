# Aventura backlog

Items identified during the September 2026 clean-up phase (Rasterizer removal, contexts audit,
configurable shadow maps, GUI-independent view) and the October 2026 spot and point light shadows,
and deliberately **not** handled yet.
See also the roadmap in [DESIGN.md](DESIGN.md#12-history-limitations-and-roadmap).

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

**Done (November 2026): the base class of the maps.** `SurfaceMap` (in `model.texture`, abstract) holds what all
the maps laid on a surface share: size, name, wrap mode (`WrapMode` moved there from `Texture`; `Texture.WrapMode`
still names it, but an `import ...Texture.WrapMode` must become `import ...SurfaceMap.WrapMode`), the folding of the
indices, the sizes of the mipmap levels and the choice of the level. `Texture` extends it (colors); the role of a map
is given by where it is put (the texture of a triangle, its emission map), not by its class. A future `FloatMap` for
bump or height maps would extend `SurfaceMap` too, storing a float per texel and averaging them in `buildLevels()`;
the bilinear interpolation, still written in `Texture` for its packed ARGB texels, could then move to the base class
behind a "read one texel" method.

**Next: bump and normal mapping, the feature itself.** The relief of a surface (the joints of stones, the flutes
of a worn column, the rivets of a metal panel) without more triangles: the normal used by the lighting is changed
at each pixel, the geometry is not. Two classic forms, in the order of teaching:

- **Bump map** (Blinn, 1978): a map of heights (the `FloatMap` above). The normal is tilted by the slopes of the
  height along the two texture directions (finite differences of the map at the texel). It shows the principle
  best, and a gray image is easy to paint or to compute (`Noise`).
- **Normal map**: an RGB image storing the normal itself, the usual format of today's assets. Read as a `Texture`,
  no new map class is needed.

Both need a **tangent frame** per vertex (tangent and bitangent, computed from the texture coordinates of the
triangles, in the geometry stage) so that the perturbation follows the surface, and a normal **per pixel**: only
the `INTERPOLATE` rendering type (`FLAT` has one normal per face). Points to watch: the mipmaps of a normal map
average vectors that must be normalized again; the light vectors and the shadow bias stay those of the geometric
surface; a surface lit at a grazing angle shows the limit of the technique (the silhouette stays flat; parallax
mapping is the next step). Options: `Element.setBumpMap()` / `setNormalMap()`, as `setEmissionMap()`, and a
`RenderContext` switch. The `SurfaceMap` base class and its mipmaps are ready for it.

**Tools to make bump maps, outside the engine.** The engine reads a bump map, it does not make one: the tools go in
the tools module next to it (`aventura-io`, where the reading and writing of 3D files will live, or a sibling module
of tools), never in `aventura-engine`. Two families:

- **From an existing texture**: a height guessed from the image. The classic heuristic is the luminance (dark = deep:
  the joints between stones, the cracks), after a **high-pass filter** that removes the slow changes of light of the
  photograph (a large blur subtracted from the image), then a light blur against the noise. From the height, a
  normal map by **convolution** with the Sobel kernels (the slopes along x and y). Parameters: strength, size of the
  filters, inversion. Limits to be stated: the luminance is not the relief (a dark stain becomes a hole); a smarter
  analysis (shape from shading, or a trained model) is beyond a didactic pure Java engine.
- **Generic bump maps**, computed: noise (the `Noise` of the demos, Perlin or value noise, several octaves) for
  stone, plaster or hammered metal; bricks and tiles with their joints; plates and rivets; wood grain; scratches. All
  of them **tileable** (they wrap without a seam, for `WrapMode.REPEAT`).

Outputs: a `FloatMap` in memory, or an image file (gray for a height, RGB for a normal map), so that the textures of
the demos get theirs once, ahead of time. Then the demos use them: the stones and columns of `TempleRuins`, the
plates of `StarshipInterior`.

## 2b. Several textures on one surface (layers, decals)

A surface often combines a **material** (the metal plates, the stone, repeated over the whole surface) and a
**decoration** shown once on a part of it (a code painted on a wall, a sign, a stain, an arrow). Today a triangle has
one texture (and an emission map, §3b), so `StarshipInterior` bakes its codes: `WallMarking` copies, pixel by pixel,
the plates of the wall into the texture of the decal, then paints the letters over them, and the section of the wall
gets steps so that the decal has its own triangles (see STARSHIP_INTERIOR.md). It works, at the cost of a texture
per decal, of extra triangles, and of code that only this demo has.

The possible solutions, by their cost in the rasterization:

| Solution | Cost per pixel | Comments |
|---|---|---|
| **Baking** (what the demo does), made a tool | None | The textures are composed once, ahead of time (in the tools module, like the bump maps of §2). Memory: a texture per decal; the decal needs its own triangles |
| **Separate triangles** in front of the surface | Overdraw, depth bias | Needs transparency, which the engine does not have, and fights z-fighting: not recommended |
| **A second layer read by the rasterizer** | One more texture read, only on the triangles that have a layer | The proposal below |

**Proposal: a decoration layer placed in the texture coordinates of the surface.** A layer is a `Texture` with
alpha (the texels are already ARGB), a rectangle in the `(u, v)` space of the base texture where it is shown, and a
blending mode: *over* (alpha: letters, signs), *multiply* (dirt, stains, a light map), *add* (already covered by the
emission maps). Its own coordinates are an affine function of the base ones, `(u - u0) / width`, so the vertices need
**no second set of texture coordinates** and the triangles nothing new: outside the rectangle the layer is simply not
read (an early test per pixel, or per triangle when its bounding box in `(u, v)` misses the rectangle). Each layer has
its mipmaps (`SurfaceMap`). The base texture stays tiled, the decal texture holds only its picture: smaller, and the
seams and rivets of the plates go on under it by construction. A short list of layers per `Element` (like
`setEmissionMap()`) keeps the cost at zero for the surfaces without one.

Then **update `StarshipInterior`**: `WallMarking` draws only the letters (with alpha) and no longer copies the plates,
the sections no longer need steps for their decals, and the arrows and signs listed in the ideas of
STARSHIP_INTERIOR.md become cheap. A visual test program and a pixel test (a layer over a plain texture) go with it.

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
- `SHADOWING_BOX_SPECIFIC` is not implemented (it falls back to `SHADOWING_BOX_WORLD`;
  `SHADOWING_BOX_ELEMENT` is, through `setShadowingElement()`, for a `DirectionalLight`). The `SHADOWING_BOX_*` int constants could become an enum, like
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
- **Done (November 2026): a light switched off**, `Light.setEnabled(boolean)` (on by default): the light stays in
  its `Lighting` but adds nothing to the shading, draws no shadow map (the last one is kept, and reused when the
  light is switched on again with static casters) and is not drawn as a visible light. Lets a scene with many
  lights keep on only those near the camera (the corridors of the spaceship demo). `TestLightEnabled`. The demos
  share a `LightBudget` (`com.aventura.demo`): the lights nearest to the camera, those in front first, faded in and out
  (see [STARSHIP_INTERIOR.md](STARSHIP_INTERIOR.md#the-light-budget)). Follow-ups: a budget by zones (the walls ignored
  by the distance today), and its move into the engine if an application needs it.
- **Performance of the shadows** (cache of the shadow maps: done for static casters, `setStaticShadowCasters()`; version counters on the Elements to detect moving casters by themselves (audit D8), a `castShadows` switch per
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
  - **Done (November 2026): emissive surfaces and emission maps.** `Element.setEmissionColor()` and
    `setEmissionMap()` (inherited by the sub-elements, overridden by `Triangle.setEmissionColor()` /
    `setEmissionMap()`): a color, or a `Texture` read at the texture coordinates of the surface, added to the shaded
    color whatever the lights and the shadows (`Material.emissionAt()`, `AbstractMaterial`). `TestEmission`, and the
    visual test `TestEmissionMap`. See [DESIGN.md §5](DESIGN.md#5-rasterization-and-fragments). Follow-ups: the
    emission lights nothing around it (a light must be added for that) and does not make a halo (a bloom pass on the
    bright pixels could); it is not drawn by the `UNLIT` and `MONOCHROME` types; its intensity is a color (at most
    white): an emission stronger than white, for a bloom, would need a gain. Used by the StarshipInterior demo:
    its screens, lamps and buttons glow (see
    [STARSHIP_INTERIOR.md §6](STARSHIP_INTERIOR.md#screens-and-lamps-that-glow)).
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

**Mipmapping done (November 2026).** `RenderContext.setMipmapping(true)` reads each texture at a
level of detail chosen from the size of the pixel on the surface (`Texture.buildMipmaps()`,
trilinear filtering), off by default. See [DESIGN.md §5](DESIGN.md#5-rasterization-and-fragments).
Not handled yet:

- **Anisotropic filtering.** On a surface seen at a low angle, a pixel covers a long and thin strip
  of texture. One level cannot fit both its length and its width: today a fixed share of the
  stretching is added to the size of the pixel (`TriangleRasterizer.TEX_STRETCH_SHARE`), a
  compromise between noise and blur. Several samples along the strip, in the level that fits its
  width, would be sharp and smooth at the same time (for 2 to 8 times the texel reads).
- **Mipmapping on by default?** It is off so that the existing renderings do not change. Since it
  only changes minified textures, for the better, the default could be reversed.
- **Other maps.** The pyramid is now built by `SurfaceMap` (sizes of the levels) and its subclass (the values):
  a future bump or normal map (§2) gets it by extending `SurfaceMap`.

## 5. Rendering options

**Done (September 2026).** `RenderingType` is now `LINE`, `MONOCHROME` (hidden-line wireframe, fill
color set by `RenderContext.setMonochromeColor()`, default the `World` background), `UNLIT` (no
lighting), `FLAT` (faceted shading) and `INTERPOLATE`. `PLAIN` was removed (its uses moved to `FLAT`),
and textures stay a separate option, honored by `UNLIT`, `FLAT` and `INTERPOLATE`. See
[DESIGN.md §9](DESIGN.md#9-configuration-the-two-contexts).

Follow-ups identified while doing it:

- `Sphere` triangles are wound inwards: their face normal (`Triangle.calculateNormal()`) points
  inside while the vertex normals point outside (all other shapes are consistent). `FLAT` now
  re-orients the face normal along the vertex normals (`RenderEngine.orientLikeVertexNormals()`),
  but the root cause is the vertex order of the mesh in `Sphere.generateVertices()`; fixing it
  there changes the texture mapping, so it must be checked with the textured sphere tests.
  It is reported by the cookbook's `ElementContractChecker`. (Its vertex normals, which were not unit
  either, are fixed: see §8.)
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

## 5c. Depth of field

A camera lens is sharp at one distance only: what is nearer or farther is blurred, the more so as it is far from
that distance. It draws the eye to the subject and gives a photographic look (a close-up of a capital of the temple
with the sea blurred behind it).

**Feasible as a post-process, like the haze.** The main Z-buffer already holds the view depth `W` of each pixel, and
the post-processes already run by bands of rows (`BandRasterizer.runRows()`):

1. **Circle of confusion.** From a thin lens model, the diameter of the blur of a point at depth `z`, for a focus
   distance `zf`, is proportional to `|1/zf - 1/z|`, times a strength that plays the role of the aperture. Options:
   `Camera` (or `RenderContext`) `setFocusDistance()`, `setAperture()` or a maximum blur radius in pixels; "focus on
   the pixel under the mouse" is then one read of the Z-buffer.
2. **Blur by a variable radius.** For each pixel, the average of the pixels of a disc of that radius, read from a
   copy of the image (a band must not read what its neighbor is writing).

The difficulties are those of every post-process depth of field, and they are good teaching material:

- **Bleeding**: a blurred background must not spread over a sharp subject in front of it. The samples are weighted
  by their own circle of confusion and their depth (a sample farther than the pixel only counts as much as it is
  blurred itself).
- **The hidden background**: a blurred foreground should let one see a little of what is behind its edges, which an
  image with one depth per pixel does not have. The usual compromise is two layers (near field and far field)
  blurred apart, then composed.
- **Cost**: a disc of radius `r` is about `3 r²` samples per pixel, too much on a CPU for a large blur. Cheaper
  forms: a pyramid of blurred images (mipmaps of the image, chosen per pixel by the size of its blur), the blur
  computed at half resolution, or a separable approximation (two passes, exact only for a constant radius).

Order in the frame: after the haze and the fogs (they belong to the scene), before the visible lights (the sun and
its lens flare are effects of the lens, drawn over the image). Not under an orthographic perspective, not in `LINE`.
A first version (one layer, weighted gather, half resolution) is a few hundred lines in an `AtmosphereRenderer`-like
class, with an option in the `SceneViewer` and `TempleRuins`.

## 6. Views

- **Triple buffering: done (v3.1.0).** By default `ImageView` still allocates a new `BufferedImage`
  for each frame, which keeps every front image immutable and safe to keep. `setImageReuse(true)`
  recycles three images in turn instead (front, previous front, spare), so that an animation
  allocates no image per frame; the condition is that an image is displayed or copied within two
  frames. Enabled in all the animated demos (`TempleRuins`, `UrbanScape`, `FractalLandscape`...).
- Dedicated views for other toolkits (JavaFX, SWT) or an image-sequence writer, on top of `ImageView`.
- **Display on a scaled screen (HiDPI) by the graphics chip.** On a screen scaled by Windows (200% on
  a laptop), Java enlarges the image of the engine at each frame when it copies it into the window,
  with its software routines: Java2D cannot accelerate a `BufferedImage` that changes at each frame
  and whose pixels are written directly (`getDataBuffer()`, as `ImageView` does). Measured on the
  `TempleRuins` tour: about 7 ms per image outside of the rendering at 200%, against 3 ms on a screen
  at 100%, and 17% fewer images displayed (see PERFORMANCE_AUDIT §11). Idea: copy the image 1:1 into
  a `VolatileImage` (kept by Java2D in the memory of the graphics chip), then draw that one into the
  window, so that the enlargement is done by the chip. About twenty lines, in `GUIView`/`SwingView`
  or in a demo first; to be measured, since it depends on the graphics pipeline Java enables. The
  engine keeps rendering on the CPU: only the display uses a standard JDK feature.

## 7. Repository housekeeping

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
- **Two code paths for the normal matrix. Done (November 2026).** The switch on an epsilon orthogonality test
  meant a matrix could jump from one path to the other between two frames, which is what made this bug appear
  only after a while. `ElementTransform` now always uses the inverse transpose of the 3x3 part of the Model
  matrix, as a `Matrix3`, computed without any inversion (`Matrix3.cofactors()` / determinant): a single,
  continuous behavior, and the translation can no longer reach the normals. Not cached per Element: it is
  computed once per Element and per frame, a few dozen multiplications.

**Done (November 2026): 3D types where 3D is meant** (vector and matrix audit, Refactoring branch). Directions
and displacements are `Vector3`, points `Vector4` (`w = 1`), the transformation of the normals a `Matrix3`:

- The normal matrix of `ElementTransform` is a `Matrix3` (above). `Matrix3` is written out like `Matrix4`
  (`times(Vector3)`, `times(Matrix3)`), and gets `cofactors()`.
- The offsets of the shapes (`center` of `Sphere`, `Cone`, `ConeFrustum`, `Disc`, `position` of `Trellis`) are
  `Vector3` displacements, added to the points with `Vector4.plus(Vector3)`, and their reference points
  (`bottom_center`, `top_center`) are points. Three bugs came from offsets stored as `Vector4` with `w = 0`:
  the vertex normals of `Sphere` were not unit (0.707 for a radius of 1: the `w` of the point was normalized
  with x, y and z); `new ConeFrustum(..., Vector4 center)` with a point (`w = 1`) gave every vertex `w = 2`
  (shape halved and moved); a `ConeFrustum` moved by its center got wrong normals. Tests: `TestShapeVectors`,
  `TestElementTransform`, `TestMatrix3`. Images of the documentation unchanged (at most 8 pixels differ by
  one level, rounding); `PerfBench` checksums unchanged.
- The `math` packages no longer use `Tracer`: they depend only on the JDK. The `try`/`catch` blocks that only
  traced impossible exceptions are gone (`getRow()`/`getColumn()` of `Matrix3`/`Matrix4`, `Translation`), and
  `Vector2 × Matrix2` is written out like the other products.
- The clip-space normals of the vertices are computed only under an `ORTHOGRAPHIC` projection, the only one whose
  backface test reads them (`ElementTransform.transformElement(e, normals, clipNormals)`). Back-face culling in
  screen space (performance audit R3) would remove them altogether.
- Differences of points are built directly as `Vector3`s (`new Vector3(a, b)`, the vector from a to b) rather than
  `b.minus(a).V3()`: `RenderEngine.isBackFace()`, `Triangle.calculateNormal()`, the vertex normals of `Cone`,
  `ConeFrustum`, `Torus` and `Trellis`, `Boulder`, `FlightPath`.
- `Vector4.normalize()`, `length()` and `dot()` include `w`: their Javadoc says so. Three methods no longer modify a
  `Vector4` they receive (`LookAt.generateLookAt(fwd, up)`, `Perspective.getFrustumFromEye()`,
  `World.getMaxDistance(p)`; test `TestArgumentsNotModified`).
- A single computation of the light → point vector per pixel and per light: `Light.sample(point, LightSample)`
  (performance audit N3, done).

## 9. Reading and writing 3D model files

Saving a scene to a file and loading it back is not available yet: scenes exist only as Java code. The analysis,
the file format, the architecture and the step-by-step plan are in [MODEL_FILE_IO.md](MODEL_FILE_IO.md).

- **Aventura JSON format first**, since it describes the whole element tree with its local model matrices,
  colors and textures, which flat formats such as OBJ cannot. Phases: (1) geometry tree with raw geometry,
  (2) built-in shapes written as type + parameters, (3) camera, lights and atmosphere, (4) other formats
  (OBJ, already listed as an idea in [DESIGN.md §12](DESIGN.md#12-history-limitations-and-roadmap)).
- **Separate Maven target.** The reader and writer, and the JSON library they need, live in their own module
  (`aventura-io`) so that the rendering engine keeps no dependency other than the JDK. The engine never references
  the module. Since the project is a multi-module reactor (`aventura-engine`, `aventura-demos`), `aventura-io` will
  be a third module of it (see [MODEL_FILE_IO.md §6](MODEL_FILE_IO.md#6-maven-structure-a-separate-build-target)).
- **Small engine changes needed first** ([§7](MODEL_FILE_IO.md#7-changes-required-in-the-engine)): a getter for the
  local model matrix of an `Element` (`getTransformation()` returns the accumulated one), and a `Texture` that
  remembers its source file and orientation arguments (it keeps only its texels today). Phase 2 also needs getters
  for the constructor parameters of the built-in shapes (only `Trellis` has some).
- Decided: the file extension is `.av3d` (JSON text), every file starts with the signature `AVENTURA3D` and a
  `major.minor` format version (1.0 first, see the versioning policy of MODEL_FILE_IO.md §4); older versions are read by default, and rewriting
  one over itself in the current version needs a confirmation from the application (never silent); textures are not embedded, they are referenced by a path
  relative to the model file. The product is called "Aventura 3D", but the Maven coordinates and the packages keep the name
  Aventura. Still open: optional compression, and where the demo that opens a file lives.

## 10. Embedding Aventura in an application

Beyond teaching, a use case for the engine is a small 3D view inside a pure Java application, without OpenGL
bindings, JavaFX 3D or Java 3D and their native dependencies (see the "Use cases" section of the README). The
engine can already do it; what is missing is mostly around it. Each item stays a good teaching subject.

- **A ready-made Swing component.** A `JComponent` (e.g. `AventuraPanel`) that wraps a `RenderEngine` and a
  `SwingView`, follows the size of the component, and renders off the Swing thread, always on the latest state
  of the scene and the camera (the model of `SceneViewer`, PERFORMANCE_AUDIT D10). Today each demo writes its own
  window, render loop and camera controller.
- **Camera controllers in the API.** Orbit (drag to turn, wheel to zoom), fly and path cameras, written today in
  each demo (`TempleRuins`, `SceneViewer`, `FractalLandscape`); see the limitations of TEMPLE_RUINS §9. The
  component above would offer the orbit one by default.
- **Rendering on demand.** An image only when the scene, the camera, the lights or the size change, so that a 3D
  view at rest uses no CPU: a "changed" flag set by the setters of the API or by the component, close to the version
  counters of PERFORMANCE_AUDIT D8.
- **Textures from the classpath: done (3.3).** `new Texture(URL)`, e.g.
  `new Texture(MyApp.class.getResource("/texture/marble.jpg"))`; the demos read theirs this way, as resources of
  their module (`DemoTextures`).
- **Model import.** OBJ at least, and the Aventura format (§9, [MODEL_FILE_IO.md](MODEL_FILE_IO.md)).
- **Picking.** Which `Element` (and which triangle) is under a pixel, to react to a click: an ID buffer filled by a
  `FragmentConsumer` during the main pass (listed as an extension point in DESIGN §10), or a ray cast through the
  pixel and tested against the triangles. Labels (text attached to a 3D point) would go with it.
- **Packaging.** **Done (3.3):** the engine alone is its own module and jar (`aventura-engine`), the demos and
  their textures another (`aventura-demos`). Still to do: runnable jars of the demos (`TempleRuins` first), a
  published artifact (Maven Central: the `groupId` must be a domain one owns, or `io.github.obarry`), a
  `module-info.java` that exports the API packages only; a pom version that follows the
  release tags (**done**: `3.3.0-SNAPSHOT`, after the 3.2.0 release; it was `0.0.1-SNAPSHOT`); a stable API with semantic
  versioning from then on.
- **Other toolkits.** A JavaFX view (§6), so that the same component exists in both toolkits.

## 11. Performance, phase 2

The pixel stages are multithreaded (v3.0.0); what remains sequential now limits the gain of the threads. The
proposals, their expected gains and their order are in
[PERFORMANCE_AUDIT.md §12](PERFORMANCE_AUDIT.md#12-update-of-november-26-2026-phase-2-what-remains-sequential),
on the `perf-multithreading` branch:

- **P0. Measure first**: the `TempleRuins` scene in `PerfBench`, the phases for 1 and N threads.
- **R1**: hierarchical frustum culling by `Element`, before any new threads.
- **MT4 / MT5**: the geometry stages of the main pass and of the shadow maps in parallel, by batches of `Element`s,
  with identical images.
- **D8**: version counters, first to skip `worldProject()` when nothing has moved.
- **V2**: render while the window displays (a rendering thread, as in `SceneViewer`), in the Swing component of §10.
- Later: **MT7** (buffers cleared by bands) if measured worth it; **D4** then **MT8** (several shadow maps at the
  same time).

## 12. Motion: curves, surfaces and animation

A large evolution, to be split into steps. Today the movements are written by each demo: `FlightPath` (a closed
Catmull-Rom spline, parametrized by the distance travelled) and `KeyFrame` live in `aventura-demos`, `TempleRuins`
and `StarshipInterior` have their own paths and easings; the engine only offers `Quaternion.slerp()`. Proposal: a
pure math package, standalone like the other `math` packages, then a thin animation layer on top of it.

- **Curves** (`math.curve`): Bézier curves (de Casteljau's algorithm, the most didactic), Hermite and Catmull-Rom
  (from `FlightPath`), uniform cubic B-splines, then rational ones (NURBS: exact circles). One interface: a point,
  a tangent and a second derivative at a parameter `t`.
- **Moving at a given speed**: the parameter `t` of a curve is not a distance. Reparametrization by arc length
  (a table of lengths sampled once, inverted at run time), then **easing** (ease in and out) on top: what each demo
  does by hand today.
- **Orientation along a path**: a camera or an object that follows a curve must turn with it. The Frenet frame
  (tangent, normal, binormal) flips at inflection points; a rotation minimizing frame, or an up vector kept close
  to `Z`, does not. Orientations given at keyframes are interpolated with quaternions (`slerp`, then `squad` for a
  smooth chain).
- **Animation tracks**: keyframes of position, orientation and scale (the `T · R · S` of an `Element`) or of a
  `Camera` (eye, target, up), each track interpolated by one of the curves above, played by a clock (time elapsed,
  not number of images). `KeyFrame` and `FlightPath` would move from the demos to the engine through it.
- **Surfaces**: the same mathematics in two parameters. Bézier patches (the Utah teapot is the classic example),
  B-spline and NURBS surfaces, as a `GenerativeElement` tessellated into triangles with exact normals (from the
  two partial derivatives) and texture coordinates `(u, v)`. A fine subject for the Geometry Cookbook.
- Later, **physical motion**: velocity, acceleration, gravity, and the integration of the equations of movement
  (explicit Euler, then semi-implicit Euler or Verlet, and why the first one drifts). It goes with §13.

Order: curves and arc length, then the tracks (with `FlightPath` and `KeyFrame` moved), then the surfaces, then the
physics. Each step can be tested without a window (positions, tangents, lengths against analytical values).

**Then update the demos to use it.** Once the curves and tracks are in the engine, the demos drop their own code:
the helicopter tour of `TempleRuins` and its easing (`FlightPath`), the visit of `StarshipInterior` and its stops, the
helicopter flight of `UrbanScape` (`HelicopterFlight`), the fly camera of `FractalLandscape`, `MovingCamera` and its
`KeyFrame`s. The images must stay the same (or the change be stated): the tests of the demos and
`DocumentationImages` check it. The same holds for §13: the camera controllers of the demos move to the reusable one.

## 13. Collision detection

Also written by each demo today, each in its own way: the camera stays above the ground in `TempleRuins` and
`FractalLandscape` (the height of the terrain under it and around it), inside rectangles of the corridors in
`StarshipInterior` (`Ship`). Proposal, in the same spirit as §12:

- **Bounding volumes**: a sphere and an axis-aligned box per `Element`, and their hierarchy along the tree. They are
  shared with three other items: the hierarchical frustum culling (PERFORMANCE_AUDIT R1), the culling of the shadow
  passes (N4), and the picking of §10. To be built once, for all of them, and kept up to date when an `Element`
  moves (version counters, PERFORMANCE_AUDIT D8).
- **Queries**: ray / triangle (Möller-Trumbore), which is also the picking of §10 and the "height of the ground
  under the camera" of the demos; sphere / triangle for a camera or a moving object; box / box and sphere / sphere
  between objects.
- **Broad phase and narrow phase**: first the bounding volumes, to find the few pairs that may touch, then the
  triangles of those pairs only. A structure in space for large scenes: a uniform grid (the simplest), then a BVH
  or an octree.
- **Response**: stop, or slide along the surface (the projection of the movement on the plane of contact), which is
  what the demos do for the camera. Bouncing and friction belong to the physics of §12.
- **Fast movements**: a moving point can cross a thin wall between two images (tunneling). Continuous detection:
  test the segment (or the swept sphere) of the movement, not the end position only.

First use: a reusable camera controller (§10) that keeps the camera out of the walls and above the ground, which
replaces the code of the three demos. Then collisions between moving objects, with §12.
