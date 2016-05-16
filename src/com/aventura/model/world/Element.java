package com.aventura.model.world;

import java.util.ArrayList;

/**
 * 
 * This class is the base class representing an element of the world
 * This element is made of multiple Vertices associated in Triangles
 * The class provides facilities to create and associate Triangles and Vertices
 * 
 * This class could be derived to create more specialized elements e.g. geometry (cubes, sphere, cylinder, etc.)
 * 
 * @author Bricolage Olivier
 * @since March 2016
 *
 */
public class Element {
	
	protected ArrayList<Element> subelements; // To create a hierarchy of elements - not necessarily used
	protected ArrayList<Triangle> triangles;  // Triangles related to this element
	//protected ArrayList<Vertex> vertices;     // Vertices of this element (also referenced by the triangles)
	
	public Element() {
		super();
	}
	
//	public void addVertex(Vertex v) {
//		//TODO To be implemented
//	}
	
	public void addTriangle(Triangle t) {
		//TODO To be implemented
	}
	
	/**
	 * @param triangles
	 */
	public void addTriangles(Triangle [] triangles) {
		
		for (int i=0; i<triangles.length; i++) {
			addTriangle(triangles[i]);
		}
	}
	
	public ArrayList<Triangle> getTriangles() {
		return triangles;
	}
	
	public Triangle getTriangle(int i) {
		return triangles.get(i);
	}

}
