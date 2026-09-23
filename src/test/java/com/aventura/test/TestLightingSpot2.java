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
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.SpotLight;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.model.perspective.PerspectiveType;

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
 * Visual test of a moving SpotLight (animation, no shadows): a "searchlight" fixed above the middle of a floor sweeps a
 * circle around a cube and a sphere, while its cone opens and closes (setLightVector() and setAngles() are called for each image).
 *
 * What to check by eye:
 * - the lit ellipse follows a circle on the floor and lights the cube and the sphere as it passes over them,
 * - the ellipse gets bigger and smaller as the cone opens and closes, and it stretches when it moves away from the camera,
 * - the edge always fades smoothly (the inner angle is 55% of the outer angle),
 * - no flicker, no black frame, no exception.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingSpot2
 * 
 */

public class TestLightingSpot2 {

	/** Number of images for a full turn of the searchlight. */
	public static final int NB_IMAGES = 180;

	private static final Vector4 SPOT_POSITION = new Vector4(0, 0, 5, 1);
	private static final float SWEEP_RADIUS = 3f;

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting Spot 2");
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

	/** Same window and resolution as TestLighting1 and TestLighting2. */
	public static PerspectiveContext createPerspectiveContext() {
		return new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1250);
	}

	public static Camera createCamera() {
		Vector4 eye = new Vector4(8,-5,5,1);
		Vector4 poi = new Vector4(0,0,0,1);
		return new Camera(eye, poi, Vector4.zAxis());
	}

	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(Color.BLACK);

		Trellis trellis = new Trellis(10, 10, 20, 20);
		trellis.setColor(new Color(170, 170, 180));
		Cube cube = new Cube(1);
		cube.setColor(new Color(200, 60, 60));
		Sphere sphere = new Sphere(0.5f, 32);
		sphere.setColor(new Color(60, 90, 200));
		cube.setTransformation(new Translation(new Vector3(1.5f, 0, 0.5f)));
		sphere.setTransformation(new Translation(new Vector3(-1.5f, 0, 0.5f)));

		world.addElement(trellis);
		world.addElement(cube);
		world.addElement(sphere);
		world.build();
		return world;
	}

	public static SpotLight createSpot() {
		SpotLight spot = new SpotLight(SPOT_POSITION, 14);
		aimSpot(spot, 0);
		return spot;
	}

	public static Lighting createLighting(SpotLight spot) {
		Lighting lighting = new Lighting(new AmbientLight(0.05f));
		lighting.addSpotLight(spot);
		return lighting;
	}

	/**
	 * Aim the spot at a point that turns on a circle of the floor and open / close its cone, for image number i.
	 */
	public static void aimSpot(SpotLight spot, int i) {
		double a = 2 * Math.PI * i / NB_IMAGES;
		Vector4 target = new Vector4((float)(SWEEP_RADIUS * Math.cos(a)), (float)(SWEEP_RADIUS * Math.sin(a)), 0.3f, 1);
		spot.setLightVector(new Vector3(SPOT_POSITION, target));

		// The outer angle oscillates between 16 and 28 degrees, twice per turn
		float outer = (float)Math.toRadians(22 + 6 * Math.sin(2 * a));
		spot.setAngles(outer, outer * 0.55f);
	}

	public static RenderContext createRenderContext() {
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setDisplayLandmark(true);
		return rContext;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");

		TestLightingSpot2 test = new TestLightingSpot2();
		
		System.out.println("********* Creating World");
		World world = createWorld();
		System.out.println(world);

		System.out.println("********* Creating Lighting");
		SpotLight spot = createSpot();
		Lighting lighting = createLighting(spot);

		PerspectiveContext pContext = createPerspectiveContext();
		GUIView gUIView = test.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, createCamera(), createRenderContext(), pContext);
		renderer.setView(gUIView);
		renderer.render();

		System.out.println("********* Rendering...");
		for (int i=0; i<=3*NB_IMAGES; i++) {
			aimSpot(spot, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
