package com.aventura.engine;

import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.NotInvertibleMatrixException;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2025 Olivier BARRY
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
 * This class represents and manages the transformations from model to gUIView then to project Vertex into homogeneous coordinates
 * 
 * Interesting explanations on this transformation can be found here:
 * http://www.opengl-tutorial.org/fr/beginners-tutorials/tutorial-3-matrices/
 * 
 *     +---------------------+
 *     |  Model Coordinates  |
 *     +---------------------+
 *                |
 *                |   [Model Matrix] 4x4 Transformation Matrix -> to transform an element into the World coordinates
 *                |   The Model matrix changes at each Element so the full Matrix needs to be recomputed.
 *                v
 *     +---------------------+
 *     |  World Coordinates  |
 *     +---------------------+
 *                |
 *                |   [Lookat Matrix] 4x4 Transformation Matrix -> to transform the world into the camera coordinates
 *                v
 *     +----------------------+
 *     |  Camera Coordinates  |
 *     +----------------------+
 *                |
 *                |   [Projection Matrix] 4x4 Transformation Matrix -> to transform any point or vector into homogeneous coordinates
 *                |   or Clip Coordinates, each coordinate being in range [-1, 1]
 *                v
 *   +--------------------------+
 *   | Homogeneous Coordinates  | 
 *   +--------------------------+
 * 
 *  It provides all needed services to the RenderEngine to compute these transformations for each Vertex of the model.
 *  
 *  Each Transformation is a set of 1) Scaling then 2) Rotation and last 3) Translation.
 *  This can be modeled by the following matrix and vectors computation:
 *  
 *  	TransformedVector = [TranslationMatrix * RotationMatrix * ScaleMatrix] * OriginalVector
 *  	TransformedVector = [4x4 Transformation Matrix] * OriginalVector
 *  
 *  The complete transformation, from model to homogeneous coordinates, is done through the following formula:
 *  
 *  	TransformedVector = [Projection Matrix] * [GUIView Matrix] * [Model Matrix] * OriginalVector
 *  
 *  Note that the Model Matrix is provided by the model itself.
 *  In Aventura world architecture made of Elements, the Model Matrix is at Element level and may require to be multiplied recursively if the model contains sub-elements.
 *  See Element class description.
 *  
 * @author Olivier BARRY
 * @since May 2016
 * 
 */
public class ModelViewProjection {
	
	// Transformation matrices
	Matrix4 projection = null;
	Matrix4 view = null;
	Matrix4 model = null;
	Matrix4 model_normals = null;
	
	// This matrix is the result of the multiplication of all Matrices
	Matrix4 full = null;
	Matrix4 full_normals = null; // restored 11/7/2023
	
	/**
	 * Default constructor.
	 * Do nothing.
	 */
	public ModelViewProjection() {
		
	}
	
	/**
	 * During the rendering operation, Camera Matrix and Projection Matrix are constant, only the Model Matrix will change for each Element.
	 * So we can safely create a ModelViewProjection object with these 2 matrices at initialization, they will not have to be updated.
	 * Then the Model matrix will be provided before any calculation / rendering of Vertices each time a new Element needs to be processed.
	 * @param gUIView the Camera Matrix4
	 * @param projection the Matrix4 to transform into homogeneous coordinates
	 */
	public ModelViewProjection(Matrix4 view, Matrix4 projection) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "ModelViewProjection(gUIView, projection)");
		this.view = view;
		this.projection = projection;
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "GUIView matrix:\n"+ view);
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Projection matrix:\n"+ projection);
	}
	
	/**
	 * Set the model Matrix to transform from Model (Element in Aventura) coordinates into World coordinates
	 * @param model the Model Matrix4
	 */
	public void setModel(Matrix4 model) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "setModel(model)");
		// Vertices Model matrix
		this.model = model;
	}
		
	public void calculateNormalMatrix() {
		// Normals Model matrix :
		// Use the Model matrix for orthogonal transformation (orthogonal transformations preserve lengths of vectors and angles between them)
		// Use the inverse transpose matrix in case of non orthogonal transformation (e.g. non uniform scaling)
		// Test of orthogonal transformation only need Matrix3 (not full Matrix 4 / homogeneous coordinate)
		Matrix3 model3 = model.getMatrix3();
		
		// To test if the transformation Matrix is orthogonal, we can use the test Transpose(A).A = I (Identity Matrix) else it is not.
		// Rounding errors in the calculation requires comparison with a margin of tolerance (Epsilon). Surprisingly the experience shows that 1.0E-4 is the lowest epsilon
		if (model3.times(model3.transpose()).equals(Matrix3.IDENTITY)) {
			// No need to compute the inverse Matrix in this case, the transformation is orthogonal
			model_normals = model;
			if (Tracer.info) Tracer.traceInfo(this.getClass(),"Model normals matrix = Model matrix !!!");
		} else {
			try {
				model_normals = model.transpose().inverse();
			} catch (NotInvertibleMatrixException e) {
				// Should never happen but just in case use the model Matrix for normals transformation in this case
				model_normals = model;
				// And log the stack trace
				e.printStackTrace();
				if (Tracer.info) Tracer.traceInfo(this.getClass(),"Error in matrix inversion !!!");
			}
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Model Matrix:\n"+ model);
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Model Normals Matrix:\n"+ model_normals);
	}
		
	/**
	 * The complete transformation, from model to homogeneous coordinates, is done through the following formula:
	 * TransformedVector = [Projection Matrix] * [GUIView Matrix] * [Model Matrix] * OriginalVector
	 * 
	 * This method calculates the MVP matrix (or recalculates when needed due to any change in one of the matrices) 
	 */
	public void calculateMVPMatrix() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "computeTransformation()");
		
		// Calculate the full matrix = MVP matrix transformation
		full = projection.times(view.times(model));
		
		// Do not compute the transformation for normals if model_normals not initialized (not required e.g. shadow map calculation)
		// restored 11/7/2023
		if (model_normals != null) { 
			full_normals = projection.times(view.times(model_normals));
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation normal matrix:\n"+ full_normals);
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation matrix:\n"+ full);
	}
	
	/**
	 * Fully compute Vertex projections resulting from the ModelViewProjection transformation for both ModelToWorld and ModelToClip projections
	 * Complete the provided Vertex with projection data but do not modify original position data
	 * Calculate the normal projection (Vertex normal) taking care of using the normals Model matrix (in case of non uniform scaling)
	 * and not the standard Model matrix.
	 * 
	 * @param v the provided Vertex
	 * @param normals boolean should be true if normals should be calculated, false otherwise (Shadowing)
	 */
	public void transformVertex(Vertex v, boolean normals) {
		// Calculate the coordinates in Clip space (full transformation) and store results in Vertex's related field
		v.setProjPos(full.times(v.getPos()));
		// Also calculate only the coordinates of the Vertex in World coordinates for geometry calculation (e.g. bounding boxes etc.)
		v.setWorldPos(model.times(v.getPos()));
		if (normals) {
			// Calculate Normals
			if (v.getNormal() != null) {
				v.setProjNormal(full_normals.times(v.getNormal().V4()).V3()); // Not used - Removed 1/1/2022 - restored 11/7/2023 
				v.setWorldNormal(model_normals.times(v.getNormal().V4()).V3());
			}
		}
	}
		
	/**
	 * Used for an offline projection of a Vertex, e.g. in context of Shadowing in Light's coordinates
	 * Project Vertex using the projection resulting from the ModelViewProjection transformation for ModelToClip projection (full)
	 * Return the resulting Vector4 without updating the Vertex (does not update Vertex's projection fields)
	 * 
	 * @param v the provided Vertex
	 * @return the projected Vector4 without Vertex transformation
	 */
	public Vector4 projectVertex(Vertex v) {
		return full.times(v.getPos());
	}
	
	/**
	 * Used for an offline projection of a Vertex, e.g. in context of Shadowing in Light's coordinates
	 * Project Vertex using the projection resulting only from the ModelViewProjection transformation for ModelToWorld projection (world)
	 * Return the resulting Vector4 without updating the Vertex (does not update Vertex's projection fields)
	 * 
	 * @param v the provided Vertex
	 * @return the projected Vector4 without Vertex transformation
	 */
	public Vector4 projectWorldVertex(Vertex v) {
		return model.times(v.getPos());
	}
	
	/**
	 * Transform all vertices of an Element
	 * 
	 * @param e the Element
	 * @param normals boolean should be true if normals should be calculated, false otherwise (Shadowing)
	 */
	public void transformElement(Element e, boolean normals) {
		// Loop on all vertices of the Element and transform each of them
		for (int i=0; i<e.getNbVertices(); i++) {
			transformVertex(e.getVertex(i), normals);
		}
	}
		
	/**
	 * Transform the normal of a Triangle (in case of usage of Triangle normal instead of Vertex normal)
	 * @param t
	 */
	public void transformNormal(Triangle t) {
				
		if (t.getNormal() != null) {
			// t.setProjNormal(full_normals.times(t.getNormal().V4()).V3()); // restored 11/7/2023
			t.setWorldNormal(model_normals.times(t.getNormal().V4()).V3());
		} else {
			// t.setProjNormal(null); // restored 11/7/2023
			t.setWorldNormal(null);
		}
	}
	
	/**
	 * Transform the normal of a Triangle (in case of usage of Triangle normal instead of Vertex normal)
	 * @param t the Triangle
	 * @return the triangle normal as Vector3
	 */
	public Vector3 projectNormal(Triangle t) {
				
		if (t.getNormal() != null) {
			return full_normals.times(t.getNormal().V4()).V3();
		} else {
			return null;
		}
	}
}
