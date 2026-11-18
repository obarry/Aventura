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
import com.aventura.model.light.LightAppearance;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.SpotLight;
import com.aventura.model.world.World;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Visual test of the visible lights (RenderContext.setLightGlow(true)): the scene of TestLightingSpot3Shadows (a
 * searchlight sweeping a floor with five objects) plus a small warm lamp (a Point light) that circles around the
 * objects, passing behind the column, the cubes and the cone.
 *
 * The first turn of the lamp is rendered without the visible lights (as every rendering was until now), the second
 * one with them, the third one without again, and so on; the state is printed in the console when it changes.
 *
 * What to check by eye:
 * - with the visible lights, the lamp shows as a bright white-ish dot with a soft warm halo around it, and the
 *   searchlight too (its halo is dimmer when the camera is outside of its cone);
 * - the lamp is hidden progressively when an object passes in front of it (no sudden pop, no halo drawn over the
 *   object), and shows again as soon as it clears the object;
 * - the halo shrinks when the lamp moves away from the camera and grows when it comes closer, like any object;
 * - nothing else changes in the image (floor, objects, shadows) between the two states.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingGlow
 */
public class TestLightingGlow {

	/** Images per turn of the lamp */
	public static final int NB_IMAGES = 180;

	private static final float LAMP_RADIUS = 4.4f;
	private static final float LAMP_HEIGHT = 1.1f;

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		JFrame frame = new JFrame("Test Lighting Glow");
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

	/** The lamp: a warm Point light, with a halo a little larger than the default one. */
	public static PointLight createLamp() {
		PointLight lamp = new PointLight(new Vector4(LAMP_RADIUS, 0, LAMP_HEIGHT, 1), 9, 0.8f);
		lamp.setLightColor(new Color(255, 200, 120));
		lamp.setAppearance(new LightAppearance().setGlowRadius(0.7f));
		moveLamp(lamp, 0);
		return lamp;
	}

	/** Puts the lamp on its circle (around the vertical axis), for image number i. */
	public static void moveLamp(PointLight lamp, int i) {
		double a = 2 * Math.PI * i / NB_IMAGES;
		lamp.setPosition(new Vector4((float) (LAMP_RADIUS * Math.cos(a)), (float) (LAMP_RADIUS * Math.sin(a)), LAMP_HEIGHT, 1));
	}

	public static Lighting createLighting(SpotLight spot, PointLight lamp) {
		Lighting lighting = TestLightingSpot3Shadows.createLighting(spot);
		lighting.addPointLight(lamp);
		return lighting;
	}

	/** The visible lights are off on the even turns of the lamp, on on the odd ones. */
	public static boolean glowAt(int i) {
		return (i / NB_IMAGES) % 2 == 1;
	}

	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		TestLightingGlow test = new TestLightingGlow();

		World world = TestLightingSpot3Shadows.createWorld();
		SpotLight spot = TestLightingSpot3Shadows.createSpot();
		PointLight lamp = createLamp();
		Lighting lighting = createLighting(spot, lamp);

		PerspectiveContext pContext = TestLightingSpot3Shadows.createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		RenderContext rContext = TestLightingSpot3Shadows.createRenderContext();
		RenderEngine renderer = new RenderEngine(world, lighting, TestLightingSpot3Shadows.createCamera(), rContext, pContext);
		renderer.setView(guiView);

		Boolean current = null;
		for (int i = 0; i <= 4 * NB_IMAGES; i++) {
			boolean glow = glowAt(i);
			if (current == null || glow != current) {
				current = glow;
				rContext.setLightGlow(glow);
				System.out.println("********* Visible lights: " + (glow ? "ON" : "OFF"));
			}
			TestLightingSpot3Shadows.aimSpot(spot, i);
			moveLamp(lamp, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
