package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.view.GUIView;

/**
 * ------------------------------------------------------------------------------
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

	public void drawLine(Vertex v1, Vertex v2, Color c) {
		view.setColor(c);
		view.drawLine(screenX(v1), screenY(v1), screenX(v2), screenY(v2));
	}

	public void drawLine(Segment s, Color c) {
		drawLine(s.getV1(), s.getV2(), c);
	}

	private int screenX(Vertex v) {
		return (int) (v.getProjPos().get3DX() * perspectiveCtx.getPixelHalfWidth());
	}

	private int screenY(Vertex v) {
		return (int) (v.getProjPos().get3DY() * perspectiveCtx.getPixelHalfHeight());
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
		return (int) (clip.get3DX() * perspectiveCtx.getPixelHalfWidth());
	}

	private int screenYWorld(Vector4 worldPoint) {
		Vector4 clip = viewProjection.project(worldPoint);
		return (int) (clip.get3DY() * perspectiveCtx.getPixelHalfHeight());
	}
}