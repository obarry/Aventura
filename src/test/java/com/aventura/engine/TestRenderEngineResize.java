package com.aventura.engine;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.SwingView;

/**
 * Off-screen tests of the resizing of the GUIView (GUIView.resize()): the RenderEngine follows the new size of the
 * view at the next render(), the Perspective being adapted according to the PerspectiveContext.ResizeMode (same
 * on-screen size of the objects with KEEP_SCALE, enlarged objects with KEEP_HEIGHT when the image gets taller).
 */
public class TestRenderEngineResize {

	private static final Color BACKGROUND = Color.BLACK;
	private static final Color SPHERE_COLOR = new Color(220, 60, 60);

	private PerspectiveContext perspective;
	private SwingView view;
	private RenderEngine engine;

	/** An UNLIT sphere at the origin seen from the front, in a 160 x 90 image (0.8 x 0.45 at 200 pixels per unit). */
	private void createSphereScene(RenderContext rc) {
		System.setProperty("java.awt.headless", "true");
		World world = new World();
		world.setBackgroundColor(BACKGROUND);
		Sphere ball = new Sphere(1.5f, 16);
		ball.setColor(SPHERE_COLOR);
		world.addElement(ball);
		world.build();
		Camera camera = new Camera(new Vector4(0, -8, 0, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		Lighting lighting = new Lighting(new DirectionalLight(new Vector3(-1, 0.6f, -0.6f), 0.4f), new AmbientLight(0.1f));
		perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 200);
		view = new SwingView(perspective);
		engine = new RenderEngine(world, lighting, camera, rc, perspective);
		engine.setView(view);
	}

	private static int count(BufferedImage img, Color c) {
		int n = 0;
		for (int y = 0; y < img.getHeight(); y++)
			for (int x = 0; x < img.getWidth(); x++)
				if (img.getRGB(x, y) == c.getRGB()) n++;
		return n;
	}

	private static Color center(BufferedImage img) {
		return new Color(img.getRGB(img.getWidth() / 2, img.getHeight() / 2));
	}

	@Test
	public void testResize_imageTakesTheNewSizeAtTheNextRender() {
		System.out.println("***** Test RenderEngine : GUIView.resize() changes the size of the image rendered by the next render() *****");
		createSphereScene(RenderContext.RENDER_STANDARD_UNLIT);
		engine.render();
		assertEquals(160, view.getImageView().getWidth());
		assertEquals(90, view.getImageView().getHeight());

		perspective.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);
		view.resize(300, 120);
		// Like any change around the RenderEngine, taken into account by the next render() only
		assertEquals(160, perspective.getPixelWidth());
		engine.render();
		assertEquals(300, perspective.getPixelWidth());
		assertEquals(120, perspective.getPixelHeight());
		BufferedImage img = view.getImageView();
		assertEquals(300, img.getWidth());
		assertEquals(120, img.getHeight());
		assertEquals("the sphere must stay in the center of the resized image", SPHERE_COLOR, center(img));
	}

	@Test
	public void testResizeKeepScale_objectsKeepTheirSizeOnScreen() {
		System.out.println("***** Test RenderEngine : resize(KEEP_SCALE) keeps the on-screen size of the objects *****");
		createSphereScene(RenderContext.RENDER_STANDARD_UNLIT);
		engine.render();
		int before = count(view.getImageView(), SPHERE_COLOR);

		perspective.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);
		view.resize(320, 180);
		engine.render();
		int after = count(view.getImageView(), SPHERE_COLOR);

		assertTrue(before > 0);
		assertEquals("same number of sphere pixels: " + before + " vs " + after, before, after, before * 0.05);
	}

	@Test
	public void testResizeKeepHeight_tallerImageEnlargesTheObjects() {
		System.out.println("***** Test RenderEngine : resize(KEEP_HEIGHT) to a twice taller image enlarges the objects twice *****");
		createSphereScene(RenderContext.RENDER_STANDARD_UNLIT);
		engine.render();
		int before = count(view.getImageView(), SPHERE_COLOR);

		view.resize(320, 180); // KEEP_HEIGHT is the default resize mode
		engine.render();
		int after = count(view.getImageView(), SPHERE_COLOR);

		// Twice the size in each direction: about four times the area
		assertEquals("about 4 times more sphere pixels: " + before + " vs " + after, 4.0, (double) after / before, 0.3);
	}

	@Test
	public void testResize_worksWithShadowsAndShrinking() {
		System.out.println("***** Test RenderEngine : resize() to a smaller image, with shadows, renders without error *****");
		createSphereScene(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
		engine.render();
		perspective.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);
		view.resize(41, 23); // odd dimensions on purpose
		engine.render();
		assertEquals(41, view.getImageView().getWidth());
		assertEquals(23, view.getImageView().getHeight());
		assertNotEquals(BACKGROUND, center(view.getImageView()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGUIViewResize_emptyViewRejected() {
		new SwingView(20, 10).resize(20, 0);
	}
}
