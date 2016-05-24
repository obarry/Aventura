package com.aventura.model.world;

import com.aventura.math.vector.Vector3;
import com.aventura.model.world.Triangle;

/**
 * A triangle with a Normal Vector that should be the same for all the triangle surface (no interpolation)
 * Usefull for a plane surface like a solid (box, cube, etc.)
 * 
 * @author Bricolage Olivier
 * @since May 2016
 *
 */
public class NTriangle extends Triangle {
	
	protected Vector3 normal;
	
	public NTriangle(Vertex v1, Vertex v2, Vertex v3, Vector3 normal) {
		super(v1, v2, v3);
		this.normal = normal;
	}



}
