package com.aventura.math.transform;

import com.aventura.math.Constants;
import com.aventura.math.vector.IndiceOutOfBoundException;
import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.MatrixArrayWrongSizeException;
import com.aventura.math.vector.Vector3;
import com.aventura.tools.tracing.Tracer;

public class Rotation extends Matrix3 {

	public Rotation() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Rotation of angle a around vector v
	 * @param a the rotation angle
	 * @param v the vector representing the axis of rotation
	 */
	public Rotation(double a, Vector3 v) {
		super();
		Vector3 v1 = new Vector3(v);
		v1.normalize();
		double cos = Math.cos(a);
		double sin = Math.sin(a);
		
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
	public Rotation(double a, int axis) throws WrongAxisException {
		// Create the array
		array = new double[Constants.SIZE_3][Constants.SIZE_3];
		double cosa = Math.cos(a);
		double sina = Math.sin(a);
		// Initialize the array, depending on axis
		if (axis == Constants.X_axis) {
			// First row
			this.array[0][0] = 1.0;
			this.array[0][1] = 0.0;
			this.array[0][2] = 0.0;
			// Second row
			this.array[1][0] = 0.0;
			this.array[1][1] = cosa;
			this.array[1][2] = -sina;
			// Third row
			this.array[2][0] = 0.0;
			this.array[2][1] = sina;
			this.array[2][2] = cosa;			

		} else if (axis == Constants.Y_axis) {
			// First row
			this.array[0][0] = cosa;
			this.array[0][1] = 0.0;
			this.array[0][2] = sina;
			// Second row
			this.array[1][0] = 0.0;
			this.array[1][1] = 1.0;
			this.array[1][2] = 0.0;
			// Third row
			this.array[2][0] = -sina;
			this.array[2][1] = 0.0;
			this.array[2][2] = cosa;			
			
		} else if (axis == Constants.Z_axis) {
			// First row
			this.array[0][0] = cosa;
			this.array[0][1] = -sina;
			this.array[0][2] = 0.0;
			// Second row
			this.array[1][0] = sina;
			this.array[1][1] = cosa;
			this.array[1][2] = 0.0;
			// Third row
			this.array[2][0] = 0.0;
			this.array[2][1] = 0.0;
			this.array[2][2] = 1.0;			
			
		} else {
			throw new WrongAxisException("axis value not in expected range: "+axis);
		}
	}
	
	public Rotation(Matrix3 a) throws NotARotationException {
		super(a);
		if (!this.isRotation()) throw new NotARotationException("This matrix is not a rotation matrix: "+this); 
	}
	
	public Rotation(double[][] array) throws NotARotationException, MatrixArrayWrongSizeException {
		super(array);
		if (!this.isRotation()) throw new NotARotationException("This matrix is not a rotation matrix: "+this); 
	}
	
	protected boolean isRotation() {
		
		Vector3 V1;
		Vector3 V2;
		Vector3 V3;
		
		try {
			V1=this.getColumn(0);
			V2=this.getColumn(1);
			V3=this.getColumn(2);
		} catch (IndiceOutOfBoundException e) {
			if (Tracer.exception) Tracer.traceException(this.getClass(), "Unexpected exception: "+e);
			return false;
		}
		
		// A matrix is a Rotation matrix when the 3 column vectors represent a direct orthonormal basis (length of vectors is 1, 2 first vectors are orthogonal and the 3d 1 is eqals to the vector product of 2 first problems
		if (V1.length() != 1 || V2.length() !=1 || V3.length() != 1) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Column vectors length is not equals to 1. V1 length: "+V1.length()+" V2 length: "+V2.length()+"  V3 length: "+V3.length());
			return false;
		}
		if(V1.scalar(V2) != 0) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Column Vectors V1 and V2 are not orthogonal. V1: "+V1+" V2: "+V2+" V1.V2: "+V1.scalar(V2));
			return false;
		}
		if(!(V1.times(V2)).equals(V3)) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "V3 is not equals to V1^V2. V3: "+V3+" V1^V2: "+V1.times(V2));
			return false;
		}
		
		// Else is Ok
		return true;
	}

}
