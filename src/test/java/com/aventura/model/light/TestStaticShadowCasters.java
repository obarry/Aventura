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
 * The shadow map kept from one frame to the next (ShadowingLight.setStaticShadowCasters(), performance audit R9).
 *
 * The scene: a floor of 30 x 30 units, a pole of 1 x 1 x 6 and a cube, lit by a sun (and, for some tests, a spot light
 * and a point light), seen by a camera turning around them. Each test renders the same sequence of frames twice, the
 * casters declared static or not, and checks that the images are the same, pixel for pixel, and that the maps were
 * kept or drawn again when expected.
 *
 * @since Nov 2026
 */
public class TestStaticShadowCasters {

	static final int FRAMES = 5;

	/** What may change between two frames of a sequence, at frame CHANGE_FRAME */
	enum Change { NONE, SUN_DIRECTION, MAP_SIZE, POLE_MOVED, POLE_MOVED_AND_INVALIDATED, FILTER }

	static final int CHANGE_FRAME = 3;

	/** The scene, its lights and the images of a sequence of frames */
	static class Sequence {
		final boolean staticCasters;
		final boolean moreLights;
		final Change change;
		Box pole;
		DirectionalLight sun;
		SpotLight spot;
		PointLight point;
		final List<int[]> images = new ArrayList<int[]>();

		Sequence(boolean staticCasters, boolean moreLights, Change change) {
			this.staticCasters = staticCasters;
			this.moreLights = moreLights;
			this.change = change;
			render();
		}

		private void render() {
			World world = new World();
			world.addElement(new Trellis(30, 30, 8, 8, (Texture) null)); // XY plane, z = 0
			pole = new Box(1, 1, 6);
			pole.setTransformation(new Translation(new Vector3(0, 0, 3)));
			world.addElement(pole);
			Box cube = new Box(2, 2, 2);
			cube.setTransformation(new Translation(new Vector3(-5, 4, 1)));
			world.addElement(cube);
			world.build();

			sun = new DirectionalLight(new Vector3(1, 0.5f, -1), 0.8f);
			sun.setShadowMapSize(300);
			Lighting lighting = new Lighting(sun, new AmbientLight(0.2f));
			if (moreLights) {
				spot = new SpotLight(new Vector4(-6, -6, 8, 1), new Vector3(1, 1, -1.5f), 30f, 0.8f, 0.6f, 0.4f);
				lighting.addSpotLight(spot);
				point = new PointLight(new Vector4(4, -3, 5, 1), 25f, 0.6f);
				lighting.addPointLight(point);
			}
			for (ShadowingLight light : lighting.getShadowingLights()) {
				light.setStaticShadowCasters(staticCasters);
			}

			PerspectiveContext perspective = new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, 100);
			RenderContext context = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
			context.setRenderThreads(2);
			Camera camera = new Camera(eye(0), new Vector4(0, 0, 1, 1), Vector4.zAxis());
			ImageView view = new ImageView(perspective);
			RenderEngine engine = new RenderEngine(world, lighting, camera, context, perspective);
			engine.setView(view);

			boolean stats = Tracer.stats;
			Tracer.stats = false;
			try {
				for (int frame = 0; frame < FRAMES; frame++) {
					camera.updateCamera(eye(frame), new Vector4(0, 0, 1, 1), Vector4.zAxis());
					if (frame == CHANGE_FRAME) change();
					engine.render();
					BufferedImage image = view.getImageView();
					images.add(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
				}
			} finally {
				engine.close();
				Tracer.stats = stats;
			}
		}

		private void change() {
			switch (change) {
			case SUN_DIRECTION:
				sun.setLightVector(new Vector3(-0.5f, -1, 1)); // Towards the light
				break;
			case MAP_SIZE:
				sun.setShadowMapSize(400);
				break;
			case POLE_MOVED:
			case POLE_MOVED_AND_INVALIDATED:
				pole.setTransformation(new Translation(new Vector3(4, 4, 3)));
				if (change == Change.POLE_MOVED_AND_INVALIDATED) sun.invalidateShadowMap();
				break;
			case FILTER:
				sun.setShadowFilter(ShadowFilter.PCF_3X3);
				break;
			default:
			}
		}

		/** The camera turns around the scene, 15 units away and 8 units above the floor */
		private static Vector4 eye(int frame) {
			double angle = -1.2 + 0.25 * frame;
			return new Vector4((float) (15 * Math.cos(angle)), (float) (15 * Math.sin(angle)), 8, 1);
		}
	}

	private static void assertSameImages(Sequence reference, Sequence kept) {
		for (int frame = 0; frame < FRAMES; frame++) {
			assertArrayEquals("frame " + frame + ": same image with the map kept", reference.images.get(frame), kept.images.get(frame));
		}
	}

	@Test
	public void testOffByDefault_theMapIsDrawnAtEachFrame() {
		System.out.println("***** Test static shadow casters : off by default, the map is drawn at each frame *****");
		assertFalse(new DirectionalLight(new Vector3(1, 0, -1), 1f).isStaticShadowCasters());
		Sequence sequence = new Sequence(false, false, Change.NONE);
		assertEquals(FRAMES, sequence.sun.getDrawnShadowMapFaces());
		assertEquals(0, sequence.sun.getKeptShadowMapFaces());
	}

	@Test
	public void testCameraMoving_theMapIsKept_sameImages() {
		System.out.println("***** Test static shadow casters : the camera moves, the map is drawn once and kept, same images *****");
		Sequence kept = new Sequence(true, false, Change.NONE);
		assertTrue(kept.sun.isStaticShadowCasters());
		assertEquals("drawn at the first frame only", 1, kept.sun.getDrawnShadowMapFaces());
		assertEquals("kept at the other frames", FRAMES - 1, kept.sun.getKeptShadowMapFaces());
		assertSameImages(new Sequence(false, false, Change.NONE), kept);
	}

	@Test
	public void testSunMoving_theMapIsDrawnAgain_sameImages() {
		System.out.println("***** Test static shadow casters : the sun moves, the map is drawn again by itself, same images *****");
		Sequence kept = new Sequence(true, false, Change.SUN_DIRECTION);
		assertEquals("drawn at the first frame and when the sun moved", 2, kept.sun.getDrawnShadowMapFaces());
		assertSameImages(new Sequence(false, false, Change.SUN_DIRECTION), kept);
	}

	@Test
	public void testMapSizeChanged_theMapIsDrawnAgain_sameImages() {
		System.out.println("***** Test static shadow casters : the size of the map changes, the map is drawn again, same images *****");
		Sequence kept = new Sequence(true, false, Change.MAP_SIZE);
		assertEquals(2, kept.sun.getDrawnShadowMapFaces());
		assertSameImages(new Sequence(false, false, Change.MAP_SIZE), kept);
	}

	@Test
	public void testFilterChanged_theMapIsKept_sameImages() {
		System.out.println("***** Test static shadow casters : the filter changes, the map is kept (read differently), same images *****");
		Sequence kept = new Sequence(true, false, Change.FILTER);
		assertEquals(1, kept.sun.getDrawnShadowMapFaces());
		assertSameImages(new Sequence(false, false, Change.FILTER), kept);
	}

	@Test
	public void testCasterMoved_invalidateShadowMap_sameImages() {
		System.out.println("***** Test static shadow casters : a caster moves, invalidateShadowMap() draws the map again, same images *****");
		Sequence reference = new Sequence(false, false, Change.POLE_MOVED);
		Sequence invalidated = new Sequence(true, false, Change.POLE_MOVED_AND_INVALIDATED);
		assertEquals(2, invalidated.sun.getDrawnShadowMapFaces());
		assertSameImages(reference, invalidated);

		// Without invalidateShadowMap(), the light cannot see that the pole moved: the shadow of the frame after is the
		// one of its former place. This is the contract of setStaticShadowCasters(), and the proof that the map is kept.
		Sequence stale = new Sequence(true, false, Change.POLE_MOVED);
		assertEquals(1, stale.sun.getDrawnShadowMapFaces());
		assertFalse("the moved pole casts its shadow at its former place",
				java.util.Arrays.equals(reference.images.get(CHANGE_FRAME), stale.images.get(CHANGE_FRAME)));
	}

	@Test
	public void testSpotAndPointLights_theMapsAreKept_sameImages() {
		System.out.println("***** Test static shadow casters : sun, spot light and the faces of a point light, maps kept, same images *****");
		Sequence kept = new Sequence(true, true, Change.NONE);
		assertEquals(1, kept.spot.getDrawnShadowMapFaces());
		assertEquals(FRAMES - 1, kept.spot.getKeptShadowMapFaces());
		long pointFaces = kept.point.getDrawnShadowMapFaces();
		assertTrue("the faces of the point light seeing the scene are drawn once: " + pointFaces, pointFaces >= 1 && pointFaces <= 6);
		assertEquals((FRAMES - 1) * pointFaces, kept.point.getKeptShadowMapFaces());
		assertSameImages(new Sequence(false, true, Change.NONE), kept);
	}

	@Test
	public void testTurnedOn_theFirstMapIsDrawn() {
		System.out.println("***** Test static shadow casters : setStaticShadowCasters() forgets the previous map *****");
		DirectionalLight sun = new DirectionalLight(new Vector3(1, 0, -1), 1f);
		sun.setShadowMapSize(50);
		World world = new World();
		world.addElement(new Box(1, 1, 1));
		world.build();
		world.worldProject();
		sun.setStaticShadowCasters(true);
		for (int i = 0; i < 2; i++) {
			sun.initShadowing(null, null, world);
			sun.generateShadowMap(world);
		}
		assertEquals(1, sun.getDrawnShadowMapFaces());
		sun.setStaticShadowCasters(true); // The World may have changed meanwhile
		sun.initShadowing(null, null, world);
		sun.generateShadowMap(world);
		assertEquals(2, sun.getDrawnShadowMapFaces());
		assertEquals(1, sun.getKeptShadowMapFaces());
	}
}
