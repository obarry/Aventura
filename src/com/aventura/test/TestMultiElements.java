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
import com.aventura.math.vector.Tools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Cylinder;
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

public class TestMultiElements {
	
	// GUIView to be displayed
	private SwingView view;
	
	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Multi Elements");
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
		world.setBackgroundColor(Color.DARK_GRAY);
		Element e;
		
		for (int i=-2; i<=2; i++) {
			for (int j=-2; j<=2; j++) {
				for (int k=-2; k<=2; k++) {
										
					// Create an Element of a random type
					switch(Math.round((float)Math.random()*5)) {
					case 0:
						e = new Cone(1,0.5f,8);
						e.setColor(Color.YELLOW);
						break;
					case 1:
						e = new Cylinder(1,0.5f,8);
						e.setColor(Color.CYAN);
						break;
					case 2:
						e = new Sphere(1,8);
						e.setColor(Color.MAGENTA);
						break;
					case 3:
						e = new Cube(1);
						e.setColor(Color.PINK);
						break;
					case 4:
						e = new Box(1,0.5f,0.3f);
						e.setColor(Color.ORANGE);
						break;
					case 5:
						e = new Trellis(1,1,8,8);
						e.setColor(Color.LIGHT_GRAY);
						break;
					default:
						e = null;
					}
					
					// Translate this element at some i,j,k indices of a 3D cube:
					Translation t = new Translation(new Vector3(i*2, j*2, k*2));
					e.setTransformation(t);

					// Add the element to the world
					world.addElement(e);
				}
			}
		}
		// Calculate normals
		world.generate();
		
		// World is created
		return world;
	}

	public Lighting createLight() {
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,-0.8f,-0.5f));
		AmbientLight al = new AmbientLight(0.2f);
		Lighting lighting = new Lighting(dl, al);
		return lighting;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");
		
		// Camera
		Vector4 eyeA = new Vector4(60,40,20,1);
		Vector4 eyeB = new Vector4(4,3,2,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eyeA, poi, Vector4.Z_AXIS);		
				
		TestMultiElements test = new TestMultiElements();
				
		World world = test.createWorld();
		Lighting light = test.createLight();
		PerspectiveContext context = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(context);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		//rContext.setRendering(RenderContext.RENDERING_TYPE_PLAIN);
		rContext.setRenderingType(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, context);
		renderer.setView(gUIView);
		int nb_images = 180;
		for (int i=0; i<=nb_images; i++) {
			Vector4 eye = Tools.interpolate(eyeA, eyeB, (float)i/nb_images);
			System.out.println("Interpolation "+i+"  - Eye: "+eye);
			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
			renderer.render();
		}
		
		for (int i=0; i<=5*nb_images; i++) {
			float a = (float)Math.PI*2*(float)i/(float)nb_images;
			Vector4 eye = new Vector4(30*(float)Math.cos(a),15*(float)Math.sin(a),5,1);
			System.out.println("Rotation "+i+"  - Eye: "+eye);
			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
			renderer.render();
		}
		
		System.out.println("********* ENDING APPLICATION *********");

	}

}
