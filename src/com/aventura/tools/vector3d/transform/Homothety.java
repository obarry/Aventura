package com.aventura.tools.vector3d.transform;

import com.aventura.tools.vector3d.matrix.Matrix3;

public class Homothety extends Matrix3 {

	public Homothety() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void setScale(double s) {
		initialize(0);
		setDiagonal(s);
	}

}
