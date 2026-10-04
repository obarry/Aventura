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
package com.aventura.model.light;

import java.awt.Color;

/**
 * The look of a light in the picture, when the lights are made visible (RenderContext.setLightGlow(true)):
 * what is drawn (mode), its color, and the size of the core and of the glow, in world units, so that they
 * shrink with the distance like any object. Every setting is optional: a light without LightAppearance, or
 * with a null mode or color, follows the RenderContext and the defaults of its type.
 *
 * Fluent: <pre>light.setAppearance(new LightAppearance().setGlowRadius(1.2f).setGain(0.8f));</pre>
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class LightAppearance {

	/** Radius of the bright core, in world units */
	public static final float DEFAULT_CORE_RADIUS = 0.07f;
	/** Radius of the glow (where it has dropped to about a third of its center value), in world units */
	public static final float DEFAULT_GLOW_RADIUS = 0.55f;
	public static final float DEFAULT_GAIN = 1f;

	private LightGlowMode mode = null; // null = not set: the RenderContext, then the default of the type of light
	private Color color = null; // null = the color of the light
	private float coreRadius = DEFAULT_CORE_RADIUS;
	private float glowRadius = DEFAULT_GLOW_RADIUS;
	private float gain = DEFAULT_GAIN;

	public LightAppearance() {
	}

	/** @return the mode of this light, null if it is not set */
	public LightGlowMode getMode() {
		return mode;
	}

	/** @param mode the mode of this light, null to follow the RenderContext and the default of its type */
	public LightAppearance setMode(LightGlowMode mode) {
		this.mode = mode;
		return this;
	}

	/** @return the color of the core and of the glow, null for the color of the light */
	public Color getColor() {
		return color;
	}

	/** @param color the color of the core and of the glow, null for the color of the light */
	public LightAppearance setColor(Color color) {
		this.color = color;
		return this;
	}

	public float getCoreRadius() {
		return coreRadius;
	}

	/** @param coreRadius radius of the bright core, in world units, positive */
	public LightAppearance setCoreRadius(float coreRadius) {
		if (!(coreRadius > 0)) throw new IllegalArgumentException("Core radius must be positive: " + coreRadius);
		this.coreRadius = coreRadius;
		return this;
	}

	public float getGlowRadius() {
		return glowRadius;
	}

	/** @param glowRadius radius of the glow, in world units, positive */
	public LightAppearance setGlowRadius(float glowRadius) {
		if (!(glowRadius > 0)) throw new IllegalArgumentException("Glow radius must be positive: " + glowRadius);
		this.glowRadius = glowRadius;
		return this;
	}

	public float getGain() {
		return gain;
	}

	/** @param gain factor on the brightness of the glow (not of the core), not negative: 1 by default */
	public LightAppearance setGain(float gain) {
		if (!(gain >= 0)) throw new IllegalArgumentException("Gain must not be negative: " + gain);
		this.gain = gain;
		return this;
	}
}
