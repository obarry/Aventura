package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
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
 * This class is a transformation that represents a rotation having its center at origin O through a Matrix 4
 * This class intends to represent a complete transformation for a 3D element (either simple or complex/agglomerated) through a Matrix4.
 * It is made  * of a combination of:
 * - 1 rotation around Origin (R)
 * - 1 scaling having Origin as center (H)
 * - 1 translation (T)
 * 
 * Applied in that order -- scaling first, then rotation, then translation -- matching the Model Matrix
 * pipeline documented on Element (scale, then rotate, then translate) and the order combineTransformation()
 * produces when called incrementally as setTransformation(scaling) then combineTransformation(rotation)
 * then combineTransformation(translation).
 *
 * So that resulting vector Y from the transformation of vector X is:
 * Y = (T.R.H).X, i.e. Y = T.(R.(H.X))
 *
 * FIX (2026): this used to build (H.times(R)).times(T) = H.R.T. Under this library's column-vector
 * convention (Y = M.X, see Matrix4.times(Vector4)/Vector4.times(Matrix4)), the right-most factor of a
 * matrix product is the one applied FIRST to X. H.R.T therefore applied Translation first and Scaling
 * last -- the reverse of the order documented above and on Element -- which would make a rotated/scaled
 * child orbit around the world origin instead of spinning/scaling in place around its own local center.
 * Fixed to T.R.H, which applies H (scale) first and T (translate) last, and which now agrees with the
 * incremental combineTransformation() path for the same three inputs (see
 * TestTransformation.testTransformation()).
 *
 * @author  Olivier BARRY
 * @date May 2014
 */
public class Transformation extends Matrix4 {

	public Transformation(Scaling h, Rotation r, Translation t) {

		super((t.times(r)).times(h));
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of new Transformation Matrix.\n"+"Scaling:\n"+h+"\nRotation:\n"+r+"\nTranslation:\n"+t);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of new Transformation Matrix.");
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Transformation:\n"+this);
	}
	
	public Transformation(Matrix4 matrix) {
		super(matrix);
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
