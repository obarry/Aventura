package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;

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
 * This class is a transformation that represents a rotation having its center at origin O through a Matrix 4
 * This class intends to represent a complete transformation for a 3D element (either simple or complex/agglomerated) through a Matrix4.
 * It is made  * of a combination of:
 * - 1 rotation around Origin (R)
 * - 1 scaling having Origin as center (H)
 * - 1 translation (T)
 * 
 * So that resulting vector Y from the transformation of vector X is:
 * Y = (R.H).X + T
 *  
 * @author  Olivier BARRY
 * @date May 2014
 */
public class Transformation extends Matrix4 {
		
	public Transformation(Scaling h, Rotation r, Translation t) {
		
		super(h.times(r).times(t));

	}
		
	/**
	 * Transforms the vector x into y based on transformation characteristics
	 * y = transform(x)
	 * @param x
	 * @return new vector, result of the transformation of x
	 */
	public Vector4 transform(Vector4 x) {
		return this.times(x);
	}
	
	/**
	 * x = transform(x)
	 * @param x is modified by the transformation and becomes the new transformed vector
	 */
	public void transformEquals(Vector4 x) {
		x.timesEquals(this);

	}

}
