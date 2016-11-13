package com.aventura.engine;

import com.aventura.context.GraphicContext;
import com.aventura.tools.tracing.Tracer;

public class TestRasterizer1 {

	public static void main(String[] args) {

		System.out.println("********* STARTING TEST RASTERIZER *********");
		
		Tracer.info = true;
		Tracer.function = true;
		
		GraphicContext graphic = GraphicContext.GRAPHIC_DEFAULT;
		
		System.out.println("GraphicContext: "+graphic);
		
		Rasterizer rasterizer = new Rasterizer(graphic);
		
		rasterizer.initZBuffer();

		System.out.println("********* ENDING TEST RASTERIZER *********");
	}

}
