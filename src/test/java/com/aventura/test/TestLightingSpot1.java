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
import com.aventura.model.light.PointLight;
import com.aventura.model.light.SpotLight;
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
 * Visual test of the SpotLight lighting model (static image, no shadows): a cube and a sphere on a floor, lit by
 * - on the left, a white SpotLight with a SOFT edge (inner angle 10 degrees, outer angle 30 degrees): a bright core that fades out smoothly,
 * - on the right, a warm SpotLight with a SHARP edge (inner angle = outer angle = 20 degrees): a disc with a clear border,
 * - behind the objects, a dim PointLight, for reference (unchanged behaviour).
 * Where the two cones overlap the colors add up.
 *
 * What to check by eye:
 * - each spot lights an ellipse of the floor (a circle seen at an angle) centered on its target, the left one aimed at the sphere, the right one at the cube,
 * - the fade is smooth on the left one and abrupt on the right one,
 * - nothing is lit outside of the cones except by the dim ambient light and the PointLight,
 * - the sides of the cube and of the sphere facing away from their spot stay dark (no light through the objects, no shadows yet).
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingSpot1
 * 
 */

public class TestLightingSpot1 {

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting Spot 1");
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
		return new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
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
		// Put the cube and the sphere on the floor
		cube.setTransformation(new Translation(new Vector3(1.5f, 0, 0.5f)));
		sphere.setTransformation(new Translation(new Vector3(-1.5f, 0, 0.5f)));

		world.addElement(trellis);
		world.addElement(cube);
		world.addElement(sphere);
		world.build();
		return world;
	}

	public static Lighting createLighting() {
		AmbientLight al = new AmbientLight(0.05f);

		// Soft edge spot: aimed at the sphere
		Vector4 p1 = new Vector4(-4, -4, 4, 1);
		SpotLight sl1 = new SpotLight(p1, new Vector3(p1, new Vector4(-1.5f, 0, 0.3f, 1)), 14, (float)Math.toRadians(30), (float)Math.toRadians(10));

		// Sharp edge spot (inner angle = outer angle): aimed at the cube
		Vector4 p2 = new Vector4(4, -4, 4, 1);
		SpotLight sl2 = new SpotLight(p2, new Vector3(p2, new Vector4(1.5f, 0, 0.3f, 1)), 14, (float)Math.toRadians(20), (float)Math.toRadians(20));
		sl2.setLightColor(new Color(255, 200, 120));

		// Reference: an unchanged point light behind the objects, dimmed
		PointLight pl = new PointLight(new Vector4(0, 4, 2.5f, 1), 8, 0.5f);

		Lighting lighting = new Lighting(al);
		lighting.addSpotLight(sl1);
		lighting.addSpotLight(sl2);
		lighting.addPointLight(pl);
		return lighting;
	}

	public static RenderContext createRenderContext() {
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		return rContext;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");

		TestLightingSpot1 test = new TestLightingSpot1();
		
		System.out.println("********* Creating World");
		World world = createWorld();
		System.out.println(world);

		System.out.println("********* Creating Lighting");
		Lighting lighting = createLighting();

		PerspectiveContext pContext = createPerspectiveContext();
		GUIView gUIView = test.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, createCamera(), createRenderContext(), pContext);
		renderer.setView(gUIView);

		System.out.println("********* Rendering...");
		renderer.render();

		System.out.println("********* ENDING APPLICATION *********");
	}
}
