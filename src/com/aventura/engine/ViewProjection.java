package com.aventura.engine;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------
 * A pure View*Projection projector: combines a Camera's view matrix and a
 * Perspective's projection matrix into a single, immutable transformation
 * that maps a world-space point directly to clip space -- no Model matrix
 * involved, since every caller already supplies a genuinely world-space point
 * (e.g. Fragment.getWorldPosition(), or a debug vector's world origin/tip).
 *
 * Replaces the vp field / calculateVPMatrix() / projectVP() trio that used to
 * live on ModelViewProjection: the VP matrix is computed once, at
 * construction, so there is no separate "did I call calculate before
 * project?" ordering risk.
 *
 * Used identically for two purposes:
 * - ShadowingLight.shadowFactorAt(): projects a fragment's world position
 *   into a light's clip space to sample its shadow map.
 * - ScreenLineRenderer.drawVector(): projects debug-vector endpoints
 *   (landmarks, normals, light directions) into the main camera's clip space.
 *
 * IMMUTABILITY NOTE: this object's VP matrix is fixed at construction. If the
 * underlying Camera or Perspective changes afterward (e.g. a moving light),
 * a new ViewProjection must be constructed -- there is no update/refresh
 * method. This matches the legacy ModelViewProjection's own behavior (its
 * view/projection fields were likewise only ever set once, at construction).
 *
 * @author Olivier BARRY
 * @since 2026 (extracted from ModelViewProjection)
 *
 */
public class ViewProjection {

	private final Matrix4 vp;

	public ViewProjection(Matrix4 view, Matrix4 projection) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "ViewProjection(view, projection)");
		this.vp = projection.times(view);
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "VP matrix:\n" + vp);
	}

	/** Convenience constructor: extracts the view matrix from camera and the projection matrix from perspective. */
	public ViewProjection(Camera camera, Perspective perspective) {
		this(camera.getMatrix(), perspective.getProjection());
	}

	/** Projects a world-space point (homogeneous, w = 1) directly into clip space. Pure -- does not mutate worldPoint. */
	public Vector4 project(Vector4 worldPoint) {
		return vp.times(worldPoint);
	}
}