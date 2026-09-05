package com.aventura.model.world;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.math.transform.Transformation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2026 Olivier BARRY
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

	/**
	 * Static construction of all Elements.
	 * Does not calculate the World projection of each vertex
	 */
	public void build() {
		
		for (int i=0; i<elements.size(); i++) {
				elements.get(i).build();
		}
	}
	
	/**
	 * Static re-construction of all Elements.
	 * Does not re-calculate the World projection of each vertex
	 */
	public void rebuild() {
		
		for (int i=0; i<elements.size(); i++) {
				elements.get(i).rebuild();
		}
	}
	
	/**
	 * Calculate the World projection for each vertex of each Element of the World
	 */
	public void worldProject() {

		for (int i=0; i<elements.size(); i++) {
			elements.get(i).transform();
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
	
	public void setTransformation(Transformation t) {
		for (int i=0; i<elements.size(); i++) {
			elements.get(i).setTransformation(t);
		}
	}

	public void expandTransformation(Transformation t) {
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
	 * @return the max distance from origin of all vertices in the World (including sub-Elements
	 *         recursively -- see the fix note on the (Vector4) overload below)
	 */
	public float getMaxDistance() {
		return getMaxDistance(Vector4.ZERO_POINT);
	}
	
	/**
	 * @return the max distance from Point p of all vertices in the World
	 *
	 * BUGFIX: this used to iterate only elements.get(i).vertices directly, missing every
	 * sub-Element's vertices entirely (Element.getSubElements() was never consulted) -- silently
	 * under-reporting the World's true extent for any scene with an Element hierarchy. Now
	 * delegates to Element.accumulateMaxDistance(), which does recurse.
	 */
	public float getMaxDistance(Vector4 p) {
		if (p.isVector()) p.point();
		float[] runningMax = { 0f };
		for (int i=0; i<elements.size(); i++) {
			elements.get(i).accumulateMaxDistance(p, runningMax);
		}
		return runningMax[0];
	}

	/**
	 * The min and max world-space (x, y, z) corners of an axis-aligned box containing every
	 * vertex of every Element in this World, INCLUDING sub-Elements recursively, in WORLD space
	 * (post full transformation -- requires worldProject() to have been called at least once).
	 *
	 * Added for e.g. sizing a directional light's shadow box: unlike getMaxX()/getMaxY()/
	 * getMaxZ()/getMinX()/getMinY()/getMinZ() below (which report LOCAL extent and don't recurse
	 * into sub-Elements -- kept as-is since other callers may rely on that), this is a genuine
	 * scene-wide, world-space bound. The two points returned can be fed directly into
	 * BoundingBox4's array constructor (it only needs the extremes of the point set).
	 *
	 * @return a 2-element array: [0] = min corner, [1] = max corner (homogeneous, w = 1)
	 */
	public Vector4[] getWorldBounds() {
		float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		for (int i=0; i<elements.size(); i++) {
			elements.get(i).accumulateWorldBounds(min, max);
		}
		return new Vector4[] {
				new Vector4(min[0], min[1], min[2], 1),
				new Vector4(max[0], max[1], max[2], 1)
		};
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