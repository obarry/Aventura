package com.aventura.engine;

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
 * A FragmentConsumer receives one Fragment per pixel produced by
 * TriangleRasterizer and decides what to do with it — compute and draw a
 * shaded color (ShadingConsumer), or only record depth for a shadow map
 * (DepthOnlyConsumer, to come).
 *
 * This is the extension point that replaces the old boolean parameters
 * (interpolate, texture, shadows, shadowmap) that used to be threaded
 * through rasterizeTriangle()/rasterizeScanLine(): what used to be "which
 * combination of booleans" is now simply "which FragmentConsumer
 * implementation is passed to TriangleRasterizer.rasterize()".
 *
 * LIFECYCLE — see Fragment's own class-level contract: the Fragment instance
 * passed to consume() is reused and mutated by TriangleRasterizer for every
 * pixel. Never retain the reference beyond the call.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public interface FragmentConsumer {

	void consume(Fragment fragment);

}