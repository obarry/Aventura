package com.aventura.model.world.shape;


import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.FanMesh;


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

 * Create a Cone made of 1 summit and a base, a circle around Z axis
 * Summit is at z = height / 2
 * Circle is at z = -height / 2
 * The diameter of the Circle is ray * 2
 * It is made of 2 * half_seg Vertices for the circle and the same number of Triangles linked with summit
 * 
 * The Cone, as any Element, can then be moved, rotated and transformed thanks to the Transformation matrix

 * @author Olivier BARRY
 * @since October 2016
 */

public class Cone extends Element {

	protected FanMesh mesh;
	double height;
	double ray;
	int half_seg;
	protected Vector4 center, bottom_center;
	
	/**
	 * Default creation of a Cone around Z axis 
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 */
	public Cone(double height, double ray, int half_seg) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		this.bottom_center = new Vector4(0,0,-height/2,0);
		this.center = new Vector4(0,0,0,0);
		createCone(null);
	}

	/**
	 * Default creation of a Cone around Z axis 
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 * @param tex the texture to wrap this Cone
	 */
	public Cone(double height, double ray, int half_seg, Texture tex) {
		super();
		subelements = null;
		this.ray = ray;
		this.height = height;
		this.half_seg = half_seg;
		this.center = new Vector4(0,0,0,0);
		this.bottom_center = new Vector4(0,0,-height/2,0);
		this.center = new Vector4(0,0,0,0);
		createCone(tex);
	}
	/**
	 * Creation of a Cone moved to a given position
	 * @param height of the Cone
	 * @param ray of the base circle of the Cone
	 * @param half_seg is half the number of segments for 360 degrees circle
	 * @param center to which the Cone is moved at creation (Vector3)
	 */

	
	protected void createCone(Texture t) {
		
		mesh = new FanMesh(this,half_seg*2+1, t); // (n) x 2 vertices on each circles + 1 duplicqte Vertex for RectangleMesh / Texture
		
		double alpha = Math.PI/half_seg;
		
		// Create vertices
		
		// Create summits (same Vertex for all summits)
		Vector4 summit = new Vector4(0, 0, height/2,  1);
		mesh.setSummit(summit);
		
		// Create bottom vertices
		for (int i=0; i<=half_seg*2; i++) {
			
			double sina = Math.sin(alpha*i);
			double cosa = Math.cos(alpha*i);
			
			// Bottom circle of the cylinder
			mesh.getVertex(i).setPos(new Vector4(ray*cosa, ray*sina, -height/2, 1).plus(center));
		}
		
		// Create Triangles
		mesh.createTriangles(FanMesh.MESH_ORIENTED_TRIANGLES);	
	}
	
	@Override
	public void calculateNormals() {
		Vector4 n, u;
		Vector4 summit = mesh.getSummit(0).getPos();
		// Create normals of vertices
		for (int i=0; i<=half_seg*2; i++) {

			// For each bottom Vertex, calculate a ray vector that is orthogonal to the slope of the cone
			// u = OS^OP (O = bottom center, S = summit, P = bottom Vertex)
			u = (summit.minus(bottom_center)).times(mesh.getVertex(i).getPos().minus(bottom_center));
			n = (mesh.getVertex(i).getPos().minus(summit)).times(u);
			n.normalize();
			mesh.getVertex(i).setNormal(n.V3());
			
			// For each summit, use the ray vector from top center to the Vertex and normalize it
			if (i<half_seg*2) {
				n = (mesh.getVertex(i).getPos().minus(summit)).times(mesh.getVertex(i+1).getPos().minus(summit));
				n.normalize();
				mesh.getSummit(i).setNormal(n.V3());
			}
		}
		calculateSubNormals();
	}
}
