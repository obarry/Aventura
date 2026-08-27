package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector3;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.material.Material;
import com.aventura.model.material.SolidMaterial;
import com.aventura.model.material.TexturedMaterial;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

/**
 * ------------------------------------------------------------------------------
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
 * NOT YET CARRIED OVER from the original Rasterizer: wireframe drawing
 * (drawTriangleLines/drawLine/Bresenham) and the full original pixel-statistics
 * set (rasterized_lines, triangles_with_pixels, etc. -- only rendered/discarded
 * pixel counts are preserved here). These were flagged early on as their own,
 * separate concerns (a WireframeRenderer class, and a RasterizerStats class)
 * for the tactical clean-up phase -- let me know if any existing caller
 * actually needs them from THIS class in the meantime.
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

	protected Camera camera;
	protected PerspectiveContext perspectiveCtx;
	protected Lighting lighting;
	protected GUIView gUIView;

	protected ZBuffer zBuffer;
	protected TriangleRasterizer triangleRasterizer;

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

		Material material = useTexture
				? new TexturedMaterial(t.getTexture(), t.getTextureOrientation(), specCol, specExp, LEGACY_AMBIENT_REFLECTIVITY)
				: new SolidMaterial(surfCol, specCol, specExp, LEGACY_AMBIENT_REFLECTIVITY);

		ShadingConsumer consumer = new ShadingConsumer(material, lighting, camera, zBuffer, gUIView, shadows);

		if (useTexture) {
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, t.getTexVec1(), t.getTexVec2(), t.getTexVec3(), consumer);
		} else {
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, consumer);
		}
	}

	public int getRenderedPixels() {
		return triangleRasterizer != null ? triangleRasterizer.getRenderedPixels() : 0;
	}

	public int getDiscardedPixels() {
		return triangleRasterizer != null ? triangleRasterizer.getDiscardedPixels() : 0;
	}
}}