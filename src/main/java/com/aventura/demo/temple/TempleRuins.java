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
package com.aventura.demo.temple;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.aventura.demo.FrameTimeStatistics;
import com.aventura.demo.StatisticsWindow;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Transformation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Fog;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.atmosphere.SphericalFog;
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
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Demo application of the Aventura rendering engine: the ruins of a Greek temple on a small island, among olive
 * trees and boulders, with the sea up to the horizon and the sun halfway up the sky. The user explores the scene with
 * the mouse, or lets the camera fly over it like a helicopter.
 *
 * <h2>What this demo shows of the Aventura API</h2>
 *
 * The {@link #createScene()} method follows the typical steps of an Aventura application, one method per step:
 * <ol>
 * <li>{@link #createWorld()}: the {@link World} and its tree of {@link Element}s.
 *     <ul>
 *     <li>The ground is a height field ({@link IslandTerrain}) drawn by a {@link TiledTrellis}, a Trellis extended
 *         to repeat its texture; its triangles are colored after the nature of the ground
 *         ({@link #colorGround}).</li>
 *     <li>The temple ({@link Temple}) is a tree of Elements three levels deep, made of built-in shapes (Box) and of
 *         a new shape written for the demo ({@link TaperedDrum}), assembled into {@link Column}s.</li>
 *     <li>The olive trees ({@link OliveTree}) and the boulders ({@link Boulder}, another new shape) are placed on
 *         the ground with the three kinds of transformation: Scaling, Rotation and Translation.</li>
 *     <li>The textures ({@link SceneTextures}) come from image files, or are computed.</li>
 *     <li>The {@link Atmosphere} of the World: the {@link Haze} that makes the sea fade into the sky at the horizon,
 *         and optional banks of mist ({@link SphericalFog}).</li>
 *     </ul></li>
 * <li>{@link #createSun()} and {@link #createLighting(DirectionalLight)}: a {@link DirectionalLight} (the sun: warm
 *     light, soft shadows limited to the island, a visible disc with a halo and a lens flare) and a bluish
 *     {@link AmbientLight} (the light of the sky, that fills the shadows), gathered in a {@link Lighting}.</li>
 * <li>{@link #createCamera()}: the {@link Camera} (eye, point of interest and up vector, Z being up).</li>
 * <li>{@link #createPerspectiveContext()} and {@link #createRenderContext()}: how the 3D scene is projected on the
 *     screen, and how it is rendered (rendering type, textures, shadows, atmosphere, sun...).</li>
 * <li>{@link RenderEngine}: the engine gathering all of the above.</li>
 * </ol>
 * Then {@link #createView(PerspectiveContext)} opens a {@link SwingView} (a {@link GUIView}) in a Swing window, with
 * its menus and the mouse and keyboard controls. From there on, at each user action or step of the flight, the
 * application only changes the model (moves the {@link Camera}, turns the sun, switches an option of the
 * {@link RenderContext}...) and calls {@link RenderEngine#render()} again.
 *
 * <h2>Cameras</h2>
 *
 * Three camera modes (Camera menu):
 * <ul>
 * <li><b>Orbit camera</b> (default, key O), to look at the temple from outside:
 *     <ul>
 *     <li>Mouse drag: orbit around the temple (a horizontal drag turns around the vertical Z axis, a vertical drag
 *         changes the elevation)</li>
 *     <li>Mouse wheel (or Up / Down arrow keys): zoom</li>
 *     </ul></li>
 * <li><b>Fly camera</b> (key F), to move freely in the scene, starting from where the camera is:
 *     <ul>
 *     <li>Mouse drag: look around</li>
 *     <li>Mouse wheel (or Up / Down arrow keys): fly forward / backward along the line of sight, always above the
 *         ground and the sea</li>
 *     </ul></li>
 * <li><b>Helicopter tour</b> (key H): the camera follows a {@link FlightPath}, a smooth loop around and through the
 *     ruins, at a constant speed whatever the speed of the computer. Space pauses and resumes the flight. Dragging
 *     the mouse or turning the wheel takes the controls: the tour stops and the fly camera starts from there.</li>
 * </ul>
 *
 * <h2>Menus</h2>
 * <ul>
 * <li>Camera: the camera mode, pause of the tour, reset of the camera, and the statistics (key I): the composition of
 *     the scene (Elements, triangles and vertices of each branch of its tree), its textures and their memory, and what
 *     the engine did for the last image. They are also printed in the console when the window is closed. This is the
 *     {@link StatisticsWindow} shared by the demos, on top of the GeometryStatistics and GeometryReport of the API.
 *     At the end of each lap of the helicopter tour, the average, fastest and slowest rendering times of the lap are
 *     printed in the console, shown in the title of the window and added to the statistics
 *     ({@link FrameTimeStatistics}): the figures to compare two rendering options.</li>
 * <li>Sun: turn the sun around the scene and move it up and down (the shadows follow)</li>
 * <li>Rendering: the rendering type (an exclusive choice)</li>
 * <li>Effects: options independent of the rendering type (textures, mipmapping, specular highlights, shadows, haze,
 *     sea mist, sun, sun halo, lens flare, light shafts), grayed out when the current rendering type does not use
 *     them</li>
 * </ul>
 *
 * <h2>Conventions</h2>
 * Z is the up axis and the unit is the meter. The island is centered on the origin; the long side of the temple is
 * along X, its main front faces -X. Angles are in radians, except the position of the sun and the settings of the
 * flight, in degrees for readability.
 *
 * @since Nov 2026
 */
public class TempleRuins {

	// ****************************************************************************************************************
	// ***** Constants
	// ****************************************************************************************************************

	// ----- Island and sea -----

	/** Side of the square of ground that carries the island, and number of cells of its Trellis on each side. */
	static final float GROUND_SIZE = 240f;
	static final int GROUND_DIVISIONS = 96;
	/** The texture of the ground is repeated every GROUND_CELLS_PER_TILE cells (10 m). */
	static final int GROUND_CELLS_PER_TILE = 4;
	/** Seed of the relief of the island (see IslandTerrain). */
	static final int ISLAND_SEED = 7;

	/**
	 * The sea is made of two flat Trellis at the altitude 0. Around the island, a textured one, whose cells are the
	 * tiles of the texture. Under it and up to the horizon, a much larger one with very large cells, of a single
	 * color: so far away, the waves could not be seen anyway.
	 */
	static final float NEAR_SEA_SIZE = 800f;
	static final int NEAR_SEA_DIVISIONS = 32;
	static final float FAR_SEA_SIZE = 9000f;
	static final int FAR_SEA_DIVISIONS = 18;
	/** The far sea is this much lower than the near one, so that their surfaces are never mixed up by the depth test. */
	static final float FAR_SEA_DEPTH = 0.5f;

	// ----- Temple, trees, boulders -----

	/** Seeds of the random variations: change one to get other trees, boulders... with the same code. */
	static final long TEMPLE_SEED = 3;
	static final long DEBRIS_SEED = 5;
	static final long TREES_SEED = 12;
	static final long BOULDERS_SEED = 4;

	/** The platform of the temple goes this deep into the ground of the plateau. */
	static final float TEMPLE_SUNK = 0.06f;

	static final int NB_TREES = 18;
	/** A tree grows on land above this altitude, on a slope below this one, farther than this from the temple and the other trees. */
	static final float TREE_MIN_ALTITUDE = 2.5f;
	static final float TREE_MAX_SLOPE = 0.5f;
	static final float TREE_MIN_DISTANCE_TO_TEMPLE = 7f;
	static final float TREE_MIN_SPACING = 9f;
	/** Trees are looked for within this distance of the center of the island. */
	static final float TREE_AREA_RADIUS = 62f;

	/** Size of the trees (1 is a tree of about 4 m). */
	static final float TREE_MIN_SIZE = 1.0f, TREE_MAX_SIZE = 1.7f;

	static final int NB_BOULDERS = 22;
	static final float BOULDER_MIN_RADIUS = 0.5f, BOULDER_MAX_RADIUS = 2.6f;

	// ----- Colors and materials -----

	static final Color SKY_COLOR = new Color(132, 176, 232);
	/** Color of the sea, multiplied with its texture (which darkens it in the troughs of the waves). */
	static final Color SEA_TINT = new Color(110, 175, 220);
	/** Specular highlights: the glitter of the sun on the water, a faint sheen on the marble. */
	static final Color SEA_SPECULAR_COLOR = new Color(190, 180, 150);
	static final float SEA_SPECULAR_EXP = 25;

	/** Tints of the ground, multiplied with its texture: dry earth, sand of the shore, dry grass, bare rock. */
	static final Color EARTH_TINT = new Color(255, 236, 200);
	static final Color SAND_TINT = new Color(255, 246, 214);
	static final Color GRASS_TINT = new Color(214, 226, 138);
	static final Color ROCK_TINT = new Color(235, 232, 230);
	static final Color BOULDER_TINT = new Color(226, 220, 208);

	// ----- Atmosphere -----

	/**
	 * The haze: the air is clear over the island and the sea fades into the sky towards the horizon (the color of the
	 * haze is the background color of the World). Visibility in meters: the distance where the contrast of an object
	 * with the sky falls to 5%.
	 */
	static final float HAZE_VISIBILITY = 1100f;

	/** Sea mist (Effects menu): a few wide banks of fog lying on the water around the island. */
	static final int NB_MIST_BANKS = 4;
	static final float MIST_DISTANCE = 190f;
	static final float MIST_MIN_RADIUS = 70f, MIST_MAX_RADIUS = 120f;
	static final Color MIST_COLOR = new Color(226, 232, 240);
	static final long MIST_SEED = 2;

	// ----- Lighting -----

	/**
	 * Position of the sun, as seen from the scene, in degrees: azimuth (around the vertical Z axis, from the X axis)
	 * and elevation (above the horizon). The sun is over the sea, in front of the main front of the temple and to its
	 * left, halfway up the sky.
	 */
	static final float SUN_AZIMUTH = 212f;
	static final float SUN_ELEVATION = 32f;
	static final float SUN_INTENSITY = 0.82f;
	static final Color SUN_COLOR = new Color(255, 246, 228);
	/** The Sun menu moves the sun by this angle, and keeps it between these elevations. */
	static final float SUN_STEP = 5f;
	static final float SUN_MIN_ELEVATION = 5f, SUN_MAX_ELEVATION = 85f;
	/**
	 * Brightness of the light shafts of the sun when they are switched on (Effects menu): well below the normal value
	 * of 1, since most of the image is an open sky, that the shafts would wash out.
	 */
	static final float LIGHT_SHAFTS_GAIN = 0.3f;
	/** Longest side of the shadow map of the sun, in pixels. It covers the island only (see createSun()). */
	static final int SHADOW_MAP_SIZE = 2048;

	/** Ambient light: the light of the sky, bluish, so that the shadows are not black. */
	static final float AMBIENT_INTENSITY = 0.46f;
	static final Color AMBIENT_COLOR = new Color(205, 222, 255);

	// ----- Projection -----

	/** Distances of the near and far planes of the view frustum: nothing is drawn closer or farther. */
	static final float NEAR_DISTANCE = 0.1f;
	static final float FAR_DISTANCE = 5000f;
	/** Width of the image at start-up, in pixels (its height is 9/16 of it). */
	static final int IMAGE_WIDTH = 1000;

	// ----- Orbit and fly cameras -----

	/** Point the orbit camera looks at and turns around (the middle of the temple), and its initial position. */
	public static final Vector4 POINT_OF_INTEREST = new Vector4(0, 0, IslandTerrain.PLATEAU_ALTITUDE + 3.5f, 1);
	public static final Vector4 INITIAL_EYE = new Vector4(-43, -12, IslandTerrain.PLATEAU_ALTITUDE + 5f, 1);

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
	/** The orbit camera stays between these distances of the point of interest. */
	static final float MIN_ORBIT_DISTANCE = 6f;
	static final float MAX_ORBIT_DISTANCE = 600f;

	/** Distance travelled by the camera per notch of the mouse wheel (zoom of the orbit camera, or flight), in meters. */
	static final float DOLLY_STEP = 2f;
	/**
	 * Minimum height of the camera above the ground and the sea: the height of the eyes of a visitor. It must stay
	 * larger than the distance of the near plane, which would otherwise cut the ground in front of the camera.
	 */
	static final float GROUND_CLEARANCE = 1.7f;

	// ----- Helicopter tour -----

	/**
	 * The waypoints of the tour (see FlightPath): position of the camera (x, y, z), then point it looks at (x, y, z).
	 * The camera comes from the open sea, flies down along the north colonnade, turns in front of the main front,
	 * low over the slope, then climbs around the south of the island to see it whole, and starts again.
	 */
	static final float[][] TOUR_WAYPOINTS = {
			{ 150, 95, 46,      0, 0, 17 },    // From the open sea, the whole island
			{ 62, 34, 27,       0, 2, 19 },    // Down towards the back of the temple
			{ 25, 18, 18.4f,    2, 4, 27 },    // Low behind the north colonnade, looking up: the sun above the columns
			{ -10, 18, 19,      -11, 0, 21 },  // Along the north colonnade
			{ -34, 6, 18.5f,    -11, 0, 21 },  // In front of the main front, lit by the sun
			{ -30, -24, 21,     -2, -2, 20 },  // Turning around the south-west corner
			{ 4, -44, 29,       2, -2, 18 },   // The south side and its fallen columns, climbing
			{ 58, -46, 44,      0, 0, 15 },    // Higher: the temple on its plateau
			{ 118, 2, 58,       0, 0, 12 },    // From high above the sea, the island and the horizon
	};
	/** Speed of the flight, in meters per second. */
	static final float TOUR_SPEED = 9f;
	/**
	 * The camera moves forward by the speed times the time elapsed since the previous image, so the flight has the
	 * same speed on a fast or a slow computer (only the number of images changes). This time is limited, so that the
	 * camera never jumps if an image is exceptionally long to render: the flight slows down instead.
	 */
	static final float TOUR_MAX_STEP_SECONDS = 0.5f;
	/** Banking in the turns: degrees of roll for a turn of one degree per meter, and maximum roll, in degrees. */
	static final float TOUR_BANK_PER_TURN = 3f;
	static final float TOUR_MAX_BANK = 6f;

	// ----- Animations and timings -----

	/** A mouse wheel notch does not move the camera at once: it eases over this duration, with frames at this rate. */
	static final int DOLLY_ANIMATION_DURATION_MS = 150;
	static final int ANIMATION_FRAME_MS = 15; // About 60 frames per second, if the rendering follows
	/**
	 * While the user drags the border of the window, the view is resized (and rendered) only once the size has not
	 * changed for this delay, instead of rendering a full image for each intermediate size.
	 */
	static final int RESIZE_DELAY_MS = 50;

	static final String TITLE = "Temple ruins";

	/** The camera modes (see the class comment). */
	enum CameraMode {
		ORBIT("Orbit camera"), FLY("Fly camera"), HELICOPTER("Helicopter tour");

		final String label;

		CameraMode(String label) {
			this.label = label;
		}
	}

	// ****************************************************************************************************************
	// ***** Fields
	// ****************************************************************************************************************

	// ----- Aventura objects -----

	World world;
	Atmosphere atmosphere;       // The air of the World: the haze and the banks of mist
	SceneTextures textures;
	IslandTerrain terrain;       // The altitudes of the ground: to put things on it, and to keep the camera above it
	Element island;              // Everything that stands on the island, and the ground itself: what casts shadows
	TiledTrellis ground;
	Temple temple;

	Lighting lighting;
	DirectionalLight sun;
	LightAppearance sunAppearance; // Halo, lens flare and light shafts of the sun (Effects menu)
	float sunAzimuth = SUN_AZIMUTH, sunElevation = SUN_ELEVATION;

	Camera camera;
	PerspectiveContext pContext;
	RenderContext rContext;
	RenderEngine renderer;
	GUIView guiView;

	// ----- Swing user interface -----

	JFrame frame;
	JPanel panel; // Displays the rendered image, fills the window
	Timer resizeTimer;
	StatisticsWindow statistics; // The statistics of the scene and of the last image (Camera menu)
	// Radio buttons of the Camera menu, to follow a change of mode not asked through the menu
	Map<CameraMode, JRadioButtonMenuItem> cameraItems = new EnumMap<CameraMode, JRadioButtonMenuItem>(CameraMode.class);
	// Check boxes of the Effects menu that can be grayed out (see updateEffectsMenu())
	JCheckBoxMenuItem textureItem, mipmappingItem, specularItem, shadowsItem, softShadowsItem;
	JCheckBoxMenuItem hazeItem, mistItem, sunItem, sunHaloItem, lensFlareItem, lightShaftsItem;
	long lastRenderMillis; // Duration of the last rendering, displayed in the title of the window
	long lastRenderNanos; // The same, to the nanosecond, for the statistics of the tour
	double lastLapAverage, lastLapMin, lastLapMax; // Rendering times of the last lap of the tour (ms), for the title

	// ----- State of the cameras -----

	CameraMode cameraMode = CameraMode.ORBIT;

	/**
	 * Orbit camera: the camera moves on a sphere centered on the point of interest, described by spherical
	 * coordinates around the up axis Z: azimuth (angle around Z, from the X axis), elevation (angle above the
	 * horizontal plane) and distance (to the point of interest, before the zoom).
	 */
	float orbitAzimuth;
	float orbitElevation;
	float orbitDistance;
	float orbitDolly; // Zoom of the orbit, set aside in the other modes to get back to it

	/**
	 * Fly camera: position of the eye and direction of the line of sight, given by two angles like for a plane: yaw
	 * (heading, angle around Z from the X axis) and pitch (angle above the horizontal plane).
	 */
	Vector4 flyEye;
	float flyYaw;
	float flyPitch;
	// Start of the flight (where the camera was when the fly camera was chosen), for the Reset camera menu
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

	/** Helicopter tour: the path, the distance travelled along it, and the clock of the flight. */
	FlightPath tour;
	float tourDistance = 0;
	boolean tourPaused = false;
	long tourLastStepNanos;
	Timer tourTimer;

	/**
	 * Rendering times of the tour, lap by lap (see finishLap()): the times of the lap being flown, the number of laps
	 * finished, the report of the last one, and the options in force when the lap started, to tell if they changed.
	 */
	FrameTimeStatistics lapTimes = new FrameTimeStatistics();
	int lapCount = 0;
	String lastLapReport = null;
	String lapOptions;
	boolean lapOptionsChanged;
	boolean lapFromStart; // false for a lap entered on the way: it is not reported

	/** Last known position of the mouse during a drag. */
	int lastMouseX, lastMouseY;

	// ****************************************************************************************************************
	// ***** Main program
	// ****************************************************************************************************************

	public static void main(String[] args) {
		System.out.println("********* STARTING APPLICATION *********");
		TempleRuins demo = new TempleRuins();
		demo.run();
		System.out.println("********* APPLICATION is RUNNING *********");
	}

	/**
	 * Creates the scene, the rendering engine and the window, then renders the first image. From then on, the
	 * application is driven by the user (mouse, keyboard and menus, see the class comment).
	 */
	public void run() {

		// Steps 1 to 5: the scene and its RenderEngine
		createScene();

		// 6. The view (where the RenderEngine draws), in a window with its menus and its mouse and keyboard controls.
		// The window is shown only once the RenderEngine knows its view, so that a user action can always be rendered.
		guiView = createView(pContext);
		renderer.setView(guiView);
		frame.setVisible(true);

		// First image
		render();

		System.out.println("********* APPLICATION LAUNCHED *********");
	}

	/**
	 * Creates everything the rendering needs, but the view: the World, the Lighting, the Camera, the contexts and the
	 * RenderEngine. Separated from run() so that the scene can also be rendered without a window (a GUIView that is
	 * not displayed, like an ImageView: see the tests and DocumentationImages).
	 */
	public void createScene() {

		// 1. The World: the island, the temple, the trees, the sea and the air
		System.out.println("********* Creating World");
		world = createWorld();

		// 2. The Lighting: the sun and the light of the sky
		System.out.println("********* Creating Lighting");
		sun = createSun();
		sunAppearance = sun.getAppearance();
		lighting = createLighting(sun);

		// 3. The Camera, the orbit it starts on, and the path of the helicopter tour
		camera = createCamera();
		initOrbit();
		tour = new FlightPath(TOUR_WAYPOINTS);

		// 4. The contexts: projection on the screen, and rendering options
		pContext = createPerspectiveContext();
		rContext = createRenderContext();

		// 5. The RenderEngine, which renders the World, lit by the Lighting, seen by the Camera, with the contexts
		renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
	}

	/** The World of the scene, once created by createScene(). */
	public World getWorld() {
		return world;
	}

	/** The Lighting of the scene, once created by createScene(). */
	public Lighting getLighting() {
		return lighting;
	}

	// ****************************************************************************************************************
	// ***** 1. The World
	// ****************************************************************************************************************

	/**
	 * Creates the World. Its tree of Elements has two roots:
	 * <pre>
	 *   World
	 *    ├── island (group)      what the sun casts shadows on (see createSun())
	 *    │    ├── ground         TiledTrellis, from the altitudes of IslandTerrain
	 *    │    ├── temple         Temple: platform, colonnade, entablature, cella
	 *    │    ├── debris (group) fallen drums and blocks around the temple
	 *    │    ├── trees (group)  OliveTree x NB_TREES
	 *    │    └── boulders (group) Boulder x NB_BOULDERS
	 *    └── sea (group)         the textured sea around the island, the plain one up to the horizon
	 * </pre>
	 * The World is then built: the vertices, triangles and normals of all the Elements are calculated.
	 */
	World createWorld() {

		World world = new World("temple ruins");
		// The background is the sky: a single color. The sun and its halo are drawn over it (see createRenderContext()).
		world.setBackgroundColor(SKY_COLOR);
		atmosphere = createAtmosphere();
		world.setAtmosphere(atmosphere);

		textures = new SceneTextures();
		terrain = new IslandTerrain(GROUND_SIZE, GROUND_DIVISIONS, ISLAND_SEED);

		// An Element without geometry of its own groups other Elements
		island = new Element("island");
		ground = createGround(terrain, textures.ground);
		ground.setName("ground"); // Instead of the default name of a Trellis, for the statistics (see GeometryReport)
		island.addElement(ground);
		temple = new Temple(textures.marble, TEMPLE_SEED);
		// The temple stands in the middle of the plateau, its platform slightly sunk into the ground
		temple.setTransformation(new Translation(new Vector3(0, 0, IslandTerrain.PLATEAU_ALTITUDE - TEMPLE_SUNK)));
		island.addElement(temple);
		island.addElement(createDebris());
		island.addElement(createTrees());
		island.addElement(createBoulders());
		world.addElement(island);

		world.addElement(createSea(textures.sea));

		// Calculate the geometry of all the Elements of the World: vertices, triangles and normals
		System.out.println("********* Building World");
		world.build();
		

		// Colors are given to the triangles of the ground, which only exist once the World is built
		colorGround(ground, terrain);

		return world;
	}

	/** The ground of the island: a Trellis with the altitudes of the terrain, its texture repeated in tiles. */
	static TiledTrellis createGround(IslandTerrain terrain, Texture texture) {
		try {
			return new TiledTrellis(terrain.getSize(), terrain.getSize(), terrain.getDivisions(), terrain.getDivisions(),
					terrain.getAltitudes(), texture, GROUND_CELLS_PER_TILE);
		} catch (WrongArraySizeException e) {
			// Cannot happen: the array of altitudes of the terrain has the size of its grid
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Colors each triangle of the ground after the nature of the ground at its center: sand near the sea, bare rock
	 * on the steep slopes, and elsewhere dry earth with patches of dry grass. The color of a triangle has priority
	 * over the color of its Element, and tints the texture (the two are multiplied).
	 */
	static void colorGround(Trellis ground, IslandTerrain terrain) {
		for (Triangle triangle : ground.getTriangles()) {
			Vector4 p1 = triangle.getV1().getPos(), p2 = triangle.getV2().getPos(), p3 = triangle.getV3().getPos();
			float x = (p1.getX() + p2.getX() + p3.getX()) / 3;
			float y = (p1.getY() + p2.getY() + p3.getY()) / 3;
			float altitude = (p1.getZ() + p2.getZ() + p3.getZ()) / 3;
			triangle.setColor(groundColor(x, y, altitude, terrain.slope(x, y)));
		}
	}

	/**
	 * The color of the ground at a point: a mix of the four tints, each one with a share between 0 and 1 that varies
	 * smoothly (so that neighbor triangles have close colors).
	 */
	static Color groundColor(float x, float y, float altitude, float slope) {
		// Patches of grass: where a noise is above a threshold
		float grass = Noise.smoothstep(0.45f, 0.65f, Noise.fbm(x / 28, y / 28, 21, 3, ISLAND_SEED));
		Color color = SceneTextures.mix(EARTH_TINT, GRASS_TINT, grass);
		// Rock on the steep slopes (a slope of 1 is 45 degrees), sand at the bottom
		color = SceneTextures.mix(color, ROCK_TINT, Noise.smoothstep(0.4f, 0.75f, slope));
		color = SceneTextures.mix(color, SAND_TINT, Noise.smoothstep(2.2f, 0.4f, altitude));
		return color;
	}

	/**
	 * The sea: two flat surfaces at the altitude 0 (see NEAR_SEA_SIZE). Both have the same tint and a texture, so
	 * that they look the same with the Textures effect on or off: the far one has a texture of a single pixel, the
	 * average color of the image of the sea.
	 */
	static Element createSea(Texture seaTexture) {
		Element sea = new Element("sea");
		sea.setColor(SEA_TINT); // Inherited by both surfaces
		sea.setSpecularExp(SEA_SPECULAR_EXP);
		sea.setSpecularColor(SEA_SPECULAR_COLOR);

		// One tile of the texture per cell (25 m)
		TiledTrellis nearSea = new TiledTrellis(NEAR_SEA_SIZE, NEAR_SEA_SIZE, NEAR_SEA_DIVISIONS, NEAR_SEA_DIVISIONS, seaTexture, 1);
		nearSea.setName("near sea");
		copySurfaceOf(sea, nearSea);
		sea.addElement(nearSea);

		// Shrinking the texture down to a single pixel gives its average color
		Texture averageColor = SceneTextures.shrink(seaTexture, Math.min(seaTexture.getWidth(), seaTexture.getHeight()));
		averageColor.setName("sea, average color");
		Trellis farSea = new Trellis(FAR_SEA_SIZE, FAR_SEA_SIZE, FAR_SEA_DIVISIONS, FAR_SEA_DIVISIONS, averageColor);
		farSea.setName("far sea");
		copySurfaceOf(sea, farSea);
		farSea.setTransformation(new Translation(new Vector3(0, 0, -FAR_SEA_DEPTH)));
		sea.addElement(farSea);
		return sea;
	}

	/** The color is inherited from the parent Element, but not the specular reflection: it is copied. */
	private static void copySurfaceOf(Element parent, Element child) {
		child.setSpecularExp(parent.getSpecularExp());
		child.setSpecularColor(parent.getSpecularColor());
	}

	/**
	 * The debris around the temple: drums fallen from the missing columns, lying on the flat ground of the plateau
	 * along the south side and the back of the temple, and a few blocks fallen from the entablature.
	 */
	Element createDebris() {
		Element debris = new Element("debris");
		Random random = new Random(DEBRIS_SEED);
		float alongSouth = -(Temple.groundHalfWidth() + 1.4f), behind = Temple.groundHalfLength() + 1.5f;

		// Fallen drums: x, y, direction of the axis of the lying drum (degrees), rank in the shaft it comes from
		float[][] drums = {
				{ Temple.columnX(4), alongSouth - 0.4f, 100, 4 }, { Temple.columnX(4) + 1.6f, alongSouth - 1.5f, 75, 3 },
				{ Temple.columnX(5), alongSouth, 15, 2 }, { Temple.columnX(5) + 1.3f, alongSouth - 1.9f, 130, 1 },
				{ Temple.columnX(7), alongSouth - 0.2f, 60, 1 }, { Temple.columnX(7) + 0.9f, alongSouth - 2.3f, 172, 2 },
				{ Temple.columnX(7) - 1.5f, alongSouth - 1.6f, 95, 3 }, { Temple.columnX(10), alongSouth - 0.5f, 40, 0 },
				{ behind + 0.2f, Temple.columnY(0) + 0.6f, 115, 1 }, { behind + 1.4f, Temple.columnY(1), 20, 2 },
				{ behind, Temple.columnY(4) + 0.4f, 80, 3 }, { Temple.columnX(7) + 0.3f, -alongSouth, 8, 2 },
		};
		for (float[] d : drums) {
			TaperedDrum drum = Column.createFallenDrum((int) d[3], (float) Math.toRadians(d[2]), textures.marble, Temple.MARBLE_TINT, random);
			// A lying drum rests on the ground when its center is at the altitude of its radius; a little less, as if
			// it had sunk into the earth
			float z = groundAltitude(d[0], d[1]) + drum.getBottomRadius() * 0.9f;
			drum.combineTransformation(new Translation(new Vector3(d[0], d[1], z)));
			debris.addElement(drum);
		}

		// Fallen blocks: x, y, direction (degrees), tilt (degrees)
		float[][] blocks = {
				{ Temple.columnX(6), alongSouth - 0.9f, 20, 9 }, { Temple.columnX(8), alongSouth - 2.6f, -35, -14 },
				{ behind + 2.2f, Temple.columnY(3), 70, 11 }, { Temple.columnX(3), alongSouth - 2.9f, 110, 6 },
				{ Temple.columnX(9) + 0.5f, -alongSouth + 1.2f, -15, -8 },
		};
		for (float[] b : blocks) {
			Box block = new Box(2.1f, Temple.ARCHITRAVE_WIDTH, Temple.ARCHITRAVE_HEIGHT, textures.marble);
			block.setColor(SceneTextures.shade(Temple.MARBLE_TINT, 0.9f + 0.1f * random.nextFloat()));
			// Tilted around its long side first, then turned to its direction, then put on the ground
			block.setTransformation(new Rotation((float) Math.toRadians(b[3]), Vector3.xAxis()));
			block.combineTransformation(new Rotation((float) Math.toRadians(b[2]), Vector3.zAxis()));
			block.combineTransformation(new Translation(new Vector3(b[0], b[1], groundAltitude(b[0], b[1]) + Temple.ARCHITRAVE_HEIGHT * 0.3f)));
			debris.addElement(block);
		}
		return debris;
	}

	/**
	 * The olive trees: NB_TREES trees of random sizes at random places of the island where a tree can grow (on land,
	 * not too steep, away from the temple and from the other trees). Places are drawn at random until enough of them
	 * are suitable.
	 */
	Element createTrees() {
		Element trees = new Element("trees");
		Random random = new Random(TREES_SEED);
		List<float[]> places = new ArrayList<float[]>();

		for (int attempt = 0; attempt < 5000 && places.size() < NB_TREES; attempt++) {
			float x = (random.nextFloat() * 2 - 1) * TREE_AREA_RADIUS, y = (random.nextFloat() * 2 - 1) * TREE_AREA_RADIUS;
			if (!canGrowAt(x, y, places)) continue;
			places.add(new float[] { x, y });

			OliveTree tree = new OliveTree(TREE_MIN_SIZE + (TREE_MAX_SIZE - TREE_MIN_SIZE) * random.nextFloat(), textures, random.nextLong());
			tree.setTransformation(new Translation(new Vector3(x, y, groundAltitude(x, y))));
			trees.addElement(tree);
		}
		return trees;
	}

	/** true if a tree can grow at (x, y), given the places of the trees already planted. */
	private boolean canGrowAt(float x, float y, List<float[]> places) {
		float altitude = terrain.altitude(x, y);
		if (Float.isNaN(altitude) || altitude < TREE_MIN_ALTITUDE || terrain.slope(x, y) > TREE_MAX_SLOPE) return false;
		if (distanceToTemple(x, y) < TREE_MIN_DISTANCE_TO_TEMPLE) return false;
		for (float[] place : places) {
			if (Math.hypot(x - place[0], y - place[1]) < TREE_MIN_SPACING) return false;
		}
		return true;
	}

	/**
	 * The boulders: NB_BOULDERS rocks of random sizes and shapes scattered on the island, most of them small. Each
	 * one is a Boulder with its own seed, stretched or flattened by a Scaling, turned, and half buried.
	 */
	Element createBoulders() {
		Element boulders = new Element("boulders");
		Random random = new Random(BOULDERS_SEED);

		int count = 0;
		for (int attempt = 0; attempt < 5000 && count < NB_BOULDERS; attempt++) {
			float x = (random.nextFloat() * 2 - 1) * TREE_AREA_RADIUS, y = (random.nextFloat() * 2 - 1) * TREE_AREA_RADIUS;
			float altitude = terrain.altitude(x, y);
			// The squared random number makes small boulders more frequent than large ones
			float radius = Noise.lerp(BOULDER_MIN_RADIUS, BOULDER_MAX_RADIUS, random.nextFloat() * random.nextFloat());
			if (Float.isNaN(altitude) || altitude < 0.3f || distanceToTemple(x, y) < 2 + 2 * radius) continue;

			Boulder boulder = new Boulder(radius, 0.32f, 12, 7, random.nextInt(), textures.rock, true);
			boulder.setColor(SceneTextures.shade(BOULDER_TINT, 0.8f + 0.2f * random.nextFloat()));
			boulder.setTransformation(new Transformation(
					new Scaling(0.8f + 0.5f * random.nextFloat(), 0.8f + 0.5f * random.nextFloat(), 0.55f + 0.3f * random.nextFloat()),
					new Rotation(random.nextFloat() * 2 * (float) Math.PI, Vector3.zAxis()),
					new Translation(new Vector3(x, y, altitude + radius * 0.15f))));
			boulders.addElement(boulder);
			count++;
		}
		return boulders;
	}

	/** Distance from (x, y) to the platform of the temple (a rectangle centered on the origin): 0 on the platform. */
	static float distanceToTemple(float x, float y) {
		float dx = Math.max(0, Math.abs(x) - Temple.groundHalfLength());
		float dy = Math.max(0, Math.abs(y) - Temple.groundHalfWidth());
		return (float) Math.hypot(dx, dy);
	}

	/** Altitude of the ground at (x, y), 0 (the sea level) outside of the island. */
	float groundAltitude(float x, float y) {
		float altitude = terrain.altitude(x, y);
		return Float.isNaN(altitude) ? 0 : altitude;
	}

	/**
	 * The air of the World. The haze makes the far sea pale and blends it into the sky at the horizon; its factor
	 * grows with the square of the distance (EXPONENTIAL_SQUARED), which keeps the air clear near the camera, and goes
	 * up to 1 (complete haze) so that the far edge of the sea cannot be seen. The mist is a set of banks of fog,
	 * switched off at the start (Effects menu). Both are only drawn if the RenderContext enables the atmosphere.
	 */
	static Atmosphere createAtmosphere() {
		Atmosphere atmosphere = new Atmosphere(new Haze(HAZE_VISIBILITY).setModel(Haze.Model.EXPONENTIAL_SQUARED).setMaxFactor(1f));
		for (Fog bank : createMist(new Random(MIST_SEED))) {
			atmosphere.addFog(bank);
		}
		atmosphere.setFog(false);
		return atmosphere;
	}

	/**
	 * Banks of mist on the sea: spherical fogs centered at the sea level, spread around the island at MIST_DISTANCE
	 * of its center, more or less. The visibility at the center of a bank is 2 to 3 times its radius: a light mist,
	 * that veils the horizon without hiding it.
	 */
	static List<Fog> createMist(Random random) {
		List<Fog> banks = new ArrayList<Fog>();
		for (int i = 0; i < NB_MIST_BANKS; i++) {
			double angle = 2 * Math.PI * (i + random.nextFloat() * 0.6f) / NB_MIST_BANKS;
			float distance = MIST_DISTANCE * (0.8f + 0.5f * random.nextFloat());
			float radius = Noise.lerp(MIST_MIN_RADIUS, MIST_MAX_RADIUS, random.nextFloat());
			Vector4 center = new Vector4((float) (distance * Math.cos(angle)), (float) (distance * Math.sin(angle)), 0, 1);
			banks.add(new SphericalFog(center, radius, radius * (2 + random.nextFloat())).setColor(MIST_COLOR));
		}
		return banks;
	}

	// ****************************************************************************************************************
	// ***** 2. The Lighting
	// ****************************************************************************************************************

	/**
	 * The sun: a DirectionalLight (parallel rays, as from a light at infinity), of a warm white.
	 * <ul>
	 * <li>Its shadow map covers the island only (setShadowingElement()). By default it would cover the whole World,
	 *     that is the sea up to the horizon: the columns would be smaller than a pixel of the map, and cast no shadow.</li>
	 * <li>Its shadows are soft (PCF_3X3 filtering of the shadow map).</li>
	 * <li>Its LightAppearance gives it a halo and a lens flare when it is in the field of view (Effects menu).</li>
	 * </ul>
	 */
	DirectionalLight createSun() {
		DirectionalLight sun = new DirectionalLight(sunDirection(sunAzimuth, sunElevation), SUN_INTENSITY);
		sun.setLightColor(SUN_COLOR);
		sun.setShadowingElement(island);
		sun.setShadowMapSize(SHADOW_MAP_SIZE);
		sun.setShadowFilter(ShadowFilter.PCF_3X3);
		sun.setAppearance(new LightAppearance()); // Default halo and lens flare
		return sun;
	}

	/**
	 * The Lighting of the scene: the sun, the ambient light (the light of the sky, bluish: it is the only light of
	 * the surfaces in the shade), and the specular highlights enabled (third parameter).
	 */
	static Lighting createLighting(DirectionalLight sun) {
		AmbientLight sky = new AmbientLight(AMBIENT_INTENSITY);
		sky.setLightColor(AMBIENT_COLOR);
		return new Lighting(sun, sky, true);
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
	 * a view plane of 0.1 x 0.05625 m at a distance of 0.1 m from the eye (a horizontal field of view of 53 degrees),
	 * up to a depth of 5 km, with 10000 pixels per meter: a 1000 x 562 pixels image (IMAGE_WIDTH).
	 *
	 * The near plane is close to the eye, much less than the GROUND_CLEARANCE of the camera above the ground, so that
	 * it never cuts the ground. The far plane is beyond the distance where the haze has hidden everything.
	 *
	 * When the window is resized, the PerspectiveContext keeps the number of pixels per unit rather than the field of
	 * view (KEEP_SCALE): a larger window shows more of the scene rather than a larger image.
	 */
	public static PerspectiveContext createPerspectiveContext() {
		return createPerspectiveContext(IMAGE_WIDTH);
	}

	/**
	 * The same projection for an image of another width, in pixels (and 9/16 of it in height): the view plane does not
	 * change, only the number of pixels per meter. Used to render the scene without a window, at any size.
	 */
	public static PerspectiveContext createPerspectiveContext(int imageWidth) {
		float viewWidth = NEAR_DISTANCE, viewHeight = NEAR_DISTANCE * 9 / 16;
		PerspectiveContext pContext = new PerspectiveContext(viewWidth, viewHeight, NEAR_DISTANCE, FAR_DISTANCE, PerspectiveType.FRUSTUM,
				Math.round(imageWidth / viewWidth));
		pContext.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);
		return pContext;
	}

	/**
	 * The RenderContext: the rendering options at start-up, which the Rendering and Effects menus then change. Smooth
	 * shading (INTERPOLATE) with textures, shadows, the atmosphere and the sun visible in the sky (light glow).
	 * The textures are read with mipmapping: the ground and the marble are seen from 5 m as well as from 500 m, where
	 * a pixel covers dozens of texels. Without it they turn into a grainy noise in the distance, which flickers during
	 * the flight (switch "Mipmapping" off in the Effects menu to see it).
	 */
	public static RenderContext createRenderContext() {
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(true);
		rContext.setMipmapping(true);
		rContext.setShadowing(true);
		rContext.setAtmosphere(true);
		rContext.setLightGlow(true);
		return rContext;
	}

	// ****************************************************************************************************************
	// ***** 6. The view, its window and its controls (step 5, the RenderEngine, is a single line of createScene())
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

		frame = new JFrame(TITLE);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				stopAnimations();
			}
		});

		// The statistics of the scene and of the last image (Camera menu), also printed in the console when the window
		// is closed. StatisticsWindow is shared by the demos: it only needs the window, the World and the RenderEngine.
		statistics = new StatisticsWindow(frame, TITLE, world, renderer);
		statistics.setImageDescription(() -> guiView.getViewWidth() + " x " + guiView.getViewHeight() + " pixels, " + cameraMode.label);
		// With the rendering times of the last lap of the helicopter tour, once a lap has been flown
		statistics.setExtraSection(() -> lastLapReport);
		statistics.printOnClose();

		// Menus
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createCameraMenu());
		menuBar.add(createSunMenu());
		menuBar.add(createRenderingMenu());
		menuBar.add(createEffectsMenu());
		frame.setJMenuBar(menuBar);
		updateEffectsMenu();

		// The view: an image the RenderEngine draws into. Given the frame, it repaints it after each rendering.
		SwingView view = new SwingView(context, frame);
		view.setImageReuse(true); // An animation: the 3 images are recycled (see ImageView.setImageReuse())

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
	 * Keyboard shortcuts without a menu item: Up / Down arrows move the camera like the mouse wheel. The other keys
	 * are the accelerators of the menu items (see the menus).
	 */
	private void installKeyboardControls() {
		JComponent root = frame.getRootPane();
		bindKey(root, KeyEvent.VK_UP, "cameraForward", () -> dollyCamera(1));
		bindKey(root, KeyEvent.VK_DOWN, "cameraBackward", () -> dollyCamera(-1));
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
		render();
	}

	/**
	 * When the window is closed, the animations are stopped: a running Swing Timer would keep the application alive
	 * after its window is gone, rendering images that nobody sees.
	 */
	private void stopAnimations() {
		stopTour();
		if (dollyTimer != null) dollyTimer.stop();
		resizeTimer.stop();
	}

	/**
	 * Renders an image, and displays the camera mode and the time taken by the rendering in the title of the window.
	 * Every rendering of the application goes through here.
	 */
	private void render() {
		long start = System.nanoTime();
		renderer.render();
		lastRenderNanos = System.nanoTime() - start;
		lastRenderMillis = Math.round(lastRenderNanos / 1e6);
		String mode = cameraMode.label + (cameraMode == CameraMode.HELICOPTER && tourPaused ? " (paused)" : "");
		String title = String.format("%s - %s - %d ms per image", TITLE, mode, lastRenderMillis);
		if (cameraMode == CameraMode.HELICOPTER && lapCount > 0) {
			// The average of the last lap of the tour: a steadier figure than the time of the last image
			title += String.format(Locale.ROOT, " - lap %d: %.1f ms on average (%.1f to %.1f)", lapCount, lastLapAverage, lastLapMin, lastLapMax);
		}
		frame.setTitle(title);
	}

	// ****************************************************************************************************************
	// ***** Menus
	// ****************************************************************************************************************

	/**
	 * Camera menu: choice of the camera mode (an exclusive choice: radio buttons in a ButtonGroup), pause and reset, and
	 * the statistics of the scene and of the image the camera sees.
	 */
	private JMenu createCameraMenu() {
		JMenu menu = new JMenu("Camera");
		menu.setMnemonic(KeyEvent.VK_C);

		ButtonGroup group = new ButtonGroup();
		addCameraItem(menu, group, CameraMode.ORBIT, KeyEvent.VK_O);
		addCameraItem(menu, group, CameraMode.FLY, KeyEvent.VK_F);
		addCameraItem(menu, group, CameraMode.HELICOPTER, KeyEvent.VK_H);
		menu.addSeparator();

		addMenuItem(menu, "Pause / resume the tour", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), this::pauseTour);
		addMenuItem(menu, "Reset camera", KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), this::resetCamera);
		menu.addSeparator();

		menu.add(statistics.createMenuItem()); // "Statistics", key I
		return menu;
	}

	private void addCameraItem(JMenu menu, ButtonGroup group, CameraMode mode, int keyCode) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(mode.label, cameraMode == mode);
		item.setAccelerator(KeyStroke.getKeyStroke(keyCode, 0));
		item.addActionListener(e -> setCameraMode(mode));
		group.add(item);
		menu.add(item);
		cameraItems.put(mode, item);
	}

	/** Sun menu: moves the sun in the sky, by steps of SUN_STEP degrees. */
	private JMenu createSunMenu() {
		JMenu menu = new JMenu("Sun");
		menu.setMnemonic(KeyEvent.VK_S);
		addMenuItem(menu, "Higher", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), () -> moveSun(0, SUN_STEP));
		addMenuItem(menu, "Lower", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), () -> moveSun(0, -SUN_STEP));
		addMenuItem(menu, "Turn left", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), () -> moveSun(SUN_STEP, 0));
		addMenuItem(menu, "Turn right", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), () -> moveSun(-SUN_STEP, 0));
		menu.addSeparator();
		addMenuItem(menu, "Reset sun", null, () -> moveSun(SUN_AZIMUTH - sunAzimuth, SUN_ELEVATION - sunElevation));
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
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, rContext.getRenderingType() == type);
		item.addActionListener(e -> {
			rContext.setRenderingType(type);
			updateEffectsMenu();
			render();
		});
		group.add(item);
		menu.add(item);
	}

	/**
	 * Effects menu: on / off options, independent of the rendering type, in four groups (surfaces, shadows,
	 * atmosphere, sun). Each check box starts from the state of the model (RenderContext, Lighting, sun) and changes it.
	 */
	private JMenu createEffectsMenu() {
		JMenu menu = new JMenu("Effects");
		menu.setMnemonic(KeyEvent.VK_E);

		// Surfaces
		textureItem = addEffectItem(menu, "Textures", rContext.isTextureProcessing(), on -> rContext.setTextureProcessing(on));
		mipmappingItem = addEffectItem(menu, "Mipmapping", rContext.isMipmapping(), on -> rContext.setMipmapping(on));
		specularItem = addEffectItem(menu, "Specular highlights", lighting.hasSpecular(), on -> lighting.setSpecularLight(on));
		menu.addSeparator();

		// Shadows, hard or soft (PCF_3X3 filtering of the shadow map of the sun)
		shadowsItem = addEffectItem(menu, "Shadows", rContext.isShadowing(), on -> rContext.setShadowing(on));
		softShadowsItem = addEffectItem(menu, "Soft shadows", sun.getShadowFilter() == ShadowFilter.PCF_3X3,
				on -> sun.setShadowFilter(on ? ShadowFilter.PCF_3X3 : ShadowFilter.HARD));
		menu.addSeparator();

		// Atmosphere: the haze of the distance and the banks of mist, switched apart (the Atmosphere of the World has a
		// switch for each of its two branches; the RenderContext switch is on when one of them is)
		hazeItem = addEffectItem(menu, "Haze", atmosphere.isHaze(), on -> {
			atmosphere.setHaze(on);
			updateAtmosphere();
		});
		mistItem = addEffectItem(menu, "Sea mist", atmosphere.isFog(), on -> {
			atmosphere.setFog(on);
			updateAtmosphere();
		});
		menu.addSeparator();

		// Sun: the "light glow" of a directional light is the sun disc in the sky, the LightAppearance its halo and lens flare
		sunItem = addEffectItem(menu, "Sun", rContext.isLightGlow(), on -> rContext.setLightGlow(on));
		sunHaloItem = addEffectItem(menu, "Sun halo", sunAppearance.getGain() > 0, on -> sunAppearance.setGain(on ? 1f : 0f));
		lensFlareItem = addEffectItem(menu, "Lens flare", sunAppearance.getFlareGain() > 0, on -> sunAppearance.setFlareGain(on ? 1f : 0f));
		lightShaftsItem = addEffectItem(menu, "Light shafts", sunAppearance.getShaftsGain() > 0, on -> sunAppearance.setShaftsGain(on ? LIGHT_SHAFTS_GAIN : 0f));
		return menu;
	}

	/** Creates a check box of the Effects menu: applies the change to the model, updates the menu and renders. */
	private JCheckBoxMenuItem addEffectItem(JMenu menu, String label, boolean selected, Consumer<Boolean> apply) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, selected);
		item.addActionListener(e -> {
			apply.accept(item.isSelected());
			updateEffectsMenu();
			render();
		});
		menu.add(item);
		return item;
	}

	/**
	 * Grays out the effects that the current rendering type does not use: the textures are used by the filled rendering
	 * types (UNLIT, FLAT, INTERPOLATE), the specular highlights and the shadows by the lit ones (FLAT, INTERPOLATE),
	 * the sun, the haze and the mist by all but LINE. The sub-options (mipmapping, soft shadows, halo, lens flare,
	 * light shafts) also need their main option.
	 */
	private void updateEffectsMenu() {
		RenderingType type = rContext.getRenderingType();
		boolean lit = type == RenderingType.FLAT || type == RenderingType.INTERPOLATE;
		textureItem.setEnabled(lit || type == RenderingType.UNLIT);
		mipmappingItem.setEnabled(textureItem.isEnabled() && textureItem.isSelected());
		specularItem.setEnabled(lit);
		shadowsItem.setEnabled(lit);
		softShadowsItem.setEnabled(lit && shadowsItem.isSelected());
		hazeItem.setEnabled(type != RenderingType.LINE);
		mistItem.setEnabled(type != RenderingType.LINE);
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

	// ****************************************************************************************************************
	// ***** Actions on the World and the Lighting
	// ****************************************************************************************************************

	/**
	 * The atmosphere is drawn by the RenderContext when at least one of its two branches, the haze and the mist, is on
	 * (there is nothing to compute otherwise).
	 */
	private void updateAtmosphere() {
		rContext.setAtmosphere(atmosphere.isHaze() || atmosphere.isFog());
	}

	/**
	 * Moves the sun by the given angles, in degrees, and renders. Only the light is changed: at the next rendering,
	 * the engine computes the shadow map again from the new direction, and the sun is drawn at its new place in the sky.
	 * DirectionalLight.setLightVector() takes the direction TOWARDS the light, the opposite of the direction the light
	 * travels in (the one given to the constructor).
	 */
	private void moveSun(float azimuthChange, float elevationChange) {
		sunAzimuth += azimuthChange;
		sunElevation = clamp(sunElevation + elevationChange, SUN_MIN_ELEVATION, SUN_MAX_ELEVATION);
		sun.setLightVector(sunDirection(sunAzimuth, sunElevation).times(-1));
		render();
	}

	// ****************************************************************************************************************
	// ***** Camera control
	// ****************************************************************************************************************

	/**
	 * Turns the camera after a mouse drag, by the given angles (in radians) and renders. The vertical angle is clamped
	 * at each event (not on a total), so that dragging back moves the camera again at once, without a dead zone.
	 * During the helicopter tour, a drag takes the controls: the fly camera starts from where the camera is.
	 *
	 * @param horizontalAngle angle for the horizontal displacement of the mouse (positive to the right)
	 * @param verticalAngle   angle for the vertical displacement of the mouse (positive downward: the screen Y axis
	 *                        points down)
	 */
	private void turnCamera(float horizontalAngle, float verticalAngle) {
		if (cameraMode == CameraMode.HELICOPTER) setCameraMode(CameraMode.FLY);
		if (cameraMode == CameraMode.FLY) {
			// "Look around", like the head of the pilot: dragging to the right turns the line of sight to the right
			// (the yaw decreases, clockwise seen from above), dragging up looks up
			flyYaw -= horizontalAngle;
			flyPitch = clamp(flyPitch - verticalAngle, -MAX_ELEVATION, MAX_ELEVATION);
		} else {
			// "Grab the World": dragging to the right turns the scene to the right, hence the camera the other way
			// (the azimuth decreases); dragging down shows the scene more from above (the elevation increases)
			orbitAzimuth -= horizontalAngle;
			orbitElevation = clamp(orbitElevation + verticalAngle, -MAX_ELEVATION, MAX_ELEVATION);
		}
		updateCamera();
		render();
	}

	/**
	 * Moves the camera forward (positive notches) or backward (negative notches) along its line of sight: one notch of
	 * the wheel, or one press of an arrow key, is 1. The movement is not immediate, it eases over a short animation.
	 * <ul>
	 * <li>Orbit camera: zoom, between the minimum and maximum distances to the point of interest</li>
	 * <li>Fly camera: flight along the line of sight, without limit (but above the ground)</li>
	 * <li>Helicopter tour: takes the controls, like a drag (see turnCamera())</li>
	 * </ul>
	 */
	private void dollyCamera(float notches) {
		if (cameraMode == CameraMode.HELICOPTER) setCameraMode(CameraMode.FLY);
		if (cameraMode == CameraMode.FLY) {
			dollyTarget += notches;
		} else {
			float maxDolly = (orbitDistance - MIN_ORBIT_DISTANCE) / DOLLY_STEP;
			float minDolly = (orbitDistance - MAX_ORBIT_DISTANCE) / DOLLY_STEP;
			dollyTarget = clamp(dollyTarget + notches, minDolly, maxDolly);
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
			dollyTimer = new Timer(ANIMATION_FRAME_MS, e -> stepDollyAnimation());
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
		render();
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
		if (cameraMode == CameraMode.FLY) {
			flyEye = keepAboveGround(flyEye.plus(lineOfSight(flyYaw, flyPitch).times((dolly - dollyApplied) * DOLLY_STEP)));
		}
		dollyApplied = dolly;
		updateCamera();
	}

	/**
	 * Places the Camera according to the current mode:
	 * <ul>
	 * <li>Orbit: on its orbit (angles and zoom), looking at the point of interest, with Z as up vector</li>
	 * <li>Fly: at the position of the fly camera, looking along its line of sight, with Z as up vector</li>
	 * <li>Helicopter tour: where the flight path is after the distance travelled, looking at the target of the path,
	 *     with an up vector that leans in the turns (the camera banks)</li>
	 * </ul>
	 * The World does not move: only the Camera does.
	 */
	private void updateCamera() {
		switch (cameraMode) {
		case ORBIT:
			float distance = orbitDistance - dollyApplied * DOLLY_STEP;
			camera.updateCamera(orbitEye(POINT_OF_INTEREST, distance, orbitAzimuth, orbitElevation), POINT_OF_INTEREST, Vector4.zAxis());
			break;
		case FLY:
			camera.updateCamera(flyEye, flyEye.plus(lineOfSight(flyYaw, flyPitch)), Vector4.zAxis());
			break;
		case HELICOPTER:
			camera.updateCamera(tour.eye(tourDistance), tour.target(tourDistance),
					tour.up(tourDistance, TOUR_BANK_PER_TURN, (float) Math.toRadians(TOUR_MAX_BANK)));
			break;
		}
	}

	/**
	 * Changes the camera mode, and renders.
	 * <ul>
	 * <li>To the fly camera: the flight starts where the camera is, looking in the same direction (so the view does
	 *     not jump).</li>
	 * <li>To the orbit camera: the camera goes back on its orbit, where it was (same angles and zoom).</li>
	 * <li>To the helicopter tour: the tour starts, or goes on from where it was left.</li>
	 * </ul>
	 */
	private void setCameraMode(CameraMode mode) {
		if (mode == cameraMode) return;
		finishDollyAnimation();
		stopTour();
		if (cameraMode == CameraMode.ORBIT) orbitDolly = dollyApplied; // Set aside, to come back to it

		switch (mode) {
		case ORBIT:
			dollyTarget = orbitDolly;
			dollyApplied = orbitDolly;
			break;
		case FLY:
			// From the current position of the camera: its eye, and its line of sight as two angles
			Vector4 eye = camera.getEye();
			Vector4 direction = currentTarget().minus(eye).normalize();
			flyStartEye = keepAboveGround(new Vector4(eye));
			flyStartYaw = (float) Math.atan2(direction.getY(), direction.getX());
			flyStartPitch = clamp((float) Math.asin(direction.getZ()), -MAX_ELEVATION, MAX_ELEVATION);
			startFlight();
			break;
		case HELICOPTER:
			tourPaused = false;
			startLap(tourDistance == 0); // The tour resumes where it was left: only a whole lap is measured
			startTour();
			break;
		}
		cameraMode = mode;
		cameraItems.get(mode).setSelected(true); // If the change does not come from the menu (see turnCamera())
		updateCamera();
		render();
	}

	/** The point the camera currently looks at, according to the current mode. */
	private Vector4 currentTarget() {
		switch (cameraMode) {
		case FLY:
			return flyEye.plus(lineOfSight(flyYaw, flyPitch));
		case HELICOPTER:
			return tour.target(tourDistance);
		default:
			return POINT_OF_INTEREST;
		}
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
	 * the start of the flight for the fly camera, the first waypoint for the helicopter tour.
	 */
	private void resetCamera() {
		if (dollyTimer != null) dollyTimer.stop();
		switch (cameraMode) {
		case ORBIT:
			dollyTarget = 0;
			dollyApplied = 0;
			initOrbit();
			break;
		case FLY:
			startFlight();
			break;
		case HELICOPTER:
			tourDistance = 0;
			startLap(true);
			break;
		}
		updateCamera();
		render();
	}

	/**
	 * Returns the given position of the eye, raised if needed to be at least GROUND_CLEARANCE above the ground and
	 * the sea: the camera slides on them instead of going under. The ground is measured under the camera and around
	 * it (at GROUND_CLEARANCE on each side): on a steep slope, the ground rising beside the camera could otherwise come
	 * closer than the near plane, which would cut it.
	 */
	private Vector4 keepAboveGround(Vector4 eye) {
		float x = eye.getX(), y = eye.getY(), r = GROUND_CLEARANCE;
		float[][] samples = { { x, y }, { x - r, y }, { x + r, y }, { x, y - r }, { x, y + r } };
		float ground = 0; // The sea level
		for (float[] sample : samples) {
			ground = Math.max(ground, groundAltitude(sample[0], sample[1]));
		}
		if (eye.getZ() < ground + GROUND_CLEARANCE) eye.setZ(ground + GROUND_CLEARANCE);
		return eye;
	}

	// ****************************************************************************************************************
	// ***** Helicopter tour
	// ****************************************************************************************************************

	/**
	 * Starts the clock of the tour: a Swing Timer that calls stepTour() again and again, on the Swing thread. If a
	 * rendering takes longer than the delay of the timer, the next step simply comes right after it: the timer never
	 * queues more than one step.
	 */
	private void startTour() {
		tourLastStepNanos = System.nanoTime();
		if (tourTimer == null) {
			tourTimer = new Timer(ANIMATION_FRAME_MS, e -> stepTour());
		}
		tourTimer.start();
	}

	private void stopTour() {
		if (tourTimer != null) tourTimer.stop();
	}

	/**
	 * One image of the tour: the camera moves forward on its path by the speed times the time elapsed since the
	 * previous step (see TOUR_MAX_STEP_SECONDS), and the image is rendered.
	 */
	private void stepTour() {
		long now = System.nanoTime();
		float elapsedSeconds = (now - tourLastStepNanos) / 1e9f;
		tourLastStepNanos = now;
		float previousDistance = tourDistance;
		tourDistance = advance(tourDistance, elapsedSeconds, TOUR_SPEED, TOUR_MAX_STEP_SECONDS, tour.getLength());
		if (tourDistance < previousDistance) {
			finishLap(); // Back to the start of the path
		}
		updateCamera();
		render();

		// The rendering time of this image, for the statistics of the lap
		lapTimes.add(lastRenderNanos / 1e6);
		lapTimes.addDuration(elapsedSeconds);
		if (!lapOptions.equals(lapOptions())) lapOptionsChanged = true;
	}

	// ****************************************************************************************************************
	// ***** Rendering times of a lap of the tour
	// ****************************************************************************************************************
	//
	// The time of one image changes with what the camera sees (from 40 to 150 ms around the island), and from an
	// image to the next. To know what a rendering option costs or saves, e.g. the mipmapping, an average over the same
	// views is needed: a whole lap of the tour, always the same path at the same speed. At the end of each lap, the
	// number of images and their average, fastest and slowest rendering times are printed in the console with the
	// options in force, shown in the title of the window, and added to the statistics (Camera menu, key I).

	/**
	 * Starts measuring a lap, with the current options.
	 *
	 * @param fromStart false if the camera is already somewhere on the path: the end of this lap will not be reported
	 */
	private void startLap(boolean fromStart) {
		lapFromStart = fromStart;
		lapTimes.reset();
		lapOptions = lapOptions();
		lapOptionsChanged = false;
	}

	/** End of a lap: reports its rendering times, and starts measuring the next one. */
	private void finishLap() {
		if (lapFromStart && lapTimes.getCount() > 0) {
			lapCount++;
			lastLapAverage = lapTimes.getAverageMillis();
			lastLapMin = lapTimes.getMinMillis();
			lastLapMax = lapTimes.getMaxMillis();
			lastLapReport = "HELICOPTER TOUR, LAP " + lapCount + " (" + lapOptions + ")\n" + lapTimes
					+ (lapOptionsChanged ? "\nThe options were changed during this lap: its times mix them" : "");
			System.out.println(lastLapReport + "\n");
		}
		startLap(true);
	}

	/**
	 * What the rendering time of a lap depends on, apart from the scene and the path: the size of the image, the
	 * rendering type, the effects switched on (the selected check boxes of the Effects menu that are not grayed out)
	 * and the position of the sun, which changes the shadows.
	 */
	private String lapOptions() {
		StringBuilder options = new StringBuilder();
		options.append(guiView.getViewWidth()).append(" x ").append(guiView.getViewHeight()).append(" pixels, ");
		options.append(rContext.getRenderingType());
		JCheckBoxMenuItem[] effects = { textureItem, mipmappingItem, specularItem, shadowsItem, softShadowsItem, hazeItem, mistItem,
				sunItem, sunHaloItem, lensFlareItem, lightShaftsItem };
		for (JCheckBoxMenuItem effect : effects) {
			if (effect.isEnabled() && effect.isSelected()) options.append(", ").append(effect.getText().toLowerCase(Locale.ROOT));
		}
		options.append(String.format(Locale.ROOT, ", sun at %.0f / %.0f degrees", sunAzimuth, sunElevation));
		return options.toString();
	}

	/** Pauses or resumes the helicopter tour (nothing to do in the other camera modes). */
	private void pauseTour() {
		if (cameraMode != CameraMode.HELICOPTER) return;
		tourPaused = !tourPaused;
		if (tourPaused) {
			stopTour();
			render(); // To update the title
		} else {
			startTour();
		}
	}

	// ****************************************************************************************************************
	// ***** Geometry helpers
	// ***** Pure functions, static and package-private so that they can be unit-tested without a window
	// ***** (see TestTempleRuins)
	// ****************************************************************************************************************

	/**
	 * The distance travelled on a closed path after one more step: the speed times the time elapsed, this time being
	 * limited to maxStepSeconds, back to 0 at the end of a lap.
	 */
	static float advance(float distance, float elapsedSeconds, float speed, float maxStepSeconds, float lapLength) {
		float next = distance + speed * Math.min(elapsedSeconds, maxStepSeconds);
		return next >= lapLength ? next - lapLength : next;
	}

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
	static Vector4 lineOfSight(float yaw, float pitch) {
		float horizontal = (float) Math.cos(pitch);
		return new Vector4(
				horizontal * (float) Math.cos(yaw),
				horizontal * (float) Math.sin(yaw),
				(float) Math.sin(pitch),
				0);
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
