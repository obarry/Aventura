package com.aventura.model.light;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * @since July 2016
 * 
 */

public class Lighting {
	
	protected AmbientLight ambient;
	protected DirectionalLight directional;
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
	
	public Lighting(DirectionalLight directional, AmbientLight ambient, boolean specularLight) {
		this.ambient = ambient;
		this.directional = directional;
		this.specularLight = specularLight;
	}
	
	public boolean hasAmbient() {
		return ambient!=null ? true : false;
	}
	
	public boolean hasDirectional() {
		return directional!=null ? true : false;
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
	
}
