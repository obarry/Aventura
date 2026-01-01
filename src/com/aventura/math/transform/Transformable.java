package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;

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
 */

public interface Transformable {
	
	/**
	 * Set the transformation Matrix for this transformable object
	 * @param transformation
	 */
	public void setTransformation(Matrix4 transformation);
	
	/**
	 * Combine the existing transformation Matrix of the transformable object by a supplementary transformation.
	 * This is done by multiplying the 2 matrices and replacing the transformation Matrix of the transformable
	 * object by the resulting Matrix.
	 * @param transformation, the Matrix representing the supplementary transformation
	 */
	public void combineTransformation(Matrix4 transformation);
	
	/**
	 * Provide the transformation Matrix of the transformable object
	 * @return
	 */
	public Matrix4 getTransformation();
	
	/**Transform immediately the Element (by transforming all its vertices) regardless the transformation Matrix of this Element.
	 * @param transformation, the Matrix to be used for this immediate transformation
	 */
	public void transform(Matrix4 transformation);
	
	// TBD add methods for translation, rotation and scaling
	// A transformable object or element should be translated, rotated or scaled
	// This behaviour (Transformable) may apply also to the Camera to allow to move the Camera
	// This behaviour may be the basis for a future evolution of the API providing means for moving the created objects of the World
	// or the other elements

}
