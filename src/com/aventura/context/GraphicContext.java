package com.aventura.context;

import com.aventura.math.perspective.Frustum;
import com.aventura.math.perspective.Orthographic;
import com.aventura.math.perspective.Perspective;
import com.aventura.math.vector.Matrix4;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2020 Olivier BARRY
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
 *        |     View          -   |
 *        |     Plane     -       |
 *        | (top)     -           |
 *        | right +               |   ^
 *        |   -   |    View       |   |  width
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
 * The view is defined by:
 *    width  = right - left
 *    height = top - bottom
 *    depth  = far - near
 *    dist   = near - 0
 *  
 * Assuming a symetric view (bottom = -top and left = -right) centered on the origin 
 *    top    = height/2
 *    bottom = -height/2
 *    right  = width/2
 *    left   = -width/2
 *    far    = dist + depth
 *    near   = dist
 * 
 * ------------------------------------------------------------------------------ 
 * 
 * PPU - Pixel Per Unit
 * The above data (width, height, depth, dist) are given in (camera) coordinates (floating point)
 * in a given Unit (can be meter or millimeter or whatever unit).
 * The size of the screen is thus defined in this unit.
 * To define the number of pixel, a ratio should be provided: the number of pixel per unit: PPU
 * 
 * 
 * @author Olivier BARRY
 * @since May 2016
 *
 */
public class GraphicContext {
	
	public static final int PERSPECTIVE_TYPE_FRUSTUM = 1;
	public static final int PERSPECTIVE_TYPE_ORTHOGRAPHIC = 2;
	
	public static final String PERSPECTIVE_TYPE_FRUSTUM_STRING = "PERSPECTIVE_TYPE_FRUSTUM";
	public static final String PERSPECTIVE_TYPE_ORTHOGRAPHIC_STRING = "PERSPECTIVE_TYPE_ORTHOGRAPHIC";
		
	// Perspective type
	int perspective_type = 0; // uninitialized

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

	// Pixel Per Unit
	int ppu = 0;
	int pixelWidth = 0; // Number of pixels on the X axis
	int pixelHeight = 0; // Number of pixels on the Y axis
	int pixelHalfWidth = 0;
	int pixelHalfHeight = 0;
	
	// Projection Matrix
	Matrix4 projection;

	// Width/Height ratio = 16/9
	public static GraphicContext GRAPHIC_DEFAULT = new GraphicContext(8,4.5f,10,1000, PERSPECTIVE_TYPE_FRUSTUM, 100);

	
	/**
	 * Empty constructor
	 */
	public GraphicContext() {
		// To be used when creating manually GraphicContext by using setter/getters
	}
	
	/**
	 * Duplicate GraphicContext(e.g. to start from default and update it)
	 * @param c the GraphicContext to duplicate
	 */
	public GraphicContext(GraphicContext c) {
		// To be used when creating manually GraphicContext by using setter/getters
		this.perspective_type = c.perspective_type;
		this.width = c.width;
		this.height = c.height;
		this.depth = c.depth;
		this.dist = c.dist;
		
		this.pixelWidth = c.pixelWidth;
		this.pixelHeight = c.pixelHeight;
		this.pixelHalfWidth = c.pixelHalfWidth;
		this.pixelHalfHeight = c.pixelHalfHeight;
	
		
		this.left = -width/2;
		this.right = width/2;
		this.bottom = -height/2;
		this.top = height/2;
		this.near = dist;
		this.far = dist + depth;

		
		createPerspective(perspective_type, left , right, bottom, top, near, far);
	}
	
	public GraphicContext(float width, float height, float dist, float depth, int perspective, int ppu) {
		this.width = width;
		this.height = height;
		this.dist = dist;
		this.depth = depth;
		this.perspective_type = perspective;
		this.ppu = ppu;

		this.pixelWidth = (int)(width*ppu);
		this.pixelHeight = (int)(height*ppu);
		this.pixelHalfWidth = pixelWidth/2;
		this.pixelHalfHeight = pixelHeight/2;

		this.left = -width/2;
		this.right = width/2;
		this.bottom = -height/2;
		this.top = height/2;
		this.near = dist;
		this.far = dist + depth;
		
		createPerspective(perspective, left , right, bottom, top, near, far);
	}
	
	public GraphicContext(float top, float bottom, float right, float left, float far, float near, int perspective, int ppu) {
		
		this.left = left;
		this.right = right;
		this.bottom = bottom;
		this.top = top;
		this.near = near;
		this.far = far;

		this.width = right - left;
		this.height = top - bottom;
		this.dist = near;
		this.depth = far - near;
		this.perspective_type = perspective;
		this.ppu = ppu;
		
		createPerspective(perspective, left , right, bottom, top, near, far);
	}
	
	public String perspectiveString(int perspective) {
		switch (perspective) {
			case PERSPECTIVE_TYPE_FRUSTUM:
				return PERSPECTIVE_TYPE_FRUSTUM_STRING;
			case PERSPECTIVE_TYPE_ORTHOGRAPHIC:
				return PERSPECTIVE_TYPE_ORTHOGRAPHIC_STRING;
			default:
				return "Undefined perspective: "+perspective;
		}
	}
	
	protected void createPerspective(int perspective, float left, float right, float bottom, float top, float near, float far) {
		
		switch (perspective) {
		case PERSPECTIVE_TYPE_FRUSTUM:
			projection = new Frustum(left , right, bottom, top, near, far);
			break;
		case PERSPECTIVE_TYPE_ORTHOGRAPHIC:
			projection = new Orthographic(left , right, bottom, top, near, far);
			break;
		default:
			if (Tracer.error) Tracer.traceError(this.getClass(), "Undefined perspective: "+perspective);
		}
		
	}
	
	public Matrix4 getProjectionMatrix() {
		return projection;
	}
	
	public String toString() {
		return "GraphicContext:\n* Perpective type: "+perspectiveString(perspective_type)+"\n* Width: "+width+"\n* Height: "+height+"\n* Dist: "+dist+"\n* Depth: "+depth+"\n* PPU: "+ppu+"\n* Pixel width: "+this.getPixelWidth()+"\n* Pixel height: "+this.getPixelHeight();
	}
	
	public void setWidth(float width) {
		this.width = width;
	}
	
	public float getWidth() {
		return width;
	}
	
	public void setHeight(float height) {
		this.height = height;
	}

	public float getHeight() {
		return height;
	}

	public int getPPU() {
		return ppu;
	}
	
	public int getPixelWidth() {
		return pixelWidth;
	}
	
	public int getPixelHeight() {
		return pixelHeight;
	}
	

	public int getPixelHalfWidth() {
		return pixelHalfWidth;
	}
	
	public int getPixelHalfHeight() {
		return pixelHalfHeight;
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
	
	public void setPerspective(int perspective) {
		this.perspective_type = perspective;
		
		float left = -width/2;
		float right = width/2;
		float bottom = -height/2;
		float top = height/2;
		float near = dist;
		float far = dist + depth;
		
		createPerspective(perspective, left , right, bottom, top, near, far);

	}
	
	public void computePerspective() {
		
		float left = -width/2;
		float right = width/2;
		float bottom = -height/2;
		float top = height/2;
		float near = dist;
		float far = dist + depth;
		
		createPerspective(perspective_type, left , right, bottom, top, near, far);
		
	}
	
	public int getPerspectiveType() {
		return perspective_type;
	}
	
	public Perspective getPerspective() {
		return (Perspective)projection;
	}
		
	public void setPpu(int ppu) {
		this.ppu = ppu;
	}
	
	public int getPpu() {
		return ppu;
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

}
