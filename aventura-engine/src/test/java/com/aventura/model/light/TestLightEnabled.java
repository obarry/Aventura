/*
 * ------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2016-2026 Olivier BARRY
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------
 */
package com.aventura.model.light;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.ImageView;

/**
 * A light switched off (Light.setEnabled(false)) stays in its Lighting but is ignored by the rendering.
 *
 * The scene: a floor of 30 x 30 units and a box, lit by a sun, an ambient light, a spot light and a point light. Each
 * test compares the images of a scene where a light is switched off with those of the same scene where that light was
 * never added: they must be the same, pixel for pixel. The counters of the shadow maps tell whether a map was drawn.
 *
 * @since Nov 2026
 */
public class TestLightEnabled {

	/** Which light of the scene is left out: switched off, or not added at all */
	enum Which { NONE, POINT, SPOT, SUN, AMBIENT }

	/** The scene, its lights and the images of a few frames */
	static class Scene {
		DirectionalLight sun;
		AmbientLight ambient;
		SpotLight spot;
		PointLight point;
		final List<int[]> images = new ArrayList<int[]>();
		// Faces of the cube map of the point light drawn since the first frame, after each frame
		final List<Long> pointFacesDrawn = new ArrayList<Long>();

		/**
		 * @param left the light left out
		 * @param switchedOff true: the light is added then switched off; false: it is not added
		 * @param glow true to make the lights visible (RenderContext.setLightGlow())
		 * @param offFrames for each frame, true if the light left out is off at that frame (false: on), when switchedOff;
		 *                  none: one frame, the light off
		 */
		Scene(Which left, boolean switchedOff, boolean glow, boolean... offFrames) {
			World world = new World();
			world.addElement(new Trellis(30, 30, 8, 8, (Texture) null)); // XY plane, z = 0
			Box box = new Box(2, 2, 4);
			box.setTransformation(new Translation(new Vector3(0, 0, 2)));
			world.addElement(box);
			world.build();

			sun = new DirectionalLight(new Vector3(1, 0.5f, -1), 0.6f);
			sun.setShadowMapSize(300);
			ambient = new AmbientLight(0.2f);
			spot = new SpotLight(new Vector4(-6, -6, 8, 1), new Vector3(1, 1, -1.5f), 30f, 0.8f, 0.6f, 0.4f);
			point = new PointLight(new Vector4(4, -3, 5, 1), 25f, 0.6f);
			spot.setShadowMapSize(256);
			point.setShadowMapSize(128);

			Lighting lighting = new Lighting();
			boolean add = switchedOff;
			if (left != Which.SUN || add) lighting.addDirectionalLight(sun);
			if (left != Which.AMBIENT || add) lighting.setAmbientLight(ambient);
			if (left != Which.SPOT || add) lighting.addSpotLight(spot);
			if (left != Which.POINT || add) lighting.addPointLight(point);
			for (ShadowingLight light : lighting.getShadowingLights()) {
				light.setStaticShadowCasters(true);
			}

			PerspectiveContext perspective = new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, 100);
			RenderContext context = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
			context.setLightGlow(glow);
			Camera camera = new Camera(new Vector4(14, -10, 9, 1), new Vector4(0, 0, 1, 1), Vector4.zAxis());
			ImageView view = new ImageView(perspective);
			RenderEngine engine = new RenderEngine(world, lighting, camera, context, perspective);
			engine.setView(view);

			int frames = Math.max(1, offFrames.length);
			boolean stats = Tracer.stats;
			Tracer.stats = false;
			try {
				for (int frame = 0; frame < frames; frame++) {
					if (switchedOff) light(left).setEnabled(offFrames.length > 0 && !offFrames[frame]);
					engine.render();
					BufferedImage image = view.getImageView();
					images.add(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
					pointFacesDrawn.add(point.getDrawnShadowMapFaces());
				}
			} finally {
				engine.close();
				Tracer.stats = stats;
			}
		}

		Light light(Which which) {
			switch (which) {
			case POINT: return point;
			case SPOT: return spot;
			case SUN: return sun;
			case AMBIENT: return ambient;
			default: return null;
			}
		}
	}

	private static void assertSameImage(String message, int[] expected, int[] actual) {
		assertArrayEquals(message, expected, actual);
	}

	private static boolean sameImage(int[] a, int[] b) {
		return java.util.Arrays.equals(a, b);
	}

	@Test
	public void testOnByDefault() {
		System.out.println("***** Test light enabled : a light is on by default *****");
		assertTrue(new PointLight(new Vector4(0, 0, 1, 1), 10f).isEnabled());
		assertTrue(new AmbientLight(0.2f).isEnabled());
		PointLight light = new PointLight(new Vector4(0, 0, 1, 1), 10f);
		light.setEnabled(false);
		assertFalse(light.isEnabled());
	}

	@Test
	public void testPointLightOff_sameImageAsWithout_noShadowMap() {
		System.out.println("***** Test light enabled : a point light switched off, same image as without it, no shadow map *****");
		Scene off = new Scene(Which.POINT, true, false);
		Scene without = new Scene(Which.POINT, false, false);
		assertSameImage("point light switched off = no point light", without.images.get(0), off.images.get(0));
		assertEquals("no shadow map drawn for a light switched off", 0, off.point.getDrawnShadowMapFaces());
		assertFalse("the point light does change the image when on",
				sameImage(new Scene(Which.NONE, false, false).images.get(0), without.images.get(0)));
	}

	@Test
	public void testSpotLightOff_sameImageAsWithout() {
		System.out.println("***** Test light enabled : a spot light switched off, same image as without it *****");
		Scene off = new Scene(Which.SPOT, true, false);
		assertSameImage("spot light switched off = no spot light", new Scene(Which.SPOT, false, false).images.get(0), off.images.get(0));
		assertEquals(0, off.spot.getDrawnShadowMapFaces());
	}

	@Test
	public void testSunOff_sameImageAsWithout() {
		System.out.println("***** Test light enabled : the sun switched off, same image as without it *****");
		Scene off = new Scene(Which.SUN, true, false);
		assertSameImage("sun switched off = no sun", new Scene(Which.SUN, false, false).images.get(0), off.images.get(0));
		assertEquals(0, off.sun.getDrawnShadowMapFaces());
	}

	@Test
	public void testAmbientOff_sameImageAsWithout() {
		System.out.println("***** Test light enabled : the ambient light switched off, same image as without it *****");
		Scene off = new Scene(Which.AMBIENT, true, false);
		assertSameImage("ambient switched off = no ambient", new Scene(Which.AMBIENT, false, false).images.get(0), off.images.get(0));
	}

	@Test
	public void testGlowOfALightOff_notDrawn() {
		System.out.println("***** Test light enabled : the glow of a point light switched off is not drawn *****");
		Scene off = new Scene(Which.POINT, true, true);
		assertSameImage("no halo for a light switched off", new Scene(Which.POINT, false, true).images.get(0), off.images.get(0));
	}

	@Test
	public void testSwitchedOnAgain_mapKept_sameImageAsBefore() {
		System.out.println("***** Test light enabled : a point light switched off then on again keeps its shadow map *****");
		// Frames: on, off, off, on
		Scene scene = new Scene(Which.POINT, true, false, false, true, true, false);
		long firstFrame = scene.pointFacesDrawn.get(0);
		assertTrue("the cube map is drawn at the first frame", firstFrame > 0);
		assertEquals("and never again", firstFrame, scene.point.getDrawnShadowMapFaces());
		assertEquals("its faces kept when the light is on again", firstFrame, scene.point.getKeptShadowMapFaces());
		assertSameImage("on again: same image as at the first frame", scene.images.get(0), scene.images.get(3));
		assertSameImage("off: same image at both frames", scene.images.get(1), scene.images.get(2));
		assertFalse("on and off differ", sameImage(scene.images.get(0), scene.images.get(1)));
	}
}
