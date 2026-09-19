package com.aventura.engine;

import com.aventura.tools.tracing.Tracer;
import com.aventura.view.MapView;

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
 * Depth buffer, decoupled from any specific rendering pass.
 *
 * A single ZBuffer instance can be used interchangeably for the main render
 * pass (built from the camera's PerspectiveContext half-dimensions) or for a
 * shadow map generation pass (built from a light's map dimensions) — the
 * caller decides the size and half-extents at construction time, this class
 * only knows how to test and update depth values.
 *
 * Screen space is centered on the origin ([-halfWidth, halfWidth] x
 * [-halfHeight, halfHeight]) while the underlying storage (MapView) is
 * indexed from [0, width) x [0, height). This class hides that translation
 * from callers, replacing the getXzBuf()/getYzBuf() pair that used to live
 * on Rasterizer.
 *
 * @author Olivier BARRY
 * @since 2026 (extracted from Rasterizer)
 *
 */

public class ZBuffer {

	private final MapView map;
	private final int width;
	private final int height;
	private final int halfWidth;
	private final int halfHeight;

	// One-entry cache of the last (x, y) -> (bx, by) translation done by test(), reused by update()
	// (new - see audit report: update() is already documented below to always be called right after
	// a passing test() for the SAME (x, y, z), so recomputing toBufferX()/toBufferY() a second time
	// in update() was pure redundant work, done on every single surviving pixel of every frame).
	// lastValid guards the very first call (no test() has run yet) and any call to update() that
	// does NOT follow the documented pattern -- in both cases update() falls back to recomputing the
	// translation itself, so behavior is unchanged for every caller; only the documented
	// test()-then-update() pattern gets faster.
	private boolean lastValid = false;
	private int lastX, lastY, lastBx, lastBy;

	/**
	 * @param width      buffer width in pixels (storage space, e.g. 2*halfWidth+1)
	 * @param height     buffer height in pixels (storage space, e.g. 2*halfHeight+1)
	 * @param halfWidth  half-width of screen space, used to translate centered screen
	 *                   coordinates into buffer indices
	 * @param halfHeight half-height of screen space, same purpose as halfWidth
	 * @param initValue  value every cell is initialized to (typically the "far" plane distance)
	 */
	public ZBuffer(int width, int height, int halfWidth, int halfHeight, float initValue) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ZBuffer. Width: " + width + " Height: " + height);
		this.width = width;
		this.height = height;
		this.halfWidth = halfWidth;
		this.halfHeight = halfHeight;
		this.map = new MapView(width, height);
		clear(initValue);
	}

	/**
	 * Resets every cell of the buffer to the given value (typically the "far" plane distance,
	 * so that any actual fragment is closer and will pass the depth test).
	 */
	public void clear(float initValue) {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				map.set(i, j, initValue);
			}
		}
	}

	/**
	 * Tests whether a fragment at centered screen-space (x, y) with depth z is at least as close
	 * as what is currently stored, WITHOUT updating the buffer. This lets a FragmentConsumer skip
	 * expensive shading work for fragments that would be discarded anyway, and only call update()
	 * once the fragment is actually going to be drawn.
	 *
	 * @return true if the fragment passes the depth test (should be drawn), false otherwise —
	 *         including when (x, y) falls outside the buffer bounds.
	 */
	public boolean test(int x, int y, float z) {
		int bx = toBufferX(x);
		int by = toBufferY(y);

		lastX = x;
		lastY = y;
		lastBx = bx;
		lastBy = by;
		lastValid = true;

		if (!inBounds(bx, by)) {
			if (Tracer.error) Tracer.traceError(this.getClass(), "ZBuffer test: coordinates out of bounds (" + bx + ", " + by + ") for size " + width + "x" + height);
			return false;
		}
		return z <= map.get(bx, by);
	}

	/**
	 * Records z as the new closest depth for centered screen-space (x, y). Should only be called
	 * after test() returned true for the same (x, y, z) — this method does not re-check the test,
	 * it unconditionally overwrites.
	 */
	public void update(int x, int y, float z) {
		int bx, by;
		if (lastValid && lastX == x && lastY == y) {
			// The documented common case: this update() follows a test() for this same (x, y) --
			// reuse its already-computed buffer coordinates instead of translating again.
			bx = lastBx;
			by = lastBy;
		} else {
			// Not preceded by a matching test() (e.g. called directly, or for a different pixel) --
			// fall back to computing the translation here, exactly as before this optimization.
			bx = toBufferX(x);
			by = toBufferY(y);
		}
		if (!inBounds(bx, by)) {
			if (Tracer.error) Tracer.traceError(this.getClass(), "ZBuffer update: coordinates out of bounds (" + bx + ", " + by + ") for size " + width + "x" + height);
			return;
		}
		map.set(bx, by, z);
	}

	/** Reads the currently stored depth at centered screen-space (x, y). */
	public float get(int x, int y) {
		return map.get(toBufferX(x), toBufferY(y));
	}

	/**
	 * Exposes the underlying MapView, e.g. for a ShadowingLight that wants to keep handing out
	 * its shadow map the same way it does today (getMap()/getMap(x,y)).
	 */
	public MapView getMapView() {
		return map;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	private int toBufferX(int x) {
		return x + halfWidth;
	}

	private int toBufferY(int y) {
		return y + halfHeight;
	}

	private boolean inBounds(int bx, int by) {
		return bx >= 0 && bx < width && by >= 0 && by < height;
	}
}