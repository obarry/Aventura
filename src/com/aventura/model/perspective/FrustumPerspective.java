package com.aventura.model.perspective;

import com.aventura.math.projection.FrustumProjection;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.tools.tracing.Tracer;

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
 *
 * @author Olivier BARRY
 * @since June 2024
 * 
 */

public class FrustumPerspective extends Perspective {
	
	public FrustumPerspective(Perspective persp) {
		super(persp);
		this.projection = new FrustumProjection(persp.projection);
	}
	
	public FrustumPerspective(float width, float height, float dist, float depth) {
		super(width, height, dist, depth);
		
		this.projection = new FrustumProjection(left , right, bottom, top, near, far);
		
	}
	
	public FrustumPerspective(float top, float bottom, float right, float left, float far, float near) {
		super(top, bottom, right, left, far, near);
		
		this.projection = new FrustumProjection(top , right, bottom, top, near, far);
		
	}

	public void updateProjection() {
		this.projection = new FrustumProjection(left , right, bottom, top, near, far);
	}

	@Override
	public Vector4[][] getFrustumFromEye(Camera camera) {
		
		Vector4[][] frustum = new Vector4[2][4];
		
		Vector4 eye = camera.getEye();
		// - The eye-point of interest (camera direction) normalized vector
		Vector4 fwd = camera.getForward().normalize();
		
		// - The up vector and side vectors
		Vector4 up = camera.getUp();
		Vector4 side = fwd.times(up).normalize();
		// - the half width and half eight of the near plane
		float half_eight_near = this.height/2;
		float half_width_near = this.width/2;
		
		// For a Frustum perspective : calculate the width and height on far plane using Thales: knowing that width and height are defined on the near plane
		float half_height_far = half_eight_near * this.far/this.near; // height_far = height_near * far/near
		float half_width_far = half_width_near * this.far/this.near; // width_far = width_near * far/near
		
		// Calculate all 8 points, vertices of the GUIView Frustum
		// TODO : later, this calculation could be done and points provided through methods in the "Frustum" class or any class
		// directly related to the gUIView Frustum
		// P11 = Eye + cam_dir*near + up*half_height_near + side*half_width_near
		frustum[0][0] = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).plus(side.times(half_width_near));
		// P12 = Eye + cam_dir*near + up*half_height_near - side*half_width_near
		frustum[0][1] = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P13 = Eye + cam_dir*near - up*half_height_near - side*half_width_near
		frustum[0][2] = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P14 = Eye + cam_dir*near - up*half_height_near + side*half_width_near
		frustum[0][3] = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).plus(side.times(half_width_near));
		//
		// P21 = Eye + cam_dir*far + up*half_height_far + side*half_width_far
		frustum[1][0] = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).plus(side.times(half_width_far));
		// P22 = Eye + cam_dir*far + up*half_height_far - side*half_width_far
		frustum[1][1] = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P23 = Eye + cam_dir*far - up*half_height_far - side*half_width_far
		frustum[1][2] = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P24 = Eye + cam_dir*far - up*half_height_far + side*half_width_far
		frustum[1][3] = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).plus(side.times(half_width_far));
		
		String s = "";
		for (int i = 0; i<2; i++) {
			for (int j = 0; j<4; j++) {
				s = s + "frustum [" + i + "," + j + "] = " + frustum[i][j] + "\n";
			}
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Frustum : \n"+s);

		return frustum;
	}
	
	public String toString() {
		String p = "***** Frustum Perspective *****\n";
		p += super.toString();
		p += "*******************************\n";
				
		return p;
	}

}
