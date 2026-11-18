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
package com.aventura.engine;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

/**
 * A Fragment carries the interpolated per-pixel attributes produced while a
 * TriangleRasterizer walks a triangle: screen position, depth, and — when
 * relevant to the current pass — world-space position, normal, and texture
 * coordinates.
 *
 * LIFECYCLE CONTRACT — read this before implementing a FragmentConsumer:
 * A single Fragment instance is owned and mutated in place by
 * TriangleRasterizer as it produces one fragment per pixel; no Fragment is
 * ever allocated per-pixel and no list of fragments is ever built. It is
 * passed to FragmentConsumer.consume(Fragment) by reference, and its content
 * is only valid for the duration of that call. A FragmentConsumer must NEVER
 * retain a reference to the Fragment it receives (store it in a field, put it
 * in a collection, etc.) — by the time consume() returns, TriangleRasterizer
 * may already have overwritten it for the next pixel. If a consumer needs a
 * value after consume() returns, it must copy that value out (e.g. read
 * fragment.getWorldPosition().getX() into a local float), never keep the
 * Vector3 instance itself, since it is reused and mutated too.
 *
 * This mirrors how a GPU fragment shader works: the fragment is a transient
 * computation state, not persisted data.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class Fragment {

	private int screenX;
	private int screenY;
	private float z;

	// Reused, mutated in place — see class-level lifecycle contract.
	// Vector4 (not Vector3) for worldPosition to match Light's getLightVectorAtPoint(Vector4)/
	// getIntensity(Vector4) contract, and the rest of the codebase's convention of representing
	// world-space points as homogeneous Vector4 (w = 1).
	private final Vector4 worldPosition = new Vector4();
	private final Vector3 normal = new Vector3();

	// Raw, un-divided homogeneous texture coordinates: (u, v, w) as interpolated with perspective
	// correction. Deliberately NOT divided by w here — which of u, v (or both) gets divided by w
	// depends on the triangle's texture orientation (isotropic/vertical/horizontal), a shading
	// decision that belongs to Material, not to the geometric interpolation done here.
	private boolean hasTexCoord = false;
	private float texU;
	private float texV;
	private float texW;
	// Scale of the texture on the screen at this fragment, for mipmapping: see getTexLog2PixelSize(). The first
	// value is the same for all the fragments of a triangle; the depth of the fragment gives the rest.
	private float texLog2PixelSizeAtUnitDepth;
	private boolean texScaleGrowsWithDepth;

	public int getScreenX() {
		return screenX;
	}

	public int getScreenY() {
		return screenY;
	}

	public float getZ() {
		return z;
	}

	/** World-space position of this fragment (homogeneous, w = 1). Reused instance — do not retain, read values out. */
	public Vector4 getWorldPosition() {
		return worldPosition;
	}

	/** World-space normal at this fragment. Reused instance — do not retain, read values out. */
	public Vector3 getNormal() {
		return normal;
	}

	/** True when this fragment carries valid texture coordinates (i.e. the triangle has a texture). */
	public boolean hasTexCoord() {
		return hasTexCoord;
	}

	/** Raw, un-divided U — caller (typically TexturedMaterial) decides whether/how to divide by W. */
	public float getTexU() {
		return texU;
	}

	/** Raw, un-divided V — caller (typically TexturedMaterial) decides whether/how to divide by W. */
	public float getTexV() {
		return texV;
	}

	/** Homogeneous W for the texture coordinate — used by TexturedMaterial to apply the correct
	 *  projective divide depending on the triangle's texture orientation. */
	public float getTexW() {
		return texW;
	}

	/**
	 * The size of a pixel of the screen on the textured surface at this fragment, in texture coordinates (the texture
	 * is 1 wide and 1 high), as a base 2 logarithm: -10 means that the pixel covers 1/1024 of the texture. This is
	 * what mipmapping needs to choose the level of detail of the texture (see Texture.mipmapLevel()); it is only
	 * computed when asked (a logarithm for each pixel).
	 *
	 * Under a perspective projection, a surface that goes away from the camera gets smaller on the screen: a pixel
	 * covers more and more of it. For a triangle, the area of texture covered by a pixel is exactly proportional to
	 * the cube of the depth of the fragment (see TriangleRasterizer.texLog2PixelSize()), hence the size of the pixel
	 * (a length: the square root of the area) to its power 1.5: 1.5 x log2(depth) once in logarithms. Under an
	 * orthographic projection, it is the same all over the triangle.
	 */
	public float getTexLog2PixelSize() {
		return texScaleGrowsWithDepth
				? texLog2PixelSizeAtUnitDepth + 1.5f * (float) (Math.log(z) / LN_2)
				: texLog2PixelSizeAtUnitDepth;
	}

	private static final double LN_2 = Math.log(2);

	//
	// Package-private mutators — only TriangleRasterizer writes into a Fragment.
	// A FragmentConsumer only ever reads.
	//

	void setScreen(int x, int y, float z) {
		this.screenX = x;
		this.screenY = y;
		this.z = z;
	}

	void setWorldPosition(float x, float y, float z) {
		// NOTE: assumes Vector4 exposes a mutable setter. If your Vector4/Vector3 are immutable,
		// tell me and I'll switch to allocating a new instance per pixel instead — a perf
		// trade-off worth knowing about either way.
		worldPosition.set(x, y, z, 1f);
	}

	void setNormal(float x, float y, float z) {
		normal.set(x, y, z);
	}

	void setTexCoord(float u, float v, float w) {
		this.hasTexCoord = true;
		this.texU = u;
		this.texV = v;
		this.texW = w;
	}

	void clearTexCoord() {
		this.hasTexCoord = false;
	}

	/**
	 * Sets the scale of the texture for all the fragments of a triangle (see getTexLog2PixelSize()).
	 *
	 * @param log2PixelSizeAtUnitDepth base 2 logarithm of the size of a pixel in texture coordinates: where the depth
	 *                                 would be 1 if it grows with the depth, everywhere on the triangle otherwise
	 * @param growsWithDepth           true under a perspective projection
	 */
	void setTexScale(float log2PixelSizeAtUnitDepth, boolean growsWithDepth) {
		this.texLog2PixelSizeAtUnitDepth = log2PixelSizeAtUnitDepth;
		this.texScaleGrowsWithDepth = growsWithDepth;
	}
}