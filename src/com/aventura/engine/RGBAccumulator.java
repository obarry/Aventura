package com.aventura.engine;

import java.awt.Color;

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
 * A mutable RGB accumulator used to combine multiple light contributions
 * (ambient + each light's diffuse/specular) into a final pixel color WITHOUT
 * allocating an intermediate java.awt.Color object at every combination step.
 * Only one Color is ever created, in toColor(), once the accumulation for a
 * given pixel is complete.
 *
 * LIFECYCLE: reused across pixels the same way Fragment is (see its
 * class-level contract) -- a ShadingConsumer owns one instance and calls
 * reset() at the start of each consume() call, rather than allocating a new
 * accumulator per pixel.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class RGBAccumulator {

	private float r, g, b;

	// Scratch array reused to pull components out of a java.awt.Color without allocating a new
	// float[3] on every call -- Color.getRGBColorComponents(float[]) writes into it in place.
	private final float[] scratch = new float[3];

	public void reset() {
		r = 0f;
		g = 0f;
		b = 0f;
	}

	/** Adds color * scale to the running total. */
	public void add(Color color, float scale) {
		color.getRGBColorComponents(scratch);
		r += scratch[0] * scale;
		g += scratch[1] * scale;
		b += scratch[2] * scale;
	}

	/** Adds (colorA * colorB) * scale to the running total, without allocating the intermediate product Color. */
	public void addProduct(Color colorA, Color colorB, float scale) {
		colorA.getRGBColorComponents(scratch);
		float ar = scratch[0], ag = scratch[1], ab = scratch[2];
		colorB.getRGBColorComponents(scratch);
		r += ar * scratch[0] * scale;
		g += ag * scratch[1] * scale;
		b += ab * scratch[2] * scale;
	}

	/** Adds colorA * colorB (no extra scale) to the running total. */
	public void addProduct(Color colorA, Color colorB) {
		addProduct(colorA, colorB, 1f);
	}

	/**
	 * Materializes the single java.awt.Color for this accumulated result. Each channel is clamped
	 * to [0, 1] first -- java.awt.Color's float constructor throws IllegalArgumentException outside
	 * that range, which summed, unclamped light contributions can easily exceed.
	 */
	public Color toColor() {
		return new Color(clamp(r), clamp(g), clamp(b));
	}

	private static float clamp(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}