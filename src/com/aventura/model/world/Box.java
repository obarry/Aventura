package com.aventura.model.world;

import com.aventura.math.vector.Vector3;

public class Box extends Element {
	
	protected Vertex[][][] vertices;
	
	public Box(double lenght, double height, double width, Vector3 position) {
		super();
		subelements = null;
		vertices = new Vertex[2][2][2];
	}

}
