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
 * A FragmentConsumer that only records depth, without computing any color.
 * Used for shadow map generation: TriangleRasterizer.rasterize() is called
 * exactly the same way as for normal rendering, just with this consumer
 * instead of a ShadingConsumer — this is what replaces the old shadowmap
 * boolean and its parallel, simplified code path.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class DepthOnlyConsumer implements FragmentConsumer {

	private final ZBuffer zBuffer;

	public DepthOnlyConsumer(ZBuffer zBuffer) {
		this.zBuffer = zBuffer;
	}

	@Override
	public void consume(Fragment fragment) {
		// TriangleRasterizer already ran ZBuffer.test() before calling consume() for this
		// fragment, so it is already the closest one seen so far for its pixel -- nothing to
		// decide here, just record the depth.
		zBuffer.update(fragment.getScreenX(), fragment.getScreenY(), fragment.getZ());
	}
}