package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;

public interface Transformable {
	
	public void setTransformation(Matrix4 transformation);
	
	public Matrix4 getTransformation();

}
