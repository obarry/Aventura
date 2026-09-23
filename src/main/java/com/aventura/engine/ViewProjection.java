package com.aventura.engine;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2026 Olivier BARRY
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------
 * 
 * A pure View*Projection projector: combines a Camera's view matrix and a
 * Perspective's projection matrix into a single transformation that maps a
 * world-space point directly to clip space -- no Model matrix involved,
 * since every caller already supplies a genuinely world-space point (e.g.
 * Fragment.getWorldPosition(), or a debug vector's world origin/tip).
 *
 * Replaces the vp field / calculateVPMatrix() / projectVP() trio that used to
 * live on ModelViewProjection.
 *
 * Used identically for two purposes:
 * - ShadowingLight.shadowFactorAt(): projects a fragment's world position
 *   into a light's clip space to sample its shadow map.
 * - ScreenLineRenderer.drawVector(): projects debug-vector endpoints
 *   (landmarks, normals, light directions) into the main camera's clip space.
 *
 * MUTABILITY NOTE (corrected from an earlier, wrong assumption): the vp
 * matrix is NOT permanently fixed at construction. Camera.updateCamera()
 * mutates the SAME LookAt/Matrix4 object in place rather than replacing it --
 * so camera.getMatrix() keeps returning the same reference, but its content
 * can change (e.g. after a mouse-driven camera move). Since vp = projection*
 * view is computed once into a brand new Matrix4 with no live link back to
 * view's array, it would silently go stale after such a move if never
 * recomputed. Call refresh() whenever the underlying Camera/Perspective may
 * have changed -- typically once per frame, before rendering, in
 * RenderEngine.render() (and, for a light, at the top of
 * ShadowingLight.generateShadowMap()).
 *
 * When built from a (Camera, Perspective) pair, refresh() re-reads BOTH
 * camera.getMatrix() and perspective.getProjection(): Perspective's setters
 * (setWidth(), setDist()...) REPLACE the Projection instance rather than
 * mutating it, so holding on to the Projection captured at construction used
 * to silently ignore any perspective change (e.g. a zoom) made after the
 * RenderEngine was created.
 *
 * @author Olivier BARRY
 * @since 2026 (extracted from ModelViewProjection)
 *
 */
public class ViewProjection {

	// Live sources, when built from a Camera and a Perspective (null otherwise): re-read on refresh()
	private final Camera camera;
	private final Perspective perspective;

	private Matrix4 view;
	private Matrix4 projection;
	private Matrix4 vp;

	/**
	 * Fixed matrices: refresh() recomputes vp from the CONTENT of these same two instances.
	 */
	public ViewProjection(Matrix4 view, Matrix4 projection) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "ViewProjection(view, projection)");
		this.camera = null;
		this.perspective = null;
		this.view = view;
		this.projection = projection;
		this.vp = projection.times(view);
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "VP matrix:\n" + vp);
	}

	/**
	 * Live link to the camera and the perspective: refresh() re-reads camera.getMatrix() and
	 * perspective.getProjection(), so both camera moves and perspective changes are taken into account.
	 */
	public ViewProjection(Camera camera, Perspective perspective) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "ViewProjection(camera, perspective)");
		this.camera = camera;
		this.perspective = perspective;
		refresh();
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "VP matrix:\n" + vp);
	}

	/**
	 * Recomputes vp from the CURRENT state of the camera/perspective (or view/projection matrices) -- call
	 * this whenever the underlying Camera (or light) or Perspective may have changed since the last
	 * refresh/construction, before project()/getMatrix() are relied upon. Cheap (one matrix
	 * multiplication): safe to call once per frame even when nothing actually changed.
	 */
	public void refresh() {
		if (camera != null) this.view = camera.getMatrix();
		if (perspective != null) this.projection = perspective.getProjection();
		this.vp = projection.times(view);
	}

	/** Projects a world-space point (homogeneous, w = 1) directly into clip space. Pure -- does not mutate worldPoint. */
	public Vector4 project(Vector4 worldPoint) {
		return vp.times(worldPoint);
	}

	/**
	 * Exposes the current View*Projection matrix for further composition -- e.g. ElementTransform
	 * uses this to compute full = vp.times(model) in one multiplication. Callers that hold onto
	 * this object across a refresh() should re-fetch via getMatrix() rather than caching the
	 * returned Matrix4, since refresh() replaces it with a new instance.
	 */
	public Matrix4 getMatrix() {
		return vp;
	}
}
