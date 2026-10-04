# Aventura

Lightweight software 3D rendering engine, **100% pure Java, CPU only**: no GPU, no native code,
no third-party runtime library (only the JDK; JUnit 4 for tests). Java 21, Maven, MIT license.

## Guiding principles

- **Stay pure Java and CPU-based.** Never add a runtime dependency, JNI, OpenGL/GPU binding.
- **The project is didactic.** Favor readable code and recognizable algorithms over ultimate
  optimization. A performance gain that makes an algorithm hard to follow must be discussed first.
- Keep the public API stable unless the task is explicitly about changing it.
- Code, comments, Javadoc and docs are written in **English**.

## Build, test, run

- Compile: `mvn compile` (tests: `mvn test-compile`)
- Unit tests (JUnit 4, ~430, headless): `mvn test`
  - Single class: `mvn test -Dtest=Matrix4Test`
- Run a demo (from the project root, textures are read from `./resources/texture`):
  `mvn test-compile exec:java -Dexec.mainClass=com.aventura.demo.AventuraDemo`
- Javadoc: `mvn javadoc:javadoc` (output in `target/reports/apidocs`)
- Always run `mvn test` before declaring a change done. Visual test programs
  (`com.aventura.test.*`) open windows: do not launch them unless asked, Olivier checks them by eye.

## Layout

- `src/main/java/com/aventura/`
  - `math/` vectors, matrices, quaternions, transforms, projections, tools
  - `model/` world, elements and shapes, camera, lights, materials, textures, perspective
  - `engine/` rendering pipeline, rasterization, fragments, shadow maps
  - `context/` graphic and rendering contexts
  - `view/` `GUIView`, `ImageView`, `SwingView`, `MapView`
  - `demo/` demo applications
  - `tools/` color, tracing
- `src/test/java/com/aventura/` unit tests mirror the main packages; `test/` holds ~70 visual
  test programs with a `main()`; `cookbook/` tests the Geometry Cookbook examples
- `resources/texture` textures; `resources/doc/images` documentation images
- `docs/`
  - `BACKLOG.md` work items not handled yet (read it when picking or finishing a backlog item)
  - `DESIGN.md` architecture, pipeline, lighting, shadows, contexts, conventions
  - `GEOMETRY_COOKBOOK.md`, `PERFORMANCE_AUDIT.md`, `JAVADOC_AUDIT.md`

## Conventions (see docs/DESIGN.md §12)

- **Z is up.** Cameras use `Vector4.zAxis()` as up vector.
- Directional lights are defined by their **propagation direction** (light towards scene).
- New shapes extend `GenerativeElement`, not `Element`.
- Never retain a `Fragment` (or its vectors) after `consume()` returns; copy values out.
- Transform order is S → R → T (`M = T · R · S`), composed parent-first down the tree.
- Refresh, don't cache: derived matrices go through `ViewProjection.refresh()` once per frame.
- Tests must run headless (Surefire sets `-Djava.awt.headless=true`).
- Constructor argument order differs between `PerspectiveContext` and `Perspective`/`Projection`
  (see BACKLOG §1): double-check before calling them.

## Workflow

- Olivier develops in **Eclipse** on the same folder. Do not edit `.project`, `.classpath`,
  `.settings/`, and ignore `bin/` and `target/` (build outputs).
- Update `docs/BACKLOG.md`, `docs/DESIGN.md` and `README.md` when a change affects them.
- Commits: small and focused, imperative English messages (e.g. "Add SpotLight shadow mapping").
  Do not commit or push unless asked.
- Scratch files and reports go in `Claude outputs/` (git-ignored), never in `src/`.
