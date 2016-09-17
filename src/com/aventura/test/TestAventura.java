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
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.model.world.Element;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
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

public class TestAventura {
	
	// Create the view to be displayed
	private SwingView view;
	
	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Aventura");
		// Set the size of the frame
		frame.setSize(800,450);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestAventura.this.view.draw(graph);
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
		
		// Build the Element: Create vertices and add Triangles to it
		Vertex v1 = new Vertex(new Vector4(0,    0,   -4, 1));
		Vertex v2 = new Vertex(new Vector4(2,  1.4, -2.6, 1));
		Vertex v3 = new Vertex(new Vector4(2, -1.4, -2.6, 1));
		Vertex v4 = new Vertex(new Vector4(2,    0, -5.4, 1));
				
		Triangle t1 = new Triangle(v1, v2, v3);
		Triangle t2 = new Triangle(v2, v3, v4);
		Triangle t3 = new Triangle(v1, v2, v4);
		Triangle t4 = new Triangle(v1, v4, v3);
		
		
		e.addTriangle(t1);
		e.addTriangle(t2);
		e.addTriangle(t3);
		e.addTriangle(t4);
		
		return world;
	}
	
	public Camera createCamera() {
		
		//Vector4 eye = new Vector4(0,0,0,1);
		//Vector4 poi = new Vector4(0,1,1,1);
		
		//Camera cam = new Camera(eye, poi, Vector4.Z_AXIS);
		
		Camera cam = new Camera(Matrix4.IDENTITY); //Camera IDENTITY for testing purpose
		
		return cam;
	}

	public Lighting createLight() {
		Lighting lighting = new Lighting();
		return lighting;
	}

	public static void main(String[] args) {
		
		TestAventura test = new TestAventura();
				
		World world = test.createWorld();
		Lighting light = test.createLight();
		
		Camera camera = test.createCamera();
		
		GraphicContext context = GraphicContext.GRAPHIC_DEFAULT;
		System.out.println(context);
		
		View view = test.createView(context);
		RenderEngine renderer = new RenderEngine(world, light, camera, RenderContext.RENDER_DEFAULT, context);
		renderer.setView(view);
		renderer.render();
		
	}

}
