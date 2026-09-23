package com.aventura.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.GUIView;
import com.aventura.view.SwingView;
import com.aventura.model.perspective.PerspectiveType;

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
 * This class is a demo application using Aventura Render Engine API: a small urban landscape made of
 * three buildings standing along a street, lit by a sun at mid-height that casts shadows, seen from a camera flying
 * around them. No texture is used (plain colors only).
 *
 * Its main purpose is to validate the concept of an Element made of sub-Elements, on three levels:
 *
 *     Building (Element)
 *       +-- Floor #0 .. #n-1 (Element), each one translated vertically by k * floorHeight
 *       |     +-- core (Box: the walls)
 *       |     +-- ledge (Box: a thin slab slightly larger than the core, materializing the floor level)
 *       |     +-- Window x nbWindows on the front facade (Element)
 *       |     |     +-- frame (Box), glass (Box), sill (Box)
 *       |     +-- Window x nbWindows on the back facade (same Window type)
 *       |     +-- door + canopy (Box, ground floor only)
 *       +-- roof slab (Box) and, optionally, a roof block (Box)
 *
 * Window, Floor and Building are typical Elements: they have no geometry of their own, they just
 * assemble sub-Elements in their constructor. An Element does not hold a reusable "model" that could
 * be instantiated several times (each Element owns its vertices and triangles), so "multiplying" a
 * Floor simply means creating one Floor per level and translating it, which is what Building does.
 *
 * The transformations compose down the tree: a Window is positioned in its Floor, the Floor in its
 * Building and the Building in the World, so moving a Building moves all its floors and windows.
 * Colors are inherited too: the Building gives its wall color to everything below it that does not
 * define its own color (the core of each Floor here), whereas windows, ledges, etc. override it.
 *
 * Unit of length: 1 unit is the height of a floor (about 3 meters).
 * Axes: X to the right, Y in depth (facades "front" are on the -Y side, facing the camera), Z up.
 *
 * The camera flies around the scene like a helicopter, for three laps (see HelicopterFlight): its orbit is
 * not centered on the buildings and its axis is tilted, so it gets closer to the buildings and to the ground
 * on one side of the scene, then farther and higher on the other side, while it keeps looking at the middle
 * building. The angle of the camera is advanced at each rendering according to the time the previous rendering
 * took, so the speed of the flight is constant whatever the speed of the computer (see nextAngle()).
 */

public class UrbanScape {

	// ****************************
	// ***** Modeling constants *****
	// ****************************

	// Unit of length is the height of one floor
	static final float FLOOR_HEIGHT = 1f;
	// Horizontal distance between the axes of two neighbor windows of a facade
	static final float WINDOW_PITCH = 1f;

	// Floor ledge (thin slab slightly wider than the walls, at the bottom of each floor)
	static final float LEDGE_HEIGHT = 0.06f;
	static final float LEDGE_OVERHANG = 0.05f;

	// Roof slab (on top of the last floor)
	static final float ROOF_HEIGHT = 0.08f;

	// Sizes of the window details (relative to the facade plane)
	static final float WINDOW_WIDTH_RATIO = 0.55f;  // of the pitch
	static final float WINDOW_HEIGHT_RATIO = 0.5f;  // of the floor height
	static final float WINDOW_CENTER_RATIO = 0.55f; // altitude of the window center, in floor height
	static final float FRAME_BORDER = 0.05f;        // visible width of the frame around the glass
	static final float FRAME_PROTRUSION = 0.03f;    // how much the frame stands out of the facade
	static final float GLASS_PROTRUSION = 0.05f;    // how much the glass stands out of the facade (a bit more than the frame, see Window)
	static final float SILL_PROTRUSION = 0.08f;
	static final float SILL_HEIGHT = 0.05f;

	// Ground floor door
	static final float DOOR_WIDTH = 0.45f;
	static final float DOOR_HEIGHT = 0.72f;
	static final float DOOR_PROTRUSION = 0.03f;
	static final float CANOPY_DEPTH = 0.5f;
	static final float CANOPY_HEIGHT = 0.04f;

	// Street: the sidewalks are a bit higher than the ledge of the ground floors (which are LEDGE_OVERHANG
	// out of the facade, over the sidewalk) so that their top faces are never coplanar
	static final float SIDEWALK_HEIGHT = 0.08f;
	static final float ROAD_HEIGHT = 0.02f;
	static final float LANE_MARK_HEIGHT = 0.03f; // measured from the ground, so 0.01 above the road
	static final float STREET_SEGMENT = 2f;      // length of each piece of sidewalk / road (see createStreet())

	// Ground plane
	static final float GROUND_WIDTH = 34f;  // on X
	static final float GROUND_LENGTH = 20f; // on Y

	// Colors
	static final Color SKY_COLOR = new Color(150, 190, 235);
	static final Color GROUND_COLOR = new Color(110, 140, 95);
	static final Color SIDEWALK_COLOR = new Color(185, 183, 178);
	static final Color ROAD_COLOR = new Color(70, 72, 78);
	static final Color LANE_MARK_COLOR = new Color(235, 235, 225);
	static final Color FRAME_COLOR = new Color(235, 235, 230);
	static final Color GLASS_COLOR = new Color(45, 85, 130);
	static final Color DOOR_COLOR = new Color(90, 55, 35);
	static final Color DARK_COLOR = new Color(90, 90, 95);

	// Front and back facades (sign of the Y coordinate of the facade in the Floor's coordinates)
	static final int FRONT = -1;
	static final int BACK = 1;

	// ****************************************
	// ***** Elements made of sub-Elements *****
	// ****************************************

	/**
	 * A window: a frame, a pane of glass and a sill.
	 *
	 * The origin of a Window is the center of the window, on the plane of the facade. All the boxes are
	 * centered on that plane and thick enough to stand out of it on both sides (the half that would
	 * lie inside the walls is hidden by them), so the very same Window can be placed on the front facade
	 * (y = -depth/2) and on the back facade (y = +depth/2) of a Floor with a simple translation.
	 */
	static class Window extends Element {

		Window(float width, float height) {
			super("window");

			// Frame: full box; only its border remains visible around the (slightly proud) glass
			Box frame = new Box(width, 2 * FRAME_PROTRUSION, height, "frame");
			frame.setColor(FRAME_COLOR);
			addElement(frame);

			// Glass pane, with a specular reflection of the sun
			Box glass = new Box(width - 2 * FRAME_BORDER, 2 * GLASS_PROTRUSION, height - 2 * FRAME_BORDER, "glass");
			glass.setColor(GLASS_COLOR);
			glass.setSpecularExp(30);
			glass.setSpecularColor(new Color(220, 220, 220));
			addElement(glass);

			// Sill, just below the window: relief and a small cast shadow on the wall
			Box sill = new Box(width + 0.1f, 2 * SILL_PROTRUSION, SILL_HEIGHT, "sill");
			sill.setColor(FRAME_COLOR);
			sill.setTransformation(new Translation(new Vector3(0, 0, -(height + SILL_HEIGHT) / 2)));
			addElement(sill);
		}
	}

	/**
	 * One floor of a building. The origin of a Floor is the center of its bottom, its walls go up to z = height.
	 * A Floor has nbWindows windows on its front facade and as many on its back facade. The ground floor has a
	 * door instead of the middle window of its front facade.
	 *
	 * The color of the walls is not defined here: it is inherited from the Building.
	 */
	static class Floor extends Element {

		Floor(float width, float depth, float height, int nbWindows, boolean groundFloor, Color ledgeColor) {
			super("floor");

			// Walls: a closed box (no color of its own -> inherited)
			Box core = new Box(width, depth, height, "core");
			core.setTransformation(new Translation(new Vector3(0, 0, height / 2)));
			addElement(core);

			// Ledge at the bottom of the floor, standing out of the walls all around
			Box ledge = new Box(width + 2 * LEDGE_OVERHANG, depth + 2 * LEDGE_OVERHANG, LEDGE_HEIGHT, "ledge");
			ledge.setColor(ledgeColor);
			ledge.setTransformation(new Translation(new Vector3(0, 0, LEDGE_HEIGHT / 2)));
			addElement(ledge);

			// Windows on both facades
			float pitch = width / nbWindows;
			float windowWidth = pitch * WINDOW_WIDTH_RATIO;
			float windowHeight = height * WINDOW_HEIGHT_RATIO;
			float windowZ = height * WINDOW_CENTER_RATIO;

			for (int i = 0; i < nbWindows; i++) {
				float x = -width / 2 + pitch * (i + 0.5f);
				for (int facade : new int[] { FRONT, BACK }) {
					if (groundFloor && facade == FRONT && i == nbWindows / 2) {
						addDoor(x, facade * depth / 2);
						continue;
					}
					Window window = new Window(windowWidth, windowHeight);
					window.setTransformation(new Translation(new Vector3(x, facade * depth / 2, windowZ)));
					addElement(window);
				}
			}
		}

		// Door and its canopy, at position x on the front facade (y)
		private void addDoor(float x, float y) {
			Box door = new Box(DOOR_WIDTH, 2 * DOOR_PROTRUSION, DOOR_HEIGHT, "door");
			door.setColor(DOOR_COLOR);
			door.setTransformation(new Translation(new Vector3(x, y, DOOR_HEIGHT / 2)));
			addElement(door);

			// The canopy sticks out of the facade (towards -Y) and casts a shadow on the door
			Box canopy = new Box(DOOR_WIDTH + 0.3f, CANOPY_DEPTH, CANOPY_HEIGHT, "canopy");
			canopy.setColor(DARK_COLOR);
			canopy.setTransformation(new Translation(new Vector3(x, y - CANOPY_DEPTH / 2 + 0.02f, DOOR_HEIGHT + 0.05f + CANOPY_HEIGHT / 2)));
			addElement(canopy);
		}
	}

	/**
	 * A building: nbFloors Floors of the same type stacked on top of each other, each one translated
	 * vertically by k * FLOOR_HEIGHT, then a roof. The origin of a Building is the center of its footprint, on the ground.
	 */
	static class Building extends Element {

		private final float width;
		private final float depth;
		private final int nbFloors;

		/**
		 * @param name       name of the Building
		 * @param nbWindows  number of windows on each facade (front and back), the width is nbWindows * WINDOW_PITCH
		 * @param depth      dimension on Y
		 * @param nbFloors   number of floors
		 * @param wallColor  color of the walls
		 * @param roofBlock  true to add a technical block on the roof
		 */
		Building(String name, int nbWindows, float depth, int nbFloors, Color wallColor, boolean roofBlock) {
			super(name);
			this.width = nbWindows * WINDOW_PITCH;
			this.depth = depth;
			this.nbFloors = nbFloors;

			// Inherited by all sub-Elements that do not have their own color (the walls of each Floor)
			setColor(wallColor);
			Color ledgeColor = darker(wallColor, 0.8f);

			// Stack the floors
			for (int k = 0; k < nbFloors; k++) {
				Floor floor = new Floor(width, depth, FLOOR_HEIGHT, nbWindows, k == 0, ledgeColor);
				floor.setTransformation(new Translation(new Vector3(0, 0, k * FLOOR_HEIGHT)));
				addElement(floor);
			}

			// Roof
			float top = nbFloors * FLOOR_HEIGHT;
			Box roof = new Box(width + 2 * LEDGE_OVERHANG, depth + 2 * LEDGE_OVERHANG, ROOF_HEIGHT, "roof");
			roof.setColor(darker(wallColor, 0.6f));
			roof.setTransformation(new Translation(new Vector3(0, 0, top + ROOF_HEIGHT / 2)));
			addElement(roof);

			if (roofBlock) {
				Box block = new Box(Math.min(1.2f, width / 2), depth / 2, 0.5f, "roofblock");
				block.setColor(darker(wallColor, 0.7f));
				block.setTransformation(new Translation(new Vector3(width / 4, depth / 6, top + ROOF_HEIGHT + 0.25f)));
				addElement(block);
			}
		}

		float getWidth() {
			return width;
		}

		float getDepth() {
			return depth;
		}

		int getNbFloors() {
			return nbFloors;
		}

		/** Height of the building, roof slab included */
		float getHeight() {
			return nbFloors * FLOOR_HEIGHT + ROOF_HEIGHT;
		}
	}

	// ********************************
	// ***** Landscape construction *****
	// ********************************

	static Color darker(Color c, float factor) {
		return new Color((int) (c.getRed() * factor), (int) (c.getGreen() * factor), (int) (c.getBlue() * factor));
	}

	/** Number of Elements in the tree rooted at e, e included (World.toString() only counts the first levels) */
	static int countElements(Element e) {
		int nb = 1;
		if (!e.isLeaf()) {
			for (Element sub : e.getSubElements()) {
				nb += countElements(sub);
			}
		}
		return nb;
	}

	/** Number of Triangles of the tree rooted at e, e included */
	static int countTriangles(Element e) {
		int nb = e.getNbTriangles();
		if (!e.isLeaf()) {
			for (Element sub : e.getSubElements()) {
				nb += countTriangles(sub);
			}
		}
		return nb;
	}

	/**
	 * Add a Building to the World, at position x along the street.
	 * The front facade of all the buildings is aligned on the y = 0 line, so it is the depth of the
	 * building that defines where its center is on Y.
	 */
	static Building addBuilding(World world, Building b, float x) {
		b.setTransformation(new Translation(new Vector3(x, b.getDepth() / 2, 0)));
		world.addElement(b);
		return b;
	}

	/**
	 * The street in front of the buildings: two sidewalks and a road with dashed lane marks, all along X
	 * (from -length/2 to +length/2). Made as a single Element (a group) whose sub-Elements are thin boxes
	 * standing out of the ground by a few hundredths of unit.
	 * 
	 * Sidewalks and road are not single long boxes but a series of short ones (STREET_SEGMENT long): the
	 * rendering engine draws a triangle only if at least one of its vertices is in the view frustum (there is
	 * no clipping), so a very long triangle with both ends out of the screen would disappear while it still
	 * covers a part of the screen. With short segments this cannot happen unless the camera is really close.
	 */
	static Element createStreet(float length) {
		Element street = new Element("street");

		int nbSegments = Math.round(length / STREET_SEGMENT);
		for (int i = 0; i < nbSegments; i++) {
			float x = -length / 2 + STREET_SEGMENT * (i + 0.5f);
			// Sidewalk along the buildings (y from -1.5 to 0), road (y from -5.5 to -1.5), opposite sidewalk (y from -7 to -5.5)
			street.addElement(createSlab("sidewalk", STREET_SEGMENT, 1.5f, SIDEWALK_HEIGHT, x, -0.75f, SIDEWALK_COLOR));
			street.addElement(createSlab("road", STREET_SEGMENT, 4f, ROAD_HEIGHT, x, -3.5f, ROAD_COLOR));
			street.addElement(createSlab("sidewalk", STREET_SEGMENT, 1.5f, SIDEWALK_HEIGHT, x, -6.25f, SIDEWALK_COLOR));
			// Dashed lane mark in the middle of the road: one dash per segment
			street.addElement(createSlab("lane mark", 0.8f, 0.08f, LANE_MARK_HEIGHT, x, -3.5f, LANE_MARK_COLOR));
		}
		return street;
	}

	// A flat box lying on the ground plane (z = 0), of the given sizes, centered at (x, y)
	private static Box createSlab(String name, float sizeX, float sizeY, float height, float x, float y, Color color) {
		Box slab = new Box(sizeX, sizeY, height, name);
		slab.setColor(color);
		slab.setTransformation(new Translation(new Vector3(x, y, height / 2)));
		return slab;
	}

	/**
	 * Creates the whole World: the ground plane, the street and three buildings of different sizes.
	 * The World is built (geometry and normals are calculated) and ready to be rendered.
	 */
	static World createWorld() {

		World world = new World("urban landscape");
		world.setBackgroundColor(SKY_COLOR);

		// Ground plane: 34 x 20, from x = -17 to 17 and from y = -11 to y = 9. Made of a grid of 2 x 2 cells rather
		// than of a single big rectangle (see createStreet() for why).
		Trellis ground = new Trellis(GROUND_WIDTH, GROUND_LENGTH, (int) (GROUND_WIDTH / 2), (int) (GROUND_LENGTH / 2));
		ground.setColor(GROUND_COLOR);
		ground.setTransformation(new Translation(new Vector3(0, -1, 0)));
		world.addElement(ground);

		// Street
		world.addElement(createStreet(GROUND_WIDTH));

		// Three buildings of different sizes: a tall one, a medium one and a long low one.
		// (nbWindows per facade, depth, nbFloors, color, roof block)
		addBuilding(world, new Building("tower", 5, 3.5f, 10, new Color(205, 192, 168), true), -6.5f);
		addBuilding(world, new Building("block", 3, 3f, 6, new Color(176, 118, 100), false), -1.0f);
		addBuilding(world, new Building("row", 7, 3f, 4, new Color(150, 160, 172), true), 5.0f);

		// Calculate the geometry (vertices, triangles and normals) of the whole tree of Elements
		world.build();

		return world;
	}

	// **********************************
	// ***** Sun, camera and rendering *****
	// **********************************

	/**
	 * A directional light representing the sun.
	 *
	 * @param heading   direction in which the sun rays travel over the ground, in degrees, from +X axis towards +Y axis
	 * @param elevation angle of the sun above the horizon, in degrees (45 is mid-height)
	 * @param intensity intensity of the light
	 */
	static DirectionalLight createSun(float heading, float elevation, float intensity) {
		double h = Math.toRadians(heading);
		double e = Math.toRadians(elevation);
		// The direction is the one of the rays, from the sun to the scene: it goes down (-z)
		Vector3 direction = new Vector3((float) (Math.cos(e) * Math.cos(h)), (float) (Math.cos(e) * Math.sin(h)), (float) -Math.sin(e));
		return new DirectionalLight(direction, intensity);
	}

	static final float SUN_HEADING = 65f;   // degrees, from +X axis towards +Y axis
	static final float SUN_ELEVATION = 45f; // degrees above the horizon

	static Lighting createLighting() {
		// Sun at mid-height (45 degrees above the horizon) coming from the front left, so that its rays travel towards the
		// back right: the front and left faces of the buildings are lit and the shadows fall behind them. The front
		// facades are lit rather frontally (heading close to +Y): a very oblique light on them, with the fixed depth bias of
		// the shadow map, would produce a lot of shadow acne on the walls and windows.
		// Plus some ambient light so that faces in the shade are not black. The third parameter enables the specular
		// reflection (windows).
		return new Lighting(createSun(SUN_HEADING, SUN_ELEVATION, 0.75f), new AmbientLight(0.3f), true);
	}

	// **************************************
	// ***** Helicopter flight of the camera *****
	// **************************************

	// Number of laps around the scene
	static final int FLIGHT_LAPS = 3;
	// Angular speed of the camera around the scene, in degrees per second: a lap takes 30 seconds
	static final float FLIGHT_SPEED = 12f;
	// A rendering takes a fraction of a second, so the angle is advanced at each rendering by the angular speed times the
	// duration of the previous rendering. This step (in degrees) is limited so that the scene never jumps if a rendering
	// is exceptionally long (the first one, when the JVM is not warmed up, or a busy computer): the flight slows down instead.
	static final float FLIGHT_MAX_STEP = 8f;
	// Radius of the orbit, in units
	static final float FLIGHT_RADIUS = 17f;
	// Position of the center of the orbit relative to the point the camera looks at: behind the buildings (+Y) and above
	// them. The orbit is not centered on the buildings, so the camera comes close to them on the front side (the street)
	// and moves away on the back side: the distance to the buildings varies by about 7 units on each side of the radius.
	// Nor is it too close to them: on the sides, the orbit stays out of the footprint of the buildings.
	static final Vector3 FLIGHT_CENTER_SHIFT = new Vector3(0, 6, 5);
	// Angle between the axis of the rotation and the vertical, in degrees. The orbit is tilted around the X axis so that its
	// front side is lower than its back side: the camera comes close to the ground when it comes close to the buildings
	static final float FLIGHT_TILT = 15f;
	// Position of the camera at the start of the flight, as an angle on the orbit: -90 is the front of the scene, where the
	// camera is the closest to the buildings and the lowest
	static final float FLIGHT_START = -90f;

	/**
	 * The trajectory of a camera flying around a point like a helicopter and looking at it all the time.
	 * 
	 * The camera turns on a circle centered on a point which is not the one it looks at (the focus): the distance between the
	 * camera and the focus then varies during a lap, the camera gets closer, then farther. The circle is not horizontal either:
	 * the rotation is done around the vertical (Z) axis tilted by a few degrees around the X axis, so the lowest point of the
	 * circle is on the -Y side and its highest point on the +Y side. With a center behind and above the focus, the camera is
	 * then the closest to the focus and the lowest on the front side, and the farthest and the highest on the back side.
	 */
	static class HelicopterFlight {

		private final Vector4 focus;
		private final Vector4 center;
		private final float radius;
		private final Vector3 axis;
		private final float start;

		/**
		 * @param focus  the point the camera always looks at
		 * @param shift  position of the center of the orbit relative to focus
		 * @param radius radius of the orbit
		 * @param tilt   angle between the axis of the rotation and the vertical, in radians
		 * @param start  angle of the position of the camera at the start of the flight (angle 0), in radians, on the orbit
		 */
		HelicopterFlight(Vector4 focus, Vector3 shift, float radius, float tilt, float start) {
			this.focus = new Vector4(focus);
			this.center = focus.plus(shift);
			this.radius = radius;
			this.start = start;
			// The axis of the rotation: Z axis tilted around X axis
			this.axis = new Rotation(tilt, Vector3.xAxis()).times(Vector4.zAxis()).V3();
		}

		/**
		 * @param angle the angle traveled since the start of the flight, in radians (2 * PI is a lap)
		 * @return the position of the camera
		 */
		Vector4 getEye(float angle) {
			// Point of the orbit at angle 0 (on the X axis relative to center), rotated by angle + start around the tilted axis
			Vector4 offset = new Vector4(radius, 0, 0, 0);
			return center.plus(new Rotation(start + angle, axis).times(offset));
		}

		/** The point the camera looks at */
		Vector4 getFocus() {
			return focus;
		}

		Vector4 getCenter() {
			return center;
		}

		float getRadius() {
			return radius;
		}
	}

	/**
	 * The angle of the camera at the next rendering, given the one of the current rendering and how long it took.
	 * The angle moves forward by the angular speed times the duration of the rendering, so the camera goes as fast in
	 * the World with a fast or a slow rendering (only the number of images per lap changes), but by no more than maxStep,
	 * so that a very slow rendering does not produce a jump of the camera.
	 * 
	 * @param angle         angle at the current rendering, in radians
	 * @param renderingTime duration of the current rendering, in milliseconds
	 * @param speed         angular speed of the camera, in radians per second
	 * @param maxStep       maximum increase of the angle, in radians
	 * @return the angle at the next rendering, in radians
	 */
	static float nextAngle(float angle, long renderingTime, float speed, float maxStep) {
		float step = speed * renderingTime / 1000f;
		return angle + Math.min(step, maxStep);
	}

	/**
	 * The Building in the middle of the World (in the sense of its position on X, among all the buildings of the World)
	 * 
	 * @return the Building, null if there is none in the World
	 */
	static Building findCentralBuilding(World world) {
		ArrayList<Building> buildings = new ArrayList<Building>();
		for (Element e : world.getElements()) {
			if (e instanceof Building) buildings.add((Building) e);
		}
		buildings.sort((a, b) -> Float.compare(getWorldCenter(a).getX(), getWorldCenter(b).getX()));
		return buildings.isEmpty() ? null : buildings.get(buildings.size() / 2);
	}

	/** Position in the World of the center of a Building (at half its height) */
	static Vector4 getWorldCenter(Building b) {
		return b.getTransformation().times(new Vector4(0, 0, b.getHeight() / 2, 1));
	}

	/** The trajectory of the camera around the middle building of the World, with the parameters of the constants above */
	static HelicopterFlight createFlight(World world) {
		return new HelicopterFlight(getWorldCenter(findCentralBuilding(world)), FLIGHT_CENTER_SHIFT, FLIGHT_RADIUS,
				(float) Math.toRadians(FLIGHT_TILT), (float) Math.toRadians(FLIGHT_START));
	}

	static PerspectiveContext createPerspectiveContext() {
		// 1.6 x 0.9 view plane at distance 1 with 800 pixels per unit -> 1280 x 720 image
		return new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, 800);
	}

	static RenderContext createRenderContext() {
		// Smooth shading, shadows enabled, no texture
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setShadowing(true);
		rContext.setTextureProcessing(false);
		return rContext;
	}

	// GUIView to be displayed
	private SwingView view;
	private JFrame frame;

	static final String TITLE = "Urban landscape";

	/** false as soon as the window is closed */
	boolean isOpen() {
		return frame != null && frame.isDisplayable();
	}

	void setTitle(String title) {
		frame.setTitle(title);
	}

	// This method will create a basic Swing GUIView
	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application
		frame = new JFrame(TITLE);

		// Create the gUIView to be displayed
		view = new SwingView(context, frame);

		// Create a panel showing the image, sized so that the whole image is visible, and add it to the frame
		JPanel panel = new JPanel() {

			public void paintComponent(Graphics graph) {
				graph.drawImage(view.getImageView(), 0, 0, null);
			}
		};
		panel.setPreferredSize(new Dimension(context.getPixelWidth(), context.getPixelHeight()));
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();

		// Locate application frame in the center of the screen
		frame.setLocationRelativeTo(null);

		// Render the frame on the display
		frame.setVisible(true);

		return view;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		System.out.println("********* Creating World");
		World world = createWorld();
		int nbElements = 0, nbTriangles = 0;
		for (Element e : world.getElements()) {
			System.out.println("* " + e.getName() + ": " + countElements(e) + " Element(s), " + countTriangles(e) + " triangles");
			nbElements += countElements(e);
			nbTriangles += countTriangles(e);
		}
		System.out.println("World: " + nbElements + " Elements and " + nbTriangles + " triangles in total");

		System.out.println("********* Creating Lighting");
		Lighting lighting = createLighting();

		// The camera flies around the middle building, it is put at the start of its trajectory
		HelicopterFlight flight = createFlight(world);
		Camera camera = new Camera(flight.getEye(0), flight.getFocus(), Vector4.zAxis());
		PerspectiveContext pContext = createPerspectiveContext();
		RenderContext rContext = createRenderContext();

		UrbanScape appli = new UrbanScape();
		GUIView gUIView = appli.createView(pContext);

		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, pContext);
		renderer.setView(gUIView);

		System.out.println("********* Flying around the scene: " + FLIGHT_LAPS + " laps");
		float lap = (float) (2 * Math.PI);
		float total = FLIGHT_LAPS * lap;
		float speed = (float) Math.toRadians(FLIGHT_SPEED);
		float maxStep = (float) Math.toRadians(FLIGHT_MAX_STEP);
		float angle = 0;
		int nbImages = 0;
		long flightStart = System.currentTimeMillis();

		while (appli.isOpen()) {
			long start = System.currentTimeMillis();

			camera.updateCamera(flight.getEye(angle), flight.getFocus(), Vector4.zAxis());
			renderer.render();
			nbImages++;

			long renderingTime = System.currentTimeMillis() - start;
			appli.setTitle(String.format("%s - lap %d/%d - %d ms per image", TITLE, Math.min(FLIGHT_LAPS, (int) (angle / lap) + 1), FLIGHT_LAPS, renderingTime));

			if (angle >= total) break; // The last image is back at the start position
			angle = Math.min(nextAngle(angle, renderingTime, speed, maxStep), total);
		}

		long duration = System.currentTimeMillis() - flightStart;
		System.out.println(nbImages + " images in " + duration + " ms, " + duration / Math.max(1, nbImages) + " ms per image in average");
		System.out.println(renderer.renderStats());

		System.out.println("********* ENDING APPLICATION *********");
	}
}
