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
 * Vector and Matrix tools
 * 
 * @author Olivier BARRY
 * @since June 2016
 * 
 */
/**
 * @author Olivier BARRY
 *
 */
public class Tools {
	
	/**
	 * Interpolate 2 Points4 A and B (or Vectors4) with a parameter t (gradient)
	 * t = 0: returns A
	 * t = 1: returns B
	 * 0<t<1: returns the interpolated point between A and B
	 * t<0 or t>1; returns the interpolated point beyond A or beyond B (on the (AB) line)
	 * @param A the Vector4 point
	 * @param B the Vector4 point
	 * @param t the interpolation parameter (gradient)
	 * @return a Vector4 interpolated on the (AB) line
	 */
	public static Vector4 interpolate(Vector4 A, Vector4 B, double t) {
		
		Vector4 P = new Vector4();
		
		P = A.times(1-t).plus(B.times(t));
		
		return P;
	}
	
	/**
	 * Interpolate 2 Points3 A and B (or Vectors3) with a parameter t (gradient)
	 * t = 0: returns A
	 * t = 1: returns B
	 * 0<t<1: returns the interpolated point between A and B
	 * t<0 or t>1; returns the interpolated point beyond A or beyond B (on the (AB) line)
	 * @param A the Vector3 point
	 * @param B the Vector3 point
	 * @param t the interpolation parameter (gradient)
	 * @return a Vector3 interpolated on the (AB) line
	 */
	public static Vector3 interpolate(Vector3 A, Vector3 B, double t) {
		
		Vector3 P = new Vector3();
		
		P = A.times(1-t).plus(B.times(t));
		
		return P;
	}
	
	/**
	 * Interpolate 2 Points2 A and B (or Vectors2) with a parameter t (gradient)
	 * t = 0: returns A
	 * t = 1: returns B
	 * 0<t<1: returns the interpolated point between A and B
	 * t<0 or t>1; returns the interpolated point beyond A or beyond B (on the (AB) line)
	 * @param A the Vector2 point
	 * @param B the Vector2 point
	 * @param t the interpolation parameter (gradient)
	 * @return a Vector2 interpolated on the (AB) line
	 */
	public static Vector2 interpolate(Vector2 A, Vector2 B, double t) {
		
		Vector2 P = new Vector2();
		
		P = A.times(1-t).plus(B.times(t));
		
		return P;
	}
	
	
	/** Interpolate a scalar value with a parameter t (gradient) - Double version
	 * t = 0: returns a
	 * t = 1: returns b
	 * 0<t<1: returns the interpolated value between a and b
	 * t<0 or t>1; returns the interpolated value beyond a or beyond b
	 * @param a
	 * @param b
	 * @param t the gradient
	 * @return double
	 */
	public static double interpolate(double a, double b, double t) {
		return a*(1-t)+b*t;
	}
	
	/** Interpolate a scalar value with a parameter t (gradient) - Float version
	 * t = 0: returns a
	 * t = 1: returns b
	 * 0<t<1: returns the interpolated value between a and b
	 * t<0 or t>1; returns the interpolated value beyond a or beyond b
	 * @param a
	 * @param b
	 * @param t the gradient
	 * @return float
	 */
	public static float interpolate(float a, float b, float t) {
		return a*(1-t)+b*t;
	}
	

}
