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
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.ClosedCylinder;
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
 * Visual test of PointLight shadows (phase 5: the six-face cube map). Unlike SpotLight's cone,
 * a PointLight radiates (and therefore shadows) in every direction at once -- so the scene here is
 * an open-top room (a floor plus four walls) with one object near each wall, and the light itself
 * orbiting in a small circle above the middle of the room. This is deliberately the ONLY light in
 * the scene (besides a faint ambient, just so nothing goes to pure black outside its reach): there
 * is no secondary directional/spot shadow to compare against here, because the point is to see
 * this light's OWN cube map working on its own, in several directions as it moves.
 *
 * As the light orbits:
 * - each object's shadow sweeps across the floor AND climbs the nearby wall -- something a purely
 *   downward-ish light (directional, or a spot aimed down) cannot show at all: a point light's
 *   shadow of an object sitting between the light and a WALL falls sideways, onto that wall, not
 *   onto the floor. This is the six-face cube map's reason to exist.
 * - the light passes, at different points in its orbit, closer to one wall than the others: the
 *   shadow of the object nearest that wall should sweep up it and back down smoothly, with no
 *   flicker, no stale/lagging frame, and no seam or discontinuity as selectFace() silently switches
 *   which of the six faces is relevant for a given surface point.
 * - the far corners of the room (where two walls meet) are where two different faces' shadow maps
 *   are each responsible for one wall: watch for any visible mismatch exactly at a corner -- there
 *   should be none (see PointLight.selectFace()'s Javadoc on why: every direction belongs to
 *   exactly one face, consistently).
 * - no exception, no black frame, over the whole orbit (two full turns).
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingPoint1Shadows
 * 
 */

public class TestLightingPoint1Shadows {

	/** Number of images for one full turn of the light's orbit. */
	public static final int NB_IMAGES = 180;

	// Room is a square of this half-extent (walls at +/- ROOM_HALF on X and Y), open top.
	private static final float ROOM_HALF = 6f;
	private static final float WALL_HEIGHT = 5f;
	private static final float WALL_THICKNESS = 0.3f;

	private static final float ORBIT_RADIUS = 1.8f;
	private static final float ORBIT_HEIGHT = 3f;
	private static final float POINT_MAX_DISTANCE = 14f;

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Lighting Point 1 Shadows");
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

	public static PerspectiveContext createPerspectiveContext() {
		return new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1250);
	}

	public static Camera createCamera() {
		// Steeply overhead, almost looking straight down into the open-top room: at a shallower
		// angle, the two NEAR walls' outward faces (unlit -- the light is inside the room, so their
		// outside is only as bright as the faint ambient) fill most of the foreground and hide the
		// room's interior. This angle keeps them to a thin band at the screen's edges, so the floor
		// and the two far walls -- where the shadows actually read -- stay in view.
		Vector4 eye = new Vector4(10,-10,18,1);
		Vector4 poi = new Vector4(0,0,1,1);
		return new Camera(eye, poi, Vector4.zAxis());
	}

	/**
	 * An open-top room (floor + four walls, no ceiling -- there would be nothing above to see it
	 * cast a shadow on anyway) with one object of a different shape near the middle of each wall,
	 * close enough that the orbiting light clearly sweeps its shadow across the floor and up that
	 * wall.
	 */
	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(Color.BLACK);

		Trellis floor = new Trellis(2 * ROOM_HALF, 2 * ROOM_HALF, 24, 24);
		floor.setColor(new Color(170, 170, 180));
		world.addElement(floor);

		Color wallColor = new Color(130, 130, 145);
		float wallZ = WALL_HEIGHT / 2f;
		// North / South walls run along X; East / West walls run along Y. Each is a little longer
		// than 2*ROOM_HALF so the four walls' corners overlap rather than leaving a gap.
		Box north = new Box(2 * ROOM_HALF + WALL_THICKNESS, WALL_THICKNESS, WALL_HEIGHT);
		north.setColor(wallColor);
		north.setTransformation(new Translation(new Vector3(0, ROOM_HALF, wallZ)));
		world.addElement(north);

		Box south = new Box(2 * ROOM_HALF + WALL_THICKNESS, WALL_THICKNESS, WALL_HEIGHT);
		south.setColor(wallColor);
		south.setTransformation(new Translation(new Vector3(0, -ROOM_HALF, wallZ)));
		world.addElement(south);

		Box east = new Box(WALL_THICKNESS, 2 * ROOM_HALF + WALL_THICKNESS, WALL_HEIGHT);
		east.setColor(wallColor);
		east.setTransformation(new Translation(new Vector3(ROOM_HALF, 0, wallZ)));
		world.addElement(east);

		Box west = new Box(WALL_THICKNESS, 2 * ROOM_HALF + WALL_THICKNESS, WALL_HEIGHT);
		west.setColor(wallColor);
		west.setTransformation(new Translation(new Vector3(-ROOM_HALF, 0, wallZ)));
		world.addElement(west);

		// One object near each wall, offset from the room's center (where the light orbits) so its
		// shadow visibly sweeps across open floor before reaching the wall, rather than sitting
		// right against it.
		ClosedCylinder column = new ClosedCylinder(2.4f, 0.35f, 24); // near the north wall
		column.setColor(new Color(210, 200, 150));
		column.setTransformation(new Translation(new Vector3(0, ROOM_HALF - 2.5f, 1.2f)));
		world.addElement(column);

		Cube cube = new Cube(1.1f); // near the east wall
		cube.setColor(new Color(200, 60, 60));
		cube.setTransformation(new Translation(new Vector3(ROOM_HALF - 2.5f, 0, 0.55f)));
		world.addElement(cube);

		Sphere sphere = new Sphere(0.6f, 32); // near the west wall
		sphere.setColor(new Color(60, 90, 200));
		sphere.setTransformation(new Translation(new Vector3(-(ROOM_HALF - 2.5f), 0, 0.6f)));
		world.addElement(sphere);

		Cone cone = new Cone(1.6f, 0.6f, 24); // near the south wall -- a distinctive triangular shadow
		cone.setColor(new Color(220, 170, 60));
		cone.setTransformation(new Translation(new Vector3(0, -(ROOM_HALF - 2.5f), 0.8f)));
		world.addElement(cone);

		world.build();
		return world;
	}

	public static PointLight createPointLight() {
		PointLight light = new PointLight(orbitPosition(0), POINT_MAX_DISTANCE);
		return light;
	}

	public static Lighting createLighting(PointLight light) {
		// Faint ambient only, so nothing outside the point light's reach is pure black -- no
		// directional/spot here, deliberately: this demo is about the PointLight's own cube map,
		// not about comparing it with another light's shadow.
		Lighting lighting = new Lighting(new AmbientLight(0.06f));
		lighting.addPointLight(light);
		return lighting;
	}

	/** Position of the light at image i of its orbit: a circle above the room's centre, with a
	 * little vertical bob so it is not always at exactly the same height either. */
	private static Vector4 orbitPosition(int i) {
		double a = 2 * Math.PI * i / NB_IMAGES;
		float x = (float) (ORBIT_RADIUS * Math.cos(a));
		float y = (float) (ORBIT_RADIUS * Math.sin(a));
		float z = ORBIT_HEIGHT + 0.4f * (float) Math.sin(2 * a);
		return new Vector4(x, y, z, 1);
	}

	/** Moves the light to its position for image i of the orbit. */
	public static void moveLight(PointLight light, int i) {
		light.setPosition(orbitPosition(i));
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

		TestLightingPoint1Shadows test = new TestLightingPoint1Shadows();
		
		System.out.println("********* Creating World");
		World world = createWorld();
		System.out.println(world);

		System.out.println("********* Creating Lighting");
		PointLight light = createPointLight();
		Lighting lighting = createLighting(light);

		PerspectiveContext pContext = createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, createCamera(), createRenderContext(), pContext);
		renderer.setView(guiView);
		renderer.render();

		System.out.println("********* Rendering...");
		for (int i=0; i<=2*NB_IMAGES; i++) {
			moveLight(light, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
