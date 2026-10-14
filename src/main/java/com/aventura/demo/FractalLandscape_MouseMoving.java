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

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.event.*;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
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
 * Controls (same conventions as most 3D / terrain viewers):
 * - Mouse drag:         orbit the camera around the point of interest (turntable: horizontal drag turns around
 *                       the vertical Z axis, vertical drag changes the elevation, clamped below the poles)
 * - Mouse wheel:        zoom (dolly the camera toward / away from the point of interest)
 * - CTRL + mouse wheel: raise / lower the sea level (a transformation of the World: the sea Element is translated on Z)
 * - Window resizing:    the rendered image always fills the window, at the same scale (PPU): a larger window shows
 *                       more of the landscape, objects keep their size on screen (PerspectiveContext.ResizeMode.KEEP_SCALE)
 * SHIFT + mouse wheel is deliberately left free: it is the usual OS / application convention for lateral scrolling,
 * hence reserved for a future camera pan.
 * 
 * @author Olivier BARRY
 * @since Nov 2023
 */
public class FractalLandscape_MouseMoving implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, ActionListener {
	
	// GUI Frame and Menus
	JFrame frame;
	JPanel panel; // Displays the rendered image, fills the content area of the frame
	JMenu menu1, menu2;
	JMenuItem m1e1, m1e2, m1e3, m2e1, m2e2, m2e3, m2e5, m2e6;
	boolean texture_menu = false;
	boolean shading_menu = true;
	boolean shadow_menu = false;

	// Camera
	Vector4 eye;
	Vector4 poi;
	Camera camera;
	
	// Render engine
	PerspectiveContext pContext;
	RenderContext rContext;
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
	// Each CTRL + mouse wheel notch moves the sea by this altitude; the sea level stays within the landscape altitudes
	static final float SEA_LEVEL_STEP = 0.01f;

	// Movement variables
	int i_rotation = 0;
	int j_rotation = 0;
	int zoom = 0;
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

	// Window resizing: the image is resized (and rendered) only once the size has not changed for RESIZE_DELAY_MS,
	// instead of rendering a full frame for each intermediate size while the user drags the border of the window
	private static final int RESIZE_DELAY_MS = 50;
	Timer resizeTimer;

	// CTRL key + mouse flag
	boolean key_control = false;

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
	    
	    // Create menu1
	    menu1 = new JMenu("Run");
	    m1e1 = new JMenuItem("Generate");
	    m1e1.addActionListener(this);
	    m1e2 = new JMenuItem("Texture off");
	    m1e2.addActionListener(this);
	    // Texture and shadows are options independent of the rendering type (Rendering menu)
	    m1e3 = new JMenuItem("Shadows off");
	    m1e3.addActionListener(this);
	    menu1.add(m1e1); 
	    menu1.add(m1e2); 
	    menu1.add(m1e3); 
	    menubar.add(menu1);
	    
	    // Create menu2
	    menu2 = new JMenu("Rendering");
	    m2e1 = new JMenuItem("Lines off");
	    m2e1.addActionListener(this);
	    m2e5 = new JMenuItem("Monochrome off");
	    m2e5.addActionListener(this);
	    m2e6 = new JMenuItem("Unlit off");
	    m2e6.addActionListener(this);
	    m2e2 = new JMenuItem("Flat off");
	    m2e2.addActionListener(this);
	    m2e3 = new JMenuItem("Shading on");
	    m2e3.addActionListener(this);
	    menu2.add(m2e1);
	    menu2.add(m2e5);
	    menu2.add(m2e6);
	    menu2.add(m2e2);
	    menu2.add(m2e3);
	    menubar.add(menu2);

	    // Add menu bar to the frame
	    frame.setJMenuBar(menubar);

		
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
        frame.addKeyListener(this);
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
	
	public void keyTyped(KeyEvent e) {
		// Do nothing
	}

	public void keyPressed(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
	    	key_control = true;
	    }
	}
 
	public void keyReleased(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
	    	key_control = false;
	    }
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

        // "Grab the World" feeling: dragging to the right makes the landscape turn to the right, hence the camera
        // turns the other way (azimuth decreases); dragging down shows the landscape more from above (elevation
        // increases). The elevation is clamped at each event (not on a total), so that dragging back immediately
        // moves the camera again, without a dead zone after having hit the limit.
        orbitAzimuth -= dragAngle(dx, panel.getWidth(), DRAG_ANGLE_PER_WIDTH);
        orbitElevation = clamp(orbitElevation + dragAngle(dy, panel.getHeight(), DRAG_ANGLE_PER_HEIGHT),
        		-MAX_ELEVATION, MAX_ELEVATION);

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
	 * Package-private and static purely so it can be unit-tested (TestFractalLandscape_MouseMoving) without a
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
	 * Restricts a value to the [min, max] interval. Package-private and static so it can be unit-tested.
	 */
	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Places the camera according to the current orbit angles and the current (possibly mid-animation) zoom,
	 * always looking at the point of interest with the World's up axis (Z) as up vector.
	 */
	private void updateCameraPosition() {
		float distance = orbitDistance - appliedZoom / 10;
		camera.updateCamera(orbitEye(poi, distance, orbitAzimuth, orbitElevation), poi, Vector4.zAxis());
	}

 	public void mouseMoved(MouseEvent e) {
		// Do nothing
 	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		//System.out.println("Mouse wheel moved: " + e.getWheelRotation());
		// The modifier keys are read on the event itself (not through keyPressed()/keyReleased(), which require the
		// frame to have the keyboard focus)
		if (e.isControlDown()) {
			// CTRL + wheel: wheel up (negative rotation) raises the sea, wheel down lowers it
			changeSeaLevel(-e.getWheelRotation());
			return;
		}
		// Increment or decrement zoom based on wheel rotation (+1 or -1), without letting the camera reach (or cross)
		// the point of interest: zoom/10 is the distance travelled toward the poi
		int maxZoom = (int) ((orbitDistance - MIN_ORBIT_DISTANCE) * 10);
		zoom = Math.min(zoom - e.getWheelRotation(), maxZoom);
		startZoomAnimation((float)zoom);
	}

	/**
	 * Raises (positive notches) or lowers (negative notches) the sea level by SEA_LEVEL_STEP per notch, within the
	 * altitudes of the landscape. This is a transformation of the World, not of the view: the sea Element (tre_sea)
	 * is translated on the Z axis; its geometry is unchanged, the RenderEngine applies the new transformation at the
	 * next rendering, so neither a rebuild of the World nor a new calculation of the normals is needed.
	 */
	private void changeSeaLevel(int notches) {
		sea_level = clamp(sea_level + notches * SEA_LEVEL_STEP, tre_land.getMinZ(), tre_land.getMaxZ());
		tre_sea.setTransformation(new Translation(new Vector3(0, 0, sea_level)));
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

	/** Displays the current sea level in the title of the frame. */
	private void updateTitle() {
		frame.setTitle(String.format("Fractal Landscape - Sea level: %.2f", sea_level));
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

		appliedZoom = interpolateZoom(zoomAnimStartValue, zoomAnimTargetValue, t);

		// Zoom camera by moving the eye toward the point of interest (orbit angles unchanged)
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
	 * (TestFractalLandscape_MouseMoving) without a live Camera/RenderEngine/Swing GUIView -- this
	 * method touches none of those.
	 */
	static float interpolateZoom(float startValue, float targetValue, float t) {
		return startValue + (targetValue - startValue) * t;
	}



	/**
	 * Switches the rendering type, updates the Rendering menu labels and renders. The texture toggle (Run menu)
	 * is only available for the rendering types that process textures (UNLIT, FLAT, INTERPOLATE), the shadows
	 * toggle for the ones that use the lights (FLAT, INTERPOLATE).
	 */
	private void selectRendering(RenderContext.RenderingType type) {
		rContext.setRenderingType(type);
		renderer.render();
		m2e1.setText(type == RenderContext.RenderingType.LINE ? "Lines on" : "Lines off");
		m2e5.setText(type == RenderContext.RenderingType.MONOCHROME ? "Monochrome on" : "Monochrome off");
		m2e6.setText(type == RenderContext.RenderingType.UNLIT ? "Unlit on" : "Unlit off");
		m2e2.setText(type == RenderContext.RenderingType.FLAT ? "Flat on" : "Flat off");
		m2e3.setText(type == RenderContext.RenderingType.INTERPOLATE ? "Shading on" : "Shading off");
		shading_menu = type == RenderContext.RenderingType.UNLIT || type == RenderContext.RenderingType.FLAT
				|| type == RenderContext.RenderingType.INTERPOLATE;
		m1e2.setEnabled(shading_menu);
		m1e3.setEnabled(type == RenderContext.RenderingType.FLAT || type == RenderContext.RenderingType.INTERPOLATE);
	}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

		if (e.getSource() == m1e1) { // Generate
			createLandscape(array_land, size, n);
			try {
				tre_land.updateTrellis(array_land);
			} catch (WrongArraySizeException exc) {
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
			world.rebuild();
			System.out.println("Number of Triangles in Trellis: "+tre_land.getNbTriangles()+" Number of vertices: " + tre_land.getNbVertices());
			updateTrianglesColorTrellis();
			renderer.render();
		} else if (e.getSource() == m1e2) { // Texture on/off (toggle)
			
			if (shading_menu) {
				if (texture_menu) {
					rContext.setTextureProcessing(false);
					renderer.render();
					m1e2.setText("Texture off");
					texture_menu = false;
				} else {
					rContext.setTextureProcessing(true);
					renderer.render();
					m1e2.setText("Texture on");
					texture_menu = true;
				}
			} else {
				// no action
			}
		} else if (e.getSource() == m2e1) { // Rendering lines
			selectRendering(RenderContext.RenderingType.LINE);
		} else if (e.getSource() == m2e5) { // Rendering monochrome (hidden-line wireframe)
			selectRendering(RenderContext.RenderingType.MONOCHROME);
		} else if (e.getSource() == m2e6) { // Rendering unlit (colors or texture, no lighting)
			selectRendering(RenderContext.RenderingType.UNLIT);
		} else if (e.getSource() == m2e2) { // Rendering flat (faceted shading)
			selectRendering(RenderContext.RenderingType.FLAT);
		} else if (e.getSource() == m2e3) { // Rendering Shading
			selectRendering(RenderContext.RenderingType.INTERPOLATE);
		} else if (e.getSource() == m1e3) { // Shadows on/off (toggle)

			if (shadow_menu) {
				rContext.setShadowing(false);
				renderer.render();
				m1e3.setText("Shadows off");
				shadow_menu = false;
			} else {
				rContext.setShadowing(true);
				renderer.render();
				m1e3.setText("Shadows on");
				shadow_menu = true;
			}
		} else {
			System.out.println("Other Action Event : "+e);			
		}
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
			tre_land.getVertex(v).setColor(new Color((int)((v_alt-min_alt)/(max_alt-min_alt)*200),(int)((v_alt-min_alt)/(max_alt-min_alt)*255),(int)((max_alt-v_alt)/(max_alt-min_alt)*200)));
		}
		
	}

	protected void updateTrianglesColorTrellis() {
		colorTrianglesByAltitude(tre_land);
	}

	/** Colors each triangle of the trellis according to its average altitude: blue (lowest) to yellow-green (highest). */
	public static void colorTrianglesByAltitude(Trellis tre) {
		float max_alt = tre.getMaxZ();
		float min_alt = tre.getMinZ();
		int t = 0;
		for (t=0; t<tre.getTriangles().size(); t++) {
			float avg_alt = (tre.getTriangle(t).getV1().getPos().getZ() + tre.getTriangle(t).getV2().getPos().getZ() + tre.getTriangle(t).getV3().getPos().getZ())/3;
			tre.getTriangle(t).setColor(new Color((int)((avg_alt-min_alt)/(max_alt-min_alt)*200),(int)((avg_alt-min_alt)/(max_alt-min_alt)*255),(int)((max_alt-avg_alt)/(max_alt-min_alt)*200)));
		}
	}


	public static Lighting createLighting() {
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,1,-0.5f), 0.8f);
		AmbientLight al = new AmbientLight(0.2f);
		return new Lighting(dl, al, false);
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

		// Initial orbit parameters: spherical coordinates of the initial eye around the point of interest
		Vector4 poiToEye = eye.minus(poi);
		orbitDistance = poiToEye.length();
		orbitAzimuth = (float) Math.atan2(poiToEye.getY(), poiToEye.getX());
		orbitElevation = (float) Math.asin(poiToEye.getZ() / orbitDistance);
				
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		
		// Create World
		world = new World();
		
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
		tre_land.setSpecularExp(8);
		
		// Create the sea
		tre_sea = new Trellis(4, 4, 4, 4);
		tre_sea.setColor(new Color(0, 0, 250));
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

		// Lighting initialization
		Lighting light = createLighting();
		
		// Graphic Context
		pContext = new PerspectiveContext(0.8f, 0.45f, 0.8f, 100, PerspectiveType.FRUSTUM, 1250);
		//PerspectiveContext pContext = new PerspectiveContext(8f, 4.5f, 1, 100, PerspectiveType.ORTHOGRAPHIC, 125);
		
		// When the window (hence the view) is resized, keep the number of pixels per unit rather than the field of view:
		// a larger window shows more of the landscape (wider and taller view volume) rather than an enlarged image
		pContext.setResizeMode(PerspectiveContext.ResizeMode.KEEP_SCALE);

		// Create view
		guiView = this.createView(pContext);

		// Rendering context
		rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(false);

		//rContext.setRendering(RenderContext.RenderingType.INTERPOLATE);
		
		// Initialize Render Engine and render a first view
		renderer = new RenderEngine(world, light, camera, rContext, pContext);
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
		FractalLandscape_MouseMoving fractal = new FractalLandscape_MouseMoving();
		fractal.run();
		System.out.println("********* APPLICATION is RUNNING *********");
	}

}
