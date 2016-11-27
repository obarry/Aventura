package com.aventura.model.light;

import com.aventura.math.vector.Vector4;

public class TestLighting {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Lighting lighting = new Lighting();
		
		DirectionalLight dl = new DirectionalLight(new Vector4(1,1,1,0));
		
		lighting.addLight(dl);
		System.out.println("Light at origin: "+lighting.getLight(new Vector4(0,0,0,1)));

	}

}
