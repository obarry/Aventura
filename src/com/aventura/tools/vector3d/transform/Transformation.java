package com.aventura.tools.vector3d.transform;

import com.aventura.tools.vector3d.matrix.Matrix3;
import com.aventura.tools.vector3d.vector.Vector3;

/**
 * This class intends to represent a complete transformation for a 3D element (either simple or complex/agglomarate). It is made
 * of a combination of:
 * - 1 rotation around Origin (R)
 * - 1 homothety having Origin as center (H)
 * - 1 translation (T)
 * 
 * So that resulting vector Y from the transformation of vector X is:
 * Y = (R.H).X + T
 *  
 * @author  Olivier BARRY
 * @date May 2014
 */
public class Transformation {
	
	protected Homothety scale;
	protected Rotation rotate;
	protected Translation translate;
	
	protected Matrix3 transform; // scale.rotation
	
	public Transformation(Homothety h, Rotation r, Translation t) {
		scale = h;
		rotate = r;
		translate = t;
		updateTransformation();
	}
	
	/**
	 * Update the transformation matrix if rotation or scale has been modified
	 */
	public void updateTransformation() {
		// Multiply rotation and scale
		transform = rotate.times(scale);
	}
	
	/**
	 * y = transform(x)
	 * @param x
	 * @return new vector, result of the transformation of x
	 */
	public Vector3 transform(Vector3 x) {
		return (transform.times(x)).plus(translate);
	}
	
	/**
	 * x = transform(x)
	 * @param x is modified by the transformation and becomes the new transformed vector
	 */
	public void transformEquals(Vector3 x) {
		x.timesEquals(transform);
		x.plusEquals(translate);
	}

}
