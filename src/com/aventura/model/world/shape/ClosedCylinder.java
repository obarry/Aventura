package com.aventura.model.world.shape;

import java.awt.Color;

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
 * A Cylinder, closed by (optional) top and bottom like a can
 * 
 * @author Olivier BARRY
 * @since March 2018
 */

public class ClosedCylinder extends Cylinder {
	
	protected static final String CLOSED_CYLINDER_DEFAULT_NAME = "closed cylinder";

	protected Texture bottom_tex, top_tex = null;
	protected Color bottom_col, top_col = null;
	protected CircularMesh top_mesh, bottom_mesh = null;

	public ClosedCylinder(float height, float ray, int half_seg) {
		super(height, ray, half_seg);
		this.isClosed = true;
		this.name = CLOSED_CYLINDER_DEFAULT_NAME;
	}
	
	public ClosedCylinder(float height, float ray, int half_seg, Texture tex) {
		super(height, ray, half_seg, tex);
		this.isClosed = true;
		this.name = CLOSED_CYLINDER_DEFAULT_NAME;
	}
	
	public void generate() {
		
		// Rely on superclass to generate the cylinder
		super.generate();
		
		// Then create specific parts of ClosedCylinder: top and bottom discs
		top_mesh = new CircularMesh(this,half_seg*2, top_tex); // (n) x 2 vertices on each circles + 1 duplicate Vertex for RectangleMesh / Texture
		bottom_mesh = new CircularMesh(this,half_seg*2, bottom_tex); // (n) x 2 vertices on each circles + 1 duplicate Vertex for RectangleMesh / Texture
		
		float alpha = (float)Math.PI/half_seg;
		
		// Create vertices for top and bottom
		
		// Create summits (same Vertex for all summits)
		top_mesh.setCenter(top_center);
		bottom_mesh.setCenter(bottom_center);
		
		// Create the ring of vertices around the disc
		for (int i=0; i<half_seg*2; i++) {
			
			float cosa = (float) Math.cos(alpha*i);
			float sina = (float) Math.sin(alpha*i);
			
			// Set the position of the corresponding vertex in the CircularMesh
			top_mesh.getVertex(i).setPos(new Vector4(this.ray*cosa, this.ray*sina, 0, 1).plus(top_center));
			bottom_mesh.getVertex(i).setPos(new Vector4(this.ray*cosa, this.ray*sina, 0, 1).plus(bottom_center));
		}
		
		// Create Triangles
		top_mesh.createTriangles(CircularMesh.MESH_CIRCULAR_CUT_TEXTURE);	
		bottom_mesh.createTriangles(CircularMesh.MESH_CIRCULAR_CUT_TEXTURE);
		
		
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

}
