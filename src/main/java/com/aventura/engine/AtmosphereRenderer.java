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
import java.util.stream.IntStream;

import com.aventura.context.PerspectiveContext;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.Atmosphere;
import com.aventura.view.GUIView;

/**
 * Draws the atmosphere of the World (see Atmosphere): the effect of distance in a landscape, where the colors fade
 * towards the color of the air as the objects get farther from the camera. A post-process on the image the main
 * pass has just finished, called by RenderEngine when RenderContext.setAtmosphere(true) and the World has an
 * Atmosphere. For each pixel where something was drawn, the distance to the camera comes from the main ZBuffer
 * (linear depth W, far where nothing was drawn: the sky is never changed) and the color is blended with the color
 * of the haze: c = c + (haze - c) * f(distance).
 *
 * It is done before the landmarks, the light vectors and the visible lights (LightGlowRenderer), so that the sun, the
 * halos and their effects are not faded by the haze, and the landmarks stay readable.
 *
 * The distance is the one to the camera, W * sqrt(1 + (x/focal)^2 + (y/focal)^2), and not W alone, so that the haze
 * does not change when the camera turns (without it the edges of the image would look nearer than they are). Under an
 * orthographic perspective, or when the view gives no access to its pixels (GUIView.getBackPixels()), nothing is drawn.
 * Each row of the image is independent, so they can be computed by several threads, with no change to the result.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class AtmosphereRenderer {

	/** Number of entries of the table of the factor by distance (a call to exp() per pixel is avoided) */
	public static final int TABLE_SIZE = 4096;

	// Sky pixels: the ZBuffer still holds the far distance there, within this tolerance
	private static final float SKY_DEPTH_TOLERANCE = 1e-3f;

	private final PerspectiveContext perspectiveCtx;
	private final GUIView view;

	public AtmosphereRenderer(PerspectiveContext perspectiveCtx, GUIView view) {
		this.perspectiveCtx = perspectiveCtx;
		this.view = view;
	}

	/**
	 * @param atmosphere the haze to draw
	 * @param background the background color of the World, the color of the haze if the Atmosphere has none
	 * @param zBuffer    the main ZBuffer, with the linear view depth of what was drawn (far for the sky)
	 * @param threads    the number of threads to compute the rows with (1: only the calling thread)
	 * @return the number of pixels that were blended with the haze
	 */
	public int render(Atmosphere atmosphere, Color background, ZBuffer zBuffer, int threads) {
		if (perspectiveCtx.getPerspectiveType() != PerspectiveType.FRUSTUM) return 0;
		int[] pixels = view.getBackPixels();
		if (pixels == null) return 0;

		float far = perspectiveCtx.getPerspective().getFar();
		float[] table = new float[TABLE_SIZE + 1]; // factor by distance, 0 .. far
		for (int i = 0; i <= TABLE_SIZE; i++) {
			table[i] = atmosphere.factorAt(far * i / TABLE_SIZE);
		}
		Color haze = atmosphere.getColor() != null ? atmosphere.getColor() : background;
		final int hazeR = haze.getRed(), hazeG = haze.getGreen(), hazeB = haze.getBlue();

		final int width = view.getViewWidth(), height = view.getViewHeight();
		final int halfWidth = width / 2, halfHeight = height / 2;
		final float focal = perspectiveCtx.getPixelHalfWidth() * 2f * perspectiveCtx.getPerspective().getNear()
				/ (perspectiveCtx.getPerspective().getRight() - perspectiveCtx.getPerspective().getLeft());
		final float tableScale = TABLE_SIZE / far;
		final float skyLimit = far - SKY_DEPTH_TOLERANCE;

		int[] counts = new int[height]; // blended pixels by row, summed at the end (no shared counter between threads)
		IntStream rows = IntStream.range(0, height);
		if (threads > 1) rows = rows.parallel();
		rows.forEach(r -> {
			int y = halfHeight - r; // Centered coordinates, Y axis up
			float ny = y / focal;
			int base = r * width;
			int count = 0;
			for (int c = 0; c < width; c++) {
				int x = c - halfWidth;
				float depth = zBuffer.get(x, y);
				if (depth >= skyLimit) continue;
				float nx = x / focal;
				float distance = depth * (float) Math.sqrt(1f + nx * nx + ny * ny);
				int index = (int) (distance * tableScale);
				float f = table[index > TABLE_SIZE ? TABLE_SIZE : index];
				if (f <= 0f) continue;
				int p = pixels[base + c];
				int pr = (p >> 16) & 255, pg = (p >> 8) & 255, pb = p & 255;
				pr += Math.round((hazeR - pr) * f);
				pg += Math.round((hazeG - pg) * f);
				pb += Math.round((hazeB - pb) * f);
				pixels[base + c] = (pr << 16) | (pg << 8) | pb;
				count++;
			}
			counts[r] = count;
		});
		int total = 0;
		for (int n : counts) total += n;
		return total;
	}
}
