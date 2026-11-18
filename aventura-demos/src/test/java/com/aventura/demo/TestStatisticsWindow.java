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
package com.aventura.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.ImageView;

/**
 * The statistics shared by the demos (StatisticsWindow), on a scene that is not one of a demo: a box and a sphere.
 * Only the text is tested, without a window (the tests run headless): the statistics of an application without a
 * window are its report() and print().
 *
 * @since Nov 2026
 */
public class TestStatisticsWindow {

	@Test
	public void testReport_sceneThenLastImage() {
		System.out.println("***** Test StatisticsWindow : report of any scene *****");
		World world = new World();
		world.addElement(new Box(1, 1, 1));
		world.addElement(new Sphere(1, 8));
		world.build();
		Lighting lighting = new Lighting(new DirectionalLight(new Vector3(-1, 1, -1), 0.8f), new AmbientLight(0.2f), false);
		Camera camera = new Camera(new Vector4(8, 0, 2, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 250);
		RenderEngine renderer = new RenderEngine(world, lighting, camera, RenderContext.RENDER_STANDARD_INTERPOLATE, perspective);
		renderer.setView(new ImageView(perspective));
		renderer.render();

		StatisticsWindow statistics = new StatisticsWindow(null, "Two shapes", world, renderer);
		String report = statistics.report();
		System.out.println(report);
		String[] lines = report.split("\\R");
		assertEquals("Two shapes - statistics", lines[0]);
		assertTrue(lines[2].startsWith("GEOMETRY"));
		int triangles = world.getStatistics().getNbTriangles();
		assertTrue(lines[3], lines[3].matches("World +2 +" + triangles + " +\\d+ +100\\.0%"));
		assertTrue(report.contains("\n  box "));
		assertTrue(report.contains("\n  sphere "));
		assertTrue(report.contains("\n0 textures "));

		// The last image: the time of each phase, and the triangles the engine processed
		assertTrue(report.contains("\nLAST IMAGE\nFrame "));
		assertTrue(report.contains("MAIN_PASS"));
		assertTrue(report.contains("Render Engine - Processed: elements: 2, triangles: " + triangles));
		assertTrue("one line of the engine, not its whole statistics", report.trim().endsWith(lines[lines.length - 1]) && !report.contains("Rasterizer"));

		// A description of the image, asked each time the report is computed
		String[] mode = { "orbit" };
		statistics.setImageDescription(() -> "200 x 112 pixels, " + mode[0]);
		assertTrue(statistics.report().contains("\nLAST IMAGE (200 x 112 pixels, orbit)\n"));
		mode[0] = "fly";
		assertTrue(statistics.report().contains("\nLAST IMAGE (200 x 112 pixels, fly)\n"));
	}
}
