package com.aventura.context;

import java.awt.Color;

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
 * 
 * The RenderContext describes the information and parameters to be used by the RenderEngine to render the World properly
 * This is all parameters not directly related to the World, the Lighting or the Camera nor the Display that is defined in PerspectiveContext
 * It can be to force the rendering to be plain or lines, to use or not textures, etc...
 * 
 * The RenderContext is passed as a parameter of the RenderEngine before asking him to render the World 
 * The application may create Several RenderContex instances to render differently a same World
 * 
 * Several pre-built-in default contexts are accessible as constants to perform standard rendering.
 * 
 * Future Evolution :
 * - The RenderContext should remain as independent as possible on the display and windowing technology (e.g. Swing or SWT). Another
 * class (PerspectiveContext) should handle these specifics.
 * - The RenderContext could also be used to define a parameter to trigger between using or not HW graphic acceleration
 * 
 * @author Olivier BARRY
 * @since May 2016
 *
 */
public class RenderContext {
	
	public static final int RENDERING_TYPE_LINE = 1;				// Draw only lines
	public static final int RENDERING_TYPE_MONOCHROME = 2;  		// Draw lines and fill with monochrome color each triangle
	public static final int RENDERING_TYPE_PLAIN = 3;				// Fill each triangle with one color depending on Lighting and orientation
	public static final int RENDERING_TYPE_INTERPOLATE = 4; 		// Fill each triangle by interpolating each pixel's color

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
	
	public static final int SHADOWING_DISABLED = 0;
	public static final int SHADOWING_ENABLED = 1;
	
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
	public int shadowing = SHADOWING_DISABLED; // Default
	
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
	public static RenderContext RENDER_STANDARD_PLAIN_SHADOWS = new RenderContext(RenderContext.RENDERING_TYPE_PLAIN, RenderContext.DISPLAY_LANDMARK_DISABLED, RenderContext.SHADOWING_ENABLED);
	public static RenderContext RENDER_STANDARD_PLAIN_WITH_LANDMARKS = new RenderContext(RenderContext.RENDERING_TYPE_PLAIN, RenderContext.DISPLAY_LANDMARK_ENABLED);
	public static RenderContext RENDER_STANDARD_INTERPOLATE = new RenderContext(RENDERING_TYPE_INTERPOLATE, DISPLAY_LANDMARK_DISABLED);
	public static RenderContext RENDER_STANDARD_INTERPOLATE_SHADOWS = new RenderContext(RENDERING_TYPE_INTERPOLATE, DISPLAY_LANDMARK_DISABLED, RenderContext.SHADOWING_ENABLED);
	public static RenderContext RENDER_STANDARD_INTERPOLATE_WITH_LANDMARKS = new RenderContext(RENDERING_TYPE_INTERPOLATE, DISPLAY_LANDMARK_ENABLED);
	public static RenderContext RENDER_DEFAULT = new RenderContext(RENDERING_TYPE_LINE, DISPLAY_LANDMARK_ENABLED);
	public static RenderContext RENDER_DEFAULT_ALL_ENABLED = new RenderContext(RENDERING_TYPE_LINE, DISPLAY_LANDMARK_ENABLED, DISPLAY_NORMALS_ENABLED, DISPLAY_LIGHT_VECTORS_ENABLED);
	
	/**
	 * Empty constructor
	 */
	public RenderContext() {
		// To be used when creating manually PerspectiveContext by using setter/getters
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
		
	public RenderContext(int type, int display_landmark, int shadowing) {
		this.renderingType = type;
		this.displayLandmark = display_landmark;
		this.shadowing = shadowing;
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
		
	public void setRenderingType(int type) {
		this.renderingType = type;
	}
	
	public int getRenderingType() {
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

	public void setShadowing(int tp) {
		this.shadowing = tp;
	}
	
	public int getShadowing() {
		return this.shadowing;
	}
	
	public String toString() {
		String renderContext = "Render Context:\n";
		
		renderContext += "* Rendering type: ";
		switch (getRenderingType()) {
		case RENDERING_TYPE_LINE:
			renderContext += "LINE";
			break;
		case RENDERING_TYPE_MONOCHROME:
			renderContext += "MONOCHROME";
			break;
		case RENDERING_TYPE_PLAIN:
			renderContext += "PLAIN";
			break;
		case RENDERING_TYPE_INTERPOLATE:
			renderContext += "INTERPOLATE";
			break;
		}
		renderContext += "\n";
		
		renderContext += "* Rendering lines: ";
		switch (getRenderingLines()) {
		case RENDERING_LINES_DISABLED:
			renderContext += "DISABLED";
			break;
		case RENDERING_LINES_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";
		
		renderContext += "* Display landmark: ";
		switch (getDisplayLandmark()) {
		case DISPLAY_LANDMARK_DISABLED:
			renderContext += "DISABLED";
			break;
		case DISPLAY_LANDMARK_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";

		renderContext += "* Display normals: ";
		switch (getDisplayNormals()) {
		case DISPLAY_NORMALS_DISABLED:
			renderContext += "DISABLED";
			break;
		case DISPLAY_NORMALS_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";

		renderContext += "* Display light vectors: ";
		switch (getDisplayLight()) {
		case DISPLAY_LIGHT_VECTORS_DISABLED:
			renderContext += "DISABLED";
			break;
		case DISPLAY_LIGHT_VECTORS_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";

		renderContext += "* Backface culling: ";
		switch (getBackFaceCulling()) {
		case BACKFACE_CULLING_DISABLED:
			renderContext += "DISABLED";
			break;
		case BACKFACE_CULLING_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";

		renderContext += "* Texture processing: ";
		switch (getTextureProcessing()) {
		case TEXTURE_PROCESSING_DISABLED:
			renderContext += "DISABLED";
			break;
		case TEXTURE_PROCESSING_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";

		renderContext += "* Shadowing: ";
		switch (getShadowing()) {
		case SHADOWING_DISABLED:
			renderContext += "DISABLED";
			break;
		case SHADOWING_ENABLED:
			renderContext += "ENABLED";
			break;
		}
		renderContext += "\n";
		
		return renderContext;
	}

}
