package com.aventura.engine;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.LightAppearance;
import com.aventura.model.light.LightGlowMode;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.SpotLight;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.SwingView;

/**
 * Off-screen tests of the visible lights (halo of the Point and Spot lights, RenderContext.setLightGlow()):
 * nothing changes when the option is off, the glow is drawn around the projected light, objects in front of the
 * light hide it (progressively on an edge), and a light behind the camera is not drawn.
 *
 * Setup: camera at (0, -8, 0) looking at the origin, image of 160 x 90 pixels, focal length of 200 pixels
 * (a length L at distance d from the eye measures 200 L / d pixels).
 */
public class TestLightGlow {

	private static final Color BACKGROUND = Color.BLACK;

	private static final RenderContext GLOW = new RenderContext(RenderingType.INTERPOLATE).setLightGlow(true).freeze();

	private static PointLight withAppearance(PointLight light, LightAppearance appearance) {
		light.setAppearance(appearance);
		return light;
	}

	private static PointLight lightAtOrigin() {
		return new PointLight(new Vector4(0, 0, 0, 1), 30);
	}

	/** Renders the world with the given light and an ambient light only (the glow is the only thing in the image). */
	private static BufferedImage render(World world, PointLight light, RenderContext rc, PerspectiveType type) {
		System.setProperty("java.awt.headless", "true");
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, type, 200);
		world.setBackgroundColor(BACKGROUND);
		world.build();
		Lighting lighting = new Lighting(new AmbientLight(0.2f));
		if (light instanceof SpotLight) lighting.addSpotLight((SpotLight) light);
		else lighting.addPointLight(light);
		Camera camera = new Camera(new Vector4(0, -8, 0, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, lighting, camera, rc, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	private static BufferedImage render(PointLight light, RenderContext rc) {
		return render(new World(), light, rc, PerspectiveType.FRUSTUM);
	}

	/** Brightness (sum of the 3 channels) of the pixel at (dx, dy) from the center of the image, Y up */
	private static int bright(BufferedImage img, int dx, int dy) {
		Color c = new Color(img.getRGB(img.getWidth() / 2 + dx, img.getHeight() / 2 - dy));
		return c.getRed() + c.getGreen() + c.getBlue();
	}

	private static int differences(BufferedImage a, BufferedImage b) {
		int n = 0;
		for (int y = 0; y < a.getHeight(); y++)
			for (int x = 0; x < a.getWidth(); x++)
				if (a.getRGB(x, y) != b.getRGB(x, y)) n++;
		return n;
	}

	private static World sphereAt(float x, float y, float z, float radius) {
		World world = new World();
		Sphere s = new Sphere(radius, 16);
		s.setColor(new Color(60, 60, 200));
		s.setTransformation(new Translation(new Vector4(x, y, z, 0)));
		world.addElement(s);
		return world;
	}

	// ------------
	// Contexts
	// ------------

	@Test
	public void testRenderContext_lightGlow() {
		RenderContext r = new RenderContext();
		assertFalse("Off by default", r.isLightGlow());
		assertNull(r.getLightGlowMode());
		r.setLightGlow(true).setLightGlowMode(LightGlowMode.EMISSIVE);
		RenderContext copy = new RenderContext(r);
		assertTrue(copy.isLightGlow());
		assertEquals(LightGlowMode.EMISSIVE, copy.getLightGlowMode());
		assertNull(copy.resetLightGlowMode().getLightGlowMode());
		assertTrue(r.toString().contains("Light glow"));
		for (RenderContext preset : new RenderContext[] { RenderContext.RENDER_STANDARD_INTERPOLATE, RenderContext.RENDER_STANDARD_FLAT }) {
			assertFalse("The presets are unchanged", preset.isLightGlow());
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRenderContext_frozenPreset() {
		RenderContext.RENDER_STANDARD_INTERPOLATE.setLightGlow(true);
	}

	@Test
	public void testResolveMode_precedence() {
		PointLight point = lightAtOrigin();
		DirectionalLight sun = new DirectionalLight(new Vector3(0, 0, -1), 1f);
		assertEquals(LightGlowMode.HALO, LightGlowRenderer.resolveMode(point, GLOW));
		assertEquals(LightGlowMode.HALO, LightGlowRenderer.resolveMode(new SpotLight(new Vector4(0, 0, 0, 1), new Vector3(0, 0, -1), 10, 0.5f), GLOW));
		assertEquals(LightGlowMode.SUN, LightGlowRenderer.resolveMode(sun, GLOW));
		assertEquals(LightGlowMode.NONE, LightGlowRenderer.resolveMode(new AmbientLight(0.1f), GLOW));

		RenderContext forced = new RenderContext(GLOW).setLightGlowMode(LightGlowMode.EMISSIVE);
		assertEquals("The RenderContext replaces the default", LightGlowMode.EMISSIVE, LightGlowRenderer.resolveMode(point, forced));
		point.setAppearance(new LightAppearance().setMode(LightGlowMode.NONE));
		assertEquals("The light's own mode comes first", LightGlowMode.NONE, LightGlowRenderer.resolveMode(point, forced));
		point.setAppearance(new LightAppearance()); // mode not set
		assertEquals(LightGlowMode.EMISSIVE, LightGlowRenderer.resolveMode(point, forced));
	}

	@Test
	public void testLightAppearance_validation() {
		LightAppearance a = new LightAppearance();
		assertEquals(LightAppearance.DEFAULT_GLOW_RADIUS, a.getGlowRadius(), 0f);
		assertNull(a.getMode());
		assertNull(a.getColor());
		a.setGain(0f); // allowed
		try { a.setGlowRadius(0f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
		try { a.setCoreRadius(-1f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
		try { a.setGain(-0.1f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
	}

	@Test
	public void testAddPixel_addsAndSaturates() {
		SwingView view = new SwingView(20, 10);
		view.initView();
		view.setBackgroundColor(new Color(100, 200, 10));
		view.initView();
		view.drawPixel(0, 0, new Color(100, 200, 10));
		view.addPixel(0, 0, 0.2f, 0.5f, 0f);
		assertEquals(new Color(151, 255, 10), view.getPixel(0, 0));
		view.addPixel(500, 500, 1, 1, 1); // Outside: ignored, no exception
	}

	// ------------
	// Rendering
	// ------------

	@Test
	public void testGlow_notDrawnWhenTheOptionIsOff() {
		BufferedImage off = render(lightAtOrigin(), new RenderContext(RenderingType.INTERPOLATE));
		assertEquals("Nothing but the background", 0, bright(off, 0, 0));
		BufferedImage on = render(lightAtOrigin(), GLOW);
		assertTrue("The option makes the light visible", bright(on, 0, 0) > 300);
	}

	@Test
	public void testGlow_isCenteredAndFadesWithTheDistance() {
		BufferedImage img = render(lightAtOrigin(), GLOW);
		int center = bright(img, 0, 0), near = bright(img, 8, 0), mid = bright(img, 20, 0), far = bright(img, 70, 0);
		System.out.println("Glow profile: " + center + ", " + near + ", " + mid + ", " + far);
		assertTrue("Whitish core", center >= 3 * 150);
		assertTrue(center > near && near > mid && mid > 0);
		assertEquals("Out of the glow: untouched", 0, far);
		assertEquals("Symmetric", bright(img, 8, 0), bright(img, -8, 0), 3);
		assertEquals("Symmetric", bright(img, 0, 8), bright(img, 0, -8), 3);
	}

	@Test
	public void testGlow_colorOfTheLightOrOfTheAppearance() {
		PointLight red = new PointLight(new Vector4(0, 0, 0, 1), 30);
		red.setLightColor(Color.RED);
		BufferedImage img = render(red, GLOW);
		Color c = new Color(img.getRGB(img.getWidth() / 2 + 12, img.getHeight() / 2));
		assertTrue("Red glow: " + c, c.getRed() > 0 && c.getGreen() == 0 && c.getBlue() == 0);

		PointLight blue = new PointLight(new Vector4(0, 0, 0, 1), 30); // white light, blue appearance
		blue.setAppearance(new LightAppearance().setColor(Color.BLUE));
		c = new Color(render(blue, GLOW).getRGB(80 + 12, 45));
		assertTrue("Blue glow: " + c, c.getBlue() > 0 && c.getRed() == 0 && c.getGreen() == 0);
	}

	@Test
	public void testGlow_sizeAndGain() {
		BufferedImage normal = render(lightAtOrigin(), GLOW);
		PointLight big = withAppearance(lightAtOrigin(), new LightAppearance().setGlowRadius(1.1f));
		BufferedImage wide = render(big, GLOW);
		assertTrue("A larger glow reaches further", bright(wide, 30, 0) > bright(normal, 30, 0) + 30);
		PointLight dim = withAppearance(lightAtOrigin(), new LightAppearance().setGain(0.5f));
		BufferedImage faint = render(dim, GLOW);
		assertTrue("Less gain: dimmer halo", bright(faint, 8, 0) < bright(normal, 8, 0));
		PointLight none = withAppearance(lightAtOrigin(), new LightAppearance().setGain(0f));
		assertTrue("No halo without gain, the core remains", bright(render(none, GLOW), 8, 0) < bright(faint, 8, 0)
				&& bright(render(none, GLOW), 0, 0) > 300);
	}

	@Test
	public void testModes_noneEmissiveHalo() {
		BufferedImage background = render(lightAtOrigin(), new RenderContext(RenderingType.INTERPOLATE));
		BufferedImage none = render(withAppearance(lightAtOrigin(), new LightAppearance().setMode(LightGlowMode.NONE)), GLOW);
		assertEquals("NONE: invisible light", 0, differences(background, none));

		BufferedImage emissive = render(withAppearance(lightAtOrigin(), new LightAppearance().setMode(LightGlowMode.EMISSIVE)), GLOW);
		assertTrue("EMISSIVE: core", bright(emissive, 0, 0) > 300);
		assertEquals("EMISSIVE: no halo", 0, bright(emissive, 8, 0));

		BufferedImage forced = render(lightAtOrigin(), new RenderContext(GLOW).setLightGlowMode(LightGlowMode.EMISSIVE));
		assertEquals("Mode forced by the RenderContext", 0, differences(emissive, forced));

		BufferedImage sun = render(withAppearance(lightAtOrigin(), new LightAppearance().setMode(LightGlowMode.SUN)), GLOW);
		assertEquals("SUN is for the lights at infinity: a Point light is not drawn with it", 0, differences(background, sun));
	}

	@Test
	public void testOccluded_isInvisible() {
		// A sphere of 60 pixels of radius in front of the light hides everything the glow would draw
		BufferedImage hidden = render(sphereAt(0, -3, 0, 1.5f), lightAtOrigin(), GLOW, PerspectiveType.FRUSTUM);
		BufferedImage off = render(sphereAt(0, -3, 0, 1.5f), lightAtOrigin(), new RenderContext(RenderingType.INTERPOLATE), PerspectiveType.FRUSTUM);
		assertEquals("The light behind the sphere changes nothing", 0, differences(hidden, off));
	}

	@Test
	public void testNotOccluded_whenTheObjectIsBehindTheLight() {
		BufferedImage img = render(sphereAt(0, 3, 0, 1.5f), lightAtOrigin(), GLOW, PerspectiveType.FRUSTUM);
		BufferedImage off = render(sphereAt(0, 3, 0, 1.5f), lightAtOrigin(), new RenderContext(RenderingType.INTERPOLATE), PerspectiveType.FRUSTUM);
		assertTrue("The light in front of the sphere shines on it", differences(img, off) > 500);
		assertTrue(bright(img, 0, 0) > 300);
	}

	@Test
	public void testPartiallyOccluded_fadesProgressively() {
		// Big core (0.5 => 12 pixels) so that the edge of the sphere crosses it
		LightAppearance big = new LightAppearance().setCoreRadius(0.5f);
		BufferedImage clear = render(new World(), withAppearance(lightAtOrigin(), big), GLOW, PerspectiveType.FRUSTUM);
		int[] values = new int[3];
		float[] edges = { 0.5f, 1.2f, 2.5f }; // Sphere center x: the edge of the sphere (60 pixels of radius) moves away from the light
		for (int i = 0; i < edges.length; i++) {
			BufferedImage img = render(sphereAt(edges[i], -3, 0, 1.5f), withAppearance(lightAtOrigin(), new LightAppearance().setCoreRadius(0.5f)), GLOW, PerspectiveType.FRUSTUM);
			BufferedImage sphere = render(sphereAt(edges[i], -3, 0, 1.5f), lightAtOrigin(), new RenderContext(RenderingType.INTERPOLATE), PerspectiveType.FRUSTUM);
			values[i] = bright(img, -14, 0) - bright(sphere, -14, 0); // Glow added on the free side of the light
		}
		System.out.println("Visibility ramp: clear " + bright(clear, -14, 0) + " -> " + values[0] + ", " + values[1] + ", " + values[2]);
		assertTrue("Fully hidden light: no glow", values[0] == 0);
		assertTrue("Partly hidden: dimmer than clear but visible", values[1] > 0 && values[1] < bright(clear, -14, 0));
		assertTrue("Free light: full glow", values[2] >= bright(clear, -14, 0) - 1);
	}

	@Test
	public void testBehindTheCamera_isNotDrawn() {
		PointLight behind = new PointLight(new Vector4(0, -12, 0, 1), 30);
		BufferedImage img = render(behind, GLOW);
		assertEquals(0, differences(img, render(behind, new RenderContext(RenderingType.INTERPOLATE))));
	}

	@Test
	public void testOffScreenLight_stillGlowsOnTheEdge() {
		// Light just right of the image (half width: 80 pixels): at 8 units from the eye, 3.6 units = 90 pixels
		PointLight outside = new PointLight(new Vector4(3.6f, 0, 0, 1), 30);
		BufferedImage img = render(outside, GLOW);
		assertTrue("The glow reaches into the image", bright(img, 78, 0) > 0);
		assertEquals("...and fades away from the light", 0, bright(img, -40, 0));
	}

	@Test
	public void testLineRenderingAndOrthographic_areUnchanged() {
		RenderContext line = new RenderContext(RenderingType.LINE).setLightGlow(true);
		RenderContext lineOff = new RenderContext(RenderingType.LINE);
		assertEquals(0, differences(render(lightAtOrigin(), line), render(lightAtOrigin(), lineOff)));

		BufferedImage ortho = render(new World(), lightAtOrigin(), GLOW, PerspectiveType.ORTHOGRAPHIC);
		BufferedImage orthoOff = render(new World(), lightAtOrigin(), new RenderContext(RenderingType.INTERPOLATE), PerspectiveType.ORTHOGRAPHIC);
		assertEquals(0, differences(ortho, orthoOff));
	}

	// ------------
	// The sun (Directional light)
	// ------------

	/** A sun in the direction the camera looks at (the center of the image, at (0, -8, 0) looking along +y) unless told otherwise */
	private static DirectionalLight sunInFront() {
		return new DirectionalLight(new Vector3(0, -1, 0), 1f);
	}

	private static BufferedImage renderSun(World world, DirectionalLight sun, RenderContext rc, Vector4 eye, Vector4 poi) {
		System.setProperty("java.awt.headless", "true");
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 200);
		world.setBackgroundColor(BACKGROUND);
		world.build();
		Lighting lighting = new Lighting(sun, new AmbientLight(0.2f));
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, lighting, new Camera(eye, poi, Vector4.zAxis()), rc, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	private static BufferedImage renderSun(World world, DirectionalLight sun, RenderContext rc) {
		return renderSun(world, sun, rc, new Vector4(0, -8, 0, 1), new Vector4(0, 0, 0, 1));
	}

	private static final RenderContext NO_GLOW = new RenderContext(RenderingType.INTERPOLATE).freeze();

	@Test
	public void testSun_discAndHaloInTheSky() {
		assertEquals(LightGlowMode.SUN, LightGlowRenderer.resolveMode(sunInFront(), GLOW));
		BufferedImage off = renderSun(new World(), sunInFront(), NO_GLOW);
		assertEquals("Invisible without the option", 0, bright(off, 0, 0));
		BufferedImage img = renderSun(new World(), sunInFront(), GLOW);
		int center = bright(img, 0, 0), disc = bright(img, 4, 0), near = bright(img, 15, 0), mid = bright(img, 40, 0), far = bright(img, 79, 0);
		System.out.println("Sun profile: " + center + ", " + disc + ", " + near + ", " + mid + ", " + far);
		assertEquals("White disc", 3 * 255, center);
		assertTrue(disc >= 3 * 200);
		assertTrue("Halo around the disc", center > near && near > mid && mid > 0);
		assertEquals("Symmetric", bright(img, 15, 0), bright(img, -15, 0), 3);
		assertEquals("Symmetric", bright(img, 0, 15), bright(img, 0, -15), 3);
	}

	@Test
	public void testSun_sizeIsAnAngle() {
		LightAppearance wide = new LightAppearance().setSunDiscAngle(4f).setSunGlowAngle(15f);
		DirectionalLight sun = sunInFront();
		sun.setAppearance(wide);
		BufferedImage big = renderSun(new World(), sun, GLOW);
		BufferedImage normal = renderSun(new World(), sunInFront(), GLOW);
		assertTrue("Larger disc (4 degrees = 14 pixels) covers pixel 10", bright(big, 10, 0) >= 3 * 250);
		assertTrue(bright(normal, 10, 0) < 3 * 250);
		assertTrue("Larger glow reaches further", bright(big, 60, 0) > bright(normal, 60, 0) + 20);
		try { wide.setSunDiscAngle(0f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
		try { wide.setSunGlowAngle(60f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
	}

	@Test
	public void testSun_colorGainAndModes() {
		DirectionalLight warm = sunInFront();
		warm.setAppearance(new LightAppearance().setColor(new Color(255, 160, 0)).setFlareGain(0f)); // no flare: its colors would mix in
		BufferedImage img = renderSun(new World(), warm, GLOW);
		Color c = new Color(img.getRGB(80 + 20, 45));
		assertTrue("Orange halo: " + c, c.getRed() > c.getGreen() && c.getGreen() > c.getBlue() && c.getBlue() == 0);

		DirectionalLight dim = sunInFront();
		dim.setAppearance(new LightAppearance().setGain(0.4f));
		assertTrue(bright(renderSun(new World(), dim, GLOW), 20, 0) < bright(renderSun(new World(), sunInFront(), GLOW), 20, 0));

		BufferedImage none = renderSun(new World(), sunInFront(), new RenderContext(GLOW).setLightGlowMode(LightGlowMode.NONE));
		assertEquals("Mode forced to NONE", 0, differences(none, renderSun(new World(), sunInFront(), NO_GLOW)));
		DirectionalLight off = sunInFront();
		off.setAppearance(new LightAppearance().setMode(LightGlowMode.NONE));
		assertEquals("NONE on the light", 0, differences(renderSun(new World(), off, GLOW), renderSun(new World(), sunInFront(), NO_GLOW)));
		BufferedImage halo = renderSun(new World(), sunInFront(), new RenderContext(GLOW).setLightGlowMode(LightGlowMode.HALO));
		assertEquals("A direction has no position: HALO does not apply", 0, differences(halo, renderSun(new World(), sunInFront(), NO_GLOW)));
	}

	@Test
	public void testSun_hiddenBehindAnObject() {
		// A sphere filling the whole image in front of the sun: nothing of the sun can be seen
		BufferedImage hidden = renderSun(sphereAt(0, 0, 0, 5f), sunInFront(), GLOW);
		BufferedImage off = renderSun(sphereAt(0, 0, 0, 5f), sunInFront(), NO_GLOW);
		assertEquals(0, differences(hidden, off));
	}

	@Test
	public void testSun_behindTheCamera_isNotDrawn() {
		DirectionalLight behind = new DirectionalLight(new Vector3(0, 1, 0), 1f); // The sun is in the back of the camera
		assertEquals(0, differences(renderSun(new World(), behind, GLOW), renderSun(new World(), behind, NO_GLOW)));
	}

	@Test
	public void testSun_isAtInfinity_cameraMovesDoNotMoveIt() {
		BufferedImage a = renderSun(new World(), sunInFront(), GLOW);
		BufferedImage b = renderSun(new World(), sunInFront(), GLOW, new Vector4(30, -8, 5, 1), new Vector4(30, 0, 5, 1));
		assertEquals("Same place on the screen after a translation of the camera", 0, differences(a, b));
		// Turning the camera moves it: 10 degrees to the right puts the sun 35 pixels to the left
		float yaw = (float) Math.toRadians(10);
		BufferedImage turned = renderSun(new World(), sunInFront(), GLOW, new Vector4(0, -8, 0, 1),
				new Vector4(8 * (float) Math.sin(yaw), -8 + 8 * (float) Math.cos(yaw), 0, 1));
		assertEquals("Disc moved to the left", 3 * 255, bright(turned, -35, 0));
		assertTrue(bright(turned, 0, 0) < 3 * 255);
	}

	@Test
	public void testSun_partiallyHidden_fadesProgressively() {
		BufferedImage clear = renderSun(new World(), sunInFront(), GLOW);
		float[] edges = { 1.0f, 1.5f, 2.5f }; // Sphere (60 pixels of radius) whose edge moves away from the sun
		int[] values = new int[3];
		for (int i = 0; i < edges.length; i++) {
			BufferedImage img = renderSun(sphereAt(edges[i], -3, 0, 1.5f), sunInFront(), GLOW);
			BufferedImage sphere = renderSun(sphereAt(edges[i], -3, 0, 1.5f), sunInFront(), NO_GLOW);
			values[i] = bright(img, -20, 0) - bright(sphere, -20, 0);
		}
		System.out.println("Sun visibility ramp: clear " + bright(clear, -20, 0) + " -> " + values[0] + ", " + values[1] + ", " + values[2]);
		assertEquals("Fully hidden: no glow", 0, values[0]);
		assertTrue("Partly hidden: dimmer than clear but visible", values[1] > 0 && values[1] < bright(clear, -20, 0));
		assertEquals("Free sun: full glow", bright(clear, -20, 0), values[2], 1);
	}

	@Test
	public void testSun_discIsNeverDrawnOverAnObject() {
		// A small sphere (about 5 pixels of radius) in front of the center, and no halo (gain 0) to see the disc alone
		DirectionalLight sun = sunInFront();
		sun.setAppearance(new LightAppearance().setGain(0f).setFlareGain(0f));
		BufferedImage img = renderSun(sphereAt(0, -3, 0, 0.1f), sun, GLOW);
		BufferedImage sphere = renderSun(sphereAt(0, -3, 0, 0.1f), sunInFront(), NO_GLOW);
		assertEquals("The pixels covered by the object keep the object's color", bright(sphere, 0, 0), bright(img, 0, 0));
		assertTrue("The object is there", bright(sphere, 0, 0) < 3 * 255);
		assertTrue("The part of the disc that is not hidden is drawn", bright(img, 4, 0) >= 3 * 250);
	}

	// ------------
	// The lens flare of the sun
	// ------------

	/** A sun that shows at the given pixel (x to the right, y up) of the 160 x 90 image, 200 pixels of focal length */
	private static DirectionalLight sunAt(float pixelX, float pixelY, float flareGain) {
		Vector3 toSun = new Vector3(pixelX / 200f, 1f, pixelY / 200f).normalize();
		DirectionalLight sun = new DirectionalLight(toSun.times(-1), 1f);
		sun.setAppearance(new LightAppearance().setFlareGain(flareGain));
		return sun;
	}

	private static int added(BufferedImage with, BufferedImage without, int dx, int dy) {
		return bright(with, dx, dy) - bright(without, dx, dy);
	}

	@Test
	public void testFlare_ghostsOnTheAxisThroughTheCenter() {
		// Sun at (40, 0): ghosts at 0.45 x 40 = 18 (disc), at the center (ring of 17.6 pixels), at -18 (disc) and at -40 (ring of 12)
		BufferedImage flare = renderSun(new World(), sunAt(40, 0, 1f), GLOW);
		BufferedImage none = renderSun(new World(), sunAt(40, 0, 0f), GLOW);
		assertTrue("Ring around the center", added(flare, none, 17, 0) > 100 && added(flare, none, -17, 0) > 100);
		assertTrue("Ring opposite to the sun (radius 12 pixels, centered on -40)", added(flare, none, -28, 0) > 20);
		assertEquals("Nothing off the axis", 0, added(flare, none, 0, 40));
		assertEquals("Nothing off the axis", 0, added(flare, none, 0, -40));
		assertEquals("The sun itself is the same", bright(none, 40, 0), bright(flare, 40, 0) - added(flare, none, 40, 0));
	}

	@Test
	public void testFlare_ghostsMoveOppositeToTheSun() {
		BufferedImage right = renderSun(new World(), sunAt(40, 0, 1f), GLOW);
		BufferedImage rightNone = renderSun(new World(), sunAt(40, 0, 0f), GLOW);
		BufferedImage left = renderSun(new World(), sunAt(-40, 0, 1f), GLOW);
		BufferedImage leftNone = renderSun(new World(), sunAt(-40, 0, 0f), GLOW);
		assertTrue(added(right, rightNone, -28, 0) > 20 && added(right, rightNone, 28, 0) == 0);
		assertTrue(added(left, leftNone, 28, 0) > 20 && added(left, leftNone, -28, 0) == 0);
		BufferedImage up = renderSun(new World(), sunAt(0, 30, 1f), GLOW);
		BufferedImage upNone = renderSun(new World(), sunAt(0, 30, 0f), GLOW);
		assertTrue("Sun above the center: a ghost below it", added(up, upNone, 12, -30) > 20);
	}

	@Test
	public void testFlare_fadesTowardsTheEdgeAndDisappearsWhenHidden() {
		int nearCenter = added(renderSun(new World(), sunAt(40, 0, 1f), GLOW), renderSun(new World(), sunAt(40, 0, 0f), GLOW), 17, 0);
		BufferedImage edge = renderSun(new World(), sunAt(70, 0, 1f), GLOW);
		int towardsEdge = added(edge, renderSun(new World(), sunAt(70, 0, 0f), GLOW), 17, 0);
		System.out.println("Flare ring: sun at 40 px " + nearCenter + ", at 70 px " + towardsEdge);
		assertTrue("Fainter when the sun is near the edge", towardsEdge < nearCenter && towardsEdge >= 0);

		// A sphere hides the whole image: no sun, no flare
		BufferedImage hidden = renderSun(sphereAt(0, 0, 0, 5f), sunAt(40, 0, 1f), GLOW);
		BufferedImage off = renderSun(sphereAt(0, 0, 0, 5f), sunAt(40, 0, 1f), NO_GLOW);
		assertEquals(0, differences(hidden, off));
	}

	@Test
	public void testFlare_gain() {
		BufferedImage none = renderSun(new World(), sunAt(40, 0, 0f), GLOW);
		int normal = added(renderSun(new World(), sunAt(40, 0, 1f), GLOW), none, 17, 0);
		int strong = added(renderSun(new World(), sunAt(40, 0, 2f), GLOW), none, 17, 0);
		assertTrue("Stronger flare with a larger gain: " + normal + " -> " + strong, strong > normal);
		assertEquals("Default is a normal flare", LightAppearance.DEFAULT_FLARE_GAIN, new LightAppearance().getFlareGain(), 0f);
		try { new LightAppearance().setFlareGain(-1f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
	}

	@Test
	public void testFlare_isForTheSunOnly() {
		// A point light in the image has a halo, but no ghosts at the other side of the center
		BufferedImage img = render(new PointLight(new Vector4(1.6f, 0, 0, 1), 30), GLOW); // At 40 pixels to the right
		assertEquals(0, bright(img, -40, 0));
		assertEquals(0, bright(img, -18, 0));
	}

	// ------------
	// The light shafts of the sun
	// ------------

	/** A sun at the given pixel, with no lens flare (to measure the shafts alone) and the given gain of the shafts */
	private static DirectionalLight sunWithShafts(float pixelX, float pixelY, float shaftsGain) {
		DirectionalLight sun = sunAt(pixelX, pixelY, 0f);
		sun.setAppearance(new LightAppearance().setFlareGain(0f).setShaftsGain(shaftsGain));
		return sun;
	}

	@Test
	public void testShafts_offByDefault() {
		assertEquals(0f, new LightAppearance().getShaftsGain(), 0f);
		BufferedImage defaultSun = renderSun(new World(), sunAt(0, 0, 0f), GLOW);
		BufferedImage explicit = renderSun(new World(), sunWithShafts(0, 0, 0f), GLOW);
		assertEquals(0, differences(defaultSun, explicit));
		try { new LightAppearance().setShaftsGain(-1f); fail("expected IllegalArgumentException"); } catch (IllegalArgumentException expected) { }
	}

	@Test
	public void testShafts_onlyAddLightAroundTheSun() {
		// No halo (gain 0) so that it does not saturate the pixels near the sun and hide the shafts
		DirectionalLight noShafts = sunWithShafts(0, 0, 0f), withShafts = sunWithShafts(0, 0, 1f);
		noShafts.setAppearance(new LightAppearance().setGain(0f).setFlareGain(0f));
		withShafts.setAppearance(new LightAppearance().setGain(0f).setFlareGain(0f).setShaftsGain(1f));
		BufferedImage none = renderSun(new World(), noShafts, GLOW);
		BufferedImage shafts = renderSun(new World(), withShafts, GLOW);
		int brighter = 0;
		for (int y = 0; y < none.getHeight(); y++) {
			for (int x = 0; x < none.getWidth(); x++) {
				Color a = new Color(none.getRGB(x, y)), b = new Color(shafts.getRGB(x, y));
				assertTrue("Light is only added", b.getRed() >= a.getRed() && b.getGreen() >= a.getGreen() && b.getBlue() >= a.getBlue());
				if (b.getRGB() != a.getRGB()) brighter++;
			}
		}
		System.out.println("Shafts: " + brighter + " pixels changed of " + none.getWidth() * none.getHeight());
		assertTrue("The sky around the sun is lit", added(shafts, none, 50, 0) > 0 && added(shafts, none, 0, 30) > 0);
		assertTrue("Brighter near the sun than far from it", added(shafts, none, 20, 0) > added(shafts, none, 70, 0));
	}

	@Test
	public void testShafts_gain() {
		BufferedImage none = renderSun(new World(), sunWithShafts(0, 0, 0f), GLOW);
		int normal = added(renderSun(new World(), sunWithShafts(0, 0, 1f), GLOW), none, 40, 0);
		int strong = added(renderSun(new World(), sunWithShafts(0, 0, 2f), GLOW), none, 40, 0);
		assertTrue("Stronger shafts with a larger gain: " + normal + " -> " + strong, strong > normal && normal > 0);
	}

	@Test
	public void testShafts_objectsCastDarkRays() {
		// A sphere (about 16 pixels of radius) at 20 pixels to the right of the sun: the rays behind it are cut
		BufferedImage none = renderSun(sphereAt(0.5f, -3, 0, 0.4f), sunWithShafts(0, 0, 0f), GLOW);
		BufferedImage shafts = renderSun(sphereAt(0.5f, -3, 0, 0.4f), sunWithShafts(0, 0, 1f), GLOW);
		int behind = added(shafts, none, 55, 0); // On the line from the sun through the sphere
		int free = added(shafts, none, -55, 0); // Same distance on the other side
		System.out.println("Shafts behind the sphere " + behind + ", free side " + free);
		assertTrue("Free side is lit", free > 0);
		assertTrue("The rays are dimmer behind the object", behind < free * 0.8);
	}

	@Test
	public void testShafts_forcedByTheRenderContext() {
		RenderContext forcedOn = new RenderContext(GLOW).setLightShafts(true);
		RenderContext forcedOff = new RenderContext(GLOW).setLightShafts(false);
		assertNull(GLOW.getLightShafts());
		assertEquals(Boolean.TRUE, forcedOn.getLightShafts());
		// Gain of the appearance, unless forced
		LightAppearance none = new LightAppearance(), two = new LightAppearance().setShaftsGain(2f);
		assertEquals(0f, LightGlowRenderer.resolveShaftsGain(none, GLOW), 0f);
		assertEquals(2f, LightGlowRenderer.resolveShaftsGain(two, GLOW), 0f);
		assertEquals("Forced on: gain 1 if the light has none", 1f, LightGlowRenderer.resolveShaftsGain(none, forcedOn), 0f);
		assertEquals("Forced on: the gain of the light if it has one", 2f, LightGlowRenderer.resolveShaftsGain(two, forcedOn), 0f);
		assertEquals(0f, LightGlowRenderer.resolveShaftsGain(two, forcedOff), 0f);

		// Same pictures as with the gain set in the light itself
		BufferedImage withoutOption = renderSun(new World(), sunWithShafts(0, 0, 0f), GLOW);
		BufferedImage forced = renderSun(new World(), sunWithShafts(0, 0, 0f), forcedOn);
		BufferedImage asked = renderSun(new World(), sunWithShafts(0, 0, 1f), GLOW);
		assertTrue("Shafts appear when forced", differences(withoutOption, forced) > 0);
		assertEquals("Forced on = a gain of 1", 0, differences(forced, asked));
		BufferedImage removed = renderSun(new World(), sunWithShafts(0, 0, 1f), forcedOff);
		assertEquals("Forced off = no shafts", 0, differences(withoutOption, removed));
	}

	@Test
	public void testShafts_hiddenSunHasNone() {
		BufferedImage hidden = renderSun(sphereAt(0, 0, 0, 5f), sunWithShafts(0, 0, 1f), GLOW);
		BufferedImage off = renderSun(sphereAt(0, 0, 0, 5f), sunWithShafts(0, 0, 1f), NO_GLOW);
		assertEquals(0, differences(hidden, off));
		DirectionalLight behind = new DirectionalLight(new Vector3(0, 1, 0), 1f); // behind the camera
		behind.setAppearance(new LightAppearance().setShaftsGain(1f));
		assertEquals(0, differences(renderSun(new World(), behind, GLOW), renderSun(new World(), behind, NO_GLOW)));
	}

	@Test
	public void testSpot_dimsOutsideOfItsCone() {
		Vector4 pos = new Vector4(0, 0, 0, 1);
		SpotLight towards = new SpotLight(pos, new Vector3(0, -1, 0), 30, 0.6f, 0.3f); // Aims at the camera
		SpotLight away = new SpotLight(pos, new Vector3(0, 1, 0), 30, 0.6f, 0.3f); // Aims away from the camera
		BufferedImage a = render(towards, GLOW);
		BufferedImage b = render(away, GLOW);
		assertTrue("Spot seen in its beam: halo", bright(a, 8, 0) > 100);
		assertEquals("Spot seen from behind: no halo", 0, bright(b, 8, 0));
		assertTrue("...but the lamp itself (core) is still visible", bright(b, 0, 0) > 300);
	}
}
