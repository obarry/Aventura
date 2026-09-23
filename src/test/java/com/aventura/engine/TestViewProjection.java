package com.aventura.engine;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.PerspectiveType;

/**
 * Tests that ViewProjection follows both the camera and the perspective on refresh().
 * Perspective's setters replace the Projection instance: ViewProjection used to keep the one
 * captured at construction, so a zoom (setWidth) had no effect on the rendering.
 */
public class TestViewProjection {

	private static final float DELTA = 1e-3f;

	@Test
	public void testRefreshFollowsPerspectiveChange() {
		PerspectiveContext ctx = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1000);
		Camera camera = new Camera(new Vector4(0, 0, 0, 1), new Vector4(0, 0, -1, 1), new Vector4(0, 1, 0, 0));
		ViewProjection vp = new ViewProjection(camera, ctx.getPerspective());

		Vector4 edge = new Vector4(0.4f, 0, -1f, 1); // right edge of the near plane
		assertEquals(1f, vp.project(edge).get3DX(), DELTA);

		// Zoom out: twice wider window
		ctx.getPerspective().setWidth(1.6f);
		vp.refresh();
		assertEquals(0.5f, vp.project(edge).get3DX(), DELTA);
	}

	@Test
	public void testRefreshFollowsCameraMove() {
		PerspectiveContext ctx = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1000);
		Camera camera = new Camera(new Vector4(0, 0, 0, 1), new Vector4(0, 0, -1, 1), new Vector4(0, 1, 0, 0));
		ViewProjection vp = new ViewProjection(camera, ctx.getPerspective());

		Vector4 p = new Vector4(0.4f, 0, -1f, 1);
		assertEquals(1f, vp.project(p).get3DX(), DELTA);

		// Move the camera 0.4 to the right: the point is now in the center
		camera.updateCamera(new Vector4(0.4f, 0, 0, 1), new Vector4(0.4f, 0, -1, 1), new Vector4(0, 1, 0, 0));
		vp.refresh();
		assertEquals(0f, vp.project(p).get3DX(), DELTA);
	}
}
