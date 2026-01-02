package com.aventura.model.light;

import java.util.ArrayList;

import com.aventura.tools.tracing.Tracer;


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
 * Central system and management of all Lighting in Aventura
 * 
 * The lighting system is centralizing all Lights of a scene. Multiple lighting systems are required to render multiple scenes.
 * The Lighting (system) can have one or several lights with the following restrictions (current implementation):
 * - One (or no) Ambient Light for the whole scene
 * - One (or no) Directional Light for the whole scene (future possible evolution with multiple Directional lights)
 * - Multiple (or no) Point Lights (either pure Point Lights or Spot Lights that are specific Point Lights)
 * 
 * All created Lights should be registered here. This is where Rasterizer will find information about all light sources.
 * This is also where the shadowing (when shadoows are activated) will find the list of Lights to calculate the associated ShadowMaps
 * 
 * Future evolution: Lights should also be part of the "extended" World as they may need to be rendered as well if part of the scene (Point Lights or Spot Lights)
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class Lighting {
	
	// One single AmbientLight
	protected AmbientLight ambient;
	// One Directional light in first approach. Future evolution is to support multiple directional lights
	protected ArrayList<DirectionalLight> directionalLights;
	// Multiple Point Lights (includes Point and Spot Lights since the 2nd one is a sub-classs of the first one).
	protected ArrayList<PointLight> pointLights;
	// All point lights and directional lights are shadowing lights, let's have a list of them (used by Rasterizer)
	protected ArrayList<ShadowingLight> shadowingLights;

	
	// In case of multiple lights, the notion of specular reflection may be associated to each directional light (+ 1 general flag to activate/deactivate specular reflection)
	protected boolean specularLight = false; // Default is no specular reflection
	
	public Lighting() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System without any Light.");
	}
	
	public Lighting(DirectionalLight directional) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Directional Light : " + directional);
		if (this.shadowingLights == null) this.shadowingLights  = new ArrayList<ShadowingLight>();
		this.shadowingLights.add(directional);
		if (this.directionalLights == null) this.directionalLights  = new ArrayList<DirectionalLight>();
		this.directionalLights.add(directional);
	}
	
	public Lighting(AmbientLight ambient) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Ambient Light : " + ambient);
		this.ambient = ambient;
	}
	
	public Lighting(DirectionalLight directional, AmbientLight ambient) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Directional Light : " + directional + " and Ambient Light : " + ambient);
		if (this.shadowingLights == null) this.shadowingLights  = new ArrayList<ShadowingLight>();
		this.shadowingLights.add(directional);
		if (this.directionalLights == null) this.directionalLights  = new ArrayList<DirectionalLight>();
		this.directionalLights.add(directional);
		this.ambient = ambient;
		}
	
	public Lighting(DirectionalLight directional, boolean specularLight) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Directional Light : " + directional + "Specular " + (specularLight ? "enabled" : "disabled"));
		if (this.shadowingLights == null) this.shadowingLights  = new ArrayList<ShadowingLight>();
		this.shadowingLights.add(directional);
		if (this.directionalLights == null) this.directionalLights  = new ArrayList<DirectionalLight>();
		this.directionalLights.add(directional);
		
		this.specularLight = specularLight; // TODO Manage multiple Specular Lights
	}
	
	public Lighting(DirectionalLight directional, AmbientLight ambient, boolean specularLight) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Ambient Light : " + ambient + "Specular " + (specularLight ? "enabled" : "disabled"));
		if (this.shadowingLights == null) this.shadowingLights  = new ArrayList<ShadowingLight>();
		this.shadowingLights.add(directional);
		if (this.directionalLights == null) this.directionalLights  = new ArrayList<DirectionalLight>();
		this.directionalLights.add(directional);
		this.specularLight = specularLight;
		this.ambient = ambient;
	}
	
	public boolean hasAmbient() {
		return ambient != null ? true : false;
	}
	
	public boolean hasDirectional() {
		return directionalLights.size() != 0 ? true : false;
	}
	
	public boolean hasPoint() {
		// true if ArrayList is not 0
		return pointLights != null ? (pointLights.size() > 0 ? true : false) : false;
	}
	
	public boolean hasShadowing() {
		// true if ArrayList is not 0
		return shadowingLights != null ? (shadowingLights.size() > 0 ? true : false) : false;		
	}
	
	public boolean hasSpecular() {
		return specularLight;
	}
	 
	public void setSpecularLight(boolean specularLight) {
		this.specularLight = specularLight;		
	}
		
	public AmbientLight getAmbientLight() {
		return ambient;
	}
	
	public void setAmbientLight(AmbientLight ambient) {
		this.ambient = ambient;
	}

	public ArrayList<DirectionalLight> getDirectionalLights() {
		return directionalLights;
	}
	
	public void addDirectionalLight(DirectionalLight directional) {
		if (this.directionalLights == null) this.directionalLights  = new ArrayList<DirectionalLight>();
		if (this.shadowingLights == null) this.shadowingLights  = new ArrayList<ShadowingLight>();
		this.directionalLights.add(directional);
		this.shadowingLights.add(directional);
	}
	
	public void addPointLight(PointLight pointLight) {
		if (this.pointLights == null) this.pointLights  = new ArrayList<PointLight>();
		if (this.shadowingLights == null) this.shadowingLights  = new ArrayList<ShadowingLight>();
		this.pointLights.add(pointLight);
		this.shadowingLights.add(pointLight);
	}
	
	public ArrayList<PointLight> getPointLights() {
		return pointLights;
	}
	
	public ArrayList<ShadowingLight> getShadowingLights() {
		return shadowingLights;
	}

}
