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
import java.awt.Toolkit;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.event.*;

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
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.model.perspective.PerspectiveType;

/**
 * This class is a Test class demonstrating usage of the API of the Aventura rendering engine
 * 
 * It generates a landscape through a Treillis material formed through a Fractal recursion with random variations to create the ground
 * Goal is to progressively build a complete application on a typical 3D graphic use case
 * 
 * Two camera modes (Landscape menu), with the same mouse gestures:
 * - Orbit camera (default), to look at the landscape from outside, as most 3D / terrain viewers:
 *   - Mouse drag:  orbit the camera around the point of interest (turntable: horizontal drag turns around
 *                  the vertical Z axis, vertical drag changes the elevation, clamped below the poles)
 *   - Mouse wheel (or Up / Down arrow keys): zoom (dolly the camera toward / away from the point of interest)
 * - Fly camera, to fly inside the landscape like a plane, starting from where the orbit camera was:
 *   - Mouse drag:  look around (horizontal drag turns left / right, vertical drag looks up / down, clamped below
 *                  the vertical)
 *   - Mouse wheel (or Up / Down arrow keys): fly forward / backward along the line of sight; the camera stays
 *                  above the ground and the sea
 *   Going back to the orbit camera puts the camera back on its orbit, where it was.
 * On a touchpad, the two finger vertical scroll and the pinch both act as the mouse wheel: the pinch is sent by the
 * system as CTRL + mouse wheel (the zoom gesture of the browsers, office or map applications), so CTRL + mouse wheel
 * is the same as the mouse wheel. The fractions of a notch sent by the touchpads are taken into account
 * (MouseWheelEvent.getPreciseWheelRotation()), for a smooth movement.
 * Common to both modes:
 * - CTRL + U / CTRL + D (or Page Up / Page Down): raise / lower the sea level (a transformation of the World: the sea
 *                       Element is translated on Z); keep the key pressed for a continuous movement
 * - Window resizing:    the rendered image always fills the window, at the same scale (PPU): a larger window shows
 *                       more of the landscape, objects keep their size on screen (PerspectiveContext.ResizeMode.KEEP_SCALE)
 * SHIFT + mouse wheel is deliberately left free: it is the usual OS / application convention for lateral scrolling,
 * hence reserved for a future camera pan.
 *
 * Menus:
 * - Landscape: generate a new landscape, raise / lower the sea level, reset the camera (to its initial orbit, or to
 *              the start of the flight), and choose the camera mode (orbit or fly)
 * - Rendering: the rendering type (exclusive choice: radio buttons)
 * - Effects:   options independent of the rendering type (check boxes), grayed out when the current rendering type
 *              does not use them: texture, specular highlights, shadows (hard or soft), and the sun with its effects
 *              (halo, lens flare, light shafts). The sun is low on the horizon, in front of the initial camera and a
 *              little to its left: orbit a little to the left and lower the camera to see it.
 * 
 * @author Olivier BARRY
 * @since Nov 2023
 */
public class FractalLandscape implements MouseListener, MouseMotionListener, MouseWheelListener {
	
	// GUI Frame and Menus
	JFrame frame;
	JPanel panel; // Displays the rendered image, fills the content area of the frame
	// Effects menu: the check boxes whose availability depends on the rendering type or on another check box (see
	// updateEffectsMenu()); their state is read from the model (RenderContext, Lighting, sun) when they are created
	JCheckBoxMenuItem textureItem, specularItem, shadowsItem, softShadowsItem, sunItem, sunHaloItem, lensFlareItem, lightShaftsItem;

	// Camera
	Vector4 eye;
	Vector4 poi;
	Camera camera;
	
	// Render engine
	PerspectiveContext pContext;
	RenderContext rContext;
	Lighting lighting;

	// Sun: a directional light, low on the horizon so that it can be seen in the sky (azimuth: angle around the
	// vertical Z axis from the X axis, elevation: angle above the horizon, both in degrees, of the direction of the sun
	// seen from the scene). The initial camera is on the X axis looking towards -X (azimuth 180): the sun is in front of
	// it, a little to the left, lighting the landscape from behind and from the side
	static final float SUN_AZIMUTH = 225f;
	static final float SUN_ELEVATION = 15f;
	DirectionalLight sun;
	LightAppearance sunAppearance; // Halo (gain) and lens flare of the sun, see the Effects menu

	// Colors
	static final Color SKY_COLOR = new Color(80, 120, 220);
	
	// Specular highlights (Effects menu): sharp and bright on the water, faint on the ground
	static final Color SEA_SPECULAR_COLOR = new Color(200, 200, 200);
	static final float SEA_SPECULAR_EXP = 200;
	static final Color LAND_SPECULAR_COLOR = new Color(60, 60, 60);
	static final float LAND_SPECULAR_EXP = 8;
	GUIView guiView;
	RenderEngine renderer;
	
	// World
	World world;
	
	// Element combining landscape and sea (parent of both, for transformations of the whole landscape; the
	// mouse no longer moves it: the camera orbits around the World instead)
	Element master_land;
	
	// Element : Trellis for landscape
	Trellis tre_land;
	float size; // Size of the Treillis (square)
	int n; // Nb of segments of the Treillis, should be a 2^n number
	float array_land[][]; // Updated array with sea level
	
	// Element : Trellis for the sea
	Trellis tre_sea;
	float sea_level = 1.5f;
	// Each key press (CTRL + U / D, Page Up / Down) moves the sea by this altitude; the sea level stays within the
	// landscape altitudes
	static final float SEA_LEVEL_STEP = 0.01f;

	// Movement variables
	int i_rotation = 0;
	int j_rotation = 0;
	// Zoom (orbit camera) or distance flown (fly camera) asked by the mouse wheel and the arrow keys, in notches of the
	// wheel (a float: a touchpad sends fractions of a notch), see moveCamera()
	float zoom = 0;
	static int NB_ROTATIONS = 90;
	
	// Mouse drag: last known position (updated on each event)
	int mouse_last_X, mouse_last_Y;
	// Drag sensitivity: dragging across the whole window width turns the camera by DRAG_ANGLE_PER_WIDTH radians
	// (azimuth, around the vertical Z axis), across the whole height by DRAG_ANGLE_PER_HEIGHT radians (elevation)
	static final float DRAG_ANGLE_PER_WIDTH = (float) Math.PI;
	static final float DRAG_ANGLE_PER_HEIGHT = (float) Math.PI / 2;

	// Orbit camera ("turntable", as in most 3D viewers): the World does not move, the camera moves on a sphere
	// centered on the point of interest (poi), described by spherical coordinates around the World's up axis (Z):
	// - azimuth: angle around Z, measured from the X axis
	// - elevation: angle above the horizontal plane, clamped to +/- MAX_ELEVATION to never reach the poles, where
	//   the forward vector would be parallel to the up vector (Z) and the LookAt matrix would be undefined
	// - distance: initial distance eye-poi, reduced by the zoom (never below MIN_ORBIT_DISTANCE, not to cross the poi)
	static final float MAX_ELEVATION = (float) Math.PI / 2 - 0.01f;
	static final float MIN_ORBIT_DISTANCE = 0.2f;
	float orbitAzimuth;
	float orbitElevation;
	float orbitDistance;

	// Fly camera (see the class comment): position of the eye, and direction of the line of sight given by two angles,
	// like a plane: yaw (heading, angle around the vertical Z axis, from the X axis) and pitch (angle above the
	// horizontal plane, clamped to +/- MAX_ELEVATION for the same reason as the orbit's elevation)
	boolean flyMode = false;
	Vector4 flyEye;
	float flyYaw;
	float flyPitch;
	// Where the flight started (for Reset camera), and the zoom of the orbit, kept while flying to go back to it
	Vector4 flyStartEye;
	float flyStartYaw, flyStartPitch;
	float orbitZoom;
	// Distance flown per mouse wheel notch (the same as one notch of the orbit's zoom), and minimum height of the
	// camera above the ground and the sea, so that it never goes under the landscape (where its "backstage" shows); it
	// must stay larger than the distance of the near plane (see run()), which would cut the ground otherwise
	static final float FLY_STEP = 0.1f;
	static final float FLY_CLEARANCE = 0.03f;

	// Window resizing: the image is resized (and rendered) only once the size has not changed for RESIZE_DELAY_MS,
	// instead of rendering a full frame for each intermediate size while the user drags the border of the window
	private static final int RESIZE_DELAY_MS = 50;
	Timer resizeTimer;

	// Zoom-wheel smoothing (Quaternion/slerp technique adapted for a translation): each mouse wheel
	// notch used to jump the camera's dolly position straight to its new value. It now eases into
	// that same target over ZOOM_ANIM_DURATION_MS via linear interpolation, driven by a Swing Timer
	// firing on the EDT (same thread as mouseWheelMoved(), so no synchronization is needed). Linear
	// interpolation is used instead of Quaternion.slerp() because zoom is a translation along the
	// camera's forward vector, not a rotation.
	private static final int ZOOM_ANIM_DURATION_MS = 150;
	private static final int ZOOM_ANIM_FRAME_MS = 15; // ~60 fps

	Timer zoomAnimTimer;
	float appliedZoom = 0f; // last applied (possibly mid-animation) zoom value
	float zoomAnimStartValue;
	float zoomAnimTargetValue;
	long zoomAnimStartMillis;

	/**
	 * Create the view and associate all needed mouse and key listeners for user interaction with screen
	 * The renderer will then be called each time Mouse generates a move to the view and a new updated view will be displayed
	 * 
	 * @param context the PerspectiveContext to be used to create the GUIView
	 * @return
	 */
	public GUIView createView(PerspectiveContext context) {
		
		// Create the frame of the application 
		frame = new JFrame("Fractal Landscape");
		
		// Create Menus and associate the menu items to appropriate listeners
	    JMenuBar menubar = new JMenuBar();
	    menubar.add(createLandscapeMenu());
	    menubar.add(createRenderingMenu());
	    menubar.add(createEffectsMenu());
	    frame.setJMenuBar(menubar);
	    updateEffectsMenu();
	    bindKeys();

		
		// Create the view to be displayed
		SwingView view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
		    	super.paintComponent(graph); // Clears with the background color (visible until a resized image is rendered)
		    	graph.drawImage(view.getImageView(), 0, 0, null);
		    }
		};
		panel.setBackground(Color.BLACK);
		// Initial size of the content area = size of the image defined by the PerspectiveContext: pack() sizes the frame
		// around it (title bar, borders and menu bar added), so the image exactly fills the window from the start
		panel.setPreferredSize(new Dimension(context.getPixelWidth(), context.getPixelHeight()));
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
 
		// Locate application frame in the center of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - frame.getWidth()/2, dim.height/2 - frame.getHeight()/2);
		
		// Render the frame on the display
		frame.setVisible(true);
		
        //where initialization occurs:
        //Register for mouse events on blankArea and the panel.
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addMouseWheelListener(this);

        // Follow the size of the panel (hence of the window): restart the delay at each intermediate size
        resizeTimer = new Timer(RESIZE_DELAY_MS, e -> resizeViewport());
        resizeTimer.setRepeats(false);
        panel.addComponentListener(new ComponentAdapter() {
        	public void componentResized(ComponentEvent e) {
        		resizeTimer.restart();
        	}
        });
 		
		return view;
	}
	
	/**
	 * Keyboard shortcuts without a menu item: Up / Down arrows move the camera like the mouse wheel, Page Up / Page Down
	 * raise / lower the sea level like CTRL + U / CTRL + D (the accelerators of the Landscape menu). They are key
	 * bindings of the whole window (WHEN_IN_FOCUSED_WINDOW), so they work whatever component has the keyboard focus,
	 * unlike a KeyListener which only receives the keys when its own component has it.
	 */
	private void bindKeys() {
		JComponent root = frame.getRootPane();
		bindKey(root, KeyEvent.VK_UP, "cameraForward", () -> moveCamera(1));
		bindKey(root, KeyEvent.VK_DOWN, "cameraBackward", () -> moveCamera(-1));
		bindKey(root, KeyEvent.VK_PAGE_UP, "seaUp", () -> changeSeaLevel(1));
		bindKey(root, KeyEvent.VK_PAGE_DOWN, "seaDown", () -> changeSeaLevel(-1));
	}

	private void bindKey(JComponent component, int keyCode, String name, Runnable action) {
		component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
		component.getActionMap().put(name, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (renderer != null) action.run(); // Ignored until the renderer is created
			}
		});
	}

	public void mousePressed(MouseEvent e) {
		// Store the location when mouse is clicked: mouseDragged() accumulates the displacement from one event to the next
		mouse_last_X = e.getX(); mouse_last_Y = e.getY();
	}

     public void mouseEntered(MouseEvent e) {
    	 // Do nothing
	 }

	public void mouseReleased(MouseEvent e) {
		// Do nothing
     }

     public void mouseExited(MouseEvent e) {
 		// Do nothing
     }

     public void mouseClicked(MouseEvent e) {
 		// Do nothing
     }

 	public void mouseDragged(MouseEvent e) {
        // Use the displacement since the PREVIOUS event (not since the click): the total angle only depends on
        // the mouse movement, whatever the number of events. Swing coalesces the drag events while a (slow,
        // e.g. zoomed-in) frame is rendered: using the offset since the click at each event made the rotation
        // depend on the rendering speed, and not come back when the mouse did.
        int dx = e.getX() - mouse_last_X;
        int dy = e.getY() - mouse_last_Y;
        mouse_last_X = e.getX(); mouse_last_Y = e.getY();

        float angleX = dragAngle(dx, panel.getWidth(), DRAG_ANGLE_PER_WIDTH);
        float angleY = dragAngle(dy, panel.getHeight(), DRAG_ANGLE_PER_HEIGHT);
        // The vertical angle is clamped at each event (not on a total), so that dragging back immediately moves
        // the camera again, without a dead zone after having hit the limit.
        if (flyMode) {
        	// "Look around" (like the head of the pilot): dragging to the right turns the line of sight to the right
        	// (the yaw decreases, clockwise seen from above), dragging up looks up (the screen Y axis points down)
        	flyYaw -= angleX;
        	flyPitch = clamp(flyPitch - angleY, -MAX_ELEVATION, MAX_ELEVATION);
        } else {
        	// "Grab the World" feeling: dragging to the right makes the landscape turn to the right, hence the
        	// camera turns the other way (azimuth decreases); dragging down shows the landscape more from above
        	// (elevation increases)
        	orbitAzimuth -= angleX;
        	orbitElevation = clamp(orbitElevation + angleY, -MAX_ELEVATION, MAX_ELEVATION);
        }

        // Move the camera and render the updated view (the World itself is unchanged)
        updateCameraPosition();
		renderer.render();
 	}

	/**
	 * Rotation angle (radians) for a mouse displacement (pixels): proportional to the displacement, anglePerSize radians
	 * for a displacement of the whole window size. Package-private and static so it can be unit-tested.
	 */
	static float dragAngle(int displacement, int windowSize, float anglePerSize) {
		return anglePerSize * displacement / windowSize;
	}

	/**
	 * Position of the eye of an orbit camera: the point at the given distance from the point of interest, in the
	 * direction given by the spherical angles azimuth (around the vertical Z axis, from the X axis) and elevation
	 * (above the horizontal plane).
	 *
	 * Package-private and static purely so it can be unit-tested (TestFractalLandscape) without a
	 * live Camera/RenderEngine/Swing GUIView -- this method touches none of those.
	 */
	static Vector4 orbitEye(Vector4 poi, float distance, float azimuth, float elevation) {
		float horizontal = distance * (float) Math.cos(elevation); // Projection of the eye-poi distance on the XY plane
		return new Vector4(
				poi.getX() + horizontal * (float) Math.cos(azimuth),
				poi.getY() + horizontal * (float) Math.sin(azimuth),
				poi.getZ() + distance * (float) Math.sin(elevation),
				1);
	}

	/**
	 * Unit vector of the line of sight of the fly camera, for the given yaw (heading, around the vertical Z axis, from
	 * the X axis) and pitch (above the horizontal plane), in radians: the same spherical coordinates as orbitEye(),
	 * here for a direction (w = 0) rather than a point. Package-private and static so it can be unit-tested.
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
	 * Altitude of the ground at (x, y), never below the surface actually drawn: a trellis of the given size, centered on
	 * the origin, made of n x n cells, with altitudes[i][j] at x = -size/2 + i*size/n, y = -size/2 + j*size/n. Each
	 * cell is drawn as 2 triangles, cut along one diagonal or the other (the mesh alternates them): the altitude is the
	 * higher of the two possible cuts, each one a planar interpolation in the triangle containing the point, so that
	 * the camera kept above it is above the drawn triangles whatever the cut. NaN outside of the trellis (no ground).
	 * Package-private and static so it can be unit-tested.
	 */
	static float groundAltitude(float[][] altitudes, float size, int n, float x, float y) {
		// Position in cells from the corner of the trellis
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

	/**
	 * Restricts a value to the [min, max] interval. Package-private and static so it can be unit-tested.
	 */
	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Places the camera according to the current mode, always with the World's up axis (Z) as up vector:
	 * - orbit: according to the orbit angles and the current (possibly mid-animation) zoom, looking at the point of
	 *   interest
	 * - fly: at the position of the fly camera, looking along its line of sight (yaw, pitch)
	 */
	private void updateCameraPosition() {
		if (flyMode) {
			camera.updateCamera(flyEye, flyEye.plus(flyDirection(flyYaw, flyPitch)), Vector4.zAxis());
		} else {
			float distance = orbitDistance - appliedZoom / 10;
			camera.updateCamera(orbitEye(poi, distance, orbitAzimuth, orbitElevation), poi, Vector4.zAxis());
		}
	}

	/** Moves the fly camera by the given distance along its line of sight (backward if negative), above the ground. */
	private void flyForward(float distance) {
		flyEye = flyEye.plus(flyDirection(flyYaw, flyPitch).times(distance));
		keepAboveGround();
	}

	/**
	 * Keeps the fly camera at least FLY_CLEARANCE above the ground and the sea (the sea covers the same square as the
	 * landscape): the camera slides on them instead of going under. The ground is taken under the camera and around
	 * it (at FLY_CLEARANCE on each side): on a steep slope, the ground rising beside the camera would otherwise come
	 * closer than the near plane, which would cut it. Outside of the landscape there is nothing to keep above.
	 */
	private void keepAboveGround() {
		float x = flyEye.getX(), y = flyEye.getY(), r = FLY_CLEARANCE;
		float ground = Float.NaN;
		float[][] samples = { { x, y }, { x - r, y }, { x + r, y }, { x, y - r }, { x, y + r } };
		for (float[] sample : samples) {
			float altitude = groundAltitude(array_land, size, n, sample[0], sample[1]);
			if (Float.isNaN(altitude)) continue; // Outside of the landscape
			if (Float.isNaN(ground) || altitude > ground) ground = altitude; // The highest
		}
		if (Float.isNaN(ground)) return;
		float floor = Math.max(ground, sea_level) + FLY_CLEARANCE;
		if (flyEye.getZ() < floor) flyEye.setZ(floor);
	}

 	public void mouseMoved(MouseEvent e) {
		// Do nothing
 	}

	/**
	 * Mouse wheel, touchpad two finger scroll and touchpad pinch (sent as CTRL + wheel, hence not distinguished): wheel
	 * up (negative rotation) moves the camera forward. getPreciseWheelRotation() rather than getWheelRotation(): a
	 * touchpad sends fractions of a notch, which getWheelRotation() rounds to 0 (no movement) until a whole notch is
	 * reached; a mouse wheel sends whole notches, so it moves exactly as before.
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		moveCamera((float) -e.getPreciseWheelRotation());
	}

	/**
	 * Moves the camera forward (positive notches) or backward (negative notches), with an easing (see
	 * startZoomAnimation()): one notch of the wheel, or one press of an arrow key, is 1.
	 * - Orbit camera: zoom, 1/10 of a unit per notch, without letting the camera reach (or cross) the point of interest
	 * - Fly camera: flight along the line of sight, FLY_STEP per notch, without limit
	 */
	private void moveCamera(float notches) {
		if (flyMode) {
			zoom += notches;
		} else {
			float maxZoom = (orbitDistance - MIN_ORBIT_DISTANCE) * 10; // zoom/10 is the distance travelled toward the poi
			zoom = Math.min(zoom + notches, maxZoom);
		}
		startZoomAnimation(zoom);
	}

	/**
	 * Raises (positive steps) or lowers (negative steps) the sea level by SEA_LEVEL_STEP per step, within the
	 * altitudes of the landscape. This is a transformation of the World, not of the view: the sea Element (tre_sea)
	 * is translated on the Z axis; its geometry is unchanged, the RenderEngine applies the new transformation at the
	 * next rendering, so neither a rebuild of the World nor a new calculation of the normals is needed.
	 */
	private void changeSeaLevel(int steps) {
		sea_level = clamp(sea_level + steps * SEA_LEVEL_STEP, tre_land.getMinZ(), tre_land.getMaxZ());
		tre_sea.setTransformation(new Translation(new Vector3(0, 0, sea_level)));
		if (flyMode) { // A rising sea lifts the flying camera
			keepAboveGround();
			updateCameraPosition();
		}
		updateTitle();
		renderer.render();
	}

	/**
	 * Resizes the GUIView to the current size of the panel and renders it: the RenderEngine adapts the perspective to
	 * the new size of the view (KEEP_SCALE, see run()). Ignored while the renderer is not created yet, or for an
	 * empty panel (e.g. minimized window).
	 */
	private void resizeViewport() {
		int width = panel.getWidth();
		int height = panel.getHeight();
		if (renderer == null || width < 1 || height < 1) return;
		// Nothing to do if the size did not change, e.g. for the first componentResized() event, when the window is
		// displayed at the initial size of the image (it can come while run() is still creating the renderer)
		if (width == guiView.getViewWidth() && height == guiView.getViewHeight()) return;
		guiView.resize(width, height);
		renderer.render();
	}

	/** Displays the camera mode and the current sea level in the title of the frame. */
	private void updateTitle() {
		frame.setTitle(String.format("Fractal Landscape - %s camera - Sea level: %.2f", flyMode ? "Fly" : "Orbit", sea_level));
	}

	/**
	 * Starts (or retargets) the eased transition of the camera's dolly position toward
	 * targetZoom (the new "zoom" accumulator value), instead of jumping straight to it as before.
	 * Each wheel notch is a discrete stepped input, similar in character to a keyboard tap, so it
	 * benefits from the same kind of easing used for MovingCamera's rotation keys -- but zoom is a
	 * translation along the camera's forward vector, not a rotation, so linear interpolation is
	 * used here instead of Quaternion.slerp().
	 *
	 * If a previous zoom animation is still in progress (e.g. several wheel notches in quick
	 * succession), this retargets from the CURRENT, already-interpolated zoom value rather than
	 * restarting from the value before that still-running animation began -- otherwise fast
	 * scrolling would look like the camera snapping backwards before each new increment.
	 */
	private void startZoomAnimation(float targetZoom) {
		zoomAnimStartValue = appliedZoom;
		zoomAnimTargetValue = targetZoom;
		zoomAnimStartMillis = System.currentTimeMillis();

		if (zoomAnimTimer == null) {
			zoomAnimTimer = new Timer(ZOOM_ANIM_FRAME_MS, e -> stepZoomAnimation());
		}
		if (!zoomAnimTimer.isRunning()) {
			zoomAnimTimer.start();
		}
	}

	/** One animation frame: advances the applied zoom value toward its target and renders. */
	private void stepZoomAnimation() {
		long elapsed = System.currentTimeMillis() - zoomAnimStartMillis;
		float t = Math.min(1f, (float) elapsed / ZOOM_ANIM_DURATION_MS);

		float previousZoom = appliedZoom;
		appliedZoom = interpolateZoom(zoomAnimStartValue, zoomAnimTargetValue, t);

		// Orbit: zoom camera by moving the eye toward the point of interest (orbit angles unchanged), see
		// updateCameraPosition(). Fly: move the eye along the CURRENT line of sight by the distance of this frame only,
		// so that the flight follows the direction even if it changes (mouse drag) during the animation.
		if (flyMode) flyForward((appliedZoom - previousZoom) * FLY_STEP);
		updateCameraPosition();

		// Render the updated view after zooming camera and rotating Element
		renderer.render();

		if (t >= 1f) {
			zoomAnimTimer.stop();
		}
	}

	/**
	 * Linearly interpolates the applied zoom value a fraction t of the way from startValue to
	 * targetValue. Package-private and static purely so it can be unit-tested
	 * (TestFractalLandscape) without a live Camera/RenderEngine/Swing GUIView -- this
	 * method touches none of those.
	 */
	static float interpolateZoom(float startValue, float targetValue, float t) {
		return startValue + (targetValue - startValue) * t;
	}



	// *****************
	// ***** Menus *****
	// *****************

	/** Landscape menu: actions on the whole scene. */
	private JMenu createLandscapeMenu() {
		JMenu menu = new JMenu("Landscape");
		menu.setMnemonic(KeyEvent.VK_L);
		JMenuItem generate = new JMenuItem("Generate");
		generate.addActionListener(e -> generateLandscape());
		menu.add(generate);

		// Sea level: CTRL + U / CTRL + D accelerators (letters: the same keys on every keyboard layout, unlike + / -,
		// which need SHIFT for + only on an AZERTY keyboard); Page Up / Page Down also work (see bindKeys())
		JMenuItem seaUp = new JMenuItem("Sea level up");
		seaUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
		seaUp.addActionListener(e -> changeSeaLevel(1));
		menu.add(seaUp);
		JMenuItem seaDown = new JMenuItem("Sea level down");
		seaDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
		seaDown.addActionListener(e -> changeSeaLevel(-1));
		menu.add(seaDown);
		menu.addSeparator();

		JMenuItem resetCamera = new JMenuItem("Reset camera");
		resetCamera.addActionListener(e -> resetCamera());
		menu.add(resetCamera);
		menu.addSeparator();

		// Camera mode: exclusive choice
		ButtonGroup cameraGroup = new ButtonGroup();
		JRadioButtonMenuItem orbitItem = new JRadioButtonMenuItem("Orbit camera", !flyMode);
		orbitItem.addActionListener(e -> setFlyMode(false));
		JRadioButtonMenuItem flyItem = new JRadioButtonMenuItem("Fly camera", flyMode);
		flyItem.addActionListener(e -> setFlyMode(true));
		cameraGroup.add(orbitItem);
		cameraGroup.add(flyItem);
		menu.add(orbitItem);
		menu.add(flyItem);
		return menu;
	}

	/**
	 * Switches between the orbit and the fly camera, and renders.
	 * - To fly: the flight starts where the orbit camera is, looking in the same direction (so the view does not
	 *   jump), and the zoom of the orbit is kept aside.
	 * - Back to orbit: the camera goes back on its orbit, where it was (same angles and zoom as before the flight).
	 */
	private void setFlyMode(boolean fly) {
		if (fly == flyMode) return;
		finishZoomAnimation();
		if (fly) {
			orbitZoom = zoom;
			Vector4 eyePosition = camera.getEye();
			Vector4 direction = poi.minus(eyePosition).normalize();
			flyStartEye = new Vector4(eyePosition);
			flyStartYaw = (float) Math.atan2(direction.getY(), direction.getX());
			flyStartPitch = clamp((float) Math.asin(direction.getZ()), -MAX_ELEVATION, MAX_ELEVATION);
			startFlight();
		} else {
			zoom = orbitZoom;
			appliedZoom = orbitZoom;
		}
		flyMode = fly;
		if (fly) keepAboveGround();
		updateCameraPosition();
		updateTitle();
		renderer.render();
	}

	/** Puts the fly camera at the start of the flight (no wheel movement pending). */
	private void startFlight() {
		flyEye = new Vector4(flyStartEye);
		flyYaw = flyStartYaw;
		flyPitch = flyStartPitch;
		zoom = 0;
		appliedZoom = 0f;
	}

	/** Stops a running zoom (or flight) animation at its target, so that a mode switch starts from a stable state. */
	private void finishZoomAnimation() {
		if (zoomAnimTimer != null && zoomAnimTimer.isRunning()) {
			zoomAnimTimer.stop();
			float previousZoom = appliedZoom;
			appliedZoom = zoom;
			if (flyMode) flyForward((appliedZoom - previousZoom) * FLY_STEP);
			updateCameraPosition();
		}
	}

	/** Rendering menu: the rendering type, an exclusive choice (radio buttons in a ButtonGroup). */
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
		item.addActionListener(e -> selectRendering(type));
		group.add(item);
		menu.add(item);
	}

	/**
	 * Effects menu: on/off options, independent of the rendering type, in three groups (surfaces, shadows, sun). Each
	 * check box starts from the state of the model and changes it, then renders.
	 */
	private JMenu createEffectsMenu() {
		JMenu menu = new JMenu("Effects");
		menu.setMnemonic(KeyEvent.VK_E);

		// Surfaces
		textureItem = addEffectItem(menu, "Texture", rContext.isTextureProcessing(), on -> rContext.setTextureProcessing(on));
		specularItem = addEffectItem(menu, "Specular highlights", lighting.hasSpecular(), on -> lighting.setSpecularLight(on));
		menu.addSeparator();

		// Shadows: PCF_3X3 filtering of the shadow map for soft edges (see ShadowFilter)
		shadowsItem = addEffectItem(menu, "Shadows", rContext.isShadowing(), on -> rContext.setShadowing(on));
		softShadowsItem = addEffectItem(menu, "Soft shadows", sun.getShadowFilter() == ShadowFilter.PCF_3X3,
				on -> sun.setShadowFilter(on ? ShadowFilter.PCF_3X3 : ShadowFilter.HARD));
		menu.addSeparator();

		// Sun: the light glow of a directional light is the SUN mode (disc and halo in the sky, see LightGlowMode)
		sunItem = addEffectItem(menu, "Sun", rContext.isLightGlow(), on -> rContext.setLightGlow(on));
		sunHaloItem = addEffectItem(menu, "Sun halo", sunAppearance.getGain() > 0, on -> sunAppearance.setGain(on ? 1f : 0f));
		lensFlareItem = addEffectItem(menu, "Lens flare", sunAppearance.getFlareGain() > 0, on -> sunAppearance.setFlareGain(on ? 1f : 0f));
		lightShaftsItem = addEffectItem(menu, "Light shafts", Boolean.TRUE.equals(rContext.getLightShafts()), on -> rContext.setLightShafts(on));
		return menu;
	}

	/** Creates a check box of the Effects menu: applies the change to the model, updates the menu and renders. */
	private JCheckBoxMenuItem addEffectItem(JMenu menu, String label, boolean selected, java.util.function.Consumer<Boolean> apply) {
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
	 * the sun by all but LINE. The sub-options (soft shadows, halo, lens flare, light shafts) also need their main option.
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

	/** Switches the rendering type, updates the Effects menu and renders. */
	private void selectRendering(RenderingType type) {
		rContext.setRenderingType(type);
		updateEffectsMenu();
		renderer.render();
	}

	/** Generates a new random landscape (same sea), recomputes its geometry and colors, and renders. */
	private void generateLandscape() {
		createLandscape(array_land, size, n);
		try {
			tre_land.updateTrellis(array_land);
		} catch (WrongArraySizeException exc) {
			exc.printStackTrace();
		}
		world.rebuild();
		System.out.println("Number of Triangles in Trellis: "+tre_land.getNbTriangles()+" Number of vertices: " + tre_land.getNbVertices());
		updateTrianglesColorTrellis();
		if (flyMode) { // The new ground may be above the flying camera
			keepAboveGround();
			updateCameraPosition();
		}
		renderer.render();
	}

	/**
	 * Puts the camera back to its initial position, and renders: orbit camera, its initial angles and no zoom; fly
	 * camera, the start of the flight.
	 */
	private void resetCamera() {
		if (zoomAnimTimer != null) zoomAnimTimer.stop();
		if (flyMode) {
			startFlight();
			keepAboveGround();
		} else {
			zoom = 0;
			appliedZoom = 0f;
			initOrbit();
		}
		updateCameraPosition();
		renderer.render();
	}

	/** Initial orbit parameters: spherical coordinates of the initial eye around the point of interest. */
	private void initOrbit() {
		Vector4 poiToEye = eye.minus(poi);
		orbitDistance = poiToEye.length();
		orbitAzimuth = (float) Math.atan2(poiToEye.getY(), poiToEye.getX());
		orbitElevation = (float) Math.asin(poiToEye.getZ() / orbitDistance);
	}
	
	/**
	 * @param size the size of the trellis
	 * @param n the number of segments (should be a power of 2)
	 */
	public void createLandscape(float[][] array_land, float size, int n) {
		createLandscape(array_land, size, n, new Random());
	}

	/**
	 * Same, with a given random generator: a Random created with a fixed seed always gives the same landscape
	 * (used to regenerate the image of the documentation, see DocumentationImages in the test sources).
	 *
	 * @param size the size of the trellis
	 * @param n the number of segments (should be a power of 2)
	 * @param random the source of the random variations of the altitudes
	 */
	public static void createLandscape(float[][] array_land, float size, int n, Random random) {
		
		float array[][] = new float[n+1][n+1]; // Temporary array to be used for the Treillis generation
		
		float mult = 0.6f; // Global multiplication factor for altitudes used by the Fractal generator. If higher, the landscape will be more accidented, if lower it will be smoother

		int ix = n; // Start from the largest dimension of the Treillis
		// Initialize the 4 corners
		array[0][0] = (float)random.nextDouble()*mult*size/2;
		array[0][ix] = (float)random.nextDouble()*mult*size/2;
		array[ix][0] = (float)random.nextDouble()*mult*size/2;
		array[ix][ix] = (float)random.nextDouble()*mult*size/2;
		
		// Recursion loop on i that will be divided by 2 at each iteration until it is equal to 1
		while (ix>1) {
			
			float factor = (float)(mult*(ix * size)/(n * 2)); // For each loop calculate the variation factor (should reduce at each Fractal iteration)
			// Then at each stage let's loop on sub-squares of the main square
			for (int j=0; j<n/ix; j++) { // n/i will be 1 (i=n at first iteration), then 2 (i is divided by 2), 4, 8, 16...
				for (int k=0; k<n/ix; k++) {
					
					// Let's calculate the average altitude of the center of the square as it will be used later
					float center = (array[0+j*ix][0+k*ix] + array[0+j*ix][ix+k*ix] + array[ix+j*ix][0+k*ix] + array[ix+j*ix][ix+k*ix])/4;
					
					// Then use Fractal approach to calculate the center of the square and the middle of each segment of the squares
					//
					// Co---M---Co
					// |    |    |
					// |    |    |
					// M---Cnt---M
					// |    |    |
					// |    |    |
					// Co---M---Co
					//
					// Co = Corners of the square
					// Cnt = Center of the square
					// M = Middle of each segment of the square
					
					// For each calculation, add a random value multiplied by the loop factor calculated above (hence proportional to the size of the square)
					array[ix/2+j*ix][ix/2+k*ix] = center + (float)random.nextDouble()*factor;
					// Use average of other points : 2 Corners + Center of the square for the middle segments
					array[ix/2+j*ix][0+k*ix] = (array[0+j*ix][0+k*ix] + array[ix+j*ix][0+k*ix] + center)/3 + (float)random.nextDouble()*factor;;
					array[0+j*ix][ix/2+k*ix] = (array[0+j*ix][0+k*ix] + array[0+j*ix][ix+k*ix] + center)/3 + (float)random.nextDouble()*factor;;
					array[ix+j*ix][ix/2+k*ix] = (array[ix+j*ix][0+k*ix] + array[ix+j*ix][ix+k*ix] + center)/3 + (float)random.nextDouble()*factor;;
					array[ix/2+j*ix][ix+k*ix] = (array[0+j*ix][ix+k*ix] + array[ix+j*ix][ix+k*ix] + center)/3 + (float)random.nextDouble()*factor;;
				}
			}
			ix/=2; // Divide i by 2 (i remains a power of 2)
		}
		
		//
		// End of Fractal generation
		//
		
		//float sea_level = 1.5f;
		
		for (int i=0; i<=n; i++ ) {
			for (int j=0; j<=n; j++) {
				// array_land[i][j] = array[i][j] >= sea_level ? array[i][j] : sea_level ;
				array_land[i][j] = array[i][j];
			}
		}
		
		array = null; // We no longer need this one, the array_land has been updated appropriately
		
	}
	
	protected void updateVertexColorTrellis( ) {
		float max_alt = tre_land.getMaxZ();
		float min_alt = tre_land.getMinZ();
		for (int v=0; v<tre_land.getVertices().size(); v++) {
			float v_alt = tre_land.getVertex(v).getPos().getZ();
			//tre_land.getVertex(v).setColor(new Color((int)((v_alt-min_alt)/(max_alt-min_alt)*200),(int)((v_alt-min_alt)/(max_alt-min_alt)*255),(int)((max_alt-v_alt)/(max_alt-min_alt)*200)));
			float ratio = (v_alt - min_alt) / (max_alt - min_alt);
			int gray = (int)(ratio*255); // From black (lowest) to white (highest)
			tre_land.getVertex(v).setColor(new Color(gray, gray, gray));
		}
		
	}

	protected void updateTrianglesColorTrellis() {
		colorTrianglesByAltitude(tre_land);
	}

	/** Colors each triangle of the trellis according to its average altitude: black (lowest) to white (highest). */
	public static void colorTrianglesByAltitude(Trellis tre) {
		float max_alt = tre.getMaxZ();
		float min_alt = tre.getMinZ();
		int t = 0;
		for (t=0; t<tre.getTriangles().size(); t++) {
			float avg_alt = (tre.getTriangle(t).getV1().getPos().getZ() + tre.getTriangle(t).getV2().getPos().getZ() + tre.getTriangle(t).getV3().getPos().getZ())/3;
			//tre.getTriangle(t).setColor(new Color((int)((avg_alt-min_alt)/(max_alt-min_alt)*200),(int)((avg_alt-min_alt)/(max_alt-min_alt)*255),(int)((max_alt-avg_alt)/(max_alt-min_alt)*200)));
			float ratio = (avg_alt-min_alt)/(max_alt-min_alt);
			int gray = (int)(ratio*255); // From black (lowest) to white (highest)
			tre.getTriangle(t).setColor(new Color(gray, gray, gray));
		}
	}


	/**
	 * The lighting of the image of the documentation (see DocumentationImages): a sun behind the camera of that image.
	 * The interactive demo uses a lower sun, that can be seen in the sky (see run() and SUN_AZIMUTH, SUN_ELEVATION).
	 */
	public static Lighting createLighting() {
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,1,-0.5f), 0.8f);
		AmbientLight al = new AmbientLight(0.2f);
		return new Lighting(dl, al, false);
	}

	/**
	 * Direction of a DirectionalLight (its propagation direction, from the light towards the scene, as the engine
	 * expects it) for a sun seen from the scene at the given azimuth (around the vertical Z axis, from the X axis) and
	 * elevation (above the horizon), in degrees. Package-private and static so it can be unit-tested.
	 */
	static Vector3 sunDirection(float azimuthDegrees, float elevationDegrees) {
		double azimuth = Math.toRadians(azimuthDegrees);
		double elevation = Math.toRadians(elevationDegrees);
		// Unit vector towards the sun, then reversed: the light travels from the sun towards the scene
		return new Vector3(
				(float) (-Math.cos(elevation) * Math.cos(azimuth)),
				(float) (-Math.cos(elevation) * Math.sin(azimuth)),
				(float) (-Math.sin(elevation)));
	}

	/**
	 * Create the World and Camera, generate a Treillis based on Fractal recursivity to create a Landscape
	 * Create the Graphic Swing view by calling the createView method that will associate mouse Listners to the panel so that user can 
	 * interact with the view and move it or zomm in it.
	 */
	public void run() {
		
		// Camera
		eye = new Vector4(6,0,3,1);
		poi = new Vector4(0,0,1.5f,1);
		camera = new Camera(eye, poi, Vector4.zAxis());

		initOrbit();
				
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");		
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		Texture tex = new Texture("resources/texture/texture_gray_concrete_wall_with_cracks_2748x2748.jpg");
		Texture tex_sea = new Texture("resources/texture/texture_waves-calm-sea_4480x4480.jpg");
		
		// Create World
		world = new World();
		world.setBackgroundColor(SKY_COLOR);
		
		master_land = new Element();
		
		// Create and form the Treillis
		size = 4; // Size of the Treillis (square)
		n = 128; // Nb of segments of the Treillis, should be a 2^n number
		array_land = new float[n+1][n+1]; // Updated array with sea level
		createLandscape(array_land, size, n);
		// Create the Treillis
		tre_land = null;
		try {
			tre_land = new Trellis(size, size, n, n, array_land, tex);
		} catch (WrongArraySizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//tre_land.setColor(new Color(100,200,50));
		tre_land.setSpecularExp(LAND_SPECULAR_EXP);
		tre_land.setSpecularColor(LAND_SPECULAR_COLOR);
		
		// Create the sea
		tre_sea = new Trellis(4, 4, 4, 4, tex_sea);
		tre_sea.setColor(new Color(0, 0, 250));
		tre_sea.setSpecularExp(SEA_SPECULAR_EXP);
		tre_sea.setSpecularColor(SEA_SPECULAR_COLOR);
		tre_sea.setTransformation(new Translation(new Vector3(0, 0, sea_level)));
		
		// Add the Treillis as a new Element of the World
		master_land.addElement(tre_land);
		master_land.addElement(tre_sea);
		world.addElement(master_land);
		
		// Generate World (including Triangles)
		System.out.println("********* Calculating normals");
		world.build();
		System.out.println("Number of Triangles in Trellis: "+tre_land.getNbTriangles()+" Number of vertices: " + tre_land.getNbVertices());
		
		// Then update Triangles as needed (Colors) to create effect on the Landscape
		updateTrianglesColorTrellis();

		// Lighting initialization: the sun (soft shadows when the shadows are on) and an ambient light, with the
		// specular highlights
		sun = new DirectionalLight(sunDirection(SUN_AZIMUTH, SUN_ELEVATION), 0.8f);
		sun.setShadowFilter(ShadowFilter.PCF_3X3);
		sunAppearance = new LightAppearance(); // Default halo and lens flare
		sun.setAppearance(sunAppearance);
		lighting = new Lighting(sun, new AmbientLight(0.2f), true);
		
		// Graphic Context
		// Near plane very close to the eye (0.01, less than the FLY_CLEARANCE of the fly camera above the ground), so
		// that the fly camera can go near the hills and the sea without cutting them (the near plane cuts everything
		// closer, which would show the "backstage" through the ground): the same field of view and image size as a
		// near plane at 0.8 with 0.8 x 0.45 and 1250 pixels per unit (all divided / multiplied by 80)
		pContext = new PerspectiveContext(0.01f, 0.005625f, 0.01f, 100, PerspectiveType.FRUSTUM, 100000);
		//PerspectiveContext pContext = new PerspectiveContext(8f, 4.5f, 1, 100, PerspectiveType.ORTHOGRAPHIC, 125);
		
		// When the window (hence the view) is resized, keep the number of pixels per unit rather than the field of view:
		// a larger window shows more of the landscape (wider and taller view volume) rather than an enlarged image
		pContext.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);

		// Rendering context (before the view: the menus start from its state): shading, no texture, no shadows,
		// sun visible (light glow) without light shafts
		rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(false);
		rContext.setLightGlow(true);

		// Create view
		guiView = this.createView(pContext);
		
		// Initialize Render Engine and render a first view
		renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
		renderer.setView(guiView);
		updateTitle();
		renderer.render();
		
		System.out.println("********* APPLICATION LAUNCHED *********");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		FractalLandscape fractal = new FractalLandscape();
		fractal.run();
		System.out.println("********* APPLICATION is RUNNING *********");
	}

}
