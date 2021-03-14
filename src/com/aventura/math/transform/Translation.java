package com.aventura.math.transform;

import com.aventura.tools.tracing.Tracer;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.math.vector.Vector3;


/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2021 Olivier BARRY
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
 * This class is a transformation that represents a translation through a Matrix 4
 * To initialize the translation, the translation Vector can be provided either through a Vector3 or Vector4.
 * The last coordinate (w) of a Vector4 is ignored; hence if this is a Point (w=1) it is simply ignored and
 * considered as a Vector.
 * 
 * @author Olivier BARRY
 * @date May 2014
 */
public class Translation extends Matrix4 {
	
	/**
	 * Create new Translation from existing Matrix4
	 * No specific validity test.
	 * @param m the Matrix4
	 */
	public Translation(Matrix4 m) {
		super(m);
	}
	
	/**
	 * Creates a Translation Matrix from a Vector4
	 * The last coordinate (w) of a Vector4 is ignored; hence if this is a Point (w=1) it is simply ignored and
	 * considered as a Vector.
	 * @param v the translation Vector
	 */
	public Translation(Vector4 v) {
		super(Matrix4.IDENTITY);
		createTranslation(v);
	}
	
	/**
	 * Creates a Translation Matrix from a Vector3
	 * @param v the translation Vector
	 */
	public Translation(Vector3 v) {
		super(Matrix4.IDENTITY);
		createTranslation(v);
	}
	
	/**
	 * Creates a Translation Matrix from a direction (Vector4) and length
	 * The last coordinate (w) of a Vector4 is ignored; hence if this is a Point (w=1) it is simply ignored and
	 * considered as a Vector.
	 * @param v the direction Vector
	 * @param l the length
	 */
	public Translation(Vector4 direction, float length) {
		super(Matrix4.IDENTITY);
		Vector3 v = new Vector3(direction);
		v.normalize();
		v.timesEquals(length);
		createTranslation(v);
	}
	
	/**
	 * Creates a Translation Matrix from a direction (Vector3) and length
	 * @param v the direction Vector
	 * @param l the length
	 */
	public Translation(Vector3 direction, float length) {
		super(Matrix4.IDENTITY);
		Vector3 v = new Vector3(direction);
		v.normalize();
		v.timesEquals(length);
		createTranslation(v);
	}
	
	protected void createTranslation(Vector4 v) {
		try {
			this.set(0, 3, v.getX());
			this.set(1, 3, v.getY());
			this.set(2, 3, v.getZ());
			if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Translation matrix:\n");
		} catch (Exception e) {
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected Error while creating new Translation with Vector4: "+v);			
			if (Tracer.exception) Tracer.traceException(this.getClass(), e.toString());			
		}
	}

	protected void createTranslation(Vector3 v) {
		try {
			this.set(0, 3, v.getX());
			this.set(1, 3, v.getY());
			this.set(2, 3, v.getZ());
			if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Translation matrix:\n");
		} catch (Exception e) {
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected Error while creating new Translation with Vector3: "+v);			
			if (Tracer.exception) Tracer.traceException(this.getClass(), e.toString());			
		}
	}

	
}
