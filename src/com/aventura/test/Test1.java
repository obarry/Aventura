package com.aventura.test;

import com.aventura.math.vector.*;
import com.aventura.model.camera.Camera;
import com.aventura.model.world.*;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.Triangle;

public class Test1 {
	
	static int NB = 100;
	
	Vertex [] vertices;
	Triangle [] triangles;

	World world;
	Element element;
	
	public Test1() {
	}
	
	public void init() {
		
		vertices = new Vertex[NB];
		triangles = new Triangle[NB-2]; // Triangle strip

		// Create the World
		world = new World();
		// Add 1 single element to this World, all vertices and triangles will report to this element
		element = world.createElement();

		// Separately, create the point of gUIView: a Camera
		Camera cam = new Camera();

	}
	
	public void createElement(Element e) {
		
		// At this stage we assume we know the geometry of the element allowing to define how vertices are connected to each other
		
		
		// Calculate or initialize (e.g. from file) Vertices
		for (int i=0; i<vertices.length; i++) {
			Vector4 pos = new Vector4();
			Vertex v = new Vertex(pos);
		}
		
		// Calculate normal of each Vertex
		for (int i=0; i<vertices.length; i++) {
			Vertex [] surroundingVertices = new Vertex[3];
			vertices[i].calculateNormal(surroundingVertices);
		}
		
		
		// Create Triangle strip from Vertices
		
		// Add Triangle to the (single) element of the world		
		element.addTriangles(triangles);
		
	}

}

