package com.aventura.model.world;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

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
 * @since May 2016
 */
public class Triangle {
	
	// Made of 3 vertices
	protected Vertex v1;
	protected Vertex v2;
	protected Vertex v3;
	
	// And an optional Normal to the triangle (default is normal at vertices level)
	protected boolean triangleNormal = false;
	protected Vector3 normal = null;
	
	// Projected normals
	protected Vector3 wld_normal = null; // Normal in World coordinates
	protected Vector3 prj_normal = null; // Normal in Homogeneous (clip) coordinates
	
	// Color if at triangle level
	protected Color color = null;
	
	public Triangle() {
		this.v1 = null;
		this.v2 = null;
		this.v3 = null;		
	}
	
	/**
	 * Duplicate triangle by creating new Vertices and normal by duplication of original vertices and normal
	 * @param t the triangle to duplicate
	 */
	public Triangle(Triangle t) {
		this.v1 = new Vertex(t.getV1());
		this.v2 = new Vertex(t.getV2());
		this.v3 = new Vertex(t.getV3());
		
		this.triangleNormal = t.triangleNormal;
		this.normal = (t.normal != null) ? new Vector3(t.normal) : null;
		this.color =  t.color;
	}
	
	public Triangle(Vertex v1, Vertex v2, Vertex v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}
	
	public String toString() {
		return ("Triangle vertices:\n"+" v1: "+v1+"\n v2: "+v2+"\n v3: "+v3);
	}

	public Vertex getV1() {
		return v1;
	}
	
	public boolean isTriangleNormal() {
		return triangleNormal;
	}
	
	public void setTriangleNormal(boolean b) {
		this.triangleNormal = b;
	}

	public Vertex getV2() {
		return v2;
	}

	public Vertex getV3() {
		return v3;
	}
	
	/**
	 * Create and return a new Vertex corresponding to the (bary)center of the triangle
	 * @return
	 */
	public Vertex getCenter() {
		Vector4 p = (v1.getPos().plus(v2.getPos()).plus(v3.getPos())).times((double)1/3);
		Vertex c = new Vertex(p);
		return c;
	}
	
	public void setV1(Vertex v) {
		this.v1 = v;
	}

	public void setV2(Vertex v) {
		this.v2 = v;
	}
	
	public void setV3(Vertex v) {
		this.v3 = v;
	}
	
	public void setNormal(Vector3 n) {
		this.normal = n;
	}
	
	public Vector3 getNormal() {
		return normal;
	}
	
	public void setNormal(Vector4 n) {
		this.normal = n.V3();
	}
	
	public void setWorldNormal(Vector3 n) {
		wld_normal = n;
	}
	
	public Vector3 getWorldNormal() {
		return wld_normal;
	}
	
	public void setProjNormal(Vector3 n) {
		prj_normal = n;
	}
	public Vector3 getProjNormal() {
		return prj_normal;
	}

	public void setColor(Color c) {
		this.color = c;
	}
	
	public Color getColor() {
		return color;
	}
	
	/**
	 * Calculate the Normal as V1V2 ^ V1V3 (normalized)
	 * The direction of the normal is resulting from this cross product 
	 */
	public void calculateNormal() {
		//P = V1V2 as a Vector3
		Vector3 p = (v2.position.minus(v1.position)).V3();

		//P = V1V3 as a Vector3
		Vector3 q = (v3.position.minus(v1.position)).V3();

		// Calculate the cross product
		normal = p.times(q);

		// Normalize the resulting Vector3
		normal.normalize();
	}
	
}
