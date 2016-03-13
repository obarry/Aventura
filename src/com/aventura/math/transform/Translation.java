package com.aventura.math.transform;

import com.aventura.math.vector.Vector3;

/**
 * This class is a transformation that represents a translation
 * The translation is formalized by a vector. Thus this class extends the class Vector3.
 * 
 * @author Olivier BARRY
 * @date May 2014
 */
public class Translation extends Vector3 {
	
	public Translation(Vector3 v) {
		super(v);
	}
	
	public Translation plus(Translation t) {
		return new Translation((Vector3)this.plus((Vector3)t));
	}
	
}
