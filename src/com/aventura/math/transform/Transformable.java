package com.aventura.math.transform;

import com.aventura.math.vector.Matrix4;

public interface Transformable {
	
	public void setTransformationMatrix(Matrix4 transformation);
	
	public Matrix4 getTransformationMatrix();
	
	// TBD add methods for translation, rotation and scaling
	// A transformable object or element should be translated, rotated or scaled
	// This behaviour (Transformable) may apply also to the Camera to allow to move the Camera
	// This behaviour may be the basis for a future evolution of the API providing means for moving the created objects of the World
	// or the other elements

}
