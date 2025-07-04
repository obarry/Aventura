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
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
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

public class TestMultiElementsInterpolate {
	
	// GUIView to be displayed
	private SwingView view;
	
	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Multi Elements Interpolate");
		// Set the size of the frame
		frame.setSize(context.getPixelWidth(), context.getPixelHeight());
		
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
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");
		
		// Camera
		Vector4 eye = new Vector4(10,6,3,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestMultiElementsInterpolate test = new TestMultiElementsInterpolate();
				
		// Create a new World
		System.out.println("********* Creating World");
		World world = new World();
		world.setBackgroundColor(Color.BLACK);
		Element e;
		
		for (int i=-1; i<=1; i++) {
			for (int j=-1; j<=1; j++) {
				for (int k=-1; k<=1; k++) {
										
					// Create an Element of a random type
					switch(Math.round((float)Math.random()*5)) {
					case 0:
						e = new Cone(1,0.5f,32);
						e.setColor(Color.YELLOW);
						break;
					case 1:
						e = new Cylinder(1,0.5f,32);
						e.setColor(Color.CYAN);
						break;
					case 2:
						e = new Sphere(0.8f,32);
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
						e.setColor(Color.GREEN);
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
		
		System.out.println(world);
		for (int i=0; i<world.getNbElements(); i++)
			System.out.println(world.getElement(i));


		// Create lighting
		System.out.println("********* Creating Lighting");
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,0.5f,-0.5f), 0.7f);
		AmbientLight al = new AmbientLight(0.3f);
		Lighting lighting = new Lighting(dl, al);

		PerspectiveContext context = new PerspectiveContext(1.5f, 0.9f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1000);
		GUIView gUIView = test.createView(context);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_WITH_LANDMARKS);
		
		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, context);
		renderer.setView(gUIView);
		renderer.render();
		
		System.out.println("********* Rendering...");
		int nb_images = 360;
		Rotation r1 = new Rotation((float)Math.PI*2/(float)nb_images, Vector3.X_AXIS);
		Rotation r2 = new Rotation((float)Math.PI*2*1.5f/(float)nb_images, Vector3.Y_AXIS);
		Rotation r3 = new Rotation((float)Math.PI*2*2.5f/(float)nb_images, Vector3.Z_AXIS);
		Matrix4 r = r1.times(r2).times(r3);
		for (int i=0; i<=nb_images; i++) {
			world.expandTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");

	}
}
