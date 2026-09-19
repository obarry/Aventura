package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.transform.NotARotationException;
import com.aventura.math.transform.Rotation;

/**
 * ------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2016-2026 Olivier BARRY
 * ------------------------------------------------------------------------------
 */
public class TestQuaternion {

	private static final float DELTA = 0.0001f;

	@Test
	public void testIdentity() {
		System.out.println("***** Test Quaternion : identity Quaternion is (0,0,0,1) *****");
		Quaternion q = new Quaternion();
		assertEquals(0f, q.getX(), 0f);
		assertEquals(0f, q.getY(), 0f);
		assertEquals(0f, q.getZ(), 0f);
		assertEquals(1f, q.getW(), 0f);
	}

	@Test
	public void testIdentity_toMatrix4IsIdentity() {
		System.out.println("***** Test Quaternion : identity Quaternion converts to identity Matrix4 *****");
		Quaternion q = new Quaternion();
		assertTrue(q.toMatrix4().isIdentity());
	}

	@Test
	public void testConstructor_fourArgs() {
		System.out.println("***** Test Quaternion : 4-arg constructor (x,y,z,w order) *****");
		Quaternion q = new Quaternion(1f, 2f, 3f, 4f);
		assertEquals(1f, q.getX(), 0f);
		assertEquals(2f, q.getY(), 0f);
		assertEquals(3f, q.getZ(), 0f);
		assertEquals(4f, q.getW(), 0f);
	}

	@Test
	public void testCopyConstructor() {
		System.out.println("***** Test Quaternion : copy constructor *****");
		Quaternion q1 = new Quaternion(1f, 2f, 3f, 4f);
		Quaternion q2 = new Quaternion(q1);
		assertTrue(q1.equals(q2));
		// Verify it's a real copy, not aliased
		q2.setX(99f);
		assertEquals(1f, q1.getX(), 0f);
	}

	@Test
	public void testGetSet_byIndex() throws IndexOutOfBoundException {
		System.out.println("***** Test Quaternion : get(i)/set(i,v) index order matches x,y,z,w *****");
		Quaternion q = new Quaternion();
		q.set(0, 10f);
		q.set(1, 20f);
		q.set(2, 30f);
		q.set(3, 40f);
		assertEquals(10f, q.get(0), 0f);
		assertEquals(20f, q.get(1), 0f);
		assertEquals(30f, q.get(2), 0f);
		assertEquals(40f, q.get(3), 0f);
		assertEquals(q.getX(), q.get(0), 0f);
		assertEquals(q.getW(), q.get(3), 0f);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testGet_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Quaternion : get(4) out of bound must throw *****");
		new Quaternion().get(4);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testSet_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Quaternion : set(4, val) out of bound must throw *****");
		new Quaternion().set(4, 1f);
	}

	@Test
	public void testLength_ofIdentityIsOne() {
		System.out.println("***** Test Quaternion : length() of identity Quaternion is 1 *****");
		assertEquals(1f, new Quaternion().length(), DELTA);
	}

	@Test
	public void testLengthSquared_matchesLengthSquared() {
		System.out.println("***** Test Quaternion : lengthSquared() is length()^2 *****");
		Quaternion q = new Quaternion(1f, 2f, 2f, 4f); // length = sqrt(1+4+4+16) = 5
		assertEquals(25f, q.lengthSquared(), DELTA);
		assertEquals(5f, q.length(), DELTA);
	}

	@Test
	public void testNormalize() {
		System.out.println("***** Test Quaternion : normalize() makes length 1, same direction *****");
		Quaternion q = new Quaternion(0f, 0f, 0f, 5f);
		q.normalize();
		assertEquals(1f, q.length(), DELTA);
		assertEquals(1f, q.getW(), DELTA);
	}

	@Test
	public void testConjugate() {
		System.out.println("***** Test Quaternion : conjugate() negates x,y,z, keeps w *****");
		Quaternion q = new Quaternion(1f, 2f, 3f, 4f);
		Quaternion c = q.conjugate();
		assertEquals(-1f, c.getX(), 0f);
		assertEquals(-2f, c.getY(), 0f);
		assertEquals(-3f, c.getZ(), 0f);
		assertEquals(4f, c.getW(), 0f);
		// Original must not be modified
		assertEquals(1f, q.getX(), 0f);
	}

	@Test
	public void testInverse_ofUnitQuaternionEqualsConjugate() {
		System.out.println("***** Test Quaternion : inverse() of a unit Quaternion equals conjugate() *****");
		Quaternion q = new Quaternion(Vector3.xAxis(), (float)Math.PI/3);
		Quaternion inv = q.inverse();
		Quaternion conj = q.conjugate();
		assertTrue(inv.equals(conj));
	}

	@Test
	public void testInverse_timesOriginalIsIdentity() {
		System.out.println("***** Test Quaternion : q * q.inverse() == identity *****");
		Quaternion q = new Quaternion(Vector3.yAxis(), (float)Math.PI/5);
		Quaternion result = q.times(q.inverse());
		assertTrue(result.equals(new Quaternion()));
	}

	@Test
	public void testTimes_withIdentityIsUnchanged() {
		System.out.println("***** Test Quaternion : q * identity == q *****");
		Quaternion q = new Quaternion(Vector3.zAxis(), (float)Math.PI/4);
		Quaternion result = q.times(new Quaternion());
		assertTrue(result.equals(q));
	}

	@Test
	public void testTimesEquals_matchesTimes() {
		System.out.println("***** Test Quaternion : timesEquals(q) matches times(q) *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		Quaternion q2 = new Quaternion(Vector3.yAxis(), (float)Math.PI/7);
		Quaternion expected = q1.times(q2);
		q1.timesEquals(q2);
		assertTrue(expected.equals(q1));
	}

	@Test
	public void testDot_ofIdenticalUnitQuaternionIsOne() {
		System.out.println("***** Test Quaternion : dot(q,q) == 1 for a unit Quaternion *****");
		Quaternion q = new Quaternion(Vector3.xAxis(), (float)Math.PI/3);
		assertEquals(1f, q.dot(q), DELTA);
	}

	@Test
	public void testAxisAngleConstructor_matchesRotationClass() {
		System.out.println("***** Test Quaternion : axis-angle constructor matches Rotation(angle, axis) *****");
		float angle = (float)Math.PI/3;
		Quaternion q = new Quaternion(Vector3.xAxis(), angle);
		Rotation r = new Rotation(angle, Vector3.xAxis());
		assertTrue(q.toMatrix4().equals(r));
	}

	@Test
	public void testAxisAngleConstructor_matchesRotationClass_yAxis() {
		System.out.println("***** Test Quaternion : axis-angle constructor matches Rotation(angle, axis) - Y axis *****");
		float angle = (float)Math.PI/4;
		Quaternion q = new Quaternion(Vector3.yAxis(), angle);
		Rotation r = new Rotation(angle, Vector3.yAxis());
		assertTrue(q.toMatrix4().equals(r));
	}

	@Test
	public void testAxisAngleConstructor_matchesRotationClass_zAxis() {
		System.out.println("***** Test Quaternion : axis-angle constructor matches Rotation(angle, axis) - Z axis *****");
		float angle = (float)Math.PI/5;
		Quaternion q = new Quaternion(Vector3.zAxis(), angle);
		Rotation r = new Rotation(angle, Vector3.zAxis());
		assertTrue(q.toMatrix4().equals(r));
	}

	@Test
	public void testToMatrix3_matchesToMatrix4UpperLeft() {
		System.out.println("***** Test Quaternion : toMatrix3() matches the upper-left 3x3 of toMatrix4() *****");
		Quaternion q = new Quaternion(Vector3.xAxis(), (float)Math.PI/3);
		Matrix3 m3 = q.toMatrix3();
		Matrix4 m4 = q.toMatrix4();
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				assertEquals(m4.get(i,j), m3.get(i,j), DELTA);
			}
		}
	}

	@Test
	public void testMatrix3Constructor_roundTrip() {
		System.out.println("***** Test Quaternion : Quaternion(Matrix3) round-trips through toMatrix3() *****");
		Quaternion original = new Quaternion(Vector3.xAxis(), (float)Math.PI/3);
		Matrix3 m = original.toMatrix3();
		Quaternion rebuilt = new Quaternion(m);
		// May differ by an overall sign (q and -q represent the same rotation matrix) -- compare the
		// matrices they produce rather than the raw components.
		assertTrue(original.toMatrix3().equals(rebuilt.toMatrix3()));
	}

	@Test
	public void testMatrix4Constructor_roundTrip() {
		System.out.println("***** Test Quaternion : Quaternion(Matrix4) round-trips through toMatrix4() *****");
		Quaternion original = new Quaternion(Vector3.yAxis(), (float)Math.PI/4);
		Matrix4 m = original.toMatrix4();
		Quaternion rebuilt = new Quaternion(m);
		assertTrue(original.toMatrix4().equals(rebuilt.toMatrix4()));
	}

	@Test
	public void testMatrix3Constructor_matchesMatrix4Constructor() {
		System.out.println("***** Test Quaternion : Quaternion(Matrix3) and Quaternion(Matrix4) agree on the same rotation *****");
		float angle = (float)Math.PI/3;
		Rotation r = new Rotation(angle, Vector3.zAxis());

		float[][] upperLeft3x3 = new float[3][3];
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				upperLeft3x3[i][j] = r.get(i,j);
			}
		}
		Matrix3 m3 = new Matrix3(upperLeft3x3);

		Quaternion fromMatrix3 = new Quaternion(m3);
		Quaternion fromMatrix4 = new Quaternion(r);

		// May differ by an overall sign (q and -q represent the same rotation) -- compare the
		// rotation matrices they produce rather than the raw components.
		assertTrue(fromMatrix3.toMatrix4().equals(fromMatrix4.toMatrix4()));
	}

	@Test
	public void testToAxisAngle_roundTrip() {
		System.out.println("***** Test Quaternion : toAxisAngle() round-trips the original axis and angle *****");
		Vector3 originalAxis = Vector3.xAxis();
		float originalAngle = (float)Math.PI/3;
		Quaternion q = new Quaternion(originalAxis, originalAngle);

		Vector3 recoveredAxis = new Vector3();
		float recoveredAngle = q.toAxisAngle(recoveredAxis);

		assertEquals(originalAngle, recoveredAngle, DELTA);
		assertTrue(originalAxis.equals(recoveredAxis));
	}

	@Test
	public void testToAxisAngle_identityHasZeroAngle() {
		System.out.println("***** Test Quaternion : toAxisAngle() of identity Quaternion is angle 0 *****");
		Quaternion q = new Quaternion();
		Vector3 axis = new Vector3();
		float angle = q.toAxisAngle(axis);
		assertEquals(0f, angle, DELTA);
	}

	@Test
	public void testSlerp_atZeroReturnsStart() {
		System.out.println("***** Test Quaternion : slerp(q1, q2, 0) == q1 *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		Quaternion q2 = new Quaternion(Vector3.xAxis(), (float)Math.PI/2);
		Quaternion result = Quaternion.slerp(q1, q2, 0f);
		assertTrue(q1.equals(result));
	}

	@Test
	public void testSlerp_atOneReturnsEnd() {
		System.out.println("***** Test Quaternion : slerp(q1, q2, 1) == q2 *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		Quaternion q2 = new Quaternion(Vector3.xAxis(), (float)Math.PI/2);
		Quaternion result = Quaternion.slerp(q1, q2, 1f);
		assertTrue(q2.equals(result));
	}

	@Test
	public void testSlerp_atHalfway_sameAxis_isAverageAngle() {
		System.out.println("***** Test Quaternion : slerp(q1, q2, 0.5) on the same axis is the average angle *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		Quaternion q2 = new Quaternion(Vector3.xAxis(), (float)Math.PI/2);
		Quaternion expected = new Quaternion(Vector3.xAxis(), ((float)Math.PI/6 + (float)Math.PI/2)/2);
		Quaternion result = Quaternion.slerp(q1, q2, 0.5f);
		assertTrue(expected.equals(result));
	}

	@Test
	public void testSlerp_resultIsUnitLength() {
		System.out.println("***** Test Quaternion : slerp() result is always unit length *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		Quaternion q2 = new Quaternion(Vector3.yAxis(), (float)Math.PI/2);
		Quaternion result = Quaternion.slerp(q1, q2, 0.37f);
		assertEquals(1f, result.length(), DELTA);
	}

	@Test
	public void testSlerp_takesShorterPath() {
		System.out.println("***** Test Quaternion : slerp() takes the shorter path when dot product is negative *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		// -q1 represents the exact same rotation as q1, but dot(q1, -q1) = -1: slerp must still
		// produce a valid, unit-length interpolation rather than blowing up or taking the long way.
		Quaternion negQ1 = new Quaternion(-q1.getX(), -q1.getY(), -q1.getZ(), -q1.getW());
		Quaternion q2 = new Quaternion(Vector3.xAxis(), (float)Math.PI/2);
		Quaternion result = Quaternion.slerp(negQ1, q2, 0.5f);
		assertEquals(1f, result.length(), DELTA);
		// The rotation it represents should still land at the same matrix as slerping q1->q2 directly
		Quaternion directResult = Quaternion.slerp(q1, q2, 0.5f);
		assertTrue(directResult.toMatrix4().equals(result.toMatrix4()));
	}

	@Test
	public void testSlerp_nearIdenticalQuaternions_fallsBackToLerp() {
		System.out.println("***** Test Quaternion : slerp() of two near-identical Quaternions does not NaN (lerp fallback) *****");
		Quaternion q1 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6);
		Quaternion q2 = new Quaternion(Vector3.xAxis(), (float)Math.PI/6 + 0.00001f);
		Quaternion result = Quaternion.slerp(q1, q2, 0.5f);
		assertFalse(Float.isNaN(result.getX()));
		assertFalse(Float.isNaN(result.getW()));
		assertEquals(1f, result.length(), DELTA);
	}

	@Test
	public void testEquals_negativeCase() {
		System.out.println("***** Test Quaternion : equals negative case *****");
		Quaternion q1 = new Quaternion(1f, 2f, 3f, 4f);
		Quaternion q2 = new Quaternion(9f, 9f, 9f, 9f);
		assertFalse(q1.equals(q2));
	}

	@Test
	public void testEquals_object_negativeCase_null() {
		System.out.println("***** Test Quaternion : equals(Object) with null and a different type *****");
		Quaternion q1 = new Quaternion(1f, 2f, 3f, 4f);
		Object nullObj = null;
		assertFalse(q1.equals(nullObj));
		assertFalse(q1.equals("not a quaternion"));
	}

	@Test
	public void testHashCode_consistentWithEquals() {
		System.out.println("***** Test Quaternion : equal Quaternions have the same hashCode *****");
		Quaternion q1 = new Quaternion(1f, 2f, 3f, 4f);
		Quaternion q2 = new Quaternion(1f, 2f, 3f, 4f);
		assertTrue(q1.equals(q2));
		assertEquals(q1.hashCode(), q2.hashCode());
	}

	@Test
	public void testRotationConstructor_fromQuaternion() throws NotARotationException {
		System.out.println("***** Test Quaternion : Rotation(Quaternion) constructor matches Rotation(angle, axis) *****");
		float angle = (float)Math.PI/4;
		Quaternion q = new Quaternion(Vector3.yAxis(), angle);
		Rotation fromQuaternion = new Rotation(q);
		Rotation fromAngleAxis = new Rotation(angle, Vector3.yAxis());
		assertTrue(fromQuaternion.equals(fromAngleAxis));
	}

}
