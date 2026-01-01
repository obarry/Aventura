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
import com.aventura.math.transform.Transformation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Torus;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2026 Olivier BARRY
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

public class TestTwoAttachedRings {

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test 2 attached rings");
		// Set the size of the frame
		frame.setSize(1200,800);
		
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
		
		// Camera
		Vector4 eye = new Vector4(10,3,8,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestTwoAttachedRings test = new TestTwoAttachedRings();
		
		System.out.println("********* Creating World");
		
		World world = new World();
		Torus torus1 = new Torus(2,0.5f,32,16);
		torus1.setSpecularExp(20);
		torus1.setTransformation(new Translation(new Vector4(0,2,0,0)));
		Torus torus2 = new Torus(2,0.5f,32,16);
		torus2.setSpecularExp(20);
		torus2.setTransformation(new Rotation((float)Math.PI/2, Vector4.Y_AXIS));
		torus1.setColor(new Color(20,100,100));
		torus2.setColor(new Color(180,100,20));
		world.addElement(torus1);
		world.addElement(torus2);
		
		System.out.println("********* Generating World");
		world.generate();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,-1,-1));
//		AmbientLight al = new AmbientLight(0.2f);
//		Lighting light = new Lighting(dl, al, true);
		Lighting light = new Lighting(dl, true);
		
		PerspectiveContext pContext = new PerspectiveContext(1.2f, 0.8f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1000);
		GUIView gUIView = test.createView(pContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		//rContext.setDisplayLight(RenderContext.DISPLAY_LIGHT_VECTORS_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setBackFaceCulling(RenderContext.BACKFACE_CULLING_DISABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
		renderer.render();

		System.out.println("********* Rendering...");
		
		System.out.println("********* Rendering...");
		int nb_images = 360;
		Rotation r1 = new Rotation((float)Math.PI*1.1f/(float)nb_images, Vector3.X_AXIS);
		Rotation r2 = new Rotation((float)Math.PI*2*4.1f/(float)nb_images, Vector3.Y_AXIS);
		Rotation r3 = new Rotation((float)Math.PI*2*3.3f/(float)nb_images, Vector3.Z_AXIS);
		Transformation t = new Transformation(r1.times(r2).times(r3));
		for (int i=0; i<=nb_images; i++) {
			world.expandTransformation(t);
			renderer.render();
		}


		System.out.println("********* ENDING APPLICATION *********");
	}
}
