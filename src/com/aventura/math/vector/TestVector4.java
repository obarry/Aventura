package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * New test class (Vector4 had no dedicated test coverage before this pass, only indirectly
 * exercised through TestVector3Matrix3 which only covers Vector3/Matrix3).
 */
public class TestVector4 {

	@Test
	public void testVector4_constructors_basic() {
		System.out.println("***** Test Vector4 : basic constructors *****");

		Vector4 v0 = new Vector4();
		assertEquals(0f, v0.getX(), 0f);
		assertEquals(0f, v0.getY(), 0f);
		assertEquals(0f, v0.getZ(), 0f);
		assertEquals(0f, v0.getW(), 0f);

		Vector4 v1 = new Vector4(2f);
		assertEquals(2f, v1.getX(), 0f);
		assertEquals(2f, v1.getW(), 0f);

		Vector4 v2 = new Vector4(1f, 2f, 3f, 1f);
		assertEquals(1f, v2.getX(), 0f);
		assertEquals(2f, v2.getY(), 0f);
		assertEquals(3f, v2.getZ(), 0f);
		assertEquals(1f, v2.getW(), 0f);

		Vector4 v3 = new Vector4(v2);
		if (!v2.equals(v3)) fail("Copy constructor should produce an equal Vector4");
		if (v2 == v3) fail("Copy constructor should produce a distinct instance");
	}

	@Test
	public void testVector4_constructor_array_ok() throws VectorArrayWrongSizeException {
		System.out.println("***** Test Vector4 : array constructor (valid size) *****");

		float[] array = {1f, 2f, 3f, 4f};
		Vector4 v = new Vector4(array);
		assertEquals(1f, v.getX(), 0f);
		assertEquals(4f, v.getW(), 0f);
	}

	@Test(expected = VectorArrayWrongSizeException.class)
	public void testVector4_constructor_array_tooShort() throws VectorArrayWrongSizeException {
		System.out.println("***** Test Vector4 : array constructor (too short, must throw) *****");

		float[] array = {1f, 2f, 3f}; // only 3 elements, Vector4 needs 4
		new Vector4(array);
	}

	@Test
	public void testVector4_constructor_fromVector3() {
		System.out.println("***** Test Vector4 : constructor from Vector3 *****");

		Vector3 v3 = new Vector3(1f, 2f, 3f);
		Vector4 v4 = new Vector4(v3);
		assertEquals(1f, v4.getX(), 0f);
		assertEquals(2f, v4.getY(), 0f);
		assertEquals(3f, v4.getZ(), 0f);
		assertEquals(0f, v4.getW(), 0f); // built from a Vector3: w defaults to 0 (a Vector, not a Point)
	}

	@Test
	public void testVector4_constructor_fromTwoPoints() {
		System.out.println("***** Test Vector4 : constructor from two points (a,b) *****");

		Vector4 a = new Vector4(1f, 1f, 1f, 1f);
		Vector4 b = new Vector4(4f, 5f, 6f, 1f);
		Vector4 ab = new Vector4(a, b);

		assertEquals(3f, ab.getX(), 0f);
		assertEquals(4f, ab.getY(), 0f);
		assertEquals(5f, ab.getZ(), 0f);
		assertEquals(0f, ab.getW(), 0f); // both points have w=1, so w difference is 0: ab is a direction, not a point
	}

	@Test
	public void testVector4_constructor_fromMatrixRowColumn() {
		System.out.println("***** Test Vector4 : constructor from Matrix4 row/column *****");

		Matrix4 m = new Matrix4(Matrix4.IDENTITY);
		Vector4 row0 = new Vector4(0, m);
		assertEquals(1f, row0.getX(), 0f);
		assertEquals(0f, row0.getY(), 0f);

		Vector4 col0 = new Vector4(m, 0);
		assertEquals(1f, col0.getX(), 0f);
		assertEquals(0f, col0.getY(), 0f);
	}

	@Test
	public void testVector4_indexedGetSet_valid() throws IndiceOutOfBoundException {
		System.out.println("***** Test Vector4 : indexed get/set (valid indices) *****");

		Vector4 v = new Vector4();
		v.set(0, 1f);
		v.set(1, 2f);
		v.set(2, 3f);
		v.set(3, 4f);

		assertEquals(1f, v.get(0), 0f);
		assertEquals(2f, v.get(1), 0f);
		assertEquals(3f, v.get(2), 0f);
		assertEquals(4f, v.get(3), 0f);
	}

	@Test(expected = IndiceOutOfBoundException.class)
	public void testVector4_get_invalidIndex_throws() throws IndiceOutOfBoundException {
		System.out.println("***** Test Vector4 : get(4) out of bound must throw *****");

		Vector4 v = new Vector4();
		v.get(4); // valid indices are 0..3
	}

	@Test(expected = IndiceOutOfBoundException.class)
	public void testVector4_set_invalidIndex_throws() throws IndiceOutOfBoundException {
		System.out.println("***** Test Vector4 : set(4, val) out of bound must throw *****");

		Vector4 v = new Vector4();
		v.set(4, 1f); // valid indices are 0..3
	}

	@Test
	public void testVector4_get3DConversions() {
		System.out.println("***** Test Vector4 : get3DX/Y/Z and get3DPoint *****");

		Vector4 v = new Vector4(4f, 6f, 8f, 2f);
		assertEquals(2f, v.get3DX(), 0.00001f);
		assertEquals(3f, v.get3DY(), 0.00001f);
		assertEquals(4f, v.get3DZ(), 0.00001f);

		Vector3 p = v.get3DPoint();
		assertEquals(2f, p.getX(), 0.00001f);
		assertEquals(3f, p.getY(), 0.00001f);
		assertEquals(4f, p.getZ(), 0.00001f);
	}

	@Test
	public void testVector4_get3DPoint_nullWhenW0() {
		System.out.println("***** Test Vector4 : get3DPoint returns null when w=0 (a Vector, not a Point) *****");

		Vector4 v = new Vector4(1f, 2f, 3f, 0f);
		assertNull(v.get3DPoint());
	}

	@Test
	public void testVector4_length_normalize() {
		System.out.println("***** Test Vector4 : length/normalize *****");

		Vector4 v = new Vector4(1f, 2f, 2f, 0f);
		assertEquals(3f, v.length(), 0.00001f); // sqrt(1+4+4) = 3

		v.normalize();
		assertEquals(1f, v.length(), 0.00001f);
	}

	@Test
	public void testVector4_equals() {
		System.out.println("***** Test Vector4 : equals *****");

		Vector4 v1 = new Vector4(1f, 2f, 3f, 4f);
		Vector4 v2 = new Vector4(1f, 2f, 3f, 4f);
		Vector4 v3 = new Vector4(9f, 9f, 9f, 9f);

		if (!v1.equals(v2)) fail("v1 should equal v2");
		if (v1.equals(v3)) fail("v1 should not equal v3");
	}

	@Test
	public void testVector4_plusMinus_vector4() {
		System.out.println("***** Test Vector4 : plus/minus/plusEquals/minusEquals with Vector4 *****");

		Vector4 v1 = new Vector4(1f, 2f, 3f, 1f);
		Vector4 v2 = new Vector4(4f, 5f, 6f, 0f);

		Vector4 sum = v1.plus(v2);
		assertEquals(5f, sum.getX(), 0f);
		assertEquals(7f, sum.getY(), 0f);
		assertEquals(9f, sum.getZ(), 0f);
		assertEquals(1f, sum.getW(), 0f);

		Vector4 diff = v1.minus(v2);
		assertEquals(-3f, diff.getX(), 0f);

		v1.plusEquals(v2);
		if (!v1.equals(sum)) fail("v1 after plusEquals should equal sum");
	}

	@Test
	public void testVector4_plusMinus_vector3_movesPointKeepingW() {
		System.out.println("***** Test Vector4 : plus(Vector3)/minus(Vector3) move a point, keep w unchanged *****");

		Vector4 point = new Vector4(1f, 1f, 1f, 1f); // a Point
		Vector3 displacement = new Vector3(2f, 3f, 4f);

		Vector4 moved = point.plus(displacement);
		assertEquals(3f, moved.getX(), 0f);
		assertEquals(4f, moved.getY(), 0f);
		assertEquals(5f, moved.getZ(), 0f);
		assertEquals(1f, moved.getW(), 0f); // w must be left unchanged (still a Point)

		Vector4 movedBack = moved.minus(displacement);
		if (!movedBack.equals(point)) fail("minus(Vector3) should undo plus(Vector3)");
	}

	@Test
	public void testVector4_dot() {
		System.out.println("***** Test Vector4 : dot *****");

		Vector4 v1 = new Vector4(1f, 2f, 3f, 4f);
		Vector4 v2 = new Vector4(5f, 6f, 7f, 8f);
		assertEquals(70f, v1.dot(v2), 0.00001f); // 5+12+21+32
	}

	@Test
	public void testVector4_timesScalar() {
		System.out.println("***** Test Vector4 : times/timesEquals scalar *****");

		Vector4 v = new Vector4(1f, 2f, 3f, 4f);
		Vector4 r = v.times(2f);
		assertEquals(2f, r.getX(), 0f);
		assertEquals(8f, r.getW(), 0f);

		v.timesEquals(2f);
		if (!v.equals(r)) fail("v after timesEquals(2) should equal r");
	}

	@Test
	public void testVector4_crossProduct_forcesW0() {
		System.out.println("***** Test Vector4 : times(Vector4) cross product forces w=0 *****");

		Vector4 x = new Vector4(1f, 0f, 0f, 0f);
		Vector4 y = new Vector4(0f, 1f, 0f, 0f);
		Vector4 z = x.times(y);

		assertEquals(0f, z.getX(), 0.00001f);
		assertEquals(0f, z.getY(), 0.00001f);
		assertEquals(1f, z.getZ(), 0.00001f);
		assertEquals(0f, z.getW(), 0f);
	}

	@Test
	public void testVector4_timesMatrix4_identity() {
		System.out.println("***** Test Vector4 : times(Matrix4) with Identity leaves the vector unchanged *****");

		Vector4 v = new Vector4(3f, -2f, 5f, 1f);
		Vector4 r = v.times(Matrix4.IDENTITY);
		if (!r.equals(v)) fail("v * Identity should equal v");

		v.timesEquals(Matrix4.IDENTITY);
		if (!v.equals(r)) fail("v after timesEquals(Identity) should equal r");
	}

	@Test
	public void testVector4_V3_conversion() {
		System.out.println("***** Test Vector4 : V3() conversion *****");

		Vector4 v4 = new Vector4(1f, 2f, 3f, 9f);
		Vector3 v3 = v4.V3();
		assertEquals(1f, v3.getX(), 0f);
		assertEquals(2f, v3.getY(), 0f);
		assertEquals(3f, v3.getZ(), 0f); // w is dropped
	}

	@Test
	public void testVector4_pointVectorSemantics() {
		System.out.println("***** Test Vector4 : isPoint/isVector/point/vector/setVector *****");

		Vector4 v = new Vector4(1f, 2f, 3f, 0f);
		assertTrue(v.isVector());
		assertFalse(v.isPoint());

		v.point();
		assertTrue(v.isPoint());
		assertFalse(v.isVector());

		v.vector();
		assertTrue(v.isVector());

		v.point();
		assertTrue(v.isPoint());

		@SuppressWarnings("deprecation")
		Vector4 same = v; // just to scope the deprecation suppression tightly
		same.setVector(); // deprecated, must still behave exactly like vector()
		assertTrue(v.isVector());
		assertEquals(0f, v.getW(), 0f);
	}

	@Test
	public void testVector4_lengthSquared() {
		System.out.println("***** Test Vector4 : lengthSquared (new method) *****");

		Vector4 v = new Vector4(1f, 2f, 2f, 0f);
		assertEquals(9f, v.lengthSquared(), 0.00001f); // 1+4+4
		assertEquals(v.length()*v.length(), v.lengthSquared(), 0.001f);
	}

	@Test
	public void testVector4_distance_distanceSquared() {
		System.out.println("***** Test Vector4 : distance/distanceSquared (new methods) *****");

		Vector4 v1 = new Vector4(0f, 0f, 0f, 0f);
		Vector4 v2 = new Vector4(1f, 2f, 2f, 0f);

		assertEquals(9f, v1.distanceSquared(v2), 0.00001f);
		assertEquals(3f, v1.distance(v2), 0.00001f);

		// Consistency with the allocating equivalent v1.minus(v2).length()
		assertEquals(v1.minus(v2).length(), v1.distance(v2), 0.0001f);
	}

	@Test
	public void testVector4_toArray() {
		System.out.println("***** Test Vector4 : toArray/toArray(dest) (new methods) *****");

		Vector4 v = new Vector4(1f, 2f, 3f, 4f);
		float[] a = v.toArray();
		assertArrayEquals(new float[]{1f,2f,3f,4f}, a, 0.00001f);

		float[] dest = new float[4];
		float[] r = v.toArray(dest);
		assertSame(dest, r); // must fill and return the same array, not allocate a new one
		assertArrayEquals(new float[]{1f,2f,3f,4f}, dest, 0.00001f);
	}
}
