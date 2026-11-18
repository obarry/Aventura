package com.aventura.model.perspective;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;

/**
 * Tests for Perspective.getFrustumFromEye() (both perspective types, symmetric and asymmetric volumes),
 * getType() and copy().
 * The camera is at the origin looking towards -Z with Y up, so eye space == world space
 * (side = forward x up = +X).
 */
public class TestPerspective {

	private static final float DELTA = 1e-4f;

	private static Camera axisCamera() {
		return new Camera(new Vector4(0, 0, 0, 1), new Vector4(0, 0, -1, 1), new Vector4(0, 1, 0, 0));
	}

	private static void assertPoint(float x, float y, float z, Vector4 p) {
		assertEquals("x of " + p, x, p.getX(), DELTA);
		assertEquals("y of " + p, y, p.getY(), DELTA);
		assertEquals("z of " + p, z, p.getZ(), DELTA);
	}

	@Test
	public void testFrustum_symmetric() {
		Perspective p = new FrustumPerspective(0.8f, 0.4f, 1f, 9f); // near 1, far 10
		Vector4[][] f = p.getFrustumFromEye(axisCamera());
		assertPoint(0.4f, 0.2f, -1f, f[0][0]);
		assertPoint(-0.4f, 0.2f, -1f, f[0][1]);
		assertPoint(-0.4f, -0.2f, -1f, f[0][2]);
		assertPoint(0.4f, -0.2f, -1f, f[0][3]);
		// far plane is 10 times further: window 10 times larger (Thales)
		assertPoint(4f, 2f, -10f, f[1][0]);
		assertPoint(-4f, -2f, -10f, f[1][2]);
	}

	@Test
	public void testFrustum_asymmetric() {
		Perspective p = new FrustumPerspective(-0.3f, 0.6f, -0.4f, 0.5f, 1f, 10f);
		Vector4[][] f = p.getFrustumFromEye(axisCamera());
		assertPoint(0.6f, 0.5f, -1f, f[0][0]);
		assertPoint(-0.3f, -0.4f, -1f, f[0][2]);
		assertPoint(6f, 5f, -10f, f[1][0]);
		assertPoint(-3f, -4f, -10f, f[1][2]);
	}

	@Test
	public void testOrthographic_notNullAndParallel() {
		Perspective p = new OrthographicPerspective(8f, 6f, 1f, 99f); // near 1, far 100
		Vector4[][] f = p.getFrustumFromEye(axisCamera());
		assertNotNull(f); // used to return null
		assertPoint(4f, 3f, -1f, f[0][0]);
		assertPoint(4f, 3f, -100f, f[1][0]); // same window size at any distance
		assertPoint(-4f, -3f, -100f, f[1][2]);
	}

	@Test
	public void testTypeAndCopy() {
		Perspective f = new FrustumPerspective(0.8f, 0.4f, 1f, 9f);
		Perspective o = new OrthographicPerspective(8f, 6f, 1f, 99f);
		assertEquals(PerspectiveType.FRUSTUM, f.getType());
		assertEquals(PerspectiveType.ORTHOGRAPHIC, o.getType());

		Perspective c = o.copy();
		assertTrue(c instanceof OrthographicPerspective);
		assertNotSame(o, c);
		assertNotSame(o.getProjection(), c.getProjection());
		assertEquals(o.getProjection().toString(), c.getProjection().toString());
		c.setWidth(16f);
		assertEquals(8f, o.getWidth(), DELTA);
	}
}
