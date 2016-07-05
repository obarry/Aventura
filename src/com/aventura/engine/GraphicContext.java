package com.aventura.engine;

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
 * 
 * @author Bricolage Olivier
 * @since May 2016
 *
 */
public class GraphicContext {
	
	public static final int PERSPECTIVE_TYPE_FRUSTUM = 1;
	public static final int PERSPECTIVE_TYPE_ORTHOGRAPHIC = 2;
	
	public static final String PERSPECTIVE_TYPE_FRUSTUM_STRING = "PERSPECTIVE_TYPE_FRUSTUM";
	public static final String PERSPECTIVE_TYPE_ORTHOGRAPHIC_STRING = "PERSPECTIVE_TYPE_ORTHOGRAPHIC";
	
	public static final int DISPLAY_LANDMARK_DISABLED = 0;
	public static final int DISPLAY_LANDMARK_ENABLED_STANDARD = 1;
	public static final int DISPLAY_LANDMARK_ENABLED_ARROW = 2;
	public static final int DISPLAY_LANDMARK_ENABLED_3D = 3;

	
	// Perspective type
	int perspective_type = 0; // uninitialized
	int display_landmark = DISPLAY_LANDMARK_DISABLED; // by default

	// Window & frustum
	double width = 0;
	double height = 0;
	double depth = 0;
	double dist = 0;
	

	public static GraphicContext GRAPHIC_DEFAULT = new GraphicContext(800,450,400,10000, PERSPECTIVE_TYPE_FRUSTUM);

	
	/**
	 * Empty constructor
	 */
	public GraphicContext() {
		// To be used when creating manually GraphicContext by using setter/getters
	}
	
	public GraphicContext(double width, double height, double dist, double depth, int perspective) {
		this.width = width;
		this.height = height;
		this.dist = dist;
		this.depth = depth;
		this.perspective_type = perspective;

	}
	
	public GraphicContext(double top, double bottom, double right, double left, double far, double near, int perspective) {
		this.width = right - left;
		this.height = top - bottom;
		this.dist = near;
		this.depth = far - near;
		this.perspective_type = perspective;

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
	
	public String toString() {
		return "GraphicContext:\n* Perpective type: "+perspectiveString(perspective_type)+"\n* Width: "+width+"\n* Height: "+height+"\n* Dist: "+dist+"\n* Depth: "+depth;
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
	}
	
	public int getPerspective() {
		return perspective_type;
	}
	
	public void setDisplayLandmark(int landmark) {
		this.display_landmark = landmark;
	}
	
	public int getDisplayLandmark() {
		return display_landmark;
	}

}
