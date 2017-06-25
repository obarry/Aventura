package com.aventura.model.world.triangle;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;

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
 * A rectangle Mesh of vertices with same Texture to create a surface part of an Element
 * This class creates properly the array of Vertices and proposes services to create the list of Triangle in different ways to rectangleMesh the surface.
 * This class does not generate the geometry of the surface, this is the user of this class who needs to set the position of each Vertex in the space.
 * 
 * @author Olivier BARRY
 * @since June 2017
 */
public class RectangleMesh extends Mesh {
	
	// Various type of triangle rectangleMesh for the vertices
	
	//  +---+---+---+---+
	//  | / | / | / | / |
	//  +---+---+---+---+
	//  | / | / | / | / |
	//  +---+---+---+---+
	public static final int MESH_ORIENTED_TRIANGLES = 1;

	//  +---+---+---+---+
	//  | \ | / | \ | / |
	//  +---+---+---+---+
	//  | / | \ | / | \ |
	//  +---+---+---+---+
	public static final int MESH_ALTERNATE_TRIANGLES = 2;
	
	// Number of Vertices on x and y of the rectangleMesh
	int nbv_x, nbv_y = 0;
	Vertex[][] vertices = null; // Table of vertices

	public RectangleMesh(Element e, int n, int p) {
		super(e);
		this.nbv_x = n;
		this.nbv_y = p;
		vertices = elm.createVertexMesh(n, p);
	}
	
	public RectangleMesh(Element e, int n, int p, Texture t) {
		super(e);
		this.nbv_x = n;
		this.nbv_y = p;
		this.tex = t;
		vertices = elm.createVertexMesh(n, p);
	}
	
	public void createTriangles(int type) {
		
		Triangle t1, t2;
		
		switch (type) {
		
		case MESH_ORIENTED_TRIANGLES:
			// (n,p) vertices -> (n-1, p-1) segments
			
			for (int i=0; i<nbv_x-1; i++) {
				for (int j=0; j<nbv_y-1; j++) {

					//In each stitch, create 2 triangles as follows (ORIENTED same in each stitch)
					//   j+1 +---+
					//       | / |
					//    j  +---+
					//       i  i+1

					t1 = new Triangle(vertices[i][j], vertices[i+1][j], vertices[i+1][j+1]);
					t2 = new Triangle(vertices[i+1][j+1], vertices[i][j+1], vertices[i][j]);
					
					// Texture application on the RectangleMesh, regular stitches
					if (tex!=null) {
						
						Vector4 tv1, tv2, tv3, tv4;
					
						// Define position for Texture vectors based on stitch in homogeneous coordinates [0,1] within the RectangleMesh
						double ti = (double)i/(double)(nbv_x-1);
						double tip1 = (double)(i+1)/(double)(nbv_x-1);
						double tj = (double)j/(double)(nbv_y-1);
						double tjp1 = (double)(j+1)/(double)(nbv_y-1);
						
						// Create texture vectors
						tv1 = new Vector4(ti,tj,0,1);
						tv2 = new Vector4(tip1,tj,0,1);
						tv3 = new Vector4(tip1,tjp1,0,1);
						tv4 = new Vector4(ti,tjp1,0,1);
						
						// Set texture vectors to newly created triangles
						t1.setTexture(tex, tv1, tv2, tv3);
						t2.setTexture(tex, tv3, tv4, tv1);
					}
					
					elm.addTriangle(t1);
					elm.addTriangle(t2);
				}
			}
			break;
			
		case MESH_ALTERNATE_TRIANGLES:
			// (n,p) vertices -> (n-1, p-1) segments
			// Furthermore in this case, let's alternate every 2 stitches -> 2 loops for respectively odd and even segments
			
			for (int i=0; i<nbv_x-1; i++) {
				for (int j=0; j<nbv_y-1; j++) {
					// Create triangles with alternate diagonal (bottom left to up right then up left to bottom right alternately)
					if ((i%2 == 0 && j%2 == 0) || (i%2 != 0 && j%2 != 0)) { // (i even and j even) or (i odd and j odd)
						//In every 2 stitches, create 2 triangles as follows (ORIENTED same in each stitch)
						//   j+1 +---+
						//       | / |
						//    j  +---+
						//       i  i+1
						t1 = new Triangle(vertices[i][j], vertices[i+1][j], vertices[i+1][j+1]);
						t2 = new Triangle(vertices[i+1][j+1], vertices[i][j+1], vertices[i][j]);

						// Texture application on the RectangleMesh, regular stitches
						if (tex!=null) {
							
							Vector4 tv1, tv2, tv3, tv4;
						
							// Define position for Texture vectors based on stitch in homogeneous coordinates [0,1] within the RectangleMesh
							double ti = (double)i/(double)(nbv_x-1);
							double tip1 = (double)(i+1)/(double)(nbv_x-1);
							double tj = (double)j/(double)(nbv_y-1);
							double tjp1 = (double)(j+1)/(double)(nbv_y-1);
							
							// Create texture vectors
							tv1 = new Vector4(ti,tj,0,1);
							tv2 = new Vector4(tip1,tj,0,1);
							tv3 = new Vector4(tip1,tjp1,0,1);
							tv4 = new Vector4(ti,tjp1,0,1);
							
							// Set texture vectors to newly created triangles
							t1.setTexture(tex, tv1, tv2, tv3);
							t2.setTexture(tex, tv3, tv4, tv1);
						}

						elm.addTriangle(t1);
						elm.addTriangle(t2);
						
					} else {
						//In every other stitch, create 2 triangles as follows (ALTERNATE)
						//   j+1 +---+
						//       | \ |
						//    j  +---+
						//       i  i+1
						t1 = new Triangle(vertices[i][j+1], vertices[i][j], vertices[i+1][j]);
						t2 = new Triangle(vertices[i+1][j], vertices[i+1][j+1], vertices[i][j+1]);
						
						// Texture application on the RectangleMesh, regular stitches
						if (tex!=null) {
							
							Vector4 tv1, tv2, tv3, tv4;
						
							// Define position for Texture vectors based on stitch in homogeneous coordinates [0,1] within the RectangleMesh
							double ti = (double)i/(double)(nbv_x-1);
							double tip1 = (double)(i+1)/(double)(nbv_x-1);
							double tj = (double)j/(double)(nbv_y-1);
							double tjp1 = (double)(j+1)/(double)(nbv_y-1);
							
							// Create texture vectors
							tv1 = new Vector4(ti,tj,0,1);
							tv2 = new Vector4(tip1,tj,0,1);
							tv3 = new Vector4(tip1,tjp1,0,1);
							tv4 = new Vector4(ti,tjp1,0,1);
							
							// Set texture vectors to newly created triangles
							t1.setTexture(tex, tv4, tv1, tv2);
							t2.setTexture(tex, tv2, tv3, tv4);
						}

						elm.addTriangle(t1);
						elm.addTriangle(t2);
					}
				}
			}
			break;
			
		default:
			// Invalid type
			return;
		}	
	}
	
	public Vertex[][] getVertices() {
		return vertices;
	}
	
	public Vertex getVertex(int i, int j) {
		return vertices[i][j];
	}
}
