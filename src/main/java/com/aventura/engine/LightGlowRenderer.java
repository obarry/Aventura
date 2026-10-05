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
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.DirectionalLight;
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
 * - SUN (default for Directional lights): the disc and the halo of a light infinitely far away, at the point of the
 *   sky where its direction points. It is seen only where the sky is visible (nothing stored in the ZBuffer),
 *   so the buildings and the hills in front of it hide it, progressively on an edge, and its size is an angle
 *   (LightAppearance.setSunDiscAngle(), setSunGlowAngle());
 * - NONE: nothing.
 *
 * The sun also makes a lens flare (see drawLensFlare()): ghosts on the axis between the sun and the center of the
 * screen, adjustable or removable with LightAppearance.setFlareGain().
 *
 * The sizes of the lights with a position are given in world units (LightAppearance), so that the picture of
 * a light shrinks with the distance like any object. Only the frustum perspective is handled (lights are not drawn for an
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

	/** Number of samples of the visibility measure of the sun */
	public static final int SUN_VISIBILITY_SAMPLES = 48;

	// The glow of the sun is drawn up to this number of glow radii
	private static final float SUN_GLOW_EXTENT = 2.6f;

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

	// The sun is seen where the ZBuffer still holds the far distance (nothing was drawn there), within this tolerance
	private static final float SKY_DEPTH_TOLERANCE = 1e-3f;

	// A direction at less than this w (distance in front of the eye plane of a unit vector) is not in front of the camera
	private static final float SUN_MIN_W = 1e-4f;

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
		for (DirectionalLight light : lighting.getDirectionalLights()) {
			if (resolveMode(light, renderContext) != LightGlowMode.SUN) continue;
			if (drawSun(light, zBuffer)) drawn++;
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

	/**
	 * Disc and halo of a Directional light, at the point of the screen where its direction (towards the light)
	 * projects: a point at infinity, so the homogeneous vector (x, y, z, 0), which the View*Projection maps like
	 * any point but without the translation of the camera: the sun does not move when the camera moves, only when
	 * it turns. The sizes are angles, converted in pixels with the focal length, and the visibility is the share of
	 * the disc (a little larger than the sun) where nothing was drawn (depth = far), so the sun is hidden by
	 * anything in front of the sky.
	 */
	private boolean drawSun(DirectionalLight light, ZBuffer zBuffer) {
		LightAppearance appearance = light.getAppearance() != null ? light.getAppearance() : new LightAppearance();

		Vector3 toSun = light.getLightVectorAtPoint(null); // unit vector, from the scene towards the light
		Vector4 clip = viewProjection.getMatrix().times(new Vector4(toSun.getX(), toSun.getY(), toSun.getZ(), 0));
		float w = clip.getW();
		if (w <= SUN_MIN_W) return false; // Behind the camera

		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		float focal = halfWidth * 2f * perspectiveCtx.getPerspective().getNear()
				/ (perspectiveCtx.getPerspective().getRight() - perspectiveCtx.getPerspective().getLeft());
		float sx = clip.getX() / w * halfWidth;
		float sy = clip.getY() / w * halfHeight;
		float discRadius = Math.max(MIN_CORE_RADIUS_PIXELS, (float) Math.tan(Math.toRadians(appearance.getSunDiscAngle())) * focal);
		float glowRadius = Math.max(2f * discRadius, (float) Math.tan(Math.toRadians(appearance.getSunGlowAngle())) * focal);
		float far = perspectiveCtx.getPerspective().getFar();

		// Visibility on a disc 2.2 times larger than the sun, so that the glow fades as the disc approaches an edge
		int visible = 0;
		for (int i = 0; i < SUN_VISIBILITY_SAMPLES; i++) {
			double r = 2.2 * discRadius * Math.sqrt((i + 0.5) / SUN_VISIBILITY_SAMPLES);
			double a = i * GOLDEN_ANGLE;
			int px = Math.round(sx + (float) (r * Math.cos(a)));
			int py = Math.round(sy + (float) (r * Math.sin(a)));
			if (!inScreen(px, py) || zBuffer.get(px, py) >= far - SKY_DEPTH_TOLERANCE) visible++;
		}
		float visibility = visible / (float) SUN_VISIBILITY_SAMPLES;
		if (visibility == 0f) return false;

		Color color = appearance.getColor() != null ? appearance.getColor() : light.getLightColor();
		float cr = color.getRed() / 255f, cg = color.getGreen() / 255f, cb = color.getBlue() / 255f;
		float gain = appearance.getGain();

		float extent = SUN_GLOW_EXTENT * glowRadius;
		int cx = Math.round(sx), cy = Math.round(sy);
		int x0 = Math.max(cx - (int) Math.ceil(extent), -halfWidth), x1 = Math.min(cx + (int) Math.ceil(extent), halfWidth);
		int y0 = Math.max(cy - (int) Math.ceil(extent), -halfHeight), y1 = Math.min(cy + (int) Math.ceil(extent), halfHeight);

		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				if (!inScreen(x, y)) continue;
				float dx = x - sx, dy = y - sy;
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				if (d < extent) {
					float g = d / glowRadius;
					float t = d / (0.5f * glowRadius);
					float window = 1f - smoothstep(0.7f * extent, extent, d);
					float intensity = gain * visibility * window * (0.75f * (float) Math.exp(-g * g) + 0.18f / (1f + t * t));
					if (intensity > 0f) view.addPixel(x, y, cr * intensity, cg * intensity, cb * intensity);
				}
				// Disc: only on the sky
				if (d <= discRadius * 1.3f && zBuffer.get(x, y) >= far - SKY_DEPTH_TOLERANCE) {
					float c = smoothstep(discRadius * 1.3f, discRadius * 0.7f, d);
					view.addPixel(x, y, c, c, c);
				}
			}
		}
		if (appearance.getFlareGain() > 0f) {
			drawLensFlare(sx, sy, visibility * appearance.getFlareGain());
		}
		return true;
	}

	// Ghosts of the lens flare: position along the axis (1 = center of the image, 0 = the sun itself, 2 = the opposite
	// of the sun), radius as a share of the width of the image, color, ring or disc
	private static final float[] FLARE_POSITION = { 0.55f, 1.0f, 1.45f, 2.0f };
	private static final float[] FLARE_RADIUS = { 0.05f, 0.11f, 0.045f, 0.075f };
	private static final float[][] FLARE_COLOR = { { 0.35f, 0.65f, 1f }, { 1f, 0.75f, 0.35f }, { 0.5f, 1f, 0.55f }, { 0.9f, 0.45f, 1f } };
	private static final boolean[] FLARE_RING = { false, true, false, true };

	// The ghosts fade as the sun gets away from the center: nothing is left past this share of the half diagonal
	private static final float FLARE_FADE_DISTANCE = 1f / 0.9f;
	private static final float FLARE_AMPLITUDE = 0.9f;

	/**
	 * The lens flare of the sun: a few ghosts (discs and rings of different colors) on the line from the sun
	 * through the center of the image, at the same place whatever the objects in front (it is an effect of the
	 * lens, not of the scene). They are drawn additively, brighter when more of the sun is visible, and fade
	 * as the sun goes towards the edge of the image.
	 *
	 * @param sx x of the sun on the screen, in pixels (centered coordinates)
	 * @param sy y of the sun on the screen
	 * @param strength visibility of the sun times the flare gain
	 */
	private void drawLensFlare(float sx, float sy, float strength) {
		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		float distance = (float) Math.sqrt(sx * sx + sy * sy);
		float halfDiagonal = (float) Math.sqrt((double) halfWidth * halfWidth + (double) halfHeight * halfHeight);
		float fade = Math.max(0f, 1f - Math.min(1f, distance / halfDiagonal / FLARE_FADE_DISTANCE));
		float amplitude = strength * fade * FLARE_AMPLITUDE;
		if (amplitude <= 0f) return;

		for (int g = 0; g < FLARE_POSITION.length; g++) {
			float gx = sx * (1f - FLARE_POSITION[g]);
			float gy = sy * (1f - FLARE_POSITION[g]);
			float radius = FLARE_RADIUS[g] * 2 * halfWidth;
			int extent = (int) Math.ceil(radius * 1.6f);
			int cx = Math.round(gx), cy = Math.round(gy);
			int x0 = Math.max(cx - extent, -halfWidth), x1 = Math.min(cx + extent, halfWidth);
			int y0 = Math.max(cy - extent, -halfHeight), y1 = Math.min(cy + extent, halfHeight);
			float[] c = FLARE_COLOR[g];
			for (int y = y0; y <= y1; y++) {
				for (int x = x0; x <= x1; x++) {
					if (!inScreen(x, y)) continue;
					float dx = x - gx, dy = y - gy;
					float d = (float) Math.sqrt(dx * dx + dy * dy);
					float intensity;
					if (FLARE_RING[g]) {
						float t = (d - radius) / (0.18f * radius);
						intensity = 0.7f * (float) Math.exp(-t * t);
					} else {
						intensity = 0.45f * smoothstep(radius, 0.5f * radius, d);
					}
					intensity *= amplitude;
					if (intensity > 0.002f) view.addPixel(x, y, c[0] * intensity, c[1] * intensity, c[2] * intensity);
				}
			}
		}
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
