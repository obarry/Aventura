package com.aventura.model.light;

import com.aventura.engine.ModelViewProjection;
import com.aventura.math.projection.Projection;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.view.MapView;


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
 * ShadowingLight is a type of light that can generate Shadows by opposite of other type of Lights e.g. Ambientlight
 * It is also this type of light that can generate "Shaded" light on the surface of World's Elements.
 * The method used for Light calculation is Shadow Mapping hence we will need to have a "Camera Light" that means a Camera corresponding to the Light direction and source
 * For a Directional Light : only a direction, no source (all light rays are parallel in space), the projection should be an Orthographic projection
 * For a Point Light or a Spot Light, a source and a direction is defined. The Camera is located at the source and pointing to the direction of light. A frustum projection
 * is used for the projection.
 * 
 * In this abstract class will be found all the necessary attributes and tools for Shadow generation as the Camera corresponding to the Light
 * the ModelViewProjection projection for this Light (should be Orthographic for a DirectionalLight), the gUIView frustrum and the Shadow Map itself.
 *
 * @author Olivier BARRY
 * @since April 2022
 * 
 */
public abstract class ShadowingLight extends Light {
	
	public static final int DEFAULT_SHADOW_MAP_DIMENSION = 200;
	
	// Fields related to Shadow generation
	protected Camera camera_light;
	protected Projection perspective_light;
	
	// ModelViewProjection matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelViewProjection modelViewProjection;

	// GUIView Frustum
	protected Vector4[][] frustum;
	protected Vector4 frustumCenter;
	
	// Shadow map
	protected MapView map; // As an attribute of the (Shadowing)Light, there will be multiple maps if multiple lights
	
	// Default constructor
	public ShadowingLight() {
		// Nothing to do here, most of the initialization is done by initShadowing, triggered when needed by RenderEngine (only when shadowing is activated)
	}
	
	public ShadowingLight(float intensity) {
		this.intensity = intensity;
	}
	
	public abstract void initShadowing(Perspective perspective, Camera camera_view, int map_size);
	
	public abstract void calculateCameraLight(Perspective perspective, Camera camera_view, Vector3 lightDirection); 
	
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
		
		// Update ModelViewProjection matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
		Matrix4 model = null;
		if (matrix == null) {
			model = e.getTransformation();			
		} else {
			model = matrix.times(e.getTransformation());
		}
		modelViewProjection.setModel(model);
		modelViewProjection.calculateMVPMatrix(); // Compute the whole ModelViewProjection modelViewProjection matrix including Camera (gUIView)

		// Calculate projection for all vertices of this Element
		modelViewProjection.transformElement(e, false); // Calculate prj_pos of each vertex
		// TODO Verify that modelViewProjection.transformVertices does not calculate normals (not needed here) projection

		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test for the triangle
			// If triangle is totally or partially in the GUIView Frustum
			// Then shadowmap this triangle
			if (t.isInViewFrustum()) {
				
				// TBD
				// Use Rasterizer with a dedicated algorithm for Shadow Map generation (simpler than full rasterization) but trying to reuse the
				// common parts.
				// "Strategy" design pattern could be used with a generic interface implementing different rasterization strategies (to be passed
				// in parameter of the generic method) and still keeping only one RasterizeTriangle method. E.g. :
				//     rasterizer.rasterizeTriangle(triangle, rasterizationStrategy);
				
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

	public ModelViewProjection getModelView() {
		return modelViewProjection;
	}
	
	public float getMap(int x, int y) {
		return map.get(x, y);
	}
	
	public MapView getMap() {
		return map;
	}

}
