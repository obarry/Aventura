package com.aventura.tools.vector3d.transform;

import com.aventura.tools.vector3d.vector.Vector3;

/**
 * This class is a transformation that represents a translation
 * The translation is formalized by a vector. Thus this class extends the class Vector3.
 * 
 * @author Olivier BARRY
 * @date May 2014
 */
public class Translation extends Vector3 {

	public Translation() {
		super();
	}
	
	public Translation(Vector3 v) {
		super(v);
	}
	
}
