package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * This class is a transformation that represents a scaling (or homothety) having its center at origin O through a Matrix 4
 * The translation is formalized by a diagonal matrix.
 * 
 * @author Olivier BARRY
 * @date May 2014
 */
public class Scaling extends Matrix4 {

	public Scaling() {
		super(Matrix4.IDENTITY);
		// TODO Auto-generated constructor stub
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Scaling matrix:\n");
	}
	
	public Scaling(double s) {
		super(Matrix4.IDENTITY);
		this.array[0][0] = s;
		this.array[1][1] = s;
		this.array[2][2] = s;
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Scaling matrix with parameter (s="+s+"):\n");
	}

	public Scaling(double a, double b, double c) {
		super(Matrix4.IDENTITY);
		this.array[0][0] = a;
		this.array[1][1] = b;
		this.array[2][2] = c;
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Scaling matrix with parameters (a="+a+", b="+b+", c="+c+"):\n");
	}

}
