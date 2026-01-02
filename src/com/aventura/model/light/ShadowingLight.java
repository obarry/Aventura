package com.aventura.model.light;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.ModelViewProjection;
import com.aventura.engine.Rasterizer;
import com.aventura.math.projection.Projection;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.MapView;


/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2026 Olivier BARRY
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
	
	// Default Shadow Map dimension (Shadow Map is Square)
	public static final int DEFAULT_SHADOW_MAP_DIMENSION = 200;
	
	// Parameter for Shadow Mapping "box" definition (used for Light's camera and perspective calculation)
	public static final int SHADOWING_BOX_WORLD = 1; // Use the World's max dimensions to calculate the Light's view box
	public static final int SHADOWING_BOX_VIEWFRUSTUM = 2; // Use the View Frustum to calculate the "box" for this Light's view - Is DEFAULT
	public static final int SHADOWING_BOX_ELEMENT = 3; // Use any Element's max dimensions to calculate the Light's view box
	public static final int SHADOWING_BOX_SPECIFIC = 4; // Use a specific box to calculate the Light's view box

	//protected int shadowingBox_type = SHADOWING_BOX_VIEWFRUSTUM; // Is Default
	protected int shadowingBox_type = SHADOWING_BOX_WORLD; // Is Default
	
	// Fields related to Shadow generation
	protected Camera camera_light; // The corresponding "camera" from Light View's perspective
	protected PerspectiveContext perspectiveCtx_light; // The perspective from the light to generate the shadow map
	protected Rasterizer rasterizer_light; // An instance of rasterizer dedicated to this light to generate the shadow map
	protected ModelViewProjection mvp_light; // ModelViewProjection matrix and vertices conversion tool for the calculation of the Shadow map

	// GUIView Frustum
	//protected Vector4[][] frustum;
	//protected Vector4 frustumCenter;
	
	// World that can cast shadows with that Light, only needed starting ShadowingLight in the class hierarchy
	World world = null;
	
	// Shadow map
	int map_size = 0;
	protected MapView map; // As an attribute of the (Shadowing)Light, there will be multiple maps if multiple lights
	
	// Default constructor
	public ShadowingLight() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight without any parameters.");
		// Nothing else to do here, most of the initialization is done by initShadowing, triggered when needed by RenderEngine (only when shadowing is activated)
	}
		
	/**
	 * Default constructor with intensity
	 * @param intensity
	 */
	public ShadowingLight(float intensity) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. Intensity : " + intensity);
		this.intensity = intensity;
	}

	/**
	 * Constructor with specification of the ShodowingBox type (see constants)
	 * @param shadowingBox_type
	 */
	public ShadowingLight(int shadowingBox_type) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type));
		this.shadowingBox_type = shadowingBox_type;
	}

	/**
	 * Default constructor with intensity
	 * @param intensity
	 */
	public ShadowingLight(float intensity, int shadowingBox_type) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. Intensity : " + intensity + ", ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type));
		this.intensity = intensity;
		this.shadowingBox_type = shadowingBox_type;
	}


	/**
	 * Constructor + Link to the World : to be used when ShadowingBox is of type SHADOWING_BOX_WORLD
	 * @param shadowingBox_type
	 * @param world the World to be used as shadowing box to calculate the shadow map and its perspective
	 */
	public ShadowingLight(int shadowingBox_type, World world) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type) + " + World");
		this.shadowingBox_type = shadowingBox_type;
		this.world = world;
	}

	/**
	 * Generic constructor with specification of the ShodowingBox type (see constants) and intensity of the Light
	 * @param shadowingBox_type
	 * @param intensity
	 */
	public ShadowingLight(int shadowingBox_type, float intensity) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type) + " Intensity : " + intensity);
		this.shadowingBox_type = shadowingBox_type;
		this.intensity = intensity;
	}
	
	/**
	 * Generic Constructor + Link to the World : to be used when ShadowingBox is of type SHADOWING_BOX_WORLD
	 * @param shadowingBox_type
	 * @param intensity
	 * @param world the World to be used as shadowing box to calculate the shadow map and its perspective
	 */
	public ShadowingLight(int shadowingBox_type, float intensity, World world) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : " + toStringShadowingBoxType(shadowingBox_type)+" Intensity : " + intensity + " + World");
		this.shadowingBox_type = shadowingBox_type;
		this.intensity = intensity;
		this.world = world;
	}
	
	public ShadowingLight(float intensity, World world) {
		this.intensity = intensity;
		this.world = world;
	}
	
	public abstract void initShadowing(Perspective perspective, Camera camera_view);
	
	public abstract void initShadowing(Perspective perspective, Camera camera_view, World world);
	
	//public abstract void calculateCameraLight(Perspective perspective, Camera camera_view); 
	
	/**
	 * This method will generate the shadow map for the elements of the world passed in parameter with the camera light previously
	 * initiated and light matrix calculated.
	 * It will use similar recursive algorithm than RenderEngine algorithm for rendering world but will only calculate a shadow map without
	 * any more rendering or rasterization calculation.
	 * @param world
	 */
	public void generateShadowMap(World world) {
	
		// Get the map from the Rasterizer while initializing it
		map = rasterizer_light.initZBuffer(map_size, map_size, 1); // ShadowMap is square

		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			//generateShadowMap(e, null); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
			generateShadowMap(e); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
	}

	//protected void generateShadowMap(Element e, Matrix4 matrix) {
	protected void generateShadowMap(Element e) {
		
		// Update ModelViewProjection matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
//		Matrix4 model = null;
//		if (matrix == null) {
//			model = e.getTransformation();			
//		} else {
//			model = matrix.times(e.getTransformation());
//		}
		
		mvp_light.setModel(e.getTransformation());
		mvp_light.calculateMVPMatrix(); // Compute the whole ModelViewProjection mvp_light matrix including Camera (gUIView)

		// Calculate projection for all vertices of this Element
		mvp_light.transformElement(e, false); // Calculate prj_pos of each vertex of this Element

		// Process each Triangle (this will update the map)
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test for the triangle
			// If triangle is totally or partially in the GUIView Frustum
			// Then shadowmap this triangle
			if (t.isInViewFrustum()) {
				
				// Simplified rasterization : only last parameter is true to indicate this is a shadow map
				rasterizer_light.rasterizeTriangle(t, null, 0, null, false, false, false, true); 
			}
		}

		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				//generateShadowMap(e.getSubElements().get(i), model);
				generateShadowMap(e.getSubElements().get(i));
			}
		}
	}

	public ModelViewProjection getModelView() {
		return mvp_light;
	}
	
	public float getMap(int x, int y) {
		return map.get(x, y);
	}
	
	public MapView getMap() {
		return map;
	}
	
	public String toStringShadowingBoxType(int shadowingBoxType) {

		String shadowingBoxType_string;

		switch (shadowingBoxType) {
		case SHADOWING_BOX_VIEWFRUSTUM:
			shadowingBoxType_string = "SHADOWING_BOX_VIEWFRUSTUM";
			break;
		case SHADOWING_BOX_WORLD:
			shadowingBoxType_string = "SHADOWING_BOX_WORLD";
			break;
		case SHADOWING_BOX_ELEMENT:
			shadowingBoxType_string = "SHADOWING_BOX_ELEMENT";
			break;
		case SHADOWING_BOX_SPECIFIC:
			shadowingBoxType_string = "SHADOWING_BOX_SPECIFIC";
			break;
		default:
			shadowingBoxType_string = "UNKNOWON SHADOWING BOX TYPE";
		}

		return shadowingBoxType_string;
	}

}
