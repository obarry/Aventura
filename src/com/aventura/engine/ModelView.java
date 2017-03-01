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
	 * Fully compute Vertex projections resulting from the ModelView transformation for both ModelToWorld and ModelToClip projections
	 * Complete the provided Vertex with projection data but do not modify original position data
	 * 
	 * @param v the provided Vertex
	 */
	public void transform(Vertex v) {
		v.setProjPos(transformation.times(v.getPos()));
		v.setWorldPos(model.times(v.getPos()));
		if (v.getNormal() != null) {
			v.setProjNormal(transformation.times(v.getNormal().V4()).V3());
			v.setWorldNormal(model.times(v.getNormal().V4()).V3());
		}
	}


}
