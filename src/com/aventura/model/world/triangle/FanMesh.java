package com.aventura.model.world.triangle;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Element;


/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2019 Olivier BARRY
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
 * A Fan Mesh of vertices with same Texture to create a surface part of an Element
 * A Fan of triangles is made of a set of triangles all sharing 1 vertice hence creating a Fan.
 * This class creates properly the array of Vertices and proposes services to create the list of Triangle for the fan of triangles and wrap a texture to it.
 * This class does not generate the geometry of the surface, this is the user of this class who needs to set the position of each Vertex in the space.
 * 
 * @author Olivier BARRY
 * @since June 2017
 */
public class FanMesh extends Mesh {
	
	// Regular Fan of Triangles
	
	//          +  1 summit vertex
	//        //|\\
	//      / / | \ \
	//    /  /  |  \  \ 
	//  / T1/   |   \T4 \
	// +   / T2 | T3 \   +
	//  \ /     |     \ /  n segments -> n + 1 base vertices
	//   +------+------+
	
	// Texture Wrapping constants for parameters
	public static final int MESH_ORIENTED_TRIANGLES = 1;
	public static final float TEXTURE_SUMMIT_SMALL_VALUE = 0.0001f;
	public static final float TEXTURE_SUMMIT_SMALL_VALUE_DOUBLE = 0.0002f;

	int nbs; // Number of base segments -> number of base vertices = nbs + 1
	Vertex[] summits;
	Vertex[] vertices;

	/**
	 * Create a FanMesh with Texture and with array of Vertices
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param vertices the array of Vertices
	 * @param t the Texture to be wrapped on the FanMesh
	 */
	public FanMesh(Element e, Vertex[] vertices, Vertex summit, Texture t) {
		super(e);
		this.vertices = vertices;
		this.nbs = vertices.length-1;
		summits = elm.createVertexMesh(this.nbs);
		this.setSummit(summit.getPos());
		this.tex = t;
	}
	
	/**
	 * Create a FanMesh without Texture
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param n the number of base segments of the FanMesh >= 1
	 */
	public FanMesh(Element e, int n) {
		super(e);
		this.nbs = n;
		
		// Need to have 1 summit for each triangle to distinguish normals at Vertex level for each (summit) of triangle even if they are all same Vertex
		summits = elm.createVertexMesh(n);
		vertices = elm.createVertexMesh(n+1);
	}
	
	/**
	 * Create a FanMesh with Texture
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param n the number of base segments of the FanMesh >= 1
	 * @param t the Texture to be wrapped on the FanMesh
	 */
	public FanMesh(Element e, int n, Texture t) {
		super(e);
		this.nbs = n;
		this.tex = t;
		// Need to have 1 summit for each triangle to distinguish normals at Vertex level for each (summit) of triangle even if they are all same Vertex
		summits = elm.createVertexMesh(n);
		vertices = elm.createVertexMesh(n+1);
	}

	/**
	 * @param type
	 */
	public void createTriangles(int type) {

		Triangle t;

		switch (type) {

		case MESH_ORIENTED_TRIANGLES:
			// (n) vertices -> (n-1) triangles

			float tsx = TEXTURE_SUMMIT_SMALL_VALUE;
			float tsz = TEXTURE_SUMMIT_SMALL_VALUE_DOUBLE;
			float ti = 0;
			float tip1 = 0;
			
			for (int i=0; i<nbs; i++) {

				// Creation of triangles
				//
				//   summit  +
				//         / | \
				//       +---+---+
				//       i  i+1 i+2

			
				t = new Triangle(vertices[i], vertices[i+1], summits[i]);

				// Texture application on the FanMesh, assuming regular stitches
				if (tex!=null) {

					Vector4 tv1, tv2, tv3;

					// Define position for Texture vectors in homogeneous coordinates [0,1]
					tip1 = (float)(i+1)/(float)(nbs);

					// Create texture vectors with an horizontal 'stretching' texture effect (on the tip of the fan) 
					tv1 = new Vector4(ti,1,0,1);    // First vertex of the triangle: i
					tv2 = new Vector4(tip1,1,0,1);  // Second vertex of the triangle: i+1
					tv3 = new Vector4(0,tsx,0,tsz); // Last vertext of the triangle: summit

					// Set texture vectors to newly created triangles
					t.setTextureOrientation(Triangle.TEXTURE_VERTICAL);
					t.setTexture(tex, tv1, tv2, tv3);
					t.setRectoVerso(!elm.isClosed());
				}

				t.setColor(this.col);
				elm.addTriangle(t);
				ti = tip1;
			}
			break;

		default:
			// Invalid type
			return;
		}	
	}

	public Vertex[] getVertices() {
		return vertices;
	}
	
	public Vertex getVertex(int i) {
		return vertices[i];
	}
	
	public void setSummit(Vector4 v) {
		for (int i=0; i<summits.length; i++) {
			summits[i].setPos(v);
		}
	}
	
	public Vertex[] getSummits() {
		return summits;
	}

	public Vertex getSummit(int i) {
		return summits[i];
	}

}
