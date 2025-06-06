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
//import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;
//import com.aventura.model.world.shape.Cylinder;
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

public class TestCubeRotationTexture {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Cube Rotation Texture");
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

	public static void main(String[] args) throws InterruptedException {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");

		// Camera
		Vector4 eye = new Vector4(8,3,2,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestCubeRotationTexture test = new TestCubeRotationTexture();
		
		System.out.println("********* Creating World");
		
		World world = new World();
		Cube cube = new Cube(1, tex);
		// Set face colors
		cube.setBottomColor(Color.CYAN);
		cube.setTopColor(Color.ORANGE);
		cube.setLeftColor(Color.DARK_GRAY);
		cube.setRightColor(Color.MAGENTA);
		cube.setFrontColor(Color.PINK);
		cube.setBackColor(Color.LIGHT_GRAY);
		world.addElement(cube);
		
//		Cylinder cylinder1 = new Cylinder(1, 0.5f, 16);
//		Translation tl1 = new Translation(new Vector3(1, 1, 1));
//		cylinder1.setTransformation(tl1);
//		cylinder1.setColor(new Color(240,50,20));
//		world.addElement(cylinder1);
//
//		Cylinder cylinder2 = new Cylinder(1, 0.1f, 16);
//		Translation tl2 = new Translation(new Vector3(1, -1, 1));
//		cylinder2.setTransformation(tl2);
//		cylinder2.setColor(new Color(240,50,20));
//		world.addElement(cylinder2);
		
		System.out.println("********* Calculating normals");
		world.generate();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,-1,0));
		AmbientLight al = new AmbientLight(0.5f);
		Lighting light = new Lighting(dl,al);
		
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(pContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setRendering(RenderContext.RENDERING_TYPE_PLAIN);
		rContext.setRenderingType(RenderContext.RENDERING_TYPE_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_DISABLED);
		//rContext.setBackFaceCulling(RenderContext.BACKFACE_CULLING_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
		renderer.render();
		System.out.println(renderer.renderStats());

		System.out.println("********* Rendering...");
		int nb_images = 180;
		float a;
		for (int i=0; i<=3*nb_images; i++) {
		//for (int i=0; i<1; i++) {
			a = (float)Math.PI*2*(float)i/(float)nb_images;
			eye = new Vector4(8*(float)Math.cos(a),8*(float)Math.sin(a),2,1);
			//System.out.println("Rotation "+i+"  - Eye: "+eye);
			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
			renderer.render();
			//System.out.println(renderer.renderStats());
			Thread.sleep(20);
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
