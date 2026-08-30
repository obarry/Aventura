package com.aventura.engine;

/**
 * ------------------------------------------------------------------------------
 * Consolidates diagnostic counters for a rasterization pass. Two usages:
 *
 * - Main render pass (Rasterizer façade): one instance, alive for the whole
 *   RenderEngine lifetime, fed one triangle at a time via recordTriangle().
 * - Shadow map generation (ShadowingLight): one instance, fed once per
 *   generateShadowMap(World) call via recordBatch(), since triangles aren't
 *   tracked individually there.
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
 * NOTE: trianglesWithLines is always 0 today. The legacy counter it mirrors
 * (triangles_with_lines) was derived from rasterized_lines, a per-scanline
 * count the new TriangleRasterizer pipeline doesn't track. Diagnostics-only,
 * no functional impact — see the backlog item about it.
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
	 * Records one triangle's contribution -- used by the main render pass, where triangles are
	 * rasterized (and their pixel counts known) one at a time.
	 */
	public void recordTriangle(int renderedPixelsForThisTriangle, int discardedPixelsForThisTriangle) {
		renderedTriangles++;
		if (renderedPixelsForThisTriangle > 0) {
			trianglesWithPixels++;
		}
		totalRenderedPixels += renderedPixelsForThisTriangle;
		totalDiscardedPixels += discardedPixelsForThisTriangle;
	}

	/**
	 * Records a whole batch of triangles at once -- used by shadow map generation, where
	 * TriangleRasterizer's pixel counters accumulate over every triangle of the pass rather than
	 * being read (and reset) triangle by triangle.
	 */
	public void recordBatch(int trianglesProcessed, int renderedPixelsInBatch, int discardedPixelsInBatch) {
		renderedTriangles += trianglesProcessed;
		totalRenderedPixels += renderedPixelsInBatch;
		totalDiscardedPixels += discardedPixelsInBatch;
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