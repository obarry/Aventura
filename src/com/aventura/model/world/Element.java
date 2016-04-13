package com.aventura.model.world;

import java.util.ArrayList;

public class Element {
	
	protected ArrayList<Element> subelements; // Not necessarily used
	protected ArrayList<Triangle> triangles;  // Triangles related to this element
	protected ArrayList<Vertex> vertices;     // Vertices of this element (also referenced by the triangles)
	
	public Element() {
		super();
	}
	
//	public void addVertex(Vertex v) {
//		//TODO To be implemented
//	}
	
	public void addTriangle(Triangle t) {
		//TODO To be implemented
	}
	
	public void addTriangles(Triangle [] triangles) {
		
		for (int i=0; i<triangles.length; i++) {
			addTriangle(triangles[i]);
		}
	}

}
