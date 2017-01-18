package com.aventura.engine;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Segment;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.tools.tracing.Tracer;

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
 * This class represents and manages the transformations from model to view.
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
 *                |   [View Matrix] 4x4 Transformation Matrix -> to transform the world into the camera coordinates
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
 *  	TransformedVector = [Projection Matrix] * [View Matrix] * [Model Matrix] * OriginalVector
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
	
	Matrix4 projection;
	Matrix4 view;
	Matrix4 model;
	
	// This matrix is the result of the multiplication of all Matrices
	Matrix4 transformation;
	
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
	 * @param view the Camera Matrix4
	 * @param projection the Matrix4 to transform into homogeneous coordinates
	 */
	public ModelView(Matrix4 view, Matrix4 projection) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "ModelView(view, projection)");
		this.view = view;
		this.projection = projection;
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "View matrix:\n"+ view);
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
	 * Set the Camera transformation aka View Matrix to transform from World to Camera coordinates 
	 * @param view the Camera Matrix4
	 */
	public void setView(Matrix4 view) {
		this.view = view;
	}
	
	/**
	 * Set the model Matrix to transform from Model (Element in Aventura) coordinates into World coordinates
	 * @param model the Model Matrix4
	 */
	public void setModel(Matrix4 model) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "setModel(model)");
		this.model = model;
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Model Matrix:\n"+ model);
	}
		
	/**
	 *  The complete transformation, from model to homogeneous coordinates, is done through the following formula:
	 * TransformedVector = [Projection Matrix] * [View Matrix] * [Model Matrix] * OriginalVector
	 */
	public void computeTransformation() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "computeTransformation()");
		transformation = projection.times(view.times(model));
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation matrix:\n"+ transformation);
	}
	
	/**
	 * Return a new Segment containing (new) projected vertices
	 * Relies on the transform method transforming vertices
	 * 
	 * @param l the Segment to transform
	 * @return the new Segment
	 */
	public Segment modelToClip(Segment l) {
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "transform line: "+l);
		
		Segment transformed = new Segment();
		
		transformed.setV1(modelToClip(l.getV1()));
		transformed.setV2(modelToClip(l.getV2()));
		
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "transformed line: "+ transformed);
		
		return transformed;
	}
	
	/**
	 * Return a new triangle containing (new) projected vertices
	 * Relies on the transform method transforming vertices
	 * 
	 * @param t the triangle to transform
	 * @return the new triangle
	 */
	public Triangle modelToClip(Triangle t) {
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "transform triangle: "+t);
		
		Triangle transformed = new Triangle();
		
		transformed.setV1(modelToClip(t.getV1()));
		transformed.setV2(modelToClip(t.getV2()));
		transformed.setV3(modelToClip(t.getV3()));
		//if (t.getNormal()!=null) transformed.setNormal(transform(t.getNormal()));

		
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "transformed triangle: "+ transformed);
		
		return transformed;
	}
	
	/**
	 * Return a new Vertex resulting from the ModelView transformation ("projection") of the provided Vertex
	 * Do not modify the provided Vertex.
	 * 
	 * @param v the provided Vertex (left unchanged)
	 * @return the new projected Vertex
	 */
	public Vertex modelToClip(Vertex v) {
		// Create a new Vertex having its Vector4 position set to the resulting of the transformation of the provided Vertex's 
		// Vector4 position by the transformation Matrix
		Vertex transformed = new Vertex(transformation.times(v.getPosition()));
		// Return the newly created Vertex (hence preserve the original Vertex of the Element)
		return transformed;
	}
	
	/**
	 * Return a new Vector4 resulting from the ModelView transformation ("projection") of the provided Vector4
	 * TransformedVector = [4x4 Transformation Matrix] * OriginalVector
	 * 
	 * @param v the Vector4 to be transformed 
	 * @return a newly created Vector4 resulting from the transformation
	 */
	public Vector4 modelToClip(Vector4 v) {
		return transformation.times(v);
	}
	
	/**
	 * Return a new Vector3 resulting from the ModelView transformation ("projection") of the provided Vector3
	 * This methods calls the Vector4 transform(Vector4 v) methods
	 * 
	 * Caution: this method relies on Matrix4 / Vector4 computation hence creates new intermediate objects.
	 * It is less effective in terms of performance and memory usage than the Vector4 transform method.
	 * 
	 * @param v the Vector3 to be transformed 
	 * @return a newly created Vector3 resulting from the transformation
	 */
	public Vector3 modelToClip(Vector3 v) {
		Vector4 v4 = v.getVector4();
		return modelToClip(v4).getVector3();
	}
	
	
	public Triangle modelToWorld(Triangle t){
		Triangle transformed = new Triangle(t);
		
		transformed.setV1(modelToWorld(t.getV1()));
		transformed.setV2(modelToWorld(t.getV2()));
		transformed.setV3(modelToWorld(t.getV3()));
		if (t.getNormal() != null) {
			transformed.setNormal(model.times(t.getNormal().getVector4()));
		}
		return transformed;
	}
	
	public Vertex modelToWorld(Vertex v) {
		Vertex transformed;
		if (v.getNormal() != null) {
			transformed = new Vertex(model.times(v.getPosition()), model.times(v.getNormalV4()));
		} else {
			transformed = new Vertex(model.times(v.getPosition()));
		}
		// Return the newly created Vertex (hence preserve the original Vertex of the Element)
		return transformed;
	}
	


}
