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

/**
 * Consolidates diagnostic counters for a rasterization pass. Two usages:
 *
 * - Main render pass (RenderEngine): one instance, alive for the whole
 *   RenderEngine lifetime, fed one triangle at a time via recordTriangle().
 * - Shadow map generation (ShadowingLight): one instance per light, also fed
 *   one triangle at a time via recordTriangle().
 *
 * Two complementary views are kept:
 * - LIFETIME totals (renderedTriangles, totalRenderedPixels, ...): accumulate
 *   forever, matching the legacy Rasterizer's counters, which were never reset.
 * - THIS-FRAME deltas (trianglesThisFrame, renderedPixelsThisFrame, ...):
 *   computed by endFrame(), which snapshots the running totals and reports
 *   only what changed since the previous call. Call this once per frame (main
 *   pass) or once per shadow map generation (shadow pass) — whichever this
 *   instance is tracking.
 *
 * trianglesWithLines counts the triangles having at least one scan line
 * inside the screen (TriangleRasterizer.getRasterizedLines() > 0), as the
 * legacy triangles_with_lines did; trianglesWithPixels those having at least
 * one rendered pixel. A triangle with lines but no pixel is a sliver whose
 * rows cover no pixel center, or one entirely hidden by the depth test.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class RasterizerStats {

	// Lifetime totals
	private int renderedTriangles = 0;
	private int trianglesWithLines = 0;
	private int trianglesWithPixels = 0;
	private long totalRenderedPixels = 0;
	private long totalDiscardedPixels = 0;

	// Snapshot of the lifetime totals as of the last endFrame() call, used to compute deltas
	private int renderedTrianglesAtLastSnapshot = 0;
	private long totalRenderedPixelsAtLastSnapshot = 0;
	private long totalDiscardedPixelsAtLastSnapshot = 0;

	// This-frame (or this-shadow-map-generation) deltas, computed by endFrame()
	private int trianglesThisFrame = 0;
	private long renderedPixelsThisFrame = 0;
	private long discardedPixelsThisFrame = 0;

	/**
	 * Records one triangle's contribution: pass the TriangleRasterizer counters read right after
	 * rasterizing it (counters reset before each triangle, see TriangleRasterizer.resetStats()).
	 */
	public void recordTriangle(int rasterizedLinesForThisTriangle, int renderedPixelsForThisTriangle, int discardedPixelsForThisTriangle) {
		renderedTriangles++;
		if (rasterizedLinesForThisTriangle > 0) {
			trianglesWithLines++;
		}
		if (renderedPixelsForThisTriangle > 0) {
			trianglesWithPixels++;
		}
		totalRenderedPixels += renderedPixelsForThisTriangle;
		totalDiscardedPixels += discardedPixelsForThisTriangle;
	}

	/**
	 * Snapshots the running totals and computes what changed since the previous call (or since
	 * this object was created, for the first call). Call once per frame for the main render pass,
	 * or once per generateShadowMap(World) call for a shadow pass.
	 */
	public void endFrame() {
		trianglesThisFrame = renderedTriangles - renderedTrianglesAtLastSnapshot;
		renderedPixelsThisFrame = totalRenderedPixels - totalRenderedPixelsAtLastSnapshot;
		discardedPixelsThisFrame = totalDiscardedPixels - totalDiscardedPixelsAtLastSnapshot;

		renderedTrianglesAtLastSnapshot = renderedTriangles;
		totalRenderedPixelsAtLastSnapshot = totalRenderedPixels;
		totalDiscardedPixelsAtLastSnapshot = totalDiscardedPixels;
	}

	public int getRenderedTriangles() {
		return renderedTriangles;
	}

	public int getTrianglesWithLines() {
		return trianglesWithLines;
	}

	public int getTrianglesWithPixels() {
		return trianglesWithPixels;
	}

	public long getTotalRenderedPixels() {
		return totalRenderedPixels;
	}

	public long getTotalDiscardedPixels() {
		return totalDiscardedPixels;
	}

	public int getTrianglesThisFrame() {
		return trianglesThisFrame;
	}

	public long getRenderedPixelsThisFrame() {
		return renderedPixelsThisFrame;
	}

	public long getDiscardedPixelsThisFrame() {
		return discardedPixelsThisFrame;
	}

	@Override
	public String toString() {
		return "Rasterizer - Triangles: rendered: " + renderedTriangles + ", rendered with lines: " + trianglesWithLines
				+ ", rendered with pixels: " + trianglesWithPixels
				+ " | Pixels (lifetime): rendered: " + totalRenderedPixels + ", discarded: " + totalDiscardedPixels
				+ " | This frame: triangles: " + trianglesThisFrame
				+ ", pixels rendered: " + renderedPixelsThisFrame + ", discarded: " + discardedPixelsThisFrame;
	}
}