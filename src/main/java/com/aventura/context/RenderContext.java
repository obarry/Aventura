package com.aventura.context;

import java.awt.Color;

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
 * The RenderContext describes the information and parameters to be used by the RenderEngine to render the World properly.
 * This is all parameters not directly related to the World, the Lighting or the Camera nor the Display (that is defined
 * in PerspectiveContext). It can be to force the rendering to be plain or lines, to use or not textures, etc...
 * 
 * The RenderContext is passed as a parameter of the RenderEngine before asking him to render the World.
 * The application may create several RenderContext instances to render differently a same World. It is read by the
 * RenderEngine at each frame, so it can also be modified between two frames (e.g. toggling textures from the GUI).
 * 
 * Several pre-built-in default contexts are accessible as constants (RENDER_*) to perform standard rendering.
 * These presets are IMMUTABLE (any setter throws an IllegalStateException): to customize one, duplicate it first:
 * 
 *     RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE)
 *                                  .setTextureProcessing(true)
 *                                  .setShadowing(true);
 * 
 * All setters return this RenderContext, so they can be chained as above.
 * 
 * Evolutions :
 * ----------
 * Sep-2026 : int constants replaced by the RenderingType enum and booleans, private fields with accessors,
 * immutable presets, fluent setters, complete copy constructor. DISPLAY_LANDMARK_ENABLED_ARROW / _3D removed
 * (never implemented).
 * 
 * Future Evolution :
 * - The RenderContext should remain as independent as possible on the display and windowing technology (e.g. Swing or SWT).
 * - The RenderContext could also be used to define a parameter to trigger between using or not HW graphic acceleration
 * 
 * @author Olivier BARRY
 * @since May 2016
 *
 */
public class RenderContext {
	
	/**
	 * How the triangles are drawn.
	 */
	public enum RenderingType {
		/** Draw only lines (wireframe), no ZBuffer */
		LINE,
		/** Fill each triangle with its color, no shading -- NOT IMPLEMENTED YET (draws nothing) */
		MONOCHROME,
		/** Fill each triangle with shading, texture always processed when the triangle has one (legacy behavior) */
		PLAIN,
		/** Fill each triangle by interpolating each pixel's normal/color (Gouraud/Phong-like) */
		INTERPOLATE,
		/** Fill each triangle using a single normal for the whole face (faceted look), regardless of whether
		 * the triangle/mesh also has per-vertex normals -- unlike PLAIN, which only looks "flat" incidentally
		 * when the mesh happens to carry an explicit per-triangle normal (Triangle.isTriangleNormal()) */
		FLAT
	}
	
	// ------------------------
	// RenderContext Attributes
	// ------------------------
	
	// Rendering
	private RenderingType renderingType = RenderingType.INTERPOLATE;
	private boolean renderingLines = false; // To show lines even with other types of Rendering
	
	// Display elements in the scene
	private boolean displayLandmark = false;
	private boolean displayNormals = false;
	private boolean displayLight = false; // Light vectors
	
	// Backface Culling
	private boolean backfaceCulling = true;
	
	// Texture processing
	private boolean textureProcessing = false;
	
	// Shadowing
	private boolean shadowing = false;
	
	// Colors of the debug displays
	private Color landmarkXColor = Color.RED;
	private Color landmarkYColor = Color.GREEN;
	private Color landmarkZColor = Color.BLUE;
	private Color normalsColor = Color.WHITE;
	private Color lightVectorsColor = Color.YELLOW;
	
	// Immutable flag, set on the presets below
	private boolean frozen = false;

	
	// Default (immutable) RenderContexts to be used for easy display -- duplicate them to customize
	public static final RenderContext RENDER_STANDARD_PLAIN = new RenderContext(RenderingType.PLAIN).freeze();
	public static final RenderContext RENDER_STANDARD_PLAIN_SHADOWS = new RenderContext(RenderingType.PLAIN).setShadowing(true).freeze();
	public static final RenderContext RENDER_STANDARD_PLAIN_WITH_LANDMARKS = new RenderContext(RenderingType.PLAIN).setDisplayLandmark(true).freeze();
	public static final RenderContext RENDER_STANDARD_INTERPOLATE = new RenderContext(RenderingType.INTERPOLATE).freeze();
	public static final RenderContext RENDER_STANDARD_INTERPOLATE_SHADOWS = new RenderContext(RenderingType.INTERPOLATE).setShadowing(true).freeze();
	public static final RenderContext RENDER_STANDARD_INTERPOLATE_WITH_LANDMARKS = new RenderContext(RenderingType.INTERPOLATE).setDisplayLandmark(true).freeze();
	public static final RenderContext RENDER_DEFAULT = new RenderContext(RenderingType.LINE).setDisplayLandmark(true).freeze();
	public static final RenderContext RENDER_DEFAULT_ALL_ENABLED = new RenderContext(RenderingType.LINE).setDisplayLandmark(true).setDisplayNormals(true).setDisplayLight(true).freeze();
	
	/**
	 * Default RenderContext: INTERPOLATE rendering, backface culling enabled, everything else disabled.
	 */
	public RenderContext() {
	}
	
	/**
	 * Default RenderContext with the given rendering type.
	 */
	public RenderContext(RenderingType type) {
		setRenderingType(type);
	}
	
	/**
	 * To duplicate a RenderContext (typically a standard, immutable one) before customizing it.
	 * The copy is always mutable.
	 */
	public RenderContext(RenderContext r) {
		this.renderingType = r.renderingType;
		this.renderingLines = r.renderingLines;
		this.displayLandmark = r.displayLandmark;
		this.displayNormals = r.displayNormals;
		this.displayLight = r.displayLight;
		this.backfaceCulling = r.backfaceCulling;
		this.textureProcessing = r.textureProcessing;
		this.shadowing = r.shadowing;
		this.landmarkXColor = r.landmarkXColor;
		this.landmarkYColor = r.landmarkYColor;
		this.landmarkZColor = r.landmarkZColor;
		this.normalsColor = r.normalsColor;
		this.lightVectorsColor = r.lightVectorsColor;
	}
	
	/**
	 * Makes this RenderContext immutable: any further setter call throws an IllegalStateException.
	 * @return this RenderContext
	 */
	public RenderContext freeze() {
		this.frozen = true;
		return this;
	}
	
	/**
	 * @return true if this RenderContext is immutable (e.g. one of the RENDER_* presets)
	 */
	public boolean isFrozen() {
		return frozen;
	}
	
	private void checkNotFrozen() {
		if (frozen) throw new IllegalStateException("This RenderContext is immutable (e.g. a RENDER_* preset): duplicate it first with new RenderContext(preset)");
	}
	
	// -------------------
	// Accessors (fluent)
	// -------------------
		
	public RenderContext setRenderingType(RenderingType type) {
		checkNotFrozen();
		if (type == null) throw new IllegalArgumentException("RenderContext: rendering type must not be null");
		this.renderingType = type;
		return this;
	}
	
	public RenderingType getRenderingType() {
		return renderingType;
	}
	
	/** Superimpose the triangles' edges (lines) on the other rendering types */
	public RenderContext setRenderingLines(boolean renderingLines) {
		checkNotFrozen();
		this.renderingLines = renderingLines;
		return this;
	}

	public boolean isRenderingLines() {
		return renderingLines;
	}

	/** Display the X, Y, Z axis of the World */
	public RenderContext setDisplayLandmark(boolean displayLandmark) {
		checkNotFrozen();
		this.displayLandmark = displayLandmark;
		return this;
	}
	
	public boolean isDisplayLandmark() {
		return displayLandmark;
	}

	/** Display the normal vectors of the triangles (or vertices) */
	public RenderContext setDisplayNormals(boolean displayNormals) {
		checkNotFrozen();
		this.displayNormals = displayNormals;
		return this;
	}
	
	public boolean isDisplayNormals() {
		return displayNormals;
	}

	/** Display the light vectors of the directional lights */
	public RenderContext setDisplayLight(boolean displayLight) {
		checkNotFrozen();
		this.displayLight = displayLight;
		return this;
	}
	
	public boolean isDisplayLight() {
		return displayLight;
	}
	
	/** Back face culling (applies to closed Elements only) */
	public RenderContext setBackFaceCulling(boolean backfaceCulling) {
		checkNotFrozen();
		this.backfaceCulling = backfaceCulling;
		return this;
	}
	
	public boolean isBackFaceCulling() {
		return backfaceCulling;
	}

	public RenderContext setTextureProcessing(boolean textureProcessing) {
		checkNotFrozen();
		this.textureProcessing = textureProcessing;
		return this;
	}
	
	public boolean isTextureProcessing() {
		return textureProcessing;
	}

	public RenderContext setShadowing(boolean shadowing) {
		checkNotFrozen();
		this.shadowing = shadowing;
		return this;
	}
	
	public boolean isShadowing() {
		return shadowing;
	}
	
	public Color getLandmarkXColor() {
		return landmarkXColor;
	}

	public Color getLandmarkYColor() {
		return landmarkYColor;
	}

	public Color getLandmarkZColor() {
		return landmarkZColor;
	}

	/** Colors of the X, Y and Z axis when the landmark is displayed */
	public RenderContext setLandmarkColors(Color x, Color y, Color z) {
		checkNotFrozen();
		this.landmarkXColor = x;
		this.landmarkYColor = y;
		this.landmarkZColor = z;
		return this;
	}

	public Color getNormalsColor() {
		return normalsColor;
	}

	public RenderContext setNormalsColor(Color normalsColor) {
		checkNotFrozen();
		this.normalsColor = normalsColor;
		return this;
	}

	public Color getLightVectorsColor() {
		return lightVectorsColor;
	}

	public RenderContext setLightVectorsColor(Color lightVectorsColor) {
		checkNotFrozen();
		this.lightVectorsColor = lightVectorsColor;
		return this;
	}
	
	private static String onOff(boolean b) {
		return b ? "ENABLED" : "DISABLED";
	}
	
	public String toString() {
		return "Render Context" + (frozen ? " (immutable)" : "") + ":\n"
				+ "* Rendering type:        " + renderingType + "\n"
				+ "* Rendering lines:       " + onOff(renderingLines) + "\n"
				+ "* Display landmark:      " + onOff(displayLandmark) + "\n"
				+ "* Display normals:       " + onOff(displayNormals) + "\n"
				+ "* Display light vectors: " + onOff(displayLight) + "\n"
				+ "* Backface culling:      " + onOff(backfaceCulling) + "\n"
				+ "* Texture processing:    " + onOff(textureProcessing) + "\n"
				+ "* Shadowing:             " + onOff(shadowing) + "\n";
	}

}
