package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2021 Olivier BARRY
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
 * Reference:
 * Constructing the lookat() matrix directly
 * http://www.cs.virginia.edu/~gfx/Courses/1999/intro.fall99.html/lookat.html
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
 * 			/  s.x  s.y  s.z  0.0 \
 * 		R = | up.x up.y up.z  0.0 |
 * 			| -f.x -f.y -f.z  0.0 |
 * 			\ 0.0   0.0  0.0  1.0 /
 * 
 * 5) Combine this matrix with the translation matrix to move the origin to the position of the camera (by translating the
 * resulting Vector of the above rotation by the negative of the camera's position)
 * 
 * 			/ 1.0  0.0  0.0 -e.x \
 * 		T = | 0.0  1.0  0.0 -e.y |
 * 			| 0.0  0.0  1.0 -e.z |
 * 			\ 0.0  0.0  0.0  1.0 /
 * 
 * LookAt Matrix : L = R.T
 * 
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class LookAt extends Matrix4 {
	
	protected Vector4 f, s, up; // Calculated Forward, Side and Up vectors

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
	
		generateLookAt(e, p, u);												
	}

	/**
	 * Construction of Camera's LookAt Matrix based on Points and Vector3
	 * 
	 * @param e the Eye point
	 * @param p the Point of interest
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public LookAt(Vector3 e, Vector3 p, Vector3 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "LookAt(e, p, u) with Vector3");
		
		Vector4 e4 = new Vector4(e);
		Vector4 p4 = new Vector4(p);
		Vector4 u4 = new Vector4(u);
		
		generateLookAt(e4, p4, u4);												
	}
	
	/**
	 * Construction of Camera's LookAt Matrix based on forward vector only (no points), useful for 'camera light' initialization
	 * 
	 * @param f the forward vector
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public LookAt(Vector4 f, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "LookAt(f, u) with Vector4");
		
		generateLookAt(f, u);												
	}
	
	/**
	 * Construction of Camera's LookAt Matrix based on Points and Vector4
	 * 
	 * @param e the Eye point
	 * @param p the Point of interest
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public void generateLookAt(Vector4 e, Vector4 p, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "createLookAt(e, p, u)");
		
		// The transformation matrix for the camera is based on a reorientation + a translation (eye - origin) 

		// Build forward, up and side Vectors
		f =  (p.minus(e)).normalize();
		s =  (f.times(u)).normalize();
		up = (s.times(f)).normalize();

		// Construct array of Reorientation Matrix
		float[][] array = { {  s.getX(),  s.getY(),  s.getZ(), 0.0f },
							{ up.getX(), up.getY(), up.getZ(), 0.0f },
							{ -f.getX(), -f.getY(), -f.getZ(), 0.0f },
							{  0.0f    ,  0.0f    ,  0.0f    , 1.0f } };

		// Build reorientation Matrix
		Matrix4 orientation = new Matrix4(array);

		// Prepare translation Vector
		Vector4 em = e.times(-1); 
		// Build the translation Matrix
		Matrix4 translation = new Translation(em);

		// Then combine the reorientation with the translation. Translation first then Reorientation.
		try {
			this.setArray(orientation.times(translation).getArray());
		} catch (Exception exc) {
			// Should never happen
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "LookAt matrix:\n"+ this);						
	}

	/**
	 * Construction of Camera's LookAt Matrix based on forward and up vectors only (no eye or PoI points)
	 * Useful for the "camera light" (needed for shading calculation) elaboration
	 * 
	 * @param f the forward vector
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public void generateLookAt(Vector4 fwd, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "createLookAt(f, u)");

		// From forward vector, build up and side Vectors
		this.f = fwd.normalize();
		s =  (f.times(u)).normalize();
		up = (s.times(f)).normalize();

		// Construct array of Reorientation Matrix
		float[][] array = { {  s.getX(),  s.getY(),  s.getZ(), 0.0f },
							{ up.getX(), up.getY(), up.getZ(), 0.0f },
							{ -f.getX(), -f.getY(), -f.getZ(), 0.0f },
							{  0.0f    ,  0.0f    ,  0.0f    , 1.0f } };

		// Then combine the reorientation with the translation. Translation first then Reorientation.
		try {
			this.setArray(array);
		} catch (Exception exc) {
			// Should never happen
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "LookAt matrix:\n"+ this);						
	}
	
	/**
	 * Construction of a LookAt Matrix from another Matrix4
	 * 
	 * @param m the Matrix4
	 */
	public LookAt(Matrix4 m) {
		super(m);
	}
	
	/**
	 * Getter for Up Vector (no setter since it is calculated)
	 * @return the Up Vector
	 */
	public Vector4 getUp() {
		return up;
	}

	/**
	 * Getter for Side Vector (no setter since it is calculated)
	 * @return the Side Vector
	 */
	public Vector4 getSide() {
		return s;
	}

	/**
	 * Getter for Forward Vector (no setter since it is calculated)
	 * @return the Forward Vector
	 */
	public Vector4 getForward() {
		return f;
	}

}
