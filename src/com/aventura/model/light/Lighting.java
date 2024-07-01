package com.aventura.model.light;

import java.util.ArrayList;

import com.aventura.model.world.shape.Element;


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
 * Central management of all Lighting
 * Specific registration of any Light should be done there. This will allow Rasterizer to go through all lights when required.
 * It also will help the shadowing to calculate the ShadowMaps associated to each Light
 * 
 * Future evolution: Lights should also be part of the World as they should be integrated with the rendering (Point Lights or Spot Lights)
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class Lighting {
	
	// One single AmbientLight
	protected AmbientLight ambient;
	// One Directional light in first approach. Future evolution is to support multiple directional lights
	protected DirectionalLight directional;
	// Multiple Point Lights (includes Point and Spot Lights since the 2nd one is a sub-classs of the first one).
	protected ArrayList<PointLight> pointLights;
	
	// In case of multiple lights, the notion of specular reflection may be associated to each directional light (+ 1 general flag to activate/deactivate specular reflection)
	protected boolean specularLight = false; // Default is no specular reflection
	
	public Lighting() {
	}
	
	public Lighting(DirectionalLight directional) {
		this.directional = directional;
	}
	
	public Lighting(AmbientLight ambient) {
		this.ambient = ambient;
	}
	
	public Lighting(DirectionalLight directional, AmbientLight ambient) {
		this.ambient = ambient;
		this.directional = directional;
	}
	
	public Lighting(DirectionalLight directional, boolean specularLight) {
		this.directional = directional;
		this.specularLight = specularLight;
	}
	
	public Lighting(DirectionalLight directional, AmbientLight ambient, boolean specularLight) {
		this.ambient = ambient;
		this.directional = directional;
		this.specularLight = specularLight;
	}
	
	public boolean hasAmbient() {
		return ambient != null ? true : false;
	}
	
	public boolean hasDirectional() {
		return directional != null ? true : false;
	}
	
	public boolean hasPoint() {
		// true if ArrayList is not 0
		return pointLights != null ? (pointLights.size() > 0 ? true : false) : false;
	}
	
	public boolean hasSpecular() {
		return specularLight;
	}
		
	public AmbientLight getAmbientLight() {
		return ambient;
	}
	
	public void setAmbientLight(AmbientLight ambient) {
		this.ambient = ambient;
	}

	public DirectionalLight getDirectionalLight() {
		return directional;
	}
	
	public void setDirectionalLight(DirectionalLight directional) {
		this.directional = directional;
	}
	
	public void addPointLight(PointLight pointLight) {
		if (this.pointLights == null) this.pointLights  = new ArrayList<PointLight>();
		this.pointLights.add(pointLight);
	}
	
	public ArrayList<PointLight> getPointLights() {
		return pointLights;
	}

}
