package com.aventura.model.world;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

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
 * Create a Torus around Z axis, it is made of a "pipe" made of vertical circles around the Z axis 
 * 
 * Characteristics:
 * - ray of the torus
 * - ray of the pipe ( < ray of the torus)
 * - number of circles 
 * - number of segment per circle
 * 
 * 
 * The Torus, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix
 * 
 * @author Olivier BARRY
 * @since February 2017
 */

public class Torus extends Element {

	protected Vertex[][] vertices;
	protected Vector4[] centers;
	double torus_ray, pipe_ray;
	int half_circ, half_seg;
	protected Vector4 center;
	
	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 */
	public Torus(double torus_ray, double pipe_ray, int half_circ, int half_seg) {
		super();
		subelements = null;
		this.torus_ray = torus_ray;
		this.pipe_ray = pipe_ray;
		this. half_circ = half_circ;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		createTorus();
	}

	/**
	 * Creation of a Cylinder moved to a given center
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 * @param center to which the Vertices are moved at creation (Vector3)
	 */
	public Torus(double torus_ray, double pipe_ray, int half_circ, int half_seg, Vector3 center) {
		super();
		subelements = null;
		this.torus_ray = torus_ray;
		this.pipe_ray = pipe_ray;
		this. half_circ = half_circ;
		this.half_seg = half_seg;
		this.center = new Vector4(center);
		createTorus();
	}
	
	/**
	 * Creation of a Cylinder moved to a given center
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 * @param center to which the Vertices are moved at creation (Vector4)
	 */
	public Torus(double torus_ray, double pipe_ray, int half_circ, int half_seg, Vector4 center) {
		super();
		subelements = null;
		this.torus_ray = torus_ray;
		this.pipe_ray = pipe_ray;
		this. half_circ = half_circ;
		this.half_seg = half_seg;
		this.center = center;
		createTorus();
	}

	
	protected void createTorus() {
		
		vertices = new Vertex[half_circ*2][half_seg*2]; // half_circ*2 circles made of half_seg*2 vertices on each circle
		centers = new Vector4[half_circ*2];
		double alpha_circ = Math.PI/half_circ;
		double alpha_seg = Math.PI/half_seg;
		
		// Create vertices
		for (int i=0; i<half_circ*2; i++) { // for all circles around the Z axis (2*half_circ)
			
			double sina = Math.sin(alpha_circ*i);
			double cosa = Math.cos(alpha_circ*i);
			// Calculate center of the circle
			centers[i] = new Vector4(torus_ray*cosa, torus_ray*sina,0,1).plus(center);
			
			for (int j=0; j<half_seg*2; j++) { // each circle is made of 2*half_seg vertices 

				
				double sinb = Math.sin(alpha_seg*j);
				double cosb = Math.cos(alpha_seg*j);


				// Each vertice
				vertices[i][j] = new Vertex(new Vector4((torus_ray+pipe_ray*cosb)*cosa, (torus_ray+pipe_ray*cosb)*sina, pipe_ray*sinb, 1).plus(center));
			}
		}
		
		// Create Triangles
		Triangle t1, t2; // local variable
		for (int i=0; i<half_circ*2-1; i++) {
			for (int j=0; j<half_seg*2-1; j++) {

				// For each face of the cylinder, create 2 Triangles
				t1 = new Triangle(vertices[i][j], vertices[i+1][j], vertices[i][j+1]);
				t2 = new Triangle(vertices[i][j+1], vertices[i+1][j], vertices[i+1][j+1]);

				// Add triangle to the Element
				this.addTriangle(t1);			
				this.addTriangle(t2);
			}
			// Create 2 last triangles
			t1 = new Triangle(vertices[i][half_seg*2-1], vertices[i+1][half_seg*2-1], vertices[i][0]);
			t2 = new Triangle(vertices[i][0], vertices[i+1][half_seg*2-1], vertices[i+1][0]);
			// Add triangle to the Element
			this.addTriangle(t1);			
			this.addTriangle(t2);
		}
		// Create last belt
		for (int j=0; j<half_seg*2-1; j++) {

			// For each face of the cylinder, create 2 Triangles
			t1 = new Triangle(vertices[half_circ*2-1][j], vertices[0][j], vertices[half_circ*2-1][j+1]);
			t2 = new Triangle(vertices[half_circ*2-1][j+1], vertices[0][j], vertices[0][j+1]);

			// Add triangle to the Element
			this.addTriangle(t1);			
			this.addTriangle(t2);
		}
		// Create 2 last triangles of the last belt
		t1 = new Triangle(vertices[half_circ*2-1][half_seg*2-1], vertices[0][half_seg*2-1], vertices[half_circ*2-1][0]);
		t2 = new Triangle(vertices[half_circ*2-1][0], vertices[0][half_seg*2-1], vertices[0][0]);
		
		// Add last triangles
		this.addTriangle(t1);			
		this.addTriangle(t2);			
	}

	@Override
	public void calculateNormals() {
		Vector4 n;
			
		// Create normals of vertices
		for (int i=0; i<half_circ*2; i++) {
			for (int j=0; j<half_seg*2; j++) {
				// For each bottom Vertex, use the ray vector from bottom center to the Vertex and normalize it 
				n = vertices[i][j].getPos().minus(centers[i]);
				n.normalize();
				vertices[i][j].setNormal(n.V3());
			}
		}
		
		calculateSubNormals();
	}
}
