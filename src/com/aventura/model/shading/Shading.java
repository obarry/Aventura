package com.aventura.model.shading;

import com.aventura.engine.ModelView;
import com.aventura.math.perspective.Orthographic;
import com.aventura.math.transform.LookAt;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2019 Olivier BARRY
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
 * @since July 2019
 * 
 */

/**
 * @author obarry
 *
 */
public class Shading {
	
	protected Lighting lighting; // A reference to the lighting element of the RenderEngine
	protected Vector4 up = Vector4.Y_AXIS; // Default is Y axis (as it is for default Camera up vector)
	
	// Future evolution : multiple directional light would mean multiple cameras
	protected Camera camera_light;
	
	// ModelView matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelView modelview;
	
	// Shadow map
	private float[][] map;
	
	/**
	 * Constructor with lighting reference
	 * @param lighting
	 */
	public Shading(Lighting lighting) {
		this.lighting = lighting;
	}
	
	/**
	 * Getter for Up Vector used to situate the vertical axis of the ShadowMap
	 * @return
	 */
	public Vector4 getUp() {
		return up;
	}

	/**
	 * Setter for Up Vector used to situate the vertical axis of the ShadowMap.
	 * Also initialized using the appropriate Constructor
	 * @param up
	 */
	public void setUp(Vector4 up) {
		this.up = up;
	}


	/**
	 * Set lighting manually if not passed in the Constructor
	 * @param lighting
	 */
	public void setLighting(Lighting lighting) {
		this.lighting = lighting;
	}
	
	/**
	 * Initialize the Shading object by calculating the projection matrix(ces) for the light source(s)
	 * 
	 * As a first implementation, only (1 single) directional light will be used for shading
	 */
	public void initShading(Vector4 up) {
		
		if (up != null) this.up = up; // Otherwise let's keep default value
		
		// For a first implementation : only directional light taken for Shadows calculation
		lighting.getDirectionalLight();
		
		// *** UNDER CONSTRUCTION ***
		
		// Calculate the camera position so that if it has the direction of light, it is targeting the middle of the view frustrum
		
		// For this calculate the 5 points of the View frustum in World coordinates
		// - Eye is one vertex
		// - The 4 points of the fare plane are the other vertices
		// To calculate these 4 vertices we need:
		// - The eye-point of interest (camera direction) normalized vector
		// - The distance to the far plane
		// - The up vector and side vectors
		// - the half width and half eight of the far plane
		// Then:
		// P0 = Eye
		// P1 = Eye + cam_dir*distance + up*half_height + side*half_width
		// P2 = Eye + cam_dir*distance + up*half_height - side*half_width
		// P3 = Eye + cam_dir*distance - up*half_height - side*half_width
		// P4 = Eye + cam_dir*distance - up*half_height + side*half_width
		
		// Then the center of this Frustrum is P = (P0+P1+P2+P3+P4)/5
		// Camera light PoI = P
		// Camera light direction = directional light
		
		
		// LookAt matrix -> direction of the light
		LookAt view = new LookAt(lighting.getDirectionalLight().getLightVector(null).V4(), up);
		
		/*
		 * Mat4 viewMatrix = LookAt(lighting.mCameraPosition,
		 * 							lighting.mCameraPosition + glm::normalize(directionalLight.mLightDirection),
		 * 							Vec3(0.0f, 1.0f, 0.0f));
		 * 
		 * Mat4 lightVP = CreateOrthographicMatrix(lighting.mCameraPosition.x - 25.0f,
		 * 											lighting.mCameraPosition.x + 25.0f,
		 * 											lighting.mCameraPosition.y - 25.0f,
		 * 											lighting.mCameraPosition.y + 25.0f,
		 * 											lighting.mCameraPosition.z + 25.0f,
		 * 											lighting.mCameraPosition.z - 25.0f) * viewMatrix;
		 */
		// Camera -> from LookAt

		// Define the bounding box
		
		/*
		 * From: https://community.khronos.org/t/directional-light-and-shadow-mapping-view-projection-matrices/71386
		 * 
		 Think of light’s orthographic frustum as a bounding box that encloses all objects visible by the camera,
		 plus objects not visible but potentially casting shadows. For the simplicity let’s disregard the latter.
		 
		 So to find this frustum:
		 - find all objects that are inside the current camera frustum
		 - find minimal aa bounding box that encloses them all
		 - transform corners of that bounding box to the light’s space (using light’s view matrix)
		 - find aa bounding box in light’s space of the transformed (now obb) bounding box
		 - this aa bounding box is your directional light’s orthographic frustum.
		 
		 Note that actual translation component in light view matrix doesn’t really matter as you’ll only get different Z values
		 for the frustum but the boundaries will be the same in world space. For the convenience, when building light view matrix,
		 you can assume the light “position” is at the center of the bounding box enclosing all visible objects.
		 */
		
		// Calculate the left, right, bottom, top, near, far distances depending on the View's frustrum planes in the Light's coordinates
		
		// Define camera from Lookat to calculate the shadow map
		
		// Orthographic projection
		// -> From math.perspective
		Matrix4 projection = new Orthographic(0, 0, 0, 0, 0, 0); // To be implemented

		
		// Create the orthographic projection matrix
		modelview = new ModelView(view, projection);
		
		// Create the light's "camera" matrix using the light direction as camera direction
		
	}
	
	public void generateShadowMap() {
		
	}
	
	
//	protected AmbientLight ambient;
//	protected DirectionalLight directional;
//	protected boolean specularLight = false; // Default is no specular reflection
//	
//	public Lighting() {
//	}
//	
//	public Lighting(DirectionalLight directional) {
//		this.directional = directional;
//	}
//	
//	public Lighting(AmbientLight ambient) {
//		this.ambient = ambient;
//	}
//	
//	public Lighting(DirectionalLight directional, AmbientLight ambient) {
//		this.ambient = ambient;
//		this.directional = directional;
//	}
//	
//	public Lighting(DirectionalLight directional, boolean specularLight) {
//		this.directional = directional;
//		this.specularLight = specularLight;
//	}
//	
//	public Lighting(DirectionalLight directional, AmbientLight ambient, boolean specularLight) {
//		this.ambient = ambient;
//		this.directional = directional;
//		this.specularLight = specularLight;
//	}
//	
//	public boolean hasAmbient() {
//		return ambient!=null ? true : false;
//	}
//	
//	public boolean hasDirectional() {
//		return directional!=null ? true : false;
//	}
//	
//	public boolean hasSpecular() {
//		return specularLight;
//	}
//		
//	public AmbientLight getAmbientLight() {
//		return ambient;
//	}
//	
//	public void setAmbientLight(AmbientLight ambient) {
//		this.ambient = ambient;
//	}
//
//	public DirectionalLight getDirectionalLight() {
//		return directional;
//	}
//	
//	public void setDirectionalLight(DirectionalLight directional) {
//		this.directional = directional;
//	}
	
}
