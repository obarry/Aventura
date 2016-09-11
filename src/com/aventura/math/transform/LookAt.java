package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.tracing.Tracer;

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
 * LookAt camera Matrix
 * ====================
 * 
 * LookAt Matrix is built as follows:
 * 
 * Provided:
 * - Point of Interest: p
 * - Eyes: e
 * - Up Vector: u (the vertical for the world, not necessarily the "up" of the camera that will be constructed)
 * 
 * 1) construction of the forward Vector f:
 * 
 * 		f = (p - e) / |p - e|
 * 
 * 2) generation of the side Vector s:
 * 
 * 		s = f x u
 * 
 * 3) construction of a new up Vector for the camera (u'): up
 * 
 * 		up = s x f
 * 
 * Note that with (f, up, s) we just built an orthonormal basis
 * 
 * 4) construction of a Rotation Matrix representing a reorientation into our newly constructed orthonormal basis
 * 
 * 			/ s.x  up.x  f.x  0.0 \
 * 		R = | s.y  up.y  f.y  0.0 |
 * 			| s.z  up.z  f.z  0.0 |
 * 			\ 0.0  0.0   0.0  1.0 /
 * 
 * 5) Add to this matrix the translation to move the origin to the position of the camera (by translating the
 * resulting Vector of the above rotation by the negative of the camera's position)
 * 
 * 			/ s.x  up.x  f.x -e.x \
 * 		T = | s.y  up.y  f.y -e.y |
 * 			| s.z  up.z  f.z -e.z |
 * 			\ 0.0  0.0   0.0  1.0 /
 * 
 * T is the LookAt Matrix.
 * 
 * @author Bricolage Olivier
 * @since July 2016
 * 
 */

public class LookAt extends Matrix4 {

	/**
	 * 
	 * Construction of Camera's LookAt Matrix based on Points and Vector4
	 * 
	 * @param e the Eye point
	 * @param p the Point of interest
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public LookAt(Vector4 e, Vector4 p, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "LookAt(e, p, u) with Vector4");
	
		Vector4 f =  (p.minus(e)).normalize();
		Vector4 s =  f.times(u);
		Vector4 up = s.times(f);
		
		double[][] array = { { s.getX(), up.getX(), f.getX(), -e.getX() },
				 			 { s.getY(), up.getY(), f.getY(), -e.getY() },
				 			 { s.getZ(), up.getZ(), f.getZ(), -e.getZ() },
				 			 { 0.0     , 0.0      , 0.0     ,  1.0      } };
		
		try {
			this.setArray(array);
		} catch (Exception exc) {
			// Should never happen
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "LookAt matrix:\n"+ this);						
	}

	/**
	 * 
	 * 	 * Construction of Camera's LookAt Matrix based on Points and Vector3
	 * 
	 * @param e the Eye point
	 * @param p the Point of interest
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public LookAt(Vector3 e, Vector3 p, Vector3 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "LookAt(e, p, u) with Vector3");
		
		Vector3 f =  (p.minus(e)).normalize();
		Vector3 s =  f.times(u);
		Vector3 up = s.times(f);
		
		double[][] array = { { s.getX(), up.getX(), f.getX(), -e.getX() },
				 			 { s.getY(), up.getY(), f.getY(), -e.getY() },
				 			 { s.getZ(), up.getZ(), f.getZ(), -e.getZ() },
				 			 { 0.0     , 0.0      , 0.0     ,  1.0      } };
		
		try {
			this.setArray(array);
		} catch (Exception exc) {
			// Should never happen
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "LookAt matrix:\n"+ this);												
	}
	
	public LookAt(Matrix4 m) {
		super(m);
	}

}
