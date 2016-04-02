package com.aventura.model.world;

import java.util.ArrayList;

public class Element {
	
	protected ArrayList<Element> subelements; // Not necessarily used
	protected ArrayList<Triangle> triangles;  // Triangles related to this element
	protected ArrayList<Vertex> vertices;     // Vertices of this element (also referenced by the triangles)

}
