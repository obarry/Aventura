package com.aventura.model.light;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * ------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2016-2026 Olivier BARRY
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------
 *
 * Light.sample() gives in one call what getLightVectorAtPoint() and getIntensity() give separately, for every kind
 * of light: the same values, and the distance.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public class TestLightSample {

	private static final float DELTA = 1e-6f;

	private static void assertSameAsSeparateCalls(String name, Light light, Vector4 point) {
		LightSample sample = new LightSample();
		light.sample(point, sample);
		assertTrue(name + " direction", light.getLightVectorAtPoint(point).equals(sample.getDirection()));
		assertEquals(name + " intensity", light.getIntensity(point), sample.getIntensity(), 0f);
	}

	@Test
	public void testPointLight_directionDistanceAndAttenuationAtOnce() {
		System.out.println("***** Test LightSample : a PointLight gives direction, distance and attenuated intensity at once *****");
		PointLight light = new PointLight(new Vector4(0, 0, 10, 1), 20f, 2f);
		LightSample sample = new LightSample();
		light.sample(new Vector4(6, 0, 2, 1), sample); // 10 away from the light: (6, 0, -8)

		assertEquals(10f, sample.getDistance(), DELTA);
		assertTrue(new Vector3(-0.6f, 0, 0.8f).equals(sample.getDirection()));
		assertEquals("linear attenuation: (20 - 10) / 20, times the intensity factor 2", 1f, sample.getIntensity(), DELTA);
		assertSameAsSeparateCalls("PointLight", light, new Vector4(6, 0, 2, 1));
		assertSameAsSeparateCalls("PointLight out of range", light, new Vector4(30, 0, 2, 1));
	}

	@Test
	public void testPointLight_atTheLightItself() {
		System.out.println("***** Test LightSample : at the position of a PointLight, no direction (zero vector, not NaN) *****");
		PointLight light = new PointLight(new Vector4(1, 2, 3, 1), 20f, 1f);
		LightSample sample = new LightSample();
		light.sample(new Vector4(1, 2, 3, 1), sample);
		assertEquals(0f, sample.getDirection().length(), 0f);
		assertEquals(0f, sample.getDistance(), 0f);
	}

	@Test
	public void testSpotLight_coneFactorFromTheSameDirection() {
		System.out.println("***** Test LightSample : a SpotLight multiplies the intensity by its cone factor *****");
		// Pointing down from (0, 0, 10), outer angle 0.6 rad, inner 0.4 rad
		SpotLight light = new SpotLight(new Vector4(0, 0, 10, 1), new Vector3(0, 0, -1), 30f, 1f, 0.6f, 0.4f);
		assertSameAsSeparateCalls("SpotLight on the axis", light, new Vector4(0, 0, 0, 1));
		assertSameAsSeparateCalls("SpotLight at the edge of the cone", light, new Vector4(5.5f, 0, 0, 1));
		assertSameAsSeparateCalls("SpotLight outside of the cone", light, new Vector4(20, 0, 0, 1));

		LightSample sample = new LightSample();
		light.sample(new Vector4(20, 0, 0, 1), sample);
		assertEquals("outside of the cone: not lit", 0f, sample.getIntensity(), 0f);
		assertEquals("but the distance is known", (float) Math.sqrt(500), sample.getDistance(), 1e-4f);
	}

	@Test
	public void testDirectionalLight_infiniteDistance() {
		System.out.println("***** Test LightSample : a DirectionalLight is at an infinite distance *****");
		DirectionalLight light = new DirectionalLight(new Vector3(1, 1, -1), 0.8f);
		LightSample sample = new LightSample();
		light.sample(new Vector4(3, 4, 5, 1), sample);
		assertEquals(Float.POSITIVE_INFINITY, sample.getDistance(), 0f);
		assertSameAsSeparateCalls("DirectionalLight", light, new Vector4(3, 4, 5, 1));
	}

	@Test
	public void testDefaultImplementation_asksTheSeparateMethods() {
		System.out.println("***** Test LightSample : the default Light.sample() asks getLightVectorAtPoint() and getIntensity() *****");
		AmbientLight light = new AmbientLight(0.3f);
		LightSample sample = new LightSample();
		light.sample(new Vector4(3, 4, 5, 1), sample);
		assertTrue(Float.isNaN(sample.getDistance()));
		assertSameAsSeparateCalls("AmbientLight", light, new Vector4(3, 4, 5, 1));
	}
}
