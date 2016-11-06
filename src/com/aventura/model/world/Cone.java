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

 * Create a Cone made of 1 summit and a base, a circle around Z axis
 * Summit is at z = height / 2
 * Circle is at z = -height / 2
 * The diameter of the Circle is ray * 2
 * It is made of 2 * half_seg Vertices for the circle and the same number of Triangles linked with summit
 * 
 * The Cone, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix

 * @author Bricolage Olivier
 * @since October 2016
 */


public class Cone extends Element {

	protected Vertex[] vertices;
	protected Vertex summit;
	
	/**
	 * Default creation of a Cone around Z axis 
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 */
	public Cone(double height, double ray, int half_seg) {
		super();
		subelements = null;
		Vector4 position = new Vector4(0,0,0,0);
		createCone(height, ray, half_seg, position);
	}

	/**
	 * Creation of a Cone moved to a given position
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 * @param position to which the Vertices are moved at creation (Vector3)
	 */
	public Cone(double height, double ray, int half_seg, Vector3 position) {
		super();
		subelements = null;
		Vector4 v = new Vector4(position);
		createCone(height, ray, half_seg, v);
	}
	
	/**
	 * Creation of a Cone moved to a given position
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 * @param position to which the Vertices are moved at creation (Vector4)
	 */
	public Cone(double height, double ray, int half_seg, Vector4 position) {
		super();
		subelements = null;
		createCone(height, ray, half_seg, position);
	}

	
	protected void createCone(double height, double ray, int half_seg, Vector4 position) {
		
		vertices = new Vertex[half_seg*2]; // (n) vertices on each circles
		double alpha = Math.PI/half_seg;
		
		// Create vertices
		summit = new Vertex(new Vector4(0, 0, height/2,  1));
		
		for (int i=0; i<half_seg*2; i++) {
			
			double sina = Math.sin(alpha*i);
			double cosa = Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			vertices[i] = new Vertex(new Vector4(ray*cosa, ray*sina, -height/2, 1).plus(position));
			
		}
		
		// Create Triangles
		Triangle t; // local variable
		for (int i=0; i<half_seg*2-1; i++) {
			
			// For each face of the cylinder, create 2 Triangles
			t = new Triangle(summit, vertices[i], vertices[i+1]);
			
			// Add triangle to the Element
			this.addTriangle(t);			
		}
		// Create last triangle to close the Cone
		t = new Triangle(summit, vertices[half_seg*2-1], vertices[0]);
		
		// Add last triangles
		this.addTriangle(t);			
			
	}

	@Override
	public void calculateNormals() {
		// TODO Auto-generated method stub
		
	}
}
