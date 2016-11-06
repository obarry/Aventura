package com.aventura.model.world;

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
 * Normal calculation is Element specific hence each Element should implement it on its own.
 * A Normal can be either at Vertex level (general case) or at Triangle level (this is 	a more specific case as this will not
 * generate a smooth and continuous surface).
 * 
 * 
 * @author Bricolage Olivier
 * @since November 2016
 */

public interface NormalGeneration {

	/**
	 * Calculate the Normals for this Element.
	 * Normal calculation is Element specific hence each Element should implement it on its own.
	 * A Normal can be either at Vertex level (general case) or at Triangle level (this is 	a more specific case as this will not
	 * generate a smooth and continuous surface).
	 */
	public abstract void calculateNormals();

}
