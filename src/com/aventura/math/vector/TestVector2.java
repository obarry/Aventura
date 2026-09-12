package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * New test class (Vector2 had no dedicated test coverage before this pass).
 * Mirrors the structure of TestVector3 as closely as Vector2's smaller service set allows.
 */
public class TestVector2 {

	@Test
	public void testVector2_constructors() {
		System.out.println("***** Test Vector2 : constructors *****");

		Vector2 v0 = new Vector2();
		assertEquals(0f, v0.getX(), 0f);
		assertEquals(0f, v0.getY(), 0f);

		Vector2 v1 = new Vector2(3f, 4f);
		assertEquals(3f, v1.getX(), 0f);
		assertEquals(4f, v1.getY(), 0f);

		Vector2 v2 = new Vector2(v1);
		if (!v1.equals(v2)) fail("Copy constructor should produce an equal Vector2");
		if (v1 == v2) fail("Copy constructor should produce a distinct instance");
	}

	@Test
	public void testVector2_settersGetters() {
		System.out.println("***** Test Vector2 : setters/getters *****");

		Vector2 v = new Vector2();
		v.setX(5f);
		v.setY(-2f);
		assertEquals(5f, v.getX(), 0f);
		assertEquals(-2f, v.getY(), 0f);
	}

	@Test
	public void testVector2_length() {
		System.out.println("***** Test Vector2 : length *****");

		Vector2 v = new Vector2(3f, 4f);
		assertEquals(5f, v.length(), 0.00001f);
	}

	@Test
	public void testVector2_lengthSquared() {
		System.out.println("***** Test Vector2 : lengthSquared (new method) *****");

		Vector2 v = new Vector2(3f, 4f);
		assertEquals(25f, v.lengthSquared(), 0.00001f);
		// Consistency check: lengthSquared must always equal length()^2
		assertEquals(v.length()*v.length(), v.lengthSquared(), 0.001f);
	}

	@Test
	public void testVector2_normalize() {
		System.out.println("***** Test Vector2 : normalize *****");

		Vector2 v = new Vector2(6f, -8f);
		v.normalize();
		assertEquals(1f, v.length(), 0.00001f);
		// direction must be preserved: still pointing in the same quadrant
		assertTrue(v.getX() > 0);
		assertTrue(v.getY() < 0);
	}

	@Test
	public void testVector2_dot() {
		System.out.println("***** Test Vector2 : dot *****");

		Vector2 v1 = new Vector2(1f, 0f);
		Vector2 v2 = new Vector2(0f, 1f);
		// Orthogonal vectors: dot product is 0
		assertEquals(0f, v1.dot(v2), 0.00001f);

		Vector2 v3 = new Vector2(2f, 3f);
		Vector2 v4 = new Vector2(4f, 5f);
		assertEquals(23f, v3.dot(v4), 0.00001f); // 2*4 + 3*5
	}

	@Test
	public void testVector2_timesScalar() {
		System.out.println("***** Test Vector2 : times/timesEquals scalar *****");

		Vector2 v = new Vector2(2f, -3f);
		Vector2 r = v.times(2f);
		assertEquals(4f, r.getX(), 0f);
		assertEquals(-6f, r.getY(), 0f);

		v.timesEquals(2f);
		assertEquals(4f, v.getX(), 0f);
		assertEquals(-6f, v.getY(), 0f);
	}

	@Test
	public void testVector2_plus() {
		System.out.println("***** Test Vector2 : plus/plusEquals *****");

		Vector2 v1 = new Vector2(1f, 2f);
		Vector2 v2 = new Vector2(3f, 4f);
		Vector2 v3 = v1.plus(v2);
		assertEquals(4f, v3.getX(), 0f);
		assertEquals(6f, v3.getY(), 0f);

		v1.plusEquals(v2);
		if (!v1.equals(v3)) fail("v1 after plusEquals should equal v3");
	}

	@Test
	public void testVector2_minus() {
		System.out.println("***** Test Vector2 : minus/minusEquals *****");

		Vector2 v1 = new Vector2(5f, 7f);
		Vector2 v2 = new Vector2(2f, 3f);
		Vector2 v3 = v1.minus(v2);
		assertEquals(3f, v3.getX(), 0f);
		assertEquals(4f, v3.getY(), 0f);

		v1.minusEquals(v2);
		if (!v1.equals(v3)) fail("v1 after minusEquals should equal v3");
	}

	@Test
	public void testVector2_equals_instance() {
		System.out.println("***** Test Vector2 : equals(Vector2) instance method (new method) *****");

		Vector2 v1 = new Vector2(1f, 2f);
		Vector2 v2 = new Vector2(1f, 2f);
		Vector2 v3 = new Vector2(9f, 9f);

		if (!v1.equals(v2)) fail("v1 should equal v2 (same coordinates)");
		if (v1.equals(v3)) fail("v1 should not equal v3 (different coordinates)");
	}

	@Test
	public void testVector2_equals_static() {
		System.out.println("***** Test Vector2 : static equals(v1,v2) *****");

		Vector2 v1 = new Vector2(1f, 2f);
		Vector2 v2 = new Vector2(1f, 2f);
		Vector2 v3 = new Vector2(9f, 9f);

		if (!Vector2.equals(v1, v2)) fail("Static equals should be true for equal vectors");
		if (Vector2.equals(v1, v3)) fail("Static equals should be false for different vectors");
	}

	@Test
	public void testVector2_copy() {
		System.out.println("***** Test Vector2 : copy() (new method) *****");

		Vector2 v1 = new Vector2(1f, 2f);
		Vector2 v2 = v1.copy();

		if (!v1.equals(v2)) fail("copy() should produce an equal Vector2");
		if (v1 == v2) fail("copy() should produce a distinct instance, not the same reference");

		// Mutating the copy must not affect the original (proves it's a real copy, not aliasing)
		v2.setX(999f);
		assertEquals(1f, v1.getX(), 0f);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testVector2_legacyEqualsNoArg_stillWorksAsCopy() {
		System.out.println("***** Test Vector2 : legacy equals() no-arg (deprecated, kept for compatibility) *****");

		// This method is a naming bug (it returns a Vector2, not a boolean) documented in the audit report.
		// It is intentionally left in place and only delegates to copy() now, so this test locks in that
		// backward-compatible behavior: existing calling code that relied on it must keep working identically.
		Vector2 v1 = new Vector2(4f, 5f);
		Vector2 v2 = v1.equals();

		if (!v2.equals(v1)) fail("Legacy equals() should still return a Vector2 equal to the original");
		if (v1 == v2) fail("Legacy equals() should still return a distinct copy, not the same reference");
	}
}
