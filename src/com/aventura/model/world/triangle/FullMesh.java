package com.aventura.model.world.triangle;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;

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
 * A Mesh made of square stitches with additional vertices on the center of each stich, creating 4 triangles in each stich
 * It is actually made of 2 meshes, see details below. 
 * 1 single Texture should be applied to create a surface of an Element
 * This class creates properly the array of Vertices but does not generate the geometry of the surface, this remains up to the object who creates instance of this class
 * to set the position of each Vertex in space.
 * 
 * @author Olivier BARRY
 * @since May 2018
 */
public class FullMesh extends Mesh {
	
	//  +-------+-------+
	//  | \   / | \   / |
	//  |   +   |   x   |
	//  | /   \ | /   \ |
	//  +-------+-------+  nbv_y : number of vertices on the main mesh (shown as '+')
	//  | \   / | \   / |  -> number of vertices on secondary mesh (centers) = nbv_y - 1 (shown as 'x')
	//  |   x   |   x   |
	//  | /   \ | /   \ |
	//  +-------+-------+
	//        nbv_x : number of vertices on the main mesh (shown as '+')
	//        -> number of vertices on secondary mesh (centers) = nbv_x - 1 (shown as 'x')
	
	// Number of Vertices on x and y of the rectangleMesh
	int nbv_x, nbv_y = 0;
	Vertex[][] vertices_main = null; // Table of vertices for main mesh
	Vertex[][] vertices_secondary = null; // Table of vertices for secondary mesh (made of the centers of each stitch of the main mesh)

	/**
	 * Create an empty FullMesh and attach it to an Element
	 * @param e the referenced Element
	 */
	public FullMesh(Element e) {
		super(e);
	}
	
	/**
	 * Create a FullMesh from sets of vertices
	 * @param e the referenced Element
	 * @param vertices_main the array of main vertices
	 * @param vertices_secondary the array of secondary vertices, centers of the stitches
	 * @param t  the Texture to apply to the mesh
	 */
	public FullMesh(Element e, Vertex[][] vertices_main, Vertex[][] vertices_secondary, Texture t) {
		super(e);
		this.vertices_main = vertices_main;
		this.vertices_secondary = vertices_secondary;
		this.nbv_x = vertices_main.length;
		this.nbv_y = vertices_main[0].length;
		this.tex = t;
	}
	
	/**
	 * Create a FullMesh of [n,p] stitches
	 * @param e the referenced Element
	 * @param n number of stitches in X
	 * @param p number of stitches in Y
	 */
	public FullMesh(Element e, int n, int p) {
		super(e);
		this.nbv_x = n+1; // Number of vertices is +1 the number of stitches
		this.nbv_y = p+1; // Number of vertices is +1 the number of stitches
		vertices_main = elm.createVertexMesh(n+1, p+1);
		vertices_secondary = elm.createVertexMesh(n, p);
	}
	
	/**
	 * Create a FullMesh of [n,p] stitches with Texture
	 * @param e the referenced Element
	 * @param n number of stitches in X
	 * @param p number of stitches in Y
	 * @param t the Texture to apply to the mesh
	 */
	public FullMesh(Element e, int n, int p, Texture t) {
		super(e);
		this.nbv_x = n+1; // Number of vertices is +1 the number of stitches
		this.nbv_y = p+1; // Number of vertices is +1 the number of stitches
		this.tex = t;
		vertices_main = elm.createVertexMesh(n+1, p+1);
		vertices_secondary = elm.createVertexMesh(n, p);
	}
	
	public void initVertices(int n, int p) {
		this.nbv_x = n+1; // Number of vertices is +1 the number of stitches
		this.nbv_y = p+1; // Number of vertices is +1 the number of stitches
		vertices_main = elm.createVertexMesh(n+1, p+1);
		vertices_secondary = elm.createVertexMesh(n, p);
	}
	
	public void initVertices(Vertex[][] vertices_main, Vertex[][] vertices_secondary) {
		this.vertices_main = vertices_main;
		this.vertices_secondary = vertices_secondary;
		this.nbv_x = vertices_main.length;
		this.nbv_y = vertices_main[0].length;
	}
	
	public void setMainVertex(int i, int j, Vertex v) {
		vertices_main[i][j] = v;
	}
	
	public void setSecondaryVertex(int i, int j, Vertex v) {
		vertices_secondary[i][j] = v;
	}
	
	/**
	 * Create triangles of the RectangleMesh with 4 triangles in each stitch of the main mesh, using the secondary mesh
	 * to locate the center of the stitches of the main mesh.
	 * Note that normal of triangles are naturally all oriented on same side of the mesh
	 */
	public void createTriangles() {

		Triangle t1, t2, t3, t4;

		// (n,p) vertices -> (n-1, p-1) segments
		//
		//   V4              V3
		//    +---------------+ j+1
		//    | \     T3    / |
		//    |   \       /   |
		//    |     \ VC/     |
		//    |  T4   x   T2  |  
		//    |     /   \     |  
		//    |   /   T1  \   |
		//    | /           \ |
		// V1 +---------------+ j V2
		//    i			   i+1
		//
		//

		for (int i=0; i<nbv_x-1; i++) {
			for (int j=0; j<nbv_y-1; j++) {

				t1 = new Triangle(vertices_main[i][j], vertices_main[i+1][j], vertices_secondary[i][j]);
				t2 = new Triangle(vertices_main[i+1][j], vertices_main[i+1][j+1], vertices_secondary[i][j]);
				t3 = new Triangle(vertices_main[i+1][j+1], vertices_main[i][j+1], vertices_secondary[i][j]);
				t4 = new Triangle(vertices_main[i][j+1], vertices_main[i][j], vertices_secondary[i][j]);

				// Texture application on the RectangleMesh, regular stitches
				if (tex!=null) {
					
					// Texture vectors for each vertex (including center)
					Vector4 tv1, tv2, tv3, tv4, tvc;

					// Define position for Texture vectors based on stitch in homogeneous coordinates [0,1] within the RectangleMesh
					float ti = (float)i/(float)(nbv_x-1);
					float tip1 = (float)(i+1)/(float)(nbv_x-1);
					float tj = (float)j/(float)(nbv_y-1);
					float tjp1 = (float)(j+1)/(float)(nbv_y-1);

					// Create texture vectors
					tv1 = new Vector4(ti,tj,0,1);
					tv2 = new Vector4(tip1,tj,0,1);
					tv3 = new Vector4(tip1,tjp1,0,1);
					tv4 = new Vector4(ti,tjp1,0,1);
					tvc = new Vector4((ti+tip1)/2, (tj+tjp1)/2, 0, 1);

					// Set texture vectors to newly created triangles
					t1.setTexture(tex, tv1, tv2, tvc);
					t2.setTexture(tex, tv2, tv3, tvc);
					t3.setTexture(tex, tv3, tv4, tvc);
					t4.setTexture(tex, tv4, tv1, tvc);
					
					t1.setRectoVerso(!elm.isClosed());
					t2.setRectoVerso(!elm.isClosed());
					t3.setRectoVerso(!elm.isClosed());
					t4.setRectoVerso(!elm.isClosed());
				}

				// Set color of each triangle to the element's color
				t1.setColor(this.col);
				t2.setColor(this.col);
				t3.setColor(this.col);
				t4.setColor(this.col);
				
				// Add triangle to the element's list of triangles
				elm.addTriangle(t1);
				elm.addTriangle(t2);
				elm.addTriangle(t3);
				elm.addTriangle(t4);
			}
		}

	}
	
	public Vertex[][] getMainVertices() {
		return vertices_main;
	}
	
	public Vertex[][] getSecondaryVertices() {
		return vertices_secondary;
	}
	
	public Vertex getMainVertex(int i, int j) {
		return vertices_main[i][j];
	}
	
	public Vertex getSecondaryVertex(int i, int j) {
		return vertices_secondary[i][j];
	}
}
