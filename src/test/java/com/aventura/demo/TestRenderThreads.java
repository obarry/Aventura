package com.aventura.demo;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.ImageView;

/**
 * Tests of the multithreaded rasterization (RenderContext.setRenderThreads(), BandRasterizer): the screen is shared
 * between the threads by bands of rows, so the image and the statistics must be exactly the same whatever the number
 * of threads, including with shadows, textures, faces clipped by the near plane and a resized view.
 */
public class TestRenderThreads {

	/** Result of a rendering: the pixels of the image and the statistics of the rasterizer */
	private static class Result {
		int[] pixels;
		String stats;
	}

	/**
	 * Renders 2 frames of UrbanScape (480 x 270 pixels) from a point of the helicopter flight with the given
	 * rendering type and number of threads, optionally resizing the view between the two frames.
	 */
	private static Result render(RenderingType type, boolean lines, int threads, float flightAngle, boolean resize) {
		System.setProperty("java.awt.headless", "true");
		boolean stats = Tracer.stats;
		Tracer.stats = false;
		World world = UrbanScape.createWorld();
		Lighting lighting = UrbanScape.createLighting();
		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(world);
		Camera camera = new Camera(flight.getEye(flightAngle), flight.getFocus(), Vector4.zAxis());
		PerspectiveContext perspective = new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, 300);
		RenderContext context = UrbanScape.createRenderContext();
		context.setRenderingType(type);
		context.setRenderingLines(lines);
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
		return result;
	}

	private static void assertSameAsOneThread(RenderingType type, boolean lines, float flightAngle, boolean resize) {
		Result reference = render(type, lines, 1, flightAngle, resize);
		for (int threads : new int[] { 2, 3, 8 }) {
			Result result = render(type, lines, threads, flightAngle, resize);
			assertArrayEquals(type + " with " + threads + " threads: same image as with 1 thread", reference.pixels, result.pixels);
			assertEquals(type + " with " + threads + " threads: same statistics as with 1 thread", reference.stats, result.stats);
		}
	}

	@Test
	public void testShadedWithShadowsAndTextures_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : INTERPOLATE with shadows and textures, 1, 2, 3 and 8 threads give the same image *****");
		assertSameAsOneThread(RenderingType.INTERPOLATE, false, 0.7f, false);
	}

	@Test
	public void testFlatAfterResize_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : FLAT, view resized between two frames, same image whatever the number of threads *****");
		assertSameAsOneThread(RenderingType.FLAT, false, 2.1f, true);
	}

	@Test
	public void testUnlit_sameImageWhateverTheNumberOfThreads() {
		System.out.println("***** Test RenderThreads : UNLIT, same image whatever the number of threads *****");
		assertSameAsOneThread(RenderingType.UNLIT, false, 4.0f, false);
	}

	@Test
	public void testLinesAreDrawnByTheRenderingThread_sameImage() {
		System.out.println("***** Test RenderThreads : MONOCHROME (faces and edges), executed by the rendering thread alone, same image *****");
		assertSameAsOneThread(RenderingType.MONOCHROME, true, 0.7f, false);
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
