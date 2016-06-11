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
 * A Vertex with a Normal Vector - this is the general case
 * Generally associated with a Triangle that has no specific normal vector
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class NVertex extends Vertex {
	
	protected Vector3 normal;
	
	public NVertex(Vector4 p, Vector3 normal) {
		super(p);
		this.normal = normal;
	}

	public Vector3 getNormal() {
		return normal;
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

	public void setNormal(Vector3 n) {
		normal = n;
	}

}
