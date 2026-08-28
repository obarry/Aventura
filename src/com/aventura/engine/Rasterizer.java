package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector3;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.material.Material;
import com.aventura.model.material.SolidMaterial;
import com.aventura.model.material.TexturedMaterial;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

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
 * COMPATIBILITY FAÇADE. Keeps the exact public API of the original Rasterizer
 * (same constructors, same rasterizeTriangle(...) signature) so that existing
 * callers (RenderEngine, test apps) keep compiling and behaving the same way,
 * while internally delegating to the new pipeline: TriangleRasterizer +
 * Material (SolidMaterial/TexturedMaterial) + ShadingConsumer/DepthOnlyConsumer.
 *
 * This class carries no rasterization logic of its own beyond translating the
 * old (surfCol, specExp, specCol, interpolate, texture, shadows, shadowmap)
 * parameter soup into the new objects. Once RenderEngine and any remaining
 * callers are migrated to call TriangleRasterizer directly with an explicit
 * Material, this façade can be deleted.
 *
 * NOT YET CARRIED OVER from the original Rasterizer: the full original pixel-
 * statistics set beyond what's listed below (rasterized_lines is not tracked
 * by the new pipeline, so triangles_with_lines in renderStats() below is
 * always 0 -- diagnostics-only, no functional impact). drawTriangleLines/
 * drawLine/Bresenham ARE carried over unchanged (byte-for-byte port from the
 * legacy code) purely so RenderEngine keeps compiling as-is; they still don't
 * go through the new pipeline and remain flagged for extraction into their
 * own WireframeRenderer class in the tactical clean-up phase.
 *
 * @author Olivier BARRY
 * @since 2026 (façade over the new Fragment-based pipeline)
 *
 */
public class Rasterizer {

	// Ambient reflectivity has no equivalent parameter in the legacy signature -- the legacy
	// computeAmbientColor() used the full surface color with no separate Ka coefficient, i.e. an
	// implicit ambient reflectivity of 1. Kept as a named constant for clarity rather than a
	// magic number.
	private static final float LEGACY_AMBIENT_REFLECTIVITY = 1f;

	// Ported unchanged from the legacy Rasterizer: an Element/Triangle with no explicit specular
	// color falls back to white rather than leaving specularColorAt() return null downstream.
	private static final Color DEFAULT_SPECULAR_COLOR = Color.WHITE;

	protected Camera camera;
	protected PerspectiveContext perspectiveCtx;
	protected Lighting lighting;
	protected GUIView gUIView;

	protected ZBuffer zBuffer;
	protected TriangleRasterizer triangleRasterizer;

	// Diagnostics-only triangle counters for renderStats(), ported unchanged from the legacy
	// Rasterizer (see its comment above about triangles_with_lines).
	protected int rendered_triangles = 0;
	protected int triangles_with_lines = 0;
	protected int triangles_with_pixels = 0;

	/**
	 * Creation of Rasterizer with requested references for run time.
	 * @param camera : a pointer to the Camera created offline by user
	 * @param graphic : a pointer to the PerspectiveContext created offline by user
	 * @param lighting : a pointer to the Lighting system created offline by user
	 */
	public Rasterizer(Camera camera, PerspectiveContext graphic, Lighting lighting) {
		this.camera = camera;
		this.perspectiveCtx = graphic;
		this.lighting = lighting;
	}

	/**
	 * Creation of minimal Rasterizer, e.g. for shadow map rendering (no Lighting needed).
	 */
	public Rasterizer(Camera camera, PerspectiveContext graphic) {
		this.camera = camera;
		this.perspectiveCtx = graphic;
		this.lighting = null;
	}

	public void setView(GUIView v) {
		this.gUIView = v;
	}

	/** Allocates the ZBuffer for a full frame, sized from this Rasterizer's PerspectiveContext. */
	public MapView initZBuffer() {
		int width = 2 * perspectiveCtx.getPixelHalfWidth() + 1;
		int height = 2 * perspectiveCtx.getPixelHalfHeight() + 1;
		return initZBuffer(width, height, perspectiveCtx.getPerspective().getFar());
	}

	public MapView initZBuffer(int width, int height, float zBuffer_init) {
		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		this.zBuffer = new ZBuffer(width, height, halfWidth, halfHeight, zBuffer_init);
		this.triangleRasterizer = new TriangleRasterizer(perspectiveCtx, zBuffer);
		return zBuffer.getMapView();
	}

	/**
	 * Triangle rasterization and zBuffering — legacy entry point, unchanged signature.
	 *
	 * @param t				the triangle to render
	 * @param surfCol		the base surface color of the triangle, may be inherited from the element or world (default)
	 * @param specExp		the specular exponent of the Element
	 * @param specCol		the specular color of the Element
	 * @param interpolate	a boolean to indicate if interpolation of colors is activated (true) or not (false)
	 * @param texture		a boolean to indicate if texture processing is activated (true) or not (false)
	 * @param shadows		a boolean to indicate if shadowing is enabled (true) or not (false)
	 * @param shadowmap		a boolean to indicate whether this is a rasterization only for a shadow map (true) or not (false).
	 **/
	public void rasterizeTriangle(
			Triangle t,
			Color surfCol,
			float specExp,
			Color specCol,
			boolean interpolate,
			boolean texture,
			boolean shadows,
			boolean shadowmap) {

		if (Tracer.debug) Tracer.traceDebug(this.getClass(), "Rasterize triangle. Color: " + surfCol);

		if (triangleRasterizer == null) {
			if (Tracer.error) Tracer.traceError(this.getClass(), "rasterizeTriangle() called before initZBuffer()");
			return;
		}

		// Legacy per-call stat granularity (the old Rasterizer reset its counters at the top of
		// every rasterizeTriangle() call) -- see TriangleRasterizer.resetStats()'s Javadoc.
		triangleRasterizer.resetStats();

		// Resolve which 3 normals to interpolate -- this is the exact isTriangleNormal/interpolate
		// logic from the legacy rasterizeTriangle(); TriangleRasterizer itself doesn't need to know
		// about this distinction (see its class Javadoc).
		Vector3 normal1, normal2, normal3;
		if (!interpolate || t.isTriangleNormal()) {
			Vector3 flat = t.getWorldNormal();
			normal1 = flat;
			normal2 = flat;
			normal3 = flat;
		} else {
			normal1 = t.getV1().getWorldNormal();
			normal2 = t.getV2().getWorldNormal();
			normal3 = t.getV3().getWorldNormal();
		}

		if (shadowmap) {
			// Depth-only pass: Material/Lighting are irrelevant, TriangleRasterizer just needs
			// something to interpolate for normal1..3, which the DepthOnlyConsumer will ignore.
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, new DepthOnlyConsumer(zBuffer));
			return;
		}

		boolean useTexture = texture && t.getTexture() != null;

		// surfCol (D) is ALWAYS used, even in textured mode -- it tints the texture sample (D*T),
		// it is never replaced by it. See TexturedMaterial's Javadoc. Only SolidMaterial needs a
		// non-null fallback (Color.WHITE) since it has no texture to fall back on if surfCol is null.
		// specCol falls back to DEFAULT_SPECULAR_COLOR (white), exactly like the legacy
		// computeSpecularColor()'s "Color spc = sc == null ? DEFAULT_SPECULAR_COLOR : sc;".
		Color effectiveSpecCol = specCol != null ? specCol : DEFAULT_SPECULAR_COLOR;

		Material material = useTexture
				? new TexturedMaterial(t.getTexture(), t.getTextureOrientation(), surfCol, effectiveSpecCol, specExp, LEGACY_AMBIENT_REFLECTIVITY)
				: new SolidMaterial(surfCol != null ? surfCol : Color.WHITE, effectiveSpecCol, specExp, LEGACY_AMBIENT_REFLECTIVITY);

		ShadingConsumer consumer = new ShadingConsumer(material, lighting, camera, zBuffer, gUIView, shadows);

		if (useTexture) {
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, t.getTexVec1(), t.getTexVec2(), t.getTexVec3(), consumer);
		} else {
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, consumer);
		}

		// Diagnostics-only counters, ported unchanged from the legacy Rasterizer.
		rendered_triangles++;
		if (triangleRasterizer.getRenderedPixels() > 0) triangles_with_pixels++;
	}

	public int getRenderedPixels() {
		return triangleRasterizer != null ? triangleRasterizer.getRenderedPixels() : 0;
	}

	public int getDiscardedPixels() {
		return triangleRasterizer != null ? triangleRasterizer.getDiscardedPixels() : 0;
	}

	//
	// Wireframe drawing -- ported unchanged from the legacy Rasterizer so RenderEngine keeps
	// compiling as-is. Does not go through TriangleRasterizer/Fragment at all. Flagged for
	// extraction into its own WireframeRenderer class in the tactical clean-up phase.
	//

	public void drawTriangleLines(Triangle t, Color c) {
		gUIView.setColor(c);
		drawLine(t.getV1(), t.getV2());
		drawLine(t.getV2(), t.getV3());
		drawLine(t.getV3(), t.getV1());
	}

	public void drawLine(Segment l) {
		drawLine(l.getV1(), l.getV2());
	}

	public void drawLine(Segment l, Color c) {
		drawLine(l.getV1(), l.getV2(), c);
	}

	public void drawLine(Vertex v1, Vertex v2) {
		int x1 = (int) xScreen(v1);
		int y1 = (int) yScreen(v1);
		int x2 = (int) xScreen(v2);
		int y2 = (int) yScreen(v2);
		gUIView.drawLine(x1, y1, x2, y2);
	}

	public void drawLine(Vertex v1, Vertex v2, Color c) {
		gUIView.setColor(c);
		drawLine(v1, v2);
	}

	private float xScreen(Vertex v) {
		return v.getProjPos().get3DX() * perspectiveCtx.getPixelHalfWidth();
	}

	private float yScreen(Vertex v) {
		return v.getProjPos().get3DY() * perspectiveCtx.getPixelHalfHeight();
	}

	public String renderStats() {
		return "Rasterizer - Triangles: rendered: " + rendered_triangles + ", rendered with lines: " + triangles_with_lines + ", rendered with pixels: " + triangles_with_pixels;
	}
}