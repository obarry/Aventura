package com.aventura.engine;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.test.TestAventura11;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.View;

public class TestRasterizer2 {

	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		Tracer.info = true;
		Tracer.function = true;

		// Camera
		Vector4 eye = new Vector4(14,8,4,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestAventura11 test = new TestAventura11();
		
		System.out.println("********* Creating World");
		World world = new World();
		System.out.println("********* Calculating normals");
		world.calculateNormals();
		
		Lighting light = new Lighting();
		
		GraphicContext gContext = new GraphicContext(0.8, 0.45, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		View view = test.createView(gContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();
				
		System.out.println("********* ENDING APPLICATION *********");


	}

}
