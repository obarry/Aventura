package com.aventura.model.world.triangle;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
 * A triangle has normals at Vertex level by default (3 different normals) to define a continuous surface.
 * But it can also have a normal at Triangle level (which then will supersede the Vertex normals) to define a non-continuous surface.
 * This is typically used for elements like cubes, rectangles and pyramids : they have sharp edges from one face to the other so the 
 * Vertex normals are meaningless in this situation (Vertex are shared by 2 or more non-continuous faces of these elements).
 * 
 * @author Olivier BARRY
 * @since May 2016
 */
public class Triangle {
	
	// ---------
	// Constants
	// ---------
	public static final int TEXTURE_ISOTROPIC = 0; // Default
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
	
	// And an optional Normal to the triangle (default is normal at vertices level)
	protected boolean triangleNormal = false;
	protected Vector3 normal = null;
	
	// Projected normals
	protected Vector3 wld_normal = null; // Normal in World coordinates
	//protected Vector3 prj_normal = null; // Normal in Homogeneous (clip) coordinates
	
	// Recto Verso characteristics (should the face of this Triangle opposite to normal(s) be displayed?)
	protected boolean rectoVerso = false; // Default is false (closed Elements) but can be set to true for open Elements
	
	// ------------------------
	// Physical characteristics
	// ------------------------
	
	// Texture
	Texture tex = null;
	// Flag for isotropic, vertical or horizontal texture interpolation of this triangle, default is ISOTROPIC
	protected int tex_orientation = TEXTURE_ISOTROPIC;
	
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
		this.tex = t.tex;
		this.tex_orientation = t.tex_orientation;
		this.rectoVerso = t.rectoVerso;
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

	/**
	 * @return true if a normal at triangle level exists (and supersedes the normals at Vertex level)
	 */
	public boolean isTriangleNormal() {
		return triangleNormal;
	}
	
	/**
	 * Use to indicate that the triangle has a triangle normal : this will supersede the other normals at vertex level
	 * @param b boolean to indicate if there is a triangle normal (true) or not (false)
	 */
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
		Vector4 p = (v1.getPos().plus(v2.getPos()).plus(v3.getPos())).times((float)1/3);
		Vertex c = new Vertex(p);
		return c;
	}
	
	public Vector4 getCenterWorldPos() {
		return (v1.getWorldPos().plus(v2.getWorldPos()).plus(v3.getWorldPos())).times((float)1/3);
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
	
	// restored 11/7/2023
//	public void setProjNormal(Vector3 n) {
//		prj_normal = n;
//	}
//	public Vector3 getProjNormal() {
//		return prj_normal;
//	}

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

	public boolean isRectoVerso() {
		return rectoVerso;
	}

	public void setRectoVerso(boolean rectoVerso) {
		this.rectoVerso = rectoVerso;
	}


	/**
	 * Calculate the Normal as V1V2 ^ V1V3 (normalized)
	 * The direction of the normal is resulting from this cross product 
	 */
	public void calculateNormal() {
		//P = V1V2 as a Vector3
		Vector3 p = (v2.getPos().minus(v1.getPos())).V3();

		//P = V1V3 as a Vector3
		Vector3 q = (v3.getPos().minus(v1.getPos())).V3();

		// Calculate the cross product
		normal = p.times(q);

		// Normalize the resulting Vector3
		normal.normalize();
	}
	
	/**
	 * Is true if at least one Vertex of the Triangle is in the GUIView Frustum in homogeneous coordinates
	 * Assumes that the projection of the vertices has been done previously
	 * @param t the Triangle
	 * @return true if triangle is at least partially inside the GUIView Frustum, else false
	 */
	public boolean isInViewFrustum() {

		// Need at least one vertice to be in the gUIView frustum
		if (v1.isInViewFrustum() || v2.isInViewFrustum() || v3.isInViewFrustum())
			return true;
		else
			return false;
	}

	
}
