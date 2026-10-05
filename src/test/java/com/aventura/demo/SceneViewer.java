package com.aventura.demo;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.ShadowingLight;
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
 * Shows any DemoScene in a Swing window, with the camera moved by the mouse. The scenes are the ones of
 * DocumentationImages.scenes() (the images of the documentation), so a scene added there is also available here,
 * with no code to write.
 *
 *     java -cp target/classes:target/test-classes com.aventura.demo.SceneViewer [scene]
 *
 * Run it from the project root (textures are read from resources/texture). The scene is given by its name
 * (urbanscape_street, fractal_landscape... the image file name without its extension); without argument, the
 * first one is shown, and the Scene menu switches between them.
 *
 * - Mouse drag: turns the camera around the point it looks at (left/right: around the vertical axis, up/down:
 *   higher or lower, up to 89 degrees). Mouse wheel: closer or farther.
 * - Rendering menu: the rendering types, and the shadows, soft shadows, textures and landmarks options (initially as
 *   the scene defines them). Soft shadows sets the filter of the shadow map of every light that casts shadows
 *   (ShadowFilter.PCF_3X3, or HARD when unchecked); it only shows when Shadows is on. Light glow makes the lights visible
 *   (a halo around the Point and Spot lights, hidden by the objects in front of them, a disc and a halo for the sun). Light shafts adds
 *   the light shafts of the sun (checked at the start if the scene's sun has some; it only shows when Light glow is on and the sun is in the field of view). View menu: back to the scene's camera (Ctrl+R), save the image shown (Ctrl+S).
 *
 * Threads: the Swing thread (EDT) only records what the user asks for, in an immutable ViewState, and requests a
 * rendering. The rendering (and the loading of a scene, which may take a moment) is done by a single background
 * thread, which owns the scene, the camera and the RenderEngine, and always renders the LATEST state: requests
 * made while an image is being rendered collapse into one more rendering. The window stays responsive, and a
 * slow scene just shows fewer images while the mouse moves. The SwingView repaints the window after each image,
 * as in the other applications.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class SceneViewer {

	// ***** Camera movement *****

	/** Mouse drag sensitivity, as in FractalLandscape_MouseMoving: the whole width turns by PI, the whole height by PI/2 */
	static final float YAW_PER_WIDTH = (float) Math.PI;
	static final float PITCH_PER_HEIGHT = (float) Math.PI / 2;
	/** The camera never goes exactly above or below the point it looks at (the "up" vector would be undefined) */
	static final float MAX_ELEVATION = (float) Math.toRadians(89);
	/** Each notch of the mouse wheel multiplies the distance to the point looked at by this factor (or divides it) */
	static final float ZOOM_STEP = 1.1f;

	/**
	 * Position of the camera turned around the point it looks at: the vector poi -> eye is expressed in spherical
	 * coordinates (distance, azimuth around Z, elevation above the horizontal plane), then the azimuth is increased
	 * by yaw, the elevation by pitch (limited to +/- MAX_ELEVATION) and the distance multiplied by zoom.
	 *
	 * @param eye   the initial position of the camera
	 * @param poi   the point it looks at
	 * @param yaw   rotation around the vertical axis passing through poi, in radians
	 * @param pitch change of elevation, in radians (positive: higher)
	 * @param zoom  factor applied to the distance between the camera and poi
	 * @return the new position of the camera (a point: w = 1)
	 */
	static Vector4 orbitEye(Vector4 eye, Vector4 poi, float yaw, float pitch, float zoom) {
		double dx = eye.getX() - poi.getX(), dy = eye.getY() - poi.getY(), dz = eye.getZ() - poi.getZ();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double azimuth = Math.atan2(dy, dx) + yaw;
		double elevation = Math.asin(dz / distance) + pitch;
		elevation = Math.max(-MAX_ELEVATION, Math.min(MAX_ELEVATION, elevation));
		distance *= zoom;
		return new Vector4(
				(float) (poi.getX() + distance * Math.cos(elevation) * Math.cos(azimuth)),
				(float) (poi.getY() + distance * Math.cos(elevation) * Math.sin(azimuth)),
				(float) (poi.getZ() + distance * Math.sin(elevation)),
				1);
	}

	/**
	 * The accumulated pitch, limited so that the elevation stays within +/- MAX_ELEVATION (orbitEye() limits the
	 * elevation anyway, but without this, dragging far beyond the limit would have to be undone before the camera
	 * comes back).
	 *
	 * @param pitch      accumulated pitch requested, in radians
	 * @param elevation0 elevation of the scene's own camera, in radians
	 */
	static float clampPitch(float pitch, double elevation0) {
		return (float) Math.max(-MAX_ELEVATION - elevation0, Math.min(MAX_ELEVATION - elevation0, pitch));
	}

	/** Elevation of eye above the horizontal plane of poi, in radians */
	static double elevation(Vector4 eye, Vector4 poi) {
		return Math.asin((eye.getZ() - poi.getZ()) / eye.minus(poi).V3().length());
	}

	/** Distance factor for a number of wheel notches (positive: farther, negative: closer) */
	static float zoomFactor(int notches) {
		return (float) Math.pow(ZOOM_STEP, notches);
	}

	/** Name of a scene for its image file name: the name without its extension */
	static String sceneName(String imageFileName) {
		int dot = imageFileName.lastIndexOf('.');
		return dot < 0 ? imageFileName : imageFileName.substring(0, dot);
	}

	/**
	 * What the user asked for: the scene, the camera movement and the rendering options. Immutable: the EDT replaces
	 * it, the rendering thread reads the latest one. A null option means "as the scene defines it" (not loaded yet).
	 */
	static final class ViewState {
		final String scene;
		final float yaw, pitch;
		final int zoomNotches;
		final RenderingType type;
		final Boolean shadows, textures, landmarks;
		/** Soft shadows (ShadowFilter.PCF_3X3 on every shadowing light), null = as the scene's lights define them */
		final Boolean softShadows;
		/** Visible lights (RenderContext.setLightGlow()), null = as the scene defines it */
		final Boolean lightGlow;
		/** Light shafts of the sun (RenderContext.setLightShafts()), null = as the scene's lights define them */
		final Boolean lightShafts;

		ViewState(String scene, float yaw, float pitch, int zoomNotches, RenderingType type, Boolean shadows, Boolean textures, Boolean landmarks) {
			this(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, null);
		}

		ViewState(String scene, float yaw, float pitch, int zoomNotches, RenderingType type, Boolean shadows, Boolean textures, Boolean landmarks,
				Boolean softShadows) {
			this(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, null);
		}

		ViewState(String scene, float yaw, float pitch, int zoomNotches, RenderingType type, Boolean shadows, Boolean textures, Boolean landmarks,
				Boolean softShadows, Boolean lightGlow) {
			this(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, lightGlow, null);
		}

		ViewState(String scene, float yaw, float pitch, int zoomNotches, RenderingType type, Boolean shadows, Boolean textures, Boolean landmarks,
				Boolean softShadows, Boolean lightGlow, Boolean lightShafts) {
			this.scene = scene;
			this.yaw = yaw;
			this.pitch = pitch;
			this.zoomNotches = zoomNotches;
			this.type = type;
			this.shadows = shadows;
			this.textures = textures;
			this.landmarks = landmarks;
			this.softShadows = softShadows;
			this.lightGlow = lightGlow;
			this.lightShafts = lightShafts;
		}

		/** A scene seen from its own camera, with its own options */
		static ViewState of(String scene) {
			return new ViewState(scene, 0, 0, 0, null, null, null, null);
		}

		ViewState withCamera(float yaw, float pitch, int zoomNotches) {
			return new ViewState(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, lightGlow, lightShafts);
		}

		ViewState withOptions(RenderingType type, Boolean shadows, Boolean textures, Boolean landmarks) {
			return new ViewState(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, lightGlow, lightShafts);
		}

		ViewState withSoftShadows(Boolean softShadows) {
			return new ViewState(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, lightGlow, lightShafts);
		}

		/** The soft shadows option, if still undefined, takes the given (scene's) value */
		ViewState withSoftShadowsDefault(boolean softShadows) {
			return this.softShadows != null ? this : withSoftShadows(softShadows);
		}

		ViewState withLightGlow(Boolean lightGlow) {
			return new ViewState(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, lightGlow, lightShafts);
		}

		/** The light glow option, if still undefined, takes the given (scene's) value */
		ViewState withLightGlowDefault(boolean lightGlow) {
			return this.lightGlow != null ? this : withLightGlow(lightGlow);
		}

		ViewState withLightShafts(Boolean lightShafts) {
			return new ViewState(scene, yaw, pitch, zoomNotches, type, shadows, textures, landmarks, softShadows, lightGlow, lightShafts);
		}

		/** The light shafts option, if still undefined, takes the given (scene's) value */
		ViewState withLightShaftsDefault(boolean lightShafts) {
			return this.lightShafts != null ? this : withLightShafts(lightShafts);
		}

		/** The options still undefined take the given (scene's) values */
		ViewState withDefaults(RenderingType type, boolean shadows, boolean textures, boolean landmarks) {
			return withOptions(this.type != null ? this.type : type, this.shadows != null ? this.shadows : shadows,
					this.textures != null ? this.textures : textures, this.landmarks != null ? this.landmarks : landmarks);
		}
	}

	// ***** The viewer *****

	private static final String TITLE = "Aventura scenes";
	private static final String[] TYPE_LABELS = { "Lines", "Monochrome", "Unlit", "Flat", "Shading" };

	private final Map<String, Supplier<DemoScene>> scenes; // by scene name
	private final AtomicReference<ViewState> state;

	// Rendering thread: one daemon thread, and a flag collapsing the requests made while it renders
	private final ExecutorService renderThread = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "SceneViewer rendering");
		t.setDaemon(true);
		return t;
	});
	private final AtomicBoolean renderRequested = new AtomicBoolean(false);

	// Owned by the rendering thread
	private String loadedScene = null;
	private DemoScene scene;
	private RenderContext sceneDefaults; // copy of the scene's own options, before any change
	private boolean sceneLightShafts; // whether the scene's own sun has light shafts
	private boolean sceneSoftShadows; // whether the scene's own lights filter their shadows (all of them, at least one)
	private Camera camera;
	private RenderEngine engine;
	// Written by the rendering thread, read by the EDT (paint, save, pitch limit)
	private volatile SwingView view;
	private volatile double sceneElevation = 0;

	// Swing components (EDT)
	private JFrame frame;
	private JPanel panel;
	private final Map<String, JRadioButtonMenuItem> sceneItems = new LinkedHashMap<>();
	private final Map<RenderingType, JRadioButtonMenuItem> typeItems = new LinkedHashMap<>();
	private JCheckBoxMenuItem shadowsItem, softShadowsItem, texturesItem, landmarksItem, lightGlowItem, lightShaftsItem;
	private int lastX, lastY;

	public SceneViewer(Map<String, Supplier<DemoScene>> scenesByImageName, String firstScene) {
		this.scenes = new LinkedHashMap<>();
		scenesByImageName.forEach((image, scene) -> scenes.put(sceneName(image), scene));
		if (!scenes.containsKey(firstScene)) {
			throw new IllegalArgumentException("Unknown scene: " + firstScene + ", known scenes: " + scenes.keySet());
		}
		this.state = new AtomicReference<>(ViewState.of(firstScene));
	}

	public Map<String, Supplier<DemoScene>> getScenes() {
		return scenes;
	}

	/** Creates and shows the window, then renders the first scene. Must be called on the EDT. */
	public void show() {
		frame = new JFrame(TITLE);
		frame.setJMenuBar(createMenus());

		panel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				SwingView v = view;
				if (v != null && v.getImageView() != null) {
					g.drawImage(v.getImageView(), 0, 0, null);
				}
			}
		};
		panel.setPreferredSize(new Dimension(800, 450));
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				lastX = e.getX();
				lastY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// Displacement since the previous event (not since the click), as in FractalLandscape_MouseMoving
				float yaw = -FractalLandscape_MouseMoving.dragAngle(e.getX() - lastX, panel.getWidth(), YAW_PER_WIDTH);
				float pitch = FractalLandscape_MouseMoving.dragAngle(e.getY() - lastY, panel.getHeight(), PITCH_PER_HEIGHT);
				lastX = e.getX();
				lastY = e.getY();
				double elevation0 = sceneElevation;
				update(s -> s.withCamera(s.yaw + yaw, clampPitch(s.pitch + pitch, elevation0), s.zoomNotches));
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				update(s -> s.withCamera(s.yaw, s.pitch, s.zoomNotches + e.getWheelRotation()));
			}
		};
		panel.addMouseListener(mouse);
		panel.addMouseMotionListener(mouse);
		panel.addMouseWheelListener(mouse);

		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				renderThread.shutdownNow();
			}
		});
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		syncMenus();
		requestRender();
	}

	private JMenuBar createMenus() {
		JMenuBar bar = new JMenuBar();

		JMenu sceneMenu = new JMenu("Scene");
		ButtonGroup sceneGroup = new ButtonGroup();
		for (String name : scenes.keySet()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
			// Also for the scene shown: back to its own camera and options
			item.addActionListener(e -> update(s -> ViewState.of(name)));
			sceneGroup.add(item);
			sceneMenu.add(item);
			sceneItems.put(name, item);
		}
		bar.add(sceneMenu);

		JMenu renderingMenu = new JMenu("Rendering");
		ButtonGroup typeGroup = new ButtonGroup();
		RenderingType[] types = RenderingType.values();
		for (int i = 0; i < types.length; i++) {
			RenderingType type = types[i];
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(i < TYPE_LABELS.length ? TYPE_LABELS[i] : type.name());
			item.addActionListener(e -> update(s -> s.withOptions(type, s.shadows, s.textures, s.landmarks)));
			typeGroup.add(item);
			renderingMenu.add(item);
			typeItems.put(type, item);
		}
		renderingMenu.addSeparator();
		shadowsItem = new JCheckBoxMenuItem("Shadows");
		shadowsItem.addActionListener(e -> update(s -> s.withOptions(s.type, shadowsItem.isSelected(), s.textures, s.landmarks)));
		softShadowsItem = new JCheckBoxMenuItem("Soft shadows");
		softShadowsItem.addActionListener(e -> update(s -> s.withSoftShadows(softShadowsItem.isSelected())));
		texturesItem = new JCheckBoxMenuItem("Textures");
		texturesItem.addActionListener(e -> update(s -> s.withOptions(s.type, s.shadows, texturesItem.isSelected(), s.landmarks)));
		landmarksItem = new JCheckBoxMenuItem("Landmarks");
		landmarksItem.addActionListener(e -> update(s -> s.withOptions(s.type, s.shadows, s.textures, landmarksItem.isSelected())));
		renderingMenu.add(shadowsItem);
		renderingMenu.add(softShadowsItem);
		renderingMenu.add(texturesItem);
		lightGlowItem = new JCheckBoxMenuItem("Light glow");
		lightGlowItem.addActionListener(e -> update(s -> s.withLightGlow(lightGlowItem.isSelected())));
		renderingMenu.add(landmarksItem);
		renderingMenu.add(lightGlowItem);
		lightShaftsItem = new JCheckBoxMenuItem("Light shafts");
		lightShaftsItem.addActionListener(e -> update(s -> s.withLightShafts(lightShaftsItem.isSelected())));
		renderingMenu.add(lightShaftsItem);
		bar.add(renderingMenu);

		JMenu viewMenu = new JMenu("View");
		JMenuItem reset = new JMenuItem("Reset camera");
		reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
		reset.addActionListener(e -> update(s -> s.withCamera(0, 0, 0)));
		JMenuItem save = new JMenuItem("Save image...");
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		save.addActionListener(e -> saveImage());
		viewMenu.add(reset);
		viewMenu.add(save);
		bar.add(viewMenu);

		return bar;
	}

	/** Updates the menus from the current state (EDT) */
	private void syncMenus() {
		ViewState s = state.get();
		JRadioButtonMenuItem sceneItem = sceneItems.get(s.scene);
		if (sceneItem != null) sceneItem.setSelected(true);
		if (s.type != null) typeItems.get(s.type).setSelected(true);
		if (s.shadows != null) shadowsItem.setSelected(s.shadows);
		if (s.softShadows != null) softShadowsItem.setSelected(s.softShadows);
		if (s.textures != null) texturesItem.setSelected(s.textures);
		if (s.landmarks != null) landmarksItem.setSelected(s.landmarks);
		if (s.lightGlow != null) lightGlowItem.setSelected(s.lightGlow);
		if (s.lightShafts != null) lightShaftsItem.setSelected(s.lightShafts);
	}

	/** Changes the state (EDT) and requests a rendering */
	private void update(UnaryOperator<ViewState> change) {
		state.updateAndGet(change);
		requestRender();
	}

	/** Schedules a rendering of the latest state, unless one is already scheduled and not started yet */
	private void requestRender() {
		if (renderRequested.compareAndSet(false, true)) {
			renderThread.execute(() -> {
				renderRequested.set(false); // requests made from now on schedule one more rendering
				renderLatest();
			});
		}
	}

	/** Rendering thread: loads the scene if it changed, applies the latest state and renders */
	private void renderLatest() {
		try {
			String name = state.get().scene;
			if (!name.equals(loadedScene)) {
				load(name);
			}
			// The options not chosen by the user are the scene's own ones
			ViewState s = state.updateAndGet(st -> st.scene.equals(loadedScene)
					? st.withDefaults(sceneDefaults.getRenderingType(), sceneDefaults.isShadowing(), sceneDefaults.isTextureProcessing(), sceneDefaults.isDisplayLandmark())
							.withSoftShadowsDefault(sceneSoftShadows)
							.withLightGlowDefault(sceneDefaults.isLightGlow())
							.withLightShaftsDefault(sceneLightShafts)
					: st);
			if (!s.scene.equals(loadedScene)) {
				return; // another scene was chosen meanwhile: it has already been requested
			}
			SwingUtilities.invokeLater(this::syncMenus);
			RenderContext options = scene.getRenderContext();
			options.setRenderingType(s.type);
			options.setShadowing(s.shadows);
			options.setTextureProcessing(s.textures);
			options.setDisplayLandmark(s.landmarks);
			options.setLightGlow(s.lightGlow);
			options.setLightShafts(s.lightShafts);
			applySoftShadows(scene.getLighting(), s.softShadows);
			Vector4 poi = scene.getPoi();
			camera.updateCamera(orbitEye(scene.getEye(), poi, s.yaw, s.pitch, zoomFactor(s.zoomNotches)), poi, Vector4.zAxis());

			long start = System.currentTimeMillis();
			engine.render(); // the SwingView repaints the panel
			long duration = System.currentTimeMillis() - start;
			SwingUtilities.invokeLater(() -> frame.setTitle(TITLE + " - " + name + " - " + duration + " ms per image"));
		} catch (RuntimeException e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, e.toString(), "Rendering failed", JOptionPane.ERROR_MESSAGE));
		}
	}

	/** True if the lighting has at least one light casting shadows, and every one of them filters its shadow map (PCF_3X3 or more) */
	static boolean hasSoftShadows(Lighting lighting) {
		List<ShadowingLight> lights = lighting == null ? null : lighting.getShadowingLights();
		if (lights == null || lights.isEmpty()) {
			return false;
		}
		for (ShadowingLight light : lights) {
			if (light.getShadowFilter() == ShadowFilter.HARD) {
				return false;
			}
		}
		return true;
	}

	/** True if at least one directional light of the scene asks for light shafts in its appearance (LightAppearance.setShaftsGain()) */
	static boolean hasLightShafts(Lighting lighting) {
		if (lighting == null) {
			return false;
		}
		for (DirectionalLight light : lighting.getDirectionalLights()) {
			if (light.getAppearance() != null && light.getAppearance().getShaftsGain() > 0f) {
				return true;
			}
		}
		return false;
	}

	/** Sets the shadow filter of every light casting shadows: soft (PCF_3X3) or hard. Does nothing for a null option. */
	static void applySoftShadows(Lighting lighting, Boolean soft) {
		if (soft == null || lighting == null || lighting.getShadowingLights() == null) {
			return;
		}
		for (ShadowingLight light : lighting.getShadowingLights()) {
			light.setShadowFilter(soft ? ShadowFilter.PCF_3X3 : ShadowFilter.HARD);
		}
	}

	/** Rendering thread: creates the scene, its camera, a SwingView of its size and the engine */
	private void load(String name) {
		SwingUtilities.invokeLater(() -> frame.setTitle(TITLE + " - loading " + name + "..."));
		DemoScene newScene = scenes.get(name).get();
		SwingView newView = new SwingView(newScene.getPerspective(), panel);
		scene = newScene;
		camera = newScene.createCamera();
		engine = newScene.createEngine(camera, newView);
		sceneDefaults = new RenderContext(newScene.getRenderContext());
		sceneSoftShadows = hasSoftShadows(newScene.getLighting());
		sceneLightShafts = hasLightShafts(newScene.getLighting());
		loadedScene = name;
		sceneElevation = elevation(newScene.getEye(), newScene.getPoi());
		view = newView;
		int width = newScene.getPerspective().getPixelWidth(), height = newScene.getPerspective().getPixelHeight();
		SwingUtilities.invokeLater(() -> {
			// The window takes the size of the new scene's image (the layout must be invalidated for pack() to see it)
			panel.setPreferredSize(new Dimension(width, height));
			panel.invalidate();
			frame.pack();
			frame.setLocationRelativeTo(null); // centered again, so that a larger window stays on the screen
		});
	}

	/** Saves the image shown (EDT) */
	private void saveImage() {
		SwingView v = view;
		BufferedImage image = v != null ? v.getImageView() : null;
		if (image == null) return;
		JFileChooser chooser = new JFileChooser(new File("."));
		chooser.setSelectedFile(new File(state.get().scene + ".png"));
		if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			try {
				DocumentationImages.save(image, chooser.getSelectedFile());
			} catch (Exception e) {
				JOptionPane.showMessageDialog(frame, e.toString(), "Save failed", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * @param args [scene]: the name of the first scene shown (default: the first one)
	 */
	public static void main(String[] args) {
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("SceneViewer needs a display (run it without -Djava.awt.headless=true).");
			return;
		}
		Map<String, Supplier<DemoScene>> scenes = DocumentationImages.scenes();
		String first = args.length > 0 ? args[0] : sceneName(scenes.keySet().iterator().next());
		SceneViewer viewer = new SceneViewer(scenes, first);
		SwingUtilities.invokeLater(viewer::show);
	}
}
