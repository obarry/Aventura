package com.aventura.demo;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * Unit tests of the pure geometry helpers of FractalLandscape (mouse drag angle, orbit and fly camera,
 * sun direction, ground altitude, interpolation of the camera animation) and of its fractal landscape generation.
 * The rest of the demo needs a window, a Camera and a RenderEngine; these helpers touch none of them, so they can be
 * tested headless.
 */
public class TestFractalLandscape {

	@Test
	public void testDragAngle_isProportionalToTheDisplacement() {
		System.out.println("***** Test FractalLandscape : dragAngle() is proportional to the displacement *****");

		float perWidth = FractalLandscape.DRAG_ANGLE_PER_WIDTH;
		assertEquals(0f, FractalLandscape.dragAngle(0, 1000, perWidth), 0f);
		assertEquals(perWidth, FractalLandscape.dragAngle(1000, 1000, perWidth), 1e-6f);
		assertEquals(-perWidth / 4, FractalLandscape.dragAngle(-250, 1000, perWidth), 1e-6f);
	}

	@Test
	public void testOrbitEye_atZeroAngles_isOnTheXAxisOfThePoi() {
		System.out.println("***** Test FractalLandscape : orbitEye(azimuth=0, elevation=0) is on the X axis of the poi *****");

		Vector4 poi = new Vector4(1f, 2f, 1.5f, 1f);
		Vector4 eye = FractalLandscape.orbitEye(poi, 5f, 0f, 0f);

		assertEquals(6f, eye.getX(), 1e-5f);
		assertEquals(2f, eye.getY(), 1e-5f);
		assertEquals(1.5f, eye.getZ(), 1e-5f);
		assertEquals(1f, eye.getW(), 0f); // A point
	}

	@Test
	public void testOrbitEye_azimuthTurnsAroundZ_elevationRaisesTheEye() {
		System.out.println("***** Test FractalLandscape : orbitEye() azimuth turns around Z, elevation raises the eye *****");

		Vector4 poi = new Vector4(0f, 0f, 0f, 1f);

		Vector4 quarterTurn = FractalLandscape.orbitEye(poi, 2f, (float) Math.PI / 2, 0f);
		assertEquals(0f, quarterTurn.getX(), 1e-5f);
		assertEquals(2f, quarterTurn.getY(), 1e-5f);
		assertEquals(0f, quarterTurn.getZ(), 1e-5f);

		Vector4 fromAbove = FractalLandscape.orbitEye(poi, 2f, 0f, (float) Math.PI / 2);
		assertEquals(0f, fromAbove.getX(), 1e-5f);
		assertEquals(0f, fromAbove.getY(), 1e-5f);
		assertEquals(2f, fromAbove.getZ(), 1e-5f);
	}

	@Test
	public void testOrbitEye_keepsTheDistanceToThePoi() {
		System.out.println("***** Test FractalLandscape : orbitEye() stays at the given distance from the poi *****");

		Vector4 poi = new Vector4(0f, 0f, 1.5f, 1f);
		Vector4 eye = FractalLandscape.orbitEye(poi, 6.2f, 2.3f, -0.7f);

		assertEquals(6.2f, eye.minus(poi).length(), 1e-4f);
	}

	@Test
	public void testSunDirection_isTheReversedDirectionTowardsTheSun() {
		System.out.println("***** Test FractalLandscape : sunDirection() is the propagation direction, from the sun towards the scene *****");

		// Sun on the horizon, on the +X side: the light travels towards -X
		Vector3 east = FractalLandscape.sunDirection(0f, 0f);
		assertEquals(-1f, east.getX(), 1e-5f);
		assertEquals(0f, east.getY(), 1e-5f);
		assertEquals(0f, east.getZ(), 1e-5f);

		// Sun at the zenith: the light travels straight down
		Vector3 zenith = FractalLandscape.sunDirection(123f, 90f);
		assertEquals(0f, zenith.getX(), 1e-5f);
		assertEquals(0f, zenith.getY(), 1e-5f);
		assertEquals(-1f, zenith.getZ(), 1e-5f);

		// Sun on the +Y side, 30 degrees high
		Vector3 north = FractalLandscape.sunDirection(90f, 30f);
		assertEquals(0f, north.getX(), 1e-5f);
		assertEquals(-(float) Math.cos(Math.PI / 6), north.getY(), 1e-5f);
		assertEquals(-0.5f, north.getZ(), 1e-5f);
	}

	@Test
	public void testSunDirection_isAUnitVectorGoingDownForASunAboveTheHorizon() {
		System.out.println("***** Test FractalLandscape : sunDirection() of the demo's sun is a unit vector going down *****");

		Vector3 d = FractalLandscape.sunDirection(FractalLandscape.SUN_AZIMUTH, FractalLandscape.SUN_ELEVATION);

		assertEquals(1f, d.length(), 1e-5f);
		assertTrue("a sun above the horizon lights downwards", d.getZ() < 0);
	}

	@Test
	public void testFlyDirection_followsYawAndPitch() {
		System.out.println("***** Test FractalLandscape : flyDirection() is the unit line of sight for a yaw and a pitch *****");

		Vector4 alongX = FractalLandscape.flyDirection(0f, 0f);
		assertEquals(1f, alongX.getX(), 1e-5f);
		assertEquals(0f, alongX.getY(), 1e-5f);
		assertEquals(0f, alongX.getZ(), 1e-5f);
		assertEquals(0f, alongX.getW(), 0f); // A direction

		Vector4 turnedLeft = FractalLandscape.flyDirection((float) Math.PI / 2, 0f);
		assertEquals(0f, turnedLeft.getX(), 1e-5f);
		assertEquals(1f, turnedLeft.getY(), 1e-5f);

		Vector4 lookingUp = FractalLandscape.flyDirection(0f, (float) Math.PI / 6);
		assertEquals((float) Math.cos(Math.PI / 6), lookingUp.getX(), 1e-5f);
		assertEquals(0.5f, lookingUp.getZ(), 1e-5f);

		assertEquals(1f, FractalLandscape.flyDirection(2.1f, -0.6f).length(), 1e-5f);
	}

	@Test
	public void testGroundAltitude_interpolatesTheTrellisAltitudes() {
		System.out.println("***** Test FractalLandscape : groundAltitude() is the altitude of the trellis (highest triangle cut), NaN outside *****");

		// 2 x 2 cells on a 4 x 4 square centered on the origin: vertices at x, y = -2, 0, 2
		float[][] altitudes = {
				{ 0f, 1f, 2f },  // x = -2 (y = -2, 0, 2)
				{ 1f, 2f, 3f },  // x = 0
				{ 2f, 3f, 8f } }; // x = 2
		assertEquals(0f, FractalLandscape.groundAltitude(altitudes, 4f, 2, -2f, -2f), 1e-5f); // A vertex
		assertEquals(2f, FractalLandscape.groundAltitude(altitudes, 4f, 2, 0f, 0f), 1e-5f); // A vertex
		assertEquals(8f, FractalLandscape.groundAltitude(altitudes, 4f, 2, 2f, 2f), 1e-5f); // The far corner
		assertEquals(1f, FractalLandscape.groundAltitude(altitudes, 4f, 2, -1f, -1f), 1e-5f); // Center of a cell
		// Center of the cell with corners 2, 3, 3, 8: 5 if cut along the 2-8 diagonal, 3 along the other one: the higher
		assertEquals(5f, FractalLandscape.groundAltitude(altitudes, 4f, 2, 1f, 1f), 1e-5f);
		assertTrue(Float.isNaN(FractalLandscape.groundAltitude(altitudes, 4f, 2, 2.5f, 0f)));
		assertTrue(Float.isNaN(FractalLandscape.groundAltitude(altitudes, 4f, 2, 0f, -3f)));
	}

	@Test
	public void testClamp_keepsTheValueWithinTheInterval() {
		System.out.println("***** Test FractalLandscape : clamp() keeps the value within [min, max] *****");

		assertEquals(0.5f, FractalLandscape.clamp(0.5f, 0f, 1f), 0f);
		assertEquals(0f, FractalLandscape.clamp(-3f, 0f, 1f), 0f);
		assertEquals(1f, FractalLandscape.clamp(7f, 0f, 1f), 0f);
	}

	@Test
	public void testInterpolateZoom_atZero_returnsStartValue() {
		System.out.println("***** Test FractalLandscape : interpolate(t=0) returns the start value *****");

		float result = FractalLandscape.interpolate(2f, 10f, 0f);

		assertEquals(2f, result, 1e-6f);
	}

	@Test
	public void testInterpolateZoom_atOne_returnsTargetValue() {
		System.out.println("***** Test FractalLandscape : interpolate(t=1) returns the target value *****");

		float result = FractalLandscape.interpolate(2f, 10f, 1f);

		assertEquals(10f, result, 1e-6f);
	}

	@Test
	public void testInterpolateZoom_atIntermediateT_isLinear() {
		System.out.println("***** Test FractalLandscape : interpolate(t) is linear between start and target *****");

		float start = -4f;
		float target = 6f;

		assertEquals(-1.5f, FractalLandscape.interpolate(start, target, 0.25f), 1e-4f);
		assertEquals(1f, FractalLandscape.interpolate(start, target, 0.5f), 1e-4f);
		assertEquals(3.5f, FractalLandscape.interpolate(start, target, 0.75f), 1e-4f);
	}

	@Test
	public void testInterpolateZoom_handlesNegativeDirection() {
		System.out.println("***** Test FractalLandscape : interpolate() also works when zooming back out (target < start) *****");

		float start = 10f;
		float target = 0f;

		assertEquals(5f, FractalLandscape.interpolate(start, target, 0.5f), 1e-4f);
		assertEquals(0f, FractalLandscape.interpolate(start, target, 1f), 1e-6f);
	}

	@Test
	public void testGenerateFractalAltitudes_isReproducibleWithASeed() {
		System.out.println("***** Test FractalLandscape : generateFractalAltitudes() gives the same landscape for the same seed *****");

		int n = 16;
		float[][] first = new float[n + 1][n + 1];
		float[][] second = new float[n + 1][n + 1];
		FractalLandscape.generateFractalAltitudes(first, 4f, n, new Random(42));
		FractalLandscape.generateFractalAltitudes(second, 4f, n, new Random(42));

		for (int i = 0; i <= n; i++) {
			assertArrayEquals(first[i], second[i], 0f);
		}
	}

	@Test
	public void testGenerateFractalAltitudes_fillsEveryVertexWithAPositiveAltitude() {
		System.out.println("***** Test FractalLandscape : generateFractalAltitudes() gives every vertex a positive altitude *****");

		int n = 16;
		float[][] altitudes = new float[n + 1][n + 1];
		for (float[] row : altitudes) Arrays.fill(row, Float.NaN); // To detect a vertex left out
		FractalLandscape.generateFractalAltitudes(altitudes, 4f, n, new Random(7));

		for (int i = 0; i <= n; i++) {
			for (int j = 0; j <= n; j++) {
				assertFalse("vertex " + i + ", " + j + " is set", Float.isNaN(altitudes[i][j]));
				assertTrue("vertex " + i + ", " + j + " is above 0", altitudes[i][j] >= 0);
			}
		}
	}
}
