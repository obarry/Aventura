package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector3;
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
 * Create a frustum of Cone made of 2 circles around Z axis
 * Bottom circle is at z = -cone_height / 2
 * Virtual summit is at z = cone_height / 2
 * Top circle is at z = frustum_height - (cone_height / 2)
 * The diameter of the cone is ray * 2
 * It is made of 2 * half_seg Vertices per circle and 2 * 2 * half_seg Triangles (each face of the cone frustum is made of 2 triangles)
 * 
 * The Cone Frustum, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix
 * 
 * @author Olivier BARRY
 * @since January 2017
 */

public class ConeFrustum extends Element {
	
	protected Vertex[][] vertices;
	protected Vertex summit;
	float cone_height, frustum_height, mid_height;
	float ray;
	int half_seg;
	protected Vector4 center, top_center, bottom_center;
	
	/**
	 * Default creation of a Cone Frustum around Z axis 
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 */
	public ConeFrustum(float cone_height, float frustum_height, float ray, int half_seg) {
		super();
		subelements = null;
		this.ray = ray;
		this.cone_height = cone_height;
		this.frustum_height = frustum_height;
		this.mid_height = frustum_height/2;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		this.top_center = new Vector4(0,0,(frustum_height-(cone_height/2)),0);
		this.bottom_center = new Vector4(0,0,-cone_height/2,0);
		createConeFrustum();
	}

	/**
	 * Creation of a Cylinder moved to a given center
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 * @param center to which the Vertices are moved at creation (Vector3)
	 */
	public ConeFrustum(float cone_height, float frustum_height, float ray, int half_seg, Vector3 center) {
		super();
		subelements = null;
		this.ray = ray;
		this.cone_height = cone_height;
		this.frustum_height = frustum_height;
		this.mid_height = frustum_height/2;
		this.half_seg = half_seg;
		this.center = new Vector4(center);
		this.top_center = new Vector4(0,0,(frustum_height-(cone_height/2)),0);
		this.bottom_center = new Vector4(0,0,-cone_height/2,0);
		createConeFrustum();
	}
	
	/**
	 * Creation of a Cylinder moved to a given center
	 * @param height of the Cylinder
	 * @param ray of the top and bottom circles of the Cylinder
	 * @param half_seg is half the number of segments for 360 degrees circles
	 * @param center to which the Vertices are moved at creation (Vector4)
	 */
	public ConeFrustum(float cone_height, float frustum_height, float ray, int half_seg, Vector4 center) {
		super();
		subelements = null;
		this.ray = ray;
		this.cone_height = cone_height;
		this.frustum_height = frustum_height;
		this.mid_height = frustum_height/2;
		this.half_seg = half_seg;
		this.center = center;
		this.top_center = new Vector4(0,0,(frustum_height-(cone_height/2)),0);
		this.bottom_center = new Vector4(0,0,-cone_height/2,0);
		createConeFrustum();
	}

	
	protected void createConeFrustum() {
		
		vertices = new Vertex[half_seg*2][3]; // (n) x 3 vertices on each circles
		float alpha = (float)Math.PI/half_seg;
		float beta = alpha/2;
		
		// Create vertices
		
		// Create summits (same Vertex for all summits)
		summit = createVertex(new Vector4(0, 0, cone_height/2,  1).plus(center));
		
		// Create circle vertices
		for (int i=0; i<half_seg*2; i++) {
			
			float sina = (float)Math.sin(alpha*i);
			float cosa = (float)Math.cos(alpha*i);
			
			// Bottom circle of the cone
			vertices[i][0] = createVertex(new Vector4(ray*cosa, ray*sina, -cone_height/2, 1).plus(center));
			
			// Top circle of the cone
			float ratio = (cone_height - frustum_height)/cone_height;
			vertices[i][2] = createVertex(new Vector4(ratio*ray*cosa, ratio*ray*sina, (frustum_height-(cone_height/2)), 1).plus(center));
			
			// Middle circle of the cylinder
			float sinb = (float)Math.sin(alpha*i+beta);
			float cosb = (float)Math.cos(alpha*i+beta);
			ratio = (cone_height - mid_height)/cone_height;
			vertices[i][1] = createVertex(new Vector4(ratio*ray*cosb, ratio*ray*sinb, (mid_height-(cone_height/2)), 1).plus(center));
		}
		
		// Create Triangles
		
		// V[i][2] +---+ V[i+1][2]
		//        /| T3|\
		//       /  | |  \
		//      / T2 + T4 \ V[i][1]
		//     /   /   \   \
		//    / /   T1   \  \
		//    +-------------+
		// V[i][0]      V[i+1][0]
		
		Triangle t1, t2, t3, t4; // local variable
		for (int i=0; i<half_seg*2-1; i++) {
			
			// For each face of the cone, create 4 triangles
			t1 = new Triangle(vertices[i][0], vertices[i+1][0], vertices[i][1]);
			t2 = new Triangle(vertices[i][0], vertices[i][1], vertices[i][2]);
			t3 = new Triangle(vertices[i][2], vertices[i][1], vertices[i+1][2]);
			t4 = new Triangle(vertices[i+1][0], vertices[i+1][2], vertices[i][1]);

			// Add triangles to Element
			this.addTriangle(t1);			
			this.addTriangle(t2);			
			this.addTriangle(t3);			
			this.addTriangle(t4);			
		}
		// Create 4 last triangles (i->half_seg*2-1, i+1->0)
		t1 = new Triangle(vertices[half_seg*2-1][0], vertices[0][0], vertices[half_seg*2-1][1]);
		t2 = new Triangle(vertices[half_seg*2-1][0], vertices[half_seg*2-1][1], vertices[half_seg*2-1][2]);
		t3 = new Triangle(vertices[half_seg*2-1][2], vertices[half_seg*2-1][1], vertices[0][2]);
		t4 = new Triangle(vertices[0][0], vertices[0][2], vertices[half_seg*2-1][1]);
		
		// Add last triangles
		this.addTriangle(t1);			
		this.addTriangle(t2);			
		this.addTriangle(t3);			
		this.addTriangle(t4);			
	}

	@Override
	public void calculateNormals() {
		Vector4 u, n;
			
		// Create normals of vertices
		for (int i=0; i<half_seg*2; i++) {
			
			// For each bottom and top Vertex, calculate a ray vector that is orthogonal to the slope of the cone
			// u = OS^OP (O = bottom center, S = summit, P = bottom Vertex)
			u = (summit.getPos().minus(bottom_center)).times(vertices[i][0].getPos().minus(bottom_center));
			n = (vertices[i][0].getPos().minus(summit.getPos())).times(u);
			n.normalize();
			vertices[i][0].setNormal(n.V3());
			vertices[i][2].setNormal(n.V3());
			
			// For each middle Vertex
			if (i==half_seg*2-1) { // Last one
				n = vertices[0][2].getPos().minus(vertices[half_seg*2-1][0].getPos()).times(vertices[half_seg*2-1][2].getPos().minus(vertices[0][0].getPos()));				
				n.normalize();
			} else {
				n = vertices[i+1][2].getPos().minus(vertices[i][0].getPos()).times(vertices[i][2].getPos().minus(vertices[i+1][0].getPos()));
			}
			n.normalize();
			vertices[i][1].setNormal(n.V3());
		}
		calculateSubNormals();
	}
}
