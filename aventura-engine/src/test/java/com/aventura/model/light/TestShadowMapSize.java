package com.aventura.model.light;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.MapView;

/**
 * Tests for the shadow map size API (default, setShadowMapSize(), resetShadowMapSize()) and for the
 * rectangular shadow map of a DirectionalLight: the longest side has getShadowMapSize() pixels, the
 * other one follows the proportions of the area covered by the light.
 */
public class TestShadowMapSize {

	/** A flat floor of sizeX x sizeY, lit straight from above: the light box footprint is sizeX x sizeY. */
	private static World floor(float sizeX, float sizeY) {
		World world = new World();
		world.addElement(new Trellis(sizeX, sizeY, 8, 8));
		world.build();
		world.worldProject();
		return world;
	}

	private static DirectionalLight sunFromAbove() {
		return new DirectionalLight(new Vector3(0, 0, -1), 1f);
	}

	private static void initShadowing(ShadowingLight light, World world) {
		Camera camera = new Camera(new Vector4(6, -7, 4, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		light.initShadowing(new PerspectiveContext().getPerspective(), camera, world);
	}

	@Test
	public void testDefaultSize() {
		DirectionalLight light = sunFromAbove();
		assertEquals(ShadowingLight.DEFAULT_SHADOW_MAP_SIZE, light.getDefaultShadowMapSize());
		assertEquals(ShadowingLight.DEFAULT_SHADOW_MAP_SIZE, light.getShadowMapSize());
		assertEquals(0, light.getShadowMapWidth()); // no shadow map computed yet
	}

	@Test
	public void testSetAndResetSize() {
		DirectionalLight light = sunFromAbove();
		light.setShadowMapSize(2048);
		assertEquals(2048, light.getShadowMapSize());
		light.resetShadowMapSize();
		assertEquals(ShadowingLight.DEFAULT_SHADOW_MAP_SIZE, light.getShadowMapSize());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidSizeRejected() {
		sunFromAbove().setShadowMapSize(1);
	}

	@Test
	public void testRectangularMap_widerThanHigh() {
		DirectionalLight light = sunFromAbove();
		World world = floor(8, 4);
		initShadowing(light, world);
		int w = light.getShadowMapWidth();
		int h = light.getShadowMapHeight();
		assertEquals(1000, Math.max(w, h));
		assertEquals(500, Math.min(w, h));
	}

	@Test
	public void testRectangularMap_customSizeAndBothOrientations() {
		for (float[] dims : new float[][] { { 8, 4 }, { 4, 8 }, { 10, 3 }, { 3, 10 } }) {
			DirectionalLight light = sunFromAbove();
			light.setShadowMapSize(300);
			initShadowing(light, floor(dims[0], dims[1]));
			int w = light.getShadowMapWidth();
			int h = light.getShadowMapHeight();
			float ratio = Math.min(dims[0], dims[1]) / Math.max(dims[0], dims[1]);
			assertEquals("longest side for " + dims[0] + " x " + dims[1], 300, Math.max(w, h));
			assertEquals("shortest side for " + dims[0] + " x " + dims[1], 300 * ratio, Math.min(w, h), 1f);
		}
	}

	@Test
	public void testGeneratedMapCoversTheWholeSize() {
		DirectionalLight light = sunFromAbove();
		light.setShadowMapSize(200);
		World world = floor(8, 4);
		initShadowing(light, world);
		light.generateShadowMap(world);
		MapView map = light.getMap();
		// centered storage: 2*half+1 cells on each axis
		assertEquals(2 * (light.getShadowMapWidth() / 2) + 1, map.getViewWidth());
		assertEquals(2 * (light.getShadowMapHeight() / 2) + 1, map.getViewHeight());
		// the floor fills the whole map: every cell of the interior got a depth in [0, 1]
		assertTrue(map.get(map.getViewWidth() / 2, map.getViewHeight() / 2) <= 1f);
		assertTrue(map.get(2, 2) <= 1f);
		assertTrue(map.get(map.getViewWidth() - 3, map.getViewHeight() - 3) <= 1f);
		// and the floor is lit (not in its own shadow)
		assertEquals(1f, light.shadowFactorAt(new Vector4(1, 0.5f, 0, 1), new Vector3(0, 0, 1)), 0f);
	}

	@Test
	public void testExactLongestSideForAnyProportions() {
		Random r = new Random(42);
		for (int i = 0; i < 200; i++) {
			int size = 2 + r.nextInt(3000);
			float sizeX = 0.5f + r.nextFloat() * 50;
			float sizeY = 0.5f + r.nextFloat() * 50;
			DirectionalLight light = sunFromAbove();
			light.setShadowMapSize(size);
			initShadowing(light, floor(sizeX, sizeY));
			int w = light.getShadowMapWidth();
			int h = light.getShadowMapHeight();
			String msg = "size " + size + " for " + sizeX + " x " + sizeY + ": " + w + " x " + h;
			assertEquals(msg, size, Math.max(w, h));
			// short side: rounded up, never more than one pixel above the exact proportion
			float exact = size * Math.min(sizeX, sizeY) / Math.max(sizeX, sizeY);
			assertTrue(msg, Math.min(w, h) >= exact - 0.01f && Math.min(w, h) <= Math.max(1, exact + 1));
		}
	}
}
