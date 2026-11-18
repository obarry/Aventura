package com.aventura.engine;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.context.RenderContext;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Fog;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.atmosphere.SphericalFog;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.SwingView;

/**
 * Off-screen tests of the fogs of the atmosphere (Fog, SphericalFog): the color of the pixels follows the exact optical
 * depth of the fog, an object in front of the fog is left alone, the fog hides the sky, a camera inside the fog sees
 * it everywhere, the switches work and the image does not depend on the number of threads.
 *
 * Setup as TestAtmosphereRenderer: camera at (0, -8, 0) looking at the origin, image of 160 x 90 pixels at 200 pixels
 * of focal length, a near sphere (front at 5.4 from the camera) at the left and a far one (front at 44) at the right,
 * drawn UNLIT so that their pixels are exactly their color. The sky is the background color.
 */
public class TestFogRenderer {

	private static final Color SKY = new Color(100, 150, 220);
	private static final Color SPHERE = new Color(60, 60, 200);
	private static final Color FOG = new Color(230, 230, 230);
	private static final int NEAR_DX = -33, FAR_DX = 25;
	private static final float FOCAL = 200f;

	private static final Vector4 EYE = new Vector4(0, -8, 0, 1);

	private static RenderContext unlit(boolean atmosphere) {
		return new RenderContext(RenderingType.UNLIT).setAtmosphere(atmosphere);
	}

	private static World world(Atmosphere atmosphere) {
		World world = new World();
		Sphere near = new Sphere(0.6f, 16);
		near.setColor(SPHERE);
		near.setTransformation(new Translation(new Vector4(-1f, -2f, 0f, 0f)));
		Sphere far = new Sphere(4f, 16);
		far.setColor(SPHERE);
		far.setTransformation(new Translation(new Vector4(6f, 40f, 0f, 0f)));
		world.addElement(near);
		world.addElement(far);
		world.setBackgroundColor(SKY);
		world.setAtmosphere(atmosphere);
		world.build();
		return world;
	}

	private static SphericalFog fog(float x, float y, float z, float radius, float visibility) {
		SphericalFog f = new SphericalFog(new Vector4(x, y, z, 1f), radius, visibility);
		f.setColor(FOG);
		return f;
	}

	private static BufferedImage render(Atmosphere atmosphere, RenderContext rc, Vector4 eye, Vector4 poi) {
		System.setProperty("java.awt.headless", "true");
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 200);
		Camera camera = new Camera(eye, poi, Vector4.zAxis());
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world(atmosphere), new Lighting(new AmbientLight(0.2f)), camera, rc, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	private static BufferedImage render(Atmosphere atmosphere, RenderContext rc) {
		return render(atmosphere, rc, EYE, new Vector4(0, 0, 0, 1));
	}

	private static Color pixel(BufferedImage img, int dx, int dy) {
		return new Color(img.getRGB(img.getWidth() / 2 + dx, img.getHeight() / 2 - dy));
	}

	private static int differences(BufferedImage a, BufferedImage b) {
		int n = 0;
		for (int y = 0; y < a.getHeight(); y++)
			for (int x = 0; x < a.getWidth(); x++)
				if (a.getRGB(x, y) != b.getRGB(x, y)) n++;
		return n;
	}

	private static void assertColor(String message, Color expected, Color actual, int tolerance) {
		assertEquals(message + " (red)", expected.getRed(), actual.getRed(), tolerance);
		assertEquals(message + " (green)", expected.getGreen(), actual.getGreen(), tolerance);
		assertEquals(message + " (blue)", expected.getBlue(), actual.getBlue(), tolerance);
	}

	/** The color of a pixel seen through a fog of the given optical depth */
	private static Color through(Color c, Color fog, double tau) {
		double t = Math.exp(-tau);
		return new Color((int) Math.round(c.getRed() * t + fog.getRed() * (1 - t)), (int) Math.round(c.getGreen() * t + fog.getGreen() * (1 - t)),
				(int) Math.round(c.getBlue() * t + fog.getBlue() * (1 - t)));
	}

	@Test
	public void testSkyThroughTheCenterOfTheFog() {
		System.out.println("***** Test FogRenderer : the sky seen through the center of a fog, exact optical depth *****");
		// The center pixel looks along +Y through the center of the fog: optical depth 4R / visibility = 4 * 3 / 6 = 2
		Atmosphere a = new Atmosphere().addFog(fog(0, 0, 0, 3f, 6f));
		BufferedImage clear = render(null, unlit(false));
		BufferedImage foggy = render(a, unlit(true));
		assertColor("The sky is there without the fog", SKY, pixel(clear, 0, 0), 0);
		assertColor("Through the center", through(SKY, FOG, 2.0), pixel(foggy, 0, 0), 2);
		// Far from the fog on the screen: unchanged
		assertColor("Away from the fog", SKY, pixel(foggy, 70, 40), 0);
		assertColor("Away from the fog", SKY, pixel(foggy, -70, -40), 0);
	}

	@Test
	public void testOffCenterRaysAreThinner() {
		System.out.println("***** Test FogRenderer : the fog is thinner at its edge, with no visible limit *****");
		// Radius 1 seen from 8 units: the edge of the sphere is at about 25 pixels from the center of the image
		Atmosphere a = new Atmosphere().addFog(fog(0, 0, 0, 1f, 2f));
		BufferedImage foggy = render(a, unlit(true));
		int previous = Integer.MAX_VALUE;
		// Along a column of the image (nothing but sky there) from the center up: the sky gets back its color step by step
		for (int dy = 0; dy <= 24; dy += 3) {
			int dist = Math.abs(pixel(foggy, 0, dy).getRed() - SKY.getRed());
			assertTrue("Monotonic at " + dy, dist <= previous);
			previous = dist;
		}
		// At the edge of the sphere (25 pixels) the fog has faded away completely
		assertTrue("Faint near the edge", Math.abs(pixel(foggy, 0, 26).getRed() - SKY.getRed()) <= 1);
		assertEquals("Out of the sphere", SKY, pixel(foggy, 0, 30));
	}

	@Test
	public void testObjectsInFrontAreLeftAlone() {
		System.out.println("***** Test FogRenderer : an object in front of the fog is left alone, the one behind is dimmed *****");
		BufferedImage clear = render(null, unlit(false));
		// A fog around the far sphere: the near sphere is not behind it
		Atmosphere around = new Atmosphere().addFog(fog(6, 40, 0, 9f, 12f));
		BufferedImage foggy = render(around, unlit(true));
		assertColor("Near sphere in front of the fog", SPHERE, pixel(foggy, NEAR_DX, 0), 0);
		Color far = pixel(foggy, FAR_DX, 0);
		assertTrue("The far sphere is dimmed and goes towards the fog", far.getRed() > SPHERE.getRed() + 10);

		// A fog entirely behind the far sphere (it starts at 60 - 8 = 52 > 44 + 8): the sphere is not touched, the sky around it is
		SphericalFog behindFog = fog(6, 70, 0, 10f, 12f);
		Atmosphere behind = new Atmosphere().addFog(behindFog);
		BufferedImage back = render(behind, unlit(true));
		assertColor("Far sphere in front of the fog", SPHERE, pixel(back, FAR_DX, 0), 0);
		assertColor("Near sphere", SPHERE, pixel(back, NEAR_DX, 0), 0);
		assertTrue("The sky behind the fog is dimmed", differences(clear, back) > 0);
		double tau = behindFog.opticalDepth(0, -8, 0, 0, 1, 0, Double.POSITIVE_INFINITY);
		assertTrue(tau > 0.3);
		assertColor("Sky seen through the fog", through(SKY, FOG, tau), pixel(back, 0, 0), 2);
	}

	@Test
	public void testSurfaceInsideTheFogSeesHalfTheChord() {
		System.out.println("***** Test FogRenderer : a surface inside the fog sees only the part of the fog in front of it *****");
		// Fog centered on the far sphere (center (6, 40, 0)): the pixel on the sphere sees the fog from its entry to the surface
		SphericalFog f = fog(6, 40, 0, 9f, 12f);
		Atmosphere a = new Atmosphere().addFog(f);
		BufferedImage foggy = render(a, unlit(true));
		Color got = pixel(foggy, FAR_DX, 0);
		// Reference: same ray, with the optical depth computed up to the surface of the sphere in front (about y = 36 on the axis)
		double dx = FAR_DX / (double) FOCAL; // Direction (dx, 1, 0) for a camera looking along +Y
		double surface = 36.0;
		double tau = f.opticalDepth(0, -8, 0, dx, 1, 0, surface + 8.0); // Parameter s of the point at y = 36 is (36 + 8) / 1
		Color expected = through(SPHERE, FOG, tau);
		assertColor("Optical depth up to the surface", expected, got, 12); // The sphere is round: the surface is not at 36 exactly
	}

	@Test
	public void testCameraInsideTheFog() {
		System.out.println("***** Test FogRenderer : the camera inside the fog sees it everywhere *****");
		// Centered on the eye, radius 5, visibility 10: every ray from the center has an optical depth of 2R / visibility = 1
		Atmosphere a = new Atmosphere().addFog(fog(0, -8, 0, 5f, 10f));
		BufferedImage foggy = render(a, unlit(true));
		Color expected = through(SKY, FOG, 1.0);
		assertColor("Center of the image", expected, pixel(foggy, 0, 0), 2);
		assertColor("Corner of the image", expected, pixel(foggy, 75, 40), 2);
		assertColor("Other corner", expected, pixel(foggy, -75, -40), 2);
		assertColor("Near sphere is farther than the fog", through(SPHERE, FOG, 1.0), pixel(foggy, NEAR_DX, 0), 3);
	}

	@Test
	public void testFogBehindTheCameraIsIgnored() {
		System.out.println("***** Test FogRenderer : a fog behind the camera changes nothing *****");
		BufferedImage clear = render(null, unlit(false));
		Atmosphere a = new Atmosphere().addFog(fog(0, -30, 0, 5f, 1f));
		assertEquals(0, differences(clear, render(a, unlit(true))));
	}

	@Test
	public void testProjectedAreaIsNotTooSmall() {
		System.out.println("***** Test FogRenderer : the part of the image searched for a fog covers all of it *****");
		// A fog out of the axis, entirely in front: every sky pixel must have the color predicted by the optical depth of the ray
		SphericalFog f = fog(3, 6, 4, 4f, 5f);
		Atmosphere a = new Atmosphere().addFog(f);
		BufferedImage clear = render(null, unlit(false));
		BufferedImage foggy = render(a, unlit(true));
		Camera camera = new Camera(EYE, new Vector4(0, 0, 0, 1), Vector4.zAxis());
		Vector4 fw = camera.getForward(), sd = camera.getSide(), up = camera.getUp();
		int changed = 0;
		for (int y = -40; y <= 40; y++) {
			for (int x = -75; x <= 75; x++) {
				if (!pixel(clear, x, y).equals(SKY)) continue; // Only the sky: its color is known
				double dx = fw.getX() + x / FOCAL * sd.getX() + y / FOCAL * up.getX();
				double dy = fw.getY() + x / FOCAL * sd.getY() + y / FOCAL * up.getY();
				double dz = fw.getZ() + x / FOCAL * sd.getZ() + y / FOCAL * up.getZ();
				double tau = f.opticalDepth(EYE.getX(), EYE.getY(), EYE.getZ(), dx, dy, dz, Double.POSITIVE_INFINITY);
				assertColor("Pixel " + x + "," + y, through(SKY, FOG, tau >= AtmosphereRenderer.MAX_OPTICAL_DEPTH ? 50 : tau), pixel(foggy, x, y), 2);
				if (tau > 0.05) changed++;
			}
		}
		assertTrue("The fog is seen on a good part of the image", changed > 500);
	}

	@Test
	public void testSwitches() {
		System.out.println("***** Test FogRenderer : the switches of the RenderContext, of the branch and of the fog *****");
		SphericalFog f = fog(0, 0, 0, 3f, 6f);
		Atmosphere a = new Atmosphere().addFog(f);
		BufferedImage clear = render(null, unlit(false));
		assertTrue("Fog on", differences(clear, render(a, unlit(true))) > 0);
		assertEquals("Global switch off", 0, differences(clear, render(a, unlit(false))));
		a.setFog(false);
		assertEquals("Branch off", 0, differences(clear, render(a, unlit(true))));
		a.setFog(true);
		f.setEnabled(false);
		assertEquals("Fog off", 0, differences(clear, render(a, unlit(true))));
		f.setEnabled(true);
		assertTrue("Back on", differences(clear, render(a, unlit(true))) > 0);
	}

	@Test
	public void testHazeAndFogAreIndependent() {
		System.out.println("***** Test FogRenderer : the haze and the fog are drawn together and switched apart *****");
		Haze haze = new Haze(30f).setMaxFactor(0.9f).setColor(new Color(230, 230, 230));
		SphericalFog f = fog(0, 0, 0, 3f, 6f);
		Atmosphere both = new Atmosphere(haze).addFog(f);
		BufferedImage onlyHaze = render(new Atmosphere(haze), unlit(true));
		BufferedImage onlyFog = render(new Atmosphere().addFog(f), unlit(true));
		BufferedImage together = render(both, unlit(true));
		assertTrue(differences(onlyHaze, together) > 0);
		assertTrue(differences(onlyFog, together) > 0);
		both.setFog(false);
		assertEquals("Fog off leaves the haze", 0, differences(onlyHaze, render(both, unlit(true))));
		both.setFog(true).setHaze(false);
		assertEquals("Haze off leaves the fog", 0, differences(onlyFog, render(both, unlit(true))));
	}

	@Test
	public void testNoDependencyOnThreads() {
		System.out.println("***** Test FogRenderer : the image does not depend on the number of threads *****");
		Atmosphere a = new Atmosphere(new Haze(40f).setStartDistance(2f)).addFog(fog(0, 0, 0, 3f, 6f)).addFog(fog(2, 20, 1, 8f, 15f));
		BufferedImage one = render(a, unlit(true).setRenderThreads(1));
		BufferedImage several = render(a, unlit(true).setRenderThreads(4));
		assertEquals(0, differences(one, several));
	}

	@Test
	public void testFarFogsAreDrawnFirst() {
		System.out.println("***** Test FogRenderer : the fogs are drawn from the farthest to the nearest, whatever the order they were added *****");
		SphericalFog n = fog(0, -2, 0, 3f, 6f);
		n.setColor(new Color(240, 100, 100));
		SphericalFog f = fog(0, 12, 0, 6f, 6f);
		f.setColor(new Color(100, 100, 240));
		BufferedImage a = render(new Atmosphere().addFog(n).addFog(f), unlit(true));
		BufferedImage b = render(new Atmosphere().addFog(f).addFog(n), unlit(true));
		assertEquals(0, differences(a, b));
		// Seen from the camera, the nearest fog is in front: the center pixel is redder than blue
		Color c = pixel(a, 0, 0);
		assertTrue("Near fog is red: " + c, c.getRed() > c.getBlue() + 50);
	}
}
