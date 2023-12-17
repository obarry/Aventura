package com.aventura.model.world.shape;

import java.awt.Color;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.RectangleMesh;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2023 Olivier BARRY
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
	
	protected static final String BOX_DEFAULT_NAME = "box";
	
	protected float x_dim, y_dim, z_dim;

	protected Vertex[][][] vertices;
	protected RectangleMesh bottom, top, left, right, front, back;
	protected Texture bottom_tex, top_tex, left_tex, right_tex, front_tex, back_tex = null;
	protected Color bottom_col, top_col, left_col, right_col, front_col, back_col = null;
		
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 */
	public Box(float x_dim, float y_dim, float z_dim) {
		super(BOX_DEFAULT_NAME, true); // A Box is a closed Element
		subelements = null;
		this.x_dim = x_dim;
		this.y_dim = y_dim;
		this.z_dim = z_dim;
	}
	
	/**
	 * Create a box aligned on axis with unique texture applied to all faces
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 */
	public Box(float x_dim, float y_dim, float z_dim, Texture tex) {
		super(BOX_DEFAULT_NAME, true); // A Box is a closed Element
		subelements = null;
		this.x_dim = x_dim;
		this.y_dim = y_dim;
		this.z_dim = z_dim;
		this.bottom_tex = tex;
		this.top_tex = tex;
		this.left_tex = tex;
		this.right_tex = tex;
		this.front_tex = tex;
		this.back_tex = tex;
	}

	/**
	 * Create a box aligned on axis with different texture for each face
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 */
	public Box(float x_dim, float y_dim, float z_dim, Texture bottom_tex, Texture top_tex, Texture left_tex, Texture right_tex, Texture front_tex, Texture back_tex) {
		super(BOX_DEFAULT_NAME, true); // A Box is a closed Element
		subelements = null;
		this.x_dim = x_dim;
		this.y_dim = y_dim;
		this.z_dim = z_dim;
		this.bottom_tex = bottom_tex;
		this.top_tex = top_tex;
		this.left_tex = left_tex;
		this.right_tex = right_tex;
		this.front_tex = front_tex;
		this.back_tex = back_tex; 
	}
	
	public void generateVertices() {
		// Box vertices, 3 dimensions array
		vertices = new Vertex[2][2][2];

		// Calculate dimensions of the box
		float xh = this.x_dim/2;
		float yh = this.y_dim/2;
		float zh = this.z_dim/2;
		
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
		Vertex [][] bottom_array = new Vertex [][] {{vertices[0][1][0],vertices[0][0][0]},{vertices[1][1][0],vertices[1][0][0]}}; // Z const
		Vertex [][] top_array    = new Vertex [][] {{vertices[1][1][1],vertices[1][0][1]},{vertices[0][1][1],vertices[0][0][1]}}; // Z const
		Vertex [][] left_array   = new Vertex [][] {{vertices[0][1][0],vertices[0][1][1]},{vertices[0][0][0],vertices[0][0][1]}}; // X const
		Vertex [][] right_array  = new Vertex [][] {{vertices[1][1][1],vertices[1][1][0]},{vertices[1][0][1],vertices[1][0][0]}}; // X const
		Vertex [][] front_array  = new Vertex [][] {{vertices[0][0][0],vertices[0][0][1]},{vertices[1][0][0],vertices[1][0][1]}}; // Y const
		Vertex [][] back_array   = new Vertex [][] {{vertices[1][1][0],vertices[1][1][1]},{vertices[0][1][0],vertices[0][1][1]}}; // Y const
		
		// Then create the RectangleMeshs
		bottom = new RectangleMesh(this, bottom_array, bottom_tex);
		top = new RectangleMesh(this, top_array, top_tex);
		left = new RectangleMesh(this, left_array, left_tex);
		right = new RectangleMesh(this, right_array, right_tex);
		front = new RectangleMesh(this, front_array, front_tex);
		back = new RectangleMesh(this, back_array, back_tex);
		
		// Set color to each face
		bottom.setCol(this.bottom_col);
		top.setCol(this.top_col);
		left.setCol(this.left_col);
		right.setCol(this.right_col);
		front.setCol(this.front_col);
		back.setCol(this.back_col);

	}
	
	public void generateTriangles() {
		// At last create Triangles of all meshes
		bottom.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		top.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		left.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		right.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		front.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		back.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);

	}
	
//	public void createGeometry() {
//
//		// Box vertices, 3 dimensions array
//		vertices = new Vertex[2][2][2];
//
//		// Calculate dimensions of the box
//		float xh = this.x_dim/2;
//		float yh = this.y_dim/2;
//		float zh = this.z_dim/2;
//		
//		// Build the Element: Create Vertices of the Cube: 8 vertices
//		vertices[0][0][0] = createVertex(new Vector4(-xh, -yh, -zh,  1));
//		vertices[0][1][0] = createVertex(new Vector4(-xh,  yh, -zh,  1));
//		vertices[1][1][0] = createVertex(new Vector4(xh, yh, -zh,  1));
//		vertices[1][0][0] = createVertex(new Vector4(xh, -yh, -zh,  1));
//		vertices[0][0][1] = createVertex(new Vector4(-xh, -yh, zh,  1));
//		vertices[0][1][1] = createVertex(new Vector4(-xh,  yh, zh,  1));
//		vertices[1][1][1] = createVertex(new Vector4(xh, yh, zh,  1));
//		vertices[1][0][1] = createVertex(new Vector4(xh,  -yh, zh,  1));
//		
//		// Create RectangleMeshs for each face of the box to wrap each face into Textures
//		// For this create 6 temporary Vertex arrays used to point on the box vertices of each face
//		Vertex [][] bottom_array = new Vertex [][] {{vertices[0][1][0],vertices[0][0][0]},{vertices[1][1][0],vertices[1][0][0]}}; // Z const
//		Vertex [][] top_array    = new Vertex [][] {{vertices[1][1][1],vertices[1][0][1]},{vertices[0][1][1],vertices[0][0][1]}}; // Z const
//		Vertex [][] left_array   = new Vertex [][] {{vertices[0][1][0],vertices[0][1][1]},{vertices[0][0][0],vertices[0][0][1]}}; // X const
//		Vertex [][] right_array  = new Vertex [][] {{vertices[1][1][1],vertices[1][1][0]},{vertices[1][0][1],vertices[1][0][0]}}; // X const
//		Vertex [][] front_array  = new Vertex [][] {{vertices[0][0][0],vertices[0][0][1]},{vertices[1][0][0],vertices[1][0][1]}}; // Y const
//		Vertex [][] back_array   = new Vertex [][] {{vertices[1][1][0],vertices[1][1][1]},{vertices[0][1][0],vertices[0][1][1]}}; // Y const
//		
//		// Then create the RectangleMeshs
//		bottom = new RectangleMesh(this, bottom_array, bottom_tex);
//		top = new RectangleMesh(this, top_array, top_tex);
//		left = new RectangleMesh(this, left_array, left_tex);
//		right = new RectangleMesh(this, right_array, right_tex);
//		front = new RectangleMesh(this, front_array, front_tex);
//		back = new RectangleMesh(this, back_array, back_tex);
//		
//		// Set color to each face
//		bottom.setCol(this.bottom_col);
//		top.setCol(this.top_col);
//		left.setCol(this.left_col);
//		right.setCol(this.right_col);
//		front.setCol(this.front_col);
//		back.setCol(this.back_col);
//		
//		// At last create Triangles of all meshes
//		bottom.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
//		top.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
//		left.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
//		right.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
//		front.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
//		back.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
//	}
	
	@Override
	public void setColor(Color col) {
		super.setColor(col);
		// Set same color to each face
		bottom_col = col;
		top_col = col;
		left_col = col;
		right_col = col;
		front_col = col;
		back_col = col;
	} 
	
	
	@Override
	public Element setTopTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.top_tex = tex;
		return this;
	} 

	@Override
	public Element setBottomTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.bottom_tex = tex;
		return this;
	}

	@Override
	public Element setLeftTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.left_tex = tex;
		return this;
	}

	@Override
	public Element setRightTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.right_tex = tex;
		return this;
	}

	@Override
	public Element setFrontTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.front_tex = tex;
		return this;
	}

	@Override
	public Element setBackTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.back_tex = tex;
		return this;
	}

	@Override
	public Element setTopColor(Color c) {
		// TODO Auto-generated method stub
		this.top_col = c;
		return this;
	}

	@Override
	public Element setBottomColor(Color c) {
		// TODO Auto-generated method stub
		this.bottom_col = c;
		return this;
	}

	@Override
	public Element setLeftColor(Color c) {
		// TODO Auto-generated method stub
		this.left_col = c;
		return this;
	}

	@Override
	public Element setRightColor(Color c) {
		// TODO Auto-generated method stub
		this.right_col = c;
		return this;
	}

	@Override
	public Element setFrontColor(Color c) {
		// TODO Auto-generated method stub
		this.front_col = c;
		return this;
	}

	@Override
	public Element setBackColor(Color c) {
		// TODO Auto-generated method stub
		this.back_col = c;
		return this;
	}

}
