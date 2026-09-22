package com.aventura.test;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.TriangleRasterizer;
import com.aventura.engine.ZBuffer;
import com.aventura.tools.tracing.Tracer;

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
 * Smoke test of the rasterization pipeline set-up: builds the main-pass ZBuffer and its
 * TriangleRasterizer from a default PerspectiveContext, the same way RenderEngine does.
 * (Formerly exercised the Rasterizer compatibility façade, now removed.)
 */

public class TestRasterizer1 {

	public static void main(String[] args) {

		System.out.println("********* STARTING TEST RASTERIZER *********");
		
		Tracer.info = true;
		Tracer.function = true;
		
		// Explicit pixel size: the empty PerspectiveContext() constructor currently leaves pixel width/height at 0
		// (see the contexts audit, point 2)
		PerspectiveContext graphic = new PerspectiveContext(1600, 900, 10, 1000, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 100);
		
		System.out.println("PerspectiveContext: "+graphic);
		
		int halfWidth = graphic.getPixelHalfWidth();
		int halfHeight = graphic.getPixelHalfHeight();
		ZBuffer zBuffer = new ZBuffer(2 * halfWidth + 1, 2 * halfHeight + 1, halfWidth, halfHeight, graphic.getPerspective().getFar());
		TriangleRasterizer rasterizer = new TriangleRasterizer(graphic, zBuffer);
		
		System.out.println("ZBuffer: " + zBuffer.getWidth() + " x " + zBuffer.getHeight() + ", init depth: " + zBuffer.get(0, 0));
		System.out.println("TriangleRasterizer created: " + (rasterizer != null));

		System.out.println("********* ENDING TEST RASTERIZER *********");
	}

}
