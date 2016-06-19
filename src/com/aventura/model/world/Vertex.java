package com.aventura.model.world;

import java.awt.Color;
import com.aventura.math.vector.*;

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
 * A generic Vertex without Normal Vector
 * Normally only used for triangles having a specific normal vector or for generic handling purpose
 * Else the general case is to use NVertex inherited class
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Vertex {
	
	// Geometry
	protected Vector4 position;
	
	// Physical characteristic
	protected Vector2 texture; //relative position of this vertex in the texture plane
	protected Color color; // base color of this vertex
	protected int material; // to be defined, a specific class/object may be needed
	
	// Reflectivity
	// TBD
	
	public Vertex(Vector4 p) {
		position = p;
		//normal = null;
	}
	
	public Vertex(Vector4 p, Vector3 n) {
		position = p;
		//normal = n;
	}
	
	public void setPosition(Vector4 p) {
		position = p;
	}
	
	public Vector4 getPosition() {
		return position;
	}
	
}
