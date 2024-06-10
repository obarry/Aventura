package com.aventura.math.projection;

import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
 
 * Orthographic Projection Matrix
 * 
 * @author Olivier BARRY
 * @since June 2016
 * 
 */
public class Orthographic extends Projection {
	
	public Orthographic(float left, float right, float bottom, float top, float near, float far) {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "New Orthographic projection");
		
		float[][] array = { { 2/(right-left)       , 0.0f            ,  0.0f          , -(right+left)/(right-left) },
				 			{ 0.0f                 , 2/(top-bottom)  ,  0.0f          , (bottom+top)/(bottom-top)  },
				 			{ 0.0f                 , 0.0f            ,  2/(near-far)  , -(near+far)/(far-near)     },
				 			{ 0.0f                 , 0.0f            ,  0.0f          , 1.0f                       } };
		
		try {
			this.setArray(array);
		} catch (Exception e) {
			// Should never happen
		}

		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Orthographic matrix:\n"+ this);						

	}

}
