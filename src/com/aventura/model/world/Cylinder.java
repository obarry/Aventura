package com.aventura.model.world;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;

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
 * Create a Cylinder made of 2 circles around Z axis
 * Top circle is at z = height / 2
 * Bottom circle is at z = -height / 2
 * The diameter of the cylinder is ray * 2
 * It is made of 2 * half_seg Vertices per circle and 2 * 2 * half_seg Triangles (each face of the cylinder is made of 2 triangles)
 * 
 * The Cylinder, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix
 * 
 * @author Olivier BARRY
 * @since October 2016
 */


public class Cylinder extends Element {
	
	protected Mesh mesh;
	double height;
	double ray;
	int half_seg;
	protected Vector4 center, top_center, bottom_center;
	
	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 */
	public Cylinder(double height, double ray, int half_seg) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.top_center = new Vector4(0,0,height/2,0);
		this.bottom_center = new Vector4(0,0,-height/2,0);
		createCylinder(null);
	}

	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 */
	public Cylinder(double height, double ray, int half_seg, Texture t) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.top_center = new Vector4(0,0,height/2,0);
		this.bottom_center = new Vector4(0,0,-height/2,0);
		createCylinder(t);
	}
	protected void createCylinder(Texture t) {
		
		mesh = new Mesh(this, half_seg*2+1, 2, t); // (n) x 2 vertices on each circles + 1 x 2 duplicate Vertex for Mesh / Texture
		double alpha = Math.PI/half_seg;
		
		// Create vertices
		for (int i=0; i<=half_seg*2; i++) { // (n) * 2 + 1 steps -> [0, 2*PI]
			
			double sina = Math.sin(alpha*i);
			double cosa = Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			mesh.getVertex(i, 0).setPos(new Vector4(ray*cosa, ray*sina, -height/2, 1));
			
			// Top circle of the cylinder
			mesh.getVertex(i,1).setPos(new Vector4(ray*cosa, ray*sina, height/2, 1));
		}
		
		// Create Triangles
		mesh.createTriangles(Mesh.MESH_ORIENTED_TRIANGLES);
	}

	@Override
	public void calculateNormals() {
		Vector4 n;
			
		// Create normals of vertices
		for (int i=0; i<=half_seg*2; i++) {
			// For each bottom Vertex, use the ray vector from bottom center to the Vertex and normalize it 
			n = mesh.getVertex(i,0).getPos().minus(bottom_center);
			n.normalize();
			mesh.getVertex(i,0).setNormal(n.V3());
			// Same normal vector can be used for the corresponding top Vertex
			mesh.getVertex(i,1).setNormal(n.V3());
		}
		
		calculateSubNormals();
	}
}
