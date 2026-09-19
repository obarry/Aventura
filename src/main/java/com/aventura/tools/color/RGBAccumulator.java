package com.aventura.tools.color;

import java.awt.Color;

/**
 * ------------------------------------------------------------------------------
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

	/**
	 * Adds (sampleR, sampleG, sampleB) * scale to the running total, without needing a
	 * java.awt.Color at all -- for callers that already have raw components (e.g. Texture,
	 * extracting R/G/B from a packed ARGB int via bit shifts) and would otherwise have to
	 * construct a throwaway Color just to hand it to add(Color, float).
	 */
	public void add(float sampleR, float sampleG, float sampleB, float scale) {
		r += sampleR * scale;
		g += sampleG * scale;
		b += sampleB * scale;
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