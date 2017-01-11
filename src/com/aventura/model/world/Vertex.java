package com.aventura.model.world;

import java.awt.Color;
import com.aventura.math.vector.*;

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
 * A generic Vertex
 * 
 * @author Olivier BARRY
 * @since May 2016
 */
public class Vertex {
	
	// Geometry
	protected Vector4 position = null; // Coordinates of the Vertex. Vector4 as this is a Point (not vector only) in space.
	protected Vector3 normal = null; // Normal of this Vertex, this is context specific and can be kept null if normal at Triangle level
	
	// Physical characteristic
	protected Vector2 texture = null; // Relative position of this Vertex in the texture plane
	protected Color color = null; // color of this Vertex, if null the Element's color (or World's color) is used. Lowest level priority.
	protected int material; // To be defined, a specific class may be needed for a complex material representation
	
	// Reflectivity
	// TODO
		
	/**
	 * Duplicate a Vertex, creating new Vectors for position and normal but keeping references for texture and color
	 * @param v the Vertex to duplicate
	 */
	public Vertex(Vertex v) {
		this.position = (v.position != null) ? new Vector4(v.position) : null;
		this.normal = (v.normal != null) ? new Vector3(v.normal) : null;
		this.texture = v.texture;
		this.color = v.getColor();
		this.material = v.material;
	}
	
	
	public Vertex(double x, double y, double z) {
		position = new Vector4(x, y, z, 1);
	}
		
	public Vertex(Vector4 p) {
		position = p;
		normal = null;
	}
		
	public Vertex(Vector3 p) {
		position = new Vector4(p.getX(), p.getY(), p.getZ(), 1);
		normal = null;
	}
	
	public Vertex(Vector4 p, Vector3 n) {
		position = p;
		normal = n;
	}

	public Vertex(Vector4 p, Vector4 n) {
		position = p;
		normal = n.getVector3();
	}

	public String toString() {
		return "Position: "+position;
	}
	
	public void setPosition(Vector4 p) {
		position = p;
	}
	
	public Vector4 getPosition() {
		return position;
	}
	
	public Vector3 getNormal() {
		return normal;
	}
	
	public Vector4 getNormalV4() {
		return new Vector4(normal);
	}
	
	public void setColor(Color c) {
		this.color = c;
	}
	
	public Color getColor() {
		return color;
	}
	
	/**
	 * Calculate the normal from a set of vertices surrounding this Vertex
	 * @param setOfVertices
	 */
	public void calculateNormal(Vertex[] setOfVertices) {
	
		// Use the position of the other Vertices relative to this Vertex to calculate an average plan and define the normal
		for (int i=0; i<setOfVertices.length; i++) {
			// TODO
		}
	}

	public void setNormal(Vector3 n) {
		normal = n;
	}

}
