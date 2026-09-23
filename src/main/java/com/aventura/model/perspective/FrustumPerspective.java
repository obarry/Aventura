package com.aventura.model.perspective;

import com.aventura.math.projection.FrustumProjection;

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
 * Frustum (perspective projection): the view window grows linearly with the distance to the eye (Thales).
 *
 * @author Olivier BARRY
 * @since June 2024
 * 
 */

public class FrustumPerspective extends Perspective {
	
	public FrustumPerspective(Perspective persp) {
		super(persp);
		this.projection = new FrustumProjection(left , right, bottom, top, near, far);
	}
	
	/**
	 * Create a symmetric FrustumPerspective from its near plane window size, near distance and depth.
	 * @param width width of the near plane window
	 * @param height height of the near plane window
	 * @param dist distance from the eye to the near plane
	 * @param depth distance from the near plane to the far plane
	 */
	public FrustumPerspective(float width, float height, float dist, float depth) {
		super(width, height, dist, depth);
		this.projection = new FrustumProjection(left , right, bottom, top, near, far);
	}
	
	/**
	 * Create a FrustumPerspective from its six bounds (possibly asymmetric).
	 * @param left left bound of the near plane
	 * @param right right bound of the near plane
	 * @param bottom bottom bound of the near plane
	 * @param top top bound of the near plane
	 * @param near distance to the near plane
	 * @param far distance to the far plane
	 */
	public FrustumPerspective(float left, float right, float bottom, float top, float near, float far) {
		super(left, right, bottom, top, near, far);
		this.projection = new FrustumProjection(left , right, bottom, top, near, far);
	}

	@Override
	public PerspectiveType getType() {
		return PerspectiveType.FRUSTUM;
	}

	@Override
	public Perspective copy() {
		return new FrustumPerspective(this);
	}

	@Override
	protected float windowScaleAt(float d) {
		return d / near;
	}

	@Override
	public void updateProjection() {
		this.projection = new FrustumProjection(left , right, bottom, top, near, far);
	}
	
	public String toString() {
		String p = "***** Frustum Perspective *****\n";
		p += super.toString();
		p += "*******************************\n";
				
		return p;
	}

}
