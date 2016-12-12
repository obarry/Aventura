package com.aventura.model.light;

import com.aventura.math.vector.Vector4;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016 Olivier BARRY
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
 * Directional Light also known as an infinite light source, radiates light in a single direction
 * from infinitely far away e.g. sun, whose rays can be considered parallel.
 * Since they have no position in space, directional lights have infinite range and the intensity
 * of light they radiate does not diminish over distance.
 *
 * @author Bricolage Olivier
 * @since July 2016
 * 
 */

public class DirectionalLight extends Light {
	
	protected Vector4 direction;
	protected double intensity;
	
	public DirectionalLight(Vector4 direction, double intensity) {
		super();
		this.direction = direction;
		this.intensity = intensity;
	}

	@Override
	public Vector4 getLight(Vector4 point) {
		// Same direction vector in all world space by definition of Directional Light
		return direction;
	}

	@Override
	public double getIntensity(Vector4 point) {
		// Same intensity in any point of world space as Directional light have infinite range
		return intensity;
	}

}
