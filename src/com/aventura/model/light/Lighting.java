package com.aventura.model.light;

import java.util.ArrayList;

import com.aventura.math.vector.Vector3;
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
 * @author Bricolage Olivier
 * @since July 2016
 * 
 */


public class Lighting {
	
	protected ArrayList<Light> lights; // There can be multiple sources of light
	// TODO To clarify if an average vector makes sense by addition of different Vectors which can neutralize each others instead of cumulating
	protected Vector3 averageDirectionalLightVector = null;
	
	public Lighting() {
		lights = new ArrayList<Light>();
	}
	
	public Lighting(Light light) {
		lights.add(light);
		if (light.getClass() == DirectionalLight.class) {
			// No matter the point, this is the same Vector
			averageDirectionalLightVector = light.getLight(null).getVector3();
		}
	}
	
	public void addLight(Light light) {
		lights.add(light);
		int n=0;
		Vector4 v = null;
		for (int i=0; i<lights.size(); i++) {
			if (lights.get(i).getClass() == DirectionalLight.class) {
				// No matter the point, this is the same Vector
				if (v==null) {
					v = new Vector4(lights.get(i).getLight(null));
				} else { 
					v.plusEquals(lights.get(i).getLight(null));
				}
				n++;
			}
		}
		// Average -> divide by number of vectors
		// TODO Should we divide?
		averageDirectionalLightVector = v.times(1/(double)n).getVector3();
	}
	
	public Vector4[] getLightVectors(Vector4 point) {
		// Create a table of vector that is the result of all lights at this point
		Vector4[] lightVectors = new Vector4[lights.size()];
		for (int i=0; i<lights.size(); i++) {
				lightVectors[i]=lights.get(i).getLight(point);
		}
		return lightVectors;
	}
	
	public Vector3 getAverageDirectionalLightVector() {
		return averageDirectionalLightVector;
	}

}
