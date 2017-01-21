package com.aventura.model.world;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.math.transform.Repere;
import com.aventura.math.vector.Matrix4;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * @author Olivier BARRY
 * @since May 2016
 */
public class World {
	
	protected Repere rep;
	
	protected ArrayList<Element> elements; // Elements connected to the world (not all Elements as some elements may also have subelements)
	
	//protected ArrayList<Vertex> vertices;    // All vertices of the World
	protected ArrayList<Triangle> triangles; // All triangles of the World
	
	// Color Management
	protected Color backgroundColor = Color.BLACK; // Color of the background ("sky")
	protected Color worldColor = Color.WHITE; // Color of the world's elements unless specified at Element or Vertex level (lowest level priority)

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
	
	public void addElement(Element e) {
		elements.add(e);
	}
	
	public ArrayList<Element> getElements() {
		return elements;
	}
	
	public Element getElement(int i) {
		return elements.get(i);
	}

	public void calculateNormals() {
		
		for (int i=0; i<elements.size(); i++) {
				elements.get(i).calculateNormals();
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
			elements.get(i).expandTransformation(t);
		}
	}

}
