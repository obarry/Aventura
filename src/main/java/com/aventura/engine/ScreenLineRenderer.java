package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.view.GUIView;

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
 * Draws lines on screen for two related but distinct needs:
 * - Triangle wireframe edges, from Vertex data already projected by the ongoing
 *   per-Element Model*View*Projection pipeline (getProjPos()).
 * - Standalone debug vectors anchored in world space (axis landmarks, surface
 *   normals, light directions), projected here directly via View*Projection
 *   only (no Model involved, since the caller already supplies a world-space
 *   point) — see drawVector()'s Javadoc.
 *
 * These were kept as one class rather than two because the actual mechanism
 * (project two points, draw a line) is identical; only which "already
 * computed" data each need starts from differs.
 *
 * NOT CARRIED OVER: the legacy bressenham_int/bressenham_short methods —
 * dead code even before this refactoring, since drawLine already delegated to
 * GUIView.drawLine() and never called them.
 *
 * DEFERRED (backlog): z-buffer-aware wireframe. Edges are currently drawn
 * regardless of what's in front of them, matching the legacy behavior.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class ScreenLineRenderer {

	private final PerspectiveContext perspectiveCtx;

	// The camera's single, frame-lifetime ViewProjection instance (not a light's) — used only for
	// drawVector()'s world-space projection. Its VP matrix is ready as soon as it's constructed
	// (no separate "calculate" call needed, unlike the legacy ModelViewProjection).
	private final ViewProjection viewProjection;

	private final GUIView view;

	public ScreenLineRenderer(PerspectiveContext perspectiveCtx, ViewProjection viewProjection, GUIView view) {
		this.perspectiveCtx = perspectiveCtx;
		this.viewProjection = viewProjection;
		this.view = view;
	}

	//
	// Triangle wireframe
	//

	public void drawTriangleEdges(Triangle t, Color c) {
		drawLine(t.getV1(), t.getV2(), c);
		drawLine(t.getV2(), t.getV3(), c);
		drawLine(t.getV3(), t.getV1(), c);
	}

	//
	// Depth-tested triangle edges (hidden-line rendering: MONOCHROME, UNLIT)
	//

	/**
	 * Relative depth tolerance of the depth-tested edges under a FRUSTUM projection (depth = eye distance W):
	 * an edge pixel is drawn if its depth is at most (1 + bias) times the ZBuffer depth. The edges lie on the
	 * faces themselves, so without this tolerance they would flicker against their own face (z-fighting).
	 */
	public static final float EDGE_DEPTH_BIAS_FRUSTUM = 0.01f;

	/**
	 * Absolute depth tolerance of the depth-tested edges under an ORTHOGRAPHIC projection (depth = NDC z).
	 */
	public static final float EDGE_DEPTH_BIAS_ORTHOGRAPHIC = 0.002f;

	/**
	 * Draws the 3 edges of a (filled) triangle, testing each pixel against the ZBuffer: edges hidden by
	 * faces already drawn are not displayed (hidden-line rendering). Each drawn pixel also writes its depth
	 * into the ZBuffer (if nearer), so that a face drawn later behind it does not overwrite the edge.
	 * Must be called AFTER the triangle itself has been rasterized.
	 */
	public void drawTriangleEdgesDepthTested(Triangle t, Color c, ZBuffer zBuffer) {
		drawLineDepthTested(t.getV1(), t.getV2(), c, zBuffer);
		drawLineDepthTested(t.getV2(), t.getV3(), c, zBuffer);
		drawLineDepthTested(t.getV3(), t.getV1(), c, zBuffer);
	}

	private void drawLineDepthTested(Vertex v1, Vertex v2, Color c, ZBuffer zBuffer) {

		boolean frustum = perspectiveCtx.getPerspectiveType() == PerspectiveType.FRUSTUM;
		// Same depth convention as TriangleRasterizer: W (eye distance) for a frustum, Z for orthographic
		float z1 = frustum ? v1.getProjPos().getW() : v1.getProjPos().getZ();
		float z2 = frustum ? v2.getProjPos().getW() : v2.getProjPos().getZ();
		if (frustum && (z1 <= 0 || z2 <= 0)) return; // Behind the eye: not handled (no clipping), as for triangles

		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		float x1 = v1.getProjPos().get3DX() * halfWidth, y1 = v1.getProjPos().get3DY() * halfHeight;
		float dx = v2.getProjPos().get3DX() * halfWidth - x1, dy = v2.getProjPos().get3DY() * halfHeight - y1;

		// Clip the parameter range {tMin, tMax} of the segment to the screen (Liang-Barsky)
		float[] range = { 0, 1 };
		if (!clip(-dx, x1 + halfWidth, range) || !clip(dx, halfWidth - x1, range)
				|| !clip(-dy, y1 + halfHeight, range) || !clip(dy, halfHeight - y1, range)) {
			return; // Entirely outside the screen
		}

		int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) * (range[1] - range[0]));
		for (int i = 0; i <= steps; i++) {
			float t = steps == 0 ? range[0] : range[0] + (range[1] - range[0]) * i / steps;
			// Rounding to the nearest pixel center: same pixel convention as TriangleRasterizer
			int x = Math.round(x1 + t * dx);
			int y = Math.round(y1 + t * dy);
			if (Math.abs(x) > halfWidth || Math.abs(y) > halfHeight) continue;

			// Depth: 1/W is linear on screen for a frustum (perspective-correct), Z is linear for orthographic
			float z = frustum ? 1 / ((1 - t) / z1 + t / z2) : z1 + t * (z2 - z1);
			float stored = zBuffer.get(x, y);
			boolean visible = frustum ? z <= stored * (1 + EDGE_DEPTH_BIAS_FRUSTUM) : z <= stored + EDGE_DEPTH_BIAS_ORTHOGRAPHIC;
			if (visible) {
				view.drawPixel(x, y, c);
				if (z < stored) zBuffer.update(x, y, z);
			}
		}
	}

	/** One Liang-Barsky clipping step on range = {tMin, tMax}; returns false if nothing is left. */
	private static boolean clip(float p, float q, float[] range) {
		if (p == 0) return q >= 0; // Parallel to this boundary: entirely inside or outside
		float r = q / p;
		if (p < 0) {
			if (r > range[1]) return false;
			if (r > range[0]) range[0] = r;
		} else {
			if (r < range[0]) return false;
			if (r < range[1]) range[1] = r;
		}
		return true;
	}

	public void drawLine(Vertex v1, Vertex v2, Color c) {
		view.setColor(c);
		view.drawLine(screenX(v1), screenY(v1), screenX(v2), screenY(v2));
	}

	public void drawLine(Segment s, Color c) {
		drawLine(s.getV1(), s.getV2(), c);
	}

	private int screenX(Vertex v) {
		return Math.round(v.getProjPos().get3DX() * perspectiveCtx.getPixelHalfWidth());
	}

	private int screenY(Vertex v) {
		return Math.round(v.getProjPos().get3DY() * perspectiveCtx.getPixelHalfHeight());
	}

	//
	// Debug vectors — world-space in, no Model matrix involved (View*Projection only), which is
	// exactly what avoids the model/world-space-mixing bug found in the legacy
	// RenderEngine.displayNormalVectors(): every caller here supplies a genuinely world-space
	// origin (e.g. Triangle.getCenterWorldPos(), Vertex.getWorldPos()) rather than a model-space
	// position combined with a world-space direction.
	//

	/**
	 * Draws a line from worldOrigin to worldOrigin + direction, both understood as world-space
	 * (Vector4.plus(Vector3) preserves worldOrigin's w, so this is a correct point+vector addition
	 * regardless of whether direction is normalized or scaled for visibility).
	 */
	public void drawVector(Vector4 worldOrigin, Vector3 direction, Color c) {
		Vector4 worldTip = worldOrigin.plus(direction);
		view.setColor(c);
		view.drawLine(screenXWorld(worldOrigin), screenYWorld(worldOrigin), screenXWorld(worldTip), screenYWorld(worldTip));
	}

	private int screenXWorld(Vector4 worldPoint) {
		Vector4 clip = viewProjection.project(worldPoint);
		return Math.round(clip.get3DX() * perspectiveCtx.getPixelHalfWidth());
	}

	private int screenYWorld(Vector4 worldPoint) {
		Vector4 clip = viewProjection.project(worldPoint);
		return Math.round(clip.get3DY() * perspectiveCtx.getPixelHalfHeight());
	}
}