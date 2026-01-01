package com.aventura.model.light;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.color.ColorTools;

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
 * Ambient Light appears to come from every direction with equal intensity in any point of space
 *
 *
 * @author Olivier BARRY
 * @since December 2016
 * 
 */

public class AmbientLight extends Light {
		
	public AmbientLight(float intensity) {
		super();
		this.intensity = intensity;
	}

	@Override
	public Vector3 getLightVectorAtPoint(Vector4 point) {
		// No direction by definition of Ambient Light
		return new Vector3(Vector3.ZERO_VECTOR);
	}

	@Override
	public float getIntensity(Vector4 point) {
		// Same intensity of light at any point of space by definition of ambient light
		return intensity;
	}

	@Override
	public Color getLightColorAtPoint(Vector4 point) {
		// TODO Auto-generated method stub
		return ColorTools.multColor(lightColor, intensity);
	}

	@Override
	public void setLightVector(Vector3 light) {
		// No vector for Ambient light nothing to do
		
	}

	@Override
	public void setIntensity(float intensity) {
		this.intensity = intensity;
	}

	@Override
	public void setLightColor(Color c) {
		// TODO Auto-generated method stub
		
	}

}
