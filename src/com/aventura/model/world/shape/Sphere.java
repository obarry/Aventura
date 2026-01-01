package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.triangle.RectangleMesh;

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
 * Normals at Vertex levels are calculated in this class while triangle normal calculation is done by the super class Elements.
 * For generic triangle normal calculation purpose based on cross product of the 2 segments P1P2 ^ P1P3  of the triangle
 * <P1 P2 P3>, it is important to create triangles so that triangle normals target the outside of the Sphere. 
 * 
 * @author Olivier BARRY
 * @since May 2016
 */
public class Sphere extends Element {
	
	protected static final String SPHERE_DEFAULT_NAME = "sphere";

	protected RectangleMesh rectangleMesh;
	protected Vector4 northPole, southPole;
	float ray;
	int half_seg;
	protected Vector4 center;
	protected Texture tex = null;
	
	public Sphere(float ray, int half_seg) {
		super(SPHERE_DEFAULT_NAME, true); // A Sphere is a closed Element
		subelements = null;
		this.ray = ray;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		this.northPole = new Vector4(0, 0, ray,  1);
		this.southPole = new Vector4(0, 0, -ray,  1);
	}

	public Sphere(float ray, int half_seg, Texture t) {
		super(SPHERE_DEFAULT_NAME, true); // A Sphere is a closed Element
		subelements = null;
		this.ray = ray;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		this.northPole = new Vector4(0, 0, ray,  1);
		this.southPole = new Vector4(0, 0, -ray,  1);
		this.tex = t;
	}

	/**
	 * Create vertices of a Sphere based on provided parameters
	 */
	public void generateVertices() {
		
		// Create mesh to wrap Cylinder
		rectangleMesh = new RectangleMesh(this, half_seg*2+1, half_seg+1, tex); // (n) x 2 vertices on each circles + 1 x 2 duplicate Vertex for RectangleMesh / Texture
		float alpha = (float)(Math.PI/half_seg);
		
		// Create Vertices
		for (int i=0; i<=half_seg*2; i++) {
			// South pole(s)-> there is as many south poles as meridians
			rectangleMesh.getVertex(i, 0).setPos(northPole);
			// Intermediate points between south and north
			for (int j=1; j<half_seg; j++) {
				float sina = (float)Math.sin(alpha*i);
				float cosa = (float)Math.cos(alpha*i);
				float sinb = (float)Math.sin(alpha*j);
				float cosb = (float)Math.cos(alpha*j);
				rectangleMesh.getVertex(i, j).setPos(new Vector4(ray*cosa*sinb, ray*sina*sinb, ray*cosb, 1));
			}
			// North pole-> there is as many north poles as meridians
			rectangleMesh.getVertex(i, half_seg).setPos(southPole);
		}
	}
	
	/**
	 * Create triangles of a Sphere based on provided parameters
	 */
	public void generateTriangles() {
		
		// Create Triangles
		rectangleMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
	}
	
	@Override
	public void calculateNormals() {
		
		// Create normals of vertices
		for (int i=0; i<=half_seg*2; i++) {
			for (int j=0; j<=half_seg; j++) {
				// For each Vertex, use the ray vector passing through the Vertex and normalize it 
				Vector4 n = rectangleMesh.getVertex(i,j).getPos().minus(center);
				n.normalize();
				rectangleMesh.getVertex(i,j).setNormal(n.V3());
			}
		}
		calculateSubNormals();
	}

}
