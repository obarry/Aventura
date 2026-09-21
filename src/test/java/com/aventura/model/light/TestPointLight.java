package com.aventura.model.light;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * Tests for PointLight's lighting model (no rendering involved): distance attenuation and light vector.
 */
public class TestPointLight {

	private static final float DELTA = 1e-4f;

	private PointLight light() {
		return new PointLight(new Vector4(0, 0, 0, 1), 10f, 2f); // at origin, max distance 10, intensity factor 2
	}

	@Test
	public void testPointLight_intensityDecreasesLinearlyToZeroAtMaxDistance() {
		System.out.println("***** Test PointLight : linear attenuation, x intensity factor, zero at and beyond max_distance *****");

		PointLight pl = light();
		assertEquals(2f, pl.getIntensity(new Vector4(0, 0, 0, 1)), DELTA);
		assertEquals(1.5f, pl.getIntensity(new Vector4(2.5f, 0, 0, 1)), DELTA);
		assertEquals(1f, pl.getIntensity(new Vector4(5f, 0, 0, 1)), DELTA);
		assertEquals(0f, pl.getIntensity(new Vector4(10f, 0, 0, 1)), DELTA);
		assertEquals(0f, pl.getIntensity(new Vector4(12f, 0, 0, 1)), DELTA);
	}

	@Test
	public void testPointLight_lightVectorIsUnitAndPointsTowardsTheLight() {
		System.out.println("***** Test PointLight : light vector is normalized and goes from the lit point towards the light *****");

		Vector3 v = light().getLightVectorAtPoint(new Vector4(3, 0, 0, 1));
		assertEquals(-1f, v.getX(), DELTA);
		assertEquals(0f, v.getY(), DELTA);
		assertEquals(0f, v.getZ(), DELTA);

		Vector3 w = light().getLightVectorAtPoint(new Vector4(3, 4, 0, 1));
		assertEquals(1f, w.length(), DELTA);
	}

	@Test
	public void testPointLight_lightVectorAtTheLightItselfIsNotNaN() {
		System.out.println("***** Test PointLight : light vector at the light's own position must not be NaN *****");

		Vector3 v = light().getLightVectorAtPoint(new Vector4(0, 0, 0, 1));
		assertFalse("NaN in light vector " + v, Float.isNaN(v.getX()) || Float.isNaN(v.getY()) || Float.isNaN(v.getZ()));
	}

	@Test
	public void testPointLight_setIntensityIsApplied() {
		System.out.println("***** Test PointLight : setIntensity() changes the intensity factor *****");

		PointLight pl = light();
		pl.setIntensity(3f);
		assertEquals(3f, pl.getIntensity(new Vector4(0, 0, 0, 1)), DELTA);
	}
}
