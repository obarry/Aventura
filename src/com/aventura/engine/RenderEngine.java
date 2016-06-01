package com.aventura.engine;

import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.World;
import com.aventura.view.View;

/**
 * This class is the core rendering engine of the Aventura API
 * It provides the render method
 * It needs to be initialized with proper:
 * - world
 * - camera
 * - lighting information
 * - some display and graphics
 * - a render context to provide the information on how to render the world
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */

public class RenderEngine {
	
	// Context's parameters
	RenderContext render;
	GraphicContext graphic;
	
	// Model
	World world;
	Lighting light;
	Camera camera;
	
	// View
	View view;
	
	// ModelView transformation
	ModelView transformation;
	
	public RenderEngine(RenderContext render, GraphicContext graphic, World world, Lighting light, Camera camera) {
		this.render = render;
		this.graphic = graphic;
		this.world = world;
		this.light = light;
		this.camera = camera;
		this.transformation = new ModelView(); // TODO pass parameters for Camera and Word Matrices
	}
	
	/**
	 * 
	 * Rasterization of all triangles of the world
	 * This assumes that the initialization is already done
	 * - Projection matrix
	 * - Screen and display area
	 * - etc.
	 */
	public void render() {
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {
			
			// Calculate the ModelView matrix for this Element (Element <-> Model)
			// TODO Calculate ModelView for this element
			
			Element e = world.getElement(i);
			
			// Process each Triangle
			for (int j=0; j<e.getTriangles().size(); j++) {
				
				render(e.getTriangle(j));
				
			}
		}
	}
	
	/**
	 * Rasterization of a single Triangle
	 * This assumes that the initialization is already done
	 * @param t the triangle to rasterize
	 */
	public void render(Triangle t) {
		// TODO implementation
		
		// Project this Triangle in the View in homogeneous coordinates
		transformation.transform(t);
		
		// Scissor test for the triangle
		
		// If triangle is totally or partially in the View Frustrum
		// Then render its fragments in the View
		
	}

}
