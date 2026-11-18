package com.aventura.demo;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.atmosphere.SphericalFog;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.light.SpotLight;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.ImageView;

/**
 * Tests of the multithreaded rasterization (RenderContext.setRenderThreads(), BandRasterizer): the screen is shared
 * between the threads by bands of rows, so the image and the statistics must be exactly the same whatever the number
 * of threads, including with shadows, textures, faces clipped by the near plane and a resized view. The shadow maps
 * are rasterized the same way, in the screen of their light: their statistics must not change either. So are the
 * post-processes (visible lights, atmosphere), by bands of the image.
 */
public class TestRenderThreads {

	/** Result of a rendering: the pixels of the image and the statistics of the rasterizer */
	private static class Result {
		int[] pixels;
		String stats;
		String shadowStats = "";
	}

	/**
	 * A view of UrbanScape (480 x 270 pixels) from a point of the helicopter flight, to be rendered (2 frames) with
	 * different numbers of threads. The options are set by the tests.
	 */
	private static class Scene {
		RenderingType type = RenderingType.INTERPOLATE;
		boolean lines = false;
		float flightAngle = 0.7f;
		boolean resize = false;     // Resize the view between the two frames
		boolean moreLights = false; // A SpotLight and a PointLight (6 shadow maps, triangles clipped by the near plane) besides the sun
		boolean glow = false;       // Visible lights with lens flare and light shafts, the camera looking up towards the sun
		boolean atmosphere = false; // Haze of the distance and a fog around the central building

		Scene type(RenderingType t) { type = t; return this; }
		Scene lines() { lines = true; return this; }
		Scene at(float angle) { flightAngle = angle; return this; }
		Scene resize() { resize = true; return this; }
		Scene moreLights() { moreLights = true; return this; }
		Scene glow() { glow = true; return this; }
		Scene atmosphere() { atmosphere = true; return this; }

		Result render(int threads) {
			System.setProperty("java.awt.headless", "true");
			boolean stats = Tracer.stats;
			Tracer.stats = false;
			World world = UrbanScape.createWorld();
			Lighting lighting = UrbanScape.createLighting();
			Vector4 center = UrbanScape.getWorldCenter(UrbanScape.findCentralBuilding(world));
			if (moreLights) {
				lighting.addPointLight(new PointLight(new Vector4(center.getX() + 3, center.getY() - 3, center.getZ() * 2 + 3, 1), 30f, 1f));
				lighting.addSpotLight(new SpotLight(new Vector4(center.getX(), center.getY() - 8, center.getZ() * 2 + 6, 1),
						new Vector3(0, 0.6f, -1f), 30f, 1f, 0.6f, 0.4f));
			}
			if (atmosphere) {
				world.setAtmosphere(new Atmosphere(new Haze(40f)).addFog(new SphericalFog(center, 6f, 3f)));
			}
			UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(world);
			Vector4 eye = flight.getEye(flightAngle), focus = flight.getFocus();
			if (glow) {
				// Look up towards the sun, as PerfBench sunglow
				Vector3 toSun = lighting.getDirectionalLights().get(0).getLightVectorAtPoint(null);
				focus = new Vector4(eye.getX() + toSun.getX() * 10, eye.getY() + toSun.getY() * 10, eye.getZ() + toSun.getZ() * 10 - 6, 1);
			}
			Camera camera = new Camera(eye, focus, Vector4.zAxis());
			PerspectiveContext perspective = new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, 300);
			RenderContext context = UrbanScape.createRenderContext();
			context.setRenderingType(type);
			context.setRenderingLines(lines);
			context.setTextureProcessing(true);
			context.setLightGlow(glow);
			context.setLightShafts(glow);
			context.setAtmosphere(atmosphere);
			context.setRenderThreads(threads);
			ImageView view = new ImageView(perspective);
			RenderEngine engine = new RenderEngine(world, lighting, camera, context, perspective);
			engine.setView(view);
			try {
				engine.render();
				if (resize) view.resize(400, 300);
				engine.render();
			} finally {
				engine.close();
				Tracer.stats = stats;
			}
			BufferedImage image = view.getImageView();
			Result result = new Result();
			result.pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			result.stats = engine.getRasterizerStats().toString();
			for (ShadowingLight light : lighting.getShadowingLights()) {
				result.shadowStats += light.getClass().getSimpleName() + ": " + light.getShadowMapStats() + "\n";
			}
			return result;
		}
	}

	private static void assertSameAsOneThread(Scene scene) {
		Result reference = scene.render(1);
		for (int threads : new int[] { 2, 3, 8 }) {
			Result result = scene.render(threads);
			String what = scene.type + " with " + threads + " threads: ";
			assertArrayEquals(what + "same image as with 1 thread", reference.pixels, result.pixels);
			assertEquals(what + "same statistics as with 1 thread", reference.stats, result.stats);
			assertEquals(what + "same shadow map statistics as with 1 thread", reference.shadowStats, result.shadowStats);
		}
	}

	@Test
	public void testShadedWithShadowsAndTextures_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : INTERPOLATE with shadows and textures, 1, 2, 3 and 8 threads give the same image *****");
		assertSameAsOneThread(new Scene());
	}

	@Test
	public void testFlatAfterResize_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : FLAT, view resized between two frames, same image whatever the number of threads *****");
		assertSameAsOneThread(new Scene().type(RenderingType.FLAT).at(2.1f).resize());
	}

	@Test
	public void testUnlit_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : UNLIT, same image whatever the number of threads *****");
		assertSameAsOneThread(new Scene().type(RenderingType.UNLIT).at(4.0f));
	}

	@Test
	public void testLinesAreDrawnByTheRenderingThread_sameImage() {
		System.out.println("***** Test RenderThreads : MONOCHROME (faces and edges), executed by the rendering thread alone, same image *****");
		assertSameAsOneThread(new Scene().type(RenderingType.MONOCHROME).lines());
	}

	@Test
	public void testShadowMapsOfThreeLightTypes_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : sun, spot and point light shadow maps, same image and maps whatever the number of threads *****");
		assertSameAsOneThread(new Scene().at(1.4f).moreLights());
	}

	@Test
	public void testVisibleLights_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : visible lights (sun disc and halo, lens flare, light shafts, halos), same image whatever the number of threads *****");
		assertSameAsOneThread(new Scene().moreLights().glow());
	}

	@Test
	public void testAtmosphere_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : haze and fog, same image whatever the number of threads *****");
		assertSameAsOneThread(new Scene().atmosphere());
	}

	@Test
	public void testRenderThreadsSetting() {
		System.out.println("***** Test RenderThreads : RenderContext.setRenderThreads() *****");
		RenderContext context = new RenderContext();
		assertEquals("One thread per processor by default", RenderContext.RENDER_THREADS_AUTO, context.getRenderThreads());
		assertEquals(3, context.setRenderThreads(3).getRenderThreads());
		assertEquals("Kept by the copy constructor", 3, new RenderContext(context).getRenderThreads());
		try {
			context.setRenderThreads(-1);
			fail("A negative number of threads must be refused");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			RenderContext.RENDER_STANDARD_FLAT.setRenderThreads(2);
			fail("A preset is immutable");
		} catch (IllegalStateException e) {
			// expected
		}
	}
}
