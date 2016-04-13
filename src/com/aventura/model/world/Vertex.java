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
	

}
