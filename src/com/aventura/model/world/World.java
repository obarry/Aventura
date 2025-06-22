package com.aventura.model.world;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------ 
 * 
 * World is the root class for all the hierarchy of Elements containing world geometry
 * World can only contain Elements that contain Vertices and Triangles
 * Elments can contain other Elements recursively, creating a tree where World is the root
 * 
 * Other attributes of World are related to shared characteristics of this world.
 * Nothing should prevent creating several different worlds.
 * 
 * @author Olivier BARRY
 * @since May 2016
 */
public class World {
	
	protected String name;
	
	protected ArrayList<Element> elements; // Elements connected to the world (not all Elements as some elements may also have subelements)
		
	// Color Management
	protected Color backgroundColor = Color.BLACK; // Color of the background ("sky")
	protected Color worldColor = Color.WHITE; // Color of the world's elements unless specified at Element or Vertex level (lowest level priority)

	public World() {
		this.name = "world";
		this.elements  = new ArrayList<Element>();
	}
	
	public World(String name) {
		this.name = name;
		this.elements  = new ArrayList<Element>();
	}
	
	public Element createElement() {
		Element e = new Element();
		this.elements.add(e);
		return e;
	}
	
	public void addElement(Element e) {
		this.elements.add(e);
	}
	
	public ArrayList<Element> getElements() {
		return elements;
	}
	
	public Element getElement(int i) {
		return elements.get(i);
	}

	public void generate() {
		
		for (int i=0; i<elements.size(); i++) {
				elements.get(i).generate();
		}
	}
	
	public void update() {
		
		for (int i=0; i<elements.size(); i++) {
				elements.get(i).update();
		}
	}
	
	public void setColor(Color c) {
		this.worldColor = c;
	}
	
	public Color getColor() {
		return worldColor;
	}

	public void setBackgroundColor(Color c) {
		this.backgroundColor = c;
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	public void setTransformation(Matrix4 t) {
		for (int i=0; i<elements.size(); i++) {
			elements.get(i).setTransformation(t);
		}
	}

	public void expandTransformation(Matrix4 t) {
		for (int i=0; i<elements.size(); i++) {
			elements.get(i).combineTransformation(t);
		}
	}
	
	public int getNbElements() {
		return elements.size();
	}
	
	public int getNbElements(Element e) {
		int nb = 0;
		
			// Do a recursive call for SubElements
			if (!e.isLeaf()) {
				for (int i=0; i<e.getSubElements().size(); i++) {
					// Recursive call
					nb+=getNbElements(e.getSubElements().get(i));
				}
			}
//			} else { // Leaf
//				nb+=1;			
//			}
		return nb;
	}
	
	int getNbAllElements() {
		int nb =0;
		
		for (int i=0; i<elements.size(); i++) {
			nb++; // the Element of the list
			nb+=getNbElements(getElement(i)); // the SubElements of the Element of the list (all SubElement recursively)
		}
		return nb;
	}
	
	public int getNbTriangles() {
		int nb = 0;
		for (int i=0; i<elements.size(); i++) {
			nb += elements.get(i).getNbTriangles();
		}
		return nb;
	}

	public int getNbTriangles(Element e) {
		int nb = 0;

		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				nb += getNbTriangles(e.getSubElements().get(i));
			}
		}
//		} else { // Leaf
//			nb+=e.getNbTriangles();			
//		}
		return nb;
	}

	int getNbAllTriangles() {
		int nb =0;
		nb+=getNbTriangles(); // Triangles of the Element of the list
		for (int i=0; i<elements.size(); i++) {
			nb += getNbTriangles(getElement(i)); // Triangles of the SubElements of the Element of the list (all SubElement recursively)
		}
		return nb;
	}

	
	public int getNbVertices() {
		int nb = 0;
		for (int i=0; i<elements.size(); i++) {
			nb += elements.get(i).getNbVertices();
		}
		return nb;
	}
	
	public int getNbVertices(Element e) {
		int nb = 0;

		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				nb += getNbVertices(e.getSubElements().get(i));
			}
		}
//		} else { // Leaf
//			nb+=e.getNbVertices();			
//		}
		return nb;
	}
	
	public int getNbAllVertices() {
		int nb =0;
		nb+=getNbVertices(); // Vertices of the Element of the list
		
		for (int i=0; i<elements.size(); i++) {
			nb+=getNbVertices(getElement(i)); // Vertices of the SubElements of the Element of the list (all SubElement recursively)
		}
		return nb;
	}
	
	/**
	 * @return the max distance from origin of all vertices in the World
	 */
	public float getMaxDistance() {
		float max = 0;
		float dist = 0;
		for (int i=0; i<elements.size(); i++) {
			for (int j=0; j<elements.get(i).vertices.size(); j++) {
				dist = elements.get(i).vertices.get(j).position.length();
				max = dist > max ? dist : max;
			}
		}
		return max;
	}
	
	/**
	 * @return the max distance from Point p of all vertices  in the World
	 */
	public float getMaxDistance(Vector4 p) {
		float max = 0;
		float dist = 0;
		if (p.isVector()) p.point();
		for (int i=0; i<elements.size(); i++) {
			for (int j=0; j<elements.get(i).vertices.size(); j++) {
				dist = elements.get(i).vertices.get(j).position.minus(p).length();
				max = dist > max ? dist : max;
			}
		}
		return max;
	}

	
	public float getMaxX() {
		float max = elements.get(0).getMaxX();
		for (int i=1; i<elements.size(); i++) {
			float newmax = elements.get(i).getMaxX();
			if (newmax > max) max = newmax;
		}
		return max;
	}
	
	public float getMaxY() {
		float max = elements.get(0).getMaxY();
		for (int i=1; i<elements.size(); i++) {
			float newmax = elements.get(i).getMaxY();
			if (newmax > max) max = newmax;
		}
		return max;
	}
	
	public float getMaxZ() {
		float max = elements.get(0).getMaxZ();
		for (int i=1; i<elements.size(); i++) {
			float newmax = elements.get(i).getMaxZ();
			if (newmax > max) max = newmax;
		}
		return max;
	}
	
	public float getMinX() {
		float min = elements.get(0).getMinX();
		for (int i=1; i<elements.size(); i++) {
			float newmin = elements.get(i).getMinX();
			if (newmin < min) min = newmin;
		}
		return min;
	}
	
	public float getMinY() {
		float min = elements.get(0).getMinY();
		for (int i=1; i<elements.size(); i++) {
			float newmin = elements.get(i).getMinY();
			if (newmin < min) min = newmin;
		}
		return min;
	}
	
	public float getMinZ() {
		float min = elements.get(0).getMinZ();
		for (int i=1; i<elements.size(); i++) {
			float newmin = elements.get(i).getMinZ();
			if (newmin < min) min = newmin;
		}
		return min;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		String world = "World\n";
		world += "* Name: "+name + "\n";
		world += "* Direct Elements: " + getNbElements() + "\n";
		world += "* All Elements: " + getNbAllElements() + "\n";
		world += "* Direct Element's Triangles: " + getNbTriangles() + "\n";
		world += "* All Triangles: " + getNbAllTriangles() + "\n";
		world += "* Direct Element's Vertices: " + getNbVertices() + "\n";
		world += "* All Vertices: " + getNbAllVertices() + "\n";
		world += "* Background color: " + getBackgroundColor() + "\n";
		world += "* World color: " + getColor() + "\n";
		return world;
		//return "World name: "+name+"\nElements: "+getNbElements()+", Triangles: "+getNbTriangles()+", Vertices: "+getNbVertices()+"\nBackground color: "+backgroundColor+"\nWorld color: "+worldColor;		
	}

}
