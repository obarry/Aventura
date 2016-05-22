package com.aventura.engine;

import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.World;

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
	
	RenderContext render;
	GraphicContext graphic;
	World world;
	Lighting light;
	Camera camera;
	
	public RenderEngine(RenderContext render, GraphicContext graphic, World world, Lighting light, Camera camera) {
		this.render = render;
		this.graphic = graphic;
		this.world = world;
		this.light = light;
		this.camera = camera;
	}
	
	/**
	 * 
	 * Rasterization of all triangles of the world
	 * This assumes that the initialization is already done
	 * - projection matrix
	 * - Screen and display area
	 * - etc.
	 */
	public void render() {
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {
			
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
		
		// Scissor test for the triangle
		
		// If triangle is totally or partially in the View Frustum
		// Then render its fragments
		
		
	}

}
