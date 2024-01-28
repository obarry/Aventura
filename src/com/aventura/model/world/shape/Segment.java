package com.aventura.model.world.shape;

import com.aventura.math.vector.Vector4;
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
 * @author Olivier BARRY
 * @since October 2016
 */

public class Segment {
	
	// Made of 2 vertices
	Vertex v1;
	Vertex v2;
	
	public Segment() {
		this.v1 = null;
		this.v2 = null;
	}
	
	public Segment(Vertex v1, Vertex v2) {
		this.v1 = v1;
		this.v2 = v2;
	}
	
	public Segment(Vector4 u1, Vector4 u2) {
		this.v1 = new Vertex(u1);
		this.v2 = new Vertex(u2);
	}
	
	public String toString() {
		return ("Segment vertices:\n"+" v1: "+v1+"\n v2: "+v2);
	}

	public Vertex getV1() {
		return v1;
	}

	public Vertex getV2() {
		return v2;
	}

	public void setV1(Vertex v) {
		this.v1 = v;
	}

	public void setV2(Vertex v) {
		this.v2 = v;
	}
}
