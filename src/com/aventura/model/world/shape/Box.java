package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.RectangleMesh;

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
 * @author Olivier BARRY
 * @since May 2016
 */
public class Box extends Element {
	
	protected static final String CONE_DEFAULT_NAME = "box";

	protected Vertex[][][] vertices;
	protected RectangleMesh bottom, top, left, right, front, back;
	protected Texture bottom_tex, top_tex, left_tex, right_tex, front_tex, back_tex = null;
		
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 */
	public Box(double x_dim, double y_dim, double z_dim) {
		super(CONE_DEFAULT_NAME, true); // A Box is a closed Element
		subelements = null;
		createBox(x_dim, y_dim, z_dim);
	}
	
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 */
	public Box(double x_dim, double y_dim, double z_dim, Texture bottom_tex, Texture top_tex, Texture left_tex, Texture right_tex, Texture front_tex, Texture back_tex) {
		super(CONE_DEFAULT_NAME, true); // A Box is a closed Element
		subelements = null;
		createBox(x_dim, y_dim, z_dim);
	}
	protected void createBox(double x_dim, double y_dim, double z_dim) {

		// Box vertices, 3 dimensions array
		vertices = new Vertex[2][2][2];

		// Calculate dimensions of the box
		double xh = x_dim/2;
		double yh = y_dim/2;
		double zh = z_dim/2;
		
		// Build the Element: Create Vertices of the Cube: 8 vertices
		vertices[0][0][0] = createVertex(new Vector4(-xh, -yh, -zh,  1));
		vertices[0][1][0] = createVertex(new Vector4(-xh,  yh, -zh,  1));
		vertices[1][1][0] = createVertex(new Vector4(xh, yh, -zh,  1));
		vertices[1][0][0] = createVertex(new Vector4(xh, -yh, -zh,  1));
		vertices[0][0][1] = createVertex(new Vector4(-xh, -yh, zh,  1));
		vertices[0][1][1] = createVertex(new Vector4(-xh,  yh, zh,  1));
		vertices[1][1][1] = createVertex(new Vector4(xh, yh, zh,  1));
		vertices[1][0][1] = createVertex(new Vector4(xh,  -yh, zh,  1));
		
		// Create RectangleMeshs for each face of the box to wrap each face into Textures
		// For this create 6 temporary Vertex arrays used to point on the box vertices of each face
//		Vertex [][] bottom_array = new Vertex [][] {{vertices[0][0][0],vertices[0][1][0]},{vertices[1][0][0],vertices[1][1][0]}};
//		Vertex [][] top_array    = new Vertex [][] {{vertices[0][0][1],vertices[1][0][1]},{vertices[0][1][1],vertices[1][1][1]}};
//		Vertex [][] left_array   = new Vertex [][] {{vertices[0][0][0],vertices[0][0][1]},{vertices[0][1][0],vertices[0][1][1]}};
//		Vertex [][] right_array  = new Vertex [][] {{vertices[1][0][0],vertices[1][1][0]},{vertices[1][0][1],vertices[1][1][1]}};
//		Vertex [][] front_array  = new Vertex [][] {{vertices[0][0][0],vertices[1][0][0]},{vertices[0][0][1],vertices[1][0][1]}};
//		Vertex [][] back_array   = new Vertex [][] {{vertices[0][1][0],vertices[0][1][1]},{vertices[1][1][0],vertices[1][1][1]}};
		Vertex [][] bottom_array = new Vertex [][] {{vertices[0][0][0],vertices[0][1][0]},{vertices[1][0][0],vertices[1][1][0]}}; // Z const
		Vertex [][] top_array    = new Vertex [][] {{vertices[1][1][1],vertices[1][0][1]},{vertices[0][1][1],vertices[0][0][1]}}; // Z const
		Vertex [][] left_array   = new Vertex [][] {{vertices[0][0][0],vertices[0][0][1]},{vertices[0][1][0],vertices[0][1][1]}}; // X const
		Vertex [][] right_array  = new Vertex [][] {{vertices[1][1][1],vertices[1][1][0]},{vertices[1][0][1],vertices[1][0][0]}}; // X const
		Vertex [][] front_array  = new Vertex [][] {{vertices[0][0][0],vertices[0][0][1]},{vertices[1][0][0],vertices[1][0][1]}}; // Y const
		Vertex [][] back_array   = new Vertex [][] {{vertices[1][1][1],vertices[1][1][0]},{vertices[0][1][1],vertices[0][1][0]}}; // Y const
		
		// Then create the RectangleMeshs
		bottom = new RectangleMesh(this, bottom_array, bottom_tex);
		top = new RectangleMesh(this, top_array, top_tex);
		left = new RectangleMesh(this, left_array, left_tex);
		right = new RectangleMesh(this, right_array, right_tex);
		front = new RectangleMesh(this, front_array, front_tex);
		back = new RectangleMesh(this, back_array, back_tex);
		
		// At last create Triangles of all meshes
		bottom.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
		top.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
		left.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
		right.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
		front.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
		back.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);

		// Creates Triangles from Vertices: 6 faces, 2 triangles each
//		Triangle t1 = new Triangle(vertices[0][0][0], vertices[0][1][0], vertices[1][1][0]);
//		Triangle t2 = new Triangle(vertices[1][1][0], vertices[1][0][0], vertices[0][0][0]);
//		
//		Triangle t3 = new Triangle(vertices[0][0][0], vertices[0][0][1], vertices[0][1][1]);
//		Triangle t4 = new Triangle(vertices[0][1][1], vertices[0][1][0], vertices[0][0][0]);
//
//		Triangle t5 = new Triangle(vertices[0][0][0], vertices[1][0][0], vertices[1][0][1]);
//		Triangle t6 = new Triangle(vertices[1][0][1], vertices[0][0][1], vertices[0][0][0]);
//
//		Triangle t7 = new Triangle(vertices[0][0][1], vertices[1][0][1], vertices[0][1][1]);
//		Triangle t8 = new Triangle(vertices[0][1][1], vertices[1][0][1], vertices[1][1][1]);
//		
//		Triangle t9 = new Triangle(vertices[1][0][1], vertices[1][0][0], vertices[1][1][0]);
//		Triangle t10 = new Triangle(vertices[1][1][0], vertices[1][1][1], vertices[1][0][1]);
//
//		Triangle t11 = new Triangle(vertices[0][1][1], vertices[1][1][1], vertices[1][1][0]);
//		Triangle t12 = new Triangle(vertices[1][1][0], vertices[0][1][0], vertices[0][1][1]);

		// Add Triangles to the Element
//		this.addTriangle(t1);
//		this.addTriangle(t2);
//		this.addTriangle(t3);
//		this.addTriangle(t4);
//		this.addTriangle(t5);
//		this.addTriangle(t6);
//		this.addTriangle(t7);
//		this.addTriangle(t8);
//		this.addTriangle(t9);
//		this.addTriangle(t10);
//		this.addTriangle(t11);
//		this.addTriangle(t12);		
	}

}
