package com.aventura.model.shading;

import com.aventura.engine.ModelView;
import com.aventura.math.transform.LookAt;
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

public class Shading {
	
	protected Lighting lighting; // A reference to the lighting element of the RenderEngine
	
	// Future evolution : multiple directional light would mean multiple cameras
	protected Camera directional_cam;
	
	// ModelView matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelView modelview;
	
	// Shadow map
	private float[][] map;
	
	public Shading(Lighting lighting) {
		this.lighting = lighting;
		lighting.getDirectionalLight();
		
		// *** UNDER CONSTRUCTION ***
		// LookAt matrix -> direction of the light
		LookAt la = new LookAt(lighting.getDirectionalLight().getLightVector(null).V4(), Vector4.Y_AXIS);
		this.directional_cam = new Camera(la, null); // Eye TBD
		// Camera -> from LookAt
		//directional_cam = new Camera(eye, poi, Vector4.Y_AXIS);	
		
		//directional_cam = new Camera();
		// *** END UNDER CONSTRUCTION ***
	}


	public void setLighting(Lighting lighting) {
		this.lighting = lighting;
	}
	
	public void initShading() {
		
		// Define the bounding box
		
		// Create the orthographic projection matrix
		
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
