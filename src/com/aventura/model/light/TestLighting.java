package com.aventura.model.light;

import com.aventura.math.vector.Vector3;

public class TestLighting {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Lighting lighting = new Lighting();
		
		DirectionalLight dl1 = new DirectionalLight(new Vector3(-1,-1,-1), 1);
		DirectionalLight dl2 = new DirectionalLight(new Vector3(0,0,-1), 1);
		DirectionalLight dl3 = new DirectionalLight(new Vector3(0.5,0.5,-1), 1);
			
//		lighting.addLight(dl1);
//		lighting.addLight(dl2);
//		lighting.addLight(dl3);
//		
//		System.out.println("Light at origin: ");
//		Vector4[] lv = lighting.getLightVectors(new Vector4(0,0,0,1));
//		for (int i=0; i<lv.length; i++) {
//			System.out.println(lv[i]);
//		}
//		
//		System.out.println("Average light vector: "+lighting.getAverageDirectionalLightVector());
	}
}
