package com.aventura.tools.vector3d.transform;

import com.aventura.tools.vector3d.matrix.Matrix3;

/**
 * This class is a transformation that represents an homothety having its center at origin O.
 * The translation is formalized by a diagonal matrix. Thus this class extends the class Matrix3.
 * 
 * @author Olivier BARRY
 * @date May 2014
 */
public class Homothety extends Matrix3 {

	public Homothety() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void setScale(double s) {
		initialize(0);
		setDiagonal(s);
	}
	
	public double getScale() {
		// Any value of the diagonal should be the same
		return array[0][0];
	}

}
