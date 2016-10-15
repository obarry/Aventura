package com.aventura.model.world;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

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
 * Create a Cylinder made of 2 circles around Z axis
 * Top circle is at z = height / 2
 * Bottom circle is at z = -height / 2
 * The diameter of the cylinder is ray * 2
 * It is made of 2 * half_seg Vertices per circle and 2 * 2 * half_seg Triangles (each face of the cylinder is made of 2 triangles)
 * 
 * @author Bricolage Olivier
 * @since October 2016
 */


public class Cylinder extends Element {
	
	protected Vertex[][] vertices;
	
	/**
	 * @param height
	 * @param ray
	 * @param half_seg is half the number of segments for 360 degrees
	 */
	public Cylinder(double height, double ray, int half_seg) {
		super();
		subelements = null;
		Vector4 position = new Vector4(0,0,0,0);
		createCylinder(height, ray, half_seg, position);
	}

	/**
	 * @param height
	 * @param ray
	 * @param half_seg is half the number of segments for 360 degrees
	 * @param position
	 */
	public Cylinder(double height, double ray, int half_seg, Vector3 position) {
		super();
		subelements = null;
		Vector4 v = new Vector4(position);
		createCylinder(height, ray, half_seg, v);
	}
	
	/**
	 * @param height
	 * @param ray
	 * @param half_seg is half the number of segments for 360 degrees
	 * @param position
	 */
	public Cylinder(double height, double ray, int half_seg, Vector4 position) {
		super();
		subelements = null;
		createCylinder(height, ray, half_seg, position);
	}

	
	protected void createCylinder(double height, double ray, int half_seg, Vector4 position) {
		
		vertices = new Vertex[half_seg*2][2]; // (n) x 2 vertices on each circles
		double alpha = Math.PI/half_seg;
		
		// Create vertices
		for (int i=0; i<half_seg*2; i++) {
			
			double sina = Math.sin(alpha*i);
			double cosa = Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			vertices[i][0] = new Vertex(new Vector4(ray*cosa, ray*sina, -height/2, 1).plus(position));
			
			// Top circle of the cylinder
			vertices[i][1] = new Vertex(new Vector4(ray*cosa, ray*sina, height/2, 1).plus(position));
		}
		
		// Create Triangles
		Triangle t1, t2; // local variable
		for (int i=0; i<half_seg*2-1; i++) {
			
			// For each face of the cylinder, create 2 Triangles
			t1 = new Triangle(vertices[i][0], vertices[i+1][0], vertices[i][1]);
			t2 = new Triangle(vertices[i][1], vertices[i+1][1], vertices[i][1]);
			
			// Add triangle to the Element
			this.addTriangle(t1);			
			this.addTriangle(t2);			
		}
		// Create 2 last triangles
		t1 = new Triangle(vertices[half_seg*2-1][0], vertices[0][0], vertices[half_seg*2-1][1]);
		t2 = new Triangle(vertices[half_seg*2-1][1], vertices[0][1], vertices[half_seg*2-1][1]);
		
		// Add last triangles
		this.addTriangle(t1);			
		this.addTriangle(t2);			
	}
}
