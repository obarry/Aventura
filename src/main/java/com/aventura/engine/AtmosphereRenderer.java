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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Fog;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.view.GUIView;

/**
 * Draws the atmosphere of the World (see Atmosphere). A post-process on the image the main pass has just finished,
 * called by RenderEngine when RenderContext.setAtmosphere(true) and the World has an Atmosphere. It has one pass for
 * each phenomenon, from the farthest layer to the nearest to the camera:
 * <ol>
 * <li>the haze (see Haze): for each pixel where something was drawn, the distance to the camera comes from the main
 * ZBuffer (linear depth W, far where nothing was drawn: the sky is never changed) and the color is blended with the
 * color of the haze: c = c + (haze - c) * f(distance);</li>
 * <li>the fogs (see Fog), one after the other, the farthest fog first: for each pixel in the part of the image where
 * the fog can be seen, a ray from the eye is followed through the fog until the surface that was drawn there (or
 * for ever for the sky: a fog hides the sky too), which gives the optical depth t of the fog on the way, and the
 * color is blended with the one of the fog: c = c * exp(-t) + fog * (1 - exp(-t)).</li>
 * </ol>
 * A pass is only done if the Atmosphere says it is on (Atmosphere.isHaze(), Atmosphere.isFog()).
 *
 * It is done before the landmarks, the light vectors and the visible lights (LightGlowRenderer), so that the sun, the
 * halos and their effects are not faded by the atmosphere, and the landmarks stay readable.
 *
 * The distance of the haze is the one to the camera, W * sqrt(1 + (x/focal)^2 + (y/focal)^2), and not W alone, so
 * that the haze does not change when the camera turns (without it the edges of the image would look nearer than they
 * are). The ray of a pixel of the fog is eye + s * (forward + x/focal * side + y/focal * up), which meets the
 * surface at s = W. Under an orthographic perspective, or when the view gives no access to its pixels
 * (GUIView.getBackPixels()), nothing is drawn. Each row of the image is independent, so they can be computed by
 * several threads, with no change to the result.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class AtmosphereRenderer {

	/** Number of entries of the table of the factor by distance (a call to exp() per pixel is avoided) */
	public static final int TABLE_SIZE = 4096;

	/** Optical depth from which a fog is considered opaque, and the span of the table of the transmittance */
	public static final float MAX_OPTICAL_DEPTH = 12f;

	// Sky pixels: the ZBuffer still holds the far distance there, within this tolerance
	private static final float SKY_DEPTH_TOLERANCE = 1e-3f;

	// Margin (pixels) around the projection of the bounding box of a fog
	private static final int FOG_MARGIN = 2;

	private final PerspectiveContext perspectiveCtx;
	private final GUIView view;

	public AtmosphereRenderer(PerspectiveContext perspectiveCtx, GUIView view) {
		this.perspectiveCtx = perspectiveCtx;
		this.view = view;
	}

	/** A fog to draw in this frame: its color and the part of the image where it is looked for (rows and columns, included) */
	private static class FogPass {
		Fog fog;
		int r0, r1, c0, c1;
		int red, green, blue;
		double distance; // From the eye to the center, to sort
	}

	/**
	 * @param atmosphere the atmosphere to draw
	 * @param background the background color of the World, the color of the haze if the Haze has none
	 * @param camera     the camera of the view (the eye and the axes of the fogs rays)
	 * @param zBuffer    the main ZBuffer, with the linear view depth of what was drawn (far for the sky)
	 * @param bands      the threads to compute the rows with, by bands (see BandRasterizer.runRows()), or null for the
	 *                   calling thread alone
	 * @return the number of pixels that were blended, with the haze and with the fogs (a pixel counts for each pass that changes it)
	 */
	public int render(Atmosphere atmosphere, Color background, Camera camera, ZBuffer zBuffer, BandRasterizer bands) {
		if (perspectiveCtx.getPerspectiveType() != PerspectiveType.FRUSTUM) return 0;
		int[] pixels = view.getBackPixels();
		if (pixels == null) return 0;

		final int width = view.getViewWidth(), height = view.getViewHeight();
		final int halfWidth = width / 2, halfHeight = height / 2;
		final float far = perspectiveCtx.getPerspective().getFar();
		final float focal = perspectiveCtx.getPixelHalfWidth() * 2f * perspectiveCtx.getPerspective().getNear()
				/ (perspectiveCtx.getPerspective().getRight() - perspectiveCtx.getPerspective().getLeft());
		final float skyLimit = far - SKY_DEPTH_TOLERANCE;

		// Haze: table of the factor by distance
		final boolean doHaze = atmosphere.isHaze();
		final float[] hazeTable = new float[doHaze ? TABLE_SIZE + 1 : 0];
		final float tableScale = TABLE_SIZE / far;
		int hr = 0, hg = 0, hb = 0;
		if (doHaze) {
			Haze haze = atmosphere.getHaze();
			for (int i = 0; i <= TABLE_SIZE; i++) {
				hazeTable[i] = haze.factorAt(far * i / TABLE_SIZE);
			}
			Color c = haze.getColor() != null ? haze.getColor() : background;
			hr = c.getRed();
			hg = c.getGreen();
			hb = c.getBlue();
		}
		final int hazeR = hr, hazeG = hg, hazeB = hb;

		// Fogs: the ones that can be seen, the farthest first
		final List<FogPass> fogs = prepareFogs(atmosphere, camera, width, height, focal);
		final boolean doFog = !fogs.isEmpty();
		final float[] transmittance = new float[doFog ? TABLE_SIZE + 1 : 0]; // exp(-t) by optical depth, 0 .. MAX_OPTICAL_DEPTH
		final float transmittanceScale = TABLE_SIZE / MAX_OPTICAL_DEPTH;
		if (doFog) {
			for (int i = 0; i <= TABLE_SIZE; i++) {
				transmittance[i] = (float) Math.exp(-(double) MAX_OPTICAL_DEPTH * i / TABLE_SIZE);
			}
		}
		if (!doHaze && !doFog) return 0;

		// Rays: the eye and the axes of the camera
		final double ox, oy, oz, fx, fy, fz, sx, sy, sz, ux, uy, uz;
		if (doFog) {
			Vector4 e = camera.getEye(), f = camera.getForward(), s = camera.getSide(), u = camera.getUp();
			ox = e.getX(); oy = e.getY(); oz = e.getZ();
			fx = f.getX(); fy = f.getY(); fz = f.getZ();
			sx = s.getX(); sy = s.getY(); sz = s.getZ();
			ux = u.getX(); uy = u.getY(); uz = u.getZ();
		} else {
			ox = oy = oz = fx = fy = fz = sx = sy = sz = ux = uy = uz = 0.0;
		}

		int[] counts = new int[height]; // blended pixels by row, summed at the end (no shared counter between threads)
		BandRasterizer.runRows(bands, 0, height, r -> {
			int y = halfHeight - r; // Centered coordinates, Y axis up
			float ny = y / focal;
			int base = r * width;
			int count = 0;
			if (doHaze) {
				for (int c = 0; c < width; c++) {
					int x = c - halfWidth;
					float depth = zBuffer.get(x, y);
					if (depth >= skyLimit) continue;
					float nx = x / focal;
					float distance = depth * (float) Math.sqrt(1f + nx * nx + ny * ny);
					int index = (int) (distance * tableScale);
					float f = hazeTable[index > TABLE_SIZE ? TABLE_SIZE : index];
					if (f <= 0f) continue;
					int p = pixels[base + c];
					int pr = (p >> 16) & 255, pg = (p >> 8) & 255, pb = p & 255;
					pr += Math.round((hazeR - pr) * f);
					pg += Math.round((hazeG - pg) * f);
					pb += Math.round((hazeB - pb) * f);
					pixels[base + c] = (pr << 16) | (pg << 8) | pb;
					count++;
				}
			}
			if (doFog) {
				for (FogPass fp : fogs) {
					if (r < fp.r0 || r > fp.r1) continue;
					Fog fog = fp.fog;
					for (int c = fp.c0; c <= fp.c1; c++) {
						int x = c - halfWidth;
						float depth = zBuffer.get(x, y);
						double sMax = depth >= skyLimit ? Double.POSITIVE_INFINITY : depth; // The ray meets the surface at s = W
						double nx = x / (double) focal, nyd = ny;
						double dx = fx + nx * sx + nyd * ux, dy = fy + nx * sy + nyd * uy, dz = fz + nx * sz + nyd * uz;
						double tau = fog.opticalDepth(ox, oy, oz, dx, dy, dz, sMax);
						if (!(tau > 0.0)) continue;
						int index = (int) (tau * transmittanceScale);
						float t = index >= TABLE_SIZE ? 0f : transmittance[index];
						if (t >= 0.9995f) continue;
						int p = pixels[base + c];
						int pr = (p >> 16) & 255, pg = (p >> 8) & 255, pb = p & 255;
						pr = Math.round(pr * t + fp.red * (1f - t));
						pg = Math.round(pg * t + fp.green * (1f - t));
						pb = Math.round(pb * t + fp.blue * (1f - t));
						pixels[base + c] = (pr << 16) | (pg << 8) | pb;
						count++;
					}
				}
			}
			counts[r] = count;
		});
		int total = 0;
		for (int n : counts) total += n;
		return total;
	}

	/**
	 * The fogs to draw, sorted from the farthest to the nearest to the eye, each with the part of the image where it
	 * can be seen: the projection of the cube that contains its bounding sphere when the cube is entirely in front of
	 * the eye, the whole image otherwise (the camera is in or near the fog).
	 */
	private List<FogPass> prepareFogs(Atmosphere atmosphere, Camera camera, int width, int height, float focal) {
		List<FogPass> passes = new ArrayList<FogPass>();
		if (camera == null || camera.getEye() == null) return passes;
		List<Fog> active = atmosphere.getActiveFogs();
		if (active.isEmpty()) return passes;

		Vector4 e = camera.getEye(), f = camera.getForward(), s = camera.getSide(), u = camera.getUp();
		final int halfWidth = width / 2, halfHeight = height / 2;
		for (Fog fog : active) {
			Vector4 c = fog.getBoundingCenter();
			double radius = fog.getBoundingRadius();
			double cx = c.getX() - e.getX(), cy = c.getY() - e.getY(), cz = c.getZ() - e.getZ();
			double forward = cx * f.getX() + cy * f.getY() + cz * f.getZ();
			if (forward + radius <= 0.0) continue; // Entirely behind the eye: no ray of the image can meet it

			FogPass fp = new FogPass();
			fp.fog = fog;
			fp.r0 = 0; fp.r1 = height - 1; fp.c0 = 0; fp.c1 = width - 1;
			fp.red = fog.getColor().getRed(); fp.green = fog.getColor().getGreen(); fp.blue = fog.getColor().getBlue();
			fp.distance = Math.sqrt(cx * cx + cy * cy + cz * cz);

			double nearest = forward - radius * (Math.abs(f.getX()) + Math.abs(f.getY()) + Math.abs(f.getZ())); // Smallest depth of the corners of the cube
			if (nearest > 1e-3) {
				double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY, yMin = xMin, yMax = xMax;
				for (int i = 0; i < 8; i++) {
					double px = cx + ((i & 1) == 0 ? -radius : radius), py = cy + ((i & 2) == 0 ? -radius : radius), pz = cz + ((i & 4) == 0 ? -radius : radius);
					double depth = px * f.getX() + py * f.getY() + pz * f.getZ();
					double x = focal * (px * s.getX() + py * s.getY() + pz * s.getZ()) / depth;
					double y = focal * (px * u.getX() + py * u.getY() + pz * u.getZ()) / depth;
					xMin = Math.min(xMin, x); xMax = Math.max(xMax, x);
					yMin = Math.min(yMin, y); yMax = Math.max(yMax, y);
				}
				// Centered coordinates (Y up) to columns and rows (row 0 on top), with a margin, clipped to the image
				fp.c0 = (int) Math.max(0, Math.floor(xMin) + halfWidth - FOG_MARGIN);
				fp.c1 = (int) Math.min(width - 1, Math.ceil(xMax) + halfWidth + FOG_MARGIN);
				fp.r0 = (int) Math.max(0, halfHeight - Math.ceil(yMax) - FOG_MARGIN);
				fp.r1 = (int) Math.min(height - 1, halfHeight - Math.floor(yMin) + FOG_MARGIN);
				if (fp.c0 > fp.c1 || fp.r0 > fp.r1) continue; // Out of the image
			}
			passes.add(fp);
		}
		passes.sort(Comparator.comparingDouble((FogPass p) -> p.distance).reversed());
		return passes;
	}
}
