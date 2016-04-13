package com.aventura.test;

import com.aventura.model.camera.Camera;
import com.aventura.model.world.*;

public class Test1 {
	
	Vertex [] vertices;
	Triangle [] triangles;

	World world;
	Element element;
	
	public Test1() {
	}
	
	public void init() {
		
		static int NB = 100;

		vertices = new Vertex[NB];
		triangles = new Triangle[NB-2]; // Triangle strip

		world = new World();
		element = world.createElement();

		Camera cam = new Camera();

	}
	
	public void createElement() {
		
		// Calculate or initialize (e.g. from file) Vertices
		
		// Create Triangle strip from Vertex
		
		// Add Triangle to the (single) element of the world		
		element.addTriangles(triangles);
		
	}

}

