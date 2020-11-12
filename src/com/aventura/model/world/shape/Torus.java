package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.RectangleMesh;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2020 Olivier BARRY
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

	protected static final String TORUS_DEFAULT_NAME = "torus";

	protected RectangleMesh rectangleMesh;
	protected Vector4[] centers; // Centers of each "ring" is used to calculate normals
	float torus_ray, pipe_ray;
	int half_circ, half_seg;
	protected Vector4 center;
	protected Texture tex = null;
	
	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 */
	public Torus(float torus_ray, float pipe_ray, int half_circ, int half_seg) {
		super(TORUS_DEFAULT_NAME, true); // A Torus is a closed Element by default
		subelements = null;
		this.torus_ray = torus_ray;
		this.pipe_ray = pipe_ray;
		this. half_circ = half_circ;
		this.half_seg = half_seg;
	}

	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 * @param t the Texture to wrap this Torus 
	 */
	public Torus(float torus_ray, float pipe_ray, int half_circ, int half_seg, Texture tex) {
		super(TORUS_DEFAULT_NAME, true); // A Torus is a closed Element by default
		subelements = null;
		this.torus_ray = torus_ray;
		this.pipe_ray = pipe_ray;
		this. half_circ = half_circ;
		this.half_seg = half_seg;
		this.tex = tex;
	}

	public void createGeometry() {
		
		// Create Mesh
		// (half_seg x 2 + 1) vertices on each circles
		// and (half_circ x 2 + 1) circles
		// + 1 (duplicate Vertices) is needed for RectangleMesh / Texture overlay
		rectangleMesh = new RectangleMesh(this, half_circ*2+1, half_seg*2+1, tex);
		
		centers = new Vector4[half_circ*2+1];
		float alpha_circ = (float)Math.PI/half_circ;
		float alpha_seg = (float)Math.PI/half_seg;
		
		// Create vertices
		for (int i=0; i<=half_circ*2; i++) { // for all circles around the Z axis (2*half_circ + 1)
			
			float sina = (float)Math.sin(alpha_circ*i);
			float cosa = (float)Math.cos(alpha_circ*i);
			// Calculate center of the circle
			centers[i] = new Vector4(torus_ray*cosa, torus_ray*sina,0,1);
			
			for (int j=0; j<=half_seg*2; j++) { // each circle is made of 2*half_seg + 1 vertices 
				
				float sinb = (float)Math.sin(alpha_seg*j);
				float cosb = (float)Math.cos(alpha_seg*j);

				// Each vertice
				rectangleMesh.getVertex(i, j).setPos(new Vector4((torus_ray+pipe_ray*cosb)*cosa, (torus_ray+pipe_ray*cosb)*sina, pipe_ray*sinb, 1));
			}
		}
		
		// Create Triangles
		rectangleMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		
	}

	@Override
	public void calculateNormals() {
		Vector4 n;
			
		// Create normals of vertices
		for (int i=0; i<=half_circ*2; i++) {
			for (int j=0; j<=half_seg*2; j++) {
				// For each bottom Vertex, use the ray vector from bottom center to the Vertex and normalize it 
				n = rectangleMesh.getVertex(i,j).getPos().minus(centers[i]);
				n.normalize();
				rectangleMesh.getVertex(i,j).setNormal(n.V3());
			}
		}
		
		calculateSubNormals();
	}
}
