package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;

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
 * @since March 2017
 */
public class Pyramid extends Element {

	protected static final String PYRAMID_DEFAULT_NAME = "pyramid";

	protected Vertex[][] vertices;
	protected Vertex summit;
	
	
	/**
	 * Create a Pyramid aligned on axis. Need to be rotated for a different orientation.
	 * 
	 * @param x_dim dimension of the pyramid on x axis
	 * @param y_dim dimension of the pyramid on y axis
	 * @param z_dim dimension of the pyramid on z axis
	 */
	public Pyramid(float x_dim, float y_dim, float z_dim) {
		super(PYRAMID_DEFAULT_NAME);
		subelements = null;
		createPyramid(x_dim, y_dim, z_dim);
	}
	
	protected void createPyramid(float x_dim, float y_dim, float z_dim) {

		vertices = new Vertex[2][2];

		// Calculate dimensions
		float xh = x_dim/2;
		float yh = y_dim/2;
		float zh = z_dim/2;
		
		// Build the Element: Create Vertices of the base of the Pyramid: 4 vertices
		vertices[0][0] = createVertex(new Vector4(-xh, -yh, -zh,  1));
		vertices[0][1] = createVertex(new Vector4(-xh,  yh, -zh,  1));
		vertices[1][1] = createVertex(new Vector4(xh, yh, -zh,  1));
		vertices[1][0] = createVertex(new Vector4(xh, -yh, -zh,  1));
		
		// Create summit
		summit = createVertex(new Vector4(0, 0, zh, 1));
		
		// Creates Triangles from Vertices: 6 faces, 2 triangles each
		Triangle t1 = new Triangle(vertices[0][0], vertices[1][0], summit);
		Triangle t2 = new Triangle(vertices[1][0], vertices[1][1], summit);
		Triangle t3 = new Triangle(vertices[1][1], vertices[0][1], summit);
		Triangle t4 = new Triangle(vertices[0][1], vertices[0][0], summit);


		// Add Triangles to the Element
		this.addTriangle(t1);
		this.addTriangle(t2);
		this.addTriangle(t3);
		this.addTriangle(t4);
	}
}
