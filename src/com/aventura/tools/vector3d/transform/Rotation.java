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
		
	}
	
	/**
	 * Rotation of angle a around axis X, Y or Z
	 * @param a the rotation angle
	 * @param axis among X_axis, Y_axis or Z_axis
	 * @throws WrongAxisException
	 */
	public Rotation(double a, int axis) throws WrongAxisException {
		if (axis == Constants.X_axis) {
			
		} else if (axis == Constants.Y_axis) {
			
		} else if (axis == Constants.Z_axis) {
			
		} else {
			throw new WrongAxisException("axis value not in expected range: "+axis);
		}
	}

}
