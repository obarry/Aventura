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
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.SpotLight;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Cylinder;
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
 * Visual test with every kind of light together and shadows enabled (animation): an AmbientLight, a DirectionalLight
 * (white, the strongest, so that its shadows are easy to see), a PointLight (warm) and a SpotLight (cool) light a floor and four shapes that turn around the vertical axis.
 * Meant as a non-regression test of the shadow pipeline while PointLight and SpotLight get their own shadows.
 *
 * What to check by eye TODAY:
 * - the DirectionalLight casts clean shadows on the floor, which turn with the objects, without acne (no stripes on lit surfaces),
 * - enabling shadows does not break the frame or the lighting of the PointLight and of the SpotLight (no exception, no black frame),
 * - the PointLight (warm, on the left) and the SpotLight (cool, on the right, aimed at the middle) light the scene and do NOT cast shadows yet:
 *   the shapes are lit by them "through" each other. This is expected until their shadow maps exist.
 *
 * What should change once PointLight and SpotLight cast shadows: each of them gets its own shadow, of the same color as its light.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingMixedShadows
 * 
 */

public class TestLightingMixedShadows {

	/** Number of images for a full turn of the objects. */
	public static final int NB_IMAGES = 180;

	private static final Translation[] POSITIONS = {
			new Translation(new Vector3(1.8f, 0, 0.5f)),
			new Translation(new Vector3(-1.8f, 0, 0.5f)),
			new Translation(new Vector3(0, 1.8f, 0.5f)),
			new Translation(new Vector3(0, -1.8f, 0))
	};

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting Mixed Shadows");
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

	/** The turning shapes, in the order of POSITIONS (the floor is element 0 of the world and does not turn). */
	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(Color.BLACK);

		Trellis trellis = new Trellis(10, 10, 20, 20);
		trellis.setColor(new Color(170, 170, 180));
		Cube cube = new Cube(1);
		cube.setColor(new Color(200, 60, 60));
		Sphere sphere = new Sphere(0.5f, 32);
		sphere.setColor(new Color(60, 90, 200));
		Cylinder cylinder = new Cylinder(1, 0.4f, 32);
		cylinder.setColor(new Color(60, 180, 90));
		Cone cone = new Cone(1.2f, 0.5f, 32);
		cone.setColor(new Color(230, 200, 60));

		world.addElement(trellis);
		world.addElement(cube);
		world.addElement(sphere);
		world.addElement(cylinder);
		world.addElement(cone);
		setImage(world, 0);
		world.build();
		return world;
	}

	/** Place the shapes for image number i: they all turn around the vertical axis of the middle of the floor. */
	public static void setImage(World world, int i) {
		Rotation r = new Rotation((float)Math.PI*2*(float)i/(float)NB_IMAGES, Vector3.zAxis());
		for (int k=0; k<POSITIONS.length; k++) {
			world.getElement(k+1).setTransformation(new Transformation(r.times(POSITIONS[k])));
		}
	}

	public static Lighting createLighting() {
		AmbientLight al = new AmbientLight(0.05f);

		// Propagation direction: the light comes from the +X, -Y side and goes down
		DirectionalLight dl = new DirectionalLight(new Vector3(-1, 0.5f, -0.5f), 0.9f);

		PointLight pl = new PointLight(new Vector4(-3.5f, 1, 2.5f, 1), 9, 0.4f);
		pl.setLightColor(new Color(255, 190, 120));

		Vector4 p = new Vector4(3.5f, -3.5f, 4, 1);
		SpotLight sl = new SpotLight(p, new Vector3(p, new Vector4(0, 0, 0, 1)), 14, 0.5f, (float)Math.toRadians(30), (float)Math.toRadians(15));
		sl.setLightColor(new Color(150, 190, 255));

		Lighting lighting = new Lighting(dl, al);
		lighting.addPointLight(pl);
		lighting.addSpotLight(sl);
		return lighting;
	}

	public static RenderContext createRenderContext() {
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setShadowing(true);
		rContext.setDisplayLandmark(true);
		return rContext;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");

		TestLightingMixedShadows test = new TestLightingMixedShadows();
		
		System.out.println("********* Creating World");
		World world = createWorld();
		System.out.println(world);

		System.out.println("********* Creating Lighting");
		Lighting lighting = createLighting();

		PerspectiveContext pContext = createPerspectiveContext();
		GUIView gUIView = test.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, createCamera(), createRenderContext(), pContext);
		renderer.setView(gUIView);
		renderer.render();

		System.out.println("********* Rendering...");
		for (int i=0; i<=3*NB_IMAGES; i++) {
			setImage(world, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
