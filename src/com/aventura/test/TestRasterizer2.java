package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;

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
 * This class is a Test class for Rasterizer
 */

public class TestRasterizer2 {

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Rasterizer 2");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create the gUIView to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
		    	graph.drawImage(view.getImageView(), 0, 0, null);
		    }
		};
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
 
		// Locate application frame in the center of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - frame.getWidth()/2, dim.height/2 - frame.getHeight()/2);
		
		// Render the frame on the display
		frame.setVisible(true);
		
		return view;
	}

	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		//Tracer.info = true;
		//Tracer.function = true;

		// Camera
		Vector4 eye = new Vector4(8,3,2,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestRasterizer2 test = new TestRasterizer2();
		
		System.out.println("********* Creating World");
		World world = new World();
		Element e = new Element();
		Vertex v1 = new Vertex(new Vector4(1,0,0,1));
		Vertex v2 = new Vertex(new Vector4(0,1,0,1));
		Vertex v3 = new Vertex(new Vector4(0,0,1,1));
		e.addVertex(v1);
		e.addVertex(v2);
		e.addVertex(v3);
		Triangle t1 = new Triangle(v1, v2, v3);
		t1.setColor(Color.ORANGE);
		e.addTriangle(t1);
		
		Vertex v4 = new Vertex(new Vector4(0,0,0,1));
		Vertex v5 = new Vertex(new Vector4(1,1,1,1));
		Vertex v6 = new Vertex(new Vector4(1,1,0,1));
		e.addVertex(v4);
		e.addVertex(v5);
		e.addVertex(v6);
		Triangle t2 = new Triangle(v4, v5, v6);
		t2.setColor(Color.MAGENTA);
		e.addTriangle(t2);
		
		Vertex v7 = new Vertex(new Vector4(0,0,0,1));
		Vertex v8 = new Vertex(new Vector4(0,2,0.5f,1));
		Vertex v9 = new Vertex(new Vector4(2,0,0.5f,1));
		e.addVertex(v7);
		e.addVertex(v8);
		e.addVertex(v9);
		Triangle t3 = new Triangle(v7, v8, v9);
		t3.setColor(Color.GREEN);
		e.addTriangle(t3);
				
		world.addElement(e);
		
		System.out.println("********* Calculating normals");
		world.generate();
		
		Lighting light = new Lighting();
		AmbientLight al = new AmbientLight(0.5f);
		light.setAmbientLight(al);
		
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(pContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		rContext.setRenderingType(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
		renderer.render();

		int nb_images = 360;
		float a;
		for (int i=0; i<=1.9*nb_images; i++) {
			a = (float)Math.PI*2*(float)i/(float)nb_images;
			eye = new Vector4(8*(float)Math.cos(a),8*(float)Math.sin(a),2,1);
			//System.out.println("Rotation "+i+"  - Eye: "+eye);
			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");


	}

}
