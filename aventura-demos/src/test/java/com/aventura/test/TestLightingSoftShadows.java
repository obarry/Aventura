package com.aventura.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.RenderEngine;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.SpotLight;
import com.aventura.model.world.World;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Visual test of the soft shadows (percentage-closer filtering, ShadowFilter.PCF_3X3): the scene of
 * TestLightingSpot3Shadows (a searchlight sweeping a floor with five objects, plus a dim moonlight), with a
 * deliberately COARSE shadow map for the spot (SHADOW_MAP_SIZE pixels, instead of 1000), so the texels of the map
 * show on the edges of the shadows, as the stair steps of the hard shadows.
 *
 * The first turn of the searchlight is rendered with hard shadows (ShadowFilter.HARD, the default), the second one
 * with soft shadows (PCF_3X3), the third one with hard shadows again, and so on; the filter in use is printed in the
 * console when it changes.
 *
 * What to check by eye:
 * - with hard shadows, the edges of the shadows of the cone, the column and the cubes are stair-stepped (the
 *   texels of the map), and they can shimmer while the spot moves;
 * - with soft shadows, the same edges are smooth, with a penumbra two or three texels wide, and shimmer less;
 * - the soft shadows keep the same shape and extent as the hard ones (the filter does not blur the objects, nor the
 *   edge of the lit ellipse, nor the shadow of the moonlight, which is not filtered);
 * - no acne (speckles) appears on the floor or on the flanks of the objects with soft shadows;
 * - the shadows do not detach from the foot of the objects (no "peter-panning").
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestLightingSoftShadows
 */
public class TestLightingSoftShadows {

	/** Size of the shadow map of the spot: coarse on purpose, to make the filtering visible. */
	public static final int SHADOW_MAP_SIZE = 256;

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		JFrame frame = new JFrame("Test Lighting Soft Shadows");
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

	/** The filter used at image i of the animation: hard on the even turns of the searchlight, soft on the odd ones. */
	public static ShadowFilter filterAt(int i) {
		return (i / TestLightingSpot3Shadows.NB_IMAGES) % 2 == 0 ? ShadowFilter.HARD : ShadowFilter.PCF_3X3;
	}

	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		TestLightingSoftShadows test = new TestLightingSoftShadows();

		World world = TestLightingSpot3Shadows.createWorld();
		SpotLight spot = TestLightingSpot3Shadows.createSpot();
		spot.setShadowMapSize(SHADOW_MAP_SIZE);
		Lighting lighting = TestLightingSpot3Shadows.createLighting(spot);

		PerspectiveContext pContext = TestLightingSpot3Shadows.createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, TestLightingSpot3Shadows.createCamera(),
				TestLightingSpot3Shadows.createRenderContext(), pContext);
		renderer.setView(guiView);

		ShadowFilter current = null;
		for (int i = 0; i <= 4 * TestLightingSpot3Shadows.NB_IMAGES; i++) {
			ShadowFilter filter = filterAt(i);
			if (filter != current) {
				current = filter;
				spot.setShadowFilter(filter);
				System.out.println("********* Shadow filter: " + filter);
			}
			TestLightingSpot3Shadows.aimSpot(spot, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
