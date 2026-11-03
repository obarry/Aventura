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
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.GUIView;
import com.aventura.view.ImageView;
import com.aventura.view.SwingView;

/**
 * Off-screen tests of the atmosphere (the haze with the distance, Haze; RenderContext.setAtmosphere() and
 * World.setAtmosphere()): nothing changes when the option is off or when the World has no Atmosphere, the sky is
 * never changed, the colors fade towards the haze as the distance grows, and the result does not depend on the
 * number of threads.
 *
 * Setup: camera at (0, -8, 0) looking at the origin, image of 160 x 90 pixels. A near sphere (front at 5.4 from the
 * camera) at the left of the image and a far one (front at 36 + 8 = 44) at its right, both of the same color, drawn
 * UNLIT so that their pixels are exactly their color. The sky is the background color, which is also the default haze.
 */
public class TestAtmosphereRenderer {

	private static final Color SKY = new Color(100, 150, 220);
	private static final Color SPHERE = new Color(60, 60, 200);
	private static final Color HAZE = new Color(230, 230, 230);

	// Pixels of the centers of the spheres, from the center of the image (focal length of 200 pixels)
	private static final int NEAR_DX = -33, FAR_DX = 25;

	private static RenderContext unlit(boolean atmosphere) {
		return new RenderContext(RenderingType.UNLIT).setAtmosphere(atmosphere);
	}

	private static World world(Haze haze) {
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
		world.setAtmosphere(haze == null ? null : new Atmosphere(haze));
		world.build();
		return world;
	}

	private static BufferedImage render(World world, RenderContext rc, PerspectiveType type) {
		System.setProperty("java.awt.headless", "true");
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, type, 200);
		Camera camera = new Camera(new Vector4(0, -8, 0, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, new Lighting(new AmbientLight(0.2f)), camera, rc, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	private static BufferedImage render(Haze haze, RenderContext rc) {
		return render(world(haze), rc, PerspectiveType.FRUSTUM);
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

	/** Distance of the pixel to the haze color, sum of the 3 channels */
	private static int distanceToHaze(Color c, Color haze) {
		return Math.abs(c.getRed() - haze.getRed()) + Math.abs(c.getGreen() - haze.getGreen()) + Math.abs(c.getBlue() - haze.getBlue());
	}

	private static void assertColor(String message, Color expected, Color actual, int tolerance) {
		assertEquals(message + " (red)", expected.getRed(), actual.getRed(), tolerance);
		assertEquals(message + " (green)", expected.getGreen(), actual.getGreen(), tolerance);
		assertEquals(message + " (blue)", expected.getBlue(), actual.getBlue(), tolerance);
	}

	private static Color blend(Color c, Color haze, float f) {
		return new Color(Math.round(c.getRed() + (haze.getRed() - c.getRed()) * f), Math.round(c.getGreen() + (haze.getGreen() - c.getGreen()) * f),
				Math.round(c.getBlue() + (haze.getBlue() - c.getBlue()) * f));
	}

	@Test
	public void testRenderContext_atmosphere() {
		System.out.println("***** Test AtmosphereRenderer : the switch of the RenderContext *****");
		RenderContext r = new RenderContext();
		assertFalse("Off by default", r.isAtmosphere());
		r.setAtmosphere(true);
		assertTrue(new RenderContext(r).isAtmosphere());
		assertTrue(r.toString().contains("Atmosphere"));
		try {
			RenderContext.RENDER_STANDARD_INTERPOLATE.setAtmosphere(true);
			fail("a frozen context cannot be changed");
		} catch (RuntimeException expected) {
		}
	}

	@Test
	public void testNothingChanges_offOrWithoutAtmosphere() {
		System.out.println("***** Test AtmosphereRenderer : nothing changes if the option is off or if the World has no Atmosphere *****");
		BufferedImage clear = render((Haze) null, unlit(false));
		assertEquals("Option on but no Atmosphere in the World", 0, differences(clear, render((Haze) null, unlit(true))));
		assertEquals("Atmosphere in the World but option off", 0, differences(clear, render(new Haze(30f), unlit(false))));
		assertTrue("With both, the image changes", differences(clear, render(new Haze(30f), unlit(true))) > 0);
	}

	@Test
	public void testSkyIsNeverChanged() {
		System.out.println("***** Test AtmosphereRenderer : the sky is never changed *****");
		Haze a = new Haze(5f).setMaxFactor(1f).setColor(HAZE);
		BufferedImage hazy = render(a, unlit(true));
		assertEquals(SKY, pixel(hazy, 0, 40));
		assertEquals(SKY, pixel(hazy, -75, -40));
		assertEquals(SKY, pixel(hazy, 70, 0));
	}

	@Test
	public void testColorsFadeWithTheDistance() {
		System.out.println("***** Test AtmosphereRenderer : the colors fade towards the haze as the distance grows *****");
		Haze a = new Haze(30f).setMaxFactor(0.9f).setColor(HAZE);
		BufferedImage clear = render((Haze) null, unlit(false));
		BufferedImage hazy = render(a, unlit(true));
		assertEquals(SPHERE, pixel(clear, NEAR_DX, 0));
		assertEquals(SPHERE, pixel(clear, FAR_DX, 0));

		Color near = pixel(hazy, NEAR_DX, 0), far = pixel(hazy, FAR_DX, 0);
		System.out.println("Near " + near + ", far " + far);
		assertTrue("The far sphere is closer to the haze than the near one", distanceToHaze(far, HAZE) < distanceToHaze(near, HAZE));
		assertTrue("The near sphere is already a little hazy", distanceToHaze(near, HAZE) < distanceToHaze(SPHERE, HAZE));
		// Exact values: the near sphere is hit at 5.4 from the camera (the polygons of the sphere are a little inside it), the far one is capped
		assertColor("Near sphere", blend(SPHERE, HAZE, a.factorAt(5.4f)), near, 7);
		assertColor("Far sphere, at the maximum factor", blend(SPHERE, HAZE, 0.9f), far, 2);
	}

	@Test
	public void testHazeColor_defaultIsTheBackground() {
		System.out.println("***** Test AtmosphereRenderer : the default haze is the color of the sky *****");
		Haze a = new Haze(1f).setMaxFactor(1f); // Everything is at the maximum factor (the haze is complete at 5.4 already)
		BufferedImage hazy = render(a, unlit(true));
		assertColor("Far sphere melts into the sky", SKY, pixel(hazy, FAR_DX, 0), 1);
		assertColor("Near sphere melts into the sky", SKY, pixel(hazy, NEAR_DX, 0), 1);
		BufferedImage other = render(new Haze(1f).setMaxFactor(1f).setColor(HAZE), unlit(true));
		assertColor("A color of its own", HAZE, pixel(other, FAR_DX, 0), 1);
	}

	@Test
	public void testNothingDrawnInLineRenderingOrOrthographic() {
		System.out.println("***** Test AtmosphereRenderer : not drawn in LINE rendering nor under an orthographic perspective *****");
		Haze a = new Haze(5f).setMaxFactor(1f).setColor(HAZE);
		RenderContext lineOn = new RenderContext(RenderingType.LINE).setAtmosphere(true);
		RenderContext lineOff = new RenderContext(RenderingType.LINE);
		assertEquals(0, differences(render(a, lineOff), render(a, lineOn)));

		BufferedImage orthoOn = render(world(a), unlit(true), PerspectiveType.ORTHOGRAPHIC);
		BufferedImage orthoOff = render(world(a), unlit(false), PerspectiveType.ORTHOGRAPHIC);
		assertEquals(0, differences(orthoOff, orthoOn));
	}

	@Test
	public void testSameImageWithAnyNumberOfThreads() {
		System.out.println("***** Test AtmosphereRenderer : the image does not depend on the number of threads *****");
		Haze a = new Haze(40f).setStartDistance(2f);
		BufferedImage one = render(a, unlit(true).setRenderThreads(1));
		BufferedImage several = render(a, unlit(true).setRenderThreads(4));
		assertEquals(0, differences(one, several));
	}

	@Test
	public void testBlendedPixelsAreCounted() {
		System.out.println("***** Test AtmosphereRenderer : the renderer counts the pixels it blended *****");
		System.setProperty("java.awt.headless", "true");
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 200);
		ImageView view = new ImageView(p);
		assertNull("No frame is being drawn", view.getBackPixels());
		Camera camera = new Camera(new Vector4(0, -8, 0, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		AtmosphereRenderer renderer = new AtmosphereRenderer(p, view);
		ZBuffer zBuffer = new ZBuffer(2 * p.getPixelHalfWidth() + 1, 2 * p.getPixelHalfHeight() + 1, p.getPixelHalfWidth(), p.getPixelHalfHeight(), p.getPerspective().getFar());
		view.initView();
		assertNotNull(view.getBackPixels());
		assertEquals("Only sky: nothing to blend", 0, renderer.render(new Atmosphere(new Haze(10f)), SKY, camera, zBuffer, 1));
		for (int y = -5; y < 5; y++)
			for (int x = -10; x < 10; x++) zBuffer.update(x, y, 20f);
		assertEquals("A block of 20 x 10 pixels at 20 units", 200, renderer.render(new Atmosphere(new Haze(10f)), SKY, camera, zBuffer, 1));
		assertEquals("Not in the clear air of the start distance", 0, renderer.render(new Atmosphere(new Haze(50f).setStartDistance(30f)), SKY, camera, zBuffer, 1));
		assertNull("A view that gives no access to its pixels is left alone", new GUIView(10, 10) {
			@Override public void initView() { }
			@Override public void initView(int w, int h) { }
			@Override public void initView(com.aventura.view.MapView map) { }
			@Override public void renderView() { }
			@Override public void setColor(Color c) { }
			@Override public void setBackgroundColor(Color c) { }
			@Override public Color getPixel(int x, int y) { return null; }
			@Override public void drawPixel(int x, int y) { }
			@Override public void drawPixel(int x, int y, Color c) { }
			@Override public void drawLine(int x1, int y1, int x2, int y2) { }
		}.getBackPixels());
	}
}
