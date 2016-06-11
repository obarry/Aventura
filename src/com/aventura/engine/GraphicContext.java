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


	// Window & frustum
	double width;
	double height;
	double depth;
	double dist;
	
	int perspective_type = 0;
	
	public GraphicContext() {
		
	}
	
	public GraphicContext(double width, double height, double dist, double depth, int type) {
		this.width = width;
		this.height = height;
		this.dist = dist;
		this.depth = depth;
		this.perspective_type = type;
	}
	
	public static GraphicContext GRAPHIC_DEFAULT = new GraphicContext(800,450,400,10000, PERSPECTIVE_TYPE_FRUSTUM);
	

}
