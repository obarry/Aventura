package com.aventura.model.atmosphere;

import static org.junit.Assert.*;

import java.awt.Color;

import org.junit.Test;

import com.aventura.model.atmosphere.Haze.Model;
import com.aventura.model.world.World;

/**
 * Tests of the Haze (the haze with the distance): defaults, validation of the settings and the factor of each model.
 */
public class TestHaze {

	@Test
	public void testDefaults() {
		System.out.println("***** Test Haze : defaults *****");
		Haze a = new Haze();
		assertEquals(Model.EXPONENTIAL, a.getModel());
		assertEquals(Haze.DEFAULT_VISIBILITY, a.getVisibility(), 0f);
		assertEquals(0f, a.getStartDistance(), 0f);
		assertEquals(Haze.DEFAULT_MAX_FACTOR, a.getMaxFactor(), 0f);
		assertNull("The color of the haze is the background of the World", a.getColor());
		assertNull("Clear air by default", new World().getAtmosphere());
		assertEquals(40f, new Haze(40f).getVisibility(), 0f);
	}

	@Test
	public void testValidation() {
		System.out.println("***** Test Haze : validation of the settings *****");
		Haze a = new Haze();
		try { a.setVisibility(0f); fail("a null visibility is refused"); } catch (IllegalArgumentException expected) { }
		try { a.setVisibility(-5f); fail("a negative visibility is refused"); } catch (IllegalArgumentException expected) { }
		try { a.setStartDistance(-1f); fail("a negative start distance is refused"); } catch (IllegalArgumentException expected) { }
		try { a.setStartDistance(100f); fail("the start distance must be below the visibility"); } catch (IllegalArgumentException expected) { }
		try { a.setMaxFactor(0f); fail("a null maximum factor is refused"); } catch (IllegalArgumentException expected) { }
		try { a.setMaxFactor(1.5f); fail("a maximum factor above 1 is refused"); } catch (IllegalArgumentException expected) { }
		try { a.setModel(null); fail("a null model is refused"); } catch (IllegalArgumentException expected) { }
		try { new Haze(0f); fail("the constructor validates the visibility"); } catch (IllegalArgumentException expected) { }
		// The settings are left unchanged by a refused call
		assertEquals(Haze.DEFAULT_VISIBILITY, a.getVisibility(), 0f);
		assertEquals(Model.EXPONENTIAL, a.getModel());
		assertTrue("Fluent setters", a == a.setMaxFactor(1f).setColor(Color.RED));
	}

	@Test
	public void testExponential() {
		System.out.println("***** Test Haze : exponential model *****");
		Haze a = new Haze(100f).setMaxFactor(1f);
		assertEquals(0f, a.factorAt(0f), 0f);
		assertEquals("A negative distance is clear air", 0f, a.factorAt(-3f), 0f);
		assertEquals("The contrast falls to 5% at the visibility", 0.95f, a.factorAt(100f), 0.001f);
		assertEquals(1f - (float) Math.exp(-1.5), a.factorAt(50f), 1e-5f);
		float previous = 0f;
		for (float d = 1f; d < 400f; d += 7f) {
			float f = a.factorAt(d);
			assertTrue("Growing with the distance at " + d, f > previous);
			assertTrue(f < 1f);
			previous = f;
		}
	}

	@Test
	public void testExponentialSquared() {
		System.out.println("***** Test Haze : exponential squared model *****");
		Haze a = new Haze(100f).setMaxFactor(1f).setModel(Model.EXPONENTIAL_SQUARED);
		assertEquals(0f, a.factorAt(0f), 0f);
		assertEquals("Same contrast of 5% at the visibility", 0.95f, a.factorAt(100f), 0.001f);
		assertEquals(1f - (float) Math.exp(-0.75), a.factorAt(50f), 1e-5f);
		Haze plain = new Haze(100f).setMaxFactor(1f);
		assertTrue("Clearer than the exponential near the camera", a.factorAt(20f) < plain.factorAt(20f));
		assertTrue("Hazier beyond the visibility", a.factorAt(150f) > plain.factorAt(150f));
	}

	@Test
	public void testLinear() {
		System.out.println("***** Test Haze : linear model *****");
		Haze a = new Haze(100f).setMaxFactor(1f).setModel(Model.LINEAR);
		assertEquals(0f, a.factorAt(0f), 0f);
		assertEquals(0.25f, a.factorAt(25f), 1e-6f);
		assertEquals(1f, a.factorAt(100f), 1e-6f);
		assertEquals("The haze does not exceed the maximum factor", 1f, a.factorAt(500f), 0f);
		a.setStartDistance(20f);
		assertEquals("Clear air up to the start distance", 0f, a.factorAt(20f), 0f);
		assertEquals(0.5f, a.factorAt(60f), 1e-6f);
		assertEquals(1f, a.factorAt(100f), 1e-6f);
	}

	@Test
	public void testStartDistanceAndMaxFactor() {
		System.out.println("***** Test Haze : start distance and maximum factor *****");
		Haze a = new Haze(100f).setStartDistance(30f).setMaxFactor(0.6f);
		assertEquals(0f, a.factorAt(10f), 0f);
		assertEquals(0f, a.factorAt(30f), 0f);
		assertTrue(a.factorAt(31f) > 0f);
		assertEquals("The contrast still falls to 5% at the visibility (here capped)", 0.6f, a.factorAt(100f), 0f);
		assertEquals(0.6f, a.factorAt(5000f), 0f);
	}

	@Test
	public void testCopy() {
		System.out.println("***** Test Haze : copy constructor *****");
		Haze a = new Haze(80f).setModel(Model.LINEAR).setStartDistance(10f).setMaxFactor(0.8f).setColor(Color.GRAY);
		Haze copy = new Haze(a);
		assertEquals(Model.LINEAR, copy.getModel());
		assertEquals(80f, copy.getVisibility(), 0f);
		assertEquals(10f, copy.getStartDistance(), 0f);
		assertEquals(0.8f, copy.getMaxFactor(), 0f);
		assertEquals(Color.GRAY, copy.getColor());
		copy.setVisibility(200f);
		assertEquals("The copy is independent", 80f, a.getVisibility(), 0f);
		assertTrue(a.toString().contains("LINEAR"));
	}
}
