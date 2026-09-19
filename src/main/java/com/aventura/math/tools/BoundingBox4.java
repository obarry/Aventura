package com.aventura.math.tools;

import java.util.Arrays;

import com.aventura.math.Constants;
import com.aventura.math.vector.Vector4;

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
 * Build a box surrounding a set of points
 * This box's faces are aligned to coordinate axis
 * 
 * @author Olivier BARRY
 * @since April 2021
 * 
 */
public class BoundingBox4 {
	
	Vector4[][] boxPoints; // 8 points of the box
	float minX, minY, minZ;
	float maxX, maxY, maxZ;
	
	public BoundingBox4() {
		boxPoints = new Vector4[2][4];
		for (int i=0; i<2; i++) {
			for (int j=0; j<4; j++) {
				boxPoints[i][j] = new Vector4();
			}
		}
	}
	
	/**
	 * Constructor of BoundingBox with an array of points
	 * Build a box containing all points by identifying the max and min coordinates of the set of points and building a box aligned
	 * to axis coordinates using max an min coordinates of the set of points.
	 * @param arrayOfPoints
	 */
	public BoundingBox4(Vector4[] arrayOfPoints) {
		
		// Initialize min an max coordinates to the first point's coordinates
		minX = maxX = arrayOfPoints[0].getX();
		minY = maxY = arrayOfPoints[0].getY();
		minZ = maxZ = arrayOfPoints[0].getZ();
		
		// Iterate on the rest of the list and keep the min and max of each coordinate
		for (int i=1; i<arrayOfPoints.length; i++) {
			float x = arrayOfPoints[i].getX();
			float y = arrayOfPoints[i].getY();
			float z = arrayOfPoints[i].getZ();
			if (x < minX) minX = x; else if (x > maxX) maxX = x;
			if (y < minY) minY = y; else if (y > maxY) maxY = y;
			if (z < minZ) minZ = z; else if (z > maxZ) maxZ = z;
		}
		// Then we have the min and max for each coordinate of all points in the set of points
		
		// We can build the bounding box
		boxPoints = new Vector4[2][4];
		// And define each of the 8 points of the rectangle (bounding box)
		// The box is aligned with coordinates axis
		boxPoints[0][0] = new Vector4(minX, minY, minZ, 1);
		boxPoints[0][1] = new Vector4(minX, minY, maxZ, 1);
		boxPoints[0][2] = new Vector4(minX, maxY, maxZ, 1);
		boxPoints[0][3] = new Vector4(minX, maxY, minZ, 1);
		boxPoints[1][0] = new Vector4(maxX, minY, minZ, 1);
		boxPoints[1][1] = new Vector4(maxX, minY, maxZ, 1);
		boxPoints[1][2] = new Vector4(maxX, maxY, maxZ, 1);
		boxPoints[1][3] = new Vector4(maxX, maxY, minZ, 1);
	}
	
	protected float getMaxX() {
		return maxX;
	}
	protected float getMaxY() {
		return maxY;
	}
	protected float getMaxZ() {
		return maxZ;
	}
	
	protected float getMinX() {
		return minX;
	}
	protected float getMinY() {
		return minY;
	}
	protected float getMinZ() {
		return minZ;
	}
	
	public float getWidth() {
		return maxX - minX;
	}
	
	public float getHeight() {
		return maxY - minY;
	}
	
	public float getDepth() {
		return maxZ - minZ;	
	}
	
	public Vector4 getP11() {
		return boxPoints[0][0];
	}

	public Vector4 getP12() {
		return boxPoints[0][1];
	}

	public Vector4 getP13() {
		return boxPoints[0][2];
	}

	public Vector4 getP14() {
		return boxPoints[0][3];
	}

	public Vector4 getP21() {
		return boxPoints[1][0];
	}

	public Vector4 getP22() {
		return boxPoints[1][1];
	}

	public Vector4 getP23() {
		return boxPoints[1][2];
	}

	public Vector4 getP24() {
		return boxPoints[1][3];
	}
	
	public String toString() {
		String s = "";

		for (int i = 0; i<2; i++) {
			for (int j = 0; j<4; j++) {
				s = s + "boxPoint [" + i + "," + j + "] = " + boxPoints[i][j] + "\n";
			}
		}
		return s;
	}

}
