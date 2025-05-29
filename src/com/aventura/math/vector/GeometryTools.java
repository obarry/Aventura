package com.aventura.math.vector;

import com.aventura.math.Constants;

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
 * Generic Geometry Tool Box
 * 
 * @author Olivier BARRY
 * @since May 2025
 * 
 */

public class GeometryTools {
	
	/**
	 * Calculate the center of a mono-dimensional array of points passed in arguments
	 * @param points an array of points (Vector4)
	 * @return the geometrical center of the array of points
	 */
	public static Vector4 center(Vector4[] points) {
		
		if (points != null) {

			// Initialize a point to be the resulting center of all points
			Vector4 center = new Vector4(0,0,0,1);

			// Add the x, y, z components of all points (keep w to 1 as the result should be a point)
			for (int i=0; i<points.length; i++) {
				center.x += points[i].x;
				center.y += points[i].y;
				center.z += points[i].z;
			}

			// Geometrical center : divide by the number of points
			center.x /= points.length;
			center.y /= points.length;
			center.z /= points.length;

			return center;

		} else {
			return null;
		}
	}
	
	/**
	 * Calculate the center of a bi-dimensional array of points passed in arguments
	 * @param points an array of points (Vector4)
	 * @return the geometrical center of the array of points
	 */
	public static Vector4 center(Vector4[][] points) {
		
		if (points != null) {

			// Initialize a point to be the resulting center of all points
			Vector4 center = new Vector4(0,0,0,1);

			// Add the x, y, z components of all points (keep w to 1 as the result should be a point)
			for (int i=0; i<points.length; i++) {
				for (int j=0; j<points[i].length; j++) {
				center.x += points[i][j].x;
				center.y += points[i][j].y;
				center.z += points[i][j].z;
				}
			}

			// Geometrical center : divide by the number of points
			int length = points.length * points[0].length; // Assuming all points[i] arrays have same length
			center.x /= length;
			center.y /= length;
			center.z /= length;

			return center;

		} else {
			return null;
		}
	}


}

