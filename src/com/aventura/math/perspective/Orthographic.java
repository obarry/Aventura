package com.aventura.math.perspective;

/*
 * Orthographic Perspective Matrix
 * 
 * @author Bricolage Olivier
 * @since June 2016
 * 
 */
public class Orthographic extends Perspective {
	
	Orthographic(double left, double right, double bottom, double top, double near, double far) {
		
		double[][] array = { { 1/(right-left)       , 0.0            ,  0.0          , (right+left)/(right-left) },
				 			 { 0.0                  , 2/(top-bottom) ,  0.0          , (bottom+top)/(bottom-top) },
				 			 { 0.0                  , 0.0            ,  2/(near-far) , near+far/(far-near)       },
				 			 { 0.0                  , 0.0            ,  0.0          , 1.0                       } };
		
		try {
			this.setArray(array);
		} catch (Exception e) {
			// Should never happen
		}
						
	}

}
