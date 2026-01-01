package com.aventura.model.perspective;

import com.aventura.math.projection.OrthographicProjection;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;

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

public class OrthographicPerspective extends Perspective {
	
	public OrthographicPerspective(Perspective persp) {
		super(persp);
		this.projection = new OrthographicProjection(persp.projection);
	}
	public OrthographicPerspective(float width, float height, float dist, float depth) {
		super(width, height, dist, depth);
		
		this.projection = new OrthographicProjection(left , right, bottom, top, near, far);
		
	}

	public OrthographicPerspective(float left, float right, float bottom, float top, float near, float far) {
		super(top, bottom, right, left, far, near);
		
		this.projection = new OrthographicProjection(left , right, bottom, top, near, far);
		
	}

	public void updateProjection() {
		this.projection = new OrthographicProjection(left , right, bottom, top, near, far);
	}
	@Override
	public Vector4[][] getFrustumFromEye(Camera camera) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String toString() {
		String p = "***** Orthographic Perspective *****\n";
		p += super.toString();
		p += "************************************\n";
				
		return p;
	}

}
