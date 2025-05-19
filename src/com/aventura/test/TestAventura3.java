package com.aventura.test;

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
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.tools.tracing.Tracer;
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

public class TestAventura3 {
	
	// Create the gUIView to be displayed
	private SwingView view;
	
	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Aventura 3");
		// Set the size of the frame
		frame.setSize(1010,630);
		
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
		
		// Consolidate the Scaling, Rotation and Translation in a single Transformation object and assign it to the Element
		for (int j=-2; j<=2; j++) {
			for (int i=-4; i<=4; i++) {
				Translation t = new Translation(new Vector3(i*4, j*5, 0));
				Sphere sph = new Sphere(1.5f,12);
				world.addElement(sph);
				sph.setTransformation(t);
			}
		}

		// World is created
		return world;
	}
	
	public Camera createCamera() {
		
		Vector4 eye = new Vector4(-15,15,20,1);
		Vector4 poi = new Vector4(-5, -0, 0,1);
		
		Camera cam = new Camera(eye, poi, Vector4.Y_AXIS);		
		
		return cam;
	}

	public Lighting createLight() {
		Lighting lighting = new Lighting();
		return lighting;
	}

	public static void main(String[] args) {
	
		Tracer.info = true;
		Tracer.function = true;

		TestAventura3 test = new TestAventura3();

		World world = test.createWorld();
		Lighting light = test.createLight();
		
		Camera camera = test.createCamera();
		
		//PerspectiveContext context = PerspectiveContext.PERSPECTIVE_DEFAULT;
		PerspectiveContext context = new PerspectiveContext();
		context.getPerspective().setHeight(6);
		context.getPerspective().setWidth(10);
		//context.computePerspective();
		System.out.println(context);
		
		GUIView gUIView = test.createView(context);
		RenderEngine renderer = new RenderEngine(world, light, camera, RenderContext.RENDER_DEFAULT, context);
		renderer.setView(gUIView);
		renderer.render();
		
	}

}
