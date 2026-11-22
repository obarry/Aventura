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
package com.aventura.demo.ship;

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
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.demo.FrameTimeStatistics;
import com.aventura.demo.LightBudget;
import com.aventura.demo.StatisticsWindow;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Light;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;

/**
 * Demo application of the Aventura rendering engine: the inside of a spaceship, as in the science fiction films of the
 * late seventies and eighties, futuristic but worn. A loop of metal corridors, straight and curved, a shortcut through
 * a control room full of consoles, two junctions, airlock doors open and closed, pipes everywhere. Lit only by its own
 * lights: dozens of point lights and spot lights, of which only the few nearest to the visitor are switched on.
 *
 * <h2>What this demo shows of the Aventura API</h2>
 *
 * The {@link #createScene()} method follows the typical steps of an Aventura application, one method per step, like
 * TempleRuins. What this demo shows that TempleRuins does not:
 * <ul>
 * <li><b>A scene lit by point lights and spot lights only</b> ({@link ShipLights}): {@link PointLight}s under the
 *     fixtures of the ceilings, SpotLights in the junctions, on the consoles and on the hatch of the hold, each one
 *     casting shadows (a cube of six shadow maps for a point light), with a colored halo when the lights are made
 *     visible (Lights menu, off by default: the lenses of the fixtures glow instead, see Glow; on in the plan view).</li>
 * <li><b>Many lights, few switched on</b>: {@link Light#setEnabled(boolean)} and the {@link LightBudget} shared by the
 *     demos. Before each image, the lights nearest to the camera are switched on, faded so that none pops in or out,
 *     and the others off: the cost of an image depends on the lights switched on, not on the forty lights of the ship.
 *     The shadow maps of the ship are drawn once and kept (static shadow casters), even while their light is off.</li>
 * <li><b>One new shape, many parts</b>: {@link SweptSection} sweeps a cross-section along a straight or curved
 *     {@link PathSegment}: the walls and the floor of the corridors (an octagon seen from inside), the pipes (circles
 *     seen from outside), the cable trays (an open U). Two more shapes: {@link SectionFrame} (the ribs of the hull, the
 *     frames of the doors) and {@link Panel} (the walls of the rooms, with or without a doorway).</li>
 * <li><b>Textures computed for their size</b> ({@link ShipTextures}): plates of 1 m with their seams, rivets and rust,
 *     a grating, a tread plate, hazard stripes, the screens of the consoles. Repeated like tiles of a size given in
 *     meters, whatever the size of the surface.</li>
 * <li><b>Moving Elements</b>: the closed door of the north junction opens and closes (Doors menu): its leaves are moved
 *     by their Translation, and the lights near it are told to draw their shadow map again
 *     ({@link ShadowingLight#invalidateShadowMap()}).</li>
 * <li><b>Back-face culling</b>: the walls are seen from inside only. From above, the plan view sees through the
 *     ceilings into the whole ship, like a doll's house.</li>
 * </ul>
 *
 * <h2>Cameras</h2>
 * <ul>
 * <li><b>Walk</b> (key W): a visitor at the height of the eyes. Mouse drag: look around. Mouse wheel (or Up / Down
 *     arrow keys): walk forward / backward, horizontally, in the corridors and the rooms ({@link Ship#keepInside}).</li>
 * <li><b>Visit</b> (key V): the camera walks the {@link ShipTour}, looking ahead in the turns and at the points of
 *     interest, at a constant speed; it stops in front of the north door, which opens, then closes behind. Space pauses and resumes the visit. A drag or the wheel takes the controls: the
 *     walk starts from there.</li>
 * <li><b>Plan</b> (key P): the whole ship seen from above, through its ceilings, all its lights switched on. Mouse
 *     drag: turn around the ship. Mouse wheel: zoom.</li>
 * </ul>
 *
 * <h2>Menus</h2>
 * <ul>
 * <li>Camera: the camera, pause of the visit, reset, the field of view (presets, or keys + and -), and the statistics
 *     (key I), with the rendering times of the last lap of the visit and the lights switched on.</li>
 * <li>Lights: the size of the light budget (1 to 6 lights), or all the lights on; the halo of the lights (off by
 *     default), which shows where the lights are and which ones the budget switches on; the north door (key D).</li>
 * <li>Rendering: the rendering type.</li>
 * <li>Effects: textures, mipmapping, specular highlights, shadows, soft shadows, haze, the screens and lamps that glow by
 *     themselves (see Glow).</li>
 * </ul>
 *
 * <h2>Conventions</h2>
 * Z is the up axis and the unit is the meter. The control room is centered on the origin, the long corridors are
 * along X (see the plan in {@link Ship}).
 *
 * @since Nov 2026
 */
public class StarshipInterior {

	// ****************************************************************************************************************
	// ***** Constants
	// ****************************************************************************************************************

	/** Nearly black, a little blue: the color of what no light reaches, and of the haze of the distance. */
	static final Color DARKNESS = new Color(6, 8, 12);
	/** The haze: the far end of a long corridor fades into the dark (visibility in meters, see TempleRuins). */
	static final float HAZE_VISIBILITY = 45f;

	/**
	 * The view plane: VIEW_WIDTH wide at IMAGE_WIDTH pixels. With a frustum, it is also the near plane, close to the eye:
	 * in a doorway, the frame is 0.5 m away. Its distance to the eye sets the field of view (see setFieldOfView()).
	 */
	static final float VIEW_WIDTH = 0.05f;
	static final float FAR_DISTANCE = 300f;
	static final int IMAGE_WIDTH = 1000;

	/**
	 * Horizontal field of view, in degrees: rather wide, as for a first person view (TempleRuins: 53 degrees). The Camera
	 * menu offers the presets; the + and - keys change it by FIELD_OF_VIEW_STEP, between the minimum and the maximum.
	 */
	static final float DEFAULT_FIELD_OF_VIEW = 72;
	static final float[] FIELD_OF_VIEW_PRESETS = { 50, 60, 72, 85, 100 };
	static final float FIELD_OF_VIEW_STEP = 2;
	static final float MIN_FIELD_OF_VIEW = 30, MAX_FIELD_OF_VIEW = 120;

	/** Where the walk starts: in the south corridor, near the south junction, looking east. */
	static final Vector4 WALK_START = new Vector4(3f, -Ship.LOOP_HALF_WIDTH, ShipTour.EYE_HEIGHT, 1);
	static final float WALK_START_YAW = 0;

	/** Plan view: the point it turns around, and its initial distance, azimuth and elevation (radians). */
	static final Vector4 PLAN_CENTER = new Vector4(0, -2, 0, 1);
	static final float PLAN_DISTANCE = 72f;
	static final float PLAN_AZIMUTH = (float) Math.toRadians(-90);
	static final float PLAN_ELEVATION = (float) Math.toRadians(62);
	static final float MIN_PLAN_DISTANCE = 12f, MAX_PLAN_DISTANCE = 160f;

	static final float DRAG_ANGLE_PER_WIDTH = (float) Math.PI;
	static final float DRAG_ANGLE_PER_HEIGHT = (float) Math.PI / 2;
	static final float MAX_ELEVATION = (float) Math.PI / 2 - 0.01f;
	/** Distance walked per notch of the mouse wheel; in the plan view, zoom per notch. */
	static final float WALK_STEP = 0.6f;
	static final float PLAN_STEP = 4f;

	/** Speed of the visit, in meters per second, and the longest step (see TempleRuins). */
	static final float VISIT_SPEED = 2.3f;
	static final float VISIT_MAX_STEP_SECONDS = 0.5f;

	/** The sizes of light budget offered by the Lights menu. */
	static final int[] BUDGET_SIZES = { 1, 2, 3, 4, 6 };

	/** The north door opens or closes in this time; the lights within this distance of it draw their shadow map again. */
	static final int DOOR_DURATION_MS = 2500;
	static final int ANIMATION_FRAME_MS = 15;
	static final int DOLLY_ANIMATION_DURATION_MS = 150;
	static final int RESIZE_DELAY_MS = 50;

	static final String TITLE = "Starship interior";

	enum CameraMode {
		WALK("Walk"), VISIT("Visit"), PLAN("Plan view");

		final String label;

		CameraMode(String label) {
			this.label = label;
		}
	}

	// ****************************************************************************************************************
	// ***** Fields
	// ****************************************************************************************************************

	World world;
	Atmosphere atmosphere;
	ShipTextures textures;
	Ship ship;
	Glow.Switch emission;     // The screens and lamps that glow by themselves, switched off and on (Effects menu)
	ShipLights lights;
	Lighting lighting;
	LightBudget budget;
	boolean allLightsOn = false;
	boolean lightHalo = false;    // The halo of the lights, at their position (Lights menu): off, the lenses glow instead
	AirlockDoor northDoor;       // The door the Lights menu opens and closes

	Camera camera;
	PerspectiveContext pContext;
	RenderContext rContext;
	RenderEngine renderer;
	GUIView guiView;

	JFrame frame;
	JPanel panel;
	Timer resizeTimer;
	StatisticsWindow statistics;
	Map<CameraMode, JRadioButtonMenuItem> cameraItems = new EnumMap<CameraMode, JRadioButtonMenuItem>(CameraMode.class);
	JCheckBoxMenuItem textureItem, mipmappingItem, specularItem, shadowsItem, softShadowsItem, hazeItem, glowItem, emissionItem;
	long lastRenderNanos;
	int lightsOn;

	CameraMode cameraMode = CameraMode.WALK;
	float fieldOfView = DEFAULT_FIELD_OF_VIEW;
	Map<Float, JRadioButtonMenuItem> fieldOfViewItems = new java.util.LinkedHashMap<Float, JRadioButtonMenuItem>();
	ButtonGroup fieldOfViewGroup = new ButtonGroup();
	Vector4 lookTarget; // The point the camera looks at: the light budget prefers the lights in front of the camera

	/** Walk: position of the eye, and direction of the look (yaw around Z from the X axis, pitch above the horizontal). */
	Vector4 walkEye = new Vector4(WALK_START);
	float walkYaw = WALK_START_YAW, walkPitch = 0;

	/** Plan view: spherical coordinates of the eye around PLAN_CENTER. */
	float planAzimuth = PLAN_AZIMUTH, planElevation = PLAN_ELEVATION, planDistance = PLAN_DISTANCE;

	/** Wheel movements, eased over a short animation (see TempleRuins). */
	float dollyTarget = 0, dollyApplied = 0, dollyAnimationStart;
	long dollyAnimationStartMillis;
	Timer dollyTimer;

	/** The visit: the path, the distance walked, its clock. */
	ShipTour tour;
	float tourDistance = 0;
	float stopTime = -1;          // Time since the visit stopped in front of the north door, -1 when walking
	float doorClosingTime = -1;   // Time since the door started closing after the stop, -1 when it does not
	boolean tourPaused = false, tourRunning = false;
	long tourLastStepNanos;
	int tourRun = 0;
	FrameTimeStatistics lapTimes = new FrameTimeStatistics();
	int lapCount = 0;
	String lastLapReport = null;
	boolean lapFromStart;
	double lastLapAverage;

	/** The animation of the north door: its opening goes from doorFrom to doorTo. */
	Timer doorTimer;
	float doorFrom, doorTo;
	long doorStartMillis;

	int lastMouseX, lastMouseY;

	// ****************************************************************************************************************
	// ***** Main program
	// ****************************************************************************************************************

	public static void main(String[] args) {
		System.out.println("********* STARTING APPLICATION *********");
		new StarshipInterior().run();
		System.out.println("********* APPLICATION is RUNNING *********");
	}

	/** Creates the scene, the rendering engine and the window, then renders the first image. */
	public void run() {
		createScene();
		guiView = createView(pContext);
		renderer.setView(guiView);
		frame.setVisible(true);
		render();
	}

	/**
	 * Creates everything the rendering needs, but the view: separated from run() so that the scene can be rendered
	 * without a window (see the tests and DocumentationImages).
	 */
	public void createScene() {
		// 1. The World: the ship, and the air of its corridors
		System.out.println("********* Creating World");
		world = createWorld();

		// 2. The Lighting: the lights of the ship, and the budget that keeps a few of them on
		System.out.println("********* Creating Lighting");
		lights = new ShipLights(ship);
		lighting = lights.getLighting();
		budget = lights.getBudget();

		// 3. The Camera, and the visit
		camera = new Camera(new Vector4(WALK_START), new Vector4(WALK_START).plus(lineOfSight(walkYaw, walkPitch)), Vector4.zAxis());
		tour = new ShipTour();

		// 4. The contexts
		pContext = createPerspectiveContext();
		rContext = createRenderContext();

		// 5. The RenderEngine
		renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
		updateCamera();
	}

	public World getWorld() {
		return world;
	}

	public Lighting getLighting() {
		return lighting;
	}

	/** The screens and lamps that glow by themselves (see Glow). */
	public Glow.Switch getEmission() {
		return emission;
	}

	public Ship getShip() {
		return ship;
	}

	// ****************************************************************************************************************
	// ***** 1. The World
	// ****************************************************************************************************************

	/**
	 * The World: the ship (see its tree in {@link Ship}), on a background nearly black, in a haze of the same color
	 * that swallows the far end of the long corridors.
	 */
	World createWorld() {
		World world = new World("starship interior");
		world.setBackgroundColor(DARKNESS);
		atmosphere = new Atmosphere(new Haze(HAZE_VISIBILITY).setColor(DARKNESS).setModel(Haze.Model.EXPONENTIAL));
		world.setAtmosphere(atmosphere);

		textures = new ShipTextures();
		ship = new Ship(textures);
		world.addElement(ship);
		northDoor = ship.getHubs().get(1).getDoors().get(0); // The first door of the north junction (its north side): the closed one
		emission = new Glow.Switch(ship); // What glows by itself: screens, lenses, buttons (Effects menu)

		System.out.println("********* Building World");
		world.build();
		return world;
	}

	// ****************************************************************************************************************
	// ***** 4. The contexts
	// ****************************************************************************************************************

	/**
	 * A perspective projection, a horizontal field of view of DEFAULT_FIELD_OF_VIEW, an image IMAGE_WIDTH pixels wide
	 * (see TempleRuins).
	 */
	public static PerspectiveContext createPerspectiveContext() {
		return createPerspectiveContext(IMAGE_WIDTH);
	}

	public static PerspectiveContext createPerspectiveContext(int imageWidth) {
		float viewWidth = VIEW_WIDTH, viewHeight = VIEW_WIDTH * 9 / 16;
		PerspectiveContext pContext = new PerspectiveContext(viewWidth, viewHeight, viewDistance(viewWidth, DEFAULT_FIELD_OF_VIEW), FAR_DISTANCE,
				PerspectiveType.FRUSTUM, Math.round(imageWidth / viewWidth));
		pContext.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);
		return pContext;
	}

	/**
	 * Smooth shading with textures, mipmapping, shadows and the haze. The halo of the lights is off: the lenses of the
	 * fixtures glow by themselves (see Glow), where a halo would float under them, at the position of the light, and only
	 * for the few lights of the budget (see render(), and the Lights menu).
	 */
	public static RenderContext createRenderContext() {
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(true);
		rContext.setMipmapping(true);
		rContext.setShadowing(true);
		rContext.setAtmosphere(true);
		rContext.setLightGlow(false);
		return rContext;
	}

	/**
	 * The distance from the eye at which a view plane of the given width sees the given horizontal field of view: the
	 * plane and the eye make an isosceles triangle, of half-angle fieldOfView / 2, so tan(fieldOfView / 2) = (width / 2)
	 * / distance. The nearer the plane, the wider the view.
	 */
	static float viewDistance(float viewWidth, float fieldOfViewDegrees) {
		return viewWidth / 2 / (float) Math.tan(Math.toRadians(fieldOfViewDegrees) / 2);
	}

	/**
	 * Changes the field of view: the view plane keeps its width (its pixels, see PerspectiveContext.ResizeMode.KEEP_SCALE),
	 * and is moved nearer to the eye or farther from it (Perspective.setDist()). The projection follows.
	 */
	public void setFieldOfView(float degrees) {
		fieldOfView = clamp(degrees, MIN_FIELD_OF_VIEW, MAX_FIELD_OF_VIEW);
		applyFieldOfView();
	}

	public float getFieldOfView() {
		return fieldOfView;
	}

	/**
	 * Places the view plane for the current field of view. Called before each image: when the window is resized, the
	 * width of the view plane changes with it (the RenderEngine resizes the PerspectiveContext at the start of the image),
	 * and the field of view is kept by moving the plane.
	 */
	private void applyFieldOfView() {
		int pixels = guiView != null ? guiView.getViewWidth() : pContext.getPixelWidth();
		float viewWidth = (float) pixels / pContext.getPPU(); // The width the view plane will have for this image
		float distance = viewDistance(viewWidth, fieldOfView);
		if (distance != pContext.getPerspective().getDist()) pContext.getPerspective().setDist(distance);
	}

	// ****************************************************************************************************************
	// ***** 6. The view, its window and its controls
	// ****************************************************************************************************************

	public GUIView createView(PerspectiveContext context) {
		frame = new JFrame(TITLE);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				stopAnimations();
			}
		});

		statistics = new StatisticsWindow(frame, TITLE, world, renderer);
		statistics.setImageDescription(() -> guiView.getViewWidth() + " x " + guiView.getViewHeight() + " pixels, " + cameraMode.label);
		statistics.setExtraSection(this::extraStatistics);
		statistics.printOnClose();

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createCameraMenu());
		menuBar.add(createLightsMenu());
		menuBar.add(createRenderingMenu());
		menuBar.add(createEffectsMenu());
		frame.setJMenuBar(menuBar);
		updateEffectsMenu();

		SwingView view = new SwingView(context, frame);
		view.setImageReuse(true);
		panel = new JPanel() {
			@Override
			public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				graphics.drawImage(view.getImageView(), 0, 0, null);
			}
		};
		panel.setBackground(Color.BLACK);
		panel.setPreferredSize(new Dimension(context.getPixelWidth(), context.getPixelHeight()));
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);

		installMouseControls();
		installKeyboardControls();
		installResizeControl();
		return view;
	}

	/** The rendering times of the last lap of the visit, and the lights switched on for the last image. */
	private String extraStatistics() {
		StringBuilder text = new StringBuilder();
		text.append("LIGHTS: ").append(lightsOn).append(" switched on out of ").append(budget.getNbLights())
				.append(allLightsOn ? " (all lights on)" : " (budget of " + budget.getMaxLights() + ")").append("\n");
		if (!allLightsOn) text.append(budget.report());
		if (lastLapReport != null) text.append("\n").append(lastLapReport);
		return text.toString();
	}

	private void installMouseControls() {
		MouseAdapter mouseControls = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				lastMouseX = e.getX();
				lastMouseY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int dx = e.getX() - lastMouseX, dy = e.getY() - lastMouseY;
				lastMouseX = e.getX();
				lastMouseY = e.getY();
				turnCamera(DRAG_ANGLE_PER_WIDTH * dx / panel.getWidth(), DRAG_ANGLE_PER_HEIGHT * dy / panel.getHeight());
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				dollyCamera((float) -e.getPreciseWheelRotation());
			}
		};
		panel.addMouseListener(mouseControls);
		panel.addMouseMotionListener(mouseControls);
		panel.addMouseWheelListener(mouseControls);
	}

	private void installKeyboardControls() {
		JComponent root = frame.getRootPane();
		bindKey(root, KeyEvent.VK_UP, "forward", () -> dollyCamera(1));
		bindKey(root, KeyEvent.VK_DOWN, "backward", () -> dollyCamera(-1));
		bindKey(root, KeyEvent.VK_LEFT, "turnLeft", () -> turnCamera(-0.12f, 0));
		bindKey(root, KeyEvent.VK_RIGHT, "turnRight", () -> turnCamera(0.12f, 0));
		// The field of view: + and -, typed (whatever the keyboard layout) or on the numeric keypad
		bindKey(root, KeyStroke.getKeyStroke('+'), "wider", () -> changeFieldOfView(fieldOfView + FIELD_OF_VIEW_STEP));
		bindKey(root, KeyStroke.getKeyStroke('-'), "narrower", () -> changeFieldOfView(fieldOfView - FIELD_OF_VIEW_STEP));
		bindKey(root, KeyEvent.VK_ADD, "widerKeypad", () -> changeFieldOfView(fieldOfView + FIELD_OF_VIEW_STEP));
		bindKey(root, KeyEvent.VK_SUBTRACT, "narrowerKeypad", () -> changeFieldOfView(fieldOfView - FIELD_OF_VIEW_STEP));
	}

	private static void bindKey(JComponent component, int keyCode, String name, Runnable action) {
		bindKey(component, KeyStroke.getKeyStroke(keyCode, 0), name, action);
	}

	private static void bindKey(JComponent component, KeyStroke key, String name, Runnable action) {
		component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
		component.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

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

	private void resizeView() {
		int width = panel.getWidth(), height = panel.getHeight();
		if (width < 1 || height < 1) return;
		if (width == guiView.getViewWidth() && height == guiView.getViewHeight()) return;
		guiView.resize(width, height);
		render();
	}

	private void stopAnimations() {
		stopTour();
		if (dollyTimer != null) dollyTimer.stop();
		if (doorTimer != null) doorTimer.stop();
		resizeTimer.stop();
	}

	/**
	 * Renders an image. Before it, the light budget switches on the lights near the camera (all of them in the plan view,
	 * or when asked by the Lights menu). Every rendering of the application goes through here.
	 */
	void render() {
		applyFieldOfView();
		updateLights();
		// The halo of the lights: on demand (Lights menu), and always in the plan view, where it shows all the lights of
		// the ship (the lenses, seen from below only, cannot be seen through the ceilings)
		rContext.setLightGlow(lightHalo || cameraMode == CameraMode.PLAN);
		// The haze is for the long corridors: from above, 70 m away, it would hide the whole ship
		rContext.setAtmosphere(atmosphere.isHaze() && cameraMode != CameraMode.PLAN);
		long start = System.nanoTime();
		renderer.render();
		lastRenderNanos = System.nanoTime() - start;
		if (frame == null) return; // Without a window (tests)
		long millis = Math.round(lastRenderNanos / 1e6);
		String mode = cameraMode.label + (cameraMode == CameraMode.VISIT && tourPaused ? " (paused)" : "");
		String title = String.format(Locale.ROOT, "%s - %s, %.0f\u00b0 - %d ms per image (%.1f per second) - %d lights on", TITLE, mode, fieldOfView,
				millis, 1e9 / Math.max(lastRenderNanos, 1), lightsOn);
		if (cameraMode == CameraMode.VISIT && lapCount > 0) {
			title += String.format(Locale.ROOT, " - lap %d: %.1f ms on average", lapCount, lastLapAverage);
		}
		frame.setTitle(title);
	}

	/** Switches on the lights for the next image: the budget around the eye, or all of them. */
	void updateLights() {
		if (allLightsOn || cameraMode == CameraMode.PLAN) {
			budget.enableAll();
			lightsOn = budget.getNbLights();
		} else {
			lightsOn = budget.update(camera.getEye(), lookTarget);
		}
	}

	// ****************************************************************************************************************
	// ***** Menus
	// ****************************************************************************************************************

	private JMenu createCameraMenu() {
		JMenu menu = new JMenu("Camera");
		menu.setMnemonic(KeyEvent.VK_C);
		ButtonGroup group = new ButtonGroup();
		addCameraItem(menu, group, CameraMode.WALK, KeyEvent.VK_W);
		addCameraItem(menu, group, CameraMode.VISIT, KeyEvent.VK_V);
		addCameraItem(menu, group, CameraMode.PLAN, KeyEvent.VK_P);
		menu.addSeparator();
		addMenuItem(menu, "Pause / resume the visit", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), this::pauseTour);
		addMenuItem(menu, "Reset camera", KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), this::resetCamera);
		menu.addSeparator();
		menu.add(createFieldOfViewMenu());
		menu.addSeparator();
		menu.add(statistics.createMenuItem());
		return menu;
	}

	/**
	 * Field of view submenu: the presets (an exclusive choice), and a wider or narrower view by FIELD_OF_VIEW_STEP (keys
	 * + and -, on the main keyboard or the numeric keypad). A value between two presets selects none of them.
	 */
	private JMenu createFieldOfViewMenu() {
		JMenu menu = new JMenu("Field of view");
		ButtonGroup group = fieldOfViewGroup;
		for (float preset : FIELD_OF_VIEW_PRESETS) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.format(Locale.ROOT, "%.0f\u00b0%s", preset,
					preset == DEFAULT_FIELD_OF_VIEW ? " (default)" : preset < 60 ? " (narrow)" : preset > 90 ? " (wide)" : ""), preset == fieldOfView);
			item.addActionListener(e -> changeFieldOfView(preset));
			group.add(item);
			menu.add(item);
			fieldOfViewItems.put(preset, item);
		}
		menu.addSeparator();
		addMenuItem(menu, "Wider (+)", null, () -> changeFieldOfView(fieldOfView + FIELD_OF_VIEW_STEP));
		addMenuItem(menu, "Narrower (-)", null, () -> changeFieldOfView(fieldOfView - FIELD_OF_VIEW_STEP));
		return menu;
	}

	/** Sets the field of view, shows it in the menu and renders. */
	private void changeFieldOfView(float degrees) {
		setFieldOfView(degrees);
		// The preset matching the new value is selected, or none (a ButtonGroup can be cleared)
		JRadioButtonMenuItem item = fieldOfViewItems.get(fieldOfView);
		if (item != null) item.setSelected(true);
		else fieldOfViewGroup.clearSelection();
		render();
	}

	private void addCameraItem(JMenu menu, ButtonGroup group, CameraMode mode, int keyCode) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(mode.label, cameraMode == mode);
		item.setAccelerator(KeyStroke.getKeyStroke(keyCode, 0));
		item.addActionListener(e -> setCameraMode(mode));
		group.add(item);
		menu.add(item);
		cameraItems.put(mode, item);
	}

	/**
	 * Lights menu: how many lights the budget keeps on (an exclusive choice), or all of them; their halo; and the north
	 * door, which can be opened and closed.
	 */
	private JMenu createLightsMenu() {
		JMenu menu = new JMenu("Lights");
		menu.setMnemonic(KeyEvent.VK_L);
		ButtonGroup group = new ButtonGroup();
		for (int size : BUDGET_SIZES) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(size + (size == 1 ? " light" : " lights") + " on, the nearest",
					!allLightsOn && budget.getMaxLights() == size);
			item.addActionListener(e -> {
				allLightsOn = false;
				budget.setMaxLights(size);
				render();
			});
			group.add(item);
			menu.add(item);
		}
		JRadioButtonMenuItem all = new JRadioButtonMenuItem("All the lights on (" + budget.getNbLights() + ")", allLightsOn);
		all.addActionListener(e -> {
			allLightsOn = true;
			render();
		});
		group.add(all);
		menu.add(all);
		menu.addSeparator();
		glowItem = addEffectItem(menu, "Visible lights (halo)", lightHalo, on -> lightHalo = on); // See render()
		menu.addSeparator();
		addMenuItem(menu, "Open / close the north door", KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), this::toggleNorthDoor);
		return menu;
	}

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

	private JMenu createEffectsMenu() {
		JMenu menu = new JMenu("Effects");
		menu.setMnemonic(KeyEvent.VK_E);
		textureItem = addEffectItem(menu, "Textures", rContext.isTextureProcessing(), on -> rContext.setTextureProcessing(on));
		mipmappingItem = addEffectItem(menu, "Mipmapping", rContext.isMipmapping(), on -> rContext.setMipmapping(on));
		specularItem = addEffectItem(menu, "Specular highlights", lighting.hasSpecular(), on -> lighting.setSpecularLight(on));
		menu.addSeparator();
		shadowsItem = addEffectItem(menu, "Shadows", rContext.isShadowing(), on -> rContext.setShadowing(on));
		softShadowsItem = addEffectItem(menu, "Soft shadows", true, on -> {
			for (ShadowingLight light : lights.getLights()) light.setShadowFilter(on ? ShadowFilter.PCF_3X3 : ShadowFilter.HARD);
		});
		menu.addSeparator();
		hazeItem = addEffectItem(menu, "Haze", atmosphere.isHaze(), on -> atmosphere.setHaze(on)); // See render()
		emissionItem = addEffectItem(menu, "Glowing screens and lamps", emission.isOn(), on -> emission.setOn(on));
		return menu;
	}

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

	/** Grays out the effects the current rendering type does not use (see TempleRuins). */
	private void updateEffectsMenu() {
		if (textureItem == null) return; // The Lights menu is created first
		RenderingType type = rContext.getRenderingType();
		boolean lit = type == RenderingType.FLAT || type == RenderingType.INTERPOLATE;
		textureItem.setEnabled(lit || type == RenderingType.UNLIT);
		mipmappingItem.setEnabled(textureItem.isEnabled() && textureItem.isSelected());
		specularItem.setEnabled(lit);
		shadowsItem.setEnabled(lit);
		softShadowsItem.setEnabled(lit && shadowsItem.isSelected());
		hazeItem.setEnabled(type != RenderingType.LINE);
		emissionItem.setEnabled(lit); // The emission is drawn by the lit rendering types only
		glowItem.setEnabled(type != RenderingType.LINE);
	}

	private static void addMenuItem(JMenu menu, String label, KeyStroke accelerator, Runnable action) {
		JMenuItem item = new JMenuItem(label);
		if (accelerator != null) item.setAccelerator(accelerator);
		item.addActionListener(e -> action.run());
		menu.add(item);
	}

	// ****************************************************************************************************************
	// ***** The north door
	// ****************************************************************************************************************

	/**
	 * Opens the north door if it is closed, closes it if it is open: an animation of DOOR_DURATION_MS. A door that is
	 * moving changes direction.
	 */
	private void toggleNorthDoor() {
		doorFrom = northDoor.getOpening();
		doorTo = doorTo > 0.5f ? 0 : 1;
		doorStartMillis = System.currentTimeMillis();
		if (doorTimer == null) doorTimer = new Timer(ANIMATION_FRAME_MS, e -> stepDoor());
		doorTimer.start();
	}

	/**
	 * One step of the door: its leaves move (setOpening() changes their Translation), so the shadow maps of the lights
	 * that can see it are no longer right. Their lights keep their maps from one image to the next (static shadow
	 * casters): they must be told to draw them again (invalidateShadowMap()). Only the lights near the door: the others
	 * keep theirs.
	 */
	private void stepDoor() {
		float t = Math.min(1, (System.currentTimeMillis() - doorStartMillis) / (float) DOOR_DURATION_MS);
		float eased = t * t * (3 - 2 * t); // Starts and stops slowly
		northDoor.setOpening(doorFrom + (doorTo - doorFrom) * eased);
		invalidateShadowsNear(northDoor.getCenter());
		if (cameraMode != CameraMode.VISIT || tourPaused) render(); // During the visit, the next image of the visit shows it
		if (t >= 1) doorTimer.stop();
	}

	/**
	 * For the tests and DocumentationImages: opens the north door by a fraction (0 closed, 1 open) at once, without
	 * animation, and tells the lights near it.
	 */
	public void setNorthDoorOpening(float opening) {
		northDoor.setOpening(opening);
		invalidateShadowsNear(northDoor.getCenter());
	}

	/** Tells the lights that can reach the given point that their shadow map must be drawn again. */
	void invalidateShadowsNear(Vector4 point) {
		for (ShadowingLight light : lights.getLights()) {
			PointLight pointLight = (PointLight) light;
			if (pointLight.getPosition().minus(point).length() < pointLight.getMaxDistance() + 1) light.invalidateShadowMap();
		}
	}

	// ****************************************************************************************************************
	// ***** Camera control
	// ****************************************************************************************************************

	/** Turns the camera after a mouse drag (radians). During the visit, a drag takes the controls. */
	private void turnCamera(float horizontalAngle, float verticalAngle) {
		if (cameraMode == CameraMode.VISIT) setCameraMode(CameraMode.WALK);
		if (cameraMode == CameraMode.WALK) {
			walkYaw -= horizontalAngle;
			walkPitch = clamp(walkPitch - verticalAngle, -MAX_ELEVATION, MAX_ELEVATION);
		} else {
			planAzimuth -= horizontalAngle;
			planElevation = clamp(planElevation + verticalAngle, 0.05f, MAX_ELEVATION);
		}
		updateCamera();
		render();
	}

	/** Walks forward or backward (positive or negative notches), or zooms the plan view, eased over a short animation. */
	private void dollyCamera(float notches) {
		if (cameraMode == CameraMode.VISIT) setCameraMode(CameraMode.WALK);
		dollyTarget += notches;
		dollyAnimationStart = dollyApplied;
		dollyAnimationStartMillis = System.currentTimeMillis();
		if (dollyTimer == null) dollyTimer = new Timer(ANIMATION_FRAME_MS, e -> stepDollyAnimation());
		if (!dollyTimer.isRunning()) dollyTimer.start();
	}

	private void stepDollyAnimation() {
		float t = Math.min(1f, (float) (System.currentTimeMillis() - dollyAnimationStartMillis) / DOLLY_ANIMATION_DURATION_MS);
		applyDolly(dollyAnimationStart + (dollyTarget - dollyAnimationStart) * t);
		render();
		if (t >= 1f) dollyTimer.stop();
	}

	/**
	 * Moves the camera to the given dolly position: the walk goes forward horizontally, in the direction of the look,
	 * and stays in the corridors and the rooms; the plan view comes closer to the ship or goes away from it.
	 */
	private void applyDolly(float dolly) {
		float step = dolly - dollyApplied;
		if (cameraMode == CameraMode.WALK) {
			Vector4 forward = lineOfSight(walkYaw, 0);
			walkEye = ship.keepInside(walkEye.plus(forward.times(step * WALK_STEP)));
		} else if (cameraMode == CameraMode.PLAN) {
			planDistance = clamp(planDistance - step * PLAN_STEP, MIN_PLAN_DISTANCE, MAX_PLAN_DISTANCE);
		}
		dollyApplied = dolly;
		updateCamera();
	}

	/** Places the Camera according to the current mode. Only the Camera moves: the World does not. */
	void updateCamera() {
		switch (cameraMode) {
		case WALK:
			lookTarget = walkEye.plus(lineOfSight(walkYaw, walkPitch));
			camera.updateCamera(walkEye, lookTarget, Vector4.zAxis());
			break;
		case VISIT:
			// During the stop in front of the north door, the look turns to the door and back (see ShipTour.Stop)
			lookTarget = stopTime >= 0 ? tour.stopTarget(stopTime) : tour.target(tourDistance);
			camera.updateCamera(tour.eye(tourDistance), lookTarget, stopTime >= 0 ? tour.stopUp(stopTime) : tour.up(tourDistance));
			break;
		case PLAN:
			Vector4 eye = PLAN_CENTER.plus(lineOfSight(planAzimuth, planElevation).times(planDistance));
			lookTarget = PLAN_CENTER;
			camera.updateCamera(eye, lookTarget, Vector4.zAxis());
			break;
		}
	}

	/**
	 * Changes the camera mode. To the walk: from where the camera is, looking the same way (from the plan view, back to
	 * where the walk was left). To the visit: it goes on from where it was left.
	 */
	void setCameraMode(CameraMode mode) {
		if (mode == cameraMode) return;
		if (dollyTimer != null && dollyTimer.isRunning()) {
			dollyTimer.stop();
			applyDolly(dollyTarget);
		}
		stopTour();
		stopTime = -1;      // A stop in progress is abandoned; the door stays as it is (key D)
		doorClosingTime = -1;
		if (mode == CameraMode.WALK && cameraMode == CameraMode.VISIT) {
			Vector4 eye = camera.getEye();
			Vector4 direction = tour.target(tourDistance).minus(eye).normalize();
			walkEye = ship.keepInside(new Vector4(eye.getX(), eye.getY(), ShipTour.EYE_HEIGHT, 1));
			walkYaw = (float) Math.atan2(direction.getY(), direction.getX());
			walkPitch = (float) Math.asin(direction.getZ());
		}
		cameraMode = mode;
		if (mode == CameraMode.VISIT) {
			tourPaused = false;
			startLap(tourDistance == 0);
			startTour();
		}
		if (cameraItems.containsKey(mode)) cameraItems.get(mode).setSelected(true);
		updateCamera();
		render();
	}

	/** Back to the start: of the walk, of the visit, or to the initial plan view. */
	private void resetCamera() {
		if (dollyTimer != null) dollyTimer.stop();
		switch (cameraMode) {
		case WALK:
			walkEye = new Vector4(WALK_START);
			walkYaw = WALK_START_YAW;
			walkPitch = 0;
			break;
		case VISIT:
			tourDistance = 0;
			stopTime = -1;
			doorClosingTime = -1;
			startLap(true);
			break;
		case PLAN:
			planAzimuth = PLAN_AZIMUTH;
			planElevation = PLAN_ELEVATION;
			planDistance = PLAN_DISTANCE;
			break;
		}
		updateCamera();
		render();
	}

	// ****************************************************************************************************************
	// ***** The visit
	// ****************************************************************************************************************

	/** Starts the chain of images of the visit, one after the other on the Swing thread (see TempleRuins.startTour()). */
	private void startTour() {
		if (tourRunning) return;
		tourRunning = true;
		tourLastStepNanos = System.nanoTime();
		scheduleTourStep(++tourRun);
	}

	private void scheduleTourStep(int run) {
		SwingUtilities.invokeLater(() -> {
			if (!tourRunning || run != tourRun) return;
			stepTour();
			scheduleTourStep(run);
		});
	}

	private void stopTour() {
		tourRunning = false;
		tourRun++;
	}

	/** One image of the visit: the camera walks on by the speed times the time elapsed (see advanceTour()). */
	void stepTour() {
		long now = System.nanoTime();
		float elapsed = (now - tourLastStepNanos) / 1e9f;
		tourLastStepNanos = now;
		advanceTour(Math.min(elapsed, VISIT_MAX_STEP_SECONDS));
		render();
		lapTimes.add(lastRenderNanos / 1e6);
		lapTimes.addDuration(elapsed);
	}

	/**
	 * Moves the visit on by the given time:
	 * <ul>
	 * <li>walking: by the speed of the visit times the time, slower near the stop (ShipTour.speedFactor());</li>
	 * <li>reaching the stop in front of the north door: the visitor stands still, the clock of the stop runs, the camera
	 *     turns to the door and the door opens (ShipTour.Stop);</li>
	 * <li>after the stop: the visitor walks on, and the door closes behind.</li>
	 * </ul>
	 * The door moves: the lights near it are told to draw their shadow map again (invalidateShadowsNear()).
	 */
	void advanceTour(float seconds) {
		ShipTour.Stop stop = tour.getNorthDoorStop();
		if (stopTime >= 0) {
			// Standing in front of the door
			stopTime += seconds;
			if (stopTime < ShipTour.Stop.DURATION) {
				moveNorthDoor(stop.doorOpening(stopTime));
				updateCamera();
				return;
			}
			stopTime = -1; // The stop is over: the visit goes on, the door closes (below)
			doorClosingTime = 0;
		}
		if (doorClosingTime >= 0) {
			doorClosingTime += seconds;
			moveNorthDoor(stop.doorClosing(doorClosingTime));
			if (doorClosingTime >= ShipTour.Stop.OPEN_SECONDS) doorClosingTime = -1;
		}
		float previous = tourDistance;
		float next = tourDistance + VISIT_SPEED * tour.speedFactor(tourDistance) * seconds;
		if (previous < stop.getDistance() && next >= stop.getDistance()) {
			// The stop is reached: stand on it
			next = stop.getDistance();
			stopTime = 0;
		}
		tourDistance = next % tour.getLength();
		if (tourDistance < previous) finishLap();
		updateCamera();
	}

	/** Moves the leaves of the north door, if they are not already there, and invalidates the shadows near it. */
	private void moveNorthDoor(float opening) {
		if (northDoor.getOpening() == opening) return;
		northDoor.setOpening(opening);
		invalidateShadowsNear(northDoor.getCenter());
	}

	private void startLap(boolean fromStart) {
		lapFromStart = fromStart;
		lapTimes.reset();
	}

	/** End of a lap of the visit: its rendering times are printed, shown in the title and in the statistics. */
	private void finishLap() {
		if (lapFromStart && lapTimes.getCount() > 0) {
			lapCount++;
			lastLapAverage = lapTimes.getAverageMillis();
			lastLapReport = "VISIT, LAP " + lapCount + " (" + guiView.getViewWidth() + " x " + guiView.getViewHeight() + " pixels, "
					+ (allLightsOn ? "all the lights on" : budget.getMaxLights() + " lights on") + ")\n" + lapTimes;
			System.out.println(lastLapReport + "\n");
		}
		startLap(true);
	}

	private void pauseTour() {
		if (cameraMode != CameraMode.VISIT) return;
		tourPaused = !tourPaused;
		if (tourPaused) {
			stopTour();
			render();
		} else {
			startTour();
		}
	}

	// ****************************************************************************************************************
	// ***** Helpers
	// ****************************************************************************************************************

	/** Unit vector of a direction given by its yaw (around Z, from the X axis) and its pitch (above the horizontal). */
	static Vector4 lineOfSight(float yaw, float pitch) {
		float horizontal = (float) Math.cos(pitch);
		return new Vector4(horizontal * (float) Math.cos(yaw), horizontal * (float) Math.sin(yaw), (float) Math.sin(pitch), 0);
	}

	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	/** For the tests and DocumentationImages: the camera at a given distance of the visit. */
	public void showVisitAt(float distance) {
		cameraMode = CameraMode.VISIT;
		tourDistance = distance;
		stopTime = -1;
		updateCamera();
	}

	/**
	 * For the tests and DocumentationImages: the walk, at the given place (kept inside the ship), looking in the given
	 * direction (degrees: yaw around Z from the X axis, pitch above the horizontal).
	 */
	public void showWalk(float x, float y, float yawDegrees, float pitchDegrees) {
		cameraMode = CameraMode.WALK;
		walkEye = ship.keepInside(new Vector4(x, y, ShipTour.EYE_HEIGHT, 1));
		walkYaw = (float) Math.toRadians(yawDegrees);
		walkPitch = (float) Math.toRadians(pitchDegrees);
		updateCamera();
	}

	/** For the tests and DocumentationImages: the plan view. */
	public void showPlan() {
		cameraMode = CameraMode.PLAN;
		updateCamera();
	}

	/**
	 * For the tests and DocumentationImages: renders into the given view (an ImageView, without a window), of any size.
	 * The field of view is kept.
	 */
	public void setView(GUIView view) {
		guiView = view;
		renderer.setView(view);
	}

	/** Renders an image with the current camera (the budget switches the lights first), without a window. */
	public void renderImage() {
		render();
	}

	public RenderEngine getRenderer() {
		return renderer;
	}

	public ShipTour getTour() {
		return tour;
	}
}
