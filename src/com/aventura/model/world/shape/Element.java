package com.aventura.model.world.shape;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.math.transform.Transformable;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;

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
 *     +---------------+
 *     |    Element    |    Color attribute
 *     +---------------+
 *             ^ 0
 *             |     1 Element contains [0..n] Triangles
 *             | n
 *     +---------------+    Color attribute -> overrides Element's color if set
 *     |    Triangle   |    Normal attribute -> to be used if specifically indicated (flag triangleNormal set to true) or if PLAIN rendering
 *     +---------------+
 *             ^ 1
 *             |     1 Triangle contains exactly 3 Vertices
 *             | 3
 *      +-------------+     Color attribute -> overrides Triangle and Element's color if set
 *      |   Vertex    |     Normal attribute -> this is default behavior
 *      +-------------+
 *  
 *
 * @author Olivier BARRY
 * @since March 2016
 *
 */
public class Element implements Transformable, Shape, Generable {
	
	private static int ID = 1; // Single ID to identify any Element. Is incremented.
	
	protected static final String ELEMENT_DEFAULT_NAME = "element";
	protected String name;
	protected int id;
	
	protected ArrayList<Element> subelements; // To create a hierarchy of elements - not necessarily used
	protected ArrayList<Triangle> triangles;  // Triangles related to this element
	protected ArrayList<Vertex> vertices;     // Vertices of this element (also referenced by the triangles)
	
	protected Matrix4 transform;  // Element to World Transformation Matrix (Model Matrix)
	
	// Colors and specular reflection characteristics
	protected Color elementColor = null; // Color of the element unless specified at Triangle or Vertex level (lowest level priority)
	protected Color specularColor = null; // Specular reflection color for this Element
	protected float specularExponent = 0; // Specular exponent
	
	// Topology
	protected boolean isClosed = false; // Defines if the Element is a closed element or not in order to eliminate or not its back faces (internal to the closed element thus not visible)
		
	/**
	 * Create an open Element (not closed)
	 * 
	 * @param isClosed a boolean indicated if Element is closed (true) or not (false)
	 */
	public Element() {
		id = ID++;
		this.name = ELEMENT_DEFAULT_NAME;
		triangles = new ArrayList<Triangle>();
		vertices = new ArrayList<Vertex>();
		transform = Matrix4.IDENTITY; // By default
	}

	/**
	 * Create an open Element (not closed)
	 * 
	 * @param isClosed a boolean indicated if Element is closed (true) or not (false)
	 */
	public Element(String name) {
		id = ID++;
		this.name = name;
		triangles = new ArrayList<Triangle>();
		vertices = new ArrayList<Vertex>();
		transform = Matrix4.IDENTITY; // By default
	}

	/**
	 * Create an Element defining its "closeness" that is wether it is closed or open
	 * 
	 * @param isClosed a boolean indicated if Element is closed (true) or not (false)
	 */
	public Element(boolean isClosed) {
		id = ID++;
		this.name = ELEMENT_DEFAULT_NAME;
		this.triangles = new ArrayList<Triangle>();
		this.vertices = new ArrayList<Vertex>();
		this.transform = Matrix4.IDENTITY; // By default
		this.isClosed = isClosed;
	}
	
	/**
	 * Create an Element defining its "closeness" that is wether it is closed or open
	 * 
	 * @param isClosed a boolean indicated if Element is closed (true) or not (false)
	 */
	public Element(String name, boolean isClosed) {
		id = ID++;
		this.name = name;
		this.triangles = new ArrayList<Triangle>();
		this.vertices = new ArrayList<Vertex>();
		this.transform = Matrix4.IDENTITY; // By default
		this.isClosed = isClosed;
	}

	public boolean isLeaf() {
		if (subelements == null) {
			return true;
		} else {
			return false;
		}
	}
	
	public ArrayList<Element> getSubElements() {
		return subelements;
	}
	
	public void addElement(Element e) {
		// If never initialized then create the Array
		if (subelements == null) subelements = new ArrayList<Element>();
		this.subelements.add(e);
	}
	
	// ********************
	// ***** Geometry *****
	// ********************

	public void addVertex(Vertex v) {
		this.vertices.add(v);
	}
	
	/**
	 * Create and return Vertex
	 * This method should necessary be used to create any Vertex' Element since it does the needful to put it in the list of vertices of this Element
	 * 
	 * @return the newly created Vertex
	 */
	public Vertex createVertex() {
		Vertex v = new Vertex();
		addVertex(v);
		return v;
	}
	
	/**
	 * Create and return Vertex
	 * This method should necessary be used to create any Vertex' Element since it does the needful to put it in the list of vertices of this Element
	 * 
	 * @param v4 the position of the Vertex
	 * @return the newly created Vertex
	 */
	public Vertex createVertex(Vector4 v4) {
		Vertex v = new Vertex(v4);
		addVertex(v);
		return v;
	}
	
	/**
	 * Create and return a Mesh of vertices
	 * This method should necessary be used to create a Mesh of vertices since it does the needful to put it in the list of vertices of this Element
	 * 
	 * @param n nb of vertices on width of the Mesh
	 * @param p nb of vertices on height of the Mesh
	 * @return an array of vertices
	 */
	public Vertex[][] createVertexMesh(int n, int p) {
		Vertex[][] mesh = new Vertex[n][p];
		
		for (int i=0; i<n; i++) {
			for (int j=0; j<p; j++) {
				Vertex v = new Vertex();
				addVertex(v);
				mesh[i][j] = v;
			}
		}
		return mesh;
	}
	
	/**
	 * Create and return a line of vertices
	 * This method should necessary be used to create a Mesh of a single line of vertices since it does the needful to put it in the list of vertices of this Element
	 * 
	 * @param n nb of vertices on width of the Mesh
	 * @return an array of vertices
	 */
	public Vertex[] createVertexMesh(int n) {
		Vertex[] mesh = new Vertex[n];

		for (int i=0; i<n; i++) {
			Vertex v = new Vertex();
			addVertex(v);
			mesh[i] = v;
		}
		return mesh;
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
	
	public int getNbTriangles() {
		return triangles.size();
	}

	public ArrayList<Vertex> getVertices() {
		return vertices;
	}
	
	public Vertex getVertex(int i) {
		return vertices.get(i);
	}
	
	public int getNbVertices() {
		return vertices.size();
	}

	// *************************
	// ***** Transfomation *****
	// *************************

	@Override
	public void setTransformation(Matrix4 transformation) {
		this.transform = transformation;
		//this.calculateNormals();
	}

	@Override
	public void combineTransformation(Matrix4 transformation) {
		// TODO Auto-generated method stub
		//this.transform.timesEquals(transformation);
		this.transform = transformation.times(this.transform);
		//this.calculateNormals();
	}

	@Override
	public Matrix4 getTransformation() {
		return transform;
	}
	
	// Transform the World position of the Vertices only
	// Do NOT transform the projected position NOR the normals of the Vertex
	// It is supposed to be used before calculating normals and projecting Elements through RenderEngine
	@Override
	public void transform(Matrix4 transformation) {

		for (int i=0; i<this.getNbVertices(); i++) {
			Vertex v = this.getVertex(i);
			v.setWorldPos(transformation.times(v.getPos()));
		}
	}

	// ********************************
	// ***** Ligthing and Shading *****
	// ********************************

	public void setColor(Color c) {
		this.elementColor = c;
	}
	
	public Color getColor() {
		return elementColor;
	}

	public void setSpecularColor(Color c) {
		this.specularColor = c;
	}
	
	public Color getSpecularColor() {
		return specularColor;
	}
	
	public void setSpecularExp(float e) {
		this.specularExponent = e;
	}
	
	public float getSpecularExp() {
		return specularExponent;
	}

	public boolean isClosed() {
		return isClosed;
	}
	
	public void setClosing(boolean isClosed) {
		this.isClosed = isClosed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String toString() {
		String element = "Element\n";
		element += "* Name: " + name + "\n";
		element += "* Id: " + id + "\n";
		element += "* Triangles: " + getNbTriangles() + "\n";
		element += "* Vertices: " + getNbVertices() + "\n";
		element += "* Element color: " + getColor() + "\n";
		element += "* Specular color: " + getSpecularColor() + "\n";
		element += "* Specular exponent: " + getSpecularExp() + "\n";		
		return element;
		//return "Element name: "+name+"\nTriangles: "+getNbTriangles()+", Vertices: "+getNbVertices()+"\nElement color: "+elementColor+"\nSpecular color: "+specularColor+" Specular exponent: "+specularExponent;		
	}
	
	// ******************************
	// ***** Normal calculation *****
	// ******************************

	/* (non-Javadoc)
	 * @see com.aventura.model.world.NormalGeneration#calculateNormals()
	 * This method calculate normals of each Triangle.
	 * It is not expected to be used in itself as the calculation of Normals for an Element is Element specific but this can be
	 * used for quick development and test.
	 * This method should be overridden by sub classes and implement calculation specific mechanism. 
	 */
	public void calculateNormals() {
		
		// Compute normals of this Element
		for (int i=0; i<triangles.size(); i++) {
			triangles.get(i).setTriangleNormal(true);
			triangles.get(i).calculateNormal();
		}
		
		// Compute normals recursively for Sub Elements
		//calculateSubNormals(); -> Now calculated through the recursive generate() method
	}
	
	protected void calculateSubNormals() {
		if (subelements != null) {
			for (int i=0; i<subelements.size(); i++) {
				subelements.get(i).calculateNormals();
			}
		}		
	}
	// ******************************
	// ***** Normal calculation *****
	// ******************************
	
	public void generate() {

		//this.createGeometry();
		this.generateVertices();
		this.generateTriangles();
		this.calculateNormals();
		this.subGenerate();
	}
	
	public void update() {
		// Clear the previously created triangles before generating the new set.
		triangles.clear();
		// Do same than generate except Vertices generation as they are assumed to be already existing (and likely modified)
		this.generateTriangles();
		this.calculateNormals();
		this.subUpdate();
		
	}

	@Override
	public void generateVertices() {
		// This method should be implemented in specific Element classes
		// Nothing to do for a generic Element that could be used as group of Elements
		
	}

	@Override
	public void generateTriangles() {
		// This method should be implemented in specific Element classes
		// Nothing to do for a generic Element that could be used as group of Elements
		
	}


	
	protected void subGenerate() {
		if (subelements != null) {
			for (int i=0; i<subelements.size(); i++) {
				subelements.get(i).generate();
			}
		}		
	}

	protected void subUpdate() {
		if (subelements != null) {
			for (int i=0; i<subelements.size(); i++) {
				subelements.get(i).update();
			}
		}		
	}

//	public void createGeometry() {
//		// This method should be implemented in specific Element classes
//		// Nothing to do for a generic Element that could be used as group of Elements
//	}

	
	// *****************
	// ***** Shape *****
	// *****************

	@Override
	public Element setTopTexture(Texture tex) {
		// TODO Auto-generated method stub
		return null;
	} 

	@Override
	public Element setBottomTexture(Texture tex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setLeftTexture(Texture tex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setRightTexture(Texture tex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setFrontTexture(Texture tex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setBackTexture(Texture tex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setTopColor(Color c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setBottomColor(Color c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setLeftColor(Color c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setRightColor(Color c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setFrontColor(Color c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element setBackColor(Color c) {
		// TODO Auto-generated method stub
		return null;
	}

	public float getMaxX() {
		float max = vertices.get(0).getPos().getX();
		for (int i=1; i<vertices.size(); i++) {
			float newmax = vertices.get(i).getPos().getX();
			if (newmax > max) max = newmax;
		}
		return max;
	}

	public float getMaxY() {
		float max = vertices.get(0).getPos().getY();
		for (int i=1; i<vertices.size(); i++) {
			float newmax = vertices.get(i).getPos().getY();
			if (newmax > max) max = newmax;
		}
		return max;
	}

	public float getMaxZ() {
		float max = vertices.get(0).getPos().getZ();
		for (int i=1; i<vertices.size(); i++) {
			float newmax = vertices.get(i).getPos().getZ();
			if (newmax > max) max = newmax;
		}
		return max;
	}

	public float getMinX() {
		float min = vertices.get(0).getPos().getX();
		for (int i=1; i<vertices.size(); i++) {
			float newmin = vertices.get(i).getPos().getX();
			if (newmin < min) min = newmin;
		}
		return min;
	}

	public float getMinY() {
		float min = vertices.get(0).getPos().getY();
		for (int i=1; i<vertices.size(); i++) {
			float newmin = vertices.get(i).getPos().getY();
			if (newmin < min) min = newmin;
		}
		return min;
	}

	public float getMinZ() {
		float min = vertices.get(0).getPos().getZ();
		for (int i=1; i<vertices.size(); i++) {
			float newmin = vertices.get(i).getPos().getZ();
			if (newmin < min) min = newmin;
		}
		return min;
	}

}
