package com.aventura.model.camera;

import com.aventura.math.transform.LookAt;
import com.aventura.math.vector.Matrix4;

public class Camera {
	
	protected LookAt camera;
	
	public Camera() {
		super();
	}
	
	public Matrix4 getMatrix() {
		return (Matrix4)camera;
	}

}
