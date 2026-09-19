package com.aventura.model.camera;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.tracing.Tracer;

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
 * @since July 2016
 * 
 */

public class Camera {

	protected LookAt lookAt; // LookAt matrix or camera gUIView matrix
	protected Vector4 eye; // Eye of the camera, this is a point
	

	public Camera() {
	}

	public Camera(LookAt la, Vector4 e) {
		this.eye =e;
		this.lookAt = la;
	}

	public Camera(Matrix4 m, Vector4 e) {
		this.eye = e;
		this.lookAt = new LookAt(m);
	}

	/**
	 * 
	 * Construction of Camera's LookAt Matrix based on Points and Vector4
	 * 
	 * @param e the Eye point
	 * @param p the Point of interest
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public Camera(Vector4 e, Vector4 p, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating Camera with vectors e: "+e+" p: "+p+" u: "+u);
		this.eye = e;
		this.lookAt = new LookAt(e,p,u);
	}
	
	/**
	 * 
	 * Construction of Camera's LookAt Matrix based on Vector4 only (fwd and up)
	 * Useful for Camera light
	 * 
	 * @param f the fwd vector
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public Camera(Vector4 f, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating Camera with vectors f: "+ f +" u: "+u);
		this.eye = null;
		this.lookAt = new LookAt(f,u);
	}

	public void updateCamera(Vector4 e, Vector4 p, Vector4 u) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Updating Camera with vectors e: "+e+" p: "+p+" u: "+u);
		this.eye = e;
		this.lookAt.generateLookAt(e, p, u);
	}

	/**
	 * Update / add an Eye to the Camera and set the according translation to the LookAt matrix
	 * Usefull for Shadowing light (the Eye is not defined at begining of the process)
	 * @param e the point where to translate (generally the Eye of the Camera)
	 */
	public void updateCamera(Vector4 e) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Updating Camera with vectors e: "+e);
		this.eye = e;
		this.lookAt.addTranslation(e);
	}

	public Matrix4 getMatrix() {
		return (Matrix4) lookAt;
	}
	
	public void setEye(Vector4 e) {
		this.eye = e;
	}
	
	public Vector4 getEye() {
		return eye;
	}
	
	/**
	 * Getter for Up Vector (no setter since it is calculated)
	 * @return the Up Vector
	 */
	public Vector4 getUp() {
		return lookAt.getUp();
	}

	/**
	 * Getter for Side Vector (no setter since it is calculated)
	 * @return the Side Vector
	 */
	public Vector4 getSide() {
		return lookAt.getSide();
	}

	/**
	 * Getter for Forward Vector (no setter since it is calculated)
	 * @return the Forward Vector
	 */
	public Vector4 getForward() {
		return lookAt.getForward();
	}

}
