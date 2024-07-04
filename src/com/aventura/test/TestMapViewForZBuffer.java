
package com.aventura.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
<<<<<<< HEAD
import java.util.Scanner;

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
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

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

public class TestMapViewForZBuffer {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting");
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		// Camera
		Vector4 eye = new Vector4(8,-5,5,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestMapViewForZBuffer test = new TestMapViewForZBuffer();
		
		System.out.println("********* Creating World");
		
		Texture tex1 = new Texture("resources/texture/texture_bricks_204x204.jpg");
		Texture tex3 = new Texture("resources/texture/texture_blueground_204x204.jpg");
		Texture tex2 = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		
		World world = new World();
		Trellis trellis = new Trellis(8, 8, 20, 20, tex2);
		Cube cube = new Cube(1, tex1);
		Sphere sphere = new Sphere (0.5f ,10 , tex3);
		// cube.setColor(new Color(200,50,50));
		// Translate cube on top of trellis
		Translation t1 = new Translation(new Vector3(1.5f, 0, 0.5f));
		Translation t2 = new Translation(new Vector3(-1.5f, 0, 0.5f));
		cube.setTransformation(t1);
		sphere.setTransformation(t2);

		world.addElement(trellis);
		world.addElement(cube);
		world.addElement(sphere);
		
		System.out.println("********* Calculating normals");
		world.generate();
		System.out.println(world);

		//DirectionalLight dl = new DirectionalLight(new Vector3(0,1,2));
		AmbientLight al = new AmbientLight(0.05f);
		PointLight pl = new PointLight(new Vector4(3,3,1.2f,1),8);
		//Lighting light = new Lighting(dl, al);
		Lighting light = new Lighting(al);
		light.addPointLight(pl);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(gContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(gUIView);
		MapView mapView = renderer.render();
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Please type return...");
		String str = sc.nextLine();
		sc.close();
=======

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
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

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

public class TestMapViewForZBuffer {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting");
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		// Camera
		Vector4 eye = new Vector4(8,-5,5,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestMapViewForZBuffer test = new TestMapViewForZBuffer();
		
		System.out.println("********* Creating World");
		
		Texture tex1 = new Texture("resources/texture/texture_bricks_204x204.jpg");
		Texture tex3 = new Texture("resources/texture/texture_blueground_204x204.jpg");
		Texture tex2 = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		
		World world = new World();
		Trellis trellis = new Trellis(8, 8, 20, 20, tex2);
		Cube cube = new Cube(1, tex1);
		Sphere sphere = new Sphere (0.5f ,10 , tex3);
		// cube.setColor(new Color(200,50,50));
		// Translate cube on top of trellis
		Translation t1 = new Translation(new Vector3(1.5f, 0, 0.5f));
		Translation t2 = new Translation(new Vector3(-1.5f, 0, 0.5f));
		cube.setTransformation(t1);
		sphere.setTransformation(t2);

		world.addElement(trellis);
		world.addElement(cube);
		world.addElement(sphere);
		
		System.out.println("********* Calculating normals");
		world.generate();
		System.out.println(world);

		//DirectionalLight dl = new DirectionalLight(new Vector3(0,1,2));
		AmbientLight al = new AmbientLight(0.05f);
		PointLight pl = new PointLight(new Vector4(3,3,1.2f,1),8);
		//Lighting light = new Lighting(dl, al);
		Lighting light = new Lighting(al);
		light.addPointLight(pl);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(gContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(gUIView);
		MapView mapView = renderer.render();
>>>>>>> branch 'shadowing' of https://github.com/obarry/Aventura

		//GUIView gUIView = test.createView(GraphicContext.GRAPHIC_DEFAULT);
		mapView.removeFar(gContext.getPerspective().getFar());
		mapView.normalizeMap();
		gUIView.initView(mapView);
		gUIView.renderView();

		System.out.println("********* ENDING APPLICATION *********");
	}
}
