package com.aventura.math.vector;

import com.aventura.math.tools.MathTools;

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
 * @author Olivier BARRY
 * @date March 2016
 *
**/
public class Vector2 {
	
	protected float x;
	protected float y;
	
	public Vector2() {
		this.x = 0;
		this.y = 0;
	}
	
	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector2(Vector2 v) {
		this.x = v.x;
		this.y = v.y;
	}

	
	public float getX() {
		return this.x;
	}
	
	public float getY() {
		return this.y;
	}
	
	public void setX(float x) {
		this.x = x;
	}
	
	public void setY(float y) {
		this.y = y;
	}
	
	public float length() {
		return (float)Math.sqrt(x*x+y*y);
	}
	
	public float dot(Vector2 w) {
		return x*w.x+y*w.y;
	}
	
	public void timesEquals(float d) {
		this.x = x*d;
		this.y = y*d;
	}
	
	public Vector2 times(float d) {
		Vector2 r = new Vector2();
		r.x = this.x*d;
		r.y = this.y*d;

		return r;
	}
	
	/**
	 * @deprecated this no-arg method does not compare anything: it returns a copy of this Vector2,
	 * despite its "equals" name. It is a naming bug (see audit report) but is left untouched here to
	 * avoid an API change in this pass; use copy() instead, or equals(Vector2) to actually compare.
	 */
	@Deprecated
	public Vector2 equals() {
		return copy();
	}

	/**
	 * Return a new Vector2 with the same coordinates as this one (new method).
	 * This is what the misleadingly-named equals() above actually does; copy() is the correctly-named
	 * equivalent to use going forward.
	 * @return a new Vector2 equal to this one
	 */
	public Vector2 copy() {
		return new Vector2(this.x, this.y);
	}

	public static boolean equals(Vector2 v1, Vector2 v2) {
		return MathTools.equals(v1.x , v2.x) && MathTools.equals(v1.y , v2.y);
	}

	/**
	 * Compare this Vector2 with another (new method, mirrors Vector3.equals(Vector3)/Vector4.equals(Vector4)
	 * for consistency - Vector2 previously only had the static equals(Vector2, Vector2) form).
	 * @param w the other Vector2
	 * @return true if all the elements of this Vector2 are equal to the elements of w
	 */
	public boolean equals(Vector2 w) {
		return MathTools.equals(this.x, w.x) && MathTools.equals(this.y, w.y);
	}

	/**
	 * Squared length of the vector (new method, mirrors Vector3.lengthSquared/Vector4.lengthSquared).
	 * Avoids the sqrt() of length() when only a comparison between lengths is needed.
	 * @return the squared length (or squared 'norm') of this Vector2
	 */
	public float lengthSquared() {
		return this.x*this.x + this.y*this.y;
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
		float length = this.length();
		this.x = x/length;
		this.y = y/length;
	}

}