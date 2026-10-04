# Aventura backlog

Items identified during the September 2026 clean-up phase (Rasterizer removal, contexts audit,
configurable shadow maps, GUI-independent view) and the October 2026 spot and point light shadows,
and deliberately **not** handled yet.
See also the roadmap in [DESIGN.md](DESIGN.md#11-history-limitations-and-roadmap).

## 1. Perspective / Viewport split

`PerspectiveContext` mixes the lens (the `Perspective`: view volume and projection, in world units)
and the raster target (pixel size), linked by a pixels-per-unit ratio.

- Pixel dimensions are fixed at construction and do not follow later `Perspective` changes;
  `ppu` only reflects the construction-time ratio.
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
