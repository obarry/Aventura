package com.aventura.model.perspective;

import com.aventura.math.projection.Projection;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
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
 * A Perspective describes the "lens" of a Camera: the viewing volume (near plane window and
 * depth) and the corresponding projection matrix. It is expressed in world units, independently
 * of any pixel resolution (see PerspectiveContext for the pixel side).
 *
 * Frustum definition:
 * ------------------
 * 
 *     X (or Y)
 *        ^                       +
 *        |     View          -   |
 *        |     Plane     -       |
 *        | (top)     -           |
 *        | right +               |   ^
 *        |   -   |     View      |   |  width
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
 * The view volume is defined by:
 *    width  = right - left
 *    height = top - bottom
 *    depth  = far - near
 *    dist   = near - 0
 *  
 * Assuming a symmetric view volume (bottom = -top and left = -right) centered on the axis 
 *    top    = height/2
 *    bottom = -height/2
 *    right  = width/2
 *    left   = -width/2
 *    far    = dist + depth
 *    near   = dist
 *
 * Setters: setting one of width/height/dist/depth recomputes the six bounds assuming a SYMMETRIC
 * volume (an asymmetric one is re-centered); setting one of the six bounds recomputes
 * width/height/dist/depth. Every setter rebuilds the projection matrix: a new Projection instance
 * is created, so consumers must re-read getProjection() (ViewProjection does, on refresh()).
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
	
	/**
	 * Copy the dimensions of another perspective (the projection is rebuilt by the subclass).
	 * @param p the perspective to copy
	 */
	protected Perspective(Perspective p) {
		
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
	 * Create a symmetric perspective with the 4 eye-related dimension factors
	 * @param width width of the near plane window
	 * @param height height of the near plane window
	 * @param dist distance from the eye to the near plane
	 * @param depth distance from the near plane to the far plane
	 */
	protected Perspective(float width, float height, float dist, float depth) {
		
		this.width = width;
		this.height = height;
		this.dist = dist;
		this.depth = depth;
		
		calculateTBRLFN();
				
		// The creation of the projection matrix is delegated to the subclasses (Perspective class is abstract)
	}
	
	/**
	 * Create a perspective with the 6 frustum related dimensions, in the same (OpenGL-like) order as
	 * the subclasses' public constructors and the Projection classes.
	 * Caution: not verified: top > bottom, right > left, far > near >= 0
	 * @param left left bound of the near plane
	 * @param right right bound of the near plane
	 * @param bottom bottom bound of the near plane
	 * @param top top bound of the near plane
	 * @param near distance to the near plane
	 * @param far distance to the far plane
	 */
	protected Perspective(float left, float right, float bottom, float top, float near, float far) {
		
		this.top = top;
		this.bottom = bottom;
		this.right = right;
		this.left = left;
		this.far = far;
		this.near = near;
		
		calculateWHDD();
		
		// The creation of the projection matrix is delegated to the subclasses (Perspective class is abstract)
	}
	
	/**
	 * @return the type of this perspective (FRUSTUM or ORTHOGRAPHIC)
	 */
	public abstract PerspectiveType getType();
	
	/**
	 * @return a new, independent copy of this perspective (same type, same bounds, own projection)
	 */
	public abstract Perspective copy();
	
	/**
	 * Ratio between the size of the view window at eye distance d and its size on the near plane.
	 * d/near for a Frustum (Thales), 1 for an Orthographic perspective (parallel projection).
	 */
	protected abstract float windowScaleAt(float d);
	
	/**
	 * Computes the 8 corners (world space) of the view volume of this perspective when used by the
	 * given camera. Works for both perspective types and for asymmetric volumes (left != -right or
	 * bottom != -top).
	 * 
	 * frustum[0][*] are the corners on the near plane, frustum[1][*] on the far plane, in this
	 * order: (right, top), (left, top), (left, bottom), (right, bottom).
	 * 
	 * @param camera the camera using this perspective
	 * @return a [2][4] array of world-space points
	 */
	public Vector4[][] getFrustumFromEye(Camera camera) {
		
		Vector4[][] frustum = new Vector4[2][4];
		
		Vector4 eye = camera.getEye();
		// Camera basis: forward (normalized), up, and side = forward x up (same convention as LookAt,
		// where side is the +X axis of eye space)
		Vector4 fwd = camera.getForward().normalize();
		Vector4 up = camera.getUp();
		Vector4 side = fwd.cross(up).normalize();
		
		float[] planes = { near, far };
		for (int i = 0; i < 2; i++) {
			float d = planes[i];
			float k = windowScaleAt(d);
			Vector4 center = eye.plus(fwd.times(d));
			frustum[i][0] = center.plus(up.times(top * k)).plus(side.times(right * k));
			frustum[i][1] = center.plus(up.times(top * k)).plus(side.times(left * k));
			frustum[i][2] = center.plus(up.times(bottom * k)).plus(side.times(left * k));
			frustum[i][3] = center.plus(up.times(bottom * k)).plus(side.times(right * k));
		}
		
		if (Tracer.info) {
			String s = "";
			for (int i = 0; i<2; i++) {
				for (int j = 0; j<4; j++) {
					s = s + "frustum [" + i + "," + j + "] = " + frustum[i][j] + "\n";
				}
			}
			Tracer.traceInfo(this.getClass(), "Frustum : \n"+s);
		}

		return frustum;
	}
		
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
	
	/**
	 * Rebuilds the projection matrix from the current bounds (a new Projection instance).
	 */
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

		p += "*** Type  : " + getType() + "\n";

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
