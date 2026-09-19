package com.aventura.demo;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.transform.Rotation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * Tests for the two pure-math helpers extracted from FractalLandscape_MouseMoving:
 * composeDragRotation() (Quaternion composition, no easing -- drag rotation must stay real-time)
 * and interpolateZoom() (linear-interpolation easing for the discrete mouse-wheel zoom steps).
 * Everything else in the class needs a live Camera/RenderEngine/Swing GUIView, which this demo
 * class has never had test coverage for; these two helpers touch none of that, so they can be
 * exercised directly.
 */
public class TestFractalLandscape_MouseMoving {

	@Test
	public void testComposeDragRotation_atZeroAngles_isIdentity() {
		System.out.println("***** Test FractalLandscape_MouseMoving : composeDragRotation(0,0) is a no-op *****");

		Rotation r = FractalLandscape_MouseMoving.composeDragRotation(0f, 0f);
		Vector4 v = new Vector4(1f, 2f, 3f, 0f);

		Vector4 result = v.times(r);

		assertEquals(v.getX(), result.getX(), 1e-4f);
		assertEquals(v.getY(), result.getY(), 1e-4f);
		assertEquals(v.getZ(), result.getZ(), 1e-4f);
	}

	@Test
	public void testComposeDragRotation_matchesEquivalentTwoRotationComposition() {
		System.out.println("***** Test FractalLandscape_MouseMoving : composeDragRotation(angleZ, angleY) matches ry.times(rz) *****");

		float angleZ = (float) Math.PI / 6;
		float angleY = (float) Math.PI / 8;

		Rotation quaternionComposed = FractalLandscape_MouseMoving.composeDragRotation(angleZ, angleY);

		// The exact composition it replaces: apply the Z rotation first, then the Y rotation
		// (this is what the original "ry.times(rz)" Matrix4 composition computed).
		Rotation rz = new Rotation(angleZ, Vector3.zAxis());
		Rotation ry = new Rotation(angleY, Vector3.yAxis());
		Matrix4 matrixComposed = ry.times(rz);

		Vector4[] samples = new Vector4[] {
				new Vector4(1f, 0f, 0f, 0f),
				new Vector4(0f, 1f, 0f, 0f),
				new Vector4(0f, 0f, 1f, 0f),
				new Vector4(1f, 1f, 1f, 0f),
				new Vector4(2f, -1f, 0.5f, 0f)
		};

		for (Vector4 v : samples) {
			Vector4 expected = v.times(matrixComposed);
			Vector4 actual = v.times(quaternionComposed);

			assertEquals("mismatch (x)", expected.getX(), actual.getX(), 1e-4f);
			assertEquals("mismatch (y)", expected.getY(), actual.getY(), 1e-4f);
			assertEquals("mismatch (z)", expected.getZ(), actual.getZ(), 1e-4f);
		}
	}

	@Test
	public void testComposeDragRotation_preservesLength() {
		System.out.println("***** Test FractalLandscape_MouseMoving : composeDragRotation() is a pure rotation -- length is preserved *****");

		Rotation r = FractalLandscape_MouseMoving.composeDragRotation((float) Math.PI / 5, (float) Math.PI / 7);
		Vector4 v = new Vector4(0.6f, 0.8f, 0f, 0f); // length 1, not axis-aligned

		Vector4 result = v.times(r);

		assertEquals(v.length(), result.length(), 1e-4f);
	}

	@Test
	public void testInterpolateZoom_atZero_returnsStartValue() {
		System.out.println("***** Test FractalLandscape_MouseMoving : interpolateZoom(t=0) returns the start value *****");

		float result = FractalLandscape_MouseMoving.interpolateZoom(2f, 10f, 0f);

		assertEquals(2f, result, 1e-6f);
	}

	@Test
	public void testInterpolateZoom_atOne_returnsTargetValue() {
		System.out.println("***** Test FractalLandscape_MouseMoving : interpolateZoom(t=1) returns the target value *****");

		float result = FractalLandscape_MouseMoving.interpolateZoom(2f, 10f, 1f);

		assertEquals(10f, result, 1e-6f);
	}

	@Test
	public void testInterpolateZoom_atIntermediateT_isLinear() {
		System.out.println("***** Test FractalLandscape_MouseMoving : interpolateZoom(t) is linear between start and target *****");

		float start = -4f;
		float target = 6f;

		assertEquals(-1.5f, FractalLandscape_MouseMoving.interpolateZoom(start, target, 0.25f), 1e-4f);
		assertEquals(1f, FractalLandscape_MouseMoving.interpolateZoom(start, target, 0.5f), 1e-4f);
		assertEquals(3.5f, FractalLandscape_MouseMoving.interpolateZoom(start, target, 0.75f), 1e-4f);
	}

	@Test
	public void testInterpolateZoom_handlesNegativeDirection() {
		System.out.println("***** Test FractalLandscape_MouseMoving : interpolateZoom() also works when zooming back out (target < start) *****");

		float start = 10f;
		float target = 0f;

		assertEquals(5f, FractalLandscape_MouseMoving.interpolateZoom(start, target, 0.5f), 1e-4f);
		assertEquals(0f, FractalLandscape_MouseMoving.interpolateZoom(start, target, 1f), 1e-6f);
	}
}
