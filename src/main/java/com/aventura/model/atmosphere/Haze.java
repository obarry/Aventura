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
package com.aventura.model.atmosphere;

import java.awt.Color;

/**
 * The haze of the distance, an effect of the Atmosphere (see Atmosphere): the effect seen in a landscape, where the
 * contrasts weaken and the colors fade towards the color of the air as the objects get farther from the camera. The
 * pixels of the image are blended with the haze color by a factor that grows with their distance to the camera: 0 on
 * the camera, 1 (or maxFactor) very far away. The sky is not touched.
 *
 * It is a property of the scene, held by the Atmosphere of the World (World.setAtmosphere()), and it is only drawn if
 * RenderContext.setAtmosphere(true) switches the atmosphere on (see AtmosphereRenderer). Unlike a Fog, which is a
 * volume at a place of the World, the haze is everywhere and depends only on the distance to the camera.
 *
 * The distance is measured in the units of the scene. The strength is given as a visibility, like in
 * meteorology: the distance at which the contrast of an object with its background falls to 5%. A visibility
 * of 100 gives clear air for the first tens of units and a pale blue hill at 100.
 *
 * Three models of the factor f(d), with d the distance beyond the start distance:
 * <ul>
 * <li>EXPONENTIAL: f = 1 - exp(-k.d / visibility). The natural law of the extinction in a homogeneous medium,
 * progressive from the camera.</li>
 * <li>EXPONENTIAL_SQUARED: f = 1 - exp(-(k.d / visibility)^2). Clear air near the camera, then a faster fading.</li>
 * <li>LINEAR: f = d / (visibility - start), from 0 at the start distance to 1 at the visibility. Easy to tune.</li>
 * </ul>
 * The factor never exceeds maxFactor (0.95 by default), so that the farthest objects keep a trace of their color.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class Haze extends AtmosphericEffect {

	/** How the factor of the haze grows with the distance */
	public enum Model {
		EXPONENTIAL, EXPONENTIAL_SQUARED, LINEAR
	}

	public static final float DEFAULT_VISIBILITY = 100f;
	public static final float DEFAULT_MAX_FACTOR = 0.95f;

	// The contrast of the exponential models is e^-3 = 5% at the visibility. The squared model reaches it with sqrt(3)
	private static final float K_EXPONENTIAL = 3f;
	private static final float K_EXPONENTIAL_SQUARED = (float) Math.sqrt(3.0);

	private Model model = Model.EXPONENTIAL;
	private float visibility = DEFAULT_VISIBILITY;
	private float startDistance = 0f;
	private float maxFactor = DEFAULT_MAX_FACTOR;
	private Color color = null; // null = the background color of the World

	public Haze() {
	}

	/**
	 * @param visibility distance (scene units) at which the contrast falls to 5% (exponential models), or where the
	 *                   haze is complete (linear model)
	 */
	public Haze(float visibility) {
		setVisibility(visibility);
	}

	public Haze(Haze a) {
		this.enabled = a.enabled;
		this.model = a.model;
		this.visibility = a.visibility;
		this.startDistance = a.startDistance;
		this.maxFactor = a.maxFactor;
		this.color = a.color;
	}

	public Model getModel() {
		return model;
	}

	public Haze setModel(Model model) {
		if (model == null) throw new IllegalArgumentException("The model of the haze cannot be null");
		this.model = model;
		return this;
	}

	public float getVisibility() {
		return visibility;
	}

	/**
	 * @param visibility distance in scene units, greater than the start distance
	 */
	public Haze setVisibility(float visibility) {
		if (!(visibility > startDistance)) throw new IllegalArgumentException("The visibility must be greater than the start distance (" + startDistance + "): " + visibility);
		this.visibility = visibility;
		return this;
	}

	public float getStartDistance() {
		return startDistance;
	}

	/**
	 * Distance from the camera below which the air is perfectly clear (0 by default).
	 */
	public Haze setStartDistance(float startDistance) {
		if (startDistance < 0f || !(startDistance < visibility)) throw new IllegalArgumentException("The start distance must be in [0, visibility[ (visibility " + visibility + "): " + startDistance);
		this.startDistance = startDistance;
		return this;
	}

	public float getMaxFactor() {
		return maxFactor;
	}

	/**
	 * @param maxFactor largest share of the haze color in a pixel, in ]0, 1] (1: the farthest objects get the color of the haze)
	 */
	public Haze setMaxFactor(float maxFactor) {
		if (!(maxFactor > 0f && maxFactor <= 1f)) throw new IllegalArgumentException("The maximum factor must be in ]0, 1]: " + maxFactor);
		this.maxFactor = maxFactor;
		return this;
	}

	/**
	 * @return the color of the haze, or null if it is the background color of the World (the default: the objects
	 *         melt into the sky). A background that is not a sky (black for instance) calls for a color of its own.
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * @param color the color of the haze, null for the background color of the World
	 */
	public Haze setColor(Color color) {
		this.color = color;
		return this;
	}

	/**
	 * The share of the haze color in a pixel at the given distance from the camera.
	 *
	 * @param distance distance to the camera, in scene units
	 * @return the factor, in [0, maxFactor]: 0 up to the start distance, growing with the distance
	 */
	public float factorAt(float distance) {
		float d = distance - startDistance;
		if (d <= 0f) return 0f;
		float f;
		switch (model) {
		case EXPONENTIAL_SQUARED: {
			float t = K_EXPONENTIAL_SQUARED * d / (visibility - startDistance);
			f = 1f - (float) Math.exp(-t * t);
			break;
		}
		case LINEAR:
			f = d / (visibility - startDistance);
			break;
		case EXPONENTIAL:
		default:
			f = 1f - (float) Math.exp(-K_EXPONENTIAL * d / (visibility - startDistance));
			break;
		}
		return Math.min(f, maxFactor);
	}

	@Override
	public String toString() {
		return "Haze: " + (enabled ? "" : "(disabled) ") + model + ", visibility " + visibility + ", start " + startDistance + ", max factor " + maxFactor + ", color "
				+ (color == null ? "World background" : color);
	}
}
