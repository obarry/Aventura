/*
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2026 Olivier BARRY
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
 */
package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.triangle.RectangleMesh;

/**
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
public class Trellis extends GenerativeElement {
	
	protected static final String TRELLIS_DEFAULT_NAME = "trellis";

	protected RectangleMesh rectangleMesh;
	protected int nx, ny;
	protected float width, length;
	protected Vector3 position; // Offset of the vertices from the Element's origin (a displacement, hence a Vector3)

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
		this.position = new Vector3(-width/2,-length/2,0);
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
		this.position = new Vector3(-width/2,-length/2,0);
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1, tex);
		initTrellis(position);
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
		this.position = new Vector3(-width/2,-length/2,0);
		if ((array.length != nx+1) || (array[0].length != ny+1)) {
			throw new WrongArraySizeException("Array should be of size("+nx+1+","+ny+1+") but is of size("+array.length+","+array[0].length+")");
		}
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1);
		initTrellis(position, array);
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
		this.position = new Vector3(-width/2,-length/2,0);
		if ((array.length != nx+1) || (array[0].length != ny+1)) {
			throw new WrongArraySizeException("Array should be of size("+nx+1+","+ny+1+") but is of size("+array.length+","+array[0].length+")");
		}
		rectangleMesh = new RectangleMesh(this, nx+1, ny+1, tex);
		initTrellis(position, array);
	}
	
	/**
	 * Repeats the texture of this Trellis like tiles: repeatX times along the X axis and repeatY times along the Y
	 * axis, instead of once over the whole Trellis (the default). This keeps the texture sharp on a large surface: a
	 * ground of 200 m with a texture repeated 20 times shows it every 10 m.
	 * <pre>
	 *   Texture grass = new Texture("grass.jpg").setWrapMode(Texture.WrapMode.MIRROR);
	 *   Trellis ground = new Trellis(200, 200, 40, 40, altitudes, grass);
	 *   ground.setTextureRepeat(20, 20);
	 * </pre>
	 * The Texture must have a wrap mode that repeats it (Texture.setWrapMode()): MIRROR for any image, REPEAT for an
	 * image made to be repeated. The numbers of repetitions need not be integers, nor related to the number of
	 * segments. To be called before the geometry is generated (World.build() or generate()).
	 */
	public void setTextureRepeat(float repeatX, float repeatY) {
		rectangleMesh.setTextureRepeat(repeatX, repeatY);
	}

	public void generateVertices() {
		// Nothing to do, all is already done by constructor
	}
	
	public void generateTriangles() {
		rectangleMesh.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);				
	}

	
//	public void createGeometry() {
//		rectangleMesh.createTriangles(RectangleMesh.MESH_ALTERNATE_TRIANGLES);		
//	}

	protected void initTrellis(Vector3 position) {
				
		// Create Vertices
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				rectangleMesh.getVertex(i, j).setPos(new Vector4(i*width/nx, j*length/ny, 0, 1).plus(position));
			}
		}
	}

	protected void initTrellis(Vector3 position, float [][] array) throws WrongArraySizeException {
		
		// Create Vertices
		for (int i=0; i<=nx; i++) {
			for (int j=0; j<=ny; j++) {
				rectangleMesh.getVertex(i, j).setPos(new Vector4(i*width/nx, j*length/ny, array[i][j], 1).plus(position));
			}
		}
	}
	
	public void updateTrellis(float [][] array) throws WrongArraySizeException {
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
		return super.toString()+"\n* width: "+width+", length: "+length+", nx: "+nx+", ny: "+ny+"\n* V[0,0]="+rectangleMesh.getVertex(0,0)+"\n* V[nx,0]="+rectangleMesh.getVertex(nx,0)+"\n* V[0,ny]="+rectangleMesh.getVertex(0,ny)+"\n* V[nx,ny]="+rectangleMesh.getVertex(nx,ny);
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
				Vector4 normal = xavg.cross(yavg);
				// Need to be normalized
				rectangleMesh.getVertex(i,j).setNormal(normal.normalize().V3());
			}
		}
		// Sub-Element recursion is build()/rebuild()'s job, not calculateNormals()'s -- see Generable's
		// Javadoc for the single rule. Calling calculateSubNormals() here used to double-compute it.
	}
	
	/**
	 * Set Altitude for one point (x,y) of the Trellis
	 * @param x
	 * @param y
	 * @param altitude
	 */
	public void setZ(int x, int y, float altitude) {
		// Select the corresponding Vertex in the RectangleMesh and set the Z altitude of the Vertex
		rectangleMesh.getVertex(x, y).getPos().setZ(altitude);
	}
}
