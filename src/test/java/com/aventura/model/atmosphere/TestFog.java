package com.aventura.model.atmosphere;

import static org.junit.Assert.*;

import java.awt.Color;

import org.junit.Test;

import com.aventura.math.vector.Vector4;

/**
 * Tests of the Fog and of the SphericalFog: validation of the settings and exact optical depth along rays.
 */
public class TestFog {

	private static final double EPS = 1e-9;

	private static SphericalFog fog() {
		return new SphericalFog(new Vector4(0f, 0f, 0f, 1f), 10f, 10f);
	}

	/** Optical depth of the fog along the ray from (0, -30, 0) towards +Y, up to sMax */
	private static double along(SphericalFog fog, double offsetX, double sMax) {
		return fog.opticalDepth(offsetX, -30, 0, 0, 1, 0, sMax);
	}

	@Test
	public void testDefaultsAndValidation() {
		System.out.println("***** Test SphericalFog : defaults and validation of the settings *****");
		SphericalFog f = new SphericalFog(new Vector4(1f, 2f, 3f, 1f), 5f);
		assertEquals(Fog.DEFAULT_VISIBILITY, f.getVisibility(), 0f);
		assertEquals(Fog.DEFAULT_COLOR, f.getColor());
		assertTrue("Enabled by default", f.isEnabled());
		assertEquals(5f, f.getRadius(), 0f);
		assertEquals(5f, f.getBoundingRadius(), 0f);
		Vector4 c = f.getBoundingCenter();
		assertEquals(1f, c.getX(), 0f);
		assertEquals(2f, c.getY(), 0f);
		assertEquals(3f, c.getZ(), 0f);
		assertEquals("A point", 1f, c.getW(), 0f);

		try { f.setRadius(0f); fail("radius > 0"); } catch (IllegalArgumentException expected) { }
		try { f.setVisibility(0f); fail("visibility > 0"); } catch (IllegalArgumentException expected) { }
		try { f.setColor(null); fail("color not null"); } catch (IllegalArgumentException expected) { }
		try { f.setCenter(null); fail("center not null"); } catch (IllegalArgumentException expected) { }
		try { new SphericalFog(new Vector4(0f, 0f, 0f, 1f), -1f); fail("the constructor validates the radius"); } catch (IllegalArgumentException expected) { }

		f.setColor(Color.GRAY);
		f.setRadius(7f);
		assertEquals(Color.GRAY, f.getColor());
		assertEquals(7f, f.getRadius(), 0f);
		assertTrue(f.setEnabled(false) == f);
		assertFalse(f.isEnabled());
		assertTrue(f.toString().contains("disabled"));
	}

	@Test
	public void testCenterIsCopied() {
		System.out.println("***** Test SphericalFog : the center is a copy *****");
		Vector4 p = new Vector4(1f, 2f, 3f, 1f);
		SphericalFog f = new SphericalFog(p, 5f);
		p.setX(100f);
		assertEquals(1f, f.getCenter().getX(), 0f);
		f.getCenter().setX(200f);
		assertEquals(1f, f.getCenter().getX(), 0f);
	}

	@Test
	public void testThroughTheCenter() {
		System.out.println("***** Test SphericalFog : optical depth through the center is 4R / visibility *****");
		SphericalFog f = fog(); // R = 10, visibility 10
		assertEquals(4.0, along(f, 0, Double.POSITIVE_INFINITY), EPS);
		f.setVisibility(40f);
		assertEquals(1.0, along(f, 0, Double.POSITIVE_INFINITY), EPS);
	}

	@Test
	public void testRayEndingInTheFog() {
		System.out.println("***** Test SphericalFog : a ray that stops inside the fog *****");
		SphericalFog f = fog();
		// Origin at y = -30, center at 30: stopping at the center gives half the chord, which is half the optical depth
		assertEquals(2.0, along(f, 0, 30), EPS);
		// Stopping before the sphere, or at its surface: nothing
		assertEquals(0.0, along(f, 0, 10), 0.0);
		assertEquals(0.0, along(f, 0, 20), 1e-12);
		// Stopping behind it: the whole chord
		assertEquals(4.0, along(f, 0, 1000), EPS);
	}

	@Test
	public void testMissesAndGrazes() {
		System.out.println("***** Test SphericalFog : rays that miss the fog or graze it *****");
		SphericalFog f = fog();
		assertEquals(0.0, along(f, 10.5, Double.POSITIVE_INFINITY), 0.0);
		assertEquals(0.0, along(f, 10.0, Double.POSITIVE_INFINITY), 1e-12);
		// Away from the fog: the ray goes the other way
		assertEquals(0.0, f.opticalDepth(0, -30, 0, 0, -1, 0, Double.POSITIVE_INFINITY), 0.0);
		// The sphere is behind the origin
		assertEquals(0.0, f.opticalDepth(0, 50, 0, 0, 1, 0, Double.POSITIVE_INFINITY), 0.0);
		// A degenerate direction
		assertEquals(0.0, f.opticalDepth(0, -30, 0, 0, 0, 0, Double.POSITIVE_INFINITY), 0.0);
		// Decreases to nothing when the ray goes away from the center
		double previous = along(f, 0, Double.POSITIVE_INFINITY);
		for (double x = 1; x < 10; x += 1) {
			double tau = along(f, x, Double.POSITIVE_INFINITY);
			assertTrue("Thinner at " + x, tau < previous);
			previous = tau;
		}
	}

	@Test
	public void testMatchesNumericalIntegration() {
		System.out.println("***** Test SphericalFog : exact optical depth against a numerical integration *****");
		SphericalFog f = new SphericalFog(new Vector4(3f, 4f, -2f, 1f), 12f, 15f);
		double sigma0 = 3.0 / 15.0;
		// Some rays from a point outside the fog, some from a point inside, some ending in the fog
		double[][] rays = { { -20, -10, 5, 1, 0.7, -0.2, 1e9 }, { -20, -10, 5, 1, 0.7, -0.2, 22 }, { 3, 4, -2, 0.3, -1, 0.5, 1e9 },
				{ 5, 6, 0, -1, 2, 1, 4 }, { 0, -30, 10, 0.1, 1, -0.3, 1e9 } };
		for (double[] r : rays) {
			double dl = Math.sqrt(r[3] * r[3] + r[4] * r[4] + r[5] * r[5]);
			int steps = 200000;
			double sMax = Math.min(r[6], 200.0); // The ray leaves the fog long before 200
			double ds = sMax / steps, sum = 0;
			for (int i = 0; i < steps; i++) {
				double s = (i + 0.5) * ds;
				double px = r[0] + s * r[3] - 3, py = r[1] + s * r[4] - 4, pz = r[2] + s * r[5] + 2;
				double rho2 = (px * px + py * py + pz * pz) / (12.0 * 12.0);
				if (rho2 < 1.0) sum += sigma0 * (1.0 - rho2) * ds * dl;
			}
			double exact = f.opticalDepth(r[0], r[1], r[2], r[3], r[4], r[5], r[6]);
			assertEquals("Ray " + java.util.Arrays.toString(r), sum, exact, 1e-4);
		}
	}

	@Test
	public void testDirectionNeedNotBeNormalized() {
		System.out.println("***** Test SphericalFog : the direction of the ray need not be normalized *****");
		SphericalFog f = fog();
		double unit = f.opticalDepth(4, -30, 3, 0, 1, 0, 35);
		double scaled = f.opticalDepth(4, -30, 3, 0, 3, 0, 35.0 / 3.0);
		assertEquals(unit, scaled, 1e-9);
		assertTrue(unit > 0);
	}
}
