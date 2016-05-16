package com.aventura.model.world;

import java.util.ArrayList;

import com.aventura.math.transform.Repere;

public class World {
	
	protected Repere rep;
	
	protected ArrayList<Element> elements; // Elements connected to the world (not all Elements as some elements may also have subelements)
	
	//protected ArrayList<Vertex> vertices;    // All vertices of the World
	protected ArrayList<Triangle> triangles; // All triangles of the World

	public World() {
		elements  = new ArrayList<Element>();
		//vertices  = new ArrayList<Vertex>();
		triangles = new ArrayList<Triangle>();
	}
	
	public Element createElement() {
		Element e = new Element();
		elements.add(e);
		return e;
	}
	
	public ArrayList<Element> getElements() {
		return elements;
	}
	
	public Element getElement(int i) {
		return elements.get(i);
	}

}
