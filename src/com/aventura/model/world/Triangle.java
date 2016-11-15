package com.aventura.model.world;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016 Olivier BARRY
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
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Triangle {
	
	// Made of 3 vertices
	protected Vertex v1;
	protected Vertex v2;
	protected Vertex v3;
	
	// And an optional Normal to the triangle (else Vertices normal  is used)
	protected Vector3 normal = null;
	
	public Triangle() {
		this.v1 = null;
		this.v2 = null;
		this.v3 = null;		
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

	public Vertex getV2() {
		return v2;
	}

	public Vertex getV3() {
		return v3;
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
		this.normal = n.getVector3();
	}
	
	/**
	 * Calculate the Normal as V1V2 ^ V1V3 (normalized)
	 * The direction of the normal is resulting from this cross product 
	 */
	public void calculateNormal() {
		//P = V1V2 as a Vector3
		Vector3 p = (v2.position.minus(v1.position)).getVector3();
		//Vector4 p = v2.position.minus(v1.position);
		//P = V1V3 as a Vector3
		Vector3 q = (v3.position.minus(v1.position)).getVector3();
		//Vector4 q = v3.position.minus(v1.position);
		// Calculate the cross product
		normal = p.times(q);
		//Vector4 n = p.times(q);
		// Normalize the resulting Vector3
		normal.normalize();
		//n.normalize();
		//normal = n.getVector3();
	}
	
}
