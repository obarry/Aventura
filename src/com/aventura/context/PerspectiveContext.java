package com.aventura.context;

import com.aventura.model.perspective.FrustumPerspective;
import com.aventura.model.perspective.OrthographicPerspective;
import com.aventura.model.perspective.Perspective;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
 * Evolutions :
 * ----------
 * 6-Oct-2023 : Proposal to rename GraphicContext into GeometryContext - DONE 27-Jan-2025 renamed into PerspectiveContext (more appropriate)
 * 15-Jun-2024 : Evolution by delegating all the Perspective management to a new Perspective class (and subclasses) in new package :
 * com.aventura.model.perspective. It should allow to bring new services in this class to calculate the Frustum related informations
 * required for example by the ShadowingLight class and related to identify the area where to cast shadows.
 * As a consequence, the width, height, dist, depth, top, bottom, left, right, far, near information are now stored in Perspetive. 
 * -------------------------------------------------------------------
 * 
 * The PerspectiveContext is a parameter class containing all information allowing to display the world
 * This is where the gUIView frustum planes are defined and also where the rasterizing definition (pixel per unit)
 * is also set.
 * At last this is where the projection Matrix is built and stored using the perspective parameters of the PerspectiveContext.
 * The resulting projection Matrix can be obtained using the corresponding getter.
 * 
 * The PerspectiveContext is passed as a parameter of the RenderEngine before asking him to render the World
 * As a "parameter" object, the application using Aventura API can prepare several PerspectiveContext and switch from one to another
 * 
 * Frustum definition:
 * ------------------
 * 
 *     X (or Y)
 *        ^                       +
 *        |     GUIView       -   |
 *        |     Plane     -       |
 *        | (top)     -           |
 *        | right +               |   ^
 *        |   -   |   GUIView     |   |  width
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
 * The gUIView is defined by:
 *    width  = right - left
 *    height = top - bottom
 *    depth  = far - near
 *    dist   = near - 0
 *  
 * Assuming a symetric gUIView (bottom = -top and left = -right) centered on the origin 
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
public class PerspectiveContext {
	
	public static final int PERSPECTIVE_TYPE_FRUSTUM = 1;
	public static final int PERSPECTIVE_TYPE_ORTHOGRAPHIC = 2;
	
	public static final String PERSPECTIVE_TYPE_FRUSTUM_STRING = "PERSPECTIVE_TYPE_FRUSTUM";
	public static final String PERSPECTIVE_TYPE_ORTHOGRAPHIC_STRING = "PERSPECTIVE_TYPE_ORTHOGRAPHIC";
		
	// Projection type
	int p_type = 0; // uninitialized


	// ViewPort related attributes (pixel related)
	// Pixel Per Unit
	int ppu = 0;
	int pixelWidth = 0; // Number of pixels on the X axis
	int pixelHeight = 0; // Number of pixels on the Y axis
	int pixelHalfWidth = 0;
	int pixelHalfHeight = 0;
	
	// Perspective
	Perspective perspective; // link to the perspective that this PerspectiveContext is defining

	// A Default context that can then be modified using accessors
	public static PerspectiveContext PERSPECTIVE_DEFAULT = new PerspectiveContext(8,4.5f,10,1000, PERSPECTIVE_TYPE_FRUSTUM, 100); // Width/Height ratio = 16/9

	
	/**
	 * Empty constructor
	 */
	public PerspectiveContext() {
		// To be used when creating manually PerspectiveContext by using setter/getters
	}
	
	/**
	 * Duplicate PerspectiveContext(e.g. to start from default and update it)
	 * @param c the PerspectiveContext to duplicate
	 */
	public PerspectiveContext(PerspectiveContext c) {
		// To be used when creating manually PerspectiveContext by using setter/getters
		this.p_type = c.p_type;
					
		this.pixelWidth = c.pixelWidth;
		this.pixelHeight = c.pixelHeight;
		this.pixelHalfWidth = c.pixelHalfWidth;
		this.pixelHalfHeight = c.pixelHalfHeight;

		// Generate the perspective using the parameters of the other PerspectiveContext
		switch (p_type) {
		case PERSPECTIVE_TYPE_FRUSTUM:
			this.perspective = new FrustumPerspective(c.perspective);
			break;
		case PERSPECTIVE_TYPE_ORTHOGRAPHIC:
			this.perspective = new OrthographicPerspective(c.perspective);
			break;
		default:
			if (Tracer.error) Tracer.traceError(this.getClass(), "Undefined perspective: "+p_type);
		}
	}
	
	/**
	 * @param width
	 * @param height
	 * @param dist
	 * @param depth
	 * @param perspective
	 * @param ppu
	 */
	public PerspectiveContext(float width, float height, float dist, float depth, int perspective, int ppu) {

		this.p_type = perspective;
		this.ppu = ppu;

		this.pixelWidth = (int)(width*ppu);
		this.pixelHeight = (int)(height*ppu);
		this.pixelHalfWidth = pixelWidth/2;
		this.pixelHalfHeight = pixelHeight/2;
		
		createPerspective(perspective, width , height, dist, depth);
	}
	
	public PerspectiveContext(float top, float bottom, float right, float left, float far, float near, int perspective, int ppu) {
		
		this.pixelWidth = (int)((right-left)*ppu);
		this.pixelHeight = (int)((top-bottom)*ppu);
		this.pixelHalfWidth = pixelWidth/2;
		this.pixelHalfHeight = pixelHeight/2;
		
		
		this.p_type = perspective;
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
	
	protected void createPerspective(int p_type, float width, float height, float dist, float depth) {
		
		switch (p_type) {
		case PERSPECTIVE_TYPE_FRUSTUM:
			this.perspective = new FrustumPerspective(width , height, dist, depth);
			break;
		case PERSPECTIVE_TYPE_ORTHOGRAPHIC:
			this.perspective = new OrthographicPerspective(width , height, dist, depth);
			break;
		default:
			if (Tracer.error) Tracer.traceError(this.getClass(), "Undefined perspective: "+p_type);
		}
		
	}
	
	protected void createPerspective(int p_type, float left, float right, float bottom, float top, float near, float far) {
		
		switch (p_type) {
		case PERSPECTIVE_TYPE_FRUSTUM:
			this.perspective = new FrustumPerspective(left , right, bottom, top, near, far);
			break;
		case PERSPECTIVE_TYPE_ORTHOGRAPHIC:
			this.perspective = new OrthographicPerspective(left , right, bottom, top, near, far);
			break;
		default:
			if (Tracer.error) Tracer.traceError(this.getClass(), "Undefined perspective: "+p_type);
		}
		
	}
		
	public String toString() {
		return "PerspectiveContext:\n* Perpective type: "+perspectiveString(p_type)+"\n* Width: "+perspective.getWidth()+"\n* Height: "+perspective.getHeight()+"\n* Dist: "+perspective.getDist()+"\n* Depth: "+perspective.getDepth()+"\n* PPU: "+ppu+"\n* Pixel width: "+pixelWidth+"\n* Pixel height: "+pixelHeight;
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
	
	public int getPerspectiveType() {
		return p_type;
	}
	
	public Perspective getPerspective() {
		return perspective;
	}
		
	public void setPPU(int ppu) {
		this.ppu = ppu;
	}
	
	public int getPPU() {
		return ppu;
	}
	
}
