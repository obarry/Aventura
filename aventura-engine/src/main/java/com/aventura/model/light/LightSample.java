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

import com.aventura.math.vector.Vector3;

/**
 * What a Light gives to a point of space: the unit vector from the point towards the light, the distance to the
 * light and the intensity received, computed together by Light.sample(). For a point or spot light, the three come
 * from the same vector from the point to the light: a single subtraction, square root and division, instead of one
 * per question when they are asked separately (getLightVectorAtPoint(), getIntensity()). See performance audit, N3.
 *
 * A LightSample is reused from one point to the next (one per FragmentConsumer, so one per rendering thread): it is
 * created once and filled again by each call of Light.sample(). Do not retain it, or its direction vector, after the
 * next call: copy the values out.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public final class LightSample {

	private final Vector3 direction = new Vector3();
	private float distance = Float.NaN;
	private float intensity = 0f;

	/**
	 * Fills this sample. Called by the implementations of Light.sample().
	 * @param dx         x of the unit vector from the point towards the light (the zero vector if there is no
	 *                   direction, e.g. for an ambient light, or a point at the very position of a point light)
	 * @param dy         y of this vector
	 * @param dz         z of this vector
	 * @param distance   the distance from the point to the light: Float.POSITIVE_INFINITY for a directional light,
	 *                   Float.NaN when the light does not say (the default implementation of Light.sample())
	 * @param intensity  the intensity of the light received at the point (0 if the point is not lit by this light)
	 */
	public void set(float dx, float dy, float dz, float distance, float intensity) {
		this.direction.set(dx, dy, dz);
		this.distance = distance;
		this.intensity = intensity;
	}

	/** @return the unit vector from the point towards the light, or the zero vector. Reused: do not retain it. */
	public Vector3 getDirection() {
		return direction;
	}

	/** @return the distance from the point to the light (Float.POSITIVE_INFINITY for a directional light, Float.NaN if unknown) */
	public float getDistance() {
		return distance;
	}

	/** @return the intensity of the light received at the point */
	public float getIntensity() {
		return intensity;
	}
}
