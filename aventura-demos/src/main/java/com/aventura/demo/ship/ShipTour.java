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

import java.util.ArrayList;
import java.util.List;

import com.aventura.math.transform.Rotation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * The visit of the ship: a closed path through the corridors, followed by a camera at the height of the eyes, which
 * looks ahead in the turns and glances at the points of interest on its way.
 *
 * <h2>The path</h2>
 * The path is a chain of {@link PathSegment}s, lines and arcs, each one starting where the previous one ends, in the
 * same direction: it has no corner. It follows the middle of the corridors, and turns in the junctions on arcs of
 * TURN_RADIUS. Unlike the helicopter tour of TempleRuins (a spline through waypoints), it is built from the plan of
 * the ship itself: the same PathSegments as the corridors.
 * <pre>
 *        .--------------- 3 ---------------.             1  south corridor, eastwards
 *       /                 |                 \            2  east bend
 *      5                  4, 9               2           3  north corridor, westwards, to the junction
 *       \                 |                 /            4  the shortcut, southwards, through the control room
 *        '------- 6 ------+------- 1 ------'             5  ...the south corridor westwards, the west bend,
 *                                                        6  ...and the north corridor eastwards
 *                                                        9  the shortcut again, then back to 1
 * </pre>
 * The shortcut is crossed twice, both times southwards: a closed visit of the whole loop and the shortcut must go
 * through one of its parts twice (the two junctions have three corridors each).
 *
 * <h2>The camera</h2>
 * <ul>
 * <li>The eye is on the path, at EYE_HEIGHT. It moves at a constant speed, whatever the time taken by an image (see
 *     TempleRuins).</li>
 * <li>It looks at the point of the path LOOK_AHEAD meters further: in a turn, the head turns before the body, as a
 *     visitor (or the pilot of an FPV drone) does.</li>
 * <li>At places, the look leaves the path for a point of interest ({@link Glance}), and comes back to it.</li>
 * <li>In the turns, the camera banks slightly, like a drone (see {@link #up}).</li>
 * </ul>
 *
 * @since Nov 2026
 */
public class ShipTour {

	public static final float EYE_HEIGHT = 1.65f;
	public static final float LOOK_AHEAD = 3.5f;
	public static final float LOOK_HEIGHT = 1.55f;
	/** Radius of the turns in the junctions. */
	public static final float TURN_RADIUS = 1.5f;
	/** Roll of the camera, in radians, for a turn of one radian per meter, and maximum roll. */
	public static final float BANK_PER_TURN = 0.13f;
	public static final float MAX_BANK = (float) Math.toRadians(5);

	/** A point the camera looks at while the visitor walks between two distances of the path. */
	public static class Glance {
		final float from, to;      // Distances along the path
		final Vector4 target;

		public Glance(float from, float to, Vector4 target) {
			this.from = from;
			this.to = to;
			this.target = target;
		}
	}

	/** How far the look turns to a point of interest, and back, in meters walked. */
	public static final float GLANCE_EASE = 2f;

	private final List<PathSegment> segments = new ArrayList<PathSegment>();
	private final List<Float> starts = new ArrayList<Float>(); // Distance at the start of each segment
	private final List<Glance> glances = new ArrayList<Glance>();
	private float length;

	/** The visit of the ship (see the class comment): its plan is in the constants of Ship. */
	public ShipTour() {
		float l = Ship.LOOP_HALF_LENGTH, w = Ship.LOOP_HALF_WIDTH, r = TURN_RADIUS;
		add(PathSegment.line(r, -w, l, -w));                 // 1  south corridor, eastwards
		add(PathSegment.arc(l, 0, w, -90, 180));             // 2  east bend
		add(PathSegment.line(l, w, r, w));                   // 3  north corridor, westwards
		add(PathSegment.arc(r, w - r, r, 90, 90));           //    turn left, southwards, in the north junction
		add(PathSegment.line(0, w - r, 0, -w + r));          // 4  the shortcut, through the control room
		add(PathSegment.arc(-r, -w + r, r, 0, -90));         //    turn right, westwards, in the south junction
		add(PathSegment.line(-r, -w, -l, -w));               // 5  south corridor, westwards
		add(PathSegment.arc(-l, 0, w, -90, -180));           //    west bend
		add(PathSegment.line(-l, w, -r, w));                 // 6  north corridor, eastwards
		add(PathSegment.arc(-r, w - r, r, 90, -90));         //    turn right, southwards, in the north junction
		add(PathSegment.line(0, w - r, 0, -w + r));          // 9  the shortcut again
		add(PathSegment.arc(r, -w + r, r, 180, 90));         //    turn left, eastwards, in the south junction: back to 1

		// The glances: the consoles of the control room (east, then west the second time), the hatch of the hold at the
		// end of the airlock corridor, from the south junction, and the closed door of the north junction
		float eyes = EYE_HEIGHT - 0.3f;
		float room1 = startOf(4) + w - r, room2 = startOf(10) + w - r; // Where the shortcut crosses the middle of the room
		glances.add(new Glance(room1 - 3.5f, room1 + 1f, new Vector4(4.4f, 1.5f, 1.1f, 1)));
		glances.add(new Glance(room2 - 3.5f, room2 + 1f, new Vector4(-4.4f, -1.5f, 1.1f, 1)));
		glances.add(new Glance(startOf(5) - 0.5f, startOf(5) + 2.2f, new Vector4(0, -w - Hub.SIZE / 2 - Ship.AIRLOCK_CORRIDOR_LENGTH, Ship.HATCH_HEIGHT, 1)));
		glances.add(new Glance(startOf(3) - 3f, startOf(3) - 0.5f, new Vector4(0, w + Hub.SIZE / 2, eyes, 1)));
	}

	private void add(PathSegment segment) {
		starts.add(length);
		segments.add(segment);
		length += segment.getLength();
	}

	/** Distance along the path at the start of its segment k. */
	public float startOf(int k) {
		return starts.get(k);
	}

	/** Length of the whole visit, in meters. */
	public float getLength() {
		return length;
	}

	public List<PathSegment> getSegments() {
		return segments;
	}

	/** The point of the path (on the floor) after the given distance; the visit is endless. */
	public Vector4 pointAt(float distance) {
		float d = wrap(distance);
		int k = segments.size() - 1;
		while (k > 0 && starts.get(k) > d) k--;
		return segments.get(k).point(d - starts.get(k));
	}

	/** The direction of the path after the given distance. */
	public Vector3 tangentAt(float distance) {
		float d = wrap(distance);
		int k = segments.size() - 1;
		while (k > 0 && starts.get(k) > d) k--;
		return segments.get(k).tangent(d - starts.get(k));
	}

	private float wrap(float distance) {
		float d = distance % length;
		return d < 0 ? d + length : d;
	}

	/** The position of the eye after the given distance. */
	public Vector4 eye(float distance) {
		Vector4 p = pointAt(distance);
		p.setZ(EYE_HEIGHT);
		return p;
	}

	/**
	 * The point the camera looks at: ahead on the path, or towards a point of interest. The weight of a glance rises
	 * smoothly from 0 to 1 over GLANCE_EASE meters, and goes down the same way at its end.
	 */
	public Vector4 target(float distance) {
		Vector4 ahead = pointAt(distance + LOOK_AHEAD);
		ahead.setZ(LOOK_HEIGHT);
		float d = wrap(distance);
		for (Glance glance : glances) {
			float weight = smoothstep(glance.from, glance.from + GLANCE_EASE, d) * (1 - smoothstep(glance.to, glance.to + GLANCE_EASE, d));
			if (weight <= 0) continue;
			// Blend the directions rather than the points: the point of interest may be much farther or nearer than the
			// point ahead
			Vector4 eye = eye(distance);
			Vector3 toAhead = new Vector3(eye, ahead).normalize(), toGlance = new Vector3(eye, glance.target).normalize();
			Vector3 direction = toAhead.times(1 - weight).plus(toGlance.times(weight));
			return eye.plus(direction.times(LOOK_AHEAD));
		}
		return ahead;
	}

	/**
	 * The up vector of the camera: the vertical, rolled towards the inside of the turns (see FlightPath.up() in
	 * TempleRuins). The turn rate is the change of the direction of the path per meter.
	 */
	public Vector4 up(float distance) {
		float d = 1f;
		Vector3 before = tangentAt(distance - d), after = tangentAt(distance + d);
		float turn = (float) Math.atan2(before.getX() * after.getY() - before.getY() * after.getX(), before.dot(after)) / (2 * d);
		float roll = Math.max(-MAX_BANK, Math.min(MAX_BANK, BANK_PER_TURN * turn));
		Vector3 lineOfSight = new Vector3(eye(distance), target(distance)).normalize();
		return new Rotation(-roll, lineOfSight).times(Vector4.zAxis());
	}

	static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
		return t * t * (3 - 2 * t);
	}
}
