package com.aventura.model.shadow;

import com.aventura.context.GraphicContext;
import com.aventura.engine.ModelView;
import com.aventura.math.perspective.Orthographic;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.camera.LookAt;
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

public class Shadowing {
	
	protected GraphicContext graphicContext; // to get far, near, width, eight distances
	protected Camera camera_view; // to calculate the center of the view frustum using the far, near, width, eight distances
	protected Lighting lighting; // A reference to the lighting element of the RenderEngine
	//protected Vector4 up = Vector4.Y_AXIS; // Default is Y axis (as it is for default Camera up vector)
	
	// Future evolution : multiple directional light would mean multiple cameras
	protected Camera camera_light;
	
	// ModelView matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelView modelview;
	
	// Shadow map
	private float[][] map;
	
	// View Frustum
	protected Vector4 frustumCenter;
	
	
	/**
	 * Constructor with lighting reference
	 * @param lighting
	 */
	public Shadowing(GraphicContext graphicContext, Lighting lighting, Camera cam) {
		this.graphicContext = graphicContext;
		this.camera_view = cam;
		this.lighting = lighting;
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
	public void initShading() {
		
		// Calculate the camera position so that if it has the direction of light, it is targeting the middle of the view frustrum
		
		// For this calculate the 8 points of the View frustum in World coordinates
		// - The 4 points of the near plane		
		// - The 4 points of the fare plane
		
		// To calculate the 8 vertices we need:
		// - The eye position
		Vector4 eye = camera_view.getEye();
		// - The eye-point of interest (camera direction) normalized vector
		Vector4 fwd = camera_view.getForward().normalize();
		// - The distance to the near plane
		float near = graphicContext.getNear();
		// - The distance to the far plane
		float far = graphicContext.getFar();
		// - The up vector and side vectors
		Vector4 up = camera_view.getUp();
		Vector4 side = fwd.times(up).normalize();
		// - the half width and half eight of the near plane
		float half_eight_near = graphicContext.getHeight()/2;
		float half_width_near = graphicContext.getWidth()/2;
		
		// - the half width and half eight of the far plane
		// Calculate the width and height on far plane using Thales: knowing that width and height are defined on the near plane
		// width_far = width_near * far/near
		// height_far = height_near * far/near
		float half_height_far = half_eight_near * far/near;
		float half_width_far = half_width_near * far/near;
		//
		// P11 = Eye + cam_dir*near + up*half_height_near + side*half_width_near
		Vector4 P11 = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).plus(side.times(half_width_near));
		// P12 = Eye + cam_dir*near + up*half_height_near - side*half_width_near
		Vector4 P12 = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P13 = Eye + cam_dir*near - up*half_height_near - side*half_width_near
		Vector4 P13 = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P14 = Eye + cam_dir*near - up*half_height_near + side*half_width_near
		Vector4 P14 = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).plus(side.times(half_width_near));
		//
		// P21 = Eye + cam_dir*far + up*half_height_far + side*half_width_far
		Vector4 P21 = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).plus(side.times(half_width_far));
		// P22 = Eye + cam_dir*far + up*half_height_far - side*half_width_far
		Vector4 P22 = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P23 = Eye + cam_dir*far - up*half_height_far - side*half_width_far
		Vector4 P23 = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P24 = Eye + cam_dir*far - up*half_height_far + side*half_width_far
		Vector4 P24 = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).plus(side.times(half_width_far));
		
		// Then the center of this Frustrum is (P11+P12+P13+P14 + P21+P22+P23+P24)/8
		// We take it as PoI for the Camera light
		Vector4 light_PoI = (P11.plus(P12).plus(P13).plus(P14).plus(P21).plus(P22).plus(P23).plus(P24)).times(1/8);
		
		Vector4 light_dir = lighting.getDirectionalLight().getLightVector(null).V4();
		
		// Build Camera light
		// We need to calculate the camera light Eye
		// In order to calculate the camera light "eye", we start form the PoI : the centre of the view frustum obtained before.
		// We then go back to the direction of light an amount equal to the distance between the near and far z planes of the view frustum.
		// Information found at: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter26/chapter26.html
		Vector4 light_eye = light_PoI.minus(light_dir.times(far-near));
		camera_light = new Camera(light_eye, light_PoI, up);
				
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
		modelview = new ModelView(camera_light.getMatrix(), projection);
		
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
