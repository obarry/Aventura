package com.aventura.model.light;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;

/**
 * Smoke tests: every kind of Light, alone or combined, with shadows on and off, must render a frame
 * without any exception. No display is needed (SwingView is used headless, like the other unit tests).
 * These were the tests that were red before PointLight and SpotLight were completed: PointLight with
 * shadows raised a NullPointerException in ShadowingLight.generateShadowMap(), and SpotLight raised
 * one in Lighting.accumulateContribution() (light vector null) whatever the shadow setting.
 */
public class TestLightTypesRendering {

	private static final Vector4 LIGHT_POS = new Vector4(2, -2, 3, 1);

	private static World scene() {
		World world = new World();
		world.setBackgroundColor(Color.BLACK);
		Trellis floor = new Trellis(8, 8, 16, 16);
		floor.setColor(new Color(120, 120, 130));
		world.addElement(floor);
		Sphere ball = new Sphere(0.8f, 32);
		ball.setColor(new Color(220, 60, 60));
		ball.setTransformation(new Translation(new Vector4(0, 0, 0.8f, 0)));
		world.addElement(ball);
		world.build();
		return world;
	}

	/** Renders one frame; any exception fails the test. */
	private static void render(Lighting lighting, RenderContext rc) {
		System.setProperty("java.awt.headless", "true");
		World world = scene();
		Camera camera = new Camera(new Vector4(6, -7, 4, 1), new Vector4(0, 0, 0.5f, 1), Vector4.zAxis());
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 200);
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, lighting, camera, rc, p);
		engine.setView(view);
		engine.render();
	}

	private static Lighting ambientOnly() {
		return new Lighting(new AmbientLight(0.15f));
	}

	private static Lighting withDirectional() {
		return new Lighting(new DirectionalLight(new Vector3(-1, 0.6f, -0.6f), 1.0f), new AmbientLight(0.15f));
	}

	private static SpotLight spot() {
		SpotLight s = new SpotLight(LIGHT_POS, 10f);
		s.setLightVector(new Vector3(-2, 2, -3)); // aim at the origin
		return s;
	}

	// ---- shadows OFF ----

	@Test
	public void testRender_ambientOnly_noShadows() {
		System.out.println("***** Test rendering : Ambient only, shadows off *****");
		render(ambientOnly(), RenderContext.RENDER_STANDARD_INTERPOLATE);
	}

	@Test
	public void testRender_directional_noShadows() {
		System.out.println("***** Test rendering : Directional, shadows off *****");
		render(withDirectional(), RenderContext.RENDER_STANDARD_INTERPOLATE);
	}

	@Test
	public void testRender_point_noShadows() {
		System.out.println("***** Test rendering : PointLight, shadows off *****");
		Lighting l = ambientOnly();
		l.addPointLight(new PointLight(LIGHT_POS, 10f));
		render(l, RenderContext.RENDER_STANDARD_INTERPOLATE);
	}

	@Test
	public void testRender_spot_noShadows() {
		System.out.println("***** Test rendering : SpotLight, shadows off *****");
		Lighting l = ambientOnly();
		l.addSpotLight(spot());
		render(l, RenderContext.RENDER_STANDARD_INTERPOLATE);
	}

	// ---- shadows ON ----

	@Test
	public void testRender_directional_shadows() {
		System.out.println("***** Test rendering : Directional, shadows on *****");
		render(withDirectional(), RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
	}

	@Test
	public void testRender_point_shadows() {
		System.out.println("***** Test rendering : PointLight, shadows on (no shadow of its own yet: it must stay fully lit) *****");
		Lighting l = ambientOnly();
		PointLight p = new PointLight(LIGHT_POS, 10f);
		l.addPointLight(p);
		render(l, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
		// Until the point light's own shadow maps exist (audit phase 5), it must not produce shadows
		assertEquals(1f, p.shadowFactorAt(new Vector4(0, 0, 0, 1), Vector3.zAxis()), 0f);
	}

	@Test
	public void testRender_spot_shadows() {
		System.out.println("***** Test rendering : SpotLight, shadows on (no shadow of its own yet: it must stay fully lit) *****");
		Lighting l = ambientOnly();
		SpotLight s = spot();
		l.addSpotLight(s);
		render(l, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
		// Until the spot light's own shadow map exists (audit phase 4), it must not produce shadows
		assertEquals(1f, s.shadowFactorAt(new Vector4(0, 0, 0, 1), Vector3.zAxis()), 0f);
	}

	@Test
	public void testRender_directionalPlusPoint_shadows() {
		System.out.println("***** Test rendering : Directional + PointLight, shadows on (the point light must not break the directional one) *****");
		Lighting l = withDirectional();
		l.addPointLight(new PointLight(LIGHT_POS, 10f));
		render(l, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
	}

	@Test
	public void testRender_directionalPlusSpot_shadows() {
		System.out.println("***** Test rendering : Directional + SpotLight, shadows on *****");
		Lighting l = withDirectional();
		l.addSpotLight(spot());
		render(l, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
	}

	// ---- Pixels: the cone of a SpotLight must show on the floor ----

	/** Renders a floor only (no other object), lit by a SpotLight aiming down at (0, 0, 5) and no ambient light. Returns the image. */
	private static BufferedImage renderSpotOnFloor(float outerAngle, float innerAngle) {
		System.setProperty("java.awt.headless", "true");
		World world = new World();
		world.setBackgroundColor(Color.BLACK);
		Trellis floor = new Trellis(8, 8, 16, 16);
		floor.setColor(new Color(200, 200, 200));
		world.addElement(floor);
		world.build();
		Lighting lighting = new Lighting(new AmbientLight(0f));
		lighting.addSpotLight(new SpotLight(new Vector4(0, 0, 5, 1), new Vector3(0, 0, -1), 20f, outerAngle, innerAngle));
		Camera camera = new Camera(new Vector4(0, -8, 8, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 200);
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, lighting, camera, RenderContext.RENDER_STANDARD_INTERPOLATE, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	private static int litPixels(BufferedImage img) {
		int n = 0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, y));
				if (c.getRed() + c.getGreen() + c.getBlue() > 30) n++;
			}
		}
		return n;
	}

	@Test
	public void testRender_spot_lightsTheFloorInsideItsConeOnly() {
		System.out.println("***** Test rendering : SpotLight lights a spot of the floor, a wider cone lights more *****");

		float deg = (float) Math.PI / 180f;
		BufferedImage narrow = renderSpotOnFloor(15 * deg, 10 * deg);
		BufferedImage wide = renderSpotOnFloor(35 * deg, 25 * deg);

		int nNarrow = litPixels(narrow), nWide = litPixels(wide);
		int total = narrow.getWidth() * narrow.getHeight();
		System.out.println("Lit pixels: narrow cone " + nNarrow + ", wide cone " + nWide + " of " + total);

		// The camera aims at the origin, which is right below the spot: the centre of the image is lit
		Color center = new Color(narrow.getRGB(narrow.getWidth() / 2, narrow.getHeight() / 2));
		assertTrue("Centre of the image should be lit by the spot: " + center, center.getRed() > 60);
		assertTrue("A narrow cone should light some pixels", nNarrow > 0);
		assertTrue("A narrow cone should leave most of the image dark", nNarrow < total / 4);
		assertTrue("A wider cone should light more pixels than a narrow one (" + nWide + " vs " + nNarrow + ")", nWide > nNarrow);
	}
}
