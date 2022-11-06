package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.RectangleMesh;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2022 Olivier BARRY
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
 * Create a Cylinder made of 2 circles around Z axis
 * Top circle is at z = height / 2
 * Bottom circle is at z = -height / 2
 * The diameter of the cylinder is ray * 2
 * It is made of 2 * half_seg Vertices per circle and 2 * 2 * half_seg Triangles (each face of the cylinder is made of 2 triangles)
 * 
 * The Cylinder, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix
 * 
 * @author Olivier BARRY
 * @since October 2016
 */


public class Cylinder extends Element {
	
	protected static final String CYLINDER_DEFAULT_NAME = "cylinder";
	
	protected RectangleMesh rectangleMesh;
	float height;
	float ray;
	int half_seg;
	protected Vector4 top_center, bottom_center;
	protected Texture tex = null;
	
	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 */
	public Cylinder(float height, float ray, int half_seg) {
		super(CYLINDER_DEFAULT_NAME);
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
	}

	/**
	 * Default creation of a Cylinder around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 * @param tex the Texture to wrap this Cylinder
	 */
	public Cylinder(float height, float ray, int half_seg, Texture tex) {
		super(CYLINDER_DEFAULT_NAME);
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.tex = tex;
	}
	
	public void createGeometry() {
		
		// Create centers
		this.top_center = new Vector4(0,0,height/2,0);
		this.bottom_center = new Vector4(0,0,-height/2,0);

		// Create mesh to wrap Cylinder
		rectangleMesh = new RectangleMesh(this, half_seg*2+1, 2, tex); // (n) x 2 vertices on each circles + 1 x 2 duplicate Vertex for RectangleMesh / Texture
		float alpha = (float)Math.PI/half_seg;
		
		// Create vertices of the mesh
		for (int i=0; i<=half_seg*2; i++) { // (n) * 2 + 1 steps -> [0, 2*PI]
			
			float sina = (float)Math.sin(alpha*i);
			float cosa = (float)Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			rectangleMesh.getVertex(i, 0).setPos(new Vector4(ray*cosa, ray*sina, -height/2, 1));
			
			// Top circle of the cylinder
			rectangleMesh.getVertex(i,1).setPos(new Vector4(ray*cosa, ray*sina, height/2, 1));
		}
		
		// Create Triangles
		rectangleMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);

	}

	@Override
	public void calculateNormals() {
		Vector3 n;
			
		// Create normals of vertices
		for (int i=0; i<=half_seg*2; i++) {
			// For each bottom Vertex, use the ray vector from bottom center to the Vertex and normalize it 
			n = rectangleMesh.getVertex(i,0).getPos().minus(bottom_center).V3();
			n.normalize();
			rectangleMesh.getVertex(i,0).setNormal(n);
			// Same normal vector can be used for the corresponding top Vertex
			rectangleMesh.getVertex(i,1).setNormal(n);
		}
		
		//calculateSubNormals();
	}
}
