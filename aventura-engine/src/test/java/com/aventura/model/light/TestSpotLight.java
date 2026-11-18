package com.aventura.model.light;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * Tests for SpotLight's lighting model (no rendering involved): direction, cone (inner / outer angle
 * with a smoothstep in between), distance attenuation and intensity. See TestLightTypesRendering for the
 * tests that render a frame with a SpotLight.
 */
public class TestSpotLight {

	private static final float DELTA = 1e-4f;
	private static final float DEG = (float) Math.PI / 180f;

	/** A spot at the origin, aiming down (-Z), max distance 10, intensity factor 2, outer angle 30 degrees, inner angle 20 degrees. */
	private SpotLight spot() {
		return new SpotLight(new Vector4(0, 0, 0, 1), new Vector3(0, 0, -1), 10f, 2f, 30 * DEG, 20 * DEG);
	}

	/** A point at distance d from the origin, at the given angle (radians) from the axis (-Z), in the XZ plane. */
	private Vector4 pointAt(float d, float angle) {
		return new Vector4((float) (d * Math.sin(angle)), 0, (float) (-d * Math.cos(angle)), 1);
	}

	@Test
	public void testSpotLight_defaults() {
		System.out.println("***** Test SpotLight : default constructor aims down with a 45 degrees cone and a soft edge *****");

		SpotLight s = new SpotLight(new Vector4(1, 2, 3, 1), 10f);
		Vector3 d = s.getDirection();
		assertEquals(0f, d.getX(), DELTA);
		assertEquals(0f, d.getY(), DELTA);
		assertEquals(-1f, d.getZ(), DELTA);
		assertEquals(45 * DEG, s.getOuterAngle(), DELTA);
		assertEquals(0.8f * 45 * DEG, s.getInnerAngle(), DELTA);
		assertEquals(1f, s.getIntensity(new Vector4(1, 2, -2, 1)) / (1 - 5f / 10f), DELTA); // On the axis, at 5 from the light: attenuation only
	}

	@Test
	public void testSpotLight_lightVectorIsTheOneOfPointLight() {
		System.out.println("***** Test SpotLight : light vector is not null, normalized, from the lit point towards the light *****");

		Vector3 v = spot().getLightVectorAtPoint(new Vector4(0, 3, -4, 1));
		assertNotNull(v);
		assertEquals(1f, v.length(), DELTA);
		assertEquals(0f, v.getX(), DELTA);
		assertEquals(-0.6f, v.getY(), DELTA);
		assertEquals(0.8f, v.getZ(), DELTA);
	}

	@Test
	public void testSpotLight_setLightVectorSetsThePropagationDirectionNormalized() {
		System.out.println("***** Test SpotLight : setLightVector() takes the direction of propagation and normalizes it *****");

		SpotLight s = spot();
		s.setLightVector(new Vector3(0, 3, -4));
		Vector3 d = s.getDirection();
		assertEquals(0f, d.getX(), DELTA);
		assertEquals(0.6f, d.getY(), DELTA);
		assertEquals(-0.8f, d.getZ(), DELTA);
		// A point on the new axis is in the core of the cone
		assertEquals(1f, s.coneFactor(new Vector4(0, 6, -8, 1)), DELTA);
		// The former axis is now outside of the cone (angle between the two axes: about 36.9 degrees > 30)
		assertEquals(0f, s.coneFactor(new Vector4(0, 0, -5, 1)), DELTA);
	}

	@Test
	public void testSpotLight_getDirectionReturnsACopy() {
		System.out.println("***** Test SpotLight : modifying the vector returned by getDirection() does not aim the light *****");

		SpotLight s = spot();
		s.getDirection().timesEquals(0f);
		assertEquals(1f, s.getDirection().length(), DELTA);
	}

	@Test
	public void testSpotLight_coneFactor_coreEdgeAndOutside() {
		System.out.println("***** Test SpotLight : cone factor is 1 in the core, 0 outside, strictly between on the fade *****");

		SpotLight s = spot();
		assertEquals(1f, s.coneFactor(pointAt(5f, 0)), DELTA); // on the axis
		assertEquals(1f, s.coneFactor(pointAt(5f, 19 * DEG)), DELTA); // inside the core
		float fade = s.coneFactor(pointAt(5f, 25 * DEG));
		assertTrue("fade factor should be strictly between 0 and 1: " + fade, fade > 0.01f && fade < 0.99f);
		assertEquals(0f, s.coneFactor(pointAt(5f, 31 * DEG)), DELTA); // just outside the cone
		assertEquals(0f, s.coneFactor(pointAt(5f, 90 * DEG)), DELTA); // beside the light
		assertEquals(0f, s.coneFactor(new Vector4(0, 0, 5, 1)), DELTA); // behind the light
	}

	@Test
	public void testSpotLight_coneFactor_isSmoothstepOfTheCosine() {
		System.out.println("***** Test SpotLight : the fade is a smoothstep of the cosine between the outer and the inner angles *****");

		SpotLight s = spot();
		float cosOuter = (float) Math.cos(30 * DEG), cosInner = (float) Math.cos(20 * DEG);
		float cosTheta = (float) Math.cos(24 * DEG);
		float t = (cosTheta - cosOuter) / (cosInner - cosOuter);
		float expected = t * t * (3 - 2 * t);
		assertEquals(expected, s.coneFactor(pointAt(5f, 24 * DEG)), 1e-3f);
		// Independent of the distance to the light
		assertEquals(expected, s.coneFactor(pointAt(0.5f, 24 * DEG)), 1e-3f);
	}

	@Test
	public void testSpotLight_coneFactor_decreasesWithTheAngle() {
		System.out.println("***** Test SpotLight : cone factor never increases when the angle to the axis increases *****");

		SpotLight s = spot();
		float previous = 2f;
		for (int deg = 0; deg <= 40; deg++) {
			float f = s.coneFactor(pointAt(5f, deg * DEG));
			assertTrue("cone factor increased at " + deg + " degrees: " + f + " > " + previous, f <= previous + 1e-6f);
			assertTrue("cone factor out of [0, 1] at " + deg + " degrees: " + f, f >= 0f && f <= 1f);
			previous = f;
		}
	}

	@Test
	public void testSpotLight_sharpEdgeWhenInnerEqualsOuter() {
		System.out.println("***** Test SpotLight : inner == outer gives a sharp edge (no NaN) *****");

		SpotLight s = new SpotLight(new Vector4(0, 0, 0, 1), new Vector3(0, 0, -1), 10f, 30 * DEG, 30 * DEG);
		assertEquals(1f, s.coneFactor(pointAt(5f, 29 * DEG)), DELTA);
		assertEquals(0f, s.coneFactor(pointAt(5f, 31 * DEG)), DELTA);
	}

	@Test
	public void testSpotLight_intensityIsDistanceAttenuationTimesFactorTimesCone() {
		System.out.println("***** Test SpotLight : intensity = linear attenuation x intensity factor x cone factor *****");

		SpotLight s = spot(); // max 10, intensity factor 2
		// On the axis at distance 5: attenuation 0.5, factor 2, cone 1
		assertEquals(1f, s.getIntensity(pointAt(5f, 0)), DELTA);
		// On the axis at 10 (max distance) and beyond: nothing
		assertEquals(0f, s.getIntensity(pointAt(10f, 0)), DELTA);
		assertEquals(0f, s.getIntensity(pointAt(12f, 0)), DELTA);
		// Outside of the cone: nothing, even close to the light
		assertEquals(0f, s.getIntensity(pointAt(1f, 45 * DEG)), DELTA);
		// On the fade: attenuation x factor x cone
		float cone = s.coneFactor(pointAt(5f, 25 * DEG));
		assertEquals(1f * cone, s.getIntensity(pointAt(5f, 25 * DEG)), DELTA);
	}

	@Test
	public void testSpotLight_setIntensityIsApplied() {
		System.out.println("***** Test SpotLight : setIntensity() changes the intensity factor (it used to do nothing, and getIntensity() returned 0) *****");

		SpotLight s = spot();
		s.setIntensity(3f);
		assertEquals(1.5f, s.getIntensity(pointAt(5f, 0)), DELTA); // attenuation 0.5 x 3
	}

	@Test
	public void testSpotLight_colorIsWeightedByIntensity() {
		System.out.println("***** Test SpotLight : the light color at a point follows its intensity (0 outside of the cone) *****");

		SpotLight s = new SpotLight(new Vector4(0, 0, 0, 1), new Vector3(0, 0, -1), 10f, 1f, 30 * DEG, 20 * DEG);
		assertEquals(127, s.getLightColorAtPoint(pointAt(5f, 0)).getRed(), 1); // white x 0.5
		assertEquals(0, s.getLightColorAtPoint(pointAt(5f, 45 * DEG)).getRed());
	}

	@Test
	public void testSpotLight_atTheLightPositionIsFiniteAndHasNoDirection() {
		System.out.println("***** Test SpotLight : at the light's own position the cone factor is finite and the light vector is null (not lit, no NaN) *****");

		SpotLight s = spot();
		Vector4 atLight = new Vector4(0, 0, 0, 1);
		assertEquals(1f, s.coneFactor(atLight), DELTA);
		assertFalse(Float.isNaN(s.getIntensity(atLight)));
		assertEquals(0f, s.getLightVectorAtPoint(atLight).length(), DELTA);
	}

	@Test
	public void testSpotLight_invalidArgumentsAreRejected() {
		System.out.println("***** Test SpotLight : null direction and out of range angles are rejected *****");

		SpotLight s = spot();
		try { s.setDirection(new Vector3(0, 0, 0)); fail("zero direction accepted"); } catch (IllegalArgumentException expected) { }
		try { s.setDirection(null); fail("null direction accepted"); } catch (IllegalArgumentException expected) { }
		try { s.setAngles(0f, 0f); fail("outer angle 0 accepted"); } catch (IllegalArgumentException expected) { }
		try { s.setAngles((float) Math.PI / 2, 0.1f); fail("outer angle PI/2 accepted"); } catch (IllegalArgumentException expected) { }
		try { s.setAngles(0.5f, 0.6f); fail("inner angle > outer angle accepted"); } catch (IllegalArgumentException expected) { }
		try { s.setAngles(0.5f, -0.1f); fail("negative inner angle accepted"); } catch (IllegalArgumentException expected) { }
		// The failed calls left the light unchanged
		assertEquals(30 * DEG, s.getOuterAngle(), DELTA);
		assertEquals(20 * DEG, s.getInnerAngle(), DELTA);
		assertEquals(-1f, s.getDirection().getZ(), DELTA);
	}

	@Test
	public void testLighting_addSpotLightRegistersItAsPointAndShadowingLight() {
		System.out.println("***** Test Lighting : addSpotLight() registers the spot with the point lights and the shadowing lights *****");

		Lighting lighting = new Lighting();
		SpotLight s = spot();
		lighting.addSpotLight(s);
		assertTrue(lighting.hasPoint());
		assertTrue(lighting.hasShadowing());
		assertTrue(lighting.getPointLights().contains(s));
		assertTrue(lighting.getShadowingLights().contains(s));
	}
}
