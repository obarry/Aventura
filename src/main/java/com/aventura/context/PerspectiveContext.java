package com.aventura.context;

import com.aventura.model.perspective.FrustumPerspective;
import com.aventura.model.perspective.OrthographicPerspective;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.perspective.PerspectiveType;
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
 * Evolutions :
 * ----------
 * 6-Oct-2023 : Proposal to rename GraphicContext into GeometryContext - DONE 27-Jan-2025 renamed into PerspectiveContext (more appropriate)
 * 15-Jun-2024 : Evolution by delegating all the Perspective management to a new Perspective class (and subclasses) in new package :
 * com.aventura.model.perspective. It should allow to bring new services in this class to calculate the Frustum related informations
 * required for example by the ShadowingLight class and related to identify the area where to cast shadows.
 * As a consequence, the width, height, dist, depth, top, bottom, left, right, far, near information are now stored in Perspective. 
 * Sep-2026 : perspective type is now the PerspectiveType enum (former PERSPECTIVE_TYPE_* int constants), owned by the Perspective.
 * Planned : split into a Perspective (lens, world units) and a Viewport (pixels) -- separate evolution.
 * -------------------------------------------------------------------
 * 
 * The PerspectiveContext is a parameter class containing all information allowing to display the world:
 * - the Perspective (view volume and projection matrix, in world units, see Perspective for the frustum definition)
 * - the raster definition: pixel width and height of the image, and PPU (Pixel Per Unit) used to derive
 *   one from the other.
 * 
 * The PerspectiveContext is passed as a parameter of the RenderEngine before asking him to render the World.
 * As a "parameter" object, the application using Aventura API can prepare several PerspectiveContext and switch from one to another
 * (one RenderEngine per PerspectiveContext).
 * 
 * PPU - Pixel Per Unit
 * The Perspective dimensions (width, height, depth, dist) are given in (camera) coordinates (floating point)
 * in a given Unit (can be meter or millimeter or whatever unit).
 * The size of the screen is thus defined in this unit.
 * To define the number of pixel, a ratio should be provided: the number of pixel per unit: PPU
 * 
 * Pixel dimensions are FIXED at construction (the RenderEngine allocates its ZBuffer, and the View its image,
 * from them). The Perspective can still be modified afterwards through getPerspective() (e.g. setWidth() to zoom):
 * the image keeps its pixel size and the new view volume is mapped onto it (the PPU value then only reflects
 * the construction-time ratio).
 * 
 * @author Olivier BARRY
 * @since May 2016
 *
 */
public class PerspectiveContext {
	
	// Default perspective (see empty constructor): 8 x 4.5 units (16/9), near at 10, depth 1000, 100 pixels per unit
	public static final float DEFAULT_WIDTH = 8f;
	public static final float DEFAULT_HEIGHT = 4.5f;
	public static final float DEFAULT_DIST = 10f;
	public static final float DEFAULT_DEPTH = 1000f;
	public static final int DEFAULT_PPU = 100;

	// ViewPort related attributes (pixel related)
	// Pixel Per Unit
	int ppu = 0;
	int pixelWidth = 0; // Number of pixels on the X axis
	int pixelHeight = 0; // Number of pixels on the Y axis
	int pixelHalfWidth = 0;
	int pixelHalfHeight = 0;
	
	// Perspective
	Perspective perspective; // link to the perspective that this PerspectiveContext is defining

	
	/**
	 * Empty constructor -> Default Perspective: FRUSTUM, 8 x 4.5 units (16/9), near at 10, depth 1000, 100 PPU,
	 * i.e. an 800 x 450 pixels image.
	 */
	public PerspectiveContext() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_DIST, DEFAULT_DEPTH, PerspectiveType.FRUSTUM, DEFAULT_PPU);
	}
		
	/**
	 * Duplicate PerspectiveContext (e.g. to start from another one and update it). The Perspective is deep copied.
	 * @param c the PerspectiveContext to duplicate
	 */
	public PerspectiveContext(PerspectiveContext c) {
		this.ppu = c.ppu;
		this.pixelWidth = c.pixelWidth;
		this.pixelHeight = c.pixelHeight;
		this.pixelHalfWidth = c.pixelHalfWidth;
		this.pixelHalfHeight = c.pixelHalfHeight;
		this.perspective = c.perspective.copy();
	}
	
	/**
	 * Pixel-based constructor: the world-unit window size is derived as pixel size / ppu.
	 * Caution: with int literals for the first 2 arguments, this constructor is selected instead of the
	 * (float width, float height, ...) one -- e.g. (10, 10, ...) means 10 x 10 PIXELS here.
	 * 
	 * @param pixel_width number of pixel for the width of this perspective
	 * @param pixel_height number of pixel for the height of this perspective
	 * @param dist distance from the eye to the near plane
	 * @param depth distance from the near plane to the far plane
	 * @param type type of perspective (Orthographic or Frustum)
	 * @param ppu pixel per unit
	 */
	public PerspectiveContext(int pixel_width, int pixel_height, float dist, float depth, PerspectiveType type, int ppu) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "New perspectiveContext: pixel width: " + pixel_width + " pixel height:" + pixel_height + " dist: " + dist + " depth: " + depth +" ppu: " + ppu + " type: " + type);

		this.ppu = ppu;
		setPixelDimensions(pixel_width, pixel_height);
		
		// float division: an int division here used to truncate the window size (e.g. 1000/300 -> 3)
		createPerspective(type, (float)pixel_width/ppu , (float)pixel_height/ppu, dist, depth);
	}
	
	/**
	 * Constructor with an exact pixel width (the pixel height is derived from the width/height ratio).
	 * Used e.g. for shadow maps, where the pixel resolution is imposed regardless of the world-space extent.
	 * 
	 * @param pixel_width number of pixel for the width
	 * @param width width of the near plane window
	 * @param height height of the near plane window
	 * @param dist distance from the eye to the near plane
	 * @param depth distance from the near plane to the far plane
	 * @param type type of perspective (Orthographic or Frustum)
	 */
	public PerspectiveContext(int pixel_width, float width, float height, float dist, float depth, PerspectiveType type) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "New perspectiveContext: pixel width: "+pixel_width+" width: "+width+" height:"+height+" dist: "+dist+" depth: "+depth+" type: "+type);

		this.ppu = (int)(pixel_width/width);

		// BUGFIX: this used to be (int)(height*ppu) -- going through ppu (an int, already
		// truncated from pixel_width/width) compounds a second truncation on top of the first.
		// Even when height == width EXACTLY (as DirectionalLight.initShadowing() guarantees for
		// its square-footprint shadow box), the two independent truncations could disagree --
		// e.g. width=10.733126, pixel_width=500 gave ppu=(int)46.58=46, then
		// pixelHeight=(int)(10.733126*46)=493, while pixelWidth stayed the exact requested 500.
		// Fix: derive pixelHeight from pixelWidth and the exact (float) height/width ratio in a
		// single rounding step, instead of round-tripping through the separately-truncated ppu.
		// When height == width bit-for-bit, this reduces to exactly pixel_width, no exceptions.
		setPixelDimensions(pixel_width, Math.round(pixel_width * (height / width)));
		
		createPerspective(type, width , height, dist, depth);
	}

	/**
	 * World-unit constructor: the pixel size is derived as window size * ppu.
	 * @param width width of the near plane window
	 * @param height height of the near plane window
	 * @param dist distance from the eye to the near plane
	 * @param depth distance from the near plane to the far plane
	 * @param type type of perspective (Orthographic or Frustum)
	 * @param ppu pixel per unit
	 */
	public PerspectiveContext(float width, float height, float dist, float depth, PerspectiveType type, int ppu) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "New perspectiveContext: width: "+width+" height:"+height+" dist: "+dist+" depth: "+depth+" ppu: "+ppu+" type: "+type);

		this.ppu = ppu;
		setPixelDimensions((int)(width*ppu), (int)(height*ppu));
		
		createPerspective(type, width , height, dist, depth);
	}

	/**
	 * Six-bounds constructor (possibly asymmetric volume).
	 * Caution: the bounds are in (top, bottom, right, left, far, near) order here, unlike Perspective's
	 * subclasses and Projection classes which use (left, right, bottom, top, near, far). Kept as is for
	 * compatibility; to be revisited with the Perspective / Viewport split.
	 * 
	 * @param top top bound of the near plane
	 * @param bottom bottom bound of the near plane
	 * @param right right bound of the near plane
	 * @param left left bound of the near plane
	 * @param far distance to the far plane
	 * @param near distance to the near plane
	 * @param type type of perspective (Orthographic or Frustum)
	 * @param ppu pixel per unit
	 */
	public PerspectiveContext(float top, float bottom, float right, float left, float far, float near, PerspectiveType type, int ppu) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "New perspectiveContext: top: "+top+" bottom: "+bottom+" right: "+right+" left: "+left+" far: "+far+" near: "+near+" ppu: "+ppu+" type: "+type);
		
		this.ppu = ppu;
		setPixelDimensions((int)((right-left)*ppu), (int)((top-bottom)*ppu));
		
		createPerspective(type, left , right, bottom, top, near, far);
	}
	
	private void setPixelDimensions(int pixel_width, int pixel_height) {
		this.pixelWidth = pixel_width;
		this.pixelHeight = pixel_height;
		this.pixelHalfWidth = pixelWidth/2;
		this.pixelHalfHeight = pixelHeight/2;
	}
	
	private static void checkType(PerspectiveType type) {
		if (type == null) throw new IllegalArgumentException("PerspectiveContext: perspective type must not be null");
	}
	
	/**
	 * Create a Perspective with width, height, dist and depth
	 */
	protected void createPerspective(PerspectiveType type, float width, float height, float dist, float depth) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating perspective: width: "+width+" height: "+height+" dist: "+dist+" depth: "+depth);
		checkType(type);
		
		switch (type) {
		case FRUSTUM:
			this.perspective = new FrustumPerspective(width , height, dist, depth);
			break;
		case ORTHOGRAPHIC:
			this.perspective = new OrthographicPerspective(width , height, dist, depth);
			break;
		}
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Created perspective : \n" + this.perspective);	
	}
	
	/**
	 * Create a Perspective with left, right bottom, top, near far
	 */
	protected void createPerspective(PerspectiveType type, float left, float right, float bottom, float top, float near, float far) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating perspective: top: "+top+" bottom: "+bottom+" right: "+right+" left: "+left+" far: "+far+" near: "+near);
		checkType(type);
		
		switch (type) {
		case FRUSTUM:
			this.perspective = new FrustumPerspective(left , right, bottom, top, near, far);
			break;
		case ORTHOGRAPHIC:
			this.perspective = new OrthographicPerspective(left , right, bottom, top, near, far);
			break;
		}

		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Created perspective : \n" + this.perspective);
	}
		
	public String toString() {
		return "PerspectiveContext:\n* Perpective type: "+getPerspectiveType()+"\n* Width: "+perspective.getWidth()+"\n* Height: "+perspective.getHeight()+"\n* Dist: "+perspective.getDist()+"\n* Depth: "+perspective.getDepth()+"\n* PPU: "+ppu+"\n* Pixel width: "+pixelWidth+"\n* Pixel height: "+pixelHeight;
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
	
	/**
	 * @return the type of the Perspective (FRUSTUM or ORTHOGRAPHIC)
	 */
	public PerspectiveType getPerspectiveType() {
		return perspective.getType();
	}
	
	public Perspective getPerspective() {
		return perspective;
	}
	
	/**
	 * @return the Pixel Per Unit ratio used at construction (see class Javadoc)
	 */
	public int getPPU() {
		return ppu;
	}
	
}
