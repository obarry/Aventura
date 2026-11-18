package com.aventura.cookbook;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.demo.DemoScene;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Fog;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.atmosphere.SphericalFog;

/**
 * Checks what docs/ATMOSPHERE_COOKBOOK.md and the tutorial of the README say about the examples of AtmosphereCookbook and about
 * GroundMist: the numbers quoted in the text, the effect of each step on the image, the switches, and the custom fog against a numerical
 * integration.
 */
public class TestAtmosphereCookbook {

	private static final int PPU = 1000; // 800 x 450 pixels

	private static BufferedImage render(Atmosphere atmosphere) {
		return AtmosphereCookbook.createScene(atmosphere, PPU).render();
	}

	private static Color pixel(BufferedImage img, int dx, int dy) {
		return new Color(img.getRGB(img.getWidth() / 2 + dx, img.getHeight() / 2 - dy));
	}

	/**
	 * The pixel of a point of the World, as an offset from the center of the image (x to the right, y up): the camera looks along +y,
	 * with z up, and the focal length is PPU pixels (near = 1).
	 */
	private static int[] onScreen(double x, double y, double z) {
		Vector4 eye = AtmosphereCookbook.EYE;
		double depth = y - eye.getY();
		return new int[] { (int) Math.round(PPU * (x - eye.getX()) / depth), (int) Math.round(PPU * (z - eye.getZ()) / depth) };
	}

	/** The color of the image at a point of the World */
	private static Color pixelOf(BufferedImage img, double x, double y, double z) {
		int[] p = onScreen(x, y, z);
		return pixel(img, p[0], p[1]);
	}

	private static int differences(BufferedImage a, BufferedImage b) {
		int n = 0;
		for (int y = 0; y < a.getHeight(); y++)
			for (int x = 0; x < a.getWidth(); x++)
				if (a.getRGB(x, y) != b.getRGB(x, y)) n++;
		return n;
	}

	private static int distance(Color a, Color b) {
		return Math.abs(a.getRed() - b.getRed()) + Math.abs(a.getGreen() - b.getGreen()) + Math.abs(a.getBlue() - b.getBlue());
	}

	private static Color through(Color c, Color fog, double tau) {
		double t = Math.exp(-tau);
		return new Color((int) Math.round(c.getRed() * t + fog.getRed() * (1 - t)), (int) Math.round(c.getGreen() * t + fog.getGreen() * (1 - t)),
				(int) Math.round(c.getBlue() * t + fog.getBlue() * (1 - t)));
	}

	@Test
	public void testTheSceneHasClearAirAndTwoRowsOfPillars() {
		System.out.println("***** Test AtmosphereCookbook : the scene *****");
		assertNull(AtmosphereCookbook.createWorld().getAtmosphere());
		assertEquals(2 * AtmosphereCookbook.PILLARS_PER_SIDE + 1, AtmosphereCookbook.createWorld().getElements().size()); // + the lawn
		BufferedImage clear = render(null);
		assertEquals(800, clear.getWidth());
		assertEquals(450, clear.getHeight());
		assertEquals("The sky above the horizon", AtmosphereCookbook.SKY, pixel(clear, 0, 100));
	}

	@Test
	public void testRecipe1_theHaze() {
		System.out.println("***** Test AtmosphereCookbook : recipe 1, the haze (5% of the contrast at the visibility, clear for the first 5 units) *****");
		Haze haze = AtmosphereCookbook.createHaze();
		assertEquals(0f, haze.factorAt(0f), 0f);
		assertEquals(0f, haze.factorAt(5f), 0f);
		assertTrue(haze.factorAt(20f) > 0f && haze.factorAt(20f) < haze.factorAt(40f));
		assertEquals("The contrast left at the visibility is e^-3 = 5%", 0.05f, 1f - haze.factorAt(80f), 0.001f);
		assertEquals("Never more than the maximum factor", Haze.DEFAULT_MAX_FACTOR, haze.factorAt(500f), 0f);

		// In the image, the far pillars are closer to the sky than the near ones (same colors, one every 8 units)
		BufferedImage clear = render(null);
		BufferedImage hazy = render(new Atmosphere(haze));
		assertTrue("The image changes", differences(clear, hazy) > 0);
		assertEquals("The sky is not touched", AtmosphereCookbook.SKY, pixel(hazy, 0, 100));
		// The front face of the third pillar on the left (23 units from the eye) and of the eleventh (87 units)
		double nearY = 2 * AtmosphereCookbook.PILLAR_SPACING - 0.75, farY = 10 * AtmosphereCookbook.PILLAR_SPACING - 0.75;
		for (double y : new double[] { nearY, farY }) {
			Vector4 eye = AtmosphereCookbook.EYE;
			float radial = (float) Math.sqrt(36 + (y - eye.getY()) * (y - eye.getY())); // the distance along the ray, not the depth
			assertEquals("Contrast left at " + radial + " units", 1f - haze.factorAt(radial), remaining(clear, hazy, -6, y, 2.2), 0.05);
		}
		assertTrue("The near pillar keeps more contrast than the far one",
				remaining(clear, hazy, -6, nearY, 2.2) > remaining(clear, hazy, -6, farY, 2.2) + 0.3);
	}

	/** The part of the contrast with the sky left to an object by the atmosphere: 1 for none lost, 0 for all lost */
	private static double remaining(BufferedImage clear, BufferedImage hazy, double x, double y, double z) {
		double before = distance(pixelOf(clear, x, y, z), AtmosphereCookbook.SKY);
		double after = distance(pixelOf(hazy, x, y, z), AtmosphereCookbook.SKY);
		assertTrue("There is a contrast to lose", before > 30);
		return after / before;
	}

	@Test
	public void testRecipe2_theBankOfFog() {
		System.out.println("***** Test AtmosphereCookbook : recipe 2, the bank of fog (optical depth 4R / visibility through the center) *****");
		SphericalFog bank = AtmosphereCookbook.createFogBank();
		Vector4 eye = AtmosphereCookbook.EYE;
		double tau = bank.opticalDepth(eye.getX(), eye.getY(), eye.getZ(), 0, 1, 0, Double.POSITIVE_INFINITY);
		assertEquals("4 x 12 / 20", 2.4, tau, 1e-6);

		// The pixel of the center of the image looks at the sky through the center of the bank
		BufferedImage foggy = render(new Atmosphere().addFog(bank));
		assertColor("The sky seen through the fog", through(AtmosphereCookbook.SKY, bank.getColor(), 2.4), pixel(foggy, 0, 0), 2);
		// Far from the bank on the screen, nothing changes
		assertEquals(AtmosphereCookbook.SKY, pixel(foggy, 380, 200));
		assertEquals(AtmosphereCookbook.SKY, pixel(foggy, -380, 200));
		// The pillars in front of the bank (the first ones, 8 and 16 units from the start) are not changed, the ones in it are
		BufferedImage clear = render(null);
		double beforeY = 2 * AtmosphereCookbook.PILLAR_SPACING - 0.75, insideY = 3 * AtmosphereCookbook.PILLAR_SPACING - 0.75;
		assertEquals("In front of the bank", pixelOf(clear, -6, beforeY, 2.2), pixelOf(foggy, -6, beforeY, 2.2));
		assertTrue("In the bank", differences(clear, foggy) > 0 && distance(pixelOf(clear, -6, insideY, 2.2), pixelOf(foggy, -6, insideY, 2.2)) > 20);
	}

	private static void assertColor(String message, Color expected, Color actual, int tolerance) {
		assertEquals(message + " (red)", expected.getRed(), actual.getRed(), tolerance);
		assertEquals(message + " (green)", expected.getGreen(), actual.getGreen(), tolerance);
		assertEquals(message + " (blue)", expected.getBlue(), actual.getBlue(), tolerance);
	}

	@Test
	public void testRecipe3_theSwitches() {
		System.out.println("***** Test AtmosphereCookbook : recipe 3, the switches of the atmosphere *****");
		Atmosphere atmosphere = AtmosphereCookbook.createAtmosphere();
		BufferedImage clear = render(null);
		BufferedImage both = render(atmosphere);
		assertTrue(differences(clear, both) > 0);

		atmosphere.setFog(false);
		BufferedImage hazeOnly = render(atmosphere);
		assertEquals("setFog(false) leaves the haze", 0, differences(hazeOnly, render(new Atmosphere(AtmosphereCookbook.createHaze()))));

		atmosphere.setFog(true).setHaze(false);
		BufferedImage fogOnly = render(atmosphere);
		assertEquals("setHaze(false) leaves the fog", 0, differences(fogOnly, render(new Atmosphere().addFog(AtmosphereCookbook.createFogBank()))));

		atmosphere.setFog(false);
		assertEquals("Both out: clear air, though the RenderContext draws the atmosphere", 0, differences(clear, render(atmosphere)));

		atmosphere.setHaze(true).setFog(true);
		atmosphere.getFogs().get(0).setEnabled(false);
		assertEquals("A fog put out by itself", 0, differences(hazeOnly, render(atmosphere)));

		assertEquals("The global switch of the RenderContext", 0,
				differences(clear, noAtmosphereDrawn(AtmosphereCookbook.createAtmosphere())));
	}

	private static BufferedImage noAtmosphereDrawn(Atmosphere atmosphere) {
		DemoScene scene = AtmosphereCookbook.createScene(atmosphere, PPU);
		scene.getRenderContext().setAtmosphere(false);
		return scene.render();
	}

	@Test
	public void testRecipe4_aFogThatMoves() {
		System.out.println("***** Test AtmosphereCookbook : recipe 4, a fog that moves *****");
		SphericalFog bank = AtmosphereCookbook.createFogBank();
		Atmosphere atmosphere = new Atmosphere().addFog(bank);
		bank.setCenter(AtmosphereCookbook.driftingCenter(0));
		BufferedImage left = render(atmosphere);
		bank.setCenter(AtmosphereCookbook.driftingCenter(50));
		BufferedImage right = render(atmosphere);
		bank.setCenter(AtmosphereCookbook.driftingCenter(100));
		BufferedImage back = render(atmosphere);
		assertEquals(-14f, AtmosphereCookbook.driftingCenter(0).getX(), 1e-4f);
		assertEquals(14f, AtmosphereCookbook.driftingCenter(50).getX(), 1e-4f);
		assertTrue("The bank is at the left of the image, then at the right", differences(left, right) > 10000);
		assertTrue("Left: " + pixel(left, -150, 120) + " " + pixel(right, -150, 120),
				distance(pixel(left, -150, 120), AtmosphereCookbook.SKY) > distance(pixel(right, -150, 120), AtmosphereCookbook.SKY));
		assertTrue("Right: " + pixel(right, 150, 120) + " " + pixel(left, 150, 120),
				distance(pixel(right, 150, 120), AtmosphereCookbook.SKY) > distance(pixel(left, 150, 120), AtmosphereCookbook.SKY));
		assertEquals("Back where it started: the same image", 0, differences(left, back));
	}

	@Test
	public void testTheCameraInsideTheFog() {
		System.out.println("***** Test AtmosphereCookbook : the camera in the bank of fog sees it all around *****");
		Atmosphere atmosphere = new Atmosphere().addFog(AtmosphereCookbook.createFogBank());
		DemoScene inside = new DemoScene(AtmosphereCookbook.createWorld(), AtmosphereCookbook.createLighting(), new Vector4(0f, 30f, 2.2f, 1f),
				new Vector4(0f, 80f, 2.2f, 1f), new com.aventura.context.RenderContext(com.aventura.context.RenderContext.RENDER_STANDARD_INTERPOLATE).setAtmosphere(true),
				AtmosphereCookbook.createPerspective(PPU));
		inside.getWorld().setAtmosphere(atmosphere);
		BufferedImage img = inside.render();
		// From the center: every ray has the optical depth 2R / visibility = 1.2 inside (to the sky), whatever the direction
		assertColor("Sky in the middle", through(AtmosphereCookbook.SKY, Fog.DEFAULT_COLOR, 1.2), pixel(img, 0, 150), 3);
		assertColor("Sky in the corner", through(AtmosphereCookbook.SKY, Fog.DEFAULT_COLOR, 1.2), pixel(img, 390, 215), 3);
	}

	// ----- Recipe 5: GroundMist

	@Test
	public void testGroundMist_validation() {
		System.out.println("***** Test GroundMist : validation, bounds *****");
		try { new GroundMist(0f, 0f, 10f); fail("height > 0"); } catch (IllegalArgumentException expected) { }
		try { new GroundMist(0f, 3f, 0f); fail("visibility > 0"); } catch (IllegalArgumentException expected) { }
		GroundMist mist = new GroundMist(1f, 4f, 30f);
		assertEquals(3f, mist.getBoundingCenter().getZ(), 0f);
		assertTrue("As wide as the World", mist.getBoundingRadius() >= 1e5f);
		assertTrue(mist.isEnabled());
	}

	@Test
	public void testGroundMist_straightDownAndOutOfTheLayer() {
		System.out.println("***** Test GroundMist : exact values *****");
		GroundMist mist = new GroundMist(0f, 4f, 30f); // sigma0 = 0.1
		// Straight down from the top to the foot: the integral of 0.1 (1 - z / 4) from 4 to 0, that is 0.1 x 4 / 2
		assertEquals(0.2, mist.opticalDepth(5, 5, 4, 0, 0, -1, 4), 1e-9);
		// From above the layer, ending above it, or going up away from it: nothing
		assertEquals(0.0, mist.opticalDepth(0, 0, 10, 0, 0, -1, 5), 0.0);
		assertEquals(0.0, mist.opticalDepth(0, 0, 10, 0, 0, 1, 100), 0.0);
		// Horizontal, in the layer, at the foot: 0.1 per unit of length
		assertEquals(5.0, mist.opticalDepth(0, 0, 0, 0, 1, 0, 50), 1e-9);
		// Horizontal at half the height: half the density
		assertEquals(2.5, mist.opticalDepth(0, 0, 2, 0, 1, 0, 50), 1e-9);
		// Horizontal and above the layer, or at its very top: nothing; horizontal to the sky in the layer: infinite
		assertEquals(0.0, mist.opticalDepth(0, 0, 5, 0, 1, 0, 50), 0.0);
		assertEquals(0.0, mist.opticalDepth(0, 0, 4, 0, 1, 0, Double.POSITIVE_INFINITY), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, mist.opticalDepth(0, 0, 1, 0, 1, 0, Double.POSITIVE_INFINITY), 0.0);
		// The length of the direction does not matter
		assertEquals(mist.opticalDepth(0, 0, 1, 0, 1, 0.2, 40), mist.opticalDepth(0, 0, 1, 0, 3, 0.6, 40.0 / 3), 1e-9);
	}

	@Test
	public void testGroundMist_matchesNumericalIntegration() {
		System.out.println("***** Test GroundMist : exact optical depth against a numerical integration *****");
		GroundMist mist = new GroundMist(1f, 5f, 20f);
		double sigma0 = 3.0 / 20.0;
		double[][] rays = { { 0, 0, 2, 0, 1, 0.1, 80 }, { 0, 0, 2, 0, 1, 0.1, 1e9 }, { 0, 0, 10, 0.2, 1, -0.15, 1e9 }, { 0, 0, 3, 0, 1, -0.05, 60 },
				{ 0, 0, 0, 0.5, 1, 0.3, 30 }, { 0, 0, 7, 0, 1, -1, 10 } };
		for (double[] r : rays) {
			double length = Math.sqrt(r[3] * r[3] + r[4] * r[4] + r[5] * r[5]);
			double sMax = Math.min(r[6], 400.0); // Beyond the layer nothing more is counted
			int steps = 400000;
			double ds = sMax / steps, sum = 0;
			for (int i = 0; i < steps; i++) {
				double z = r[2] + (i + 0.5) * ds * r[5];
				if (z >= 1 && z <= 6) sum += sigma0 * (1 - (z - 1) / 5) * ds * length;
			}
			assertEquals("Ray " + java.util.Arrays.toString(r), sum, mist.opticalDepth(r[0], r[1], r[2], r[3], r[4], r[5], r[6]), 2e-3);
		}
	}

	@Test
	public void testGroundMist_inTheImage() {
		System.out.println("***** Test GroundMist : the pillars lose their feet in the mist, the sky stays clear above it *****");
		Atmosphere atmosphere = new Atmosphere().addFog(new GroundMist(0f, 4f, 30f));
		BufferedImage clear = render(null);
		BufferedImage misty = render(atmosphere);
		// The second pillar on the left, in front of the camera at 15 units: its foot (z = 0.5) and its top (z = 4.5)
		double y = 2 * AtmosphereCookbook.PILLAR_SPACING - 0.75;
		Color foot = pixelOf(misty, -6, y, 0.4), top = pixelOf(misty, -6, y, 4.8);
		assertTrue("The foot is closer to the color of the mist than the top: " + foot + " " + top,
				distance(foot, Fog.DEFAULT_COLOR) < distance(top, Fog.DEFAULT_COLOR));
		assertTrue("The mist changes the image", differences(clear, misty) > 1000);
		// The horizon is lost in the mist (a ray along the ground is in it for ever), the sky higher up less and less so
		int atHorizon = distance(pixel(misty, 0, 3), Fog.DEFAULT_COLOR), high = distance(pixel(misty, 0, 220), AtmosphereCookbook.SKY);
		assertTrue("Horizon: " + atHorizon, atHorizon < 40);
		assertTrue("Top of the image: " + high, high < distance(pixel(misty, 0, 40), AtmosphereCookbook.SKY));
	}
}
