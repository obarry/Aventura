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

}
