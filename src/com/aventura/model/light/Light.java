package com.aventura.model.light;

import java.awt.Color;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public abstract class Light {
	
	public static Color DEFAULT_LIGHT_COLOR = Color.WHITE;
	
	protected Color lightColor = DEFAULT_LIGHT_COLOR;
	protected float intensity;

	
	// Get light vector (or null vector for ambient light) at a given point of world space
	public abstract Vector3 getLightVector(Vector4 point);
	
	public abstract void setLightVector(Vector3 light);
	
	// Get intensity of light at a given point of world space
	public abstract float getIntensity(Vector4 point);
	
	public abstract void setIntensity(float intensity);
		
	// Get color of light
	public abstract Color getLightColor(Vector4 point);
	
	public abstract void setLightColor(Color c);

	// Get color of light
	public Color getBaseLightColor() {
		return lightColor;
	}

}
