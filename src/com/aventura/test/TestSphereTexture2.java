package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.SwingView;
import com.aventura.view.View;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2021 Olivier BARRY
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
 * This class is a Test class demonstrating usage of the API of the Aventura rendering engine 
 */

public class TestSphereTexture2 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Sphere Texture 2");
		// Set the size of the frame
		frame.setSize(1500,880);
		
		// Create the view to be displayed
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");

		// Camera
		Vector4 eye = new Vector4(400,3,3,1);
		Vector4 poi = new Vector4(0,0,-4,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestSphereTexture2 test = new TestSphereTexture2();
		
		System.out.println("********* Creating World");
		
		Texture texearth = new Texture("resources/texture/texture_earthtruecolor_nasa_big_2048x1024.jpg");
		Texture texmoon = new Texture("resources/texture/texture_moon_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_jupiter_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_mars_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_neptune_2048x1024.jpg");
		
		// Create World
		World world = new World();
		
		Sphere earth = new Sphere(12.742f, 48, texearth);
		earth.setSpecularExp(4);
		earth.setSpecularColor(new Color(100,100,100));
		world.addElement(earth);
		
		Sphere moon = new Sphere(3.474f, 48, texmoon);
		moon.setSpecularExp(4);
		moon.setSpecularColor(new Color(100,100,100));
		Translation t = new Translation(new Vector4(384.4f,0,0,0));
		moon.setTransformation(t);
		world.addElement(moon);
		
		world.generate();
		
		System.out.println("********* Creating light");
		DirectionalLight dl = new DirectionalLight(new Vector3(1,-1,0), 1.0f);
		AmbientLight al = new AmbientLight(0.05f);
		Lighting light = new Lighting(dl, al, true);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 1000, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250+625);
		View view = test.createView(gContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);

		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		
		System.out.println("********* Rendering");
		renderer.render();
		System.out.println(renderer.renderStats());
		
//		System.out.println("********* Rotating...");
//		int nb_images = 450;
//		Rotation r = new Rotation(Math.PI*2/(double)nb_images, Vector3.Z_AXIS);
//		for (int i=0; i<=3*nb_images; i++) {
//			//Rotation r = new Rotation(Math.PI*2*(double)i/(double)nb_images, Vector3.Z_AXIS);
//			moon.combineTransformation(r);
//			//cyl.setTransformation(r);
//			renderer.render();
//		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
