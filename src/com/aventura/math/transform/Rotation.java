package com.aventura.math.transform;

import com.aventura.math.Constants;
import com.aventura.math.vector.IndiceOutOfBoundException;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.MatrixArrayWrongSizeException;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2022 Olivier BARRY
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
 * The rotation is implemented in the 3x3 upper left part of the 4x4 Matrix
 * 
 * @author Olivier BARRY
 * @date May 2014
 */


public class Rotation extends Matrix4 {

	public Rotation() {
		super(Matrix4.IDENTITY);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Rotation of angle a around vector v
	 * @param a the rotation angle
	 * @param v the vector representing the axis of rotation
	 */
	public Rotation(float a, Vector3 v) {
		super(Matrix4.IDENTITY);
		initRotation(a, v);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Rotation matrix:\n");
	}
	
	public Rotation(float a, Vector4 v) {
		super(Matrix4.IDENTITY);
		initRotation(a, v.V3());
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creation of Rotation matrix:\n");
	}
	
	protected void initRotation(float a, Vector3 v) {
		Vector3 v1 = new Vector3(v);
		v1.normalize();
		float cos = (float)Math.cos(a);
		float sin = (float)Math.sin(a);
		
		// First row
		this.array[0][0] = v1.getX()*v1.getX()+ (1-v1.getX()*v1.getX())*cos;
		this.array[0][1] = v1.getX()*v1.getY()*(1-cos)-v1.getZ()*sin;
		this.array[0][2] = v1.getX()*v1.getZ()*(1-cos)+v1.getY()*sin;
		// Second row
		this.array[1][0] = v1.getX()*v1.getY()*(1-cos)+v1.getZ()*sin;
		this.array[1][1] = v1.getY()*v1.getY()+(1-v1.getY()*v1.getY())*cos;
		this.array[1][2] = v1.getY()*v1.getZ()*(1-cos)-v1.getX()*sin;
		// Third row
		this.array[2][0] = v1.getX()*v1.getZ()*(1-cos)-v1.getY()*sin;
		this.array[2][1] = v1.getY()*v1.getZ()*(1-cos)+v1.getX()*sin;
		this.array[2][2] = v1.getZ()*v1.getZ()+ (1-v1.getZ()*v1.getZ())*cos;					
	}
	
	/**
	 * Rotation of angle a around axis X, Y or Z
	 * @param a the rotation angle
	 * @param axis among X_axis, Y_axis or Z_axis
	 * @throws WrongAxisException
	 */
	public Rotation(float a, int axis) throws WrongAxisException {
		super(Matrix4.IDENTITY);
		float cosa = (float)Math.cos(a);
		float sina = (float)Math.sin(a);
		// Initialize the array, depending on axis
		if (axis == Constants.X_axis) {
			// First row
			this.array[0][0] = 1.0f;
			this.array[0][1] = 0.0f;
			this.array[0][2] = 0.0f;
			// Second row
			this.array[1][0] = 0.0f;
			this.array[1][1] = (float)cosa;
			this.array[1][2] = (float)-sina;
			// Third row
			this.array[2][0] = 0.0f;
			this.array[2][1] = (float)sina;
			this.array[2][2] = (float)cosa;			

		} else if (axis == Constants.Y_axis) {
			// First row
			this.array[0][0] = (float)cosa;
			this.array[0][1] = 0.0f;
			this.array[0][2] = (float)sina;
			// Second row
			this.array[1][0] = 0.0f;
			this.array[1][1] = 1.0f;
			this.array[1][2] = 0.0f;
			// Third row
			this.array[2][0] = (float)-sina;
			this.array[2][1] = 0.0f;
			this.array[2][2] = (float)cosa;			
			
		} else if (axis == Constants.Z_axis) {
			// First row
			this.array[0][0] = (float)cosa;
			this.array[0][1] = (float)-sina;
			this.array[0][2] = 0.0f;
			// Second row
			this.array[1][0] = (float)sina;
			this.array[1][1] = (float)cosa;
			this.array[1][2] = 0.0f;
			// Third row
			this.array[2][0] = 0.0f;
			this.array[2][1] = 0.0f;
			this.array[2][2] = 1.0f;			
			
		} else {
			throw new WrongAxisException("axis value not in expected range: "+axis);
		}
	}
	
	public Rotation(Matrix4 a) throws NotARotationException {
		super(a);
		if (!this.isRotation()) throw new NotARotationException("This matrix is not a rotation matrix: "+this); 
	}
	
	public Rotation(float[][] array) throws NotARotationException, MatrixArrayWrongSizeException {
		super(array);
		if (!this.isRotation()) throw new NotARotationException("This matrix is not a rotation matrix: "+this); 
	}
	
	protected boolean isRotation() {
		
		Vector4 v1;
		Vector4 v2;
		Vector4 v3;
		
		try {
			v1=this.getColumn(0);
			v2=this.getColumn(1);
			v3=this.getColumn(2);
		} catch (IndiceOutOfBoundException e) {
			if (Tracer.exception) Tracer.traceException(this.getClass(), "Unexpected exception: "+e);
			return false;
		}
		
		// A matrix is a Rotation matrix when the 3 column vectors represent a direct orthonormal basis (length of vectors is 1, 2 first vectors are orthogonal and the 3d 1 is eqals to the vector product of 2 first problems
		if (v1.length() != 1 || v2.length() !=1 || v3.length() != 1) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Column vectors length is not equals to 1. V1 length: "+v1.length()+" V2 length: "+v2.length()+"  V3 length: "+v3.length());
			return false;
		}
		if(v1.dot(v2) != 0) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Column Vectors V1 and V2 are not orthogonal. V1: "+v1+" V2: "+v2+" V1.V2: "+v1.dot(v2));
			return false;
		}
		if(!(v1.times(v2)).equals(v3)) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "V3 is not equals to V1^V2. V3: "+v3+" V1^V2: "+v1.times(v2));
			return false;
		}
		
		// Else is Ok
		return true;
	}

}
