package com.aventura.tools.vector3d.transform;

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
	
	Homothety scale;
	Rotation rotate;
	Translation translate;
	
	/**
	 * y = transform(x)
	 * @param x
	 * @return new vector, result of the transformation of x
	 */
	public Vector3 transform(Vector3 x) {
		return new Vector3();
		
	}
	
	/**
	 * x = transform(x)
	 * @param x is modified by the transformation and becomes the new transformed vector
	 */
	public void transformEquals(Vector3 x) {
		
	}

}