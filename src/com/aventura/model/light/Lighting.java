package com.aventura.model.light;

import java.util.ArrayList;

public class Lighting {
	
	protected ArrayList<Light> lights; // There can be multiple sources of light
	
	public Lighting() {
		
	}
	
	public Lighting(Light light) {
		lights.add(light);
	}
	
	public void addLight(Light light) {
		lights.add(light);
	}


}
