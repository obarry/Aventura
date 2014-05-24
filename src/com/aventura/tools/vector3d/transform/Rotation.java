package com.aventura.tools.vector3d.transform;

import com.aventura.tools.vector3d.Constants;
import com.aventura.tools.vector3d.WrongAxisException;
import com.aventura.tools.vector3d.matrix.Matrix3;
import com.aventura.tools.vector3d.vector.Vector3;

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
		array = new double[Constants.SIZE][Constants.SIZE];
		double cos = Math.cos(a);
		double sin = Math.sin(a);
		// Initialize the array, depending on axis
		if (axis == Constants.X_axis) {
			// First row
			this.array[0][0] = 1.0;
			this.array[0][1] = 0.0;
			this.array[0][2] = 0.0;
			// Second row
			this.array[1][0] = 0.0;
			this.array[1][1] = cos;
			this.array[1][2] = -sin;
			// Third row
			this.array[2][0] = 0.0;
			this.array[2][1] = sin;
			this.array[2][2] = cos;			

		} else if (axis == Constants.Y_axis) {
			// First row
			this.array[0][0] = cos;
			this.array[0][1] = 0.0;
			this.array[0][2] = sin;
			// Second row
			this.array[1][0] = 0.0;
			this.array[1][1] = 1.0;
			this.array[1][2] = 0.0;
			// Third row
			this.array[2][0] = -sin;
			this.array[2][1] = 0.0;
			this.array[2][2] = cos;			
			
		} else if (axis == Constants.Z_axis) {
			// First row
			this.array[0][0] = cos;
			this.array[0][1] = -sin;
			this.array[0][2] = 0.0;
			// Second row
			this.array[1][0] = sin;
			this.array[1][1] = cos;
			this.array[1][2] = 0.0;
			// Third row
			this.array[2][0] = 0.0;
			this.array[2][1] = 0.0;
			this.array[2][2] = 1.0;			
			
		} else {
			throw new WrongAxisException("axis value not in expected range: "+axis);
		}
	}

}
