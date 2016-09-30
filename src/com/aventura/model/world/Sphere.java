package com.aventura.model.world;

import com.aventura.math.transform.Translation;
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
 *  Vertices: +
 *  Segments: / \ | -
 *  Exemple;
 *  Number of segments: 12
 *  -> 12 segments around the sphere horizontally (360 degrees) -> 12 vertices (only, not 13!)
 *  -> 6 segments from north to south (180 degrees) -> north pole, 5 vertices, south pole (n+1)
 *  -> table of 12 x 5 vertices + 2 poles
 * 
 *               +					North Pole
 *    / / / / / /|\ \ \ \ \            
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | |
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | |
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | |
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *   | | | | | | | | | | | | 
 *   +-+-+-+-+-+-+-+-+-+-+-+-
 *    \ \ \ \ \ \|/ / / / / 
 *               +					South Pole
 * 
 *   
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class Sphere extends Element {
	
	protected Vertex[][] vertices;
	
	/**
	 * @param ray
	 * @param half_seg is half the number of segments for 360 degrees
	 * @param position
	 */
	public Sphere(double ray, int half_seg, Vector3 position) {
		super();
		subelements = null;
		vertices = new Vertex[half_seg*2][half_seg+1]; // (n) x (n/2+1) vertices (n being the number of segments)
		Vertex northPole, southPole;
		double alpha = Math.PI/half_seg;
		
		// Create Vertices
		northPole = new Vertex(new Vector4(0, 0, ray,  1));
		southPole = new Vertex(new Vector4(0, 0, -ray,  1));
		
		for (int i=0; i<half_seg*2; i++) {
			for (int j=0; j<half_seg+1; j++) {
				vertices[i][j] = new Vertex(new Vector4(ray*Math.cos(alpha*i), ray*Math.sin(alpha*i), 0,  1));
			}
		}
		
		// Create Triangles - 2 triangles per "square" face
		
		// Complete creation of this element with a translation
		this.transform = new Translation(position);

	}



}
