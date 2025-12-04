
package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.Scanner;

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
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2025 Olivier BARRY
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
 * This class is a Test class for Rasterization of a Shadow Map (inherited from TestmMapViewForZBuffer)
 */

public class TestShadowMapRasterization {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Shadow Map Rasterization");
		// Set the size of the frame
		frame.setSize(1000,1000);
		
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
		Tracer.info = true;
		Tracer.function = true;
		Tracer.debug = true;
		
		// Camera
		//Vector4 eye = new Vector4(-8,-2,12,1);
		//Vector4 poi = new Vector4(0,0,0,1);
		// camera = new Camera(eye, poi, Vector4.Z_AXIS);		

		// Camera similar to Light for testing
		//Vector4 eye = new Vector4(-1,0,0,1);
		//Vector4 eye = new Vector4(0.1f,0.1f,-1,1);
		//Vector4 eye = new Vector4(-8,8,1,1);
		Vector4 eye = new Vector4(8,8,10,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		

		
		TestShadowMapRasterization test = new TestShadowMapRasterization();
		
		System.out.println("********* Creating World");
		
		Texture tex1 = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex3 = new Texture("resources/texture/texture_blueground_204x204.jpg");
		Texture tex2 = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		
		World world = new World();
		Trellis trellis = new Trellis(8, 8, 10, 10, tex2);
		Cube cube = new Cube(1, tex1);
		//Cube cube = new Cube(1);
		//Sphere sphere = new Sphere (0.5f ,10 , tex3);
		Sphere sphere = new Sphere (0.5f ,6);
		sphere.setColor(new Color(0,10,210));
		//Cube cube2 = new Cube(1);
		//cube2.setColor(new Color(0,10,210));
		// cube.setColor(new Color(200,50,50));
		// Translate cube on top of trellis
		Translation t1 = new Translation(new Vector3(1.5f, 0, 0.5f));
		Translation t2 = new Translation(new Vector3(-1.5f, 0, 0.5f));
		cube.setTransformation(t1);
		sphere.setTransformation(t2);
		//cube2.setTransformation(t2);

		world.addElement(trellis);
		world.addElement(cube);
		world.addElement(sphere);
		//world.addElement(cube2);
		
		System.out.println("********* Calculating normals");
		world.generate();
		System.out.println(world);
		System.out.println(trellis);
		System.out.println(cube);
		System.out.println(sphere);
		//System.out.println(cube2);
		
		//DirectionalLight dl = new DirectionalLight(new Vector3(0,1,2));
		AmbientLight al = new AmbientLight(0.25f);
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,-3,-2));
		//DirectionalLight dl = new DirectionalLight(new Vector3(1,3,2));
		//Lighting light = new Lighting(dl, al);
		//Lighting light = new Lighting(al);
		//light.setDirectionalLight(dl);
		Lighting light = new Lighting(dl);
		
		// Regular frustum perspective
		//PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.8f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		// Perspective Orthographic similar to what is used for Shadow Mapping - For Testing
		//PerspectiveContext pContext = new PerspectiveContext(6.4f, 3.6f, 0.1f, 100, PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, 156);
		PerspectiveContext pContext = new PerspectiveContext(10, 10, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, 100);
		GUIView gUIView = test.createView(pContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		//RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_PLAIN);
		//rContext.setBackFaceCulling(RenderContext.BACKFACE_CULLING_DISABLED);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setShadowing(RenderContext.SHADOWING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);
		rContext.setDisplayLight(RenderContext.DISPLAY_LIGHT_VECTORS_ENABLED);
		rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		System.out.println(rContext);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
		MapView map = renderer.render();
		System.out.println("ZBuffer min: "+map.getMin() + ", ZBuffer max: "+map.getMax() + ", ZBuffer avg: " + map.getAverage());
		System.out.println(renderer.renderStats());
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Please type return...");
		sc.nextLine(); // Block until return is typed
		sc.close();
		
		// Now rendering the shadow map for the Directional Light
		MapView mapView = dl.getMap();
		
		if (mapView != null) {
			System.out.println("Now rendering shadow map...");
			mapView.removeFar(pContext.getPerspective().getFar());
			mapView.normalizeMap();
			gUIView.setDimensions(mapView.getViewWidth(), mapView.getViewHeight());
			gUIView.initView(mapView);
			gUIView.renderView();
		} else {
			System.out.println("No shadow map to display.\n");
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
