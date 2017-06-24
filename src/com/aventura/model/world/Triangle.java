package com.aventura.model.world;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;

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
 * Most basic surface element, the Triangle can be rendered autonomously without any other information
 * than those contained in its attributes.
 * 
 * @author Olivier BARRY
 * @since May 2016
 */
public class Triangle {
	
	// ---------
	// Constants
	// ---------
	public static final int TEXTURE_ISOTROPIC = 0;
	public static final int TEXTURE_VERTICAL = 1;
	public static final int TEXTURE_HORIZONTAL = 2;
	
	// --------
	// Geometry
	// --------
	
	// Made of 3 vertices
	protected Vertex v1;
	protected Vertex v2;
	protected Vertex v3;
	
	// The relative position of each Vertex in an optional Texture (in homogeneous coordinates [0,1])protected Vector2 t1;
	protected Vector4 t1;
	protected Vector4 t2;
	protected Vector4 t3;
	
	// Flag for isotropic, vertical or horizontal texture interpolation of this triangle, default is ISOTROPIC
	protected int tex_orientation = TEXTURE_ISOTROPIC;
	
	// And an optional Normal to the triangle (default is normal at vertices level)
	protected boolean triangleNormal = false;
	protected Vector3 normal = null;
	
	// Projected normals
	protected Vector3 wld_normal = null; // Normal in World coordinates
	protected Vector3 prj_normal = null; // Normal in Homogeneous (clip) coordinates
	
	// ------------------------
	// Physical characteristics
	// ------------------------
	
	// Color if at triangle level
	protected Color color = null;
	
	// Texture
	Texture tex = null;
	
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
		this.tex = t.tex;
	}
	
	/**
	 * Create a Triangle from 3 Vertices
	 * @param v1 first Vertex
	 * @param v2 second Vertex
	 * @param v3 third Vertex
	 */
	public Triangle(Vertex v1, Vertex v2, Vertex v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}
	
	/**
	 * Create a Triangle from 3 Vertices and a Texture
	 * Default orientation for the Texture is TEXTURE_ISOTROPIC
	 * @param v1 first Vertex
	 * @param v2 second Vertex
	 * @param v3 third Vertex
	 */
	public Triangle(Vertex v1, Vertex v2, Vertex v3, Texture t) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.tex = t;
	}
	
	/**
	 * Create a Triangle from 3 Vertices and a Texture
	 * Specify orientation for the Texture as one of TEXTURE_ISOTROPIC, TEXTURE_VERTICAL or TEXTURE_HORIZONTAL
	 * @param v1 first Vertex
	 * @param v2 second Vertex
	 * @param v3 third Vertex
	 */
	public Triangle(Vertex v1, Vertex v2, Vertex v3, Texture t, int to) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.tex = t;
		this.tex_orientation = to;
	}
	
	public String toString() {
		return ("Triangle vertices:\n"+" v1: "+v1+"\n v2: "+v2+"\n v3: "+v3);
	}

	public boolean isTriangleNormal() {
		return triangleNormal;
	}
	
	public void setTriangleNormal(boolean b) {
		this.triangleNormal = b;
	}

	/**
	 * Get the first Vertex of this Triangle
	 * @return the first Triangle
	 */
	public Vertex getV1() {
		return v1;
	}
	
	/**
	 * Get the second Vertex of this Triangle
	 * @return the second Triangle
	 */
	public Vertex getV2() {
		return v2;
	}

	/**
	 * Get the third Vertex of this Triangle
	 * @return the third Triangle
	 */
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
	
	/**
	 * Set the first Vertex of this Triangle
	 */
	public void setV1(Vertex v) {
		this.v1 = v;
	}

	/**
	 * Set the second Vertex of this Triangle
	 */
	public void setV2(Vertex v) {
		this.v2 = v;
	}
	
	/**
	 * Set the third Vertex of this Triangle
	 */
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

	public void setTexture(Texture t, Vector4 t1, Vector4 t2 , Vector4 t3) {
		this.tex = t;
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
	}
	
	public void setTexture(Vector4 t1, Vector4 t2 , Vector4 t3) {
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
	}
	
	public Texture getTexture() {
		return tex;
	}

	public Vector4 getTexVec1() {
		return this.t1;
	}

	public Vector4 getTexVec2() {
		return this.t2;
	}

	public Vector4 getTexVec3() {
		return this.t3;
	}
	
	public int getTextureOrientation() {
		return this.tex_orientation;
	}
	
	public void setTextureOrientation(int t) {
		this.tex_orientation = t;
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
