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
import com.aventura.model.light.LightAppearance;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Visual test of the sun (a Directional light made visible, RenderContext.setLightGlow(true)): a street between
 * eight tower blocks under a blue sky, seen by a camera that slowly turns left and right, so that the sun, low on
 * the horizon, crosses the field of view and passes behind the edges of the buildings.
 *
 * The visible lights are off during the first sweep of the camera, on during the second one, and so on; the state
 * is printed in the console when it changes.
 *
 * STATE OF THE VISIBLE SUN: the effects of the sun are added one at a time (see the Lighting plan, phase 8):
 * - the disc and the halo of the sun (LightGlowMode.SUN) -- done;
 * - the lens flare (ghosts aligned on the axis sun - center of the screen) -- done;
 * - the light shafts (crepuscular rays through the gaps between the buildings) -- not implemented yet.
 * This demo will show each new effect as soon as it is available, without any change.
 *
 * What to check by eye:
 * - the sun is a bright disc with a soft warm halo, and it is hidden progressively by the edge of a building (the
 *   share of the disc still visible gives the brightness of the halo), and it shows again as soon as it clears it;
 * - the sun stays at the same place of the sky when the camera turns only a little: it moves with the camera's
 *   turn, not with its position;
 * - the halo is added over the buildings too (a glow in the air in front of them), but the disc is never drawn
 *   over a building;
 * - the ghosts of the lens flare (rings and discs of different colors) are aligned on the line from the sun through
 *   the center of the screen, move opposite to the sun when the camera turns, fade when the sun gets near the edge
 *   of the image and disappear when it is hidden behind a building;
 * - the buildings, the ground and the sky are unchanged outside of the sun and of its effects.
 *
 * Once the light shafts are there, they should start from the sun and be visible in the gaps between the buildings
 * only.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingSun
 */
public class TestLightingSun {

	/** Images per sweep of the camera (left to right and back) */
	public static final int NB_IMAGES = 240;

	/** Direction towards the sun (it is the opposite of the direction of its light), low on the horizon, a little to the left of the street */
	private static final Vector3 TO_SUN = new Vector3(-0.30f, 1f, 0.16f).normalize();

	private static final Vector4 EYE = new Vector4(0, -14, 1.8f, 1);
	private static final float VIEW_DISTANCE = 24f;

	/** Color of the disc and of the halo of the sun */
	public static final Color SUN_COLOR = new Color(255, 235, 180);

	/** Sky color: the background color of the World */
	public static final Color SKY = new Color(96, 150, 215);

	/** Tower blocks: x, y of the center, width (x), depth (y) and height */
	private static final float[][] BUILDINGS = {
			{ -9.75f, 20, 3, 3, 7.0f }, { 3, 14, 3, 3, 8f }, { -1, 28, 4, 4, 14f }, { 8, 22, 4, 3, 5f },
			{ -14, 26, 3, 3, 9f }, { 12, 30, 5, 4, 9f }, { -4, 40, 6, 5, 11f }, { 18, 45, 6, 6, 16f } };

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		JFrame frame = new JFrame("Test Lighting Sun");
		frame.setSize(1000, 600);

		view = new SwingView(context, frame);

		JPanel panel = new JPanel() {

			public void paintComponent(Graphics graph) {
				graph.drawImage(view.getImageView(), 0, 0, null);
			}
		};
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width / 2 - frame.getWidth() / 2, dim.height / 2 - frame.getHeight() / 2);
		frame.setVisible(true);

		return view;
	}

	public static PerspectiveContext createPerspectiveContext() {
		return new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1000);
	}

	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(SKY);

		Trellis ground = new Trellis(60, 90, 60, 90);
		ground.setColor(new Color(110, 120, 100));
		ground.setTransformation(new Translation(new Vector3(0, 32, 0)));
		world.addElement(ground);

		for (float[] b : BUILDINGS) {
			Box building = new Box(b[2], b[3], b[4]);
			building.setColor(new Color(150, 140, 135));
			building.setTransformation(new Translation(new Vector3(b[0], b[1], b[4] / 2)));
			world.addElement(building);
		}

		world.build();
		return world;
	}

	/** The sun: a Directional light shining from TO_SUN, with a soft ambient light for the shaded sides. */
	public static Lighting createLighting() {
		DirectionalLight sun = new DirectionalLight(new Vector3(-TO_SUN.getX(), -TO_SUN.getY(), -TO_SUN.getZ()), 1f);
		sun.setAppearance(new LightAppearance().setColor(SUN_COLOR)); // warm halo, the light itself stays white
		return new Lighting(sun, new AmbientLight(0.35f));
	}

	/** Direction of the sun, as seen from the scene (pointing towards it) */
	public static Vector3 getSunDirection() {
		return new Vector3(TO_SUN);
	}

	/** Angle of the camera (degrees, positive to the right) at image i: from -30 to +5 degrees and back, smoothly. */
	public static double yawAt(int i) {
		double phase = (1 - Math.cos(2 * Math.PI * i / NB_IMAGES)) / 2; // 0 -> 1 -> 0
		return -30 + 35 * phase;
	}

	/** The point the camera looks at, at image i: the camera, always at the same place, is turned by yawAt(i) around the vertical axis (at 0 degrees it looks along +y). */
	public static Vector4 poiAt(int i) {
		double yaw = Math.toRadians(yawAt(i));
		return new Vector4(EYE.getX() + (float) (VIEW_DISTANCE * Math.sin(yaw)), EYE.getY() + (float) (VIEW_DISTANCE * Math.cos(yaw)), EYE.getZ(), 1);
	}

	public static Camera createCamera() {
		return new Camera(EYE, poiAt(0), Vector4.zAxis());
	}

	/** The visible lights are off on the even sweeps, on on the odd ones. */
	public static boolean glowAt(int i) {
		return (i / NB_IMAGES) % 2 == 1;
	}

	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		TestLightingSun test = new TestLightingSun();

		World world = createWorld();
		Lighting lighting = createLighting();
		PerspectiveContext pContext = createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		Camera camera = createCamera();
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
		renderer.setView(guiView);

		Boolean current = null;
		for (int i = 0; i <= 4 * NB_IMAGES; i++) {
			boolean glow = glowAt(i);
			if (current == null || glow != current) {
				current = glow;
				rContext.setLightGlow(glow);
				System.out.println("********* Visible lights: " + (glow ? "ON" : "OFF"));
			}
			camera.updateCamera(EYE, poiAt(i), Vector4.zAxis());
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
