package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.triangle.RectangleMesh;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2018 Olivier BARRY
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
 * @author Olivier BARRY
 * @since May 2016
 */
public class Trellis extends Element {
	
	protected static final String TRELLIS_DEFAULT_NAME = "trellis";

	protected RectangleMesh rectangleMesh;
	protected int nx, ny;
	protected float width, length;

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * z axis altitudes are set to 0.
	 * This Trellis is centered on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 */
	public Trellis(float width, float length, int nx, int ny) {
		super(TRELLIS_DEFAULT_NAME);
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 position = new Vector4(-width/2,-length/2,0,0);
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1);
		initTrellis(position);
		rectangleMesh.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
	}

	/**
	 * Create a new Trellis of size width and length on x and y axis and made of nx and ny segments (respectively nx+1 and ny+1 vertices)
	 * z axis altitudes are set to 0.
	 * This Trellis is centered on origin
	 * @param width
	 * @param length
	 * @param nx
	 * @param ny
	 */
	public Trellis(float width, float length, int nx, int ny, Texture tex) {
		super(TRELLIS_DEFAULT_NAME);
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 position = new Vector4(-width/2,-length/2,0,0);
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1, tex);
		initTrellis(position);
		rectangleMesh.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
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
	public Trellis(float width, float length, int nx, int ny, float [][] array) throws WrongArraySizeException {
		super(TRELLIS_DEFAULT_NAME);
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 position = new Vector4(-width/2,-length/2,0,0);
		if ((array.length != nx+1) || (array[0].length != ny+1)) {
			throw new WrongArraySizeException("Array should be of size("+nx+1+","+ny+1+") but is of size("+array.length+","+array[0].length+")");
		}
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1);
		initTrellis(position, array);
		rectangleMesh.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
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
	public Trellis(float width, float length, int nx, int ny, float [][] array, Texture tex) throws WrongArraySizeException {
		super(TRELLIS_DEFAULT_NAME);
		subelements = null;
		this.width = width;
		this.length = length;
		this.nx = nx;
		this.ny = ny;
		Vector4 position = new Vector4(-width/2,-length/2,0,0);
		if ((array.length != nx+1) || (array[0].length != ny+1)) {
			throw new WrongArraySizeException("Array should be of size("+nx+1+","+ny+1+") but is of size("+array.length+","+array[0].length+")");
		}
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1, tex);
		initTrellis(position, array);
		rectangleMesh.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);
	}

	protected void initTrellis(Vector4 position) {
				
		// Create Vertices
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				rectangleMesh.getVertex(i, j).setPos(new Vector4(i*width/nx, j*length/ny, 0, 1).plus(position));
			}
		}
	}

	protected void initTrellis(Vector4 position, float [][] array) throws WrongArraySizeException {
		
		// Create Vertices
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				rectangleMesh.getVertex(i, j).setPos(new Vector4(i*width/nx, j*length/ny, array[i][j], 1).plus(position));
			}
		}
	}
	
	public int getNx() {
		return nx;
	}
	
	public int getNy() {
		return ny;
	}
	
	public float getWidth() {
		return width;
	}
	
	public float getLength() {
		return length;
	}
	
	public String toString() {
		return super.toString()+"\nwidth: "+width+", length: "+length+", nx: "+nx+", ny: "+ny+"\nV[0,0]="+rectangleMesh.getVertex(0,0)+"\nV[nx,0]="+rectangleMesh.getVertex(nx,0)+"\nV[0,ny]="+rectangleMesh.getVertex(0,ny)+"\nV[nx,ny]="+rectangleMesh.getVertex(nx,ny);
	}

	@Override
	public void calculateNormals() {

		// This algorithm takes into account only the N, S, W and E surrounding vertices to calculate the normal vector of a given Vertex of the Trellis.
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				// Calculate 2 vectors averaging the plane around the considered Vertex
				// One is aligned on the X axis
				Vector4 xa, xb, ya, yb, xavg, yavg;
				
				if (i>0) {
					xa = rectangleMesh.getVertex(i-1, j).getPos();
				} else {
					xa = rectangleMesh.getVertex(i,j).getPos(); // When on the side: take the vertex itself to calculate the average
				}
				if (i<nx) {
					xb = rectangleMesh.getVertex(i+1,j).getPos();
				} else {
					xb = rectangleMesh.getVertex(i,j).getPos(); // When on the side: take the vertex itself to calculate the average
				}
				xavg = xb.minus(xa);
				
				// The second one is aligned on the Y axis
				if (j>0) {
					ya = rectangleMesh.getVertex(i,j-1).getPos();
				} else {
					ya = rectangleMesh.getVertex(i,j).getPos(); // When on the side: take the vertex itself to calculate the average
				}
				if (j<ny) {
					yb = rectangleMesh.getVertex(i,j+1).getPos();
				} else {
					yb = rectangleMesh.getVertex(i,j).getPos(); // When on the side: take the vertex itself to calculate the average
				}
				yavg = yb.minus(ya);
								
				// The normal vector is the cross product of both vectors.
				Vector4 normal = xavg.times(yavg);
				// Need to be normalized
				rectangleMesh.getVertex(i,j).setNormal(normal.normalize().V3());
			}
		}
		calculateSubNormals();
	}
}
