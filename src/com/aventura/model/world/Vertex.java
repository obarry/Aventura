package com.aventura.model.world;

import java.awt.Color;
import com.aventura.math.vector.*;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016 Olivier BARRY
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
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Vertex {
	
	// Geometry
	protected Vector4 position = null;
	protected Vector3 normal = null;
	
	// Physical characteristic
	protected Vector2 texture = null; //relative position of this vertex in the texture plane
	protected Color color = null; // base color of this vertex
	protected int material; // to be defined, a specific class/object may be needed
	
	// Reflectivity
	// TBD
	
	public Vertex() {
		
	}
	
	public Vertex(double x, double y, double z) {
		position = new Vector4(x, y, z, 1);
	}
	
	public Vertex(Vector4 p) {
		position = p;
		normal = null;
	}
	
	public Vertex(Vector4 p, Vector3 n) {
		position = p;
		normal = n;
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
