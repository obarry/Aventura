package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.Random;

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
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.Atmosphere;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Visual test of the atmosphere (the haze of the distance, World.setAtmosphere() and RenderContext.setAtmosphere(true)):
 * a long avenue between two rows of buildings, under a blue sky, seen by a camera that slowly travels along it and
 * back again.
 *
 * The atmosphere is always on here (the SceneViewer, scene haze_avenue, lets you switch it on and off to compare).
 *
 * What to check by eye:
 * - the buildings get paler and bluer, and lose their contrast, as they get farther: the last ones are almost the color
 *   of the sky, and the avenue melts into the horizon without a visible edge;
 * - the first buildings, near the camera, keep their colors, and the fading is progressive (no step), also when the
 *   camera moves: a building fades as it gets farther;
 * - the sky is not changed;
 * - the haze does not change when the camera is turned (the distance is the real distance to the camera, not the depth).
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestAtmosphere
 */
public class TestAtmosphere {

	/** Images of a trip along the avenue and back */
	public static final int NB_IMAGES = 400;

	/** Number of trips */
	public static final int NB_TRIPS = 3;

	/** Position of the camera at the start of the avenue (it moves along the Y axis), and the point it looks at */
	public static final Vector4 EYE = new Vector4(0, -10, 1.8f, 1);
	public static final Vector4 POI = new Vector4(0, 20, 1.4f, 1);

	/** Length of the trip, in scene units */
	private static final float TRIP_LENGTH = 140f;

	/** Sky color: the background color of the World, and the color of the haze */
	public static final Color SKY = new Color(130, 175, 225);

	// Buildings of each side of the avenue: one every 14 units, up to 290 units from the start
	private static final int BUILDINGS_PER_SIDE = 21;
	private static final Color[] FACADES = { new Color(150, 90, 75), new Color(140, 140, 135), new Color(175, 150, 110), new Color(90, 105, 125) };

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		JFrame frame = new JFrame("Test Atmosphere");

		view = new SwingView(context, frame);

		JPanel panel = new JPanel() {

			public void paintComponent(Graphics graph) {
				graph.drawImage(view.getImageView(), 0, 0, null);
			}
		};
		// The window fits the image exactly: the panel has the size of the viewport and the frame is packed around it
		panel.setPreferredSize(new Dimension(context.getPixelWidth(), context.getPixelHeight()));
		frame.getContentPane().add(panel);
		frame.setResizable(false);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width / 2 - frame.getWidth() / 2, dim.height / 2 - frame.getHeight() / 2);
		frame.setVisible(true);

		return view;
	}

	/** 1000 x 562 pixels, up to 260 units from the camera */
	public static PerspectiveContext createPerspectiveContext() {
		return new PerspectiveContext(0.8f, 0.45f, 1, 260, PerspectiveType.FRUSTUM, 1250);
	}

	/** The haze: the objects melt into the sky about 220 units away (the contrast is 5% there), clear air for the first 8 units. */
	public static Atmosphere createAtmosphere() {
		return new Atmosphere(220f).setStartDistance(8f);
	}

	/**
	 * The avenue: a road between two rows of buildings of different heights and colors (always the same ones), on a
	 * grass ground, under a blue sky, with the atmosphere of the World.
	 */
	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(SKY);

		Trellis ground = new Trellis(160, 330, 4, 12);
		ground.setColor(new Color(105, 130, 90));
		ground.setTransformation(new Translation(new Vector3(0, 145, 0)));
		world.addElement(ground);

		Box road = new Box(9, 330, 0.06f);
		road.setColor(new Color(70, 72, 78));
		road.setTransformation(new Translation(new Vector3(0, 145, 0.03f)));
		world.addElement(road);

		Random random = new Random(7);
		for (int side = -1; side <= 1; side += 2) {
			for (int k = 0; k < BUILDINGS_PER_SIDE; k++) {
				float width = 6 + 3 * random.nextFloat();
				float depth = 9 + 3 * random.nextFloat();
				float height = 8 + 24 * random.nextFloat();
				Box building = new Box(width, depth, height);
				building.setColor(FACADES[random.nextInt(FACADES.length)]);
				float x = side * (8 + width / 2);
				float y = 4 + 14 * k + 3 * random.nextFloat();
				building.setTransformation(new Translation(new Vector3(x, y, height / 2)));
				world.addElement(building);
			}
		}

		world.setAtmosphere(createAtmosphere());
		world.build();
		return world;
	}

	/** A sun high on one side, and a soft ambient light */
	public static Lighting createLighting() {
		return new Lighting(new DirectionalLight(new Vector3(-0.5f, 0.35f, -0.8f), 0.9f), new AmbientLight(0.4f));
	}

	/** Position of the camera at image i of a trip: from the start of the avenue to TRIP_LENGTH units farther, and back, smoothly. */
	public static Vector4 eyeAt(int i) {
		double phase = (1 - Math.cos(2 * Math.PI * i / NB_IMAGES)) / 2; // 0 -> 1 -> 0
		return new Vector4(EYE.getX(), EYE.getY() + (float) (TRIP_LENGTH * phase), EYE.getZ(), 1);
	}

	/** The point the camera looks at at image i: always 30 units ahead of it */
	public static Vector4 poiAt(int i) {
		Vector4 eye = eyeAt(i);
		return new Vector4(POI.getX(), eye.getY() + (POI.getY() - EYE.getY()), POI.getZ(), 1);
	}

	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		TestAtmosphere test = new TestAtmosphere();

		World world = createWorld();
		Lighting lighting = createLighting();
		PerspectiveContext pContext = createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		Camera camera = new Camera(eyeAt(0), poiAt(0), Vector4.zAxis());
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE).setAtmosphere(true);
		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
		renderer.setView(guiView);

		for (int i = 0; i <= NB_TRIPS * NB_IMAGES; i++) {
			camera.updateCamera(eyeAt(i), poiAt(i), Vector4.zAxis());
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
