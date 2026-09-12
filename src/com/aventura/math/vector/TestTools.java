package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * New test class (Tools/interpolate had no test coverage before this pass).
 */
public class TestTools {

	@Test
	public void testInterpolate_vector4_endpoints() {
		System.out.println("***** Test Tools : interpolate(Vector4,Vector4,t) at t=0 and t=1 *****");

		Vector4 a = new Vector4(0f, 0f, 0f, 1f);
		Vector4 b = new Vector4(10f, 20f, 30f, 1f);

		Vector4 atStart = Tools.interpolate(a, b, 0f);
		if (!atStart.equals(a)) fail("interpolate at t=0 should return A");

		Vector4 atEnd = Tools.interpolate(a, b, 1f);
		if (!atEnd.equals(b)) fail("interpolate at t=1 should return B");
	}

	@Test
	public void testInterpolate_vector4_midpoint() {
		System.out.println("***** Test Tools : interpolate(Vector4,Vector4,t) at t=0.5 *****");

		Vector4 a = new Vector4(0f, 0f, 0f, 1f);
		Vector4 b = new Vector4(10f, 20f, 30f, 1f);

		Vector4 mid = Tools.interpolate(a, b, 0.5f);
		assertEquals(5f, mid.getX(), 0.00001f);
		assertEquals(10f, mid.getY(), 0.00001f);
		assertEquals(15f, mid.getZ(), 0.00001f);
	}

	@Test
	public void testInterpolate_vector4_extrapolation() {
		System.out.println("***** Test Tools : interpolate(Vector4,Vector4,t) extrapolates beyond [0,1] *****");

		Vector4 a = new Vector4(0f, 0f, 0f, 1f);
		Vector4 b = new Vector4(10f, 0f, 0f, 1f);

		Vector4 beyondB = Tools.interpolate(a, b, 2f);
		assertEquals(20f, beyondB.getX(), 0.00001f); // beyond B, on the (AB) line

		Vector4 beforeA = Tools.interpolate(a, b, -1f);
		assertEquals(-10f, beforeA.getX(), 0.00001f); // beyond A, on the (AB) line
	}

	@Test
	public void testInterpolate_vector3_endpointsAndMidpoint() {
		System.out.println("***** Test Tools : interpolate(Vector3,Vector3,t) *****");

		Vector3 a = new Vector3(0f, 0f, 0f);
		Vector3 b = new Vector3(4f, 8f, 12f);

		if (!Tools.interpolate(a, b, 0f).equals(a)) fail("t=0 should return A");
		if (!Tools.interpolate(a, b, 1f).equals(b)) fail("t=1 should return B");

		Vector3 mid = Tools.interpolate(a, b, 0.5f);
		assertEquals(2f, mid.getX(), 0.00001f);
		assertEquals(4f, mid.getY(), 0.00001f);
		assertEquals(6f, mid.getZ(), 0.00001f);
	}

	@Test
	public void testInterpolate_vector2_endpointsAndMidpoint() {
		System.out.println("***** Test Tools : interpolate(Vector2,Vector2,t) *****");

		Vector2 a = new Vector2(0f, 0f);
		Vector2 b = new Vector2(6f, 10f);

		if (!Tools.interpolate(a, b, 0.0).equals(a)) fail("t=0 should return A");
		if (!Tools.interpolate(a, b, 1.0).equals(b)) fail("t=1 should return B");

		Vector2 mid = Tools.interpolate(a, b, 0.5);
		assertEquals(3f, mid.getX(), 0.00001f);
		assertEquals(5f, mid.getY(), 0.00001f);
	}

	@Test
	public void testInterpolate_scalar_double() {
		System.out.println("***** Test Tools : interpolate(double,double,double) *****");

		assertEquals(0.0, Tools.interpolate(0.0, 10.0, 0.0), 0.00001);
		assertEquals(10.0, Tools.interpolate(0.0, 10.0, 1.0), 0.00001);
		assertEquals(5.0, Tools.interpolate(0.0, 10.0, 0.5), 0.00001);
		assertEquals(20.0, Tools.interpolate(0.0, 10.0, 2.0), 0.00001); // extrapolation beyond b
	}

	@Test
	public void testInterpolate_scalar_float() {
		System.out.println("***** Test Tools : interpolate(float,float,float) *****");

		assertEquals(0f, Tools.interpolate(0f, 10f, 0f), 0.00001f);
		assertEquals(10f, Tools.interpolate(0f, 10f, 1f), 0.00001f);
		assertEquals(5f, Tools.interpolate(0f, 10f, 0.5f), 0.00001f);
		assertEquals(-10f, Tools.interpolate(0f, 10f, -1f), 0.00001f); // extrapolation before a
	}
}
