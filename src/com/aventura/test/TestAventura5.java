package com.aventura.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Transformation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.model.world.Element;
import com.aventura.model.world.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.View;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2016 Olivier BARRY
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

public class TestAventura5 {
	
	// Create the view to be displayed
	private SwingView view;
	
	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Aventura 5");
		// Set the size of the frame
		frame.setSize(1010,630);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestAventura5.this.view.draw(graph);
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
		
		// Create an Element in the World
		Element e = world.createElement();
		
		// Create a Transformation for this Element
		//Rotation r = new Rotation(Math.PI/10, Vector3.Z_AXIS);
		//Rotation r = new Rotation(0, Vector3.Z_AXIS);
		//Scaling s = new Scaling(1);

		// Consolidate the Scaling, Rotation and Translation in a single Transformation object and assign it to the Element
		//Translation t = new Translation(new Vector3(0, 0, 0));
		//Transformation trans = new Transformation(s, r, t);
		Trellis trel = new Trellis(3,2,2,1);
		System.out.println(trel);
		world.addElement(trel);
		//trel.setTransformationMatrix(trans);

		// World is created
		return world;
	}

	public Camera createCamera() {
		
		Vector4 eye = new Vector4(0,-5,0,1);
		Vector4 poi = new Vector4(0,0,0,1);
		
		Camera cam = new Camera(eye, poi, Vector4.Z_AXIS);		
		
		return cam;
	}

	public Lighting createLight() {
		Lighting lighting = new Lighting();
		return lighting;
	}

	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		Tracer.info = true;
		Tracer.function = true;
		
		TestAventura5 test = new TestAventura5();
				
		System.out.println("********* CREATING WORLD");
		World world = test.createWorld();
		Lighting light = test.createLight();
		
		System.out.println("********* CREATING CAMERA");
		Camera camera = test.createCamera();
		
		System.out.println("********* CREATING GRAPHIC CONTEXT");
		GraphicContext context = new GraphicContext(0.8, 0.45, 1, 10, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1000);
		System.out.println(context);
		
		System.out.println("********* CREATING VIEW");
		View view = test.createView(context);
		
		System.out.println("********* CREATING RENDER ENGINE");
		RenderEngine renderer = new RenderEngine(world, light, camera, RenderContext.RENDER_DEFAULT, context);
		renderer.setView(view);
		
		System.out.println("********* RENDERING !!!");
		renderer.render();
		System.out.println("********* ENDING APPLICATION *********");

	}

}
