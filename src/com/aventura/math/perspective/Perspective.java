package com.aventura.math.perspective;

import com.aventura.math.vector.Matrix4;

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
 Abstract class for all Perspective Matrices
 * 
 * @author Bricolage Olivier
 * @since June 1016
 *
 */
public abstract class Perspective extends Matrix4 {
	
	double left = 0;
	double right = 0;
	double top = 0;
	double bottom = 0;
	double near = 0;
	double far = 0;
	
	public double getLeft() {
		return left;
	}
	
	public double getRight() {
		return right;
	}
	
	public double getTop() {
		return top;
	}
	
	public double getBottom() {
		return bottom;
	}
	
	public double getNear() {
		return near;
	}
	
	public double getFar() {
		return far;
	}

}
