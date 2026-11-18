package com.aventura.model.world.triangle;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;

/**
 * Tests of Triangle.isInViewFrustum(): outcode trivial rejection in homogeneous clip coordinates (a triangle is
 * rejected only if its 3 vertices are outside the same plane of the view frustum).
 */
public class TestTriangleViewFrustum {

	/** A triangle whose vertices have the given clip coordinates (x, y, z, w) */
	private static Triangle clipTriangle(float[] a, float[] b, float[] c) {
		Vertex v1 = new Vertex(), v2 = new Vertex(), v3 = new Vertex();
		v1.setProjPos(new Vector4(a[0], a[1], a[2], a[3]));
		v2.setProjPos(new Vector4(b[0], b[1], b[2], b[3]));
		v3.setProjPos(new Vector4(c[0], c[1], c[2], c[3]));
		return new Triangle(v1, v2, v3);
	}

	@Test
	public void testOutcode_insideIsZeroAndEachPlaneHasItsBit() {
		System.out.println("***** Test Triangle : outcode() is 0 inside the frustum, one bit per plane outside *****");
		assertEquals(0, Triangle.outcode(new Vector4(0.5f, -0.5f, 0f, 1f)));
		assertEquals(0, Triangle.outcode(new Vector4(2f, 2f, 2f, 2f))); // On the edges (x = y = z = w): inside
		assertEquals(1, Triangle.outcode(new Vector4(-3f, 0f, 0f, 2f)));
		assertEquals(2, Triangle.outcode(new Vector4(3f, 0f, 0f, 2f)));
		assertEquals(4, Triangle.outcode(new Vector4(0f, -3f, 0f, 2f)));
		assertEquals(8, Triangle.outcode(new Vector4(0f, 3f, 0f, 2f)));
		assertEquals(16, Triangle.outcode(new Vector4(0f, 0f, -3f, 2f)));
		assertEquals(32, Triangle.outcode(new Vector4(0f, 0f, 3f, 2f)));
		assertEquals(1 | 8, Triangle.outcode(new Vector4(-3f, 3f, 0f, 2f)));
	}

	@Test
	public void testInViewFrustum_triangleWithAVertexInside() {
		System.out.println("***** Test Triangle : a triangle with a vertex inside the frustum is kept *****");
		assertTrue(clipTriangle(new float[] { 0, 0, 0, 1 }, new float[] { 5, 0, 0, 1 }, new float[] { 0, 5, 0, 1 }).isInViewFrustum());
	}

	@Test
	public void testInViewFrustum_largeTriangleCoveringTheViewWithAllVerticesOutside() {
		System.out.println("***** Test Triangle : a large triangle covering the whole view, its 3 vertices outside, is kept *****");
		// Vertices left, right and above the view, each one outside a different plane: the triangle covers the center
		Triangle t = clipTriangle(new float[] { -10, -10, 0, 1 }, new float[] { 10, -10, 0, 1 }, new float[] { 0, 10, 0, 1 });
		assertTrue("used to be rejected (no vertex inside)", t.isInViewFrustum());
	}

	@Test
	public void testInViewFrustum_triangleOutsideOnePlaneIsRejected() {
		System.out.println("***** Test Triangle : a triangle with its 3 vertices outside the same plane is rejected *****");
		assertFalse(clipTriangle(new float[] { 2, 0, 0, 1 }, new float[] { 5, -3, 0, 1 }, new float[] { 3, 4, 0, 1 }).isInViewFrustum()); // Right
		assertFalse(clipTriangle(new float[] { 0, 0, -2, 1 }, new float[] { 0.5f, 0, -3, 1 }, new float[] { 0, 0.5f, -5, 1 }).isInViewFrustum()); // Near
	}

	@Test
	public void testInViewFrustum_triangleBehindTheEye() {
		System.out.println("***** Test Triangle : a triangle behind the eye (w < 0) is rejected, one crossing the near plane is kept *****");
		// Behind the eye under a frustum perspective: w < 0 and z < -w (outside the near plane), whatever x and y
		assertFalse(clipTriangle(new float[] { 0, 0, -3, -1 }, new float[] { 1, 0, -4, -2 }, new float[] { 0, 1, -3, -1.5f }).isInViewFrustum());
		// One vertex in front, inside, two behind: kept (the near plane clipper cuts it)
		assertTrue(clipTriangle(new float[] { 0, 0, 0, 1 }, new float[] { 1, 0, -4, -2 }, new float[] { 0, 1, -3, -1.5f }).isInViewFrustum());
	}
}
