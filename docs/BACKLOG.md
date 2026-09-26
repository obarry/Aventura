# Aventura backlog

Items identified during the September 2026 clean-up phase (Rasterizer removal, contexts audit,
configurable shadow maps, GUI-independent view) and deliberately **not** handled yet.
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

- **Spot light shadows**: one perspective (frustum) shadow map. `SHADOW_MAP_FAR_NDC` in
  `ShadowingLight.generateShadowMap()` assumes the orthographic [0, 1] depth convention, so it must
  be re-checked for a frustum projection.
- **Point light shadows**: a cube map (6 faces). Default resolution planned at **512 per face**,
  exposed through a `PointLight.DEFAULT_SHADOW_MAP_SIZE` constant and an override of
  `getDefaultShadowMapSize()` (the mechanism is in place).
- `SHADOWING_BOX_ELEMENT` and `SHADOWING_BOX_SPECIFIC` are not implemented (they fall back to
  `SHADOWING_BOX_WORLD`). The `SHADOWING_BOX_*` int constants could become an enum, like
  `RenderingType`. Also, `ShadowingLight(int)` (box type) and `ShadowingLight(float)` (intensity) are
  easy to confuse.
- Tuning constants are hard-coded: `SHADOW_BIAS_WORLD`, `DOT_NL_FLOOR`, `SHADOW_BIAS_TEXEL_FACTOR`.
- Soft shadows (PCF).

## 4. Rasterization

- `TriangleRasterizer` converts to pixels with `(int)` casts, which truncate toward zero: column 0
  and row 0 cover twice the width of the others. Use `floor` or a pixel-center convention. Shadow
  map sampling could then be aligned exactly (today it keeps the former alignment, which is exact
  for x >= 0 only).
- `RasterizerStats`: "rendered with lines" is always 0 (lines are not counted).

## 5. Rendering options

- `RenderingType.MONOCHROME` is declared but not implemented (it draws nothing).
- `RenderingType.PLAIN` always processes textures, whatever `setTextureProcessing()` says (legacy
  behavior, documented).

## 6. Views

- `ImageView` allocates a new `BufferedImage` for each frame, which keeps the front image immutable
  and thread-safe. A pool (triple buffering) would avoid the allocation.
- Dedicated views for other toolkits (JavaFX, SWT) or an image-sequence writer, on top of `ImageView`.

## 7. Repository housekeeping

- Remove `aventura_export_tmp.tar.gz` (untracked) and the `Claude outputs` folders (at the project
  root and under `src/`), left over by earlier working sessions.
- Run `mvn test` locally: during these sessions, the code was compiled and tested with `javac` and
  JUnit directly, because Maven could not download its plugins.
