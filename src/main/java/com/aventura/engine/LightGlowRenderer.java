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
package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Light;
import com.aventura.model.light.LightAppearance;
import com.aventura.model.light.LightGlowMode;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.SpotLight;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.view.GUIView;

/**
 * Makes the lights visible in the picture (RenderContext.setLightGlow(true)): a post-process drawn on the
 * finished frame, just before it is displayed, that adds the light of the glow around the lights
 * (additively, so a glow never hides what is behind it).
 *
 * The positions of the lights are projected with the View*Projection of the camera. A light is hidden by
 * the objects in front of it: its visibility is measured by sampling the main ZBuffer (the depth of the
 * objects, as eye distance W) on a small disc around the projected position, like an occlusion query
 * (a light partly hidden fades out progressively, a light behind an edge does not pop). Samples outside
 * of the screen count as visible, so that a light slightly out of the image keeps its glow in the image.
 *
 * What is drawn depends on the LightGlowMode of each light (see resolveMode()):
 * - HALO (default for Point and Spot lights): a bright core and a soft glow around it;
 * - EMISSIVE: the bright core only;
 * - NONE and SUN: nothing (SUN, for Directional lights, is not implemented yet).
 *
 * The sizes are given in world units (LightAppearance), so that the picture of a light shrinks with the
 * distance like any object. Only the frustum perspective is handled (lights are not drawn for an
 * orthographic perspective), and the lights behind the near plane are not drawn.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class LightGlowRenderer {

	/** Number of samples of the visibility measure of a light */
	public static final int VISIBILITY_SAMPLES = 32;

	/** A sample is visible if the depth stored in the ZBuffer is at least the depth of the light minus this tolerance (world units) */
	public static final float DEPTH_TOLERANCE = 0.03f;

	/** Minimum radius of the core on the screen, in pixels, so that a far light remains a visible dot */
	public static final float MIN_CORE_RADIUS_PIXELS = 1.5f;

	// Golden angle, in radians: spreads the samples regularly over the disc
	private static final double GOLDEN_ANGLE = 2.399963229728653;

	// The glow is drawn up to this number of glow radii (its long tail is brought smoothly to zero there)
	private static final float GLOW_EXTENT = 3.5f;

	private final PerspectiveContext perspectiveCtx;
	private final ViewProjection viewProjection;
	private final GUIView view;

	public LightGlowRenderer(PerspectiveContext perspectiveCtx, ViewProjection viewProjection, GUIView view) {
		this.perspectiveCtx = perspectiveCtx;
		this.viewProjection = viewProjection;
		this.view = view;
	}

	/**
	 * The mode that applies to a light: its own appearance if it defines a mode, else the one forced by the
	 * RenderContext if any, else the default mode of its type.
	 */
	public static LightGlowMode resolveMode(Light light, RenderContext renderContext) {
		LightAppearance appearance = light.getAppearance();
		if (appearance != null && appearance.getMode() != null) return appearance.getMode();
		if (renderContext != null && renderContext.getLightGlowMode() != null) return renderContext.getLightGlowMode();
		return light.getDefaultGlowMode();
	}

	/**
	 * Draws the lights of the lighting into the view. To be called when the frame is complete (after the
	 * rasterization of the objects), with the ViewProjection already refreshed for this frame.
	 *
	 * @return the number of lights drawn (visible, at least partly)
	 */
	public int render(Lighting lighting, Camera camera, RenderContext renderContext, ZBuffer zBuffer) {
		if (perspectiveCtx.getPerspectiveType() != PerspectiveType.FRUSTUM) return 0;
		int drawn = 0;
		// The Spot lights are in the same list as the Point lights
		for (PointLight light : lighting.getPointLights()) {
			LightGlowMode mode = resolveMode(light, renderContext);
			if (mode != LightGlowMode.HALO && mode != LightGlowMode.EMISSIVE) continue;
			if (drawPointLight(light, mode, camera, zBuffer)) drawn++;
		}
		return drawn;
	}

	private boolean drawPointLight(PointLight light, LightGlowMode mode, Camera camera, ZBuffer zBuffer) {
		LightAppearance appearance = light.getAppearance() != null ? light.getAppearance() : new LightAppearance();

		Vector4 clip = viewProjection.project(light.getPosition());
		float w = clip.getW(); // Eye distance of the light, for a frustum projection
		if (w < perspectiveCtx.getPerspective().getNear()) return false; // Behind the near plane

		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		// Focal length in pixels: a length L at eye distance w measures L * focal / w pixels
		float focal = halfWidth * 2f * perspectiveCtx.getPerspective().getNear()
				/ (perspectiveCtx.getPerspective().getRight() - perspectiveCtx.getPerspective().getLeft());
		float sx = clip.getX() / w * halfWidth;
		float sy = clip.getY() / w * halfHeight;
		float coreRadius = Math.max(MIN_CORE_RADIUS_PIXELS, appearance.getCoreRadius() * focal / w);
		float glowRadius = appearance.getGlowRadius() * focal / w;

		// Visibility: the share of the core's disc that is not hidden by an object
		int visible = 0;
		for (int i = 0; i < VISIBILITY_SAMPLES; i++) {
			double r = coreRadius * Math.sqrt((i + 0.5) / VISIBILITY_SAMPLES);
			double a = i * GOLDEN_ANGLE;
			int px = Math.round(sx + (float) (r * Math.cos(a)));
			int py = Math.round(sy + (float) (r * Math.sin(a)));
			if (!inScreen(px, py) || zBuffer.get(px, py) >= w - DEPTH_TOLERANCE) visible++;
		}
		float visibility = visible / (float) VISIBILITY_SAMPLES;
		if (visibility == 0f) return false;

		Color color = appearance.getColor() != null ? appearance.getColor() : light.getLightColor();
		float cr = color.getRed() / 255f, cg = color.getGreen() / 255f, cb = color.getBlue() / 255f;

		float gain = appearance.getGain();
		if (light instanceof SpotLight) {
			// A Spot light seen from outside of its cone looks dimmer
			gain *= ((SpotLight) light).coneFactor(camera.getEye());
		}

		boolean halo = mode == LightGlowMode.HALO;
		float extent = halo ? Math.max(GLOW_EXTENT * glowRadius, coreRadius * 1.4f) : coreRadius * 1.4f;
		int cx = Math.round(sx), cy = Math.round(sy);
		int x0 = Math.max(cx - (int) Math.ceil(extent), -halfWidth), x1 = Math.min(cx + (int) Math.ceil(extent), halfWidth);
		int y0 = Math.max(cy - (int) Math.ceil(extent), -halfHeight), y1 = Math.min(cy + (int) Math.ceil(extent), halfHeight);

		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				if (!inScreen(x, y)) continue;
				float dx = x - sx, dy = y - sy;
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				if (halo && d < GLOW_EXTENT * glowRadius) {
					float g = d / glowRadius;
					float t = d / (0.7f * glowRadius);
					// The window brings the long tail of the glow smoothly to zero at its extent (no visible edge)
					float window = 1f - smoothstep(0.7f * GLOW_EXTENT * glowRadius, GLOW_EXTENT * glowRadius, d);
					float intensity = gain * visibility * window * (0.55f * (float) Math.exp(-g * g) + 0.25f / (1f + t * t));
					if (intensity > 0f) view.addPixel(x, y, cr * intensity, cg * intensity, cb * intensity);
				}
				// Core: depth tested, so that it does not show through an object that hides part of it
				if (d <= coreRadius * 1.4f && zBuffer.get(x, y) >= w - DEPTH_TOLERANCE) {
					float c = smoothstep(coreRadius * 1.4f, coreRadius * 0.6f, d);
					// The core is whitish, tinted by the color of the light
					view.addPixel(x, y, c * (0.35f + 0.65f * cr), c * (0.35f + 0.65f * cg), c * (0.35f + 0.65f * cb));
				}
			}
		}
		return true;
	}

	// Same bounds as the images centered on the origin (see ImageView.inImage())
	private boolean inScreen(int x, int y) {
		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		return x >= -halfWidth && x <= halfWidth && y >= -halfHeight && y <= halfHeight;
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
		return t * t * (3f - 2f * t);
	}
}
