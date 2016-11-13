package com.aventura.model.world;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.math.transform.Transformable;
import com.aventura.math.vector.Matrix4;

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
 * This class is the class representing a base element of the world. An element can be a Sphere, a Box, a Cube or a much complex thing.
 * This element is made of multiple Triangles all made of Vertices.
 * 
 * An element is then positioned in the World through a Transformation applied to all vertices / triangles (see below). This transformation
 * is an attribute of the Element.
 * 
 * An element can be recursively made of sub-elements, this can be useful to build complex elements made of more basic forms. This then
 *  builds a tree of elements.
 * 
 * This class is derived to provide the basic geometries (cubes, sphere, cylinder, etc.) or can be derived by the application to create
 * more specialized objects.
 * 
 * 
 * Each Element is positioned within the World by a specific Transformation :
 * 
 *     +-----------------------+
 *     |  Element Coordinates  |
 *     +-----------------------+
 *                 |
 *                 |   [Model Matrix] 4x4 Transformation Matrix
 *                 v
 *      +---------------------+
 *      |  World Coordinates  |
 *      +---------------------+
 *
 * The Model Matrix will transform the Model into the World through the following operations: 
 * 		1) scaling
 * 		2) Rotation
 * 		3) Translation

 *  * This can be modeled as follows :
 * 
 *  	TransformedVector = [TranslationMatrix * RotationMatrix * ScaleMatrix] * OriginalVector
 *  	TransformedVector = [4x4 Transformation Matrix] * OriginalVector
 *

 * In case of sub-elements, the matrix will transform the SubElement coordinates into its father Element coordinates.
 * Hence the transformation to the world is done recursively:
 * 
 *     +--------------------------+
 *     |  SubElement Coordinates  |
 *     +--------------------------+
 *                  |
 *                  |   [SubElement Matrix] 4x4 Transformation Matrix
 *                  v
 *      +-----------------------+
 *      |  Element Coordinates  |
 *      +-----------------------+
 *                  |
 *                  |   [Element Matrix] 4x4 Transformation Matrix
 *                  v
 *       +---------------------+
 *       |  World Coordinates  |
 *       +---------------------+
 * 
 * Leading to :
 *  	TransformedVector = [Element Matrix] * [SubElement Matrix] * OriginalVector
 *  And recursively (L0, L1, ...Ln figuring the level of depth of the sub-element in the hierachy) :
 *  	TransformedVector = [Element Matrix (L0)] * [SubElement Matrix (L1)] * ... * [SubElement Matrix (Ln)] * OriginalVector
 *  
 *  Note that although the Transformation matrix is owned by the Element, the transformation of the vectors (Vertices) is performed
 *  by the Render Engine when necessary and the way it is configured for.
 *
 * @author Bricolage Olivier
 * @since March 2016
 *
 */
public class Element implements Transformable, NormalGeneration {
	
	protected ArrayList<Element> subelements; // To create a hierarchy of elements - not necessarily used
	protected ArrayList<Triangle> triangles;  // Triangles related to this element
	//protected ArrayList<Vertex> vertices;     // Vertices of this element (also referenced by the triangles)
	
	protected Matrix4 transform;  // Element to World Transformation Matrix (Model Matrix)
	protected Color color; // Color of the element unless specified at Vertex level (lowest level priority)
	
	public Element() {
		super();
		triangles = new ArrayList<Triangle>();
		transform = Matrix4.IDENTITY; // By default
	}
	
	public boolean isLeaf() {
		if (subelements == null) {
			return true;
		} else {
			return false;
		}
	}
	
//	public void addVertex(Vertex v) {
//		//TODO To be implemented
//	}
	
	public ArrayList<Element> getSubElements() {
		return subelements;
	}
	
	public void addElement(Element e) {
		// If never initialized then create the Array
		if (subelements == null) subelements = new ArrayList<Element>();
		this.subelements.add(e);
	}
	
	public void addTriangle(Triangle t) {
		this.triangles.add(t);
	}
	
	/**
	 * @param triangles
	 */
	public void addTriangles(Triangle [] tri) {
		
		for (int i=0; i<tri.length; i++) {
			addTriangle(tri[i]);
		}
	}
	
	public ArrayList<Triangle> getTriangles() {
		return triangles;
	}
	
	public Triangle getTriangle(int i) {
		return triangles.get(i);
	}
	
	public int getNbOfTriangles() {
		return triangles.size();
	}

	@Override
	public void setTransformation(Matrix4 transformation) {
		this.transform = transformation;
		
	}

	@Override
	public Matrix4 getTransformation() {
		return transform;
	}
	
	public void setColor(Color c) {
		this.color = c;
	}
	
	public Color getColor() {
		return color;
	}

	/* (non-Javadoc)
	 * @see com.aventura.model.world.NormalGeneration#calculateNormals()
	 * This method calculate normals of each Triangle.
	 * It is not expected to be used in itself as the calculation of Normals for an Element is Element specific but this can be
	 * used for quick development and test.
	 * This method should be overridden by sub classes and implement calculation specific mechanism. 
	 */
	@Override
	public void calculateNormals() {
		
		for (int i=0; i<triangles.size(); i++) {
			this.triangles.get(i).calculateNormal();
		}
	}

}
