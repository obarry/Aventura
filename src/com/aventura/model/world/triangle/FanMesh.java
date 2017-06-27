package com.aventura.model.world.triangle;

import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Element;


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
 * A Fan Mesh of vertices with same Texture to create a surface part of an Element
 * This class creates properly the array of Vertices and proposes services to create the list of Triangle for the fan of triangles.
 * This class does not generate the geometry of the surface, this is the user of this class who needs to set the position of each Vertex in the space.
 * 
 * @author Olivier BARRY
 * @since June 2017
 */
public class FanMesh extends Mesh {
	
	// Regular Fan of Triangles
	
	//          +
	//        //|\\
	//      / / | \ \
	//    /  /  |  \  \ 
	//  / T1/   |   \T4 \
	// +   / T2 | T3 \   +
	//  \ /     |     \ /
	//   +------+------+
	
	public static final int MESH_ORIENTED_TRIANGLES = 1;

	int nbv;
	Vertex summit;
	Vertex[] vertices;

	public FanMesh(Element e, int n) {
		super(e);
		this.nbv = n;
		vertices = elm.createVertexMesh(n);
	}

}
