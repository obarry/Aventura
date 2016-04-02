package com.aventura.math.vector;

/**
 * @author Bricolage Olivier
 * @date March 2016
 *
 */
public class Vector2 {
	
	protected double x;
	protected double y;
	
	public Vector2() {
		this.x = 0;
		this.y = 0;
	}
	
	public Vector2(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public double length() {
		return Math.sqrt(x*x+y*y);
	}
	
	public double dot(Vector2 w) {
		return x*w.x+y*w.y;
	}
	
	public void times(double d) {
		this.x = x*d;
		this.y = y*d;
	}
	
	public Vector2 equals() {
		Vector2 w = new Vector2(this.x, this.y);
		return w;
	}
	
	public static boolean equals(Vector2 v1, Vector2 v2) {
		if (v1.x == v2.x && v1.y == v2.y)  return true; else return false;
	}
	
	/**
	 * return sum of V = this vector + W the provided vector
	 * 
	 * @param w provided vector
	 * @return v+w
	 */
	public Vector2 plus(Vector2 w) {
		Vector2 s = new Vector2(x+w.x, y+w.y);
		return s;
	}
	
	/**
	 * return difference of V = this vector - W the provided vector
	 * 
	 * @param w provided vector
	 * @return v+w
	 */
	public Vector2 minus(Vector2 w) {
		Vector2 d = new Vector2(x-w.x, y-w.y);
		return d;
	}
	
	
	public void normalize() {
		double length = this.length();
		this.x = x/length;
		this.y = y/length;
	}

}
