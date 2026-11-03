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
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.SphericalFog;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Visual test of the fogs of the atmosphere (SphericalFog, Atmosphere.addFog() and RenderContext.setAtmosphere(true)):
 * the avenue of TestAtmosphere with banks of fog at places of the World, seen by a camera that travels along it and
 * back again, through two of the banks.
 *
 * The three trips show the switches of the Atmosphere: the first one has the haze and the fogs, the second one the
 * fogs only (Atmosphere.setHaze(false)), the third one the haze only (Atmosphere.setFog(false)). The mode is printed
 * in the console. The SceneViewer, scene fog_avenue, has a checkbox for each of them.
 *
 * What to check by eye:
 * - a bank of fog is a soft cloud, with no visible edge: it is the thickest in its center and fades away at its
 *   surface, from every side;
 * - the buildings behind a bank are dimmed and take its color, the ones in front of it are not changed;
 * - when the camera enters a bank, the fog is all around: the whole image is dimmed, the sky as well, and the
 *   buildings that are a few units away almost disappear; when it comes out, the view clears up progressively;
 * - the fog is seen above the buildings against the sky, which the haze (a function of the distance only) cannot do.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestFog
 */
public class TestFog {

	/** Images of a trip along the avenue and back */
	public static final int NB_IMAGES = TestAtmosphere.NB_IMAGES;

	/** Number of trips: with the haze and the fogs, with the fogs only, with the haze only */
	public static final int NB_TRIPS = 3;

	/** Color of the fog banks: a pale grey, a little warmer than the sky */
	public static final Color FOG = new Color(214, 216, 220);

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		JFrame frame = new JFrame("Test Fog");

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

	/**
	 * The atmosphere of the avenue (its haze) with four banks of fog: a first one over the road that the camera crosses at
	 * the start of its trip, a thick one farther that it crosses too, one between the buildings of the right side, and a
	 * high one over the left side that floats at the height of the roofs.
	 */
	public static Atmosphere createAtmosphere() {
		Atmosphere atmosphere = TestAtmosphere.createAtmosphere();
		atmosphere.addFog(new SphericalFog(new Vector4(0f, 38f, 3f, 1f), 14f, 30f).setColor(FOG));
		atmosphere.addFog(new SphericalFog(new Vector4(0f, 88f, 2f, 1f), 18f, 16f).setColor(FOG));
		atmosphere.addFog(new SphericalFog(new Vector4(16f, 62f, 14f, 1f), 13f, 22f).setColor(FOG));
		atmosphere.addFog(new SphericalFog(new Vector4(-14f, 120f, 26f, 1f), 18f, 20f).setColor(FOG));
		return atmosphere;
	}

	/** The avenue of TestAtmosphere, with the banks of fog */
	public static World createWorld() {
		World world = TestAtmosphere.createWorld();
		world.setAtmosphere(createAtmosphere());
		return world;
	}

	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		TestFog test = new TestFog();

		World world = createWorld();
		Atmosphere atmosphere = world.getAtmosphere();
		Lighting lighting = TestAtmosphere.createLighting();
		PerspectiveContext pContext = TestAtmosphere.createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		Camera camera = new Camera(TestAtmosphere.eyeAt(0), TestAtmosphere.poiAt(0), Vector4.zAxis());
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE).setAtmosphere(true);
		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
		renderer.setView(guiView);

		for (int i = 0; i <= NB_TRIPS * NB_IMAGES; i++) {
			if (i % NB_IMAGES == 0) {
				int trip = Math.min(i / NB_IMAGES, NB_TRIPS - 1);
				atmosphere.setHaze(trip != 1).setFog(trip != 2);
				System.out.println("Trip " + (trip + 1) + ": " + (atmosphere.isHaze() ? "haze " : "") + (atmosphere.isFog() ? "fogs" : ""));
			}
			camera.updateCamera(TestAtmosphere.eyeAt(i), TestAtmosphere.poiAt(i), Vector4.zAxis());
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
