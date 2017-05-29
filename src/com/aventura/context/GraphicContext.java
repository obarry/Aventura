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
	double width = 0;
	double height = 0;
	double depth = 0;
	double dist = 0;
	
	// Other characteristics
	double left = 0;
	double right = 0;
	double bottom = 0;
	double top = 0;
	double near = 0;
	double far = 0;

	// Pixel Per Unit
	int ppu = 0;
	int pixelWidth = 0; // Number of pixels on the X axis
	int pixelHeight = 0; // Number of pixels on the Y axis
	int pixelHalfWidth = 0;
	int pixelHalfHeight = 0;
	
	// Projection Matrix
	Matrix4 projection;

	// Width/Height ratio = 16/9
	public static GraphicContext GRAPHIC_DEFAULT = new GraphicContext(8,4.5,10,1000, PERSPECTIVE_TYPE_FRUSTUM, 100);

	
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
	
	public GraphicContext(double width, double height, double dist, double depth, int perspective, int ppu) {
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
	
	public GraphicContext(double top, double bottom, double right, double left, double far, double near, int perspective, int ppu) {
		
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
	
	protected void createPerspective(int perspective, double left, double right, double bottom, double top, double near, double far) {
		
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
	
	public void setWidth(double width) {
		this.width = width;
	}
	
	public double getWidth() {
		return width;
	}
	
	public void setHeight(double height) {
		this.height = height;
	}

	public double getHeight() {
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

	public void setDepth(double depth) {
		this.depth = depth;
	}
	
	public double getDepth() {
		return depth;
	}

	public void setDist(double dist) {
		this.dist = dist;
	}
	
	public double getDist() {
		return dist;
	}
	
	public void setPerspective(int perspective) {
		this.perspective_type = perspective;
		
		double left = -width/2;
		double right = width/2;
		double bottom = -height/2;
		double top = height/2;
		double near = dist;
		double far = dist + depth;
		
		createPerspective(perspective, left , right, bottom, top, near, far);

	}
	
	public void computePerspective() {
		
		double left = -width/2;
		double right = width/2;
		double bottom = -height/2;
		double top = height/2;
		double near = dist;
		double far = dist + depth;
		
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
	
	
	public double getLeft() {
		return left;
	}

	public void setLeft(double left) {
		this.left = left;
	}

	public double getRight() {
		return right;
	}

	public void setRight(double right) {
		this.right = right;
	}

	public double getBottom() {
		return bottom;
	}

	public void setBottom(double bottom) {
		this.bottom = bottom;
	}

	public double getTop() {
		return top;
	}

	public void setTop(double top) {
		this.top = top;
	}

	public double getNear() {
		return near;
	}

	public void setNear(double near) {
		this.near = near;
	}

	public double getFar() {
		return far;
	}

	public void setFar(double far) {
		this.far = far;
	}

}
