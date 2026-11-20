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

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * A piece of the plan of the ship, in the horizontal XY plane: a straight line or an arc of circle. A corridor, a
 * pipe, a cable tray, the path of the visit: everything that runs along the corridors is built on a PathSegment.
 *
 * <p>A segment is described by its <b>curvilinear abscissa</b> s, the distance travelled from its start: s goes from 0
 * to {@link #getLength()}. At each s it gives a point ({@link #point}), the direction of travel ({@link #tangent}) and
 * the horizontal direction to its left ({@link #left}). With these three, a shape can be swept along the segment: a
 * point of a cross-section at (lateral, height) goes to point(s) + lateral * left(s) + height * Z (see
 * {@link SweptSection}).
 * <pre>
 *   straight line                        arc of circle (center C, radius R, from angle a0 turning by da)
 *
 *   start ------------------&gt; end              . ' end
 *          left(s) is to the left           .'
 *          of the direction of travel      :   C        da &gt; 0: counter-clockwise (turning left)
 *                                           '.          da &lt; 0: clockwise (turning right)
 *                                             ' . start
 * </pre>
 * All the points are at the altitude z = 0 (the floor): the heights are added by the shapes.
 *
 * @since Nov 2026
 */
public final class PathSegment {

	// A straight line: start and unit direction. An arc: center, radius, start angle and signed angle turned.
	private final boolean arc;
	private final float x0, y0, dx, dy;          // Line
	private final float cx, cy, radius, a0, da;  // Arc
	private final float length;

	private PathSegment(boolean arc, float x0, float y0, float dx, float dy, float cx, float cy, float radius, float a0, float da, float length) {
		this.arc = arc;
		this.x0 = x0;
		this.y0 = y0;
		this.dx = dx;
		this.dy = dy;
		this.cx = cx;
		this.cy = cy;
		this.radius = radius;
		this.a0 = a0;
		this.da = da;
		this.length = length;
	}

	/**
	 * A straight segment from (x0, y0) to (x1, y1).
	 */
	public static PathSegment line(float x0, float y0, float x1, float y1) {
		float length = (float) Math.hypot(x1 - x0, y1 - y0);
		if (length <= 0) throw new IllegalArgumentException("A line needs two different points");
		return new PathSegment(false, x0, y0, (x1 - x0) / length, (y1 - y0) / length, 0, 0, 0, 0, 0, length);
	}

	/**
	 * An arc of circle.
	 *
	 * @param cx           center
	 * @param cy           center
	 * @param radius       radius, positive
	 * @param startDegrees angle of the start point, around the center, from the X axis
	 * @param turnDegrees  angle turned: positive counter-clockwise (the path turns left), negative clockwise
	 */
	public static PathSegment arc(float cx, float cy, float radius, float startDegrees, float turnDegrees) {
		if (radius <= 0 || turnDegrees == 0) throw new IllegalArgumentException("An arc needs a radius and an angle");
		float a0 = (float) Math.toRadians(startDegrees), da = (float) Math.toRadians(turnDegrees);
		return new PathSegment(true, 0, 0, 0, 0, cx, cy, radius, a0, da, radius * Math.abs(da));
	}

	/** Length of the segment, in meters. */
	public float getLength() {
		return length;
	}

	public boolean isArc() {
		return arc;
	}

	/** Radius of an arc (0 for a line). */
	public float getRadius() {
		return radius;
	}

	/**
	 * The curvature of the segment: 0 for a line, 1 / radius for an arc that turns left (counter-clockwise), -1 / radius
	 * for an arc that turns right. A line parallel to the segment at the lateral offset l (to the left) is (1 - l *
	 * curvature) times as long: shorter on the inside of a bend, longer on the outside.
	 */
	public float curvature() {
		return arc ? Math.signum(da) / radius : 0;
	}

	/** Angle of the point at s around the center of an arc, in radians. */
	private float angleAt(float s) {
		return a0 + da * s / length;
	}

	/** The point at the distance s from the start, at z = 0 (s may be out of [0, length]: the segment is prolonged). */
	public Vector4 point(float s) {
		if (!arc) return new Vector4(x0 + dx * s, y0 + dy * s, 0, 1);
		float a = angleAt(s);
		return new Vector4(cx + radius * (float) Math.cos(a), cy + radius * (float) Math.sin(a), 0, 1);
	}

	/** The unit horizontal direction of travel at s. */
	public Vector3 tangent(float s) {
		if (!arc) return new Vector3(dx, dy, 0);
		// Derivative of the point with the angle: (-sin, cos), turned the other way for a clockwise arc
		float a = angleAt(s), sign = Math.signum(da);
		return new Vector3(-sign * (float) Math.sin(a), sign * (float) Math.cos(a), 0);
	}

	/** The unit horizontal direction to the left of the direction of travel at s: Z x tangent. */
	public Vector3 left(float s) {
		Vector3 t = tangent(s);
		return new Vector3(-t.getY(), t.getX(), 0);
	}

	/** The start point. */
	public Vector4 start() {
		return point(0);
	}

	/** The end point. */
	public Vector4 end() {
		return point(length);
	}

	/**
	 * The abscissa of the point of the segment nearest to (x, y), in [0, length].
	 */
	public float nearestAbscissa(float x, float y) {
		if (!arc) {
			float s = (x - x0) * dx + (y - y0) * dy; // Projection on the line
			return Math.max(0, Math.min(length, s));
		}
		// The angle of the point around the center, brought into the turn of the arc
		double a = Math.atan2(y - cy, x - cx);
		double fromStart = (a - a0) * Math.signum(da); // Angle travelled from the start, in the direction of the arc
		fromStart = ((fromStart % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
		double turn = Math.abs(da);
		if (fromStart > turn) {
			// Beyond the end of the arc: the nearest of the two ends (the gap of the circle is split in its middle)
			fromStart = fromStart - turn < (2 * Math.PI - turn) / 2 ? turn : 0;
		}
		return (float) (fromStart / turn * length);
	}

	/**
	 * Signed horizontal distance from (x, y) to the segment at its nearest point: positive to the left of the
	 * direction of travel.
	 */
	public float lateralOffset(float x, float y) {
		float s = nearestAbscissa(x, y);
		Vector4 p = point(s);
		Vector3 left = left(s);
		return (x - p.getX()) * left.getX() + (y - p.getY()) * left.getY();
	}

	/** The same segment, travelled the other way. */
	public PathSegment reversed() {
		if (!arc) {
			Vector4 e = end();
			return line(e.getX(), e.getY(), x0, y0);
		}
		return arc(cx, cy, radius, (float) Math.toDegrees(a0 + da), (float) -Math.toDegrees(da));
	}

	/** A part of this segment, from the abscissa s0 to s1 (s0 &lt; s1). */
	public PathSegment part(float s0, float s1) {
		if (s1 <= s0) throw new IllegalArgumentException("Empty part: " + s0 + " to " + s1);
		if (!arc) {
			Vector4 a = point(s0), b = point(s1);
			return line(a.getX(), a.getY(), b.getX(), b.getY());
		}
		return arc(cx, cy, radius, (float) Math.toDegrees(angleAt(s0)), (float) Math.toDegrees(da * (s1 - s0) / length));
	}

	@Override
	public String toString() {
		Vector4 s = start(), e = end();
		return String.format(java.util.Locale.ROOT, "%s (%.1f, %.1f) -> (%.1f, %.1f), %.1f m", arc ? "arc" : "line",
				s.getX(), s.getY(), e.getX(), e.getY(), length);
	}
}
