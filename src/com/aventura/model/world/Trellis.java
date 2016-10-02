package com.aventura.model.world;

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
 *  Vertices: +
 *  Segments: | - / \
 *  
 *  Example:  *  6 x 4 segments -> 7 x 5 vertices
 *  
 *  +---+---+---+---+---+---+
 *  | \ | / | \ | / | \ | / |
 *  +---+---+---+---+---+---+
 *  | / | \ | / | \ | / | \ |
 *  +---+---+---+---+---+---+
 *  | \ | / | \ | / | \ | / |
 *  +---+---+---+---+---+---+
 *  | / | \ | / | \ | / | \ |
 *  +---+---+---+---+---+---+
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Trellis extends Element {
	
	protected Vertex[][] vertices;
	protected int nx, ny;
	protected double width, length;

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * z axis altitudes are set to 0.
	 * This Trellis is centered on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 */
	public Trellis(double width, double length, int nx, int ny) {
		super();
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 position = new Vector4(-width/2,-length/2,0,0);
		createTrellis(position);
		createTriangles();
	}

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * z axis altitudes are set to 0.
	 * The first vertex of the Trellis is translated by the Vector3 position
	 * If position vector is 0 then the first vertex starts on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 * @param position
	 */
	public Trellis(double width, double length, int nx, int ny, Vector3 position) {
		super();
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 v = new Vector4(position);
		createTrellis(v);
		createTriangles();
	}

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * z axis altitudes are set to 0.
	 * The first vertex of the Trellis is translated by the Vector3 position
	 * If position vector is 0 then the first vertex starts on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 * @param position
	 */
	public Trellis(double width, double length, int nx, int ny, Vector4 position) {
		super();
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		createTrellis(position);
		createTriangles();
	}

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * Use the z axis altitudes from the provided array.
	 * This Trellis is centered on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 * @param array
	 */
	public Trellis(double width, double length, int nx, int ny, double [][] array) throws WrongArraySizeException {
		super();
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 position = new Vector4(-width/2,-length/2,0,0);
		createTrellis(position, array);
		createTriangles();
	}

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * Use the z axis altitudes from the provided array.
	 * The first vertex of the Trellis is translated by the Vector3 position
	 * If position vector is 0 then the first vertex starts on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 * @param position
	 * @param array
	 */
	public Trellis(double width, double length, int nx, int ny, Vector3 position, double [][] array) throws WrongArraySizeException {
		super();
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 v = new Vector4(position);
		createTrellis(v, array);
		createTriangles();
	}

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * Use the z axis altitudes from the provided array.
	 * The first vertex of the Trellis is translated by the Vector3 position
	 * If position vector is 0 then the first vertex starts on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 * @param position
	 * @param array
	 */
	public Trellis(double width, double length, int nx, int ny, Vector4 position, double [][] array) throws WrongArraySizeException {
		super();
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		createTrellis(position, array);
		createTriangles();
	}

	protected void createTrellis(Vector4 position) {
		
		vertices = new Vertex[nx+1][ny+1]; // for a Trellis: number of vertices = number of segments + 1
		
		// Create Vertices
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				vertices[i][j] = new Vertex(new Vector4(i*width/nx, j*length/ny, 0, 1).plus(position));
			}
		}
	}

	protected void createTrellis(Vector4 position, double [][] array) throws WrongArraySizeException {
		
		if ((array.length != nx+1) || (array[0].length != ny+1)) {
			throw new WrongArraySizeException("Array should be of size("+nx+1+","+ny+1+") but is of size("+array.length+","+array[0].length+")");
		}
		
		vertices = new Vertex[nx+1][ny+1]; // for a Trellis: number of vertices = number of segments + 1
		
		// Create Vertices
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				vertices[i][j] = new Vertex(new Vector4(i*width/nx, j*length/ny, array[i][j], 1).plus(position));
			}
		}
	}

	protected void createTriangles() {
		Triangle t1, t2;
		
		for (int i=0; i<nx; i++) {
			for (int j=0; j<ny; j++) {
				// Create triangles with alternate diagonal (bottom left to up right then up left to bottom right alternately)
				if ((i%2 == 0 && j%2 == 0) || (i%2 != 0 && j%2 != 0)) { // (i even and j even) or (i odd and j odd)
					t1 = new Triangle(vertices[i][j], vertices[i+1][j], vertices[i+1][j+1]);
					t2 = new Triangle(vertices[i+1][j+1], vertices[i][j+1], vertices[i][j]);
					this.addTriangle(t1);			
					this.addTriangle(t2);			
				} else {
					t1 = new Triangle(vertices[i][j+1], vertices[i][j], vertices[i+1][j]);
					t2 = new Triangle(vertices[i+1][j], vertices[i+1][j+1], vertices[i][j+1]);
					this.addTriangle(t1);			
					this.addTriangle(t2);			
				}
			}
		}
	}
	
	public int getNx() {
		return nx;
	}
	
	public int getNy() {
		return ny;
	}
	
	public double getWidth() {
		return width;
	}
	
	public double getLength() {
		return length;
	}
	
}
