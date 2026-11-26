# Aventura — Performance audit

*[Version française](PERFORMANCE_AUDIT.fr.md)*

*Version 1: September 30, 2026. **Version 2: October 6, 2026**, updated on the `Refactoring` branch (commit
`d3a2a47`, tag `v2-3-0`). Scope: `src/main` (engine, model, math, views), pure Java, CPU rendering.*

> **What changes in version 2.** Since version 1, the engine has gained the shadows of spot lights (perspective
> shadow map) and of point lights (cube map with 6 faces), near-plane clipping, soft shadows (PCF 3 × 3, now on by
> default in `UrbanScape`) and visible lights (halo, sun, lens flare, light shafts). The per-pixel path of the
> engine has not changed: **all the findings of version 1 still hold**. Version 2 adds the **measured cost of the
> new features**, new proposals (N1 to N7) and the **progress** of each proposal: see
> [§10](#10-update-of-october-6-2026-new-features-and-progress). Sections §0 to §9 are those of version 1, completed
> with "**v2**" boxes where the situation has changed.

The goal is to speed up the engine **without making it unreadable**: each proposal gives its gain (measured when
possible), its cost and its effect on readability. The identifiers (A1, D3, R5…) are used again in the roadmap of
§8.

## Contents

0. [Summary](#0-summary)
1. [Method and measurements](#1-method-and-measurements)
2. [Algorithms (programming)](#2-algorithms-programming)
3. [Design](#3-design)
4. [Rendering techniques: removing work upstream](#4-rendering-techniques-removing-work-upstream)
5. [The three dimensions: memory, CPU, I/O](#5-the-three-dimensions-memory-cpu-io)
6. [Multithreading](#6-multithreading)
7. [Tracer and instrumentation](#7-tracer-and-instrumentation)
8. [Proposed roadmap](#8-proposed-roadmap)
9. [What not to do](#9-what-not-to-do)
10. [Update of October 6, 2026: new features and progress](#10-update-of-october-6-2026-new-features-and-progress)
11. [Update of November 17, 2026: what surrounds the rendering](#11-update-of-november-17-2026-what-surrounds-the-rendering)
12. [Update of November 26, 2026: phase 2, what remains sequential](#12-update-of-november-26-2026-phase-2-what-remains-sequential)

---

## 0. Summary

**The engine is limited by the processing of pixels, not by the geometry.** In `UrbanScape` (8,912 triangles,
1280 × 720), **96%** of the CPU time is spent in `TriangleRasterizer.rasterizeScanLine()` and the consumers it
calls. Transforming the vertices takes about **1%**. The work must therefore focus on the per-pixel path, on the
number of pixels processed, then on sharing this work between the cores.

Main findings:

1. **About 145 MB allocated per frame**, almost all of it in the per-pixel loop (`java.awt.Color`, `Vector3`,
   `Vector4`, the `int[]` created by `BufferedImage.setRGB()`). The "zero allocation per pixel" principle of
   DESIGN §5 only holds for the `Fragment` object.
2. **Z-buffer, shadow map and textures are stored by columns (`[x][y]`) but traversed by rows**: each pixel of a row
   falls in a different array, which causes cache misses.
3. **The static counters of the `Vector3` and `Vector4` constructors** (`count()`, tied to `Tracer.object`) are
   written at each allocation. They cost little with one thread, but **they prevent any gain with several
   threads**: in the prototype, 2 threads were *slower* than one as long as they were there.
4. **Shadows double the time of a frame**: the shadow map (2000 × 2000 in UrbanScape) is allocated and computed
   again at each frame, whereas with a fixed light and scene it does not change.
5. **About 46% of the triangles sent to the rasterizer cover no pixel center**, and **1.18 million pixels are
   shaded for 0.92 million pixels on screen**: there is room for removing work upstream.
6. The **8 to 10% CPU usage** observed corresponds exactly to **a single busy logical core** (1/12 ≈ 8% on a
   machine with 12 threads). Multithreading is therefore the biggest lever, **but only once points 1 to 3 are
   fixed**.

Gains measured with prototypes (on a throwaway copy, not in the repository):

| Step (cumulative) | Without shadows | With shadows |
|---|---:|---:|
| Reference | 114 ms | 267 ms |
| + Z-buffer and shadow map as 1D `float[]`, row by row (D7) | 109 ms | 239 ms |
| + writing directly into the `int[]` of the image (D6) | 98 ms | 229 ms |
| + unrolled `Matrix4 × Vector4`, light color computed once (A4, A2) | **84 ms** (−26%) | 214 ms |
| + shadow map kept from one frame to the next (static scene, R9) | — | **≈ 137 ms** (−49%) |
| Multithreaded: 2 threads, 8 horizontal bands (§6), without the static counters | **≈ 53 ms** against ≈ 95 ms single-threaded with the same code | not tested |

These measurements were taken on a **2-core** machine (Xeon 2.8 GHz, Java 21). The absolute values will be
different on the author's laptop; what matters are the trends. On 8 to 12 threads, a gain of **×3 to ×5** on the
pixel stage is realistic after phases 1 and 2.

> **v2 — summary as of October 6.** The new features have **moved the cost towards the shadows**: with PCF 3 × 3
> (the default of UrbanScape), a frame costs about **3.4 times** a frame without shadows (against 2 times with hard
> shadows). A point light or a spot light with shadows each add about 60% to the time of a frame. The same 4 quick
> wins (D1, D6, D7, A4) now give **−13 to −19% with shadows** and halve the cost of the visible lights. Adding
> **D2a** (the quick part of D2 on the `Color`s), a frame without shadows costs **45% less** and the allocations fall
> from ≈ 145 to ≈ 21 MB per frame. The most profitable new proposals are the **PCF gradient per triangle** (N1),
> **culling, reusing and caching the shadow passes** (N4) and **computing the direction and distance of the lights
> once** (N3). Details in [§10](#10-update-of-october-6-2026-new-features-and-progress).

---

## 1. Method and measurements

### Protocol

- A benchmark without a user interface (`PerfBench`, left in `Claude outputs/perf-audit/`) renders the
  `UrbanScape` scene into an `ImageView` for 40 to 80 frames along the helicopter flight. The average is taken over
  the last frames, once the JIT is warm (the first frames take 700 to 1,000 ms).
- Two configurations: `INTERPOLATE` + specular, with and without shadows (shadow map 2000 × 2000).
- **v2**: `PerfBench2` (same folder) adds the modes `hard`, `pcf`, `point`, `spot`, `sunview` and `sunglow` (§10.2).
- Profiling with **Java Flight Recorder** (`-XX:StartFlightRecording`), CPU and allocation sampling.
- The benchmark computes a **checksum of the image**: the multithreaded prototype produces an image identical to
  the bit to the single-threaded version.

### Where the time goes

Without shadows (1,169 samples):

| Area (inclusive time) | Share |
|---|---:|
| `rasterizeScanLine()` and everything it calls (pixel stage) | 96% |
| `ShadingConsumer.consume()` (per-pixel lighting) | 70% |
| `ZBuffer.test()`, mostly cache misses in `MapView.get()` | 16% |
| `BufferedImage.setRGB()` | 12% |
| `ElementTransform` (vertices and normals) | ≈ 1% |

With shadows (2,371 samples): `generateShadowMap()` **30%**, `shadowFactorAt()` **24%** (most of it in
`Vector4.times(Matrix4)`, which goes through `get(i)` and `set(i)` with a `switch` and a `try`/`catch` for each
coefficient), `setRGB()` 8%.

### Allocations and GC

- About **11.7 GB allocated over 80 frames, i.e. ≈ 145 MB per frame**. Main allocation sites:
  `ColorTools.multColor()` (through `Light.getLightColorAtPoint()`, a new `Color` per pixel and per light) and the
  internal `float[]` of `Color`, 52% together; `DirectColorModel.getDataElements()` (an `int[]` per call of
  `setRGB()`), 19%; `Vector4.V3()`, 17%.
- About **1.5 young GCs per frame**, for 4 ms of pause per frame without shadows and 9 ms with. The pauses remain
  modest: the real cost comes from the allocations themselves and the cache pollution they cause.

### Pipeline statistics, per frame

- 8,912 triangles: 4,110 removed by back-face culling, 212 outside the frustum, **4,590 rasterized**.
- Of the rasterized triangles, about **46% produce no pixel** (`RasterizerStats` counters: 160,693 triangles with at
  least one row, 86,413 with at least one pixel).
- 1.18 million pixels shaded and 236,000 rejected by the Z-test, for 921,600 pixels on screen (part of them sky).
  There is therefore a shading overhead due to overlapping (overdraw).

### Why 8 to 10% of CPU

The rendering runs on a single thread: the `main` thread in `UrbanScape`, the Swing **EDT** in `MovingCamera` and
`FractalLandscape`. A thread that works without stopping occupies one logical core. On a machine with 12 logical
threads, this shows as 8.3%. The engine is already at 100% of what a single thread can give.

---

## 2. Algorithms (programming)

Legend: **Gain** measured (m) or estimated (e) · **Effort** S / M / L · **Readability** + (better), = (neutral), −
(more technical).

### A1. Per-pixel interpolation on objects → incremental steps on `float`s

*`TriangleRasterizer.rasterizeScanLine()`, l. 309-340*

- For each pixel: `Tools.interpolate(Vector4…)` creates **4 objects**, including a `new Vector4()` that is
  immediately overwritten. Then come `.times(wPixel)`, `lerpV3()` (3 objects), another `.times()`, then the same for
  the texture coordinates. In all, **up to 14 allocations per pixel**. The escape analysis of the JIT only removes
  part of them (JFR confirms it).
- `z = 1 / interpolate(1/z1, 1/z2, g)` computes `1/z1` and `1/z2` again for each pixel, whereas `z1` and `z2`
  themselves come from an inversion.
- `xScreen()` and `yScreen()` redo the division `x/w` at each call: 8 divisions per row and 6 or more per triangle,
  for values that do not change.

**Proposal.** This is the classic method of software rasterizers, and it is also very didactic:

- compute **once per vertex** `xs`, `ys`, `invW = 1/w`, and the pre-divided attributes `attr × invW`;
- on a row, compute the **step** of each quantity (`d(invW)/dx`, `d(attr×invW)/dx`), then move forward by
  **additions**: `invW += dInvW; u += du; …`. The local variables are `float`s;
- fill the `Fragment` with `set(float…)`, as already planned.

The code stays readable if the computation of the steps is grouped in a small `EdgeInterpolant` or `ScanlineSetup`
class with a 5-line comment (GPU analogy: the "plane equations" of the attributes).
**Gain** e: large (removes most of the allocations and divisions per pixel) · **Effort** M · **Readability** =.

### A2. Per-pixel lighting: compute once what does not depend on the light

*`ShadingConsumer.consume()` l. 79-101, `Lighting.accumulateContribution()` l. 212-240*

- `material.baseColorAt(fragment)` is called **1 + N times per pixel** (ambient, then each light). With a
  `TexturedMaterial`, the texture is therefore **sampled and filtered several times** and a `Color` is created each
  time (+ `ColorTools.multColors()`).
- `fragment.getNormal().normalize()` is done again for each light, **and changes the fragment in place** (see D5).
- `viewerDirection` (2 allocations) is computed even when specular is off.
- `light.getLightColorAtPoint()` creates a `Color` per pixel through `ColorTools.multColor()`, whereas for a
  directional or ambient light the result is **constant over the whole image**.
- The order in `consume()`: `shadowFactorAt()` (projection and read of the shadow map) is called **before** the
  `dotNL <= 0` test of `accumulateContribution()`. For all the pixels that face away from the light, the shadow map
  is read for nothing (see R10).

**Proposal.** In `consume()`: base color once, normal normalized once, viewing direction only if specular is on,
`dotNL` before the shadow. Light colors pre-multiplied **once per frame** (`Lighting.prepareFrame()`) as three
`float`s. Accumulate in `float`s and produce an RGB `int` only at the end (see D2).
**Gain** m (partial, light color): part of the 84 ms line of the table · **Effort** S · **Readability** +.

### A3. Z-buffer: hidden state and double addressing

*`ZBuffer.test()` / `update()`, l. 107-135*

`test()` remembers `lastX`, `lastY`, `lastBx`, `lastBy` so that `update()` avoids computing two additions again. The
gain is nil, but this shared state makes the class **impossible to use from several threads**.
**Proposal**: `test()` and `update()` without state. The 1D row-by-row access is handled in D7.
**Gain** ≈ 0 single-threaded, a prerequisite of §6 · **Effort** S · **Readability** +.

### A4. Matrix × vector product

*`Matrix4.times(Vector4)` l. 530 → `Vector4.times(Matrix4)` l. 615*

The double loop uses `get(i)` and `set(i, …)` (a `switch` and a checked exception). It is **24% of the time with
shadows**, because `shadowFactorAt()` projects each pixel into the space of the light.
**Proposal**: 16 multiply-adds written out (4 readable lines), and `Matrix4` stored in a `float[16]` or 16 fields
rather than a `float[][]` (5 objects). A `transform(Vector4 in, Vector4 out)` without allocation can serve in the
critical loops.
**Gain** m (included in the 84 ms line) · **Effort** S · **Readability** + (the unrolled code is more readable than
the current loop).

### A5. Per-triangle details

- `sortByScreenY()`: `Arrays.sort` with a `Comparator` created by a lambda and boxed `Float`s, to sort **3
  elements**. Three comparisons and swaps are enough. **Effort** S.
- 3 `RasterVertex`, an array, a `Material` and a consumer are allocated **per triangle**: about 4,600 per frame,
  cheap, but to be reviewed as part of D3.
- `ElementTransform.computeNormalMatrix()` tests `M3 · M3ᵀ` **by exact float equality** with the identity. A real
  rotation almost always fails this test, so a 4 × 4 inversion is computed for each element at each frame. It must
  compare with a tolerance and cache the result with the transformation (D8). **Gain** small (≈ 1%).

### A6. Textures

*`Texture`, l. 70-207*

- `int[width][height]` storage (by columns): a bilinear sample reads two different arrays.
- `getInterpolatedColor()` returns a new `Color`, and `TexturedMaterial` creates a second one for the tint.
- Loading calls `img.getRGB(w, h)` **pixel by pixel**, i.e. 2.2 million calls for the 1706 × 1279 texture of
  `MovingCamera`. It is not a cost per frame, but the start-up is slow.
- **`Texture` holds a shared `RGBAccumulator`**: two threads that sample the same texture step on each other (see
  §6).

**Proposal**: an `int[]` stored row by row, bulk read with `img.getRGB(0, 0, w, h, buf, 0, w)`, and a sampling that
returns an ARGB `int` or writes three `float`s into an accumulator **provided by the caller**.
**Effort** S · **Readability** =.

### A7. Constant FLAT shading and a true Gouraud mode

- In `FLAT` without specular, shadows or texture, the color is **the same for the whole triangle**: it can be
  computed once and the triangle filled.
- `INTERPOLATE` is described as "Gouraud" in the code, but it is actually **Phong shading** (interpolated normal,
  lighting computed at each pixel). A true `GOURAUD` mode (lighting at the 3 vertices, interpolated colors) would
  cost **a fraction** of the price. It is also very interesting from a teaching point of view: comparing Gouraud and
  Phong on the same scene is a classic of computer graphics courses.
**Gain** e: large for these modes · **Effort** M · **Readability** +.

---

## 3. Design

### D1. Static counters in the vector constructors — **priority 1**

*`Vector3.count()` l. 182, `Vector4.count()` l. 200*

Each `new Vector3` and `new Vector4` increments `nb_vectors` and `nb_to_display`, two public static fields. This has
three consequences:

- **a global side effect in a math class**;
- a **data race** as soon as there are several threads (the counters become wrong);
- above all, **the cache line of these fields bounces between the cores** at each allocation. Measured: with 2
  threads, **118 to 150 ms against 91 ms with 1 thread**. After making the counter depend on `Tracer.object`:
  **53 ms**.

**Proposal**: remove these counters. If the diagnosis is useful, put it behind `if (Tracer.object)` or use a
`LongAdder`. This is exactly the "Tracer has become a burden" case that was suspected, except that it is not `Tracer`
itself that costs, but this instrumentation, which runs even when tracing is off. **Effort** S · **Readability** +.

> **v2 — still there, and more costly.** The new features create even more vectors per pixel (PCF: 2 more
> projections, cross products and normalizations; point and spot lights: a `new Vector3` at each computation of a
> direction or a distance). Each of these allocations writes into the two shared counters. It is still the **number
> one prerequisite of multithreading**.

### D2. `java.awt.Color` as the internal color type

`Color` is immutable (each operation allocates), `getRGBColorComponents(null)` allocates a `float[]`, and the
`Color(float, float, float)` constructor checks its bounds at each call. This is **more than half of the
allocations** measured. It also ties the model to AWT.
**Proposal**: in the pipeline, colors travel as three `float`s (accumulator) or as a packed RGB `int`. `Color` stays
the type of the **public API** (`Element.setColor()`, lights), converted **once** on input (preparation of the frame
or of the material). **Effort** M · **Readability** = (the public signatures do not change).

> **v2 — D2 is split into two steps.**
>
> **D2a — quick win (phase 1).** Two local changes that touch no public API:
> - the **color of the lights whose intensity does not depend on the point** (ambient, directional) is computed once
>   instead of a `ColorTools.multColor()` per pixel and per light. In the final version, this preparation is done at
>   the start of the frame (`Lighting.prepareFrame()`) rather than in a lazy cache, to respect "Refresh, don't
>   cache" and stay safe with several threads;
> - `RGBAccumulator.toRGB()` produces an RGB `int` directly, written by a new `GUIView.drawPixel(x, y, int rgb)` into
>   the `int[]` of the image (completes D6). The conversion rounds like `new Color(float, float, float)`, hence an
>   image identical to the bit.
>
> **Measured** (§10.3): the allocations fall from **134 to 21 MB per frame**, there are almost 3 times fewer GCs,
> and a frame without shadows costs **≈ 30% less** than the frame with only the 4 quick wins.
> **Effort** S · **Readability** =.
>
> **D2b — refactoring (phase 2).** `Material`, `Texture` and `Light` provide their components as `float`s (in an
> accumulator provided by the caller) instead of returning a `Color`. This is needed for the **textures** (a bilinear
> sample produces one or two `Color`s today) and for the **point and spot lights**, whose color changes at each pixel
> with the attenuation, which no cache can avoid. **Effort** M.

### D3. Materials and consumers created for each triangle

*`RenderEngine.rasterizeShadedTriangle()` l. 580-583, `rasterizeUnlitTriangle()` l. 524-527*

The cost is moderate, but the design is a problem for multithreading: a consumer holds a mutable accumulator and
must belong to a single thread. **Proposal**: consumers **reused, one per thread** (`consumer.setMaterial(m)`), and
the material resolved **per element** (or cached per combination of element × texture × color).

### D4. The state of the pipeline is stored in the model of the scene

`Vertex.prj_position`, `wld_position`, `wld_normal`, `prj_normal` and `Triangle.wld_normal` are **written by each
pass**. The shadow pass, for example, overwrites `prj_position` with the coordinates in the space of the light before
the main pass writes them again. Consequences:

- the passes must be strictly sequential (impossible to compute two shadow maps in parallel, or a shadow map during
  the main pass);
- each `Vertex` carries 7 references to separate objects (unfriendly to the cache);
- the same object represents both "the scene" and "the result of a pass", which is confusing when discovering the
  code.

**Proposal**: each pass owns its **vertex output buffers** (`float[]`s per element, for example `xs, ys, invW, …`).
This is the direct analogue of the output of the *vertex shader* on a GPU. `Vertex` becomes again a simple
description of the scene. **Effort** L · **Readability** + (separates the model from the pipeline). It is the
condition for parallelizing the geometry and the shadows.

### D5. Half-immutable math API

`plus()`, `minus()` and `times()` return new objects, but **`Vector3.normalize()` changes the object and returns
itself**. Current consequences: `Lighting` normalizes the normal of the `Fragment` in place, and
`shadowFactorAt(worldPosition, normal)` normalizes **the normal of the caller** in place. It is both a readability
trap and a risk of data race.
**Proposal**: an explicit convention, `normalize()` that returns a new vector and `normalizeInPlace()` (or
`…Equals()` like `timesEquals()`) for the rare critical paths. **Effort** S to M · **Readability** +.

### D6. Framebuffer: `setRGB()` and a new image per frame

*`ImageView.drawPixel()` l. 200, `initBack()` l. 106*

`BufferedImage.setRGB()` goes through the `ColorModel` and allocates an `int[]` at each call (19% of the
allocations). `initBack()` also allocates a new 3.7 MB image per frame (BACKLOG §6).
**Proposal**: get the underlying `int[]` once (`((DataBufferInt) img.getRaster().getDataBuffer()).getData()`), write
into it directly, clear it with `Arrays.fill`, and keep a pool of 2 or 3 images (triple buffering) as planned in the
backlog. Add `GUIView.drawPixel(int x, int y, int rgb)` next to the `Color` version.
**Gain** m: −11 ms · **Effort** S · **Readability** =.

> **v2.** The new `ImageView.addPixel()` method (visible lights) does avoid the allocation of a `Color`, but it does
> a `getRGB()` then a `setRGB()` per pixel. In a view turned towards the sun with the light shafts, these two calls
> are **41% of the time of the effect**, which covers almost the whole screen. Writing into the `int[]` fixes both
> methods at once (see N6 in §10).

### D7. `MapView` as a `float[][]` by columns → `FloatMap` as a 1D `float[]` by rows

*`MapView` l. 36, used by `ZBuffer` and the shadow maps*

The rasterizer walks the pixels row by row (x varies) whereas the storage is `map[x][y]`: each pixel accesses a
different Java array. The `clear()` of the Z-buffer also walks 1,281 small arrays.
**Proposal**: this is the opportunity to do **BACKLOG §2** (a `FloatMap` class outside of the `view` package) with a
`float[width * height]` and an index `y * width + x`, a `clear` with `Arrays.fill`, and the bilinear sampling
factored out. `MapView` becomes a display adapter.
**Gain** m: −5 ms without shadows, −28 ms with · **Effort** S to M · **Readability** +.

### D8. Everything is computed again at each frame

`world.worldProject()`, normal matrices, `initShadowing()` and `generateShadowMap()` (with a new 2001 × 2001
`ZBuffer` at each frame), light colors… are computed again even when nothing has moved.

DESIGN §13 sets the rule "*Refresh, don't cache*", which answers real bugs of stale caches. **Do not work around it
with improvised caches.** Proposal: explicit **version counters** (`Element`, `Light`, `World` increment a number at
each change; a derived result remembers the version from which it was computed). The rule becomes "refresh if the
version has changed".
**Gain** m: shadow map kept, 214 → ≈ 137 ms · **Effort** M · **Readability** = if done uniformly.

### D9. Geometry and rasterization interleaved → two stages

Today, `render(Element)` transforms an element and then rasterizes its triangles right away, recursively. Several
techniques of §4 and §6 (front-to-back sorting, Z-prepass, distribution by tiles, multithreading) need a **two-stage**
pipeline:

1. **Geometry stage**: transformation, culling (frustum, back-face, small triangles), clipping, then production of a
   **list of ready triangles** (screen coordinates, `1/w`, attributes, material);
2. **Rasterization stage**: walking this list (sequentially, by bands or by tiles).

This is the "vertex processing → primitive assembly → rasterization" split of a GPU, which makes it a **teaching
gain** as much as a performance gain. **Effort** M to L · **Readability** +.

### D10. Rendering on the EDT in two demos

`MovingCamera` and `FractalLandscape` call `renderer.render()` from Swing `Timer`s and mouse listeners, hence **on
the thread of the user interface**. During a rendering of 100 to 300 ms, the window does not respond and the events
pile up. `UrbanScape` already renders in its own loop.
**Proposal**: a dedicated rendering loop (or a `SwingWorker`) that only renders the **last** camera state requested.
**Effort** S · **Readability** +.

> **v2.** `SceneViewer` (test code) shows the right model: a dedicated rendering thread that reads the last state
> requested. `MovingCamera` and `FractalLandscape` still render on the EDT: following that model is enough.

---

## 4. Rendering techniques: removing work upstream

The idea is to discard as early as possible what will not be visible. From the cheapest to the most costly:
**element → triangle → block of pixels → pixel**. Today Aventura only does back-face culling (and only for the
`isClosed` elements) and a frustum test per triangle.

### R1. Hierarchical frustum culling by `Element` (bounding volumes)

Compute at `build()` a **bounding sphere** (or an AABB) per element, which also covers its sub-elements. At each
frame, test it against the 6 planes of the frustum. These planes can be extracted directly from the `P · V` matrix
(the Gribb and Hartmann method, about ten lines).

- **Entirely outside**: skip the transformation of the vertices, all the triangles and all the sub-elements.
- **Entirely inside**: skip the frustum test and the clipping of each of its triangles.

In UrbanScape (the whole city is on screen), the gain is small. It is **large** as soon as the camera only sees part
of the scene (zooming in `FractalLandscape`, `MovingCamera`). The same method applies to the shadow pass, in the space
of the light. **Effort** M · **Readability** +.

### R2. Frustum test by outcodes, and near-plane clipping (correctness and performance)

*`Triangle.isInViewFrustum()` l. 344, `Vertex.isInViewFrustum()` l. 203*

The current test ("at least one vertex in the frustum") is **wrong both ways**:

- it **rejects** a large triangle whose 3 vertices are off screen but which crosses it (a ground seen from close) →
  holes;
- it **accepts** a triangle with a vertex behind the camera (`w < 0`). The division by `w` then produces absurd
  screen coordinates (the comment of `ScreenLineRenderer` admits it: "no clipping").

Moreover, it does 3 divisions per vertex, **redone for each triangle** that shares this vertex (about 6 times).

**Proposal**: a 6-bit **outcode** per vertex, computed **once** in clip space and without division (`x < -w`,
`x > w`, …):

- `oc1 & oc2 & oc3 != 0` → entirely on the same side of one plane: **reject**;
- `oc1 | oc2 | oc3 == 0` → entirely inside: **no clipping**;
- otherwise, clip **only against the near plane** (Sutherland-Hodgman, 0 to 2 triangles out). The other edges are
  already handled by the rasterizer, which limits rows and columns to the screen (the "guard band" principle).

**Effort** M · **Readability** + (a classic, well-documented algorithm).

> **v2 — partly done.** `NearPlaneClipper` (October 2026) now clips the triangles against the near plane in
> perspective, in the main pass and in the shadow maps of the spot and point lights: the triangles behind the camera
> no longer produce absurd coordinates. Still to do: the **frustum test by outcodes** (`Triangle.isInViewFrustum()` is
> unchanged, so the large triangles that cross the screen with their 3 vertices outside are still rejected), and a
> **fast path** in the clipper, which today allocates 5 to 7 objects for each triangle, even one entirely in front of
> the near plane (see N5 in §10).
>
> **Done (October 2026): the frustum test by outcodes.** `Triangle.isInViewFrustum()` rejects a triangle only if its
> 3 vertices are on the same side of the same plane of the frustum (`oc1 & oc2 & oc3 != 0`), in clip space and
> without division. The large triangles that cross the screen are no longer rejected: the sea of `FractalLandscape`
> (triangles of one unit) disappeared entirely when the flying camera passed just above it. The outcodes are still
> computed again for each triangle (not once per vertex), and the "entirely inside" case is not yet used to skip the
> clipper.

### R3. Back-face culling in screen space

*`RenderEngine.isBackFace()` l. 606*

Computing the **sign of the area of the projected triangle** (2D cross product of the edges in screen coordinates)
is enough. It is the test of GPUs:

- exact, and **independent of the kind of projection** (no more `switch FRUSTUM / ORTHOGRAPHIC`);
- no normal to transform (today, `transformNormal()` and `projectNormal()` are done only for this test in
  `INTERPOLATE` mode);
- a null area removes the **degenerate** triangles along the way.

For open elements, the `Triangle.rectoVerso` flag, already there but unused (BACKLOG §5), would say "do not cull".
⚠ **Prerequisite**: this test relies on **the order of the vertices (winding)**. The winding of `Sphere` must be
fixed first (BACKLOG §5) and the other shapes checked with `ElementContractChecker`. **Effort** S (plus the fix of
`Sphere`) · **Readability** +.

### R4. Small triangles

46% of the rasterized triangles cover no pixel center. Yet each one pays today for the sort, 3 `RasterVertex`, a
`Material`, a consumer and the preparation of the rows.
**Proposal**: as soon as the screen coordinates are known, test whether the bounding box contains at least one pixel
center (`ceil(minX) < ceil(maxX)` and likewise in y). If not, the triangle is rejected before any preparation. Later,
as an option: a **level of detail (LOD)** that reduces the number of segments of far spheres and cylinders.
**Effort** S · **Readability** =.

### R5. Front-to-back order

The Z-test happens **before** shading (a good design point). If the near elements are drawn first, the pixels hidden
behind them are rejected **before** being shaded. Sorting the ~900 elements by the distance of their bounding sphere
to the camera is enough (negligible cost). With 1.18 million pixels shaded for at most 0.92 million visible, **up to
20 to 30% of the shading** can be saved. **Effort** S (after R1) · **Readability** =.

### R6. Z-prepass or "visibility buffer"

This is the radical version of R5: **each visible pixel is shaded only once**.

1. Pass 1: **depth-only** rasterization (the `DepthOnlyConsumer` already exists) of the whole scene. Variant: also
   write the **identifier of the triangle** into an `int[]` buffer.
2. Pass 2: shade only the pixels whose depth equals that of the Z-buffer. With the variant, simply walk the screen
   and shade each pixel from its triangle.

It pays off when shading is expensive (shadows, several lights, textures), which is the case here. The "visibility
buffer" variant is moreover **perfectly parallelizable** (each pixel is independent). GPU analogue: deferred shading.
**Effort** M to L · **Readability** = to + (two clearly named passes).

### R7. Occlusion culling (Hi-Z)

Keep a **coarse Z-buffer** (maximum depth per tile of 8 × 8 pixels). Before processing an element, compare its box on
screen and its minimum depth with the Hi-Z: if everything it covers is already nearer, skip the whole element.
UrbanScape suits it perfectly (buildings hidden behind other buildings). Advanced, to be done after R1 and R5.
**Effort** L.

### R8. What about the stencil?

The **stencil buffer** is an integer mask per pixel that **limits where one draws**: mirrors, portals, shadow volumes,
outlines, decals. It **does not determine visibility** by itself. For opaque geometry, the tools for early removal are
those of R1 to R7. In Aventura, the stencil is therefore not a performance lever. It would become useful for a feature
like shadow volumes or drawing outlines in `MONOCHROME`.
Its cousin, the **scissor test** (restricting drawing to a rectangle), is on the other hand exactly what
multithreading by tiles needs (§6).

### R9. Shadow pass

- **Keep the shadow map** as long as the light and the scene do not change (D8). With `SHADOWING_BOX_WORLD`, it does
  not depend on the camera. Measured: **214 → ≈ 137 ms**.
- **Reuse the `ZBuffer`** instead of allocating one per frame (16 MB for 2000 × 2000).
- Cull by element in the space of the light (R1), and **cull the front faces** (draw only the back faces). It is a
  known technique that divides the number of triangles and reduces shadow acne.
- **Resolution**: 2000² = 4 million texels rasterized per frame. `SHADOWING_BOX_VIEWFRUSTUM`, a tighter box, gives
  the same sharpness with fewer texels.
- Quality note: `shadowFactorAt()` interpolates the **depths** bilinearly, which is not PCF (which interpolates the
  **results of the comparisons**, BACKLOG §3).

> **v2.** PCF is now implemented correctly (weighting of the comparisons, `ShadowFilter.PCF_3X3`). What is at stake
> in this section is **multiplied**: there are now up to three kinds of shadow maps (orthographic, perspective, cube
> map with 6 faces), and each `PointLight` can cost 6 shadow passes per frame. None of the proposals above is done
> yet. See N4 in §10.

### R10. Leave the lighting early

Test `dotNL <= 0` **before** `shadowFactorAt()`: a pixel that faces away from the light is not lit by it, whether it
is in the shadow or not. This saves a projection and a bilinear read of the shadow map for about half of the pixels.
**Effort** S · **Readability** +.

> **v2 — measured, and estimate corrected.** The prototype (test of `dotNL`, then of the intensity of the light,
> before `shadowFactorAt()`, image identical to the bit) gives **no measurable gain on UrbanScape**: back-face culling
> has already removed the faces turned away, and the sun lights most of the visible faces. "Half of the pixels" was
> too optimistic for this scene. R10 remains right and cheap to write, but its gain depends on the scene: it becomes
> large for a **narrow spot light** or a **point light of short range**, most of whose pixels are outside the cone or
> out of range (see N2 in §10).

---

## 5. The three dimensions: memory, CPU, I/O

### Memory

| Topic | Finding | Action |
|---|---|---|
| Allocation rate | ≈ 145 MB per frame, 1.5 GC per frame | A1, A2, D2, D6 |
| Data layout | `float[][]` and `int[][]` by columns, traversed by rows | D7, A6 |
| Large allocations per frame | shadow map 16 MB, image 3.7 MB | R9, D6 |
| Pointer chasing | `Vertex` → 7 `Vector` objects; `Matrix4` → 5 objects | D4, A4 |
| Object headers | a `Vector4` weighs 32 bytes for 16 bytes of data | acceptable in the model; to be avoided in the critical loops |

JVM options: setting `-Xms` = `-Xmx` avoids growing the heap at start-up, and `-XX:+UseParallelGC` favors throughput.
This is secondary tuning: **the real solution is to allocate less**. The escape analysis of the JIT helps, but is not
enough to make these allocations free (JFR showed it).

### CPU

- **A single core used** (§1). The main lever is §6.
- Hot spots: shading (A2, R10), Z-test (D7), writing the pixels (D6), shadow math (A4), generation of the shadow map
  (R9).
- **JIT warm-up**: the first 2 or 3 frames are 3 to 7 times slower. It is normal, but must be taken into account in
  any measurement.
- The FPS computation in `RenderEngine.render()` uses `currentTimeMillis()`. `System.nanoTime()` is more precise, and
  a **breakdown by phase** would be more useful (§7).

### I/O

**There is no input/output in the rendering loop**, so it is not a bottleneck. A few secondary points:

- `Tracer.stats` prints a line at each frame (`System.out` is synchronized, and the Windows console is slow). It is
  negligible, but an average printed every N frames would be better. **Since then: off by default**
  (`Tracer.stats = false`); the demos print their own statistics (`FrameTimeStatistics`).
- Loading the textures: pixel by pixel reads (A6) and paths relative to the current directory (roadmap: loading from
  the classpath). This slows down the start-up, not the rendering.
- Tracing to a file: `Tracer.output()` writes line by line into an unbuffered `FileOutputStream`. A `BufferedWriter`
  is enough.
- Saving images (`ImageView.saveImage()`): fine. For a future image sequence (BACKLOG §6), write the files **in a
  separate thread** so as not to block the rendering.

---

## 6. Multithreading

### Why not rasterize "one triangle per thread"

On a GPU, thousands of triangles are rasterized in parallel, but **the writes into the Z-buffer and the framebuffer go
through hardware units (ROPs) that guarantee order and atomicity**. On a CPU, two threads that rasterize two
overlapping triangles fight over the same pixels. One would then need:

- a lock or an atomic operation **per pixel** (far too expensive);
- and to accept a **non-deterministic** image (at equal depth, the winner depends on chance), which would break the
  tests.

### The classic solution: split the screen

Each thread **owns an area of the screen** (its share of the Z-buffer and of the framebuffer) and processes there
**all the triangles that touch it, in the original order**. This organization is called *sort-middle*. It brings:

- **no synchronization per pixel**;
- an image **identical to the bit** to the single-threaded version (checked by checksum in the prototype), so the
  existing tests remain valid.

Two levels:

1. **Bands of rows** (the simplest with a scanline rasterizer). Passing a range `[rowMin, rowMax)` to
   `TriangleRasterizer`, which already limits the rows to the screen, is enough. This is what the prototype did:
   **about 40 lines of code**.
2. **Tiles** (for example 64 × 64) with a **binning** step: the bounding box of each triangle determines the list of
   tiles concerned. The data of a tile fit in the L2 cache, the load is better balanced, and it is the model of mobile
   GPUs. To be done after D9.

### Balancing the load

The prototype shows it clearly:

| Configuration (same code, 2 cores) | Time per frame |
|---|---:|
| 1 thread | ≈ 95 ms |
| 2 threads, **2 bands** | ≈ 100 ms (no gain: the top band is mostly sky) |
| 2 threads, **8 bands** | **≈ 53 ms** (×1.8) |
| 2 threads, 8 bands, **with the static counters of D1** | 118 to 150 ms (slower than a single thread) |

Rule: **more bands or tiles than threads** (4 to 8 times more), distributed **dynamically** by the queue of the pool,
so that a thread that has finished takes the next area.

### Controlling the number of threads (the Factory idea)

- **A fixed-size pool, created only once** (not at each frame), owned by the `RenderEngine` and whose size is set in
  `RenderContext` (`setRenderThreads(n)`). Reasonable default: `Runtime.getRuntime().availableProcessors() - 1`, to
  leave one core to the user interface. Add a `close()` or `shutdown()` method.
  *Done (MT1)*: `RENDER_THREADS_AUTO` (default) = **one thread per processor**, since the rendering thread does not
  compute during the rasterization (it waits for the end of `invokeAll`); `setRenderThreads(1)` gives back the
  single-threaded behavior. `RenderEngine.close()` stops the threads (optional: they are daemons). The workers are
  objects created once (`BandRasterizer.Worker`), which take the free bands one by one; their `TriangleRasterizer` is
  created in their own thread. The consumers are still created per triangle (D3), but only for the triangles that
  touch the band (`TriangleRasterizer.coversNoRow()`).
- A **`ThreadFactory`** to name the threads (`aventura-raster-3`, readable in a profiler) and make them daemons.
- A **factory of workers** (`RasterWorker`): each worker owns **its** `TriangleRasterizer`, **its** `Fragment`,
  **its** consumers and **its** statistics, merged at the end of the frame. This is where the Factory pattern makes
  full sense: it builds an execution context per thread, not the threads themselves.
- `ExecutorService.invokeAll(tasks)` serves as a **barrier** at the end of the frame: simple and readable.
  `ForkJoinPool` brings nothing more here.
- To be avoided: **virtual threads** (they are made for blocking I/O, not for computation) and `parallelStream()` in
  the engine (less control and less didactic).

### Prerequisites: the shared state found in the code

| Shared mutable state | Where | Fix |
|---|---|---|
| Static counters of the vectors | `Vector3`, `Vector4` | D1 |
| `lastX/lastY/lastBx/lastBy` | `ZBuffer` | A3 |
| Internal `RGBAccumulator` | `Texture` | A6: accumulator provided by the caller |
| Counters and `Fragment` | `TriangleRasterizer` | one instance per worker |
| `RGBAccumulator` | `ShadingConsumer` | one consumer per worker (D3) |
| `RasterizerStats` | `RenderEngine` | statistics per worker, merged |
| In-place `normalize()` on shared vectors | `Lighting`, `ShadowingLight` | D5 |
| `prj_position` and others in `Vertex` | geometry and shadow passes | barrier between the stages (D9), then D4 |

⚠ **False sharing**: the objects of a worker must be **created in the thread of the worker** (so that they do not
sit next to those of other workers in memory), and the counters must be local variables added to the total at the
end of the task.

### Other possible parallelizations, by order of profitability

1. **Pixel stage by bands or tiles**, described above: about 95% of the time.
2. **Shading of a visibility buffer** (R6): each pixel is independent.
3. **Shadow maps**: one task per light (after D4), and each shadow map itself by bands in the space of the light.
4. **Geometry stage**: one element per task. Small gain here (≈ 1% of the time), but useful for very detailed scenes.
5. **Overlap between frames**: render frame N+1 while the user interface displays frame N (with the image pool of
   D6).

> **v2.** Two new easy sources of parallelism:
> - the **6 faces of the cube map** of a `PointLight` are 6 independent renderings, as are the shadow maps of several
>   lights. Each pass must first have its own vertex outputs (D4), since they all write into `Vertex.prj_position`
>   today;
> - the **post-processing of the visible lights** (`LightGlowRenderer`: halo, light shafts) works pixel by pixel on
>   the finished image, and can therefore be split into bands without any difficulty.

On a machine with 8 to 12 logical threads, Amdahl's law gives a maximum of about ×6 to ×8 for 95% of parallel code.
In practice, memory bandwidth, hyperthreading and load imbalance bring this down to **×3 to ×5**. The engine would
then use 60 to 90% of the CPU instead of 8%.

> **v3.** With the pixel stages parallel (MT1 to MT3), the next limit is what remains sequential: the geometry
> stages, `worldProject()`, the display outside the engine. See [§12](#12-update-of-november-26-2026-phase-2-what-remains-sequential).

---

## 7. Tracer and instrumentation

- **`Tracer` itself costs nothing measurable**: the `if (Tracer.info)` tests are well-predicted reads of static
  fields, and no function of `Tracer` appears in the profiles. Making them `static final` would let the compiler
  remove them, but the configuration at run time would be lost. **It is not necessary.**
- **The real cost tied to tracing is D1** (the counters of `Vector3` and `Vector4`, which run even when
  `Tracer.object` is off).
- Rather to be added: a **timing per phase** in `RenderEngine` (clearing, shadows, geometry, rasterization,
  presentation) with `System.nanoTime()`, exposed in `renderStats()` with a moving average. This permanently answers,
  without a profiler, the question "where does the time go?".
- Make the **benchmark** a tool of the project: `PerfBench` (in `src/test/java/…/demo`, as a program and not a unit
  test), with the checksum of the image as a visual non-regression test for each optimization.

---

## 8. Proposed roadmap

### Phase 0 — Measure (½ day)

- `PerfBench` and the checksum of the image in the repository; timing by phase (§7).
- Measure the reference on **the author's** laptop (without and with shadows, 1 thread).

### Phase 1 — Quick wins, low risk, equal or better readability

| # | Action | Expected gain |
|---|---|---|
| D1 | Remove the static counters of `Vector3` and `Vector4` | prerequisite of multithreading |
| D7 | `FloatMap` as a 1D `float[]` row by row (does BACKLOG §2) | −5 to −28 ms (m) |
| D6 | Direct writes into the `int[]` of the image, image pool (BACKLOG §6) | −11 ms (m) |
| D2a | **v2**: constant light colors prepared once, `RGBAccumulator` → `int` written directly | −30% without shadows, allocations ÷6 (m, §10.3) |
| A4 | Unrolled `Matrix4`, `float[16]` | included in −14 ms (m) |
| A2, R10 | Lighting: common values computed once, `dotNL` before the shadow | large with shadows or textures (e) |
| R9 | Reuse the Z-buffer of the shadows | small to medium |
| R4 | Reject small triangles early | medium (e) |
| A6 | Textures: `int[]` by rows, bulk loading, no shared state | medium with textures (e) |
| A3, A5 | Stateless `ZBuffer`, sort of 3 vertices | small |
| D10 | No more rendering on the EDT in the demos | responsiveness |

### Phase 2 — Restructure the pipeline (conditions for what follows)

- **D9**: geometry stage → list of ready triangles → rasterization stage.
- **R2**: outcodes and near-plane clipping (also fixes the triangles behind the camera).
- **R3**: back-face culling in screen space (after fixing the winding of `Sphere`).
- **R1**, **R5**: bounding volumes, culling by element, front-to-back sorting.
- **A1**: incremental interpolation on `float`s.
- **D2b**: materials, textures and lights as `float`s (D2a is in phase 1). **D8**: version counters (keeping the
  shadow map).

### Phase 3 — Multithreading

- Thread pool, factory of workers, bands (4 to 8 times more than threads), merged statistics.
- Then tiles with binning, and shadow maps in parallel (with **D4**).

### Phase 4 — Optional and advanced (to be chosen by teaching interest)

- **R6** Z-prepass or visibility buffer · **R7** Hi-Z occlusion · **A7** true Gouraud mode · LOD · mipmapping.

Each step is validated with: identical image (checksum), then measured gain. If a step brings no measurable gain, it
is not kept.

---

## 9. What not to do

To stay in the spirit of a teaching project:

- **Vector API** (`jdk.incubator.vector`, SIMD): still incubating, verbose, and the gain is uncertain on this code.
- `Unsafe`, off-heap memory, fixed-point arithmetic, generalized manual unrolling.
- Turning **the whole** scene into structures of arrays (`float[]` everywhere). It is better to keep the objects
  (`Element`, `Vertex`, `Triangle`) as the **description of the scene**, and to use arrays of primitives only in the
  structures traversed at each pixel (Z-buffer, framebuffer, textures, outputs of the passes).
- Improvised caches that work around the "Refresh, don't cache" rule: use versions (D8).

Guiding principle: **optimize the data paths, not the concepts**. `Fragment`, `FragmentConsumer`, `Material`,
`TriangleRasterizer` and the two contexts keep their role. What changes is *how* the data flow inside. Each
optimization deserves a comment that gives its GPU equivalent: that is where the project stays didactic.

---

## 10. Update of October 6, 2026: new features and progress

### 10.1 What changed in the code

Between version 1 (commit `6d7a016`) and the current `Refactoring` branch (`d3a2a47`), the commits about the engine
are:

- **perspective shadow maps** (generalized `ShadowingLight`) and **shadows of the spot lights**;
- **shadows of the point lights**: a cube map with 6 faces (`PointLight`, 512 × 512 per face by default). Only the
  faces that see part of the scene are computed;
- **near-plane clipping** (`NearPlaneClipper`), in the main pass and in the perspective shadow passes;
- **PCF 3 × 3 soft shadows** (`ShadowFilter`), with a *receiver plane depth bias* computed at each pixel. They are on
  by default for the sun of `UrbanScape`;
- **visible lights** (`LightGlowRenderer`): halo of the point and spot lights, disc and halo of the sun, lens flare,
  *light shafts*, as a post-process on the finished image;
- the rest of the diff (about 100 files) is the license header and the documentation.

The **per-pixel path of the main pass has not changed**: without shadows, the image produced is identical to the bit
to that of version 1, and the time is the same. None of the proposals of phases 1 to 3 has been done yet, except the
near-plane clipping (R2, in part).

### 10.2 Measurements

Same benchmark as in §1 (`UrbanScape`, 1280 × 720, 8,912 triangles, `INTERPOLATE` + specular). Medians of 2 or 3
alternated runs, 40 frames, on 2 cores.

⚠ The test machine was **about 1.8 times slower** than on September 30, and noisier (±10%): the old code, measured
again in the same session, gives 214 ms without shadows and 413 ms with. Compare the columns **with each other**,
not with the figures of §0.

| Configuration | Time per frame | Compared with "without shadows" |
|---|---:|---:|
| Version 1, without shadows | 214 ms | — |
| **Current, without shadows** | **≈ 210 ms** | ×1 (identical image) |
| Version 1, sun with shadows | 413 ms | ×1.9 |
| Current, sun, hard shadows (`HARD`) | ≈ 430 ms | ×2.1 |
| **Current, sun, PCF 3 × 3** (default of `UrbanScape`) | **≈ 720 ms** | **×3.4** (+65% compared with `HARD`) |
| Current, sun `HARD` + **1 point light** with shadows | ≈ 680 ms | ×3.2 (+250 ms) |
| Current, sun `HARD` + **1 spot light** with shadows | ≈ 680 ms | ×3.2 (+250 ms) |
| View turned towards the sun, without visible lights | ≈ 40 ms | — |
| The same view, with halo, disc and light shafts | ≈ 120 ms | +80 ms for the effect |

Where the time goes (JFR, share of the samples):

| Configuration | Shadow passes | `shadowFactorAt()` | Other notable points |
|---|---:|---:|---|
| Sun `HARD` | ≈ 30% | 34% | writing of the shadow map (`MapView.set`) at the top of the most expensive methods |
| Sun PCF 3 × 3 | ≈ 15-20% (same absolute cost as `HARD`) | **59%** | of which **23%** for the 2 extra projections per pixel (`posInLightSpace1`) |
| + point light | ≈ 26% | 43% | up to 6 shadow passes; direction and distance of the light computed several times per pixel |
| + spot light | ≈ 36% | 42% | the shadow map is read even for the pixels outside the cone |
| Visible lights | — | — | light shafts **73%** of the effect, `addPixel()` (through `getRGB`/`setRGB`) **41%** |

### 10.3 Quick wins, measured again on the current code

Prototype on a throwaway copy: **D1** (counters), **D7** (`MapView` as a 1D `float[]`), **D6** (direct `int[]`,
including in `addPixel()`) and **A4** (unrolled `Matrix4 × Vector4`). The images are identical to the bit in the six
configurations.

| Configuration | Current | With the 4 quick wins | Gain |
|---|---:|---:|---:|
| Without shadows | ≈ 218 ms | ≈ 207 ms | −5% (within the noise that day; −26% on September 30 with A2 in addition) |
| Sun `HARD` | ≈ 427 ms | ≈ 373 ms | −13% |
| Sun PCF 3 × 3 | ≈ 772 ms | ≈ 625 ms | **−19%** |
| + point light | ≈ 665 ms | ≈ 557 ms | −16% |
| + spot light | ≈ 695 ms | ≈ 650 ms | −7% (very noisy measurement) |
| View towards the sun with visible lights | ≈ 118 ms | ≈ 79 ms | −33% (the cost of the effect is halved) |

These four changes are small, with no effect on readability, and they benefit the new features more than the old
main pass.

#### With D2a in addition (light colors prepared once, accumulator → direct `int`)

Measured in a second alternated series (same day, same caveats about the noise), images still identical to the bit:

| Configuration | Current | 4 quick wins | **+ D2a** | Total gain |
|---|---:|---:|---:|---:|
| Without shadows | ≈ 222 ms | ≈ 173 ms | **≈ 122 ms** | **−45%** |
| Sun PCF 3 × 3 | ≈ 659 ms | ≈ 581 ms | ≈ 557 ms | −15% |
| + point light | ≈ 677 ms | ≈ 616 ms | ≈ 532 ms | −21% |

| Allocations, without shadows | 4 quick wins | + D2a |
|---|---:|---:|
| Memory allocated per frame | ≈ 134 MB | **≈ 21 MB** |
| of which `Color`s and their `float[]`s | ≈ 87 MB | ≈ 1 MB |
| Number of GCs (40 frames) | 47 | 17 |

Reading:

- in this series, the 4 quick wins give −22% without shadows. The −5% of the previous table was due to the noise of
  the machine;
- **D2a is the most profitable quick win of all** on the main pass: it removes almost all the remaining allocations;
- with PCF, the gain of D2a is smaller (−4%): the time there is dominated by `shadowFactorAt()` (N1). With a point
  light, it stays at −14%, because the ambient light and the sun no longer allocate anything. The attenuated color of
  the point light, however, is still created again at each pixel, which D2b will fix.

### 10.4 New proposals

#### N1. PCF: compute the plane of the receiver once per triangle, not at each pixel

*`ShadowingLight.shadowFactorAt(MapView, …)`, `posInLightSpace1()`*

For its *receiver plane depth bias*, PCF measures at each pixel the depth gradient of the surface: 2 tangents (cross
products and normalizations), then **2 extra projections** into the space of the light through the slow path of
`Matrix4 × Vector4`, i.e. about 10 allocations per pixel and per light. It is 23% of the total time of a frame with
PCF.

Yet on a flat triangle, **this gradient is constant**: it only depends on the plane of the triangle and the matrix of
the light. It can be computed **once per triangle and per light** (when the triangle is prepared), from the 3 vertices
projected into the space of the light. The BACKLOG already anticipates it ("a gradient computed from the geometry
instead of two projections"). For smooth surfaces (`INTERPOLATE`), the plane of the triangle remains an excellent
approximation at the scale of a texel.

Two more details:

- in the PCF path, `map.getInterpolation(s, t)` (bilinear read of 4 texels) is computed and then **never used** (it
  only serves `HARD`);
- the "3 × 3" kernel actually reads **16 texels** (4 × 4 weighted bilinearly), which is right for quality. A classic
  **early exit** can be added: first read the 4 corners, and if they all give "lit" or all "in the shadow", do not
  read the other 12. This is the case for the vast majority of the pixels, far from the shadow edges.

**Gain** e: large with PCF (of the order of −30%) · **Effort** M · **Readability** + (a named geometric computation,
instead of a measurement by finite differences).

> **Measured on October 8 (after steps 1 to 5 of phase 1) — estimate corrected.** The JIT now removes the
> allocations of the two extra projections: `posInLightSpace1()` weighs only 0.1% of the time. With PCF, **72% of the
> time is in `shadowFactorAt()`, and almost all of it in the loop of the 16 reads of the shadow map**: a 2000 × 2000
> map is 16 MB and fits in no cache, so each pixel pays cache misses. Computing the plane per triangle would therefore
> bring almost nothing today. The effective levers are, in order: the **early exit** on the 4 corners (reading 4
> texels instead of 16 far from the shadow edges), and a **smaller or better framed** shadow map
> (`SHADOWING_BOX_VIEWFRUSTUM`, R9).

> **Tried on October 10, abandoned: the early exit on a min/max bound.** The min/max depth per block of 4 × 4 texels
> (one pass over the map at each frame) makes it possible to conclude without reading the 16 texels when the whole
> area is clearly nearer (shadow) or farther (lit) than the point. Result: identical images, but **no gain, and even
> +10%**. A lit point is compared with the depth of **its own surface**, which is in the map up to a few biases: for
> the vast majority of the pixels (lit ones), only the plane of the receiver can decide, and the bound never
> concludes. Only the shadowed areas benefit, and computing the blocks (≈ 15 ms per frame for 2000²) costs more than it
> saves. The "4 corners" early exit first considered would have the same flaw, and would moreover make the shadows of
> thin objects disappear (a handrail thinner than 4 texels). What remains for PCF: a smaller or better framed shadow
> map (R9) and multithreading.

#### N2. Do not read the shadow map of a light that does not light the pixel

*`ShadingConsumer.consume()`*

This is R10 extended to the new lights. Today, `shadowFactorAt()` is called **before** knowing whether the light
contributes to the pixel: a pixel **outside the cone of a spot light** or **out of range of a point light** (zero
intensity) still pays for the projection, the selection of the face and the read of the shadow map (16 texels with
PCF). The tests must come in this order, from the cheapest to the most expensive: intensity > 0 (range, cone), then
`dotNL > 0`, then the shadow.
**Gain**: in UrbanScape, the test spot light lights a large part of the screen, and the gain stayed within the noise.
It will be **proportional to the share of the screen outside the cone or out of range**: high for a street lamp or a
stage spot light · **Effort** S · **Readability** +.

> **v2 — done (phase 1, step 6).** For each light, `ShadingConsumer` tests `dotNL` then the intensity (range, cone)
> before `shadowFactorAt()`, which receives `dotNL` and the unit normal instead of computing them again. Measured
> with A2: PCF ≈ −19% (main pass −20%), spot light −5% (main pass −17%), point light −6%, without shadows unchanged;
> images identical to the bit.

#### N3. Point and spot lights: a single computation of direction and distance per pixel

For a `PointLight` or a `SpotLight`, the same pixel computes the light → point vector **3 or 4 times**, each time with
a `new Vector3`, a square root and a normalization: in `getIntensity()` (attenuation), `getLightVectorAtPoint()`
(lighting), `coneFactor()` (spot light, which calls the two previous ones again) and `shadowFactorAt()` (selection of
the face, then `getLightVectorAtPoint()` again for the bias).
**Proposal**: a small reusable `LightSample` object (unit direction, distance, attenuation, cone factor), computed
once per pixel and per light, then passed to the lighting and to the shadow. The code also becomes more readable: it
says clearly what is computed once.
**Gain** e: medium · **Effort** S to M · **Readability** +.

> **v2 — in part (phase 1, step 6).** The color of a light is computed from the intensity already obtained
> (`Light.getLightColorAtPoint(point, intensity)`), and the cone of the spot light and the direction are no longer
> computed again by the shadow map. Two computations of the light → point vector remain for a point light (direction
> and intensity): the complete `LightSample` object remains to be done, with D2b.
>
> **v3 — done (November 2026, Refactoring branch).** `Light.sample(point, LightSample)` gives the unit direction, the
> distance and the intensity in one call; `PointLight` computes them from a single light → point vector (one square
> root), `SpotLight` adds its cone factor from the same direction. `ShadingConsumer` reuses one `LightSample` for all
> its pixels and lights: no allocation. The default implementation in `Light` asks `getLightVectorAtPoint()` and
> `getIntensity()`, so every light keeps working. Measured with `PerfBench` (1 thread): allocations 40 → 25 MB per
> frame for `point`, 26 → 11 MB for `spot`, main pass about −5%; images identical to the bit. The selection of the
> face of the cube map (`PointLight.shadowFactorAt()`) still builds its own light → point vector (no square root).

#### N4. Shadow passes: cull, cache, reuse

The shadow pass now represents 25 to 35% of the time, and a `PointLight` can do 6 of them per frame. Each of these
passes:

- transforms **all the vertices** and walks **all the triangles** of the scene, without culling by element;
- allocates a **new `ZBuffer`** (6 × 513² floats, i.e. about 6 MB per frame and per point light; 4 MB for a spot
  light at 1000²);
- computes everything again, even when neither the light nor the scene has moved.

Proposals, by order of simplicity:

1. **Reuse the buffers** from one frame to the next (S).
2. **Culling by range**: an element entirely outside the sphere of range of a point light (or the cone of a spot
   light) cannot cast a shadow on a point that this light lights, since the light → point segment stays in the
   sphere. It can therefore be ignored in the shadow pass. With bounding spheres (R1), it is one test per element
   (M).
3. **Culling by face** of the cube map (R1 in the frustum of each face) (M).
4. **Cache with a change indicator** (D8, already noted in the BACKLOG): a fixed lamp in a fixed scene needs its cube
   map **only once** (M).
5. The **`castShadows` per element** planned in the BACKLOG (S).

#### N5. `NearPlaneClipper`: fast path

`NearPlaneClipper.clip()` allocates 3 `Corner`s, 2 `ArrayList`s, at least one `ClippedTriangle` and an iterator **for
each triangle** rendered in perspective, whereas in almost all cases the 3 vertices are in front of the near plane and
nothing is clipped.
**Proposal**: if the 3 `w`s are above the near plane, rasterize the original triangle directly. One line, without
effect on the rest. The measured cost is small (≈ 0.1%), but it is allocation noise removed, and it prepares the test
by outcodes (R2), which gives the same result without any division.
**Effort** S · **Readability** =.

#### N6. Visible lights: write into the `int[]`, and parallelize later

`LightGlowRenderer` is well designed for performance: computations on `float`s, light shafts computed at 1/4 of the
resolution then interpolated, visibility estimated with 32 to 48 samples of the Z-buffer. Its cost comes mostly from
the writes: `addPixel()` goes through `getRGB()`/`setRGB()` for almost every pixel of the screen (light shafts). With
direct access to the `int[]` (D6), the cost of the effect is **about halved** (measured: from ≈ 80 to ≈ 40 ms). The
mask of the shafts also reads the Z-buffer 4 times per grid point, through the storage by columns (D7).
Later, the effect can be parallelized by bands without any precaution (§6).

#### N7. Lighting: what the new features add to A2 and D5

- `shadowFactorAt()` calls `normal.normalize()`, which **changes the normal of the fragment** (D5): the result is right
  today, but it is a side effect to remove before multithreading.
- With PCF, `shadowFactorAt()` returns a value between 0 and 1, and the contribution is multiplied by this factor.
  The structure of `ShadingConsumer` (a loop over the lights) lends itself well to the reorganization proposed in A2,
  N2 and N3: **compute once** the base color, the unit normal and the viewing direction, then for each light
  **`LightSample` → contribution test → shadow → accumulation**.

### 10.5 Progress of the proposals

| Id | Proposal | State as of November 5 |
|---|---|---|
| D1 | Static counters of the vectors | **Done** (phase 1, step 2): neutral single-threaded (±5%, within the noise), prerequisite of multithreading lifted |
| D7 | `FloatMap` as a 1D `float[]` | **Storage done** (phase 1, step 3): `MapView` as a `float[]` row by row, −10% without shadows and with PCF, −15 to −20% with a point or spot light (shadow passes −15 to −18%). Extracting `FloatMap` out of the `view` package remains (BACKLOG §2) |
| D6 | Direct writes into the `int[]` (+ `addPixel()`) | **Done** (phase 1, step 4, with D2a). **Image pool done** (November): `ImageView.setImageReuse(true)`, 3 images in rotation, enabled in the animated demos and `PerfBench` (off by default: an image obtained from `getImageView()` is drawn again two frames later). On 2 cores, SETUP −0.4 ms, PCF allocations 7.4 → 3.9 MB per frame; total time within the noise |
| A4 | Unrolled `Matrix4 × Vector4` | **Done** (phase 1, step 5): **no measurable gain** (the JIT already optimized the original loop), kept for readability. The `float[16]` storage is not done (no measured stake) |
| A2 | Lighting: common values computed once per pixel | **Done** (phase 1, step 6, with N2 and N3 in part; N3 completed in November with `Light.sample()`) |
| A3, A5, A6 | Stateless `ZBuffer`, sort of the 3 vertices, textures as `int[]` | **Done** (phase 1, step 8): without shadows −12% (with R4), allocations 12 → 7 MB per frame; loading a 4480 × 4480 texture ≈ 1.2 → 0.7-0.9 s; `ZBuffer` and `Texture` without shared state (prerequisite of multithreading) |
| A1 | Interpolation without temporary objects | **Done** (without incremental steps): `TriangleRasterizer` interpolates positions, normals and texture coordinates as `float`s, with exactly the same formulas, hence images identical to the bit; no more `Iterator` per pixel in `ShadingConsumer`, nor temporary vectors for the direction of the viewer, the specular reflection and the tangents of PCF. Measured with JFR: these vectors were ≈ 80% of the allocations. PCF: 26 to 41 → 7.4 MB per frame, **stable** (no more bimodal JIT); spot light 35 to 56 → 27 MB; on 2 cores, frame −10 to −15%. Incremental steps (one addition per pixel) would change the rounding, hence the images: not done |
| A7 | Gouraud mode | To do |
| D2a | Light colors and accumulator without `Color` | **Done** (phase 1, step 4, with D6): without shadows −43% (91 → 52 ms, 92 → 11 MB allocated per frame), point and spot lights −15%, visible lights −18%; PCF within the noise (dominated by N1) |
| D9 | Two-stage pipeline (geometry → `DrawList` → rasterization) | **Done** (`perf-multithreading` branch): identical images in all the rendering modes, time and memory unchanged (list reused from one frame to the next); the geometry stage weighs only ≈ 1.4 ms out of ≈ 50 to 190 ms of main pass |
| MT1 | Multithreading of the main pass by bands of rows (`BandRasterizer`, `RenderContext.setRenderThreads(n)`) | **Done** (`perf-multithreading` branch): identical images **and statistics** for 1, 2, 3, 5, 8 and 16 threads (all rendering modes, documentation images, `PerfBench`, `TestRenderThreads` test). On 2 cores, main pass ×1.8 to ×1.9 (PCF ≈ 200 → 110 ms, spot light ≈ 150 → 80-100 ms, without shadows ≈ 48 → 28-32 ms); single-threaded, unchanged. The edges and normals stay in the rendering thread (`Graphics2D` is not thread-safe). The shadow pass, now the longest with PCF and spot lights, stays single-threaded (next step) |
| MT2 | Shadow maps in two stages and by bands (`ShadowingLight`: `prepareShadowMapElement()` → `DrawList` → `BandRasterizer`) | **Done** (`perf-multithreading` branch): identical shadow maps, images and statistics for 1 to 16 threads (sun, spot light, point light with 6 faces). On 2 cores, shadow phase ×1.5 to ×1.9 (hard ≈ 67 → 35 ms, PCF ≈ 68 → 45, point ≈ 100 → 61, spot ≈ 120 → 75). The geometry stage of the light and the faces of the cube map stay sequential (D4) |
| MT3 | Post-processing by bands (`BandRasterizer.runRows()`: visible lights, haze and fogs) | **Done**: same threads as the rasterization (the atmosphere used a `parallelStream()` on the common pool of Java); identical images for 1 to 8 threads. On 2 cores, visible lights (sunglow) ≈ 51 → 32 ms; atmosphere ≈ 16 → 9.5 ms, as before. Note: a loop over all the rows called once per frame stays compiled "on stack replacement" (OSR) by the JIT and was 30% slower; hence one call per row (`runRows()`) |
| D2b, D4, D5, D8 | Design | To do (D8 referenced in the BACKLOG for the shadows) |
| D3 | Consumers created for each triangle | **Unnecessary**: measured with JFR at ≈ 0.1% of the allocations (one consumer per triangle and per band, against one vector per pixel before A1) |
| D10 | Rendering off the EDT | Done in `SceneViewer`; `MovingCamera` and `FractalLandscape` remain |
| R4 | Rejection of the triangles without a pixel center | **Done** (phase 1, step 8), before any allocation in `TriangleRasterizer` |
| R5 | Front-to-back order | **Done**: faces of the `DrawList` sorted by the depth of their nearest vertex before the rasterization (`RenderEngine.sortFrontToBack()`, unless the list contains edges). On UrbanScape, lit pixels −42 to −53% (the hidden pixels are rejected by the Z-buffer before the lighting); on 2 cores, main pass −30% without shadows, −40 to −45% with PCF and spot lights. Images unchanged (same checksums) |
| R1, R3, R6, R7 | Upstream culling | To do |
| R2 | Outcodes and near-plane clipping | **Done**: clipping, N5 fast path (phase 1, step 7), rejection test by outcodes (October 2026) |
| R9 | Shadow pass | **Shadow map kept done** (November, `perf-multithreading` branch): `ShadowingLight.setStaticShadowCasters(true)`, the map of each face is reused if the View·Projection of the light, the size and the maximum depth are the same as at the previous generation, otherwise it is drawn again; `invalidateShadowMap()` when the objects move. Images identical to the pixel (`TestStaticShadowCasters`, `PerfBench kept`: same checksum as `pcf`); on 2 cores, shadow phase 28 → 0.3 ms on UrbanScape. Enabled in `TempleRuins`. Culling in the space of the light and the D8 version counters remain. **N4.1 done** (phase 1, step 7): shadow buffers reused from one frame to the next; allocations −20 to −40%, time unchanged (allocating a new array or filling it cost the same) |
| R10 | Early exit from the lighting | **Done** with N2 (phase 1, step 6) |
| §6 | Multithreading | **Done**: see D9, MT1, MT2, MT3 (v3-0-0). The shadow maps of several lights and the faces of the cube map in parallel remain (after D4) |
| §7 | Timing per phase, `PerfBench` in the repository | **Done** (phase 0): `FrameTimer` and `PerfBench`, with the options `-frames`, `-warmup`, `-threads` and the modes `sunglow` and `atmosphere` |

### 10.6 Roadmap: what changes

Phase 1 of §8 remains valid as it is. **D2a** (at the top, with D1), **N2, N3, N5** and point 1 of **N4** (reusing the
shadow buffers) are added to it, all of them small. **N1** (PCF gradient per triangle) and points 2 to 4 of **N4**
(culling and caching of the shadow passes) join phase 2, with R1 and D8 on which they depend.

The pending decisions of the BACKLOG can now rely on these figures:

- **PCF 3 × 3 by default?** Today, it costs about +65 to +80% per frame with a single light. After the quick wins and
  N1, the difference should drop clearly (estimate: of the order of +25%, to be measured). It is better to wait for N1
  before making it the default behavior.
- **Shadows of the point lights**: 6 passes per frame, this is the case where caching (N4.4) and culling by range
  (N4.2) pay the most.

---

## 11. Update of November 17, 2026: what surrounds the rendering

Measurements taken in `TempleRuins` (56,676 triangles, window of 996 × 558, default options, shadow map kept), over
whole laps of the helicopter flight: at the end of each lap the demo gives the average rendering time (the engine
alone) and the number of images **displayed** per second (number of images / duration of the lap). The difference
between the two is the time spent outside of the engine, on the Swing thread: copy of the image into the window, pace
of the animation, rest of the application. Laptop with 20 threads, limited to about 28 W.

| Conditions | Average rendering | Images displayed per second | Outside of the rendering, per image |
|---|---:|---:|---:|
| "Balanced" power mode, Swing `Timer` of 15 ms | 63.3 ms | 13.1 | 13.0 ms |
| "Best performance", `Timer` of 1 ms | 36.8 ms | 22.1 | 8.4 ms |
| "Best performance", images chained by `invokeLater()` | 36.5 ms | 25.3 | 3.1 ms |
| Same, window on the screen of the laptop scaled to 200% | 40.3 ms | 21.0 | 7.3 ms |

### 11.1 The power mode

When plugged in, the laptop was in the "Balanced" power mode of Windows, which limits the clock speed of the
processor. In "Best performance", the engine is **1.7 times faster**: more than most of the optimizations of this
audit. All the earlier measurements taken on the laptop were made in "Balanced" mode. **Check this setting before
any measurement**, and only compare measurements taken in the same mode.

### 11.2 The pace of the animation

A Swing `Timer` starts a step only on one of its ticks: after each image, the Swing thread waited for the next tick
(0 to 15 ms with a delay of 15 ms). With a delay of 1 ms, the wait is still about 5 ms: the clock of Windows wakes the
programs at a coarser pace. **Done** in `TempleRuins`: each image schedules the next one with
`SwingUtilities.invokeLater()`, which comes after the repaint of the window and the mouse and keyboard events.
Rendering unchanged, **+14% images displayed** (92% of what the engine computes). The other demos animated by a
`Timer` (`FractalLandscape`, `MovingCamera`…) lose up to one clock step per image in the same way.

### 11.3 Display on a scaled screen

On the screen of the laptop (2880 × 1800, scaled to 200% by Windows), Java creates the window in logical pixels and
must enlarge the image of the engine × 2 at each copy: about 2.2 million pixels written instead of 0.56 million. The
time outside of the rendering goes from 3 to 7 ms per image, and the number of images displayed drops by 17%. The
rendering itself is also 10% slower, without a certain explanation (possible causes: the integrated graphics chip,
which drives a larger screen, shares the power budget of the processor; or a warmer laptop, this lap being the 5th).

Why does the graphics chip not do the enlargement? Java2D accelerates it for an image it keeps in graphics memory,
which assumes an image that rarely changes. The image of the engine changes at each frame, and its pixels are written
directly (`getDataBuffer()`, see D6): Java2D can no longer track its changes and processes it with its software
routines.

**Proposal V1**: copy the image 1:1 into a `VolatileImage` (kept in graphics memory), then draw that one into the
window, so that the enlargement is done by the graphics chip. **Expected gain**: bring the display at 200% back to
about 3 ms per image, to be measured (depends on the graphics pipeline enabled by Java on Windows) · **Effort** S
(about twenty lines, in `SwingView` or first in a demo) · **Readability** = · postponed (BACKLOG §6). The engine keeps
computing everything on the CPU.

---

## 12. Update of November 26, 2026: phase 2, what remains sequential

With MT1 to MT3 (§10.5), every stage that touches the pixels is shared between the cores: the main pass, the shadow
maps, the post-processes. What remains in the rendering thread alone was negligible when it was 1 to 2% of a frame
computed by one core. It is no longer: by **Amdahl's law**, the sequential part is what limits the gain of the
threads, and its share grows with the number of cores.

An order of magnitude, from the statistics shown in [TEMPLE_RUINS.md](TEMPLE_RUINS.md#statistics) (56,676
triangles, 753 Elements; the conditions of that image were not recorded, they must be measured again):
`SETUP 3.1, SHADOWS 42.1, GEOMETRY 10.4, MAIN_PASS 148.3, OVERLAYS 4.9 ms`. If the main pass goes down to about
30 ms with the threads, SETUP and GEOMETRY, about 13 ms, become **30% of the frame**. On `UrbanScape` (9,000
triangles) the geometry weighed ≈ 1.4 ms (D9): the question depends on the scene, and `TempleRuins` is the one that
asks it.

### 12.1 Where the sequential time is

| Phase (`FrameTimer`) | Sequential part today | Parallel today |
|---|---|---|
| `SETUP` | `world.worldProject()` (world coordinates of all the vertices), `Lighting.prepareFrame()`, clearing of the back buffer and of the Z-buffer | — |
| `SHADOWS` | Geometry stage of each light, in its space (`prepareShadowMapElement()`); the lights one after the other, and the 6 faces of a point light one after the other | Rasterization of each map (MT2) |
| `GEOMETRY` | All of it: transformation, culling, normals, materials, filling of the `DrawList`, building of the mipmaps (once) | — |
| `MAIN_PASS` | Front-to-back sort (R5) | Rasterization (MT1) |
| `OVERLAYS` | Lines (landmarks, light vectors): `Graphics2D` | Haze, fogs, visible lights (MT3) |
| Outside the engine | Copy of the image into the window, on the EDT, while nothing is rendered (`TempleRuins` renders on the EDT): 3 ms per image, 7 ms on a screen scaled to 200% (§11) | — |

### 12.2 Proposals

**P0. Measure the sequential part.** `PerfBench` renders `UrbanScape`, a scene where the geometry is small. Add
the `TempleRuins` scene to it (its world is built without a window), and print the phases for `-threads 1` and the
default: the time of each phase that does not decrease with the threads is the sequential part. Take the
measurements in the "Best performance" power mode (§11.1).
**Effort** S · **Readability** = · the condition of all that follows.

**R1 first: remove work before sharing it.** The hierarchical frustum culling by `Element` (§4, R1, still to do)
skips a whole subtree out of view with one test of its bounding volume, instead of transforming and testing each of
its triangles. In `TempleRuins`, 25,155 of the 56,676 triangles are not displayed. This reduces `GEOMETRY` for every
number of threads, and is in line with the rule of this audit: remove the work before parallelizing it.

**MT4. Geometry stage of the main pass in parallel.** `prepare()` goes through the tree of `Element`s and appends
the visible triangles to the `DrawList`. Proposal, with the same principles as the bands of MT1:

- flatten the tree into a list of **batches** (subtrees of a few hundred triangles, with the color and emission they
  inherit), handed out dynamically to the threads of the `BandRasterizer` (no second pool); a split by root Element
  would be unbalanced (the temple on one side, a sea of 2,048 triangles on the other);
- each worker owns its **`ElementTransform`** (mutable: `setModel()`), its **counters** (`nbt_in`, `nbt_out`,
  `nbt_bf`…) and its **segment of `DrawList`**;
- the segments are concatenated **in the order of the batches**: the list, hence the sort, the image and the
  statistics, is the same as with one thread (checked by `TestRenderThreads` as it is);
- what makes it safe is already there: a vertex belongs to a single `Element` (rule of `prepare()`), so two workers
  never write the same `prj_position`; `buildMipmaps()` is `synchronized`.

D4 is **not** needed for this: D4 separates the **passes** (a shadow pass and the main pass write the same vertices),
whereas MT4 splits **one** pass between Elements that share no vertex.
**Expected gain**: `GEOMETRY` divided by 3 to 5 (≈ 10 → 2-3 ms on `TempleRuins`) · **Effort** M · **Readability** =
(the batch runner is the generalization of `runBands()`; `prepare()` itself does not change).

**MT5. Geometry stage of the shadow maps in parallel.** The same code, in `ShadowingLight`. It only matters when
the map is drawn again at each frame: nothing in `TempleRuins` (map kept, R9), a lot in `StarshipInterior` and with
moving lights.
**Expected gain**: the geometry part of `SHADOWS` divided by 3 to 5 · **Effort** S after MT4 · **Readability** =.

**D8 for `worldProject()`.** The world coordinates of the vertices are computed again at each frame, even when no
`Element` has moved (nothing moves in `TempleRuins`). The version counters of D8 avoid the work entirely; a
parallel `worldProject()` (MT6, trivial: one Element per task) only divides it. D8 first; MT6 only if a scene with
much movement shows the need.
**Expected gain**: `worldProject()` → 0 for a static scene · **Effort** M (D8 done uniformly) · **Readability** =.

**MT7. Clear the buffers by bands.** Today the rendering thread clears the whole back buffer and Z-buffer, then the
workers read them again band by band. Each worker could clear its band at the start of its first pass over it: one
pass over the memory instead of two, and the band is then in the cache of the core that draws it.
**Expected gain**: ≈ 1 ms · **Effort** S · **Readability** − (the clear moves away from `initView()`): only if P0
shows it is worth it.

**The sort of R5 stays sequential**, unless P0 shows more than a few percent: `Arrays.parallelSort()` runs on the
common pool of Java (against the choice of §6), and the faces at equal depth must keep a deterministic order.

**MT8. Several shadow maps at the same time** (the lights, the 6 faces of a point light): already listed in §6,
still blocked by **D4** (vertex output buffers per pass). It is the case of `StarshipInterior` (2 to 3 point and spot
lights). Before it, the culling of the faces of the cube map by the range of the light (N4.2) removes more work, with
no threads.
**Effort** L (D4) · **Readability** + (D4 separates the model from the pipeline).

**V2. Render while the window displays.** In `TempleRuins` and `StarshipInterior`, the rendering runs on the EDT
(model A of [DESIGN §8](DESIGN.md#integration-into-a-swing-application)): while the image is copied into the
window, nothing is rendered. With a rendering thread that always draws the latest state of the camera (model B, that
of `SceneViewer`, D10), image N+1 is computed while the EDT displays image N.
**Expected gain**: the 3 to 7 ms per image outside the engine (§11) overlap with the rendering, **+10 to 20% images
displayed**, to be measured · **Effort** M · **Readability** = if it is done once, in the Swing component of the
BACKLOG (§10, "A ready-made Swing component"), rather than in each demo. Image reuse (D6) must then be checked: the
image must be painted before it is drawn again, two frames later.

**Not proposed:** overlapping two frames inside the engine (geometry of frame N+1 during the rasterization of N, as
a GPU does). It needs D4, breaks the rule "do not change the scene during `render()`" (DESIGN §8), and would make the
pipeline much harder to follow. The lines (`Graphics2D`, not thread-safe) stay sequential.

### 12.3 Roadmap of phase 2

On the `perf-multithreading` branch, one step at a time, each one checked by `mvn test`, `TestRenderThreads` and the
checksums of `PerfBench` (identical images), and measured with P0:

| Step | Content | Condition |
|---|---|---|
| 0 | **P0**: `TempleRuins` in `PerfBench`, phases for 1 and N threads | — |
| 1 | **R1**: hierarchical frustum culling by `Element` | — |
| 2 | **MT4**, then **MT5** with the same batch runner | P0 shows `GEOMETRY` (and the geometry of `SHADOWS`) above ≈ 10% of the frame |
| 3 | **D8**: version counters, first for `worldProject()` | — |
| 4 | **V2**: rendering thread, in the reusable Swing component | — |
| 5 | **MT7**, sort of R5 | Only if P0 shows a stake |
| 6 | **D4**, then **MT8** and N4.2 | The heaviest; to be decided after steps 0 to 4 |

| Id | Proposal | State as of November 26 |
|---|---|---|
| P0 | Measure the sequential part on `TempleRuins` | To do |
| MT4 | Geometry stage of the main pass in parallel, by batches of `Element`s | To do |
| MT5 | Geometry stage of the shadow maps in parallel | To do (after MT4) |
| MT6 | `worldProject()` in parallel | Not needed if D8 is done |
| MT7 | Clearing of the buffers by bands | To do if P0 shows a stake |
| MT8 | Several shadow maps at the same time | To do after D4 |
| V2 | Rendering thread in the demos (model B) | To do, with the Swing component of the BACKLOG |
