package com.aventura.math.perspective;

import com.aventura.math.vector.Matrix4;

public class Frustum extends Matrix4 {
	
	Frustum(double left, double right, double bottom, double top, double near, double far) {
		
		double[][] array = { { (2-near)/(right-left),  0.0,                  (right+left)/(right-left), 0.0                   },
				 			 { 0.0                  , (2-near)/(top-bottom), (top+bottom)/(top-bottom), 0.0                   },
				 			 { 0.0                  ,  0.0,                  (near+far)/(near-far)    , 2*near*far/(near-far) },
				 			 { 0.0                  ,  0.0,                  -1.0                     , 0.0                   } };
		
		try {
			super.setArray(array);
		} catch (Exception e) {
			// Should never happen
		}
						
	}

}
