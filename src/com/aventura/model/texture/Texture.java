package com.aventura.model.texture;

import java.awt.Color;

import com.aventura.tools.color.ColorTools;

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
 * @since April 2017
 */
public class Texture {
	
	// array containing data e.g. rgb values
	protected Color[][] tex;
	protected int width, height;
	
	public Texture(int width, int height) {
		this.width = width;
		this.height = height;
		tex = new Color[width][height];
	}
	
	/**
	 * Calculate the bilinear interpolated Color of this Texture at coordinates <s,t> with 0 <= s <= 1 and 0 <= t <= 1
	 * @param s
	 * @param t
	 * @return
	 */
	public Color getInterpolatedColor(double s, double t) {

		// Calculate the coordinates within the texture (-0.5 as per bressenham to 
		double u = s * this.width - 0.5;
		double v = t * this.height - 0.5;

		// Calculate the integer value of u and v
		int x = (int) Math.floor(u);
		int y = (int) Math.floor(v);

		// Calculate the frac value of u and v and their respective complement to 1
		float u_ratio = (float)u - x;
		float v_ratio = (float)v - y;
		
		// Calculate the interpolated value as per Bilinear Filtered algorithm
		Color result = ColorTools.getBilinearFilteredColor(tex[x][y], tex[x][y+1], tex[x+1][y], tex[x+1][y+1], u_ratio, v_ratio);
		return result;
	}
		
}
