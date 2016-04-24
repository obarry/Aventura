package com.aventura.model.world;

import java.awt.Color;
import com.aventura.math.vector.*;

public class Vertex {
	
	// Geometry
	protected Vector4 position;
	protected Vector3 normal;
	
	// Physical characteristic
	protected Vector2 texture; //relative position of this vertex in the texture plane
	protected Color color; // base color of this vertex
	protected int material; // to be defined, a specific class/object may be needed
	
	// Reflectivity
	// TBD
	
	public Vertex(Vector4 p) {
		position = p;
		normal = null;
	}
	
	public Vertex(Vector4 p, Vector3 n) {
		position = p;
		normal = n;
	}
	
	public void setPosition(Vector4 p) {
		position = p;
	}
	
	public Vector4 getPosition() {
		return position;
	}
	
	public void setNormal(Vector3 n) {
		normal = n;
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
	}

}
