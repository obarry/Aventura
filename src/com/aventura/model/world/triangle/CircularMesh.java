package com.aventura.model.world.triangle;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Element;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
 * A Circular Mesh of vertices around a central vertex with a Texture to create a disc
 * The triangles are distributed as pie slices from the center of the disc.
 * This class creates properly the array of Vertices and proposes services to create the list of Triangle for the fan of triangles and wrap a texture to it.
 * This class does not generate the geometry of the surface, this is the user of this class who needs to set the position of each Vertex in the space.
 * 
 * @author Olivier BARRY
 * @since Feb 2018
 */
public class CircularMesh extends Mesh {

	// Circular Mesh
	
	//          V0 _____ V1
	//          _--     --_
	//         /  \  T1 /  \
	//  V7   /_ T8 \   / T2 _\ V2
	//      |  -- _ \ / _ --  |
	//     |  T7  _ >+< _  T3  |      1 center vertex
	//      | _--   / \   --_ |
	//   V6  \  T6 /   \ T4  / V3
	//         \_ / T5  \ _/  
	//           --_____--
	//         V5          V4      n segments <-> n vertices
	//
	
	// Texture Wrapping constants for parameters
	public static final int MESH_CIRCULAR_CUT_TEXTURE = 1;
	public static final int MESH_DEFORM_FULL_TEXTURE = 2;
	
	int nbs; // Number of base segments = number of base vertices
	Vertex center;
	Vertex[] vertices;

	/**
	 * Create a CircularMesh with nothing (base constructor)
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 */
	public CircularMesh(Element e) {
		super(e);
	}
	
	/**
	 * Create a FanMesh
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param n the number of vertices on the ring
	 */
	public CircularMesh(Element e, int n) {
		super(e);
		this.nbs = n;
		center = elm.createVertex();
		vertices = elm.createVertexMesh(this.nbs);
	}
	
	/**
	 * Create a FanMesh with Texture and with array of Vertices
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param n the number of vertices on the ring
	 */
	public CircularMesh(Element e, int n, Texture t) {
		super(e);
		this.nbs = n;
		center = elm.createVertex();
		vertices = elm.createVertexMesh(this.nbs);
		this.tex = t;
	}
	
	public void initVertices(int n) {
		this.nbs = n;
		center = elm.createVertex();
		vertices = elm.createVertexMesh(this.nbs);
	}
	
	public void setVertex(int i, Vertex v) {
		vertices[i] = v;
	}
	
	public Vertex getVertex(int i) {
		return vertices[i];
	}
	
	public void setCenter(Vertex v) {
		center = v;
	}
	
	public void setCenter(Vector4 v) {
		center.setPos(v);
	}
	
	public Vertex getCenter() {
		return center;
	}


	/**
	 * Create triangles of the CircularMesh
	 * And apply texture on this mesh
	 */
	public void createTriangles(int type) {
		Triangle t;

		// Create all triangles around the ring
		for (int i=0; i<=this.nbs-1; i++) {

			if (i<this.nbs-1) {
				t = new Triangle(center, vertices[i], vertices[i+1]);
			} else { // i = this.nbs-1 -> last triangle to loop the circular mesh
				t = new Triangle(center, vertices[i], vertices[0]);
			}

			// Texture application on the CircularMesh, assuming regular stitches
			if (tex!=null) {

				switch (type) {

				case MESH_CIRCULAR_CUT_TEXTURE:

					Vector4 tv1, tv2, tv3;
					float alpha = (float) (2*Math.PI/nbs);

					// Create texture vectors
					tv1 = new Vector4(0.5f,0.5f,0,1);    // First vertex of the triangle: center
					tv2 = new Vector4((0.5f+(float)(Math.cos(alpha*i))/2),(0.5f+(float)(Math.sin(alpha*i))/2),0,1);  // Second vertex of the triangle: i
					tv3 = new Vector4((0.5f+(float)(Math.cos(alpha*(i+1)))/2),(0.5f+(float)(Math.sin(alpha*(i+1)))/2),0,1); // Last vertext of the triangle: i+1

					// Set texture vectors to newly created triangles
					t.setTexture(tex, tv1, tv2, tv3);
					t.setRectoVerso(!elm.isClosed());

					break;

				case MESH_DEFORM_FULL_TEXTURE:
					// TODO To be implemented
					break;
					
				default:
					// Invalid type
					return;
				}
				
				// Add newly created triangle to the reporting Element
				t.setColor(this.col);
				elm.addTriangle(t);
			}

		}
	}
	
}
