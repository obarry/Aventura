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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests of FrameTimeStatistics: the average, fastest and slowest rendering times of a series of images.
 */
public class TestFrameTimeStatistics {

	@Test
	public void testEmpty() {
		FrameTimeStatistics times = new FrameTimeStatistics();
		assertEquals(0, times.getCount());
		assertEquals(0, times.getAverageMillis(), 0);
		assertEquals(0, times.getRenderingRate(), 0);
		assertEquals(0, times.getDisplayRate(), 0);
		assertEquals("no image", times.toString());
	}

	@Test
	public void testAverageMinMax() {
		FrameTimeStatistics times = new FrameTimeStatistics();
		times.add(40);
		times.add(120);
		times.add(80);
		times.add(60);
		assertEquals(4, times.getCount());
		assertEquals(75, times.getAverageMillis(), 1e-9);
		assertEquals(40, times.getMinMillis(), 0);
		assertEquals(120, times.getMaxMillis(), 0);
		assertEquals("1000 / 75", 13.3333, times.getRenderingRate(), 1e-3);
	}

	@Test
	public void testDisplayRateIsLowerThanRenderingRate() {
		FrameTimeStatistics times = new FrameTimeStatistics();
		for (int i = 0; i < 10; i++) {
			times.add(50); // 20 images per second of rendering
			times.addDuration(0.1); // But one image every 100 ms
		}
		assertEquals(1.0, times.getDurationSeconds(), 1e-9);
		assertEquals(20, times.getRenderingRate(), 1e-9);
		assertEquals(10, times.getDisplayRate(), 1e-9);
	}

	@Test
	public void testSummary() {
		FrameTimeStatistics times = new FrameTimeStatistics();
		times.add(50);
		assertEquals("1 image, rendering: average 50.0 ms (20.0 per second), fastest 50.0 ms (20.0), slowest 50.0 ms (20.0)", times.toString());
		times.add(25);
		times.addDuration(0.5);
		assertEquals("2 images in 0.5 s (4.0 per second), rendering: average 37.5 ms (26.7 per second), fastest 25.0 ms (40.0), slowest 50.0 ms (20.0)",
				times.toString());
	}

	@Test
	public void testReset() {
		FrameTimeStatistics times = new FrameTimeStatistics();
		times.add(50);
		times.addDuration(1);
		times.reset();
		assertEquals(0, times.getCount());
		assertEquals(0, times.getDurationSeconds(), 0);
		times.add(10);
		assertEquals("the fastest image of the new series only", 10, times.getMinMillis(), 0);
		assertEquals(10, times.getMaxMillis(), 0);
	}
}
