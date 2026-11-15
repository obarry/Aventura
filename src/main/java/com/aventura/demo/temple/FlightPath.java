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

import com.aventura.math.transform.Rotation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * The path of a camera that flies over the scene like a helicopter, without any action of the user: a closed loop
 * through a list of waypoints.
 *
 * <p>A waypoint is a position of the camera (the eye) and the point it looks at there (the target). Joining the
 * waypoints by straight lines would make the camera turn abruptly at each of them. The path is a smooth curve
 * instead, a <b>Catmull-Rom spline</b>: between two waypoints P1 and P2, the curve is a cubic polynomial that goes
 * through both, and whose direction at P1 is parallel to the line from the previous waypoint P0 to P2 (and likewise
 * at P2). The curve therefore goes through every waypoint, with no change of direction there, and only needs the
 * list of points: there is no tangent to give ({@link #catmullRom}). The eye and the target each follow their own
 * spline.
 *
 * <p>The position on the path is given by the <b>distance travelled</b> since the start, in meters (the length of
 * the straight lines between the waypoints is used, which is close enough): moving this distance forward by the
 * speed times the elapsed time makes the camera fly at a constant speed, whatever the time taken to render an image.
 *
 * <p>The camera also <b>banks</b> in the turns, like an aircraft: its up vector, vertical in a straight line, leans
 * towards the inside of the turn ({@link #up}).
 *
 * @since Nov 2026
 */
public class FlightPath {

	/** The path is measured over this distance, before and after a point, to know how much it turns there (meters). */
	static final float TURN_MEASURE_DISTANCE = 6f;

	private final Vector4[] eyes, targets;
	/** distances[k]: distance travelled when reaching the waypoint k; distances[n] is the length of the whole loop. */
	private final float[] distances;

	/**
	 * @param waypoints the waypoints of the loop, each one as 6 coordinates: the eye (x, y, z), then the target
	 *                  (x, y, z). At least 4; the path goes from the last one back to the first one.
	 */
	public FlightPath(float[][] waypoints) {
		int n = waypoints.length;
		if (n < 4) throw new IllegalArgumentException("A flight path needs at least 4 waypoints: " + n);
		eyes = new Vector4[n];
		targets = new Vector4[n];
		for (int k = 0; k < n; k++) {
			float[] w = waypoints[k];
			eyes[k] = new Vector4(w[0], w[1], w[2], 1);
			targets[k] = new Vector4(w[3], w[4], w[5], 1);
		}
		distances = new float[n + 1];
		for (int k = 0; k < n; k++) {
			distances[k + 1] = distances[k] + eyes[(k + 1) % n].minus(eyes[k]).length();
		}
	}

	/** Length of the whole loop, in meters. */
	public float getLength() {
		return distances[distances.length - 1];
	}

	public int getNbWaypoints() {
		return eyes.length;
	}

	/** Position of the camera after the given distance travelled from the first waypoint (the loop is endless). */
	public Vector4 eye(float distance) {
		return pointAt(eyes, distance);
	}

	/** The point the camera looks at, after the given distance travelled. */
	public Vector4 target(float distance) {
		return pointAt(targets, distance);
	}

	/**
	 * The up vector of the camera after the given distance travelled: the vertical, rolled around the line of sight
	 * towards the inside of the turn. The roll is proportional to how fast the path turns there (the change of its
	 * horizontal direction per meter), up to maxBank.
	 *
	 * @param bankPerTurn roll, in radians, for a turn of one radian per meter
	 * @param maxBank     maximum roll, in radians
	 */
	public Vector4 up(float distance, float bankPerTurn, float maxBank) {
		float roll = Math.max(-maxBank, Math.min(maxBank, bankPerTurn * turnRate(distance)));
		Vector3 lineOfSight = target(distance).minus(eye(distance)).V3().normalize();
		// The Z axis, turned around the line of sight. Seen from behind the camera, a rotation by a negative angle
		// leans it to the left: the inside of a turn to the left (positive turn rate, hence positive roll)
		return new Rotation(-roll, lineOfSight).times(Vector4.zAxis());
	}

	/**
	 * How fast the path turns after the given distance travelled, in radians per meter: positive for a turn to the
	 * left (counter-clockwise seen from above). It is the change of the heading (the horizontal direction of the
	 * movement) between a point a little before and a point a little after.
	 */
	public float turnRate(float distance) {
		float d = TURN_MEASURE_DISTANCE;
		float before = heading(eye(distance - d), eye(distance));
		float after = heading(eye(distance), eye(distance + d));
		double change = after - before;
		// The headings are angles in ]-PI, PI]: bring their difference back to the same interval
		while (change > Math.PI) change -= 2 * Math.PI;
		while (change <= -Math.PI) change += 2 * Math.PI;
		return (float) change / d;
	}

	private static float heading(Vector4 from, Vector4 to) {
		return (float) Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
	}

	/** The point of the spline through the given points, after the given distance travelled. */
	private Vector4 pointAt(Vector4[] points, float distance) {
		int n = points.length;
		// The loop is endless: bring the distance back into one lap
		float lap = distance % getLength();
		if (lap < 0) lap += getLength();

		// The segment k, from the waypoint k to the next one, that contains this distance
		int k = 0;
		while (k < n - 1 && lap >= distances[k + 1]) k++;
		float t = (lap - distances[k]) / (distances[k + 1] - distances[k]);

		return catmullRom(points[(k + n - 1) % n], points[k], points[(k + 1) % n], points[(k + 2) % n], t);
	}

	/**
	 * Point of the Catmull-Rom spline between p1 (t = 0) and p2 (t = 1); p0 and p3 are the points before and after,
	 * which give the direction of the curve at p1 and p2. Each coordinate is the cubic polynomial
	 * <pre>
	 *   0.5 * ( 2 p1 + (p2 - p0) t + (2 p0 - 5 p1 + 4 p2 - p3) t² + (3 p1 - p0 - 3 p2 + p3) t³ )
	 * </pre>
	 * which is p1 for t = 0 and p2 for t = 1, with the derivatives (p2 - p0) / 2 and (p3 - p1) / 2 there.
	 */
	static Vector4 catmullRom(Vector4 p0, Vector4 p1, Vector4 p2, Vector4 p3, float t) {
		return new Vector4(
				catmullRom(p0.getX(), p1.getX(), p2.getX(), p3.getX(), t),
				catmullRom(p0.getY(), p1.getY(), p2.getY(), p3.getY(), t),
				catmullRom(p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), t),
				1);
	}

	static float catmullRom(float p0, float p1, float p2, float p3, float t) {
		return 0.5f * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (3 * p1 - p0 - 3 * p2 + p3) * t * t * t);
	}
}
