package com.aventura.engine;

import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;

public class RenderEngine {
	
	RenderContext render;
	GraphicContext graphic;
	World world;
	Lighting light;
	Camera camera;
	
	public RenderEngine(RenderContext render, GraphicContext graphic, World world, Lighting light, Camera camera) {
		this.render = render;
		this.graphic = graphic;
		this.world = world;
		this.light = light;
		this.camera = camera;
		
	}
	
	public void render() {
		
	}

}
