package com.aventura.model.perspective;

import com.aventura.math.vector.Matrix4;

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
	Matrix4 projection;
	
	public Perspective (float width, float height, float depth, float dist) {
		
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.dist = dist;
		
		left = -width/2;
		right = width/2;
		bottom = -height/2;
		top = height/2;
		near = dist;
		far = dist + depth;
		
	}
	
	public float getLeft() {
		return left;
	}

	public void setLeft(float left) {
		this.left = left;
	}

	public float getRight() {
		return right;
	}

	public void setRight(float right) {
		this.right = right;
	}

	public float getBottom() {
		return bottom;
	}

	public void setBottom(float bottom) {
		this.bottom = bottom;
	}

	public float getTop() {
		return top;
	}

	public void setTop(float top) {
		this.top = top;
	}

	public float getNear() {
		return near;
	}

	public void setNear(float near) {
		this.near = near;
	}

	public float getFar() {
		return far;
	}

	public void setFar(float far) {
		this.far = far;
	}

	public void setDepth(float depth) {
		this.depth = depth;
	}
	
	public float getDepth() {
		return depth;
	}

	public void setDist(float dist) {
		this.dist = dist;
	}
	
	public float getDist() {
		return dist;
	}


}
