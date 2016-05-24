package com.aventura.model.world;

public class Triangle {
	
	// Made of 3 vertices
	Vertex v1;
	Vertex v2;
	Vertex v3;
	
	public Triangle(NVertex v1, NVertex v2, NVertex v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}

	public Triangle(Vertex v1, Vertex v2, Vertex v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}


}
