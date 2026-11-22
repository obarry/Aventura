/*
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
 */
package com.aventura.test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

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
import com.aventura.model.light.Lighting;
import com.aventura.model.light.SpotLight;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Visual test of the emission (Element.setEmissionColor(), Element.setEmissionMap()): the scene of
 * TestLightingSpot3Shadows (a searchlight sweeping a dark floor with five objects), plus a screen standing on the
 * floor and a glowing ball.
 * <ul>
 * <li>the screen has a texture (a dark panel with a grid, a curve and lines of text) and an emission map: the same
 *     picture, black where the panel is dark. Only the grid, the curve and the text glow;</li>
 * <li>the ball emits a uniform color: a neon lamp.</li>
 * </ul>
 * The first turn of the searchlight is rendered without the emission, the second one with it, and so on; the state is
 * printed in the console when it changes.
 *
 * What to check by eye:
 * - without the emission, the screen and the ball are seen only when the searchlight passes over them, dark otherwise;
 * - with the emission, the grid, the curve and the text of the screen are bright even in the dark and in the shadows,
 *   while the panel around them stays dark (lit by the searchlight only when it passes); the ball glows evenly;
 * - when the searchlight passes over them, its light adds to the emission (brighter, not replaced);
 * - the emission lights nothing around it (it is not a light): the floor next to the screen stays dark;
 * - nothing else changes in the image between the two states.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestEmissionMap
 */
public class TestEmissionMap {

	/** Images per turn of the searchlight */
	public static final int NB_IMAGES = TestLightingSpot3Shadows.NB_IMAGES;

	private static final Color NEON = new Color(120, 255, 170);

	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		JFrame frame = new JFrame("Test Emission Map");
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

	/**
	 * The picture of the screen, drawn with Java2D: a dark panel (or black, for the emission map) with a grid, a curve
	 * and lines of text in bright colors.
	 *
	 * @param panel the color of the panel: dark gray blue for the texture, black for the emission map
	 */
	public static Texture screen(Color panel) {
		int width = 320, height = 200;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(panel);
		g.fillRect(0, 0, width, height);
		// The grid
		g.setColor(new Color(30, 110, 90));
		for (int x = 20; x < 200; x += 30) g.drawLine(x, 20, x, 180);
		for (int y = 20; y < 190; y += 32) g.drawLine(20, y, 200, y);
		// The curve
		g.setColor(new Color(110, 255, 140));
		g.setStroke(new BasicStroke(3));
		int previous = 100;
		for (int x = 21; x < 200; x += 3) {
			int y = (int) (100 - 60 * Math.sin((x - 20) / 22.0) * Math.exp(-(x - 20) / 160.0));
			g.drawLine(x - 3, previous, x, y);
			previous = y;
		}
		// The lines of text, as bars
		g.setColor(new Color(255, 190, 70));
		for (int line = 0; line < 6; line++) g.fillRect(220, 28 + line * 26, 40 + (line * 37) % 50, 8);
		// A red warning lamp
		g.setColor(new Color(255, 60, 40));
		g.fillOval(270, 160, 24, 24);
		g.dispose();

		// Copied upside down: row 0 of a Texture is at v = 0, the bottom of the standing screen (see createScreen())
		Texture texture = new Texture(width, height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				texture.setColor(x, height - 1 - y, new Color(image.getRGB(x, y)));
			}
		}
		return texture;
	}

	/** The screen: a Trellis standing on the floor, turned towards the camera; its emission is set by setEmission() */
	public static Trellis createScreen() {
		Trellis screen = new Trellis(2.6f, 1.6f, 4, 4, screen(new Color(34, 40, 48)));
		// Stood up (90 degrees around X), turned (around Z), then moved: M = T . Rz . Rx
		screen.setTransformation(new Transformation(new Translation(new Vector3(-0.3f, -3.4f, 0.95f))
				.times(new Rotation((float) Math.toRadians(71), Vector3.zAxis()))
				.times(new Rotation((float) Math.toRadians(90), Vector3.xAxis()))));
		return screen;
	}

	/** The ball: a sphere that glows evenly */
	public static Sphere createBall() {
		Sphere ball = new Sphere(0.35f, 24);
		ball.setColor(new Color(60, 70, 64));
		ball.setTransformation(new Translation(new Vector3(1.4f, -3.9f, 0.35f)));
		return ball;
	}

	/** Switches the emission of the screen (its map) and of the ball (a color) on or off */
	public static void setEmission(Element screen, Texture map, Element ball, boolean on) {
		screen.setEmissionMap(on ? map : null);
		ball.setEmissionColor(on ? NEON : null);
	}

	/** The emission is off on the even turns of the searchlight, on on the odd ones. */
	public static boolean emissionAt(int i) {
		return (i / NB_IMAGES) % 2 == 1;
	}

	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		TestEmissionMap test = new TestEmissionMap();

		World world = TestLightingSpot3Shadows.createWorld();
		Trellis screen = createScreen();
		Sphere ball = createBall();
		world.addElement(screen);
		world.addElement(ball);
		world.build();
		Texture map = screen(Color.BLACK); // The same picture, black where the panel is: only the drawing glows

		SpotLight spot = TestLightingSpot3Shadows.createSpot();
		Lighting lighting = TestLightingSpot3Shadows.createLighting(spot);

		PerspectiveContext pContext = TestLightingSpot3Shadows.createPerspectiveContext();
		GUIView guiView = test.createView(pContext);

		RenderContext rContext = TestLightingSpot3Shadows.createRenderContext();
		rContext.setTextureProcessing(true);
		RenderEngine renderer = new RenderEngine(world, lighting, TestLightingSpot3Shadows.createCamera(), rContext, pContext);
		renderer.setView(guiView);

		Boolean current = null;
		for (int i = 0; i <= 4 * NB_IMAGES; i++) {
			boolean on = emissionAt(i);
			if (current == null || on != current) {
				current = on;
				setEmission(screen, map, ball, on);
				System.out.println("********* Emission: " + (on ? "ON" : "OFF"));
			}
			TestLightingSpot3Shadows.aimSpot(spot, i);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
