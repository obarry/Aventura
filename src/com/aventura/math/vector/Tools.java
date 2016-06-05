package com.aventura.math.vector;

/*
 * Vector and Matrix tools
 * 
 * @author Bricolage Olivier
 * @since June 2016
 * 
 */
public class Tools {
	
	public static Vector4 interpolate(Vector4 A, Vector4 B, double t) {
		
		Vector4 P = new Vector4();
		
		P = A.times(1-t).plus(B.times(t));
		
		return P;
	}
	

}
