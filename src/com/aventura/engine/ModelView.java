package com.aventura.engine;

import com.aventura.math.perspective.Perspective;
import com.aventura.math.vector.Matrix4;
import com.aventura.model.world.Element;
import com.aventura.model.world.Triangle;

/**
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
 *                |   [View Matrix] 4x4 Transformation Matrix -> to transform the world into the camara coordinates
 *                v
 *     +----------------------+
 *     |  Camera Coordinates  |
 *     +----------------------+
 *                |
 *                |   [Projection Matrix] 4x4 Transformation Matrix -> to transform any point or vector into homogeneous coordinates
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
 *  	TransformedVector = [Projection Matrix] * [View Matrix] * [Model Matrix] * OriginalVector
 *  
 *  Note that the Model Matrix is provided by the model itself.
 *  In Aventura model, it is at Element level and it may require to be multiplied recursively if the model contains sub-elements.
 *  See Element class description.
 *  
 * @author Bricolage Olivier
 * @since May 2016
 * 
 */
public class ModelView {
	
	Perspective projection;
	Matrix4 view;
	Matrix4 model;
	
	public ModelView() {
		
	}
	
	public ModelView(Perspective projection, Matrix4 view) {
		this.projection = projection;
		this.view = view;
	}
	
	public void setProjection(Perspective projection) {
		this.projection = projection;
	}
	
	public void setView(Matrix4 view) {
		this.view = view;
	}
	
	public void setModel(Matrix4 model) {
		this.model = model;
	}
	
	public void transform(Triangle t) {
		
		
	}

}
