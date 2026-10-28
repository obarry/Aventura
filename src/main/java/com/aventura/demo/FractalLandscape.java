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
package com.aventura.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.Timer;
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
import com.aventura.model.light.LightAppearance;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Demo application of the Aventura rendering engine: an interactive fractal landscape, surrounded by a sea and lit by
 * a low sun, that the user explores with the mouse.
 *
 * <h2>What this demo shows of the Aventura API</h2>
 *
 * The {@link #run()} method follows the typical steps of an Aventura application, one method per step:
 * <ol>
 * <li>{@link #createWorld()}: the {@link World} and its {@link Element}s. The ground is a {@link Trellis} (a grid of
 *     triangles) whose altitudes come from a fractal algorithm ({@link #generateFractalAltitudes}); the sea is a flat
 *     Trellis, moved vertically by a {@link Translation}. Both are grouped under a parent Element.</li>
 * <li>{@link #createSun()} and {@link #createLighting(DirectionalLight)}: a {@link DirectionalLight} (the sun, with
 *     soft shadows, a halo and a lens flare) plus an {@link AmbientLight}, gathered in a {@link Lighting}.</li>
 * <li>{@link #createCamera()}: the {@link Camera} (eye, point of interest and up vector, Z being up).</li>
 * <li>{@link #createPerspectiveContext()} and {@link #createRenderContext()}: how the 3D scene is projected on the
 *     screen, and how it is rendered (rendering type, texture, shadows, sun...).</li>
 * <li>{@link RenderEngine}: the engine gathering all of the above.</li>
 * <li>{@link #createView(PerspectiveContext)}: a {@link SwingView} (a {@link GUIView}) displayed in a Swing window,
 *     with its menus and the mouse and keyboard controls.</li>
 * </ol>
 * Then, at each user interaction, the application only changes the model (moves the {@link Camera}, translates the
 * sea, switches an option of the {@link RenderContext}...) and calls {@link RenderEngine#render()} again.
 *
 * <h2>Controls</h2>
 *
 * Two camera modes (Landscape menu), driven by the same mouse gestures:
 * <ul>
 * <li><b>Orbit camera</b> (default), to look at the landscape from outside, as in most 3D viewers:
 *     <ul>
 *     <li>Mouse drag: orbit around the point of interest ("turntable": a horizontal drag turns around the vertical Z
 *         axis, a vertical drag changes the elevation, never up to the poles)</li>
 *     <li>Mouse wheel (or Up / Down arrow keys): zoom, i.e. move the camera toward / away from the point of
 *         interest</li>
 *     </ul></li>
 * <li><b>Fly camera</b>, to fly inside the landscape like a plane, starting from where the orbit camera was:
 *     <ul>
 *     <li>Mouse drag: look around (horizontal drag: turn left / right, vertical drag: look up / down)</li>
 *     <li>Mouse wheel (or Up / Down arrow keys): fly forward / backward along the line of sight, always above the
 *         ground and the sea</li>
 *     </ul>
 *     Going back to the orbit camera puts the camera back on its orbit, where it was.</li>
 * </ul>
 * Common to both modes:
 * <ul>
 * <li>CTRL + U / CTRL + D (or Page Up / Page Down): raise / lower the sea level (keep the key pressed for a
 *     continuous movement)</li>
 * <li>Window resizing: the image always fills the window, at the same scale: a larger window shows more of the
 *     landscape ({@link PerspectiveContext.ResizeMode#KEEP_SCALE})</li>
 * <li>Touchpad: the two-finger vertical scroll and the pinch both act as the mouse wheel (the system sends the pinch
 *     as CTRL + mouse wheel). SHIFT + mouse wheel, the usual convention for a lateral scroll, is left free for a future
 *     camera pan.</li>
 * </ul>
 *
 * <h2>Menus</h2>
 * <ul>
 * <li>Landscape: generate a new landscape, raise / lower the sea level, reset the camera, choose the camera mode</li>
 * <li>Rendering: the rendering type (an exclusive choice)</li>
 * <li>Effects: options independent of the rendering type (texture, specular highlights, shadows, sun, sun halo, lens
 *     flare, light shafts), grayed out when the current rendering type does not use them. The sun is low on the
 *     horizon, in front of the initial camera and a little to its left: orbit a little to the left and lower the
 *     camera to see it.</li>
 * </ul>
 *
 * <h2>Conventions</h2>
 * Z is the up axis. The landscape is a square of {@link #LANDSCAPE_SIZE} units, centered on the origin. Angles are in
 * radians, except the position of the sun, given in degrees for readability.
 *
 * @author Olivier BARRY
 * @since Nov 2023
 */
public class FractalLandscape {

	// ****************************************************************************************************************
	// ***** Constants
	// ****************************************************************************************************************

	// ----- Landscape -----

	/** Side of the square landscape (and of the sea), in units. */
	static final float LANDSCAPE_SIZE = 4f;
	/** Number of cells of the landscape Trellis on each side. Must be a power of 2 for the fractal algorithm. */
	static final int LANDSCAPE_DIVISIONS = 128;
	/** Roughness of the fractal landscape: higher gives a more rugged landscape, lower a smoother one. */
	static final float FRACTAL_ROUGHNESS = 0.6f;

	/** Textures of the ground and of the sea (displayed when the Texture effect is on). */
	static final String LAND_TEXTURE = "resources/texture/texture_gray_concrete_wall_with_cracks_2748x2748.jpg";
	static final String SEA_TEXTURE = "resources/texture/texture_waves-calm-sea_4480x4480.jpg";

	/** Initial altitude of the sea. */
	static final float INITIAL_SEA_LEVEL = 1.5f;
	/** Change of the sea level per key press (CTRL + U / D, Page Up / Down). */
	static final float SEA_LEVEL_STEP = 0.01f;

	// ----- Colors and materials -----

	static final Color SKY_COLOR = new Color(80, 120, 220);
	static final Color SEA_COLOR = new Color(0, 0, 250);

	/** Specular highlights (Effects menu): sharp and bright on the water, faint and wide on the ground. */
	static final Color SEA_SPECULAR_COLOR = new Color(200, 200, 200);
	static final float SEA_SPECULAR_EXP = 200;
	static final Color LAND_SPECULAR_COLOR = new Color(60, 60, 60);
	static final float LAND_SPECULAR_EXP = 8;

	// ----- Lighting -----

	/**
	 * Position of the sun, as seen from the scene, in degrees: azimuth (around the vertical Z axis, from the X axis)
	 * and elevation (above the horizon). The initial camera is on the X axis and looks toward -X (azimuth 180): the sun
	 * is in front of it, a little to its left, low enough to be seen in the sky.
	 */
	static final float SUN_AZIMUTH = 225f;
	static final float SUN_ELEVATION = 15f;
	static final float SUN_INTENSITY = 0.8f;
	/** Ambient light, so that the faces in the shade are not black. */
	static final float AMBIENT_INTENSITY = 0.2f;

	// ----- Camera -----

	/** Initial position of the camera (eye) and point it looks at (point of interest, center of the orbit). */
	static final Vector4 INITIAL_EYE = new Vector4(6, 0, 3, 1);
	static final Vector4 POINT_OF_INTEREST = new Vector4(0, 0, 1.5f, 1);

	/**
	 * Drag sensitivity: dragging the mouse across the whole width of the window turns the camera by
	 * DRAG_ANGLE_PER_WIDTH radians (horizontal angle), across the whole height by DRAG_ANGLE_PER_HEIGHT radians
	 * (vertical angle).
	 */
	static final float DRAG_ANGLE_PER_WIDTH = (float) Math.PI;
	static final float DRAG_ANGLE_PER_HEIGHT = (float) Math.PI / 2;

	/**
	 * Limit of the vertical angle of the camera (orbit elevation, fly pitch), just below the vertical: at the vertical,
	 * the line of sight would be parallel to the up vector (Z) and the orientation of the camera would be undefined.
	 */
	static final float MAX_ELEVATION = (float) Math.PI / 2 - 0.01f;
	/** Minimum distance between the orbit camera and the point of interest, so that the zoom never crosses it. */
	static final float MIN_ORBIT_DISTANCE = 0.2f;

	/** Distance travelled by the camera per notch of the mouse wheel (zoom of the orbit camera, or flight). */
	static final float DOLLY_STEP = 0.1f;
	/**
	 * Minimum height of the fly camera above the ground and the sea. It must stay larger than the distance of the near
	 * plane (see {@link #createPerspectiveContext()}), otherwise the near plane would cut the ground in front of the
	 * camera and show what is under the landscape.
	 */
	static final float FLY_CLEARANCE = 0.03f;

	// ----- Animations and timings -----

	/** A mouse wheel notch does not move the camera at once: it eases over this duration, with frames at this rate. */
	static final int DOLLY_ANIMATION_DURATION_MS = 150;
	static final int DOLLY_ANIMATION_FRAME_MS = 15; // About 60 frames per second
	/**
	 * While the user drags the border of the window, the view is resized (and rendered) only once the size has not
	 * changed for this delay, instead of rendering a full image for each intermediate size.
	 */
	static final int RESIZE_DELAY_MS = 50;

	// ****************************************************************************************************************
	// ***** Fields
	// ****************************************************************************************************************

	// ----- Aventura objects -----

	World world;
	Trellis landTrellis;         // The ground
	float[][] landAltitudes;     // Altitudes of the vertices of the ground: (LANDSCAPE_DIVISIONS + 1)^2 values
	Trellis seaTrellis;          // The sea: a flat Trellis translated at the altitude seaLevel
	float seaLevel = INITIAL_SEA_LEVEL;

	Lighting lighting;
	DirectionalLight sun;
	LightAppearance sunAppearance; // Halo and lens flare of the sun (Effects menu)

	Camera camera;
	PerspectiveContext pContext;
	RenderContext rContext;
	RenderEngine renderer;
	GUIView guiView;

	// ----- Swing user interface -----

	JFrame frame;
	JPanel panel; // Displays the rendered image, fills the window
	Timer resizeTimer;
	// Check boxes of the Effects menu that can be grayed out (see updateEffectsMenu())
	JCheckBoxMenuItem textureItem, specularItem, shadowsItem, softShadowsItem;
	JCheckBoxMenuItem sunItem, sunHaloItem, lensFlareItem, lightShaftsItem;

	// ----- State of the camera -----

	boolean flyMode = false;

	/**
	 * Orbit camera: the camera moves on a sphere centered on the point of interest, described by spherical
	 * coordinates around the up axis Z: azimuth (angle around Z, from the X axis), elevation (angle above the
	 * horizontal plane) and distance (to the point of interest, before the zoom).
	 */
	float orbitAzimuth;
	float orbitElevation;
	float orbitDistance;
	float orbitDolly; // Zoom of the orbit, set aside while flying to get back to it

	/**
	 * Fly camera: position of the eye and direction of the line of sight, given by two angles like for a plane: yaw
	 * (heading, angle around Z from the X axis) and pitch (angle above the horizontal plane).
	 */
	Vector4 flyEye;
	float flyYaw;
	float flyPitch;
	// Start of the flight (where the orbit camera was), for the Reset camera menu
	Vector4 flyStartEye;
	float flyStartYaw, flyStartPitch;

	/**
	 * Mouse wheel and arrow keys move the camera along its line of sight ("dolly", in cinema terms). The movement is
	 * counted in notches of the wheel (a float: a touchpad sends fractions of a notch). dollyTarget is where the user
	 * asked to go, dollyApplied is where the camera is, and moves toward dollyTarget during a short animation.
	 */
	float dollyTarget = 0;
	float dollyApplied = 0;
	float dollyAnimationStart;
	long dollyAnimationStartMillis;
	Timer dollyTimer;

	/** Last known position of the mouse during a drag. */
	int lastMouseX, lastMouseY;

	// ****************************************************************************************************************
	// ***** Main program
	// ****************************************************************************************************************

	public static void main(String[] args) {
		System.out.println("********* STARTING APPLICATION *********");
		FractalLandscape demo = new FractalLandscape();
		demo.run();
		System.out.println("********* APPLICATION is RUNNING *********");
	}

	/**
	 * Creates the scene, the rendering engine and the window, then renders the first image. From then on, the
	 * application is driven by the user (mouse, keyboard and menus, see the class comment).
	 */
	public void run() {

		// 1. The World: landscape and sea
		System.out.println("********* Creating World");
		world = createWorld();

		// 2. The Lighting: the sun and an ambient light
		System.out.println("********* Creating Lighting");
		sun = createSun();
		sunAppearance = sun.getAppearance();
		lighting = createLighting(sun);

		// 3. The Camera, and the orbit it starts on
		camera = createCamera();
		initOrbit();

		// 4. The contexts: projection on the screen, and rendering options
		pContext = createPerspectiveContext();
		rContext = createRenderContext();

		// 5. The RenderEngine, which renders the World, lit by the Lighting, seen by the Camera, with the contexts
		renderer = new RenderEngine(world, lighting, camera, rContext, pContext);

		// 6. The view (where the RenderEngine draws), in a window with its menus and its mouse and keyboard controls.
		// The window is shown only once the RenderEngine knows its view, so that a user action can always be rendered.
		guiView = createView(pContext);
		renderer.setView(guiView);
		frame.setVisible(true);

		// First image
		updateTitle();
		renderer.render();

		System.out.println("********* APPLICATION LAUNCHED *********");
	}

	// ****************************************************************************************************************
	// ***** 1. The World
	// ****************************************************************************************************************

	/**
	 * Creates the World: a fractal landscape (a Trellis whose altitudes come from {@link #generateFractalAltitudes})
	 * and a sea (a flat Trellis of the same size, at the altitude seaLevel), both grouped under a parent Element.
	 * The World is built (vertices, triangles and normals are calculated), then the triangles of the ground are colored
	 * according to their altitude.
	 */
	World createWorld() {

		World world = new World();
		world.setBackgroundColor(SKY_COLOR);

		// The ground: a square Trellis, centered on the origin, of LANDSCAPE_DIVISIONS x LANDSCAPE_DIVISIONS cells,
		// whose vertices take their altitude in the landAltitudes array
		landAltitudes = new float[LANDSCAPE_DIVISIONS + 1][LANDSCAPE_DIVISIONS + 1];
		generateFractalAltitudes(landAltitudes, LANDSCAPE_SIZE, LANDSCAPE_DIVISIONS, new Random());
		try {
			landTrellis = new Trellis(LANDSCAPE_SIZE, LANDSCAPE_SIZE, LANDSCAPE_DIVISIONS, LANDSCAPE_DIVISIONS, landAltitudes,
					new Texture(LAND_TEXTURE));
		} catch (WrongArraySizeException e) {
			// Cannot happen: the array has the size of the Trellis
			throw new IllegalStateException(e);
		}
		landTrellis.setSpecularExp(LAND_SPECULAR_EXP);
		landTrellis.setSpecularColor(LAND_SPECULAR_COLOR);

		// The sea: a flat Trellis (altitude 0 everywhere), raised to the sea level by a Translation. A few cells are
		// enough for a flat surface; they only need to be small enough to stay partly in the view frustum.
		seaTrellis = new Trellis(LANDSCAPE_SIZE, LANDSCAPE_SIZE, 4, 4, new Texture(SEA_TEXTURE));
		seaTrellis.setColor(SEA_COLOR);
		seaTrellis.setSpecularExp(SEA_SPECULAR_EXP);
		seaTrellis.setSpecularColor(SEA_SPECULAR_COLOR);
		seaTrellis.setTransformation(new Translation(new Vector3(0, 0, seaLevel)));

		// An Element without geometry of its own can group other Elements: a Transformation given to it would apply
		// to the whole landscape, ground and sea together
		Element landscape = new Element("landscape");
		landscape.addElement(landTrellis);
		landscape.addElement(seaTrellis);
		world.addElement(landscape);

		// Calculate the geometry of all the Elements of the World: vertices, triangles and normals
		System.out.println("********* Calculating normals");
		world.build();
		printLandscapeStatistics();

		// Colors are given to the triangles, which only exist once the World is built
		colorTrianglesByAltitude(landTrellis);

		return world;
	}

	/**
	 * Fills the altitudes array of a square grid with a fractal landscape, by recursive subdivision (a variant of the
	 * "midpoint displacement" / "diamond-square" algorithms):
	 * <ol>
	 * <li>The 4 corners of the grid get a random altitude.</li>
	 * <li>The grid is a single square. Each square is divided into 4 smaller squares, by calculating the altitude of its
	 *     center and of the middle of its 4 sides: the average of the neighbor points, plus a random value.</li>
	 * <li>Step 2 is repeated on the smaller squares, with random values proportional to their size, until the squares
	 *     are a single cell: large squares give the overall shape of the landscape (mountains, valleys), small ones
	 *     its details.</li>
	 * </ol>
	 * <pre>
	 *  C---M---C     C: corners of the square (known)
	 *  |   |   |     X: center = average of the 4 corners + random value
	 *  M---X---M     M: middle of a side = average of the 2 corners of the side and of the center (before its random
	 *  |   |   |        value) + random value
	 *  C---M---C
	 * </pre>
	 * The middle of a side shared by two neighbor squares is calculated by both: the second value is kept.
	 *
	 * <p>A Random created with a fixed seed always gives the same landscape (used to generate the image of the
	 * documentation, see DocumentationImages in the test sources).
	 *
	 * @param altitudes the array to fill, of size (n + 1) x (n + 1): altitudes[i][j] is the altitude of the vertex in
	 *                  column i and row j
	 * @param size      the size of the landscape (the random values are proportional to it)
	 * @param n         the number of cells on each side, must be a power of 2
	 * @param random    the source of the random variations of the altitudes
	 */
	public static void generateFractalAltitudes(float[][] altitudes, float size, int n, Random random) {

		// 1. Corners of the whole grid
		altitudes[0][0] = (float) random.nextDouble() * FRACTAL_ROUGHNESS * size / 2;
		altitudes[0][n] = (float) random.nextDouble() * FRACTAL_ROUGHNESS * size / 2;
		altitudes[n][0] = (float) random.nextDouble() * FRACTAL_ROUGHNESS * size / 2;
		altitudes[n][n] = (float) random.nextDouble() * FRACTAL_ROUGHNESS * size / 2;

		// 2. and 3. Subdivide squares of side "step" (in cells), from the whole grid down to squares of 2 x 2 cells
		for (int step = n; step > 1; step /= 2) {
			int half = step / 2;
			// The random variations are proportional to the size of the squares: smaller and smaller details
			float amplitude = FRACTAL_ROUGHNESS * (step * size) / (n * 2);

			for (int i = 0; i < n; i += step) {
				for (int j = 0; j < n; j += step) {
					// Square with corners (i, j) and (i + step, j + step)
					float corner00 = altitudes[i][j];
					float corner01 = altitudes[i][j + step];
					float corner10 = altitudes[i + step][j];
					float corner11 = altitudes[i + step][j + step];
					float center = (corner00 + corner01 + corner10 + corner11) / 4;

					altitudes[i + half][j + half] = center + (float) random.nextDouble() * amplitude;
					altitudes[i + half][j] = (corner00 + corner10 + center) / 3 + (float) random.nextDouble() * amplitude;
					altitudes[i][j + half] = (corner00 + corner01 + center) / 3 + (float) random.nextDouble() * amplitude;
					altitudes[i + step][j + half] = (corner10 + corner11 + center) / 3 + (float) random.nextDouble() * amplitude;
					altitudes[i + half][j + step] = (corner01 + corner11 + center) / 3 + (float) random.nextDouble() * amplitude;
				}
			}
		}
	}

	/**
	 * Colors each triangle of a Trellis according to its average altitude: from black (lowest) to white (highest).
	 * With the Texture effect on, the texture is modulated by this color.
	 */
	public static void colorTrianglesByAltitude(Trellis trellis) {
		float minAltitude = trellis.getMinZ();
		float maxAltitude = trellis.getMaxZ();
		for (int t = 0; t < trellis.getTriangles().size(); t++) {
			float altitude = (trellis.getTriangle(t).getV1().getPos().getZ()
					+ trellis.getTriangle(t).getV2().getPos().getZ()
					+ trellis.getTriangle(t).getV3().getPos().getZ()) / 3;
			int gray = (int) ((altitude - minAltitude) / (maxAltitude - minAltitude) * 255);
			trellis.getTriangle(t).setColor(new Color(gray, gray, gray));
		}
	}

	private void printLandscapeStatistics() {
		System.out.println("Landscape: " + landTrellis.getNbTriangles() + " triangles, " + landTrellis.getNbVertices() + " vertices");
	}

	// ****************************************************************************************************************
	// ***** 2. The Lighting
	// ****************************************************************************************************************

	/**
	 * The sun: a DirectionalLight (parallel rays, as from a light at infinity), low on the horizon (see SUN_AZIMUTH,
	 * SUN_ELEVATION). Its shadows are soft (PCF_3X3 filtering of the shadow map) and its LightAppearance gives it a halo
	 * and a lens flare when it is in the field of view (Effects menu).
	 */
	static DirectionalLight createSun() {
		DirectionalLight sun = new DirectionalLight(sunDirection(SUN_AZIMUTH, SUN_ELEVATION), SUN_INTENSITY);
		sun.setShadowFilter(ShadowFilter.PCF_3X3);
		sun.setAppearance(new LightAppearance()); // Default halo and lens flare
		return sun;
	}

	/**
	 * The Lighting of the scene: the sun, an ambient light, and the specular highlights enabled (third parameter).
	 */
	static Lighting createLighting(DirectionalLight sun) {
		return new Lighting(sun, new AmbientLight(AMBIENT_INTENSITY), true);
	}

	/**
	 * Direction of a DirectionalLight for a sun seen from the scene at the given azimuth (around the vertical Z axis,
	 * from the X axis) and elevation (above the horizon), in degrees. Aventura defines a DirectionalLight by the
	 * direction in which its light travels, from the sun toward the scene: the opposite of the direction toward the
	 * sun.
	 */
	static Vector3 sunDirection(float azimuthDegrees, float elevationDegrees) {
		double azimuth = Math.toRadians(azimuthDegrees);
		double elevation = Math.toRadians(elevationDegrees);
		return new Vector3(
				(float) (-Math.cos(elevation) * Math.cos(azimuth)),
				(float) (-Math.cos(elevation) * Math.sin(azimuth)),
				(float) (-Math.sin(elevation)));
	}

	// ****************************************************************************************************************
	// ***** 3. The Camera
	// ****************************************************************************************************************

	/** The Camera at its initial position, looking at the point of interest, with Z as up vector. */
	static Camera createCamera() {
		return new Camera(new Vector4(INITIAL_EYE), new Vector4(POINT_OF_INTEREST), Vector4.zAxis());
	}

	/** Initial orbit: spherical coordinates of the initial eye around the point of interest, no zoom. */
	private void initOrbit() {
		Vector4 poiToEye = INITIAL_EYE.minus(POINT_OF_INTEREST);
		orbitDistance = poiToEye.length();
		orbitAzimuth = (float) Math.atan2(poiToEye.getY(), poiToEye.getX());
		orbitElevation = (float) Math.asin(poiToEye.getZ() / orbitDistance);
	}

	// ****************************************************************************************************************
	// ***** 4. The contexts
	// ****************************************************************************************************************

	/**
	 * The PerspectiveContext: how the scene is projected on the screen. Here a perspective projection (FRUSTUM) through
	 * a view plane of 0.01 x 0.005625 units at a distance of 0.01 from the eye, up to a depth of 100 units, with 100000
	 * pixels per unit: a 1000 x 562 pixels image.
	 *
	 * The near plane is very close to the eye, less than the FLY_CLEARANCE of the fly camera above the ground, so that
	 * the fly camera can go near the hills and the sea without the near plane cutting them.
	 *
	 * When the window is resized, the PerspectiveContext keeps the number of pixels per unit rather than the field of
	 * view (KEEP_SCALE): a larger window shows more of the landscape rather than a larger image.
	 */
	static PerspectiveContext createPerspectiveContext() {
		PerspectiveContext pContext = new PerspectiveContext(0.01f, 0.005625f, 0.01f, 100, PerspectiveType.FRUSTUM, 100000);
		pContext.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);
		return pContext;
	}

	/**
	 * The RenderContext: the rendering options at start-up, which the Rendering and Effects menus then change. Smooth
	 * shading (INTERPOLATE), no texture, the sun visible in the sky (light glow).
	 */
	static RenderContext createRenderContext() {
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(false);
		rContext.setLightGlow(true);
		return rContext;
	}

	// ****************************************************************************************************************
	// ***** 6. The view, its window and its controls (step 5, the RenderEngine, is a single line of run())
	// ****************************************************************************************************************

	/**
	 * Creates the window (not shown yet) with its menus, the SwingView where the RenderEngine draws and the Swing panel
	 * that displays it, and installs the mouse and keyboard controls. The menus start from the state of the model, so
	 * the Lighting and the RenderContext must be created before.
	 *
	 * @param context the PerspectiveContext, which gives the initial size of the image
	 * @return the view, to be given to the RenderEngine
	 */
	public GUIView createView(PerspectiveContext context) {

		frame = new JFrame("Fractal Landscape");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Menus
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createLandscapeMenu());
		menuBar.add(createRenderingMenu());
		menuBar.add(createEffectsMenu());
		frame.setJMenuBar(menuBar);
		updateEffectsMenu();

		// The view: an image the RenderEngine draws into. Given the frame, it repaints it after each rendering.
		SwingView view = new SwingView(context, frame);

		// The panel displays the image of the view
		panel = new JPanel() {
			@Override
			public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics); // Clears with the background color, visible until a resized image is rendered
				graphics.drawImage(view.getImageView(), 0, 0, null);
			}
		};
		panel.setBackground(Color.BLACK);
		// The panel has the size of the image: pack() sizes the frame around it (title bar, borders and menu bar)
		panel.setPreferredSize(new Dimension(context.getPixelWidth(), context.getPixelHeight()));
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setLocationRelativeTo(null); // Center of the screen

		// User controls
		installMouseControls();
		installKeyboardControls();
		installResizeControl();

		return view;
	}

	/**
	 * Mouse controls on the panel: drag to turn the camera, wheel to move it. A MouseAdapter implements all the mouse
	 * listener interfaces with empty methods, so that only the useful ones are written.
	 */
	private void installMouseControls() {
		MouseAdapter mouseControls = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// Start of a drag: mouseDragged() uses the displacement from one event to the next
				lastMouseX = e.getX();
				lastMouseY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int dx = e.getX() - lastMouseX;
				int dy = e.getY() - lastMouseY;
				lastMouseX = e.getX();
				lastMouseY = e.getY();
				turnCamera(dragAngle(dx, panel.getWidth(), DRAG_ANGLE_PER_WIDTH), dragAngle(dy, panel.getHeight(), DRAG_ANGLE_PER_HEIGHT));
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				// Wheel up (negative rotation) moves forward. getPreciseWheelRotation() rather than getWheelRotation():
				// a touchpad sends fractions of a notch, which getWheelRotation() would round to 0 (no movement)
				dollyCamera((float) -e.getPreciseWheelRotation());
			}
		};
		panel.addMouseListener(mouseControls);
		panel.addMouseMotionListener(mouseControls);
		panel.addMouseWheelListener(mouseControls);
	}

	/**
	 * Keyboard shortcuts without a menu item: Up / Down arrows move the camera like the mouse wheel, Page Up / Page Down
	 * raise / lower the sea level like CTRL + U / CTRL + D (the accelerators of the Landscape menu).
	 */
	private void installKeyboardControls() {
		JComponent root = frame.getRootPane();
		bindKey(root, KeyEvent.VK_UP, "cameraForward", () -> dollyCamera(1));
		bindKey(root, KeyEvent.VK_DOWN, "cameraBackward", () -> dollyCamera(-1));
		bindKey(root, KeyEvent.VK_PAGE_UP, "seaUp", () -> changeSeaLevel(1));
		bindKey(root, KeyEvent.VK_PAGE_DOWN, "seaDown", () -> changeSeaLevel(-1));
	}

	/**
	 * Binds a key to an action for the whole window (WHEN_IN_FOCUSED_WINDOW): it works whatever component has the
	 * keyboard focus, unlike a KeyListener, which only receives the keys when its own component has it.
	 */
	private static void bindKey(JComponent component, int keyCode, String name, Runnable action) {
		component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
		component.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	/**
	 * Follows the size of the panel (hence of the window). Each intermediate size restarts the delay of the timer, so
	 * that the view is resized and rendered only once the user stops resizing.
	 */
	private void installResizeControl() {
		resizeTimer = new Timer(RESIZE_DELAY_MS, e -> resizeView());
		resizeTimer.setRepeats(false);
		panel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizeTimer.restart();
			}
		});
	}

	/**
	 * Resizes the view to the size of the panel and renders it: the RenderEngine adapts the perspective to the new size
	 * of the view (KEEP_SCALE, see createPerspectiveContext()). Nothing to do for an empty panel (minimized window) or
	 * if the size did not change (the first event, when the window is displayed at the initial size of the image).
	 */
	private void resizeView() {
		int width = panel.getWidth();
		int height = panel.getHeight();
		if (width < 1 || height < 1) return;
		if (width == guiView.getViewWidth() && height == guiView.getViewHeight()) return;
		guiView.resize(width, height);
		renderer.render();
	}

	/** Displays the camera mode and the sea level in the title of the window. */
	private void updateTitle() {
		frame.setTitle(String.format("Fractal Landscape - %s camera - Sea level: %.2f", flyMode ? "Fly" : "Orbit", seaLevel));
	}

	// ****************************************************************************************************************
	// ***** Menus
	// ****************************************************************************************************************

	/** Landscape menu: actions on the scene and choice of the camera mode. */
	private JMenu createLandscapeMenu() {
		JMenu menu = new JMenu("Landscape");
		menu.setMnemonic(KeyEvent.VK_L);

		addMenuItem(menu, "Generate", null, this::generateLandscape);

		// Accelerators CTRL + U / CTRL + D: letters are at the same place on every keyboard layout, unlike + and -
		// (Page Up / Page Down also work, see installKeyboardControls())
		addMenuItem(menu, "Sea level up", KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK), () -> changeSeaLevel(1));
		addMenuItem(menu, "Sea level down", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), () -> changeSeaLevel(-1));
		menu.addSeparator();

		addMenuItem(menu, "Reset camera", null, this::resetCamera);
		menu.addSeparator();

		// Camera mode: an exclusive choice (radio buttons in a ButtonGroup)
		ButtonGroup cameraGroup = new ButtonGroup();
		addRadioItem(menu, cameraGroup, "Orbit camera", !flyMode, () -> setFlyMode(false));
		addRadioItem(menu, cameraGroup, "Fly camera", flyMode, () -> setFlyMode(true));
		return menu;
	}

	/** Rendering menu: the rendering type of the RenderContext, an exclusive choice. */
	private JMenu createRenderingMenu() {
		JMenu menu = new JMenu("Rendering");
		menu.setMnemonic(KeyEvent.VK_R);
		ButtonGroup group = new ButtonGroup();
		addRenderingItem(menu, group, "Lines", RenderingType.LINE);
		addRenderingItem(menu, group, "Monochrome", RenderingType.MONOCHROME);
		addRenderingItem(menu, group, "Unlit", RenderingType.UNLIT);
		addRenderingItem(menu, group, "Flat", RenderingType.FLAT);
		addRenderingItem(menu, group, "Shading", RenderingType.INTERPOLATE);
		return menu;
	}

	private void addRenderingItem(JMenu menu, ButtonGroup group, String label, RenderingType type) {
		addRadioItem(menu, group, label, rContext.getRenderingType() == type, () -> {
			rContext.setRenderingType(type);
			updateEffectsMenu();
			renderer.render();
		});
	}

	/**
	 * Effects menu: on / off options, independent of the rendering type, in three groups (surfaces, shadows, sun). Each
	 * check box starts from the state of the model (RenderContext, Lighting, sun) and changes it.
	 */
	private JMenu createEffectsMenu() {
		JMenu menu = new JMenu("Effects");
		menu.setMnemonic(KeyEvent.VK_E);

		// Surfaces
		textureItem = addEffectItem(menu, "Texture", rContext.isTextureProcessing(), on -> rContext.setTextureProcessing(on));
		specularItem = addEffectItem(menu, "Specular highlights", lighting.hasSpecular(), on -> lighting.setSpecularLight(on));
		menu.addSeparator();

		// Shadows, hard or soft (PCF_3X3 filtering of the shadow map of the sun)
		shadowsItem = addEffectItem(menu, "Shadows", rContext.isShadowing(), on -> rContext.setShadowing(on));
		softShadowsItem = addEffectItem(menu, "Soft shadows", sun.getShadowFilter() == ShadowFilter.PCF_3X3,
				on -> sun.setShadowFilter(on ? ShadowFilter.PCF_3X3 : ShadowFilter.HARD));
		menu.addSeparator();

		// Sun: the "light glow" of a directional light is the sun disc in the sky, the LightAppearance its halo and lens flare
		sunItem = addEffectItem(menu, "Sun", rContext.isLightGlow(), on -> rContext.setLightGlow(on));
		sunHaloItem = addEffectItem(menu, "Sun halo", sunAppearance.getGain() > 0, on -> sunAppearance.setGain(on ? 1f : 0f));
		lensFlareItem = addEffectItem(menu, "Lens flare", sunAppearance.getFlareGain() > 0, on -> sunAppearance.setFlareGain(on ? 1f : 0f));
		lightShaftsItem = addEffectItem(menu, "Light shafts", Boolean.TRUE.equals(rContext.getLightShafts()), on -> rContext.setLightShafts(on));
		return menu;
	}

	/** Creates a check box of the Effects menu: applies the change to the model, updates the menu and renders. */
	private JCheckBoxMenuItem addEffectItem(JMenu menu, String label, boolean selected, Consumer<Boolean> apply) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, selected);
		item.addActionListener(e -> {
			apply.accept(item.isSelected());
			updateEffectsMenu();
			renderer.render();
		});
		menu.add(item);
		return item;
	}

	/**
	 * Grays out the effects that the current rendering type does not use: the texture is used by the filled rendering
	 * types (UNLIT, FLAT, INTERPOLATE), the specular highlights and the shadows by the lit ones (FLAT, INTERPOLATE),
	 * the sun by all but LINE. The sub-options (soft shadows, halo, lens flare, light shafts) also need their main
	 * option.
	 */
	private void updateEffectsMenu() {
		RenderingType type = rContext.getRenderingType();
		boolean lit = type == RenderingType.FLAT || type == RenderingType.INTERPOLATE;
		textureItem.setEnabled(lit || type == RenderingType.UNLIT);
		specularItem.setEnabled(lit);
		shadowsItem.setEnabled(lit);
		softShadowsItem.setEnabled(lit && shadowsItem.isSelected());
		sunItem.setEnabled(type != RenderingType.LINE);
		boolean sunVisible = sunItem.isEnabled() && sunItem.isSelected();
		sunHaloItem.setEnabled(sunVisible);
		lensFlareItem.setEnabled(sunVisible);
		lightShaftsItem.setEnabled(sunVisible);
	}

	private static void addMenuItem(JMenu menu, String label, KeyStroke accelerator, Runnable action) {
		JMenuItem item = new JMenuItem(label);
		if (accelerator != null) item.setAccelerator(accelerator);
		item.addActionListener(e -> action.run());
		menu.add(item);
	}

	private static void addRadioItem(JMenu menu, ButtonGroup group, String label, boolean selected, Runnable action) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
		item.addActionListener(e -> action.run());
		group.add(item);
		menu.add(item);
	}

	// ****************************************************************************************************************
	// ***** Actions on the World
	// ****************************************************************************************************************

	/**
	 * Generates a new random landscape and renders it. The geometry of the existing Trellis is updated with the new
	 * altitudes, then the World is rebuilt (new normals) and the triangles colored again.
	 */
	private void generateLandscape() {
		generateFractalAltitudes(landAltitudes, LANDSCAPE_SIZE, LANDSCAPE_DIVISIONS, new Random());
		try {
			landTrellis.updateTrellis(landAltitudes);
		} catch (WrongArraySizeException e) {
			// Cannot happen: the array has the size of the Trellis
			throw new IllegalStateException(e);
		}
		world.rebuild();
		printLandscapeStatistics();
		colorTrianglesByAltitude(landTrellis);

		if (flyMode) { // The new ground may be above the flying camera
			keepAboveGround();
			updateCamera();
		}
		renderer.render();
	}

	/**
	 * Raises (positive steps) or lowers (negative steps) the sea level, within the altitudes of the landscape, and
	 * renders. This is a transformation of the World, not of the view: the sea Element is given a new Translation; its
	 * geometry is unchanged, so neither a rebuild of the World nor a new calculation of the normals is needed.
	 */
	private void changeSeaLevel(int steps) {
		seaLevel = clamp(seaLevel + steps * SEA_LEVEL_STEP, landTrellis.getMinZ(), landTrellis.getMaxZ());
		seaTrellis.setTransformation(new Translation(new Vector3(0, 0, seaLevel)));
		if (flyMode) { // A rising sea lifts the flying camera
			keepAboveGround();
			updateCamera();
		}
		updateTitle();
		renderer.render();
	}

	// ****************************************************************************************************************
	// ***** Camera control
	// ****************************************************************************************************************

	/**
	 * Turns the camera after a mouse drag, by the given angles (in radians) and renders. The vertical angle is clamped
	 * at each event (not on a total), so that dragging back moves the camera again at once, without a dead zone.
	 *
	 * @param horizontalAngle angle for the horizontal displacement of the mouse (positive to the right)
	 * @param verticalAngle   angle for the vertical displacement of the mouse (positive downward: the screen Y axis
	 *                        points down)
	 */
	private void turnCamera(float horizontalAngle, float verticalAngle) {
		if (flyMode) {
			// "Look around", like the head of the pilot: dragging to the right turns the line of sight to the right
			// (the yaw decreases, clockwise seen from above), dragging up looks up
			flyYaw -= horizontalAngle;
			flyPitch = clamp(flyPitch - verticalAngle, -MAX_ELEVATION, MAX_ELEVATION);
		} else {
			// "Grab the World": dragging to the right turns the landscape to the right, hence the camera the other way
			// (the azimuth decreases); dragging down shows the landscape more from above (the elevation increases)
			orbitAzimuth -= horizontalAngle;
			orbitElevation = clamp(orbitElevation + verticalAngle, -MAX_ELEVATION, MAX_ELEVATION);
		}
		updateCamera();
		renderer.render();
	}

	/**
	 * Moves the camera forward (positive notches) or backward (negative notches) along its line of sight: one notch of
	 * the wheel, or one press of an arrow key, is 1. The movement is not immediate, it eases over a short animation.
	 * <ul>
	 * <li>Orbit camera: zoom, without letting the camera reach the point of interest</li>
	 * <li>Fly camera: flight along the line of sight, without limit (but above the ground)</li>
	 * </ul>
	 */
	private void dollyCamera(float notches) {
		if (flyMode) {
			dollyTarget += notches;
		} else {
			float maxDolly = (orbitDistance - MIN_ORBIT_DISTANCE) / DOLLY_STEP;
			dollyTarget = Math.min(dollyTarget + notches, maxDolly);
		}
		startDollyAnimation();
	}

	/**
	 * Starts the animation of the camera from where it is (dollyApplied) toward dollyTarget. If an animation is already
	 * running (several wheel notches in quick succession), it restarts from the current, already interpolated, position,
	 * so that the camera never jumps backward.
	 */
	private void startDollyAnimation() {
		dollyAnimationStart = dollyApplied;
		dollyAnimationStartMillis = System.currentTimeMillis();
		if (dollyTimer == null) {
			dollyTimer = new Timer(DOLLY_ANIMATION_FRAME_MS, e -> stepDollyAnimation());
		}
		if (!dollyTimer.isRunning()) {
			dollyTimer.start();
		}
	}

	/**
	 * One frame of the animation: moves the camera a step further toward dollyTarget (a linear interpolation over
	 * the duration of the animation) and renders. The Swing Timer calls it on the Swing thread, the same as the mouse
	 * events, so no synchronization is needed.
	 */
	private void stepDollyAnimation() {
		long elapsed = System.currentTimeMillis() - dollyAnimationStartMillis;
		float t = Math.min(1f, (float) elapsed / DOLLY_ANIMATION_DURATION_MS);
		applyDolly(interpolate(dollyAnimationStart, dollyTarget, t));
		renderer.render();
		if (t >= 1f) {
			dollyTimer.stop();
		}
	}

	/** Stops a running animation at its target, so that a change of camera mode starts from a stable state. */
	private void finishDollyAnimation() {
		if (dollyTimer != null && dollyTimer.isRunning()) {
			dollyTimer.stop();
			applyDolly(dollyTarget);
		}
	}

	/**
	 * Moves the camera to the given dolly position.
	 * <ul>
	 * <li>Orbit: the distance to the point of interest is calculated from it (see updateCamera())</li>
	 * <li>Fly: the eye moves along the CURRENT line of sight by the distance since the previous position only, so that
	 *     the flight follows the direction if it changes (mouse drag) during the animation</li>
	 * </ul>
	 */
	private void applyDolly(float dolly) {
		if (flyMode) {
			flyEye = flyEye.plus(flyDirection(flyYaw, flyPitch).times((dolly - dollyApplied) * DOLLY_STEP));
			keepAboveGround();
		}
		dollyApplied = dolly;
		updateCamera();
	}

	/**
	 * Places the Camera according to the current mode, always with Z as up vector:
	 * <ul>
	 * <li>Orbit: on its orbit (angles and zoom), looking at the point of interest</li>
	 * <li>Fly: at the position of the fly camera, looking along its line of sight</li>
	 * </ul>
	 * The World does not move: only the Camera does.
	 */
	private void updateCamera() {
		if (flyMode) {
			camera.updateCamera(flyEye, flyEye.plus(flyDirection(flyYaw, flyPitch)), Vector4.zAxis());
		} else {
			float distance = orbitDistance - dollyApplied * DOLLY_STEP;
			camera.updateCamera(orbitEye(POINT_OF_INTEREST, distance, orbitAzimuth, orbitElevation), POINT_OF_INTEREST, Vector4.zAxis());
		}
	}

	/**
	 * Switches between the orbit and the fly camera, and renders.
	 * <ul>
	 * <li>To fly: the flight starts where the orbit camera is, looking in the same direction (so the view does not
	 *     jump); the zoom of the orbit is set aside.</li>
	 * <li>Back to orbit: the camera goes back on its orbit, where it was (same angles and zoom as before the
	 *     flight).</li>
	 * </ul>
	 */
	private void setFlyMode(boolean fly) {
		if (fly == flyMode) return;
		finishDollyAnimation();
		if (fly) {
			orbitDolly = dollyApplied;
			Vector4 eye = camera.getEye();
			Vector4 direction = POINT_OF_INTEREST.minus(eye).normalize();
			flyStartEye = new Vector4(eye);
			flyStartYaw = (float) Math.atan2(direction.getY(), direction.getX());
			flyStartPitch = clamp((float) Math.asin(direction.getZ()), -MAX_ELEVATION, MAX_ELEVATION);
			startFlight();
			keepAboveGround();
		} else {
			dollyTarget = orbitDolly;
			dollyApplied = orbitDolly;
		}
		flyMode = fly;
		updateCamera();
		updateTitle();
		renderer.render();
	}

	/** Puts the fly camera at the start of the flight. */
	private void startFlight() {
		flyEye = new Vector4(flyStartEye);
		flyYaw = flyStartYaw;
		flyPitch = flyStartPitch;
		dollyTarget = 0;
		dollyApplied = 0;
	}

	/**
	 * Puts the camera back at its initial position, and renders: the initial orbit without zoom for the orbit camera,
	 * the start of the flight for the fly camera.
	 */
	private void resetCamera() {
		if (dollyTimer != null) dollyTimer.stop();
		if (flyMode) {
			startFlight();
			keepAboveGround();
		} else {
			dollyTarget = 0;
			dollyApplied = 0;
			initOrbit();
		}
		updateCamera();
		renderer.render();
	}

	/**
	 * Keeps the fly camera at least FLY_CLEARANCE above the ground and the sea: the camera slides on them instead of
	 * going under. The ground is measured under the camera and around it (at FLY_CLEARANCE on each side): on a steep
	 * slope, the ground rising beside the camera could otherwise come closer than the near plane, which would cut it.
	 * Outside of the landscape there is nothing to stay above.
	 */
	private void keepAboveGround() {
		float x = flyEye.getX(), y = flyEye.getY(), r = FLY_CLEARANCE;
		float[][] samples = { { x, y }, { x - r, y }, { x + r, y }, { x, y - r }, { x, y + r } };
		float ground = Float.NaN;
		for (float[] sample : samples) {
			float altitude = groundAltitude(landAltitudes, LANDSCAPE_SIZE, LANDSCAPE_DIVISIONS, sample[0], sample[1]);
			if (Float.isNaN(altitude)) continue; // Outside of the landscape
			if (Float.isNaN(ground) || altitude > ground) ground = altitude; // Keep the highest
		}
		if (Float.isNaN(ground)) return;
		float minAltitude = Math.max(ground, seaLevel) + FLY_CLEARANCE;
		if (flyEye.getZ() < minAltitude) flyEye.setZ(minAltitude);
	}

	// ****************************************************************************************************************
	// ***** Geometry helpers
	// ***** Pure functions, static and package-private so that they can be unit-tested without a window
	// ***** (see TestFractalLandscape)
	// ****************************************************************************************************************

	/**
	 * Rotation angle (radians) for a mouse displacement (pixels): proportional to the displacement, anglePerSize
	 * radians for a displacement of the whole window size. Using the displacement since the previous event (not since
	 * the click) makes the total angle depend on the mouse movement only, whatever the number of events received.
	 */
	static float dragAngle(int displacement, int windowSize, float anglePerSize) {
		return anglePerSize * displacement / windowSize;
	}

	/**
	 * Position of the eye of an orbit camera: the point at the given distance from the point of interest, in the
	 * direction given by the spherical angles azimuth (around the vertical Z axis, from the X axis) and elevation
	 * (above the horizontal plane).
	 */
	static Vector4 orbitEye(Vector4 poi, float distance, float azimuth, float elevation) {
		float horizontal = distance * (float) Math.cos(elevation); // Projection of the distance on the XY plane
		return new Vector4(
				poi.getX() + horizontal * (float) Math.cos(azimuth),
				poi.getY() + horizontal * (float) Math.sin(azimuth),
				poi.getZ() + distance * (float) Math.sin(elevation),
				1);
	}

	/**
	 * Unit vector of the line of sight of the fly camera, for the given yaw (heading, around the vertical Z axis, from
	 * the X axis) and pitch (above the horizontal plane): the same spherical coordinates as orbitEye(), here for a
	 * direction (w = 0) rather than a point (w = 1).
	 */
	static Vector4 flyDirection(float yaw, float pitch) {
		float horizontal = (float) Math.cos(pitch);
		return new Vector4(
				horizontal * (float) Math.cos(yaw),
				horizontal * (float) Math.sin(yaw),
				(float) Math.sin(pitch),
				0);
	}

	/**
	 * Altitude of the ground at (x, y), never below the surface actually drawn, for a Trellis of the given size,
	 * centered on the origin, made of n x n cells, with altitudes[i][j] at x = -size/2 + i*size/n, y = -size/2 +
	 * j*size/n. Each cell is drawn as 2 triangles, cut along one diagonal or the other: the altitude returned is the
	 * higher of the two possible cuts (each one a planar interpolation in the triangle containing the point), so that a
	 * camera kept above it is above the drawn triangles whatever the cut.
	 *
	 * @return the altitude, NaN outside of the Trellis
	 */
	static float groundAltitude(float[][] altitudes, float size, int n, float x, float y) {
		// Position in cells from the corner of the Trellis
		float u = (x + size / 2) / size * n;
		float v = (y + size / 2) / size * n;
		if (u < 0 || v < 0 || u > n || v > n) return Float.NaN;
		int i = Math.min((int) u, n - 1); // The last cell for the far edge (u = n)
		int j = Math.min((int) v, n - 1);
		float fu = u - i, fv = v - j; // Position inside the cell, in [0, 1]
		float a = altitudes[i][j], b = altitudes[i + 1][j], c = altitudes[i][j + 1], d = altitudes[i + 1][j + 1];
		// Cut along the diagonal a-d: triangle a-b-d below the diagonal (fu >= fv), a-c-d above
		float cutAD = fu >= fv ? a + (b - a) * fu + (d - b) * fv : a + (c - a) * fv + (d - c) * fu;
		// Cut along the diagonal b-c: triangle a-b-c near the corner a (fu + fv <= 1), d-c-b near the corner d
		float cutBC = fu + fv <= 1 ? a + (b - a) * fu + (c - a) * fv : d + (c - d) * (1 - fu) + (b - d) * (1 - fv);
		return Math.max(cutAD, cutBC);
	}

	/** Linear interpolation: the value a fraction t (in [0, 1]) of the way from start to target. */
	static float interpolate(float start, float target, float t) {
		return start + (target - start) * t;
	}

	/** Restricts a value to the [min, max] interval. */
	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
