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

 * Create a Cone made of 1 summit and a base, a circle around Z axis
 * Summit is at z = height / 2
 * Circle is at z = -height / 2
 * The diameter of the Circle is ray * 2
 * It is made of 2 * half_seg Vertices for the circle and the same number of Triangles linked with summit
 * 
 * The Cone, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix

 * @author Olivier BARRY
 * @since January 2017
 */


public class ConeSummit extends Element {

	protected Vertex[] vertices;
	protected Vertex summit; // In order to have different Vertex based normals, we need to have independent summit for each triangle
	double height;
	double ray;
	int half_seg;
	protected Vector4 center, bottom_center;
	
	/**
	 * Default creation of a Cone around Z axis 
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 */
	public ConeSummit(double height, double ray, int half_seg) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		this.bottom_center = new Vector4(0,0,-height/2,0);
		this.center = new Vector4(0,0,0,0);
		createCone();
	}

	/**
	 * Creation of a Cone moved to a given position
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 * @param center to which the Cone is moved at creation (Vector3)
	 */
	public ConeSummit(double height, double ray, int half_seg, Vector3 center) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.bottom_center = new Vector4(0,0,-height/2,0);
		this.center = new Vector4(center);
		createCone();
	}
	
	/**
	 * Creation of a Cone moved to a given position
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 * @param center to which the Cone is moved at creation (Vector4)
	 */
	public ConeSummit(double height, double ray, int half_seg, Vector4 center) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.bottom_center = new Vector4(0,0,-height/2,0);
		this.center = center;
		createCone();
	}

	
	protected void createCone() {
		
		vertices = new Vertex[half_seg*2]; // (n) vertices on each circles
		double alpha = Math.PI/half_seg;
		
		// Create vertices
		
		// Create summits (same Vertex for all summits)
		summit = createVertex(new Vector4(0, 0, height/2,  1).plus(center));
		
		// Create bottom vertices
		for (int i=0; i<half_seg*2; i++) {
			
			double sina = Math.sin(alpha*i);
			double cosa = Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			vertices[i] = createVertex(new Vector4(ray*cosa, ray*sina, -height/2, 1).plus(center));
			
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
		
		// Add last triangle
		this.addTriangle(t);			
			
	}
	
	@Override
	public void calculateNormals() {
		Vector4 n, u;
		
		// Create normal of summit
		n = new Vector4(Vector4.Z_AXIS);
		summit.setNormal(n.V3());
			
		// Create normals of vertices
		for (int i=0; i<half_seg*2; i++) {

			// For each bottom Vertex, calculate a ray vector that is orthogonal to the slope of the cone
			// u = OS^OP (O = bottom center, S = summit, P = bottom Vertex)
			u = (summit.getPos().minus(bottom_center)).times(vertices[i].getPos().minus(bottom_center));
			n = (vertices[i].getPos().minus(summit.getPos())).times(u);
			n.normalize();
			vertices[i].setNormal(n.V3());
		}
		calculateSubNormals();
	}
}
