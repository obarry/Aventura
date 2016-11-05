package com.aventura.context;

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
 * 
 * The RenderContext describes the information and parameters to be used by the RenderEngine to render the World properly
 * This is all elements not related to the World, the Lighting or the Camera.
 * It can be to force the rendering to be plain or lines, to use or not textures, etc...
 * This can also allow future evolutions like selecting using a HW graphic acceleration
 * 
 * The RenderContext is passed as a parameter of the RenderEngine before asking him to render the World 
 * 
 * The application may create Several RenderContex instances to render differently a same World.
 * Several default contexts should be accessible as constants to perform standard rendering.
 * 
 * The RenderContext should remain as independent as possible on the display and windowing technology (e.g. Swing or SWT).
 * Another class (GraphicContext) should handle these specifics.
 * 
 * @author Bricolage Olivier
 * @since May 2016
 *
 */
public class RenderContext {
	
	public static final int RENDERING_TYPE_LINE = 1;		// Draw only lines
	public static final int RENDERING_TYPE_MONOCHROME = 2;  // Draw lines and fill with monochrome color each triangle
	public static final int RENDERING_TYPE_PLAIN = 3;		// Fill each triangle with one color depending on Lighting and orientation
	public static final int RENDERING_TYPE_INTERPOLATE = 4; // Fill each triangle by interpolating each pixel's color
	
	public static final int DISPLAY_LANDMARK_DISABLED = 0;
	public static final int DISPLAY_LANDMARK_ENABLED = 1;
	public static final int DISPLAY_LANDMARK_ENABLED_ARROW = 2;
	public static final int DISPLAY_LANDMARK_ENABLED_3D = 3;

	public int display_landmark = DISPLAY_LANDMARK_DISABLED; // by default

	public int rendering_type = 0;
	
	
	public static RenderContext RENDER_DEFAULT = new RenderContext(RENDERING_TYPE_LINE, DISPLAY_LANDMARK_ENABLED);
	
	/**
	 * Empty constructor
	 */
	public RenderContext() {
		// To be used when creating manually GraphicContext by using setter/getters
	}
	
	public RenderContext(int type, int display_landmark) {
		this.rendering_type = type;
		this.display_landmark = display_landmark;
	}
		
	public void setRendering(int type) {
		this.rendering_type = type;
	}
	
	public int getRendering() {
		return rendering_type;
	}

	public void setDisplayLandmark(int landmark) {
		this.display_landmark = landmark;
	}
	
	public int getDisplayLandmark() {
		return display_landmark;
	}


}
