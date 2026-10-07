/*
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
 */
package com.aventura.model.light;

import java.awt.Color;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.color.ColorTools;

/**
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public abstract class Light {
	
	// Default Constants
	public static Color DEFAULT_LIGHT_COLOR = Color.WHITE;
	public static float DEFAULT_LIGHT_INTENSITY = 1;
	
	// Light generic attributes (more attributes in derivated classes)
	// E.g. light direction is not generic since some lights are omni-directional (e.g. PointLight) but have other attributes as the origin of the light source
	protected Color lightColor = DEFAULT_LIGHT_COLOR;
	protected float intensity = DEFAULT_LIGHT_INTENSITY;

	// How this light shows in the picture, null = as the RenderContext and the default of its type say (see LightAppearance)
	private LightAppearance appearance = null;

	/**
	 * @return the look of this light in the picture, null if it was not set (see LightAppearance, LightGlowMode)
	 */
	public LightAppearance getAppearance() {
		return appearance;
	}

	/**
	 * @param appearance the look of this light in the picture, null to follow the RenderContext and the defaults of its type.
	 *                   Only used when the lights are made visible (RenderContext.setLightGlow(true)).
	 */
	public void setAppearance(LightAppearance appearance) {
		this.appearance = appearance;
	}

	/**
	 * Default way this type of light shows in the picture, when neither the light nor the RenderContext say anything:
	 * NONE, for the types that do not declare their own.
	 */
	public LightGlowMode getDefaultGlowMode() {
		return LightGlowMode.NONE;
	}

	
	// Get light vector (or null vector for ambient light) at a given point of world space
	public abstract Vector3 getLightVectorAtPoint(Vector4 point);
	
	// Get intensity of light at a given point of world space
	public abstract float getIntensity(Vector4 point);

	/**
	 * Get color of light at a given point. Default implementation: lightColor weighted by this
	 * point's intensity — correct for every light we have today (none of them actually vary in
	 * hue by position, only in intensity). Override only if a future light needs to vary its hue
	 * by position (e.g. a colored gel, a projected texture).
	 */
	public Color getLightColorAtPoint(Vector4 point) {
		if (frameColor != null) return frameColor; // Same color at every point, computed once by prepareFrame()
		return ColorTools.multColor(lightColor, getIntensity(point));
	}

	// Color of this light for the frame being rendered, when it is the same at every point of space (see
	// prepareFrame()). null otherwise, and as soon as a setter changes the color or the intensity of the light.
	private Color frameColor = null;

	/**
	 * Prepares this light for a new frame. Called by Lighting.prepareFrame() at the start of each
	 * RenderEngine.render(): a light whose color is the same at every point (hasUniformColor()) computes it once
	 * here, instead of once per pixel in getLightColorAtPoint() (performance audit, D2a).
	 */
	public void prepareFrame() {
		frameColor = hasUniformColor() ? ColorTools.multColor(lightColor, getIntensity(null)) : null;
	}

	/**
	 * @return true if the color of this light (color x intensity) is the same at every point of space, as for an
	 *         ambient or a directional light. False by default: the subclasses that can say true override it.
	 */
	protected boolean hasUniformColor() {
		return false;
	}

	/**
	 * To be called by every setter that changes the color or the intensity of this light: the color computed
	 * by prepareFrame() is then no longer valid.
	 */
	protected void colorChanged() {
		frameColor = null;
	}

	// Set this Light's direction (Directional Light)
	public abstract void setLightVector(Vector3 light);

	// Set this Light's intensity
	public abstract void setIntensity(float intensity);		

	// Get color of light
	public Color getLightColor() {
		return lightColor;
	}
	
	// Set this Light's color
	public void setLightColor(Color c) {
		this.lightColor = c;
		colorChanged();
	}


}