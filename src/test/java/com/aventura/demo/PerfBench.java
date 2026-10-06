package com.aventura.demo;

import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.FrameTimer;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.light.SpotLight;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.ImageView;

/**
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
 *
 * Performance benchmark of the engine (see docs/PERFORMANCE_AUDIT.md). It renders the UrbanScape World off-screen
 * (1280 x 720, about 9,000 triangles, INTERPOLATE shading with specular reflection) along the helicopter flight,
 * in one of these modes:
 *
 *     noshadow   no shadows
 *     hard       the sun with hard shadows (ShadowFilter.HARD)
 *     pcf        the sun with soft shadows (ShadowFilter.PCF_3X3, UrbanScape's default)
 *     point      hard sun shadows, plus a PointLight with its cube-map shadows (6 faces)
 *     spot       hard sun shadows, plus a SpotLight with its shadows
 *     sunview    the camera looks towards the sun, no shadows, no visible lights
 *     sunglow    the same view with the visible sun (disc, halo, lens flare, light shafts)
 *
 * For each mode it prints the median time of a frame and of each of its phases (FrameTimer), the memory allocated
 * per frame by the rendering thread, and a checksum of the last image: an optimization that does not change the
 * images must leave every checksum unchanged.
 *
 * Run it from the project root, headless, ideally one mode per JVM (the JIT of one mode influences the next):
 *
 *     java -Djava.awt.headless=true -cp target/classes:target/test-classes com.aventura.demo.PerfBench [mode...] [-frames n] [-warmup n]
 *
 * In Windows PowerShell, quote the -D option (PowerShell splits it at the dot) and the class path (';' separator):
 *
 *     java "-Djava.awt.headless=true" -cp "target/classes;target/test-classes" com.aventura.demo.PerfBench pcf
 *
 * With no mode, all of them run in sequence. Default: 30 warm-up frames (not measured, the JIT compiles the
 * code meanwhile) then 30 measured frames. The times depend on the computer: compare runs on the same computer,
 * and repeat them (a few percent of noise is common).
 *
 * @author Olivier BARRY
 * @since October 2026
 */
public class PerfBench {

	static final String[] MODES = { "noshadow", "hard", "pcf", "point", "spot", "sunview", "sunglow" };

	/** Angle step of the helicopter flight between two frames, in radians */
	static final float FLIGHT_STEP = 0.05f;

	public static void main(String[] args) {
		int frames = 30, warmup = 30;
		java.util.List<String> modes = new java.util.ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-frames")) frames = Integer.parseInt(args[++i]);
			else if (args[i].equals("-warmup")) warmup = Integer.parseInt(args[++i]);
			else if (Arrays.asList(MODES).contains(args[i])) modes.add(args[i]);
			else throw new IllegalArgumentException("Unknown argument: " + args[i] + ", modes: " + Arrays.toString(MODES));
		}
		if (modes.isEmpty()) modes.addAll(Arrays.asList(MODES));

		Tracer.stats = false; // One line per frame would disturb the measure
		System.out.println("Mode        frame ms  (SETUP  SHADOWS  MAIN_PASS  OVERLAYS  PRESENT)  MB/frame  checksum");
		for (String mode : modes) {
			System.out.println(run(mode, warmup, frames));
		}
	}

	/** Renders warmup + frames images in this mode, and returns the result line */
	static String run(String mode, int warmup, int frames) {

		World world = UrbanScape.createWorld();
		Lighting lighting = UrbanScape.createLighting();
		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(world);
		Vector4 center = UrbanScape.getWorldCenter(UrbanScape.findCentralBuilding(world));

		// Shadow filter of the sun: PCF 3x3 is UrbanScape's default, HARD for the other modes
		if (!mode.equals("pcf")) {
			for (ShadowingLight light : lighting.getShadowingLights()) light.setShadowFilter(ShadowFilter.HARD);
		}
		if (mode.equals("point")) {
			lighting.addPointLight(new PointLight(new Vector4(center.getX() + 3, center.getY() - 3, center.getZ() * 2 + 3, 1), 30f, 1f));
		}
		if (mode.equals("spot")) {
			lighting.addSpotLight(new SpotLight(new Vector4(center.getX(), center.getY() - 8, center.getZ() * 2 + 6, 1),
					new Vector3(0, 0.6f, -1f), 30f, 1f, 0.6f, 0.4f));
		}
		boolean towardsSun = mode.startsWith("sun");

		PerspectiveContext perspective = UrbanScape.createPerspectiveContext();
		RenderContext context = UrbanScape.createRenderContext();
		context.setShadowing(!mode.equals("noshadow") && !towardsSun);
		if (mode.equals("sunglow")) {
			context.setLightGlow(true);
			context.setLightShafts(true);
		}

		Camera camera = new Camera(flight.getEye(0), flight.getFocus(), Vector4.zAxis());
		ImageView view = new ImageView(perspective);
		RenderEngine engine = new RenderEngine(world, lighting, camera, context, perspective);
		engine.setView(view);

		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		long threadId = Thread.currentThread().getId();

		int nbPhases = FrameTimer.Phase.values().length;
		float[] frameMillis = new float[frames];
		float[][] phaseMillis = new float[nbPhases][frames];
		long allocated = 0;

		for (int i = 0; i < warmup + frames; i++) {
			Vector4 eye = flight.getEye(i * FLIGHT_STEP);
			Vector4 focus = flight.getFocus();
			if (towardsSun) {
				// Look 45 degrees up towards the sun (UrbanScape's sun is 45 degrees above the horizon)
				Vector3 toSun = lighting.getDirectionalLights().get(0).getLightVectorAtPoint(null);
				focus = new Vector4(eye.getX() + toSun.getX() * 10, eye.getY() + toSun.getY() * 10, eye.getZ() + toSun.getZ() * 10 - 6, 1);
			}
			camera.updateCamera(eye, focus, Vector4.zAxis());

			long allocatedBefore = threads.getThreadAllocatedBytes(threadId);
			engine.render();
			if (i >= warmup) {
				int k = i - warmup;
				allocated += threads.getThreadAllocatedBytes(threadId) - allocatedBefore;
				FrameTimer timer = engine.getFrameTimer();
				frameMillis[k] = timer.getFrameMillis();
				for (FrameTimer.Phase phase : FrameTimer.Phase.values()) {
					phaseMillis[phase.ordinal()][k] = timer.getMillis(phase);
				}
			}
		}

		StringBuilder line = new StringBuilder(String.format("%-10s %8.1f  (", mode, median(frameMillis)));
		for (int p = 0; p < nbPhases; p++) {
			line.append(String.format(p == 0 ? "%.1f" : " %.1f", median(phaseMillis[p])));
		}
		line.append(String.format(")  %8.1f  %d", allocated / (double) frames / (1024 * 1024), checksum(view.getImageView())));
		return line.toString();
	}

	static float median(float[] values) {
		float[] sorted = values.clone();
		Arrays.sort(sorted);
		int n = sorted.length;
		return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
	}

	/** Checksum of the pixels of an image: equal checksums mean (in practice) identical images */
	static long checksum(BufferedImage image) {
		long h = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				h = h * 31 + image.getRGB(x, y);
			}
		}
		return h;
	}
}
