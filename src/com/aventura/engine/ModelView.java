package com.aventura.engine;

import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.NotInvertibleMatrixException;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

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
 * This class represents and manages the transformations from model to gUIView.
 * 
 * Interesting explanations on this transformation can be found here:
 * http://www.opengl-tutorial.org/fr/beginners-tutorials/tutorial-3-matrices/
 * 
 *     +---------------------+
 *     |  Model Coordinates  |
 *     +---------------------+
 *                |
 *                |   [Model Matrix] 4x4 Transformation Matrix -> to transform an element into the World coordinates
 *                v
 *     +---------------------+
 *     |  World Coordinates  |
 *     +---------------------+
 *                |
 *                |   [GUIView Matrix] 4x4 Transformation Matrix -> to transform the world into the camera coordinates
 *                v
 *     +----------------------+
 *     |  Camera Coordinates  |
 *     +----------------------+
 *                |
 *                |   [Projection Matrix] 4x4 Transformation Matrix -> to transform any point or vector into homogeneous coordinates
 *                v
 *   +--------------------------+
 *   | Homogeneous Coordinates  | or Clip Coordinates, each coordinate being in range [-1, 1]
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
 *  In Aventura model, it is at Element level and it may require to be multiplied recursively if the model contains sub-elements.
 *  See Element class description.
 *  
 * @author Olivier BARRY
 * @since May 2016
 * 
 */
public class ModelView {
	
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
	public ModelView() {
		
	}
	
	/**
	 * Generally the Camera Matrix and Projection Matrix do not change over time in static mode.
	 * So we can safely create a ModelView object with these 2 matrices at initialization.
	 * If one of these matrices change over time, the corresponding set methods should then be used. 
	 * @param gUIView the Camera Matrix4
	 * @param projection the Matrix4 to transform into homogeneous coordinates
	 */
	public ModelView(Matrix4 view, Matrix4 projection) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "ModelView(gUIView, projection)");
		this.view = view;
		this.projection = projection;
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "GUIView matrix:\n"+ view);
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Projection matrix:\n"+ projection);
	}
	
	/**
	 * Set the Projection Matrix into homogeneous coordinates
	 * @param projection the Matrix4 to transform into homogeneous coordinates
	 */
	public void setProjection(Matrix4 projection) {
		this.projection = projection;
	}
	
	/**
	 * Set the Camera transformation Matrix aka GUIView Matrix to transform from World to Camera coordinates 
	 * @param gUIView the Camera
	 */
	public void setView(Matrix4 view) {
		this.view = view;
	}
	
	/**
	 * Get the "GUIView" transformation Matrix and Eye related information 
	 * @return the Camera
	 */
	public Matrix4 getView() {
		return this.view;
	}
	
	/**
	 * Set the model Matrix to transform from Model (Element in Aventura) coordinates into World coordinates
	 * @param model the Model Matrix4
	 */
	public void setModel(Matrix4 model) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "setModel(model)");
		// Vertices Model matrix
		this.model = model;
		
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
	 * Set the model Matrix to transform from Model (Element in Aventura) coordinates into World coordinates
	 * @param model the Model Matrix4
	 */
	public void setModelWithoutNormals(Matrix4 model) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "setModelWithoutNormals(model)");
		// Vertices Model matrix
		this.model = model;
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Model Matrix:\n"+ model);
	}
	/**
	 *  The complete transformation, from model to homogeneous coordinates, is done through the following formula:
	 * TransformedVector = [Projection Matrix] * [GUIView Matrix] * [Model Matrix] * OriginalVector
	 */
	public void computeTransformation() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "computeTransformation()");
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
	 * Fully compute Vertex projections resulting from the ModelView transformation for both ModelToWorld and ModelToClip projections
	 * Complete the provided Vertex with projection data but do not modify original position data
	 * Calculate the normal projection (Vertex normal) taking care of using the normals Model matrix (in case of non uniform scaling)
	 * and not the standard Model matrix.
	 * 
	 * @param v the provided Vertex
	 */
	public void transform(Vertex v) {
		v.setProjPos(full.times(v.getPos()));
		v.setWorldPos(model.times(v.getPos()));
		if (v.getNormal() != null) {
			v.setProjNormal(full_normals.times(v.getNormal().V4()).V3()); // Not used - Removed 1/1/2022 - restored 11/7/2023 
			v.setWorldNormal(model_normals.times(v.getNormal().V4()).V3());
		}
	}
	
	/**
	 * Fully compute Vertex projections resulting from the ModelView transformation for both ModelToWorld and ModelToClip projections
	 * Complete the provided Vertex with projection data but do not modify original position data
	 * Calculate the normal projection (Vertex normal) taking care of using the normals Model matrix (in case of non uniform scaling)
	 * and not the standard Model matrix.
	 * 
	 * @param v the provided Vertex
	 */
	public void transformWithoutNormals(Vertex v) {
		v.setProjPos(full.times(v.getPos()));
		v.setWorldPos(model.times(v.getPos())); // TBC if required for Shadow map and shadow casting calculation
	}
	
	/**
	 * Project Vertex using the projection resulting from the ModelView transformation for ModelToClip projection (full)
	 * Return the resulting Vector4 without updating the Vertex (does not update Vertex's projection fields)
	 * 
	 * @param v the provided Vertex
	 * @return the projected Vector4 without Vertex transformation
	 */
	public Vector4 project(Vertex v) {
		return full.times(v.getPos());
	}
	
	/**
	 * Transform all vertices of an Element
	 * 
	 * @param e the Element
	 */
	public void transformVertices(Element e) {
		for (int i=0; i<e.getNbVertices(); i++) {
			transform(e.getVertex(i));
		}
	}
	
	/**
	 * Transform all vertices of an Element without normal calculation (shadowing)
	 * 
	 * @param e the Element
	 */
	public void transformVerticesWithoutNormals(Element e) {
		for (int i=0; i<e.getNbVertices(); i++) {
			transformWithoutNormals(e.getVertex(i));
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
	public Vector3 calculateProjNormal(Triangle t) {
				
		if (t.getNormal() != null) {
			return full_normals.times(t.getNormal().V4()).V3();
		} else {
			return null;
		}
	}
}
