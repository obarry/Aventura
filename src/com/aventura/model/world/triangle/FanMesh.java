package com.aventura.model.world.triangle;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Element;


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
	//  \ /     |     \ /  n base vertices
	//   +------+------+
	
	// Texture Wrapping constants for parameters
	public static final int MESH_ORIENTED_TRIANGLES = 1;
	public static final double TEXTURE_SUMMIT_SMALL_VALUE = 0.0001;
	public static final double TEXTURE_SUMMIT_SMALL_VALUE_DOUBLE = 0.0002;

	int nbv;
	Vertex[] summits;
	Vertex[] vertices;

	/**
	 * Create a FanMesh without Texture
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param n the number of base vertices of the FanMesh >= 1
	 */
	public FanMesh(Element e, int n) {
		super(e);
		this.nbv = n;
		
		// Need to have 1 summit for each triangle to distinguish normals at Vertex level for each (summit) of triangle even if they are all same Vertex
		summits = elm.createVertexMesh(n-1);
		vertices = elm.createVertexMesh(n);
	}
	
	/**
	 * Create a FanMesh with Texture
	 * @param e the Element to which all created vertices of the FanMesh should belong
	 * @param n the number of base vertices of the FanMesh
	 * @param t the Texture to be wrapped on the FanMesh
	 */
	public FanMesh(Element e, int n, Texture t) {
		super(e);
		this.nbv = n;
		this.tex = t;
		// Need to have 1 summit for each triangle to distinguish normals at Vertex level for each (summit) of triangle even if they are all same Vertex
		summits = elm.createVertexMesh(n-1);
		vertices = elm.createVertexMesh(n);
	}

	/**
	 * @param type
	 */
	public void createTriangles(int type) {

		Triangle t;

		switch (type) {

		case MESH_ORIENTED_TRIANGLES:
			// (n) vertices -> (n-1) triangles

			double tsx = TEXTURE_SUMMIT_SMALL_VALUE;
			double tsz = TEXTURE_SUMMIT_SMALL_VALUE_DOUBLE;
			double ti = 0;
			double tip1 = 0;
			
			for (int i=0; i<nbv-1; i++) {

				// Creation of triangles
				//
				//   summit  +
				//         / |
				//       +---+
				//       i  i+1

			
				t = new Triangle(vertices[i], vertices[i+1], summits[i]);

				// Texture application on the FanMesh, assuming regular stitches
				if (tex!=null) {

					Vector4 tv1, tv2, tv3;

					// Define position for Texture vectors in homogeneous coordinates [0,1]
					tip1 = (double)(i+1)/(double)(nbv-1);

					// Create texture vectors with an horizontal 'stretching' texture effect (on the tip of the fan) 
//					tv1 = new Vector4(ti,0,0,1);
//					tv2 = new Vector4(0,tsx,0,tsz);
//					tv3 = new Vector4(tip1,0,0,1);
					tv1 = new Vector4(ti,1,0,1);
					tv2 = new Vector4(tip1,1,0,1);
					tv3 = new Vector4(0,tsx,0,tsz);
//					tv1 = new Vector4(0,tsx,0,tsz);
//					tv2 = new Vector4(ti,0,0,1);
//					tv3 = new Vector4(tip1,0,0,1);

					// Set texture vectors to newly created triangles
					//t1.setTexture(new Vector4(0,0,0,1), new Vector4(1,0.0001,0,0.0002), new Vector4(0,1,0,1));

//					t.setTextureOrientation(Triangle.TEXTURE_HORIZONTAL);
					t.setTextureOrientation(Triangle.TEXTURE_VERTICAL);
					t.setTexture(tex, tv1, tv2, tv3);
					t.setRectoVerso(!elm.isClosed());
				}

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