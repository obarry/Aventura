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
 * same direction: it has no corner, but one, in the north junction, where the visitor stops in front of the door and
 * turns on the spot (see {@link Stop}). It follows the middle of the corridors, and turns in the junctions on arcs of
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
 * <li>In front of the north door, the visitor slows down and stops; the camera turns to the door, which opens on the
 *     store, then turns back, and the visit goes on while the door closes ({@link Stop}).</li>
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

	/**
	 * A stop of the visit: the visitor slows down, stops at a distance of the path, turns to a point of interest while a
	 * door opens, looks, turns back, and walks on, faster and faster, while the door closes behind. The stop is described
	 * in time, from the moment the visitor stands still:
	 * <pre>
	 *   0 ---- TURN ---- OPEN (the door opens) ---- HOLD ---- TURN (back to the path) ----&gt; walks on, the door closes
	 * </pre>
	 * Around it, the walk slows down and speeds up again over EASE_DISTANCE meters (see speedFactor()).
	 * <p>
	 * The stop is a corner of the path: the visitor arrives along one segment and leaves along another one, turning on the
	 * spot. The look turns by angles (yaw around Z, pitch above the horizontal), at a regular pace: from the direction of
	 * arrival to the point of interest (the shortest way), then from the point of interest back past the direction of
	 * arrival to the direction of departure. A visitor coming from the east, the door on his right, turns right to the
	 * door, then left to leave southwards: never a turn on himself.
	 */
	public static class Stop {
		public static final float TURN_SECONDS = 1.5f, OPEN_SECONDS = 2.5f, HOLD_SECONDS = 2f;
		public static final float DURATION = TURN_SECONDS + OPEN_SECONDS + HOLD_SECONDS + TURN_SECONDS;
		/** The speed goes down to this share of the speed of the visit, at the stop. */
		public static final float MIN_SPEED_FACTOR = 0.12f;
		public static final float EASE_DISTANCE = 2.5f;

		final float distance;
		final Vector4 target;
		// The way the visitor arrives and the way he leaves: the stop is the corner between two segments of the path
		final PathSegment arrival, departure;

		Stop(float distance, Vector4 target, PathSegment arrival, PathSegment departure) {
			this.distance = distance;
			this.target = target;
			this.arrival = arrival;
			this.departure = departure;
		}

		public float getDistance() {
			return distance;
		}

		public Vector4 getTarget() {
			return target;
		}

		/** How much the camera looks at the point of interest, from 0 (along the path) to 1, at the time t of the stop. */
		public float lookWeight(float t) {
			float back = DURATION - TURN_SECONDS;
			return smoothstep(0, TURN_SECONDS, t) * (1 - smoothstep(back, DURATION, t));
		}

		/** The opening of the door, from 0 (closed) to 1, at the time t of the stop (it closes after the stop). */
		public float doorOpening(float t) {
			return smoothstep(TURN_SECONDS, TURN_SECONDS + OPEN_SECONDS, t);
		}

		/** True during the turn back to the path, at the end of the stop. */
		boolean turningBack(float t) {
			return t > DURATION - TURN_SECONDS;
		}

		/** The opening of the door at the time t after the stop, while the visitor walks on: it closes. */
		public float doorClosing(float t) {
			return 1 - smoothstep(0, OPEN_SECONDS, t);
		}
	}

	private final List<PathSegment> segments = new ArrayList<PathSegment>();
	private final List<Float> starts = new ArrayList<Float>(); // Distance at the start of each segment
	private final List<Glance> glances = new ArrayList<Glance>();
	private final Stop northDoorStop;
	private float length;

	/** The visit of the ship (see the class comment): its plan is in the constants of Ship. */
	public ShipTour() {
		float l = Ship.LOOP_HALF_LENGTH, w = Ship.LOOP_HALF_WIDTH, r = TURN_RADIUS;
		add(PathSegment.line(r, -w, l, -w));                 // 1  south corridor, eastwards
		add(PathSegment.arc(l, 0, w, -90, 180));             // 2  east bend
		add(PathSegment.line(l, w, 0, w));                   // 3  north corridor, westwards, to the middle of the junction:
		                                                     //    the stop in front of the north door, then a turn on the spot
		add(PathSegment.line(0, w, 0, -w + r));              // 4  the shortcut, through the control room
		add(PathSegment.arc(-r, -w + r, r, 0, -90));         //    turn right, westwards, in the south junction
		add(PathSegment.line(-r, -w, -l, -w));               // 5  south corridor, westwards
		add(PathSegment.arc(-l, 0, w, -90, -180));           //    west bend
		add(PathSegment.line(-l, w, -r, w));                 // 6  north corridor, eastwards
		add(PathSegment.arc(-r, w - r, r, 90, -90));         //    turn right, southwards, in the north junction
		add(PathSegment.line(0, w - r, 0, -w + r));          // 9  the shortcut again
		add(PathSegment.arc(r, -w + r, r, 180, 90));         //    turn left, eastwards, in the south junction: back to 1

		// The glances: the consoles of the control room (east, then west the second time), and the hatch of the hold at the
		// end of the airlock corridor, from the south junction
		float room1 = startOf(3) + w, room2 = startOf(9) + w - r; // Where the shortcut crosses the middle of the room
		glances.add(new Glance(room1 - 3.5f, room1 + 1f, new Vector4(4.4f, 1.5f, 1.1f, 1)));
		glances.add(new Glance(room2 - 3.5f, room2 + 1f, new Vector4(-4.4f, -1.5f, 1.1f, 1)));
		glances.add(new Glance(startOf(4) - 0.5f, startOf(4) + 2.2f, new Vector4(0, -w - Hub.SIZE / 2 - Ship.AIRLOCK_CORRIDOR_LENGTH, Ship.HATCH_HEIGHT, 1)));

		// The stop in front of the north door: in the middle of the north junction, facing the door 2 m away. The camera
		// looks through the doorway, at the back of the store
		northDoorStop = new Stop(startOf(3), new Vector4(0, w + Hub.SIZE / 2 + 2f, 1.3f, 1), segments.get(2), segments.get(3));
	}

	private void add(PathSegment segment) {
		starts.add(length);
		segments.add(segment);
		length += segment.getLength();
	}

	/** The stop in front of the north door (see Stop). */
	public Stop getNorthDoorStop() {
		return northDoorStop;
	}

	/**
	 * The share of the speed of the visit at the given distance: 1, except around the stop, where the visitor slows
	 * down to MIN_SPEED_FACTOR, then speeds up again. Never 0, or the visitor would never reach the stop.
	 */
	public float speedFactor(float distance) {
		float d = wrap(distance), s = northDoorStop.distance;
		// The distance to the stop, the visit being a loop
		float gap = Math.abs(d - s);
		gap = Math.min(gap, length - gap);
		return Stop.MIN_SPEED_FACTOR + (1 - Stop.MIN_SPEED_FACTOR) * smoothstep(0, Stop.EASE_DISTANCE, gap);
	}

	/**
	 * The point the camera looks at during the stop, at the time t of the stop (see Stop): the angles of the look
	 * (yaw and pitch) go from the direction of arrival to the point of interest, then back to the direction of departure.
	 */
	public Vector4 stopTarget(float t) {
		Stop stop = northDoorStop;
		Vector4 eye = eye(stop.distance);
		float weight = stop.lookWeight(t);
		// The three directions: arrival (along the segment of arrival), point of interest, departure (the look ahead)
		Vector3 in = stop.arrival.tangent(stop.arrival.getLength());
		Vector3 toTarget = new Vector3(eye, stop.target);
		Vector4 aheadPoint = pointAt(stop.distance + LOOK_AHEAD);
		aheadPoint.setZ(LOOK_HEIGHT);
		Vector3 out = new Vector3(eye, aheadPoint);
		float yawIn = yaw(in), pitchIn = pitch(new Vector3(eye, target(stop.distance - 1e-3f)));
		float toDoor = shortestTurn(yawIn, yaw(toTarget)), toLeave = shortestTurn(yawIn, yaw(out));
		float yaw, pitch;
		if (!stop.turningBack(t)) {
			yaw = yawIn + weight * toDoor;
			pitch = pitchIn + weight * (pitch(toTarget) - pitchIn);
		} else {
			// Back past the direction of arrival, on to the direction of departure
			yaw = yawIn + weight * toDoor + (1 - weight) * toLeave;
			pitch = pitch(out) + weight * (pitch(toTarget) - pitch(out));
		}
		float horizontal = (float) Math.cos(pitch);
		Vector3 direction = new Vector3(horizontal * (float) Math.cos(yaw), horizontal * (float) Math.sin(yaw), (float) Math.sin(pitch));
		return eye.plus(direction.times(LOOK_AHEAD));
	}

	/** Angle of a direction around Z, from the X axis (radians). */
	private static float yaw(Vector3 direction) {
		return (float) Math.atan2(direction.getY(), direction.getX());
	}

	/** Angle of a direction above the horizontal (radians). */
	private static float pitch(Vector3 direction) {
		return (float) Math.atan2(direction.getZ(), Math.hypot(direction.getX(), direction.getY()));
	}

	/** The angle to turn from one yaw to another, the shortest way: between -PI and PI. */
	private static float shortestTurn(float from, float to) {
		double turn = (to - from) % (2 * Math.PI);
		if (turn > Math.PI) turn -= 2 * Math.PI;
		if (turn <= -Math.PI) turn += 2 * Math.PI;
		return (float) turn;
	}

	/** The up vector during the stop: the vertical, made perpendicular to the line of sight (no roll). */
	public Vector4 stopUp(float t) {
		Vector3 sight = new Vector3(eye(northDoorStop.distance), stopTarget(t)).normalize();
		Vector3 up = new Vector3(0, 0, 1).minus(sight.times(sight.getZ())).normalize();
		return new Vector4(up.getX(), up.getY(), up.getZ(), 0);
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
		float d = wrap(distance), s = northDoorStop.distance;
		Vector4 ahead;
		if (d < s && d + LOOK_AHEAD > s) {
			// Before the stop the look does not turn the corner: ahead, along the segment of arrival prolonged
			PathSegment arrival = northDoorStop.arrival;
			ahead = arrival.point(arrival.getLength() - (s - d) + LOOK_AHEAD);
		} else {
			ahead = pointAt(distance + LOOK_AHEAD);
		}
		ahead.setZ(LOOK_HEIGHT);
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
		// The stop is a corner of the path, not a turn: the camera stays upright around it
		float gap = Math.abs(wrap(distance) - northDoorStop.distance);
		roll *= smoothstep(0.5f, 1.5f, Math.min(gap, length - gap));
		Vector3 lineOfSight = new Vector3(eye(distance), target(distance)).normalize();
		return new Rotation(-roll, lineOfSight).times(Vector4.zAxis());
	}

	static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
		return t * t * (3 - 2 * t);
	}
}
