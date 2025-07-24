package com.aventura.model.perspective;

import com.aventura.math.projection.Projection;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2025 Olivier BARRY
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
 * Frustum definition:
 * ------------------
 * 
 *     X (or Y)
 *        ^                       +
 *        |     GUIView       -   |
 *        |     Plane     -       |
 *        | (top)     -           |
 *        | right +               |   ^
 *        |   -   |    GUIView    |   |  width
 * Camera +-------+---------------+---+--------------------------> -Z
 *            -   |   Frustum     |   | (height)
 *          left  +               |   v
 *        (bottom)    -           |
 *                        -       |
 *                            -   |
 *                                +
 *        0      near            far 
 *        <-------><-------------->
 *          dist        depth
 * 
 * The gUIView is defined by:
 *    width  = right - left
 *    height = top - bottom
 *    depth  = far - near
 *    dist   = near - 0
 *  
 * Assuming a symetric gUIView (bottom = -top and left = -right) centered on the origin 
 *    top    = height/2
 *    bottom = -height/2
 *    right  = width/2
 *    left   = -width/2
 *    far    = dist + depth
 *    near   = dist
 * 
 * ------------------------------------------------------------------------------ 
 *
 * @author Olivier BARRY
 * @since June 2024
 * 
 */

public abstract class Perspective {
	
	// Window & frustum
	float width = 0;
	float height = 0;
	float depth = 0;
	float dist = 0;
	
	// Other characteristics
	float left = 0;
	float right = 0;
	float bottom = 0;
	float top = 0;
	float near = 0;
	float far = 0;

	
	// Projection Matrix
	Projection projection;
	
	
	public Perspective(Perspective p) {
		
		this.width = p.width;
		this.height = p.height;
		this.depth = p.depth;
		this.dist = p.dist;
		
		this.top = p.top;
		this.bottom = p.bottom;
		this.right = p.right;
		this.left = p.left;
		this.far = p.far;
		this.near = p.near;
		
	}
	
	/**
	 * Create a perspective with the 4 eye-related dimension factors
	 * @param width
	 * @param height
	 * @param depth
	 * @param dist
	 */
	public Perspective(float width, float height, float dist, float depth) {
		
		this.width = width;
		this.height = height;
		this.dist = dist;
		this.depth = depth;
		
		calculateTBRLFN();
				
		// The creation of the projection matrix is delegated to the subclasses (Perspective class is abstract)
	}
	
	/**
	 * Create a perspective with the 6 frustum related dimensions
	 * @param top
	 * @param bottom
	 * @param right
	 * @param left
	 * @param far
	 * @param near
	 */
	public Perspective(float top, float bottom, float right, float left, float far, float near) {
		
		// Caution : not verified : top > botttom, right > left, far > near >=0
		
		this.top = top;
		this.bottom = bottom;
		this.right = right;
		this.left = left;
		this.far = far;
		this.near = near;
		
		calculateWHDD();
		
		// The creation of the projection matrix is delegated to the subclasses (Perspective class is abstract)
	}
	
	public abstract Vector4[][] getFrustumFromEye(Camera camera);
		
	private void calculateTBRLFN() {
		
		// Assuming that width, height, dist and depth are positive values
		
		left = -width/2;
		right = width/2;
		bottom = -height/2;
		top = height/2;
		near = dist;
		far = dist + depth;
	}
	
	private void calculateWHDD() {
		
		width = right - left;
		height = top - bottom;
		depth = far - near;
		dist = near;
		
	}
	
	public abstract void updateProjection();
	
	public Projection getProjection() {
		return projection;
	}
	
	// TBRLFN accessors
	
	public float getTop() {
		return top;
	}

	public void setTop(float top) {
		this.top = top;
		calculateWHDD();
		updateProjection();
	}

	public float getBottom() {
		return bottom;
	}

	public void setBottom(float bottom) {
		this.bottom = bottom;
		calculateWHDD();
		updateProjection();
	}

	public float getRight() {
		return right;
	}

	public void setRight(float right) {
		this.right = right;
		calculateWHDD();
		updateProjection();
	}

	public float getLeft() {
		return left;
	}

	public void setLeft(float left) {
		this.left = left;
		calculateWHDD();
		updateProjection();
	}

	public float getFar() {
		return far;
	}

	public void setFar(float far) {
		this.far = far;
		calculateWHDD();
		updateProjection();
	}

	public float getNear() {
		return near;
	}

	public void setNear(float near) {
		this.near = near;
		calculateWHDD();
		updateProjection();
	}
	
	// WHDD accessors

	public void setWidth(float width) {
		this.width = width;
		calculateTBRLFN();
		updateProjection();
	}
	
	public float getWidth() {
		return width;
	}
	
	public void setHeight(float height) {
		this.height = height;
		calculateTBRLFN();
		updateProjection();
	}
	
	public float getHeight() {
		return height;
	}
	
	public void setDepth(float depth) {
		this.depth = depth;
		calculateTBRLFN();
		updateProjection();
	}
	
	public float getDepth() {
		return depth;
	}

	public void setDist(float dist) {
		this.dist = dist;
		calculateTBRLFN();
		updateProjection();
	}
	
	public float getDist() {
		return dist;
	}
	
	public String toString() {
		
		String p = "";

		p += "*** Height: " + height + "\n";
		p += "*** Width : " + width + "\n";
		p += "*** Depth : " + depth + "\n";
		p += "*** Dist  : " + dist + "\n";
		p += "*** Left  : " + left + "\n";
		p += "*** Right : " + right + "\n";
		p += "*** Bottom: " + bottom + "\n";
		p += "*** Top   : " + top + "\n";
		p += "*** Near  : " + near + "\n";
		p += "*** Far   : " + far + "\n";

		return p;
	}


}
