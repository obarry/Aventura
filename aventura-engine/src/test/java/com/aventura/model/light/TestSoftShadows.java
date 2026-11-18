package com.aventura.model.light;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;

/**
 * Phase 7: soft shadows through percentage-closer filtering (ShadowFilter.PCF_3X3). The filter replaces
 * the single depth comparison of shadowFactorAt() by a weighted average of the comparisons on the texels
 * around the point, so the factor goes smoothly from 0 to 1 across the edge of a shadow, and stays
 * exactly 0 or 1 away from it. The tests check the contract on the three kinds of light (the default
 * keeps hard shadows, the factor is fractional only at the edge, a shadow keeps its extent, no light
 * leaks at the seam of two faces of a point light) and the final image (ShadingConsumer scales the
 * contribution of the light by the factor).
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class TestSoftShadows {

	private static World sphereOnFloor() {
		World world = new World();
		Trellis floor = new Trellis(20, 20, 4, 4); // XY plane, z=0
		world.addElement(floor);
		Sphere ball = new Sphere(1f, 16);
		ball.setTransformation(new Translation(new Vector4(0, 0, 1, 1)));
		world.addElement(ball);
		world.build();
		world.worldProject();
		return world;
	}

	private static SpotLight spotOverTheSphere(ShadowFilter filter) {
		Vector4 lightPos = new Vector4(5, 0, 10, 1);
		SpotLight spot = new SpotLight(lightPos, new Vector3(lightPos, new Vector4(0, 0, 0, 1)).normalize(), 20f, (float) Math.toRadians(60));
		spot.setShadowFilter(filter);
		World world = sphereOnFloor();
		spot.initShadowing(null, null, world);
		spot.generateShadowMap(world);
		return spot;
	}

	@Test
	public void testDefaultFilterIsHard_andNullIsRejected() {
		System.out.println("***** Test soft shadows : hard shadows by default, null filter rejected *****");
		SpotLight spot = new SpotLight(new Vector4(0, 0, 10, 1), new Vector3(0, 0, -1), 20f, (float) Math.toRadians(45));
		assertEquals(ShadowFilter.HARD, spot.getShadowFilter());
		assertEquals(ShadowFilter.HARD, new PointLight(new Vector4(0, 0, 5, 1), 20f).getShadowFilter());
		assertEquals(ShadowFilter.HARD, new DirectionalLight(new Vector3(0, 0, -1), 1f).getShadowFilter());
		spot.setShadowFilter(ShadowFilter.PCF_3X3);
		assertEquals(ShadowFilter.PCF_3X3, spot.getShadowFilter());
		try {
			spot.setShadowFilter(null);
			fail("a null filter must be rejected");
		} catch (IllegalArgumentException expected) {
			// expected
		}
		assertEquals(0, ShadowFilter.HARD.getRadius());
		assertEquals(1, ShadowFilter.PCF_3X3.getRadius());
		System.out.println("PASS testDefaultFilterIsHard_andNullIsRejected");
	}

	@Test
	public void testSpot_hardIsBinary_pcfIsFractionalOnlyAcrossTheEdge() {
		System.out.println("***** Test soft shadows : spot, a scan across the edge of the shadow is 0/1 when hard, and goes through intermediate values with PCF *****");

		SpotLight hard = spotOverTheSphere(ShadowFilter.HARD);
		SpotLight soft = spotOverTheSphere(ShadowFilter.PCF_3X3);
		Vector3 up = Vector3.zAxis();

		// Scan the floor along X, from the centre of the umbra (x = -5/9, see TestSpotLightShadows) to well outside
		int fractional = 0;
		boolean sawShadow = false, sawLit = false;
		float previous = -1;
		int nonMonotonic = 0;
		for (float x = -5f / 9f; x <= 4f; x += 0.005f) {
			Vector4 p = new Vector4(x, 0, 0, 1);
			float h = hard.shadowFactorAt(p, up);
			float f = soft.shadowFactorAt(p, up);
			assertTrue("hard shadows give exactly 0 or 1, got " + h + " at x=" + x, h == 0f || h == 1f);
			assertTrue("the factor stays in [0, 1], got " + f, f >= 0f && f <= 1f);
			if (f > 0f && f < 1f) fractional++;
			if (f == 0f) sawShadow = true;
			if (f == 1f) sawLit = true;
			if (previous >= 0 && f < previous - 1e-6f) nonMonotonic++;
			previous = f;
		}
		assertTrue("the centre of the umbra is in shadow", sawShadow);
		assertTrue("the far end of the scan is lit", sawLit);
		assertTrue("PCF gives intermediate values across the edge", fractional > 0);
		// One texel of this map is about 0.035 unit wide at the floor, the penumbra about 3 texels once projected at this angle
		assertTrue("the penumbra is narrow (a few texels), not a blur: " + fractional + " samples of 5 mm", fractional < 60);
		assertTrue("the factor grows from the shadow to the light (rare small ripples only: " + nonMonotonic + ")", nonMonotonic <= 3);
		System.out.println("PASS testSpot_hardIsBinary_pcfIsFractionalOnlyAcrossTheEdge (" + fractional + " fractional samples)");
	}

	@Test
	public void testSpot_pcfKeepsTheShadowOfTheHardOneAwayFromItsEdge() {
		System.out.println("***** Test soft shadows : spot, deep in the shadow and far from it PCF and hard agree *****");
		SpotLight hard = spotOverTheSphere(ShadowFilter.HARD);
		SpotLight soft = spotOverTheSphere(ShadowFilter.PCF_3X3);
		Vector3 up = Vector3.zAxis();
		Vector4 umbraCentre = new Vector4(-5f / 9f, 0, 0, 1);
		Vector4 clear = new Vector4(-5f / 9f + 4f, 0, 0, 1);
		assertEquals(0f, hard.shadowFactorAt(umbraCentre, up), 0f);
		assertEquals(0f, soft.shadowFactorAt(umbraCentre, up), 0f);
		assertEquals(1f, hard.shadowFactorAt(clear, up), 0f);
		assertEquals(1f, soft.shadowFactorAt(clear, up), 0f);
		// A lit, tilted surface facing the light has no self-shadowing acne with PCF either: the floor under the spot, away from the sphere
		for (float x = 2f; x <= 8f; x += 0.25f) {
			assertEquals("no acne on the lit floor at x=" + x, 1f, soft.shadowFactorAt(new Vector4(x, 0, 0, 1), up), 0f);
		}
		System.out.println("PASS testSpot_pcfKeepsTheShadowOfTheHardOneAwayFromItsEdge");
	}

	@Test
	public void testDirectional_pcfGivesIntermediateValuesAtTheEdgeOnly() {
		System.out.println("***** Test soft shadows : directional light, orthographic map, same contract *****");
		World world = sphereOnFloor();
		DirectionalLight sun = new DirectionalLight(new Vector3(0, 0, -1), 1f);
		sun.setShadowFilter(ShadowFilter.PCF_3X3);
		sun.initShadowing(null, null, world);
		sun.generateShadowMap(world);
		Vector3 up = Vector3.zAxis();

		assertEquals("below the sphere", 0f, sun.shadowFactorAt(new Vector4(0, 0, 0, 1), up), 0f);
		assertEquals("far from the sphere", 1f, sun.shadowFactorAt(new Vector4(6, 0, 0, 1), up), 0f);
		int fractional = 0;
		for (float x = 0f; x <= 2f; x += 0.005f) {
			float f = sun.shadowFactorAt(new Vector4(x, 0, 0, 1), up);
			assertTrue(f >= 0f && f <= 1f);
			if (f > 0f && f < 1f) fractional++;
		}
		assertTrue("intermediate values across the edge of the shadow (radius 1)", fractional > 0);
		System.out.println("PASS testDirectional_pcfGivesIntermediateValuesAtTheEdgeOnly (" + fractional + " fractional samples)");
	}

	@Test
	public void testPoint_pcfDoesNotLeakLightAcrossTheSeamOfTwoFaces() {
		System.out.println("***** Test soft shadows : point light, a shadow straddling the seam of two cube faces stays whole with PCF *****");
		PointLight light = new PointLight(new Vector4(0, 0, 5, 1), 30f);
		light.setShadowFilter(ShadowFilter.PCF_3X3);

		World world = new World();
		world.addElement(new Trellis(30, 30, 4, 4)); // the floor, z = 0
		Box roof = new Box(4, 4, 0.2f); // a slab at (4, 0, 2): its shadow on the floor covers x from 3.3 to 10
		roof.setTransformation(new Translation(new Vector4(4, 0, 2, 1)));
		world.addElement(roof);
		world.build();
		world.worldProject();
		light.initShadowing(null, null, world);
		light.generateShadowMap(world);

		// The floor point (5, 0, 0) is at 45 degrees below the light: |x| = |z - 5|, the seam of the +X and -Z faces
		Vector3 up = Vector3.zAxis();
		for (float x = 4.2f; x <= 5.8f; x += 0.05f) {
			assertEquals("floor in the shadow of the slab, around the seam, x=" + x, 0f, light.shadowFactorAt(new Vector4(x, 0, 0, 1), up), 0f);
		}
		assertEquals("floor clear of the slab", 1f, light.shadowFactorAt(new Vector4(-4, 0, 0, 1), up), 0f);
		System.out.println("PASS testPoint_pcfDoesNotLeakLightAcrossTheSeamOfTwoFaces");
	}

	@Test
	public void testDirectional_pcfKeepsTheShadowOfAThinLedgeAttachedToItsWall() {
		System.out.println("***** Test soft shadows : the shadow of a thin ledge on a wall starts at the ledge, it is not pushed away by the bias *****");

		// A wall (front face on the plane y = 0, normal -Y) and a thin ledge on it: 0.06 thick, 0.1 deep, at z = 3
		World world = new World();
		Box wall = new Box(6, 0.2f, 6);
		wall.setTransformation(new Translation(new Vector4(0, 0.1f, 3, 1)));
		world.addElement(wall);
		Box ledge = new Box(4, 0.1f, 0.06f);
		ledge.setTransformation(new Translation(new Vector4(0, -0.05f, 3, 1)));
		world.addElement(ledge);
		world.build();
		world.worldProject();

		// The sun comes from the front and above: it travels towards the wall (+Y), and down
		Vector3 direction = new Vector3(0, 0.5f, -1f).normalize();
		Vector3 wallNormal = new Vector3(0, -1, 0);
		float underside = 3f - 0.03f;
		for (ShadowFilter filter : ShadowFilter.values()) {
			DirectionalLight sun = new DirectionalLight(direction, 1f);
			sun.setShadowFilter(filter);
			sun.initShadowing(null, null, world);
			sun.generateShadowMap(world);

			// The ledge sticks out by 0.1: the rays that graze its front underside edge reach the wall 0.2 lower.
			// A wall point 0.07 under the ledge is well inside the shadow, and one 0.5 under it well outside.
			assertEquals(filter + ": wall just under the ledge", 0f, sun.shadowFactorAt(new Vector4(0, 0, underside - 0.07f, 1), wallNormal), 0f);
			assertEquals(filter + ": wall far under the ledge", 1f, sun.shadowFactorAt(new Vector4(0, 0, underside - 0.5f, 1), wallNormal), 0f);
			assertEquals(filter + ": wall above the ledge", 1f, sun.shadowFactorAt(new Vector4(0, 0, underside + 0.5f, 1), wallNormal), 0f);
		}
		System.out.println("PASS testDirectional_pcfKeepsTheShadowOfAThinLedgeAttachedToItsWall");
	}

	// ---- Pixels: the factor reaches the image ----

	private static BufferedImage renderSphereOnFloor(ShadowFilter filter) {
		System.setProperty("java.awt.headless", "true");
		World world = new World();
		world.setBackgroundColor(Color.BLACK);
		Trellis floor = new Trellis(12, 12, 8, 8);
		floor.setColor(new Color(200, 200, 200));
		world.addElement(floor);
		Sphere ball = new Sphere(1f, 24);
		ball.setColor(new Color(200, 200, 200));
		ball.setTransformation(new Translation(new Vector4(0, 0, 1, 1)));
		world.addElement(ball);
		world.build();

		Lighting lighting = new Lighting(new AmbientLight(0.05f));
		SpotLight spot = new SpotLight(new Vector4(4, -2, 8, 1), new Vector3(new Vector4(4, -2, 8, 1), new Vector4(0, 0, 0, 1)).normalize(), 30f, (float) Math.toRadians(50));
		spot.setShadowFilter(filter);
		lighting.addSpotLight(spot);
		Camera camera = new Camera(new Vector4(0, -9, 7, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 300);
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, lighting, camera, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	@Test
	public void testRender_pcfSoftensTheEdgesOnly() {
		System.out.println("***** Test soft shadows : rendered image, PCF changes only a small band of pixels and adds intermediate levels *****");
		BufferedImage hard = renderSphereOnFloor(ShadowFilter.HARD);
		BufferedImage soft = renderSphereOnFloor(ShadowFilter.PCF_3X3);
		assertEquals(hard.getWidth(), soft.getWidth());

		int differing = 0, total = hard.getWidth() * hard.getHeight();
		Set<Integer> hardLevels = new HashSet<>(), softLevels = new HashSet<>();
		for (int y = 0; y < hard.getHeight(); y++) {
			for (int x = 0; x < hard.getWidth(); x++) {
				if (hard.getRGB(x, y) != soft.getRGB(x, y)) differing++;
				hardLevels.add(new Color(hard.getRGB(x, y)).getGreen());
				softLevels.add(new Color(soft.getRGB(x, y)).getGreen());
			}
		}
		System.out.println("differing pixels: " + differing + " of " + total + ", grey levels hard " + hardLevels.size() + ", soft " + softLevels.size());
		assertTrue("the filter changes the image", differing > 0);
		assertTrue("but only along the edges of the shadows: " + differing + " of " + total, differing < total / 10);
		assertTrue("more grey levels with the penumbra", softLevels.size() >= hardLevels.size());
		System.out.println("PASS testRender_pcfSoftensTheEdgesOnly");
	}
}
