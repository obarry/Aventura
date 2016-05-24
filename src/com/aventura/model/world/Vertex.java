package com.aventura.model.world;

import java.awt.Color;
import com.aventura.math.vector.*;

/**
 * A generic Vertex without Normal Vector
 * Normally only used for triangles having a specific normal vector or for generic handling purpose
 * Else the general case is to use NVertex inherited class
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Vertex {
	
	// Geometry
	protected Vector4 position;
	
	// Physical characteristic
	protected Vector2 texture; //relative position of this vertex in the texture plane
	protected Color color; // base color of this vertex
	protected int material; // to be defined, a specific class/object may be needed
	
	// Reflectivity
	// TBD
	
	public Vertex(Vector4 p) {
		position = p;
		//normal = null;
	}
	
	public Vertex(Vector4 p, Vector3 n) {
		position = p;
		//normal = n;
	}
	
	public void setPosition(Vector4 p) {
		position = p;
	}
	
	public Vector4 getPosition() {
		return position;
	}
	
	

}
