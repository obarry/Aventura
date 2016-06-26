package com.aventura.test;

import com.aventura.engine.GraphicContext;
import com.aventura.engine.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestAventura {
	
	public static View createView() {
		View view = new SwingView();
		return view;
	
	}
		
	public static World createWorld() {
		World world = new World();
		return world;
	}

	public static Lighting createLight() {
		Lighting lighting = new Lighting();
		return lighting;
	}

	public static void main(String[] args) {
		
		World world = createWorld();
		Lighting light = createLight();
		
		Camera camera = new Camera();
		
		System.out.println(GraphicContext.GRAPHIC_DEFAULT);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, RenderContext.RENDER_DEFAULT, GraphicContext.GRAPHIC_DEFAULT);
		renderer.render();
		
		

	}

}
