package com.aventura.demo;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.transform.Rotation;
import com.aventura.math.vector.Quaternion;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * Tests for MovingCamera.interpolateDirection() -- the pure math core of the rotation-key easing
 * added via Quaternion.slerp() (MovingCamera has no other testable surface: everything else needs
 * a live Camera/RenderEngine/Swing GUIView, which this demo class has never had test coverage
 * for). interpolateDirection() touches none of that, so it can be exercised directly.
 *
 * The key mathematical property under test: slerp(identity, Q(axis,angle), t) is exactly
 * Q(axis, t*angle) -- so interpolateDirection(start, Q(axis,angle), t) must match rotating start
 * directly by t*angle around axis (via the already-tested Rotation(float, Vector3) constructor),
 * for every t, not just at the endpoints.
 */
public class TestMovingCamera {

	@Test
	public void testInterpolateDirection_atZero_returnsStartDirectionUnchanged() {
		System.out.println("***** Test MovingCamera : interpolateDirection(t=0) is a no-op *****");

		Vector4 start = new Vector4(1f, 0f, 0f, 0f);
		Quaternion delta = new Quaternion(Vector3.zAxis(), (float)Math.PI/2);

		Vector4 result = MovingCamera.interpolateDirection(start, delta, 0f);

		assertTrue(result.equals(start));
	}

	@Test
	public void testInterpolateDirection_atOne_matchesFullDirectRotation() {
		System.out.println("***** Test MovingCamera : interpolateDirection(t=1) matches an instant Rotation by the full angle *****");

		Vector4 start = new Vector4(1f, 0f, 0f, 0f);
		float angle = (float)Math.PI/2; // 90 degrees
		Quaternion delta = new Quaternion(Vector3.zAxis(), angle);

		Vector4 result = MovingCamera.interpolateDirection(start, delta, 1f);
		Vector4 expected = start.times(new Rotation(angle, Vector3.zAxis()));

		assertTrue(result.equals(expected));
	}

	@Test
	public void testInterpolateDirection_atIntermediateT_matchesScaledDirectRotation() {
		System.out.println("***** Test MovingCamera : interpolateDirection(t) matches an instant Rotation by t*angle, for several t *****");

		Vector4 start = new Vector4(1f, 0f, 0f, 0f);
		float angle = (float)Math.PI/2; // 90 degrees -- large enough that slerp() uses its true
		                                 // spherical-interpolation formula, not its near-identical
		                                 // lerp fallback (see Quaternion.slerp()'s Javadoc), so this
		                                 // exercises the same code path production use will hit for
		                                 // any rotation key held down over several animation frames.
		Quaternion delta = new Quaternion(Vector3.zAxis(), angle);

		for (float t : new float[] {0.25f, 0.5f, 0.75f}) {
			Vector4 result = MovingCamera.interpolateDirection(start, delta, t);
			Vector4 expected = start.times(new Rotation(t*angle, Vector3.zAxis()));

			assertEquals("mismatch at t=" + t + " (x)", expected.getX(), result.getX(), 1e-4f);
			assertEquals("mismatch at t=" + t + " (y)", expected.getY(), result.getY(), 1e-4f);
			assertEquals("mismatch at t=" + t + " (z)", expected.getZ(), result.getZ(), 1e-4f);
		}
	}

	@Test
	public void testInterpolateDirection_preservesLength() {
		System.out.println("***** Test MovingCamera : interpolateDirection() is a pure rotation -- length is preserved *****");

		Vector4 start = new Vector4(0.6f, 0.8f, 0f, 0f); // length 1, but not axis-aligned
		Quaternion delta = new Quaternion(new Vector3(0f, 0f, 1f), (float)Math.PI/3);

		for (float t : new float[] {0f, 0.3f, 0.6f, 1f}) {
			Vector4 result = MovingCamera.interpolateDirection(start, delta, t);
			assertEquals("length not preserved at t=" + t, start.length(), result.length(), 1e-4f);
		}
	}

	@Test
	public void testInterpolateDirection_productionAngle_atOne_matchesFullDirectRotation() {
		System.out.println("***** Test MovingCamera : with the actual 1-degree production increment, t=1 still matches the direct rotation exactly *****");

		// The real increment_rotation used by MovingCamera's rotate*() methods is tiny (~1 degree),
		// small enough that slerp() takes its near-identical lerp-then-normalize fallback rather
		// than the true spherical formula (see Quaternion.slerp()'s Javadoc) -- but the endpoints
		// (t=0, t=1) are exact under either branch, which is what matters for correctness: the
		// animation's final resting orientation must be identical to what the un-eased, instant
		// version used to produce, only how it's reached is smoothed.
		float productionAngle = (float)Math.PI/180;
		Vector4 start = new Vector4(1f, 0f, 0f, 0f);
		Quaternion delta = new Quaternion(Vector3.zAxis(), productionAngle);

		Vector4 result = MovingCamera.interpolateDirection(start, delta, 1f);
		Vector4 expected = start.times(new Rotation(productionAngle, Vector3.zAxis()));

		assertTrue(result.equals(expected));
	}
}
