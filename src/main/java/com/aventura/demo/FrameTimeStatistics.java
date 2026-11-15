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

import java.util.Locale;

/**
 * The rendering times of a series of images: how many, their average, the fastest and the slowest one. A demo adds
 * the time of each image it renders (add()), and asks for a summary at the end of the series: a lap of a flight, a
 * benchmark, a recording...
 *
 * The rendering time of a single image says little: it changes with what the camera sees, and from one image to the
 * next on the same view (other programs, the garbage collector, the processor changing its frequency). To compare
 * two rendering options, compare the average over the same series of views, e.g. a whole lap of a flight path: this
 * is what TempleRuins does at each lap of its helicopter tour.
 *
 * Two rates are given, which are not the same thing:
 * <ul>
 * <li>the images rendered per second of rendering (1000 / average time): what the engine can do;</li>
 * <li>the images displayed per second of the series (number of images / duration), when the duration is given: what
 *     the user sees, lower because of the display of the image and of the pace of the animation (a Swing Timer does
 *     not go faster than its delay).</li>
 * </ul>
 * Not thread-safe: to be used from a single thread (the Swing thread, in a demo).
 *
 * @since Nov 2026
 */
public class FrameTimeStatistics {

	private int count = 0;
	private double totalMillis = 0, minMillis = 0, maxMillis = 0;
	private double durationSeconds = 0;

	/** Adds the rendering time of an image, in milliseconds. */
	public void add(double millis) {
		minMillis = count == 0 ? millis : Math.min(minMillis, millis);
		maxMillis = count == 0 ? millis : Math.max(maxMillis, millis);
		totalMillis += millis;
		count++;
	}

	/**
	 * Adds time to the duration of the series, in seconds: the time elapsed between two images, rendering included.
	 * Optional: without it, the summary only gives the rendering times.
	 */
	public void addDuration(double seconds) {
		durationSeconds += seconds;
	}

	/** Forgets all the images: ready for a new series. */
	public void reset() {
		count = 0;
		totalMillis = minMillis = maxMillis = 0;
		durationSeconds = 0;
	}

	/** @return the number of images added */
	public int getCount() {
		return count;
	}

	/** @return the average rendering time of an image, in milliseconds (0 without any image) */
	public double getAverageMillis() {
		return count == 0 ? 0 : totalMillis / count;
	}

	/** @return the rendering time of the fastest image, in milliseconds (0 without any image) */
	public double getMinMillis() {
		return minMillis;
	}

	/** @return the rendering time of the slowest image, in milliseconds (0 without any image) */
	public double getMaxMillis() {
		return maxMillis;
	}

	/** @return the duration of the series, in seconds, as given to addDuration() */
	public double getDurationSeconds() {
		return durationSeconds;
	}

	/** @return the images rendered per second of rendering: 1000 / average time (0 without any image) */
	public double getRenderingRate() {
		return totalMillis > 0 ? 1000 * count / totalMillis : 0;
	}

	/** @return the images displayed per second of the series (0 if no duration was given) */
	public double getDisplayRate() {
		return durationSeconds > 0 ? count / durationSeconds : 0;
	}

	/**
	 * @return the summary on one line, e.g. "412 images in 31.2 s (13.2 per second), rendering: average 71.4 ms
	 *         (14.0 per second), fastest 38.2 ms (26.2), slowest 124.9 ms (8.0)"
	 */
	@Override
	public String toString() {
		if (count == 0) return "no image";
		StringBuilder summary = new StringBuilder();
		summary.append(count).append(count == 1 ? " image" : " images");
		if (durationSeconds > 0) {
			summary.append(String.format(Locale.ROOT, " in %.1f s (%.1f per second)", durationSeconds, getDisplayRate()));
		}
		summary.append(String.format(Locale.ROOT, ", rendering: average %.1f ms (%.1f per second), fastest %.1f ms (%.1f), slowest %.1f ms (%.1f)",
				getAverageMillis(), getRenderingRate(), minMillis, rate(minMillis), maxMillis, rate(maxMillis)));
		return summary.toString();
	}

	/** Images per second at a given time per image, in milliseconds */
	private static double rate(double millis) {
		return millis > 0 ? 1000 / millis : 0;
	}
}
