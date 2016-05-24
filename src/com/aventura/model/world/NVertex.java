package com.aventura.model.world;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * A Vertex with a Normal Vector - this is the general case
 * Generally associated with a Triangle that has no specific normal vector
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class NVertex extends Vertex {
	
	protected Vector3 normal;
	
	public NVertex(Vector4 p, Vector3 normal) {
		super(p);
		this.normal = normal;
	}

	public Vector3 getNormal() {
		return normal;
	}
	
	/**
	 * Calculate the normal from a set of vertices surrounding this Vertex
	 * @param setOfVertices
	 */
	public void calculateNormal(Vertex[] setOfVertices) {
	
		// Use the position of the other Vertices relative to this Vertex to calculate an average plan and define the normal
		for (int i=0; i<setOfVertices.length; i++) {
			// TODO
		}
	}

	public void setNormal(Vector3 n) {
		normal = n;
	}

}
