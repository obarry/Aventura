package com.aventura.model.light;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
 * Point Light source is one that radiates light equally in every direction from a single point in space.
 * The intensity of light naturally decreases with distance according to the inverse square law.
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class PointLight extends ShadowingLight {
	
	Vector4 light_point; // The light source
	float max_distance; // The max distance were this light is generating light
	
	public PointLight(Vector4 point, float max) {
		super();
		this.light_point = point;
		this.max_distance = max;
	}
	
	public PointLight(Vector4 point, float max, float intensity) {
		super(intensity);
		this.light_point = point;
		this.max_distance = max;
	}

	/**
	 * The return vector will hold the intensity information in its norm
	 */
	@Override
	public Vector3 getLightVectorAtPoint(Vector4 point) {
		// Return the NON-normalized vector from light source to point so that it contains
		Vector3 light_dir = new Vector3(point, this.light_point);
		float distance = light_dir.length();
		light_dir.normalize();
		light_dir.timesEquals(attenuationFunc(distance));
		return light_dir;
	}
	
	protected float attenuationFunc(float distance) {
		// First implementation of attenuation as linear law
		float attenuation = max_distance - distance;
		// Clamp to 0 if negative and return normalized attenuation between [0, 1]
		return attenuation >= 0 ? attenuation/max_distance : 0;
	}

	@Override
	public float getIntensity(Vector4 point) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Color getLightColorAtPoint(Vector4 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLightVector(Vector3 light) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setIntensity(float intensity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightColor(Color c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initShadowing(Perspective perspective, Camera camera_view, int map_size) {
		// TODO Auto-generated method stub
		
	}

}
