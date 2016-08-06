package com.aventura.model.camera;

import com.aventura.math.transform.LookAt;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;

public class Camera {

	protected LookAt camera;

	public Camera() {
		super();
	}

	public Camera(LookAt la) {
		super();
		camera = la;
	}

	public Camera(Matrix4 m) {
		super();
		camera = (LookAt) m;
	}

	/**
	 * 
	 * Construction of Camera's LookAt Matrix based on Points and Vector4
	 * 
	 * @param e the Eye point
	 * @param p the Point of interest
	 * @param u the Up vector for the World to leverage for the Camera
	 */
	public Camera(Vector4 e, Vector4 p, Vector4 u) {
		super();
		camera = new LookAt(e,p,u);
	}

	public Matrix4 getMatrix() {
		return (Matrix4) camera;
	}

}
