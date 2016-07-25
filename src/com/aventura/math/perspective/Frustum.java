package com.aventura.math.perspective;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016 Olivier BARRY
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
 * Frustum perspective Matrix
 * 
 * @author Bricolage Olivier
 * @since June 2016
 * 
 */
public class Frustum extends Perspective {
	
	Frustum(double left, double right, double bottom, double top, double near, double far) {
		
		double[][] array = { { (2-near)/(right-left), 0.0,                   (right+left)/(right-left), 0.0                   },
				 			 { 0.0                  , (2-near)/(top-bottom), (top+bottom)/(top-bottom), 0.0                   },
				 			 { 0.0                  , 0.0,                   (near+far)/(near-far)    , 2*near*far/(near-far) },
				 			 { 0.0                  , 0.0,                   -1.0                     , 0.0                   } };
		
		try {
			this.setArray(array);
		} catch (Exception e) {
			// Should never happen
		}
						
	}

}
