package com.aventura.model.world;

import com.aventura.math.transform.Translation;
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
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Box extends Element {
	
	protected Vertex[][][] vertices;
	
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * Box is translated by the provided Vector3
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 * @param position the translation vector
	 */
	public Box(double x_dim, double y_dim, double z_dim, Vector3 position) {
		super();
		subelements = null;
		Vector4 v = new Vector4(position);
		createBox(x_dim, y_dim, z_dim, v);
	}
	
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * Box is translated by the provided Vector3
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 * @param position the translation vector
	 */
	public Box(double x_dim, double y_dim, double z_dim, Vector4 position) {
		super();
		subelements = null;
		createBox(x_dim, y_dim, z_dim, position);
	}
	
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * 
	 * @param x_dim dimension of the box on x axis
	 * @param y_dim dimension of the box on y axis
	 * @param z_dim dimension of the box on z axis
	 */
	public Box(double x_dim, double y_dim, double z_dim) {
		super();
		subelements = null;
		Vector4 position = new Vector4(0,0,0,0);
		createBox(x_dim, y_dim, z_dim, position);
	}
	
	protected void createBox(double x_dim, double y_dim, double z_dim, Vector4 position) {

		vertices = new Vertex[2][2][2];

		// Calculate dimensions
		double xh = x_dim/2;
		double yh = y_dim/2;
		double zh = z_dim/2;
		
		// Build the Element: Create Vertices of the Cube: 8 vertices
		vertices[0][0][0] = new Vertex(new Vector4(-xh, -yh, -zh,  1).plus(position));
		vertices[0][1][0] = new Vertex(new Vector4(xh,  -yh, -zh,  1).plus(position));
		vertices[1][1][0] = new Vertex(new Vector4(xh, -yh, zh,  1).plus(position));
		vertices[1][0][0] = new Vertex(new Vector4(-xh,  -yh, zh,  1).plus(position));
		vertices[0][0][1] = new Vertex(new Vector4(-xh, yh, -zh,  1).plus(position));
		vertices[0][1][1] = new Vertex(new Vector4(xh,  yh, -zh,  1).plus(position));
		vertices[1][1][1] = new Vertex(new Vector4(xh, yh, zh,  1).plus(position));
		vertices[1][0][1] = new Vertex(new Vector4(-xh,  yh, zh,  1).plus(position));
		
		// Creates Triangles from Vertices: 6 faces, 2 triangles each
		Triangle t1 = new Triangle(vertices[0][0][0], vertices[0][1][0], vertices[1][1][0]);
		Triangle t2 = new Triangle(vertices[1][1][0], vertices[1][0][0], vertices[0][0][0]);
		
		Triangle t3 = new Triangle(vertices[0][0][0], vertices[0][1][0], vertices[0][1][1]);
		Triangle t4 = new Triangle(vertices[0][1][1], vertices[0][0][1], vertices[0][0][0]);

		Triangle t5 = new Triangle(vertices[0][0][0], vertices[1][0][0], vertices[1][0][1]);
		Triangle t6 = new Triangle(vertices[1][0][1], vertices[0][0][1], vertices[0][0][0]);

		Triangle t7 = new Triangle(vertices[1][0][1], vertices[0][0][1], vertices[0][1][1]);
		Triangle t8 = new Triangle(vertices[0][1][1], vertices[1][1][1], vertices[1][0][1]);
		
		Triangle t9 = new Triangle(vertices[1][0][1], vertices[1][0][0], vertices[1][1][0]);
		Triangle t10 = new Triangle(vertices[1][1][0], vertices[1][1][1], vertices[1][0][1]);

		Triangle t11 = new Triangle(vertices[1][1][0], vertices[1][1][1], vertices[0][1][1]);
		Triangle t12 = new Triangle(vertices[0][1][1], vertices[0][1][0], vertices[1][1][0]);

		// Add Triangles to the Element
		this.addTriangle(t1);
		this.addTriangle(t2);
		this.addTriangle(t3);
		this.addTriangle(t4);
		this.addTriangle(t5);
		this.addTriangle(t6);
		this.addTriangle(t7);
		this.addTriangle(t8);
		this.addTriangle(t9);
		this.addTriangle(t10);
		this.addTriangle(t11);
		this.addTriangle(t12);		
	}

	@Override
	public void calculateNormals() {
		// TODO Auto-generated method stub
		
	}

}
