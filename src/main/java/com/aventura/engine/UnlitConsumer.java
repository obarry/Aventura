package com.aventura.engine;

import com.aventura.model.material.Material;
import com.aventura.view.GUIView;

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
 * FragmentConsumer without any lighting: each fragment gets the Material's base color as is (the surface
 * color, or the texture sample tinted by it for a TexturedMaterial), then the ZBuffer is updated.
 * 
 * Used by the UNLIT rendering type (base color or texture) and by the MONOCHROME rendering type (a
 * SolidMaterial of the single fill color). Only Fragment.getScreenX()/getScreenY()/getZ() and the texture
 * coordinates are read, so the triangle can be rasterized without normals (no normal or world position
 * interpolation, see TriangleRasterizer).
 * 
 * @author Olivier BARRY
 * @since September 2026
 */
public class UnlitConsumer implements FragmentConsumer {

	private final Material material;
	private final ZBuffer zBuffer;
	private final GUIView view;

	public UnlitConsumer(Material material, ZBuffer zBuffer, GUIView view) {
		this.material = material;
		this.zBuffer = zBuffer;
		this.view = view;
	}

	@Override
	public void consume(Fragment fragment) {
		view.drawPixel(fragment.getScreenX(), fragment.getScreenY(), material.baseColorAt(fragment));
		zBuffer.update(fragment.getScreenX(), fragment.getScreenY(), fragment.getZ());
	}
}
