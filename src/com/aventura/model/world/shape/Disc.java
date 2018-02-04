package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.CircularMesh;

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
 * @author Olivier BARRY
 * @since Feb 2018
 */
public class Disc extends Element {

	protected static final String DISC_DEFAULT_NAME = "disc";

	protected CircularMesh mesh;
	float ray;
	int half_seg;
	protected Vector4 center;
	
	protected Texture tex = null;
		
	/**
	 * Create a box aligned on axis. Need to be rotated for a different orientation.
	 * 
	 * @param ray ray of the disc
	 * @param half_seg is half the number of segments for 360 degrees circle
	 */
	public Disc(float ray, int half_seg) {
		super(DISC_DEFAULT_NAME, false); // A Disc is *not* a closed Element
		subelements = null;
		this.ray = ray;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		createDisc(null);
	}


	/**
	 * Creation of a Disc
	 * @param t the Texture to apply
	 */	
	protected void createDisc(Texture t) {
		
		mesh = new CircularMesh(this,half_seg*2, t); // (n) x 2 vertices on each circles + 1 duplicate Vertex for RectangleMesh / Texture
		
		float alpha = (float)Math.PI/half_seg;
		
		// Create vertices
		
		// Create summits (same Vertex for all summits)
		Vector4 summit = new Vector4(0, 0, 0,  1);
		mesh.setCenter(summit);
		
		// Create bottom vertices
		for (int i=0; i<=half_seg*2; i++) {
			
			float sina = (float)Math.sin(alpha*i);
			float cosa = (float)Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			mesh.getVertex(i).setPos(new Vector4(ray*cosa, ray*sina, 0, 1).plus(center));
		}
		
		// Create Triangles
		mesh.createTriangles(CircularMesh.MESH_CIRCULAR_CUT_TEXTURE);	
	}
}
