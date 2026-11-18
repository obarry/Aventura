package com.aventura.engine;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector4;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;

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
 * Pixel coverage of TriangleRasterizer (pixel-center convention: pixel (x, y) is
 * covered if its center, the integer point (x, y), is inside the triangle, with
 * half-open bounds left <= x < right and bottom <= y < top).
 *
 * Triangles are given directly in screen (pixel) coordinates: each vertex gets an
 * orthographic projected position (x / halfWidth, y / halfHeight, 0.5, 1).
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class TestTriangleRasterizer {

	// 81 x 61 pixels: halfWidth = 40, halfHeight = 30
	private static final int HALF_W = 40;
	private static final int HALF_H = 30;

	/** Records every pixel handed to it, with the number of times it was received. */
	private static class RecordingConsumer implements FragmentConsumer {
		final Map<Long, Integer> hits = new HashMap<>();

		@Override
		public void consume(Fragment f) {
			hits.merge(key(f.getScreenX(), f.getScreenY()), 1, Integer::sum);
		}

		int count(int x, int y) {
			return hits.getOrDefault(key(x, y), 0);
		}

		static long key(int x, int y) {
			return ((long) x << 32) ^ (y & 0xffffffffL);
		}
	}

	private final PerspectiveContext ctx = new PerspectiveContext(2 * HALF_W, 2 * HALF_H, 1, 10, PerspectiveType.ORTHOGRAPHIC, 1);
	private final ZBuffer zBuffer = new ZBuffer(2 * HALF_W + 1, 2 * HALF_H + 1, HALF_W, HALF_H, 1f);
	private final TriangleRasterizer rasterizer = new TriangleRasterizer(ctx, zBuffer);

	private Vertex screenVertex(float x, float y) {
		Vertex v = new Vertex(x, y, 0);
		v.setProjPos(new Vector4(x / HALF_W, y / HALF_H, 0.5f, 1));
		return v;
	}

	private Triangle triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
		return new Triangle(screenVertex(x1, y1), screenVertex(x2, y2), screenVertex(x3, y3));
	}

	/** Rasterizes the rectangle [x0, x1] x [y0, y1] as 2 triangles sharing a diagonal. */
	private RecordingConsumer rasterizeRectangle(float x0, float y0, float x1, float y1) {
		RecordingConsumer consumer = new RecordingConsumer();
		rasterizer.rasterize(triangle(x0, y0, x1, y0, x1, y1), consumer);
		rasterizer.rasterize(triangle(x0, y0, x1, y1, x0, y1), consumer);
		return consumer;
	}

	@Test
	public void testRectangle_coversExactlyThePixelCenters_eachOnce() {
		System.out.println("***** Test TriangleRasterizer : a rectangle split in 2 triangles covers each pixel center inside it exactly once *****");

		RecordingConsumer c = rasterizeRectangle(-10.3f, -7.6f, 10.3f, 5.2f);

		// Centers inside: x in [-10, 10] (21 columns), y in [-7, 5] (13 rows)
		assertEquals(21 * 13, c.hits.size());
		for (int x = -10; x <= 10; x++) {
			for (int y = -7; y <= 5; y++) {
				assertEquals("pixel (" + x + ", " + y + ")", 1, c.count(x, y));
			}
		}
	}

	@Test
	public void testColumnAndRowZero_haveTheSameSizeAsTheOthers() {
		System.out.println("***** Test TriangleRasterizer : a 4 x 4 pixel square covers 4 x 4 pixels wherever it is (incl. across 0) *****");

		// A span of length 4 contains exactly 4 pixel centers, whatever its position: every pixel
		// has width 1. With the former (int) truncation, column/row 0 covered [-1, 1), so spans
		// straddling 0 lost a pixel.
		for (int i = -60; i <= 20; i++) {
			float a = i / 10f; // -6.0 .. 2.0, step 0.1
			RecordingConsumer c = rasterizeRectangle(a + 0.05f, a + 0.05f, a + 4.05f, a + 4.05f);
			assertEquals("square at " + (a + 0.05f), 16, c.hits.size());
		}
	}

	@Test
	public void testMirroredTriangles_coverMirroredPixels() {
		System.out.println("***** Test TriangleRasterizer : coverage is symmetric around 0 (no special case for negative coordinates) *****");

		RecordingConsumer c = new RecordingConsumer();
		rasterizer.rasterize(triangle(-12.4f, -3.3f, 3.7f, 9.8f, 8.1f, -11.6f), c);
		RecordingConsumer m = new RecordingConsumer();
		rasterizer.rasterize(triangle(12.4f, -3.3f, -3.7f, 9.8f, -8.1f, -11.6f), m);

		assertTrue(c.hits.size() > 100);
		assertEquals(c.hits.size(), m.hits.size());
		for (Long k : c.hits.keySet()) {
			int x = (int) (k >> 32);
			int y = (int) (long) k;
			assertEquals("mirror of (" + x + ", " + y + ")", 1, m.count(-x, y));
		}
	}

	@Test
	public void testFullScreen_coversEveryPixelOfTheBuffer_andNothingOutside() {
		System.out.println("***** Test TriangleRasterizer : a quad larger than the screen covers exactly the 2*half+1 x 2*half+1 buffer *****");

		RecordingConsumer c = rasterizeRectangle(-100f, -100f, 100f, 100f);

		assertEquals((2 * HALF_W + 1) * (2 * HALF_H + 1), c.hits.size());
		assertEquals(1, c.count(-HALF_W, -HALF_H));
		assertEquals(1, c.count(HALF_W, HALF_H));
		assertEquals(0, c.count(HALF_W + 1, 0));
		assertEquals(0, c.count(0, -HALF_H - 1));
	}

	@Test
	public void testRasterizedLines_countsRowsInsideTheScreen() {
		System.out.println("***** Test TriangleRasterizer : rasterized lines = rows of the triangle inside the screen *****");

		// Rows y in [-2, 7] (bottom -2.5, top 7.5): 10 lines
		rasterizer.resetStats();
		rasterizer.rasterize(triangle(-5f, -2.5f, 5f, -2.5f, 0f, 7.5f), new RecordingConsumer());
		assertEquals(10, rasterizer.getRasterizedLines());
		assertTrue(rasterizer.getRenderedPixels() > 0);

		// A sliver between x = 0.1 and x = 0.4: lines, but no pixel center covered
		rasterizer.resetStats();
		rasterizer.rasterize(triangle(0.1f, -3f, 0.4f, -3f, 0.25f, 3f), new RecordingConsumer());
		assertEquals(6, rasterizer.getRasterizedLines());
		assertEquals(0, rasterizer.getRenderedPixels());

		// Entirely above the screen: no line
		rasterizer.resetStats();
		rasterizer.rasterize(triangle(-5f, 50f, 5f, 50f, 0f, 60f), new RecordingConsumer());
		assertEquals(0, rasterizer.getRasterizedLines());
	}

	@Test
	public void testRasterizerStats_countsTrianglesWithLines() {
		System.out.println("***** Test TriangleRasterizer : RasterizerStats counts the triangles rendered with lines *****");

		RasterizerStats stats = new RasterizerStats();
		stats.recordTriangle(10, 25, 0); // lines and pixels
		stats.recordTriangle(6, 0, 0);   // sliver: lines, no pixel
		stats.recordTriangle(0, 0, 0);   // off screen
		stats.endFrame();

		assertEquals(3, stats.getRenderedTriangles());
		assertEquals(2, stats.getTrianglesWithLines());
		assertEquals(1, stats.getTrianglesWithPixels());
		assertTrue(stats.toString().contains("rendered with lines: 2"));
	}
}
