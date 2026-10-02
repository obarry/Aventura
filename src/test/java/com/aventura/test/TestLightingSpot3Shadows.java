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
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.SpotLight;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.ClosedCylinder;
import com.aventura.model.world.shape.Cone;
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
 * Visual test of SpotLight shadows (phase 4), with two lights at once so the two shadow styles
 * read as clearly distinct on screen:
 * - a dim, FIXED DirectionalLight ("moonlight") casts a soft but permanent shadow of every object,
 *   always in the same direction -- the scene never goes flat/shadowless, and it gives a constant
 *   reference to compare the spot's shadow against.
 * - a SpotLight ("searchlight") fixed above the scene sweeps a circle around five objects of
 *   different shapes and heights (a column, two cubes, a sphere, a cone), its cone opening and
 *   closing as it goes (same technique as TestLightingSpot2, now with shadows turned on). Its own
 *   shadow is entirely dynamic: a fresh shadow map is rebuilt from the light's current direction
 *   every frame (ShadowingLight.generateShadowMap(), called once per render() from RenderEngine),
 *   so the sweeping shadow should track the cone exactly, with no lag, smearing or stale frame.
 *
 * What to check by eye:
 * - two shadows per object at once, both present together: the directional one (soft, fixed
 *   direction, always there) and the spot's (sharper-edged while in the cone, following the sweep).
 * - as the spot sweeps past the column, the cube(s), the sphere and the cone, each one's
 *   spot-shadow sweeps across the floor and across its neighbours with it -- the cone's own shadow
 *   is a distinctive triangular silhouette, easy to tell apart from the others -- no flicker, no
 *   stale/lagging shadow, no exception, no black frame.
 * - the lit ellipse itself still behaves as in TestLightingSpot2 (follows the circle, grows/shrinks
 *   as the cone opens/closes, stretches away from the camera, fades smoothly at its edge).
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingSpot3Shadows
 * 
 */

public class TestLightingSpot3Shadows {

	/** Number of images for a full turn of the searchlight. */
	public static final int NB_IMAGES = 180;

	private static final Vector4 SPOT_POSITION = new Vector4(0, 0, 5.5f, 1);
	private static final float SWEEP_RADIUS = 3f;

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting Spot 3 Shadows");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create the view to be displayed
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
		// Pulled back and raised a little from TestLightingSpot2's camera, to fit the taller column
		// and the bigger floor while still reading the shadows clearly on the ground.
		Vector4 eye = new Vector4(9,-7,6,1);
		Vector4 poi = new Vector4(0,0,0,1);
		return new Camera(eye, poi, Vector4.zAxis());
	}

	/**
	 * Five objects of different shapes/heights, placed near (but not exactly on) the spot's sweep
	 * circle (radius SWEEP_RADIUS): close enough that the spot clearly lights and shadows each one
	 * as it passes, off the circle enough that the shadow visibly swings across the floor past them
	 * rather than just vanishing underneath.
	 */
	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(Color.BLACK);

		Trellis trellis = new Trellis(14, 14, 28, 28);
		trellis.setColor(new Color(170, 170, 180));
		world.addElement(trellis);

		// Tall column: casts the longest, most visible raking shadow from the dim directional light.
		ClosedCylinder column = new ClosedCylinder(2.4f, 0.35f, 24);
		column.setColor(new Color(210, 200, 150));
		column.setTransformation(new Translation(new Vector3(0, 2.6f, 1.2f)));
		world.addElement(column);

		Cube cube1 = new Cube(1.1f);
		cube1.setColor(new Color(200, 60, 60));
		cube1.setTransformation(new Translation(new Vector3(2.2f, -1.6f, 0.55f)));
		world.addElement(cube1);

		Cube cube2 = new Cube(0.7f);
		cube2.setColor(new Color(90, 160, 90));
		cube2.setTransformation(new Translation(new Vector3(-1.9f, 1.7f, 0.35f)));
		world.addElement(cube2);

		Sphere sphere = new Sphere(0.6f, 32);
		sphere.setColor(new Color(60, 90, 200));
		sphere.setTransformation(new Translation(new Vector3(-2.3f, -1.3f, 0.6f)));
		world.addElement(sphere);

		// Cone: a fifth, visually distinct shape -- its shadow is a clean triangular silhouette,
		// easy to tell apart from the column's and cubes' rectangular ones as the spot sweeps by.
		Cone cone = new Cone(1.6f, 0.6f, 24);
		cone.setColor(new Color(220, 170, 60));
		cone.setTransformation(new Translation(new Vector3(2.4f, 2.0f, 0.8f)));
		world.addElement(cone);

		world.build();
		return world;
	}

	public static SpotLight createSpot() {
		SpotLight spot = new SpotLight(SPOT_POSITION, 14);
		aimSpot(spot, 0);
		return spot;
	}

	/** Dim, fixed light so every object always casts a soft shadow of its own, as a constant
	 * reference to compare the spot's moving shadow against. Low enough intensity that it never
	 * competes with the spot for which shadow reads as "the interesting one" on screen. */
	public static DirectionalLight createMoonlight() {
		return new DirectionalLight(new Vector3(-1.2f, 0.7f, -0.9f), 0.35f);
	}

	public static Lighting createLighting(SpotLight spot) {
		Lighting lighting = new Lighting(createMoonlight(), new AmbientLight(0.05f));
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
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);
		rContext.setDisplayLandmark(true);
		return rContext;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");

		TestLightingSpot3Shadows test = new TestLightingSpot3Shadows();
		
		System.out.println("********* Creating World");
		World world = createWorld();
		System.out.println(world);

		System.out.println("********* Creating Lighting");
		SpotLight spot = createSpot();
		Lighting lighting = createLighting(spot);

		PerspectiveContext pContext = createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, createCamera(), createRenderContext(), pContext);
		renderer.setView(guiView);
		renderer.render();

		System.out.println("********* Rendering...");
		for (int i=0; i<=3*NB_IMAGES; i++) {
			aimSpot(spot, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
