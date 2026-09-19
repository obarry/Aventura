package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * New test class (GeometryTools had no test coverage before this pass).
 * Also locks in the empty-array robustness fix from the audit (center() used to silently
 * return a NaN/Infinity-laden point on an empty array instead of a clear null).
 */
public class TestGeometryTools {

	@Test
	public void testCenter_monoDimensional_normalCase() {
		System.out.println("***** Test GeometryTools : center(Vector4[]) normal case *****");

		Vector4[] points = new Vector4[] {
			new Vector4(0f, 0f, 0f, 1f),
			new Vector4(2f, 0f, 0f, 1f),
			new Vector4(0f, 2f, 0f, 1f),
			new Vector4(0f, 0f, 2f, 1f)
		};
		Vector4 center = GeometryTools.center(points);
		assertEquals(0.5f, center.getX(), 0.00001f);
		assertEquals(0.5f, center.getY(), 0.00001f);
		assertEquals(0.5f, center.getZ(), 0.00001f);
		assertEquals(1f, center.getW(), 0.00001f); // center of points must remain a Point (w=1)
	}

	@Test
	public void testCenter_monoDimensional_singlePoint() {
		System.out.println("***** Test GeometryTools : center(Vector4[]) single point *****");

		Vector4[] points = new Vector4[] { new Vector4(3f, 4f, 5f, 1f) };
		Vector4 center = GeometryTools.center(points);
		assertEquals(3f, center.getX(), 0.00001f);
		assertEquals(4f, center.getY(), 0.00001f);
		assertEquals(5f, center.getZ(), 0.00001f);
	}

	@Test
	public void testCenter_monoDimensional_emptyArray_returnsNull() {
		System.out.println("***** Test GeometryTools : center(empty Vector4[]) returns null (bug fix regression) *****");

		Vector4 center = GeometryTools.center(new Vector4[0]);
		assertNull(center);
	}

	@Test
	public void testCenter_monoDimensional_null_returnsNull() {
		System.out.println("***** Test GeometryTools : center(null Vector4[]) returns null *****");

		Vector4 center = GeometryTools.center((Vector4[]) null);
		assertNull(center);
	}

	@Test
	public void testCenter_biDimensional_normalCase() {
		System.out.println("***** Test GeometryTools : center(Vector4[][]) normal case *****");

		Vector4[][] points = new Vector4[][] {
			{ new Vector4(0f, 0f, 0f, 1f), new Vector4(4f, 0f, 0f, 1f) },
			{ new Vector4(0f, 4f, 0f, 1f), new Vector4(0f, 0f, 4f, 1f) }
		};
		Vector4 center = GeometryTools.center(points);
		assertEquals(1f, center.getX(), 0.00001f);
		assertEquals(1f, center.getY(), 0.00001f);
		assertEquals(1f, center.getZ(), 0.00001f);
	}

	@Test
	public void testCenter_biDimensional_emptyOuterArray_returnsNull() {
		System.out.println("***** Test GeometryTools : center(empty outer Vector4[][]) returns null (bug fix regression) *****");

		Vector4 center = GeometryTools.center(new Vector4[0][0]);
		assertNull(center);
	}

	@Test
	public void testCenter_biDimensional_emptyInnerArray_returnsNull() {
		System.out.println("***** Test GeometryTools : center(Vector4[][] with an empty first row) returns null (bug fix regression) *****");

		Vector4[][] points = new Vector4[][] { new Vector4[0] };
		Vector4 center = GeometryTools.center(points);
		assertNull(center);
	}

	@Test
	public void testCenter_biDimensional_null_returnsNull() {
		System.out.println("***** Test GeometryTools : center(null Vector4[][]) returns null *****");

		Vector4 center = GeometryTools.center((Vector4[][]) null);
		assertNull(center);
	}
}
