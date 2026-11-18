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
package com.aventura.demo.temple;

/**
 * A small "value noise" generator, the source of every irregular shape of the demo: the relief of the island, the
 * bumps of the boulders and of the foliage, the grain of the procedural textures.
 *
 * <p>Value noise gives a pseudo-random value to each point of a grid of integer coordinates (a hash of the
 * coordinates: the same point always gets the same value, without storing anything) and interpolates smoothly
 * between them. Summing several "octaves" of it, each one twice as fine and half as strong as the previous one, gives
 * the fractal look of natural shapes ({@link #fbm}, for "fractional Brownian motion"): large shapes with smaller and
 * smaller details.
 *
 * <p>All the functions are pure (no state): the same arguments, seed included, always give the same value.
 *
 * @since Nov 2026
 */
final class Noise {

	private Noise() {
	}

	/**
	 * Pseudo-random value of a point of the integer grid, in [0, 1[: an integer hash that scrambles the bits of the
	 * coordinates and of the seed.
	 */
	static float hash(int x, int y, int z, int seed) {
		int h = seed + x * 374761393 + y * 668265263 + z * 1274126177;
		h = (h ^ (h >>> 13)) * 1274126177;
		h = h ^ (h >>> 16);
		return (h & 0xFFFFFF) / (float) 0x1000000;
	}

	/**
	 * Value noise at a point of space, in [0, 1[: the values of the 8 corners of the grid cell containing the point,
	 * interpolated along x, then y, then z. The interpolation weights follow an S curve ({@link #fade}) rather than a
	 * line, so that the result has no crease at the cell borders.
	 */
	static float value(float x, float y, float z, int seed) {
		int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
		float fx = fade(x - ix), fy = fade(y - iy), fz = fade(z - iz);

		float x00 = lerp(hash(ix, iy, iz, seed), hash(ix + 1, iy, iz, seed), fx);
		float x10 = lerp(hash(ix, iy + 1, iz, seed), hash(ix + 1, iy + 1, iz, seed), fx);
		float x01 = lerp(hash(ix, iy, iz + 1, seed), hash(ix + 1, iy, iz + 1, seed), fx);
		float x11 = lerp(hash(ix, iy + 1, iz + 1, seed), hash(ix + 1, iy + 1, iz + 1, seed), fx);
		return lerp(lerp(x00, x10, fy), lerp(x01, x11, fy), fz);
	}

	/**
	 * Fractal noise in [0, 1[: the sum of several octaves of value noise, each one with twice the frequency and half
	 * the amplitude of the previous one, divided by the sum of the amplitudes.
	 *
	 * @param octaves number of octaves: 1 is the plain value noise, more octaves add finer details
	 */
	static float fbm(float x, float y, float z, int octaves, int seed) {
		float sum = 0, amplitude = 1, total = 0;
		for (int o = 0; o < octaves; o++) {
			sum += amplitude * value(x, y, z, seed + o);
			total += amplitude;
			amplitude /= 2;
			x *= 2;
			y *= 2;
			z *= 2;
		}
		return sum / total;
	}

	/** S curve from 0 to 1 with a zero slope at both ends (3t² - 2t³). */
	static float fade(float t) {
		return t * t * (3 - 2 * t);
	}

	/** Linear interpolation: the value a fraction t of the way from a to b. */
	static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	/**
	 * Smooth step: 0 below edge0, 1 above edge1, and an S curve in between. Used to blend two shapes without a crease
	 * (the plateau of the temple into the slopes of the hill, the beach into the grass...).
	 */
	static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
		return fade(t);
	}
}
