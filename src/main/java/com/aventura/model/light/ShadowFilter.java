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
package com.aventura.model.light;

/**
 * How a ShadowingLight filters its shadow map when it decides how much a point is shadowed.
 * <ul>
 * <li>HARD: one depth comparison per point, so the shadow factor is 0 or 1 and the edges of a shadow
 * are sharp, with visible steps at the resolution of the map. This is the default, and the behavior
 * of every version before the soft shadows were added.</li>
 * <li>PCF_3X3: percentage-closer filtering. The depth of each texel of the 3x3 block around the point is
 * compared with the depth of the point, and the shadow factor is the fraction of the texels that see the
 * point lit, each comparison being weighted bilinearly so that the factor varies continuously when the
 * point moves. It goes smoothly from 0 to 1 across the edge of a shadow, which becomes a penumbra two or three
 * texels wide, without stair-steps. The filtering is done on the results of the comparisons, never on the
 * depths or on the final image. It costs a few times more lookups of the map than HARD.</li>
 * </ul>
 *
 * @author Olivier BARRY
 * @since 2026
 */
public enum ShadowFilter {

	HARD(0),
	PCF_3X3(1);

	private final int radius;

	private ShadowFilter(int radius) {
		this.radius = radius;
	}

	/**
	 * @return the number of texels sampled on each side of the central one: 0 for HARD, 1 for PCF_3X3.
	 *         The filter compares (2 * radius + 1)^2 texels.
	 */
	public int getRadius() {
		return radius;
	}
}
