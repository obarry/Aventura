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
 * Spot Light is similar to Point Light but has a preferred direction of radiation.
 * The intensity of a spot light is attenuated over distance in the same way that it is for a point light
 * and is also attenuated by another factor called the spot light effect.
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class SpotLight extends Light {

	@Override
	public Vector3 getLightVector(Vector4 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getIntensity(Vector4 point) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Color getLightColor(Vector4 point) {
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
	public Vector3 getLightNormalizedVector(Vector4 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLightNormalizedVector(Vector3 light) {
		// TODO Auto-generated method stub
		
	}

}
