package com.aventura.tools.color;

import java.awt.Color;

import com.aventura.math.vector.Tools;

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
 * This static class provides low level color services on top of the java.awt.Color class
 * It allows to add, multiply (modulation) 2 or more colors and provides the resulting color.
 * It also provides services to increment all component of a color or to multiply them by a scalar
 * 
 * @author Olivier BARRY
 * @since November 2016
 * 
 */

public class ColorTools {
	
	/**
	 * Multiply components of a Color by the same scalar
	 * 
	 * @param c the Color to be multiplied
	 * @param mult the multiplication scalar (float)
	 * @return a newly created Color resulting from the operation
	 */
	public static Color multColor(Color c, float mult) {
	
		float[] c_array = c.getRGBColorComponents(null);

		float r = clip(c_array[0]*mult);
		float g = clip(c_array[1]*mult);
		float b = clip(c_array[2]*mult);
		
		return new Color(r, g, b);
	}
	
	/**
	 * @param c the Color to be incremented
	 * @param inc the increment to be added to all components
	 * @return a newly created Color resulting from the operation
	 */
	public static Color incColor(Color c, float inc) {
		
		float[] c_array = c.getRGBColorComponents(null);

		float r = clip(c_array[0]+inc);
		float g = clip(c_array[1]+inc);
		float b = clip(c_array[2]+inc);
		
		return new Color(r, g, b);		
	}
	
	/**
	 * Addition of 2 Colors.
	 * 
	 * @param c1 the first Color
	 * @param c2 the second Color
	 * @return a newly created Color that is the results of the addition of the 2 Colors passed in parameters
	 */
	public static Color addColors(Color c1, Color c2) {
		
		float[] c1_array = c1.getRGBColorComponents(null);
		float[] c2_array = c2.getRGBColorComponents(null);
		
		float r = clip(c1_array[0]+c2_array[0]);
		float g = clip(c1_array[1]+c2_array[1]);
		float b = clip(c1_array[2]+c2_array[2]);
		
		return new Color(r, g, b);
	}
	
	/**
	 * Multiplication or modulation of 2 Colors.
	 * 
	 * @param c1 the first Color
	 * @param c2 the second Color
	 * @return a newly created Color that is the results of the multiplication of the 2 Colors passed in parameters
	 */
	public static Color multColors(Color c1, Color c2) {
		
		float[] c1_array = c1.getRGBColorComponents(null);
		float[] c2_array = c2.getRGBColorComponents(null);
		
		float r = clip(c1_array[0]*c2_array[0]);
		float g = clip(c1_array[1]*c2_array[1]);
		float b = clip(c1_array[2]*c2_array[2]);
		
		return new Color(r, g, b);		
	}
	
	/**
	 * Addition of several Colors.
	 * 
	 * @param ctab an array containing a set of Colors to be added
	 * @return a newly created Color that is the result of the addition of all Colors passed in array
	 */
	public static Color addColors(Color[] ctab) {

		float r = 0;
		float g = 0;
		float b = 0;
		
		for (int i=0; i<ctab.length; i++) {
			float[] c_array = ctab[i].getRGBColorComponents(null);
			r = r + c_array[0];
			g = g + c_array[1];
			b = b + c_array[2];
		}
		return new Color(clip(r), clip(g), clip(b));				
	}
	
	/**
	 * Multiplication of several Colors.
	 * 
	 * @param ctab n array containing a set of Colors to be multiplied
	 * @return a newly created Color that is the result of the multiplication of all Colors passed in array
	 */
	public static Color multColors(Color[] ctab) {
		float r = 0;
		float g = 0;
		float b = 0;
		
		for (int i=0; i<ctab.length; i++) {
			float[] c_array = ctab[i].getRGBColorComponents(null);
			r = r * c_array[0];
			g = g * c_array[1];
			b = b * c_array[2];
		}
		
		return new Color(clip(r), clip(g), clip(b));
	}
	
	/**
	 * Interpolate 2 colors with a parameter t in [0, 1]
	 * @param c1
	 * @param c2
	 * @param t
	 * @return a new interpolated Color
	 */
	public static Color interpolateColors(Color c1, Color c2, float t) {
		
		// Get RGB components of c1 and c2
		float[] c1_array = c1.getRGBColorComponents(null);
		float[] c2_array = c2.getRGBColorComponents(null);
		
		// Interpolate each component
		float r = Tools.interpolate(c1_array[0], c2_array[0], t);
		float g = Tools.interpolate(c1_array[1], c2_array[1], t);
		float b = Tools.interpolate(c1_array[2], c2_array[2], t);
		
		// Return a new color
		return new Color(clip(r), clip(g), clip(b));
	}
	
	/**
	 * Clip component to the range [0,1].
	 * If outside the range it is rounded to 0 (for values below) and 1 (for values above)
	 * 
	 * @param c the component (float) to clip
	 * @return a new float clipped to range [0,1]
	 */
	protected static float clip(float c) {
		if (c>1) {
			return 1;
		} else if (c<0) {
			return 0;
		} else {
			return c;
		}
	}
	
	/**
	 * Calculate one Color by filtering linearly in 2 directions/axis with respective ratios
	 * This requires 4 Color samples (2 on each directions/axis).
	 * This is generally used for interpolating Color in a Texture plane
	 * 
	 * @param z11 First color sample on axis 1 (generally X)
	 * @param z12 Second color sample on axis 1 (generally X)
	 * @param z21 First color sample on axis 2 (generally Y)
	 * @param z22 Second color sample on axis 2 (generally Y)
	 * @param u_ratio Ratio of the first position on first axis (second position ratio is 1-u_ratio)
	 * @param v_ratio Ratio of the first position on second axis (second position ratio is 1-v_ratio)
	 * @return the interpolated Bi-linear filtered Color
	 */
	public static Color getBilinearFilteredColor(Color z11, Color z12, Color z21, Color z22, float u_ratio, float v_ratio) {
		
		// Components of the interpolated Color to calculate
		float r, g, b;
		
		// Get components of each provided Colors
		float[] z11_array = z11.getRGBColorComponents(null); 
		float[] z12_array = z12.getRGBColorComponents(null); 
		float[] z21_array = z21.getRGBColorComponents(null); 
		float[] z22_array = z22.getRGBColorComponents(null);
		
		// Calculate interpolation of each component
		r = getBilinearFilteredComponent(z11_array[0], z12_array[0], z21_array[0], z22_array[0], u_ratio, v_ratio);
		g = getBilinearFilteredComponent(z11_array[1], z12_array[1], z21_array[1], z22_array[1], u_ratio, v_ratio);
		b = getBilinearFilteredComponent(z11_array[2], z12_array[2], z21_array[2], z22_array[2], u_ratio, v_ratio);

		// Combine components to create the new Color
		return new Color(r, g, b);
	}
	
	/**
	 * Calculate one Bilinear filtered Color component
	 * 
	 * @param z11 First color sample on axis 1 (generally X)
	 * @param z12 Second color sample on axis 1 (generally X)
	 * @param z21 First color sample on axis 2 (generally Y)
	 * @param z22 Second color sample on axis 2 (generally Y)
	 * @param u_ratio Ratio of the first position on first axis (second position ratio is 1-u_ratio)
	 * @param v_ratio Ratio of the first position on second axis (second position ratio is 1-v_ratio)
	 * @return the interpolated Bi-linear filtered component
	 */
	protected static float getBilinearFilteredComponent(float z11, float z12, float z21, float z22, float u_ratio, float v_ratio) {
		
		float u_opposite = 1 - u_ratio;
		float v_opposite = 1 - v_ratio;

		// Calculate the interpolated value as per algorithm: f(x,y) = (1 - {x})((1 - {y})z11 + {y}z12) + {x}((1 - {y})z21 + {y}z22)
		// https://en.wikipedia.org/wiki/Bilinear_filtering
		float result = (z11*u_opposite+z21*u_ratio)*v_opposite+(z12*u_opposite+z22*u_ratio)*v_ratio;
		return result;
	}

}
