package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Torus;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

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
 * Visual test of the rendering types (RenderContext.RenderingType) and of the texture and shadow options: the same
 * scene (a sphere, a torus and a textured cube on a textured floor, lit by a directional light) is rendered in turn
 * with each rendering mode. Press Return in the console to show the next image, type q then Return to quit.
 * The mode being displayed is printed in the console and shown in the window title.
 *
 * What to check by eye:
 * - LINE: all the edges, including the hidden ones (back of the sphere, of the torus, of the cube),
 * - MONOCHROME: the same wireframe without the hidden edges; faces filled with the background color, then white,
 * - UNLIT: flat colors and textures without any lighting (no shading, no shadow), with and without edges,
 * - FLAT: shaded facets (one normal per face), the sphere lit like in INTERPOLATE (not dark), with shadows,
 * - INTERPOLATE: smooth shading, with and without textures, with shadows.
 *
 * Run with: mvn test-compile exec:java -Dexec.mainClass=com.aventura.test.TestRenderingModes
 * (from the project root, textures are loaded from resources/texture)
 * 
 * @author Olivier BARRY
 * @since September 2026
 */
public class TestRenderingModes {

	/** One image of the sequence: a title for the console and the RenderContext to use */
	private static class Mode {
		final String title;
		final RenderContext context;

		Mode(String title, RenderContext context) {
			this.title = title;
			this.context = context;
		}
	}

	private static final Mode[] MODES = {
		new Mode("LINE - wireframe, hidden edges visible",
				new RenderContext(RenderingType.LINE)),
		new Mode("MONOCHROME - hidden-line wireframe, faces filled with the background color (default)",
				new RenderContext(RenderContext.RENDER_MONOCHROME)),
		new Mode("MONOCHROME - hidden-line wireframe, faces filled with white (setMonochromeColor)",
				new RenderContext(RenderContext.RENDER_MONOCHROME).setMonochromeColor(Color.WHITE)),
		new Mode("UNLIT - element colors, no lighting",
				new RenderContext(RenderContext.RENDER_STANDARD_UNLIT)),
		new Mode("UNLIT - textures and edges (setTextureProcessing, setRenderingLines)",
				new RenderContext(RenderContext.RENDER_STANDARD_UNLIT).setTextureProcessing(true).setRenderingLines(true)),
		new Mode("FLAT - faceted shading",
				new RenderContext(RenderContext.RENDER_STANDARD_FLAT)),
		new Mode("FLAT - faceted shading, textures and shadows",
				new RenderContext(RenderContext.RENDER_STANDARD_FLAT_SHADOWS).setTextureProcessing(true)),
		new Mode("INTERPOLATE - smooth shading",
				new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE)),
		new Mode("INTERPOLATE - smooth shading, textures and shadows",
				new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS).setTextureProcessing(true)),
	};

	private JFrame frame;
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		frame = new JFrame("Test Rendering Modes");
		frame.setSize(1000, 600);

		view = new SwingView(context, frame);

		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;

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

	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(new Color(20, 20, 30));

		Trellis floor = new Trellis(8, 8, 8, 8, new Texture("resources/texture/texture_bricks_204x204.jpg"));
		floor.setColor(new Color(200, 200, 200));

		Sphere sphere = new Sphere(0.9f, 12);
		sphere.setColor(new Color(230, 80, 80));
		sphere.setTransformation(new Translation(new Vector3(-1.5f, 0, 0.9f)));

		Box cube = new Box(1.4f, 1.4f, 1.4f, new Texture("resources/texture/texture_woodfloor_160x160.jpg"));
		cube.setColor(new Color(90, 200, 120));
		cube.setTransformation(new Translation(new Vector3(1.3f, 0.5f, 0.7f)));

		Torus torus = new Torus(0.8f, 0.25f, 12, 8);
		torus.setColor(new Color(90, 140, 240));
		torus.setTransformation(new Translation(new Vector3(0, -2f, 0.3f)));

		world.addElement(floor);
		world.addElement(sphere);
		world.addElement(cube);
		world.addElement(torus);
		world.build();
		return world;
	}

	public static Lighting createLighting() {
		return new Lighting(new DirectionalLight(new Vector3(-1, 0.6f, -1), 0.8f), new AmbientLight(0.25f));
	}

	public static Camera createCamera() {
		return new Camera(new Vector4(6, -7, 5, 1), new Vector4(0, -0.5f, 0.5f, 1), Vector4.zAxis());
	}

	public static void main(String[] args) throws IOException {

		System.out.println("********* STARTING APPLICATION *********");

		TestRenderingModes test = new TestRenderingModes();
		World world = createWorld();
		Lighting lighting = createLighting();
		Camera camera = createCamera();
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1250);
		GUIView guiView = test.createView(pContext);

		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		int i = 0;
		while (true) {
			Mode mode = MODES[i];
			String header = "Image " + (i + 1) + "/" + MODES.length + " : " + mode.title;

			System.out.println();
			System.out.println("==========================================================================================");
			System.out.println(header);
			System.out.println("==========================================================================================");
			test.frame.setTitle("Test Rendering Modes - " + header);

			// The RenderContext is given at construction: one RenderEngine per mode, same World, view and camera
			RenderEngine renderer = new RenderEngine(world, lighting, camera, mode.context, pContext);
			renderer.setView(guiView);
			renderer.render();

			System.out.print("Return: next image, q + Return: quit > ");
			String line = console.readLine();
			if (line == null || line.trim().equalsIgnoreCase("q") || !test.frame.isDisplayable()) break;
			i = (i + 1) % MODES.length; // Back to the first image after the last one
		}

		test.frame.dispose();
		System.out.println("********* ENDING APPLICATION *********");
	}
}
