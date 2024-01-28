package com.aventura.model.world;

import java.awt.Color;
import com.aventura.math.vector.*;

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
 * A generic Vertex
 * 
 * @author Olivier BARRY
 * @since May 2016
 */
public class Vertex {
	
	//
	// Static characteristics: Geometry and Physical characteristic
	//
	
	// Original Geometry
	protected Vector4 position = null; // Coordinates of the Vertex. Vector4 as this is a Point (not vector only) in space.
	protected Vector3 normal = null; // Normal of this Vertex, this is context specific and can be kept null if normal at Triangle level
	
	// Physical characteristic
	protected Color color = null; // color of this Vertex, if null the Element's color (or World's color) is used. Lowest level priority.
	// TODO Material
	//protected int material; // To be defined, a specific class may be needed for a complex material representation

	// Reflectivity
	// TODO
		
	//
	// Dynamic characteristics: Projection and Shading
	//
	
	// Projected or calculated Geometry
	protected Vector4 wld_position = null; // Position of this Vertex in World reference (Model to World projection)
	protected Vector4 prj_position = null; // Position of this Vertex in Homogeneous (clip) coordinates (Model to Clip projection)
	protected Vector3 wld_normal = null; // Normal in World coordinates
	protected Vector3 prj_normal = null; // Normal in Homogeneous (clip) coordinates - Not used - Removed 1/1/2022 - restored 11/7/2023
	
	// Lighting and Shading
	protected Color shadedCol = null; // Gouraud's shading at this Vertex, calculated at Rasterization time
	protected Color specularCol = null; // Gouraud's specular reflection at this Vertex, calculated at Rasterization time
	
	/**
	 * Duplicate a Vertex, creating new Vectors for position and normal but keeping references for texture and color
	 * @param v the Vertex to duplicate
	 */
	public Vertex(Vertex v) {
		this.position = (v.position != null) ? new Vector4(v.position) : null;
		this.normal = (v.normal != null) ? new Vector3(v.normal) : null;
		this.color = v.getColor();
		//this.material = v.material; TODO
	}
	
	public Vertex() {
		// Empty Vertex to be used for generation of rectangleMesh of vertices
	}
	
	public Vertex(float x, float y, float z) {
		position = new Vector4(x, y, z, 1);
	}
		
	public Vertex(Vector4 p) {
		position = p;
		normal = null;
	}
		
	public Vertex(Vector3 p) {
		position = new Vector4(p.getX(), p.getY(), p.getZ(), 1);
		normal = null;
	}
	
	public Vertex(Vector4 p, Vector3 n) {
		position = p;
		normal = n;
	}

	public Vertex(Vector4 p, Vector4 n) {
		position = p;
		normal = n.V3();
	}

	public String toString() {
		return "Position: "+position;
	}
	
	public void setPos(Vector4 p) {
		position = p;
	}
	
	public Vector4 getPos() {
		return position;
	}
	
	public void setWorldPos(Vector4 p) {
		wld_position = p;
	}
	
	public Vector4 getWorldPos() {
		return wld_position;
	}
	
	public void setProjPos(Vector4 p) {
		prj_position = p;
	}
	
	public Vector4 getProjPos() {
		return prj_position;
	}
	
	public void setNormal(Vector3 n) {
		normal = n;
	}
	
	public Vector3 getNormal() {
		return normal;
	}
		
	public void setWorldNormal(Vector3 n) {
		wld_normal = n;
	}
	
	public Vector3 getWorldNormal() {
		return wld_normal;
	}
	
	// Not used - Removed 1/1/2022
	// restored 11/7/2023
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
	
	public void setShadedCol(Color c) {
		this.shadedCol = c;
	}
	
	public Color getShadedCol() {
		return shadedCol;
	}
		
	public Color getSpecularCol() {
		return specularCol;
	}

	public void setSpecularCol(Color specularCol) {
		this.specularCol = specularCol;
	}

	/**
	 * Calculate the normal from a set of vertices surrounding this Vertex
	 * @param setOfVertices
	 */
	public void calculateNormal(Vertex[] setOfVertices) {
	
		// Use the position of the other Vertices relative to this Vertex to calculate an average plan and define the normal
		for (int i=0; i<setOfVertices.length; i++) {
			// TODO
		}
	}
	
	/**
	 * Is true if the Vertex is in the View Frustum in homogeneous coordinates
	 * Assumes that the Vertex projection is done
	 * @return true if Vertex is inside the View Frustum, else false
	 */
	public boolean isInViewFrustum() {
		
		// Get homogeneous coordinates of the Vertex
		float x = prj_position.get3DX();
		float y = prj_position.get3DY();
		float z = prj_position.get3DZ();
		
		// Need all (homogeneous) coordinates to be within range [-1, 1]
		if ((x<=1 && x>=-1) && (y<=1 && y>=-1) && (z<=1 && z>=-1))
			return true;
		else
			return false;
	}


}
