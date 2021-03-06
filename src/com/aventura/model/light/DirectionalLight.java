package com.aventura.model.light;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2021 Olivier BARRY
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
 * Since they have no position in space, directional directional have infinite range and the intensity
 * of light they radiate does not diminish over distance.
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class DirectionalLight extends Light {
	
	protected Vector3 direction;
	protected Vector3 normalized_direction;
	
	public DirectionalLight(Vector3 direction) {
		super();
		this.direction = direction;
		this.normalized_direction = new Vector3(direction).normalize();
		this.intensity = direction.length();
	}
	
	public DirectionalLight(Vector3 direction, float intensity) {
		super();
		this.normalized_direction = new Vector3(direction).normalize();
		this.intensity = intensity;
		this.direction = this.normalized_direction.times(intensity);
	}

	@Override
	public Vector3 getLightVector(Vector4 point) {
		// Same direction vector in all world space by definition of Directional Light
		return this.direction;
	}
	
	@Override
	public Vector3 getLightNormalizedVector(Vector4 point) {
		// TODO Auto-generated method stub
		return this.normalized_direction;
	}

	@Override
	public float getIntensity(Vector4 point) {
		// Same intensity in any point of world space as Directional light have infinite range
		return intensity;
	}

	@Override
	public Color getLightColor(Vector4 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLightVector(Vector3 light) {
		this.direction = light;
		this.normalized_direction = new Vector3(light).normalize();
		this.intensity = light.length();
	}

	@Override
	public void setLightNormalizedVector(Vector3 light) {
		// TODO Auto-generated method stub
		this.normalized_direction = light.normalize();
		this.direction = this.normalized_direction.times(intensity);
	}

	@Override
	public void setIntensity(float intensity) {
		this.intensity = intensity;
		this.direction = this.normalized_direction.times(intensity);
	}

	@Override
	public void setLightColor(Color c) {
		// TODO Auto-generated method stub
		
	}

}
