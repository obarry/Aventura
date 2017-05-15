package com.aventura.math.vector;

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
 * @date March 2016
 *
**/
public class Vector2 {
	
	protected double x;
	protected double y;
	
	public Vector2() {
		this.x = 0;
		this.y = 0;
	}
	
	public Vector2(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector2(Vector2 v) {
		this.x = v.x;
		this.y = v.y;
	}

	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public double length() {
		return Math.sqrt(x*x+y*y);
	}
	
	public double dot(Vector2 w) {
		return x*w.x+y*w.y;
	}
	
	public void timesEquals(double d) {
		this.x = x*d;
		this.y = y*d;
	}
	
	public Vector2 times(double d) {
		Vector2 r = new Vector2();
		r.x = this.x*d;
		r.y = this.y*d;

		return r;
	}
	
	public Vector2 equals() {
		Vector2 w = new Vector2(this.x, this.y);
		return w;
	}
	
	public static boolean equals(Vector2 v1, Vector2 v2) {
		if (v1.x == v2.x && v1.y == v2.y)  return true; else return false;
	}
	
	/**
	 * Vector2 addition V=V+W. This Vector2 (V) is modified and contains the result of the operation.
	 * @param w the Vector2 to be added to this Vector2
	 */
	public void plusEquals(Vector2 w) {
		this.x+=w.x;
		this.y+=w.y;
	}
	
	/**
	 * return sum of V = this vector + W the provided vector
	 * 
	 * @param w provided vector
	 * @return v+w
	 */
	public Vector2 plus(Vector2 w) {
		Vector2 s = new Vector2(x+w.x, y+w.y);
		return s;
	}

	/**
	 * Vector2 subtraction V=V-W. This Vector2 (V) is modified and contains the result of the operation.
	 * @param w the Vector2 to be subtracted to this Vector3
	 */
	public void minusEquals(Vector2 w) {
		this.x-=w.x;
		this.y-=w.y;
	}

	/**
	 * return difference of V = this vector - W the provided vector
	 * 
	 * @param w provided vector
	 * @return v+w
	 */
	public Vector2 minus(Vector2 w) {
		Vector2 d = new Vector2(x-w.x, y-w.y);
		return d;
	}
	
	
	public void normalize() {
		double length = this.length();
		this.x = x/length;
		this.y = y/length;
	}

}
