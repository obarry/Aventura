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
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
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
 * This class is a Test class demonstrating usage of the API of the Aventura rendering engine 
 */

public class TestAventura11 {
	
	// GUIView to be displayed
	private SwingView view;
	
	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Aventura 11");
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
	
	public World createWorld() {
		
		// Create a new World
		World world = new World();
		Element e1, e2, e3;
		
		// m1e1 and m1e2  elements
		e1 = new Sphere(1.5f,12);
		e1.setColor(Color.MAGENTA);
		e2 = new Cube(2);
		e2.setColor(Color.ORANGE);
		e3 = new Trellis(3, 2, 5, 4);
		e3.setColor(Color.DARK_GRAY);

		// Translate Elements m1e1 and m1e2 respectively above and below main Element e:
		Translation t1 = new Translation(new Vector3(0, 0, 2));
		Translation t2 = new Translation(new Vector3(0, 0, -2));
		//Rotation r3 = new Rotation(Math.PI/4,Vector3.X_AXIS);
		Translation t3 = new Translation(new Vector3(0, 4, -2));
		e1.setTransformation(t1);
		e2.setTransformation(t2);
		e3.setTransformation(t3);
		

		// Add Element to the world
		world.addElement(e1);
		world.addElement(e2);
		world.addElement(e3);

		// World is created
		return world;
	}

	public Lighting createLight() {
		Lighting lighting = new Lighting();
		return lighting;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");
		
		//Tracer.info = true;
		//Tracer.function = true;

		// Camera
		Vector4 eye = new Vector4(14,8,4,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestAventura11 test = new TestAventura11();
		
		System.out.println("********* Creating World");
		World world = test.createWorld();
		System.out.println("********* Calculating normals");
		world.generate();
		
		Lighting light = test.createLight();
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(pContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
//		renderer.render();

		int nb_images = 360;
		for (int i=0; i<=4.9*nb_images; i++) {
			float a = (float)Math.PI*2*(float)i/(float)nb_images;
			eye = new Vector4(15*(float)Math.cos(a),15*(float)Math.sin(a),4,1);
			System.out.println("Rotation "+i+"  - Eye: "+eye);
			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
			renderer.render();
		}
		
		System.out.println("********* ENDING APPLICATION *********");

	}

}
