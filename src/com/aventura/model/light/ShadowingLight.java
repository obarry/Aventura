package com.aventura.model.light;

import com.aventura.context.GraphicContext;
import com.aventura.engine.ModelView;
import com.aventura.math.perspective.Perspective;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.Triangle;


/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2022 Olivier BARRY
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
 * @author Olivier BARRY
 * @since April 2022
 * 
 */
public abstract class ShadowingLight extends Light {
	
	// Fields related to Shadow generation
	protected Camera camera_light;
	protected Perspective perspective_light;
	
	// ModelView matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelView modelView;

	// View Frustum
	protected Vector4 frustumCenter;
	
	// Shadow map
	protected float[][] map; // TODO multiple maps if multiple lights
	
	public abstract void initShadowing(GraphicContext graphicContext, Camera camera_view);
	
	/**
	 * This method will generate the shadow map for the elements of the world passed in parameter with the camera light previously
	 * initiated and light matrix calculated.
	 * It will use similar recursive algorithm than RenderEngine algorithm for rendering world but will only calculate a shadow map without
	 * any more rendering or rasterization calculation.
	 * @param world
	 */
	public void generateShadowMap(World world) {
	
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			generateShadowMap(e, null); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
	}

	protected void generateShadowMap(Element e, Matrix4 matrix) {
		
		// Update ModelView matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
		Matrix4 model = null;
		if (matrix == null) {
			model = e.getTransformation();			
		} else {
			model = matrix.times(e.getTransformation());
		}
		modelView.setModelWithoutNormals(model);
		modelView.computeTransformation(); // Compute the whole ModelView modelView matrix including Camera (view)

		// Calculate projection for all vertices of this Element
		modelView.transformVerticesWithoutNormals(e); // Calculate prj_pos of each vertex
		// TODO Verify that modelView.transformVertices does not calculate normals (not needed here) projection
				
		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test for the triangle
			// If triangle is totally or partially in the View Frustum
			// Then shadowmap this triangle
			if (t.isInViewFrustum()) {
				
				// TBD
				
			}
		}
	
		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				generateShadowMap(e.getSubElements().get(i), model);
			}
		}
	}

	public ModelView getModelView() {
		return modelView;
	}
	
	public float getMap(int x, int y) {
		return map[x][y];
	}

}
