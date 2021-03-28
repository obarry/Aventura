package com.aventura.context;

import java.awt.Color;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2021 Olivier BARRY
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
 * @author Olivier BARRY
 * @since May 2016
 *
 */
public class RenderContext {
	
	public static final int RENDERING_TYPE_LINE = 1;		// Draw only lines
	public static final int RENDERING_TYPE_MONOCHROME = 2;  // Draw lines and fill with monochrome color each triangle
	public static final int RENDERING_TYPE_PLAIN = 3;		// Fill each triangle with one color depending on Lighting and orientation
	public static final int RENDERING_TYPE_INTERPOLATE = 4; // Fill each triangle by interpolating each pixel's color

	public static final int RENDERING_LINES_DISABLED = 0;
	public static final int RENDERING_LINES_ENABLED = 1;

	public static final int DISPLAY_LANDMARK_DISABLED = 0;
	public static final int DISPLAY_LANDMARK_ENABLED = 1;
	public static final int DISPLAY_LANDMARK_ENABLED_ARROW = 2;
	public static final int DISPLAY_LANDMARK_ENABLED_3D = 3;
	
	public static final int DISPLAY_NORMALS_DISABLED = 0;
	public static final int DISPLAY_NORMALS_ENABLED = 1;
	
	public static final int DISPLAY_LIGHT_VECTORS_DISABLED = 0;
	public static final int DISPLAY_LIGHT_VECTORS_ENABLED = 1;
	
	public static final int BACKFACE_CULLING_DISABLED = 0;
	public static final int BACKFACE_CULLING_ENABLED = 1;
	
	public static final int TEXTURE_PROCESSING_DISABLED = 0;
	public static final int TEXTURE_PROCESSING_ENABLED = 1;
	
	public static final int SHADING_DISABLED = 0;
	public static final int SHADING_ENABLED = 1;
	
	// ------------------------
	// RenderContext Attributes
	// ------------------------
	
	// Display elements in the scene
	public int displayLandmark = DISPLAY_LANDMARK_DISABLED; // by default
	public int displayNormals = DISPLAY_NORMALS_DISABLED;  // by default
	public int displayLight = DISPLAY_LIGHT_VECTORS_DISABLED; // by default

	// Rendering
	public int renderingType = 0;
	public int renderingLines = RENDERING_LINES_DISABLED; // To show lines even with other types of Rendering. Disabled by default
	
	// Backface Culling
	public int backfaceCulling = BACKFACE_CULLING_ENABLED; // Default
	
	// Texture processing
	public int textureProcessing = TEXTURE_PROCESSING_DISABLED; // Default
	
	// Shading
	public int shading = SHADING_DISABLED; // Default
	
	// --------------
	// Default colors
	// --------------
	
	public Color landmarkXColor = Color.RED;
	public Color landmarkYColor = Color.GREEN;
	public Color landmarkZColor = Color.BLUE;
	public Color normalsColor = Color.WHITE;
	public Color lightVectorsColor = Color.YELLOW;

	
	// Default RenderContext to be used for easy display
	public static RenderContext RENDER_STANDARD_PLAIN = new RenderContext(RenderContext.RENDERING_TYPE_PLAIN, RenderContext.DISPLAY_LANDMARK_DISABLED);
	public static RenderContext RENDER_STANDARD_PLAIN_WITH_LANDMARKS = new RenderContext(RenderContext.RENDERING_TYPE_PLAIN, RenderContext.DISPLAY_LANDMARK_ENABLED);
	public static RenderContext RENDER_STANDARD_INTERPOLATE = new RenderContext(RENDERING_TYPE_INTERPOLATE, DISPLAY_LANDMARK_DISABLED);
	public static RenderContext RENDER_STANDARD_INTERPOLATE_WITH_LANDMARKS = new RenderContext(RENDERING_TYPE_INTERPOLATE, DISPLAY_LANDMARK_ENABLED);
	public static RenderContext RENDER_DEFAULT = new RenderContext(RENDERING_TYPE_LINE, DISPLAY_LANDMARK_ENABLED);
	public static RenderContext RENDER_DEFAULT_ALL_ENABLED = new RenderContext(RENDERING_TYPE_LINE, DISPLAY_LANDMARK_ENABLED, DISPLAY_NORMALS_ENABLED, DISPLAY_LIGHT_VECTORS_ENABLED);
	
	/**
	 * Empty constructor
	 */
	public RenderContext() {
		// To be used when creating manually GraphicContext by using setter/getters
	}
	
	/**
	 * To duplicate a standard RenderContext before customizing it
	 */
	public RenderContext(RenderContext r) {
		this.displayLandmark = r.displayLandmark;
		this.displayNormals = r.displayNormals;
		this.displayLight = r.displayLight;
		this.renderingType = r.renderingType;
		this.backfaceCulling = r.backfaceCulling;
		this.textureProcessing = r.textureProcessing;
	}
	
	public RenderContext(int type, int display_landmark) {
		this.renderingType = type;
		this.displayLandmark = display_landmark;
	}
		
	public RenderContext(int type, int display_landmark, int display_normals, int display_light) {
		this.renderingType = type;
		this.displayLandmark = display_landmark;
		this.displayNormals = display_normals;
		this.displayLight = display_light;
	}
		
	public RenderContext(int type, int display_landmark, int display_normals, int display_light, int backfaceCulling) {
		this.renderingType = type;
		this.displayLandmark = display_landmark;
		this.displayNormals = display_normals;
		this.displayLight = display_light;
		this.backfaceCulling = backfaceCulling;
	}
		
	public RenderContext(int type, int display_landmark, int display_normals, int display_light, int backfaceCulling, int textureProcessing) {
		this.renderingType = type;
		this.displayLandmark = display_landmark;
		this.displayNormals = display_normals;
		this.displayLight = display_light;
		this.backfaceCulling = backfaceCulling;
		this.textureProcessing = textureProcessing;
	}
		
	public void setRendering(int type) {
		this.renderingType = type;
	}
	
	public int getRendering() {
		return this.renderingType;
	}
	
	public int getRenderingLines() {
		return renderingLines;
	}

	public void setRenderingLines(int renderingLines) {
		this.renderingLines = renderingLines;
	}

	public void setDisplayLandmark(int landmark) {
		this.displayLandmark = landmark;
	}
	
	public int getDisplayLandmark() {
		return this.displayLandmark;
	}

	public void setDisplayNormals(int display_normals) {
		this.displayNormals = display_normals;
	}
	
	public int getDisplayNormals() {
		return this.displayNormals;
	}

	public void setDisplayLight(int display_light) {
		this.displayLight = display_light;
	}
	
	public int getDisplayLight() {
		return this.displayLight;
	}
	
	public void setBackFaceCulling(int bfc) {
		this.backfaceCulling = bfc;
	}
	
	public int getBackFaceCulling() {
		return this.backfaceCulling;
	}

	public void setTextureProcessing(int tp) {
		this.textureProcessing = tp;
	}
	
	public int getTextureProcessing() {
		return this.textureProcessing;
	}

	public void setShading(int tp) {
		this.shading = tp;
	}
	
	public int getShading() {
		return this.shading;
	}

}
