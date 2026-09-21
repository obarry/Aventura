package com.aventura.context;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.perspective.Perspective;

/**
 * Tests for PerspectiveContext's six-bounds constructor (top, bottom, right, left, far, near, type, ppu),
 * which is the one a SpotLight (and every face of a PointLight cube) needs to build an asymmetric or
 * explicitly bounded frustum. FrustumPerspective's six-bounds constructor used to declare its parameters
 * in a different order than OrthographicPerspective's and passed "top" where "left" was expected, so the
 * same arguments gave a correct Orthographic perspective and a broken Frustum one.
 */
public class TestPerspectiveContext {

	private static final float DELTA = 1e-5f;

	private Perspective frustum(float top, float bottom, float right, float left, float far, float near) {
		return new PerspectiveContext(top, bottom, right, left, far, near, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 100).getPerspective();
	}

	@Test
	public void testFrustumSixBounds_preservesEveryBound() {
		System.out.println("***** Test PerspectiveContext : FRUSTUM six-bounds constructor keeps left/right/bottom/top/near/far *****");

		Perspective p = frustum(0.5f, -0.4f, 0.6f, -0.3f, 100f, 1f);

		assertEquals(-0.3f, p.getLeft(), DELTA);
		assertEquals(0.6f, p.getRight(), DELTA);
		assertEquals(-0.4f, p.getBottom(), DELTA);
		assertEquals(0.5f, p.getTop(), DELTA);
		assertEquals(1f, p.getNear(), DELTA);
		assertEquals(100f, p.getFar(), DELTA);
	}

	@Test
	public void testFrustumSixBounds_sameBoundsAsOrthographicWithSameArguments() {
		System.out.println("***** Test PerspectiveContext : FRUSTUM and ORTHOGRAPHIC agree on the bounds for the same arguments *****");

		Perspective f = frustum(0.5f, -0.5f, 0.5f, -0.5f, 100f, 1f);
		Perspective o = new PerspectiveContext(0.5f, -0.5f, 0.5f, -0.5f, 100f, 1f, PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, 100).getPerspective();

		assertEquals(o.getLeft(), f.getLeft(), DELTA);
		assertEquals(o.getRight(), f.getRight(), DELTA);
		assertEquals(o.getBottom(), f.getBottom(), DELTA);
		assertEquals(o.getTop(), f.getTop(), DELTA);
		assertEquals(o.getNear(), f.getNear(), DELTA);
		assertEquals(o.getFar(), f.getFar(), DELTA);
	}

	@Test
	public void testFrustumSixBounds_projectionIsFiniteAndMapsNearAndFar() {
		System.out.println("***** Test PerspectiveContext : FRUSTUM six-bounds projection maps the near plane to -1 and the far plane to +1 *****");

		Matrix4 m = frustum(0.5f, -0.5f, 0.5f, -0.5f, 100f, 1f).getProjection();

		// A point on the axis at the near plane and one at the far plane (the eye looks towards -Z)
		Vector4 atNear = m.times(new Vector4(0, 0, -1f, 1));
		Vector4 atFar = m.times(new Vector4(0, 0, -100f, 1));

		assertFinite(atNear);
		assertFinite(atFar);
		assertEquals(-1f, atNear.getZ() / atNear.getW(), 1e-3f);
		assertEquals(1f, atFar.getZ() / atFar.getW(), 1e-3f);
		// After projection, w carries the eye-space depth (used as the depth stored by the rasterizer)
		assertEquals(1f, atNear.getW(), 1e-3f);
		assertEquals(100f, atFar.getW(), 1e-2f);
	}

	@Test
	public void testFrustumSixBounds_edgeOfNearPlaneProjectsToEdgeOfScreen() {
		System.out.println("***** Test PerspectiveContext : FRUSTUM six-bounds: the edge of the near plane lands on x = +1 / -1 *****");

		Matrix4 m = frustum(0.5f, -0.5f, 0.5f, -0.5f, 100f, 1f).getProjection();

		Vector4 right = m.times(new Vector4(0.5f, 0, -1f, 1));
		Vector4 left = m.times(new Vector4(-0.5f, 0, -1f, 1));
		assertEquals(1f, right.getX() / right.getW(), 1e-3f);
		assertEquals(-1f, left.getX() / left.getW(), 1e-3f);
	}

	private static void assertFinite(Vector4 v) {
		assertFalse("NaN or Infinity in " + v, Float.isNaN(v.getX()) || Float.isNaN(v.getY()) || Float.isNaN(v.getZ()) || Float.isNaN(v.getW())
				|| Float.isInfinite(v.getX()) || Float.isInfinite(v.getY()) || Float.isInfinite(v.getZ()) || Float.isInfinite(v.getW()));
	}
}
