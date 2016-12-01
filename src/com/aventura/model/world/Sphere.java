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
 *  Vertices: +
 *  Segments: / \ | -
 *  
 *  Example;
 *  Number of segments: 12
 *  -> 12 segments around the sphere horizontally (360 degrees) -> 12 vertices (only, not 13!)
 *  -> 6 segments from north to south (180 degrees) -> north pole, 5 vertices, south pole (n+1)
 *  -> table of 12 x 5 vertices + 2 poles
 * 
 *               +					North Pole
 *    / / / / / /|\ \ \ \ \            
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | |
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | |
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | |
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | | 
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *    \ \ \ \ \ \|/ / / / / 
 *               +					South Pole
 * 
 *   
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Sphere extends Element {
	
	protected Vertex[][] vertices;
	protected Vertex northPole, southPole;
	double ray;
	int half_seg;
	protected Vector4 center;
	
	public Sphere(double ray, int half_seg) {
		super();
		subelements = null;
		this.ray = ray;
		this.half_seg = half_seg;
		center = new Vector4(0,0,0,0);
		createSphere();
	}

	/**
	 * @param ray
	 * @param half_seg is half the number of segments for 360 degrees
	 * @param center
	 */
	public Sphere(double ray, int half_seg, Vector3 center) {
		super();
		subelements = null;
		this.ray = ray;
		this.half_seg = half_seg;
		this.center = center.getVector4();
		createSphere();
	}
	
	/**
	 * @param ray
	 * @param half_seg is half the number of segments for 360 degrees
	 * @param center
	 */
	public Sphere(double ray, int half_seg, Vector4 center) {
		super();
		subelements = null;
		this.ray = ray;
		this.half_seg = half_seg;
		this.center = center;
		createSphere();
	}
	
	/**
	 * Create vertices and triangles of a Sphere based on provided parameters
	 * @param ray the ray of the Sphere
	 * @param half_seg is half the number of segments for 360 degrees
	 * @param center to translate the Sphere vertices by the Vector4 center
	 */
	protected void createSphere() {
		
		vertices = new Vertex[half_seg*2][half_seg-1]; // (n) x (n/2-1) vertices (n being the number of segments)	
		double alpha = Math.PI/half_seg;
		
		// Create Vertices
		northPole = new Vertex(new Vector4(0, 0, ray,  1));
		southPole = new Vertex(new Vector4(0, 0, -ray,  1));
		
		for (int i=0; i<half_seg*2; i++) {
			for (int j=0; j<(half_seg-1); j++) {
				double sina = Math.sin(alpha*i);
				double cosa = Math.cos(alpha*i);
				double sinb = Math.sin(alpha*(j+1));
				double cosb = Math.cos(alpha*(j+1));
				vertices[i][j] = new Vertex(new Vector4(ray*cosa*sinb, ray*sina*sinb, ray*cosb, 1).plus(center));
			}
		}

		// Create Triangles
		// 2 triangles per "square" face, 1 triangle for each face on the north and south poles
		Triangle t; // local variable
		
		// North pole to first meridian - "triangle" faces
		for (int i=0; i<half_seg*2-1; i++) {
			t = new Triangle(northPole, vertices[i+1][0], vertices[i][0]);
			this.addTriangle(t);
		}
		t = new Triangle(northPole, vertices[0][0], vertices[half_seg*2-1][0]);
		this.addTriangle(t);
		
		// Meridian to meridian - "square" faces
		for (int j=0; j<half_seg-2; j++) {
			for (int i=0; i<half_seg*2-1; i++) {
				t = new Triangle(vertices[i][j], vertices[i+1][j], vertices[i][j+1]);
				this.addTriangle(t);			
				t = new Triangle(vertices[i][j+1], vertices[i+1][j], vertices[i+1][j+1]);
				this.addTriangle(t);			
			}
			t = new Triangle(vertices[half_seg*2-1][j], vertices[0][j], vertices[half_seg*2-1][j+1]);
			this.addTriangle(t);			
			t = new Triangle(vertices[half_seg*2-1][j+1], vertices[0][j], vertices[0][j+1]);
			this.addTriangle(t);			
		}
				
		// South pole to last meridian - "triangle" faces
		for (int i=0; i<half_seg*2-1; i++) {
			t = new Triangle(southPole, vertices[i][half_seg-2], vertices[i+1][half_seg-2]);
			this.addTriangle(t);			
		}
		t = new Triangle(southPole, vertices[half_seg*2-1][half_seg-2], vertices[0][half_seg-2]);
		this.addTriangle(t);		
	}

	@Override
	public void calculateNormals() {
		
		// Create normals of poles
		northPole.setNormal(Vector3.Z_AXIS);
		southPole.setNormal(Vector3.Z_OPP_AXIS);
		
		// Create normals of vertices
		for (int i=0; i<half_seg*2; i++) {
			for (int j=0; j<(half_seg-1); j++) {
				// For each Vertex, use the ray vector passing through the Vertex and normalize it 
				Vector4 n = vertices[i][j].getPosition().minus(center);
				n.normalize();
				vertices[i][j].setNormal(n.getVector3());
			}
		}	
	}

}
