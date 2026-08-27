package com.aventura.engine;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Tools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------
 * TriangleRasterizer is the pure geometric core of the rendering pipeline: it
 * knows how to walk the pixels covered by a triangle on screen, interpolate
 * per-vertex attributes (world position, normal, texture coordinates) with
 * perspective correction, test depth against a ZBuffer, and hand each
 * surviving pixel to a FragmentConsumer.
 *
 * It deliberately knows NOTHING about lighting, materials, or shadows — that
 * is entirely the responsibility of whichever FragmentConsumer is passed in.
 * This same class is used both for normal rendering (with a ShadingConsumer)
 * and for shadow map generation (with a DepthOnlyConsumer): the only
 * difference is which consumer is supplied.
 *
 * It also does not know or care whether a triangle is flat-shaded or
 * smooth-shaded: the caller decides which 3 normals to pass in (either each
 * vertex's own normal for smooth shading, or the triangle's single face
 * normal repeated 3 times for flat shading) — interpolating 3 identical
 * values naturally produces a constant result, so no special case is needed
 * here.
 *
 * @author Olivier BARRY
 * @since 2026 (replaces the rasterizeTriangle/rasterizeScanLine pair on the
 *              legacy Rasterizer)
 *
 */
public class TriangleRasterizer {

	private final PerspectiveContext perspectiveCtx;
	private final ZBuffer zBuffer;

	// Reused across every pixel of every triangle rasterized by this instance.
	// See Fragment's class-level lifecycle contract.
	private final Fragment fragment = new Fragment();

	// Minimal pixel statistics for now; extended in the tactical clean-up phase.
	private int renderedPixels = 0;
	private int discardedPixels = 0;

	public TriangleRasterizer(PerspectiveContext perspectiveCtx, ZBuffer zBuffer) {
		this.perspectiveCtx = perspectiveCtx;
		this.zBuffer = zBuffer;
	}

	public int getRenderedPixels() {
		return renderedPixels;
	}

	public int getDiscardedPixels() {
		return discardedPixels;
	}

	/**
	 * Rasterizes a triangle without texture (e.g. shadow map depth-only pass, or an untextured
	 * material). Equivalent to calling the full overload with a null texture coordinate for
	 * all 3 corners.
	 */
	public void rasterize(Triangle t, Vector3 normal1, Vector3 normal2, Vector3 normal3, FragmentConsumer consumer) {
		rasterize(t, normal1, normal2, normal3, null, null, null, consumer);
	}

	/**
	 * Rasterizes a triangle, producing one Fragment per covered, depth-test-passing pixel and
	 * handing it to consumer.
	 *
	 * @param t				the triangle to rasterize (used for screen/world vertex positions and texture)
	 * @param normal1..3	per-corner normals to interpolate; pass each vertex's own normal for
	 *						smooth shading, or the same face normal 3 times for flat shading —
	 *						this class does not distinguish between the two cases
	 * @param texCoord1..3	per-corner homogeneous texture coordinates (as stored on Triangle), or
	 *						null (all three) if this triangle has no texture. Interpolated here with
	 *						perspective correction but left UN-divided by W — Fragment exposes the
	 *						raw (u, v, w), and it's up to Material (which knows the texture's
	 *						orientation) to apply the right projective divide. This class has no
	 *						notion of texture orientation at all.
	 * @param consumer		receives one Fragment per surviving pixel
	 */
	public void rasterize(
			Triangle t,
			Vector3 normal1, Vector3 normal2, Vector3 normal3,
			Vector4 texCoord1, Vector4 texCoord2, Vector4 texCoord3,
			FragmentConsumer consumer) {

		if (Tracer.debug) Tracer.traceDebug(this.getClass(), "Rasterizing triangle.");

		RasterVertex a = new RasterVertex(t.getV1(), normal1, texCoord1);
		RasterVertex b = new RasterVertex(t.getV2(), normal2, texCoord2);
		RasterVertex c = new RasterVertex(t.getV3(), normal3, texCoord3);

		RasterVertex[] ordered = sortByScreenY(a, b, c);
		RasterVertex v1 = ordered[0];
		RasterVertex v2 = ordered[1];
		RasterVertex v3 = ordered[2];

		float dP1P2, dP1P3;

		if (yScreen(v2.vertex) - yScreen(v1.vertex) > 0) {
			dP1P2 = (xScreen(v2.vertex) - xScreen(v1.vertex)) / (yScreen(v2.vertex) - yScreen(v1.vertex));
		} else { // horizontal segment, infinite invert slope
			dP1P2 = Float.MAX_VALUE;
		}

		if (yScreen(v3.vertex) - yScreen(v1.vertex) > 0) {
			dP1P3 = (xScreen(v3.vertex) - xScreen(v1.vertex)) / (yScreen(v3.vertex) - yScreen(v1.vertex));
		} else { // horizontal segment, infinite invert slope
			dP1P3 = Float.MAX_VALUE;
		}

		if (dP1P2 > dP1P3) {
			for (int y = (int) yScreen(v1.vertex); y <= (int) yScreen(v3.vertex); y++) {
				if (y < yScreen(v2.vertex)) {
					rasterizeScanLine(y, v1, v3, v1, v2, consumer);
				} else {
					rasterizeScanLine(y, v1, v3, v2, v3, consumer);
				}
			}
		} else {
			for (int y = (int) yScreen(v1.vertex); y <= (int) yScreen(v3.vertex); y++) {
				if (y < yScreen(v2.vertex)) {
					rasterizeScanLine(y, v1, v2, v1, v3, consumer);
				} else {
					rasterizeScanLine(y, v2, v3, v1, v3, consumer);
				}
			}
		}
	}

	/**
	 * Rasterizes one horizontal scan line of pixels, from segment [va, vb] to segment [vc, vd] —
	 * same geometry as the legacy rasterizeScanLine, minus every bit of lighting/texture-color/
	 * shadow logic, which now lives entirely in whatever FragmentConsumer is supplied.
	 */
	private void rasterizeScanLine(int y, RasterVertex va, RasterVertex vb, RasterVertex vc, RasterVertex vd, FragmentConsumer consumer) {

		if (!isInScreenY(y)) {
			return;
		}

		float ya = yScreen(va.vertex), yb = yScreen(vb.vertex);
		float yc = yScreen(vc.vertex), yd = yScreen(vd.vertex);
		float xa = xScreen(va.vertex), xb = xScreen(vb.vertex);
		float xc = xScreen(vc.vertex), xd = xScreen(vd.vertex);

		float gradient1 = ya != yb ? (y - ya) / (yb - ya) : 1;
		float gradient2 = yc != yd ? (y - yc) / (yd - yc) : 1;

		int sx = (int) Tools.interpolate(xa, xb, gradient1);
		int ex = (int) Tools.interpolate(xc, xd, gradient2);

		int smin = (int) Math.min(xa, xb), smax = (int) Math.max(xa, xb);
		int emin = (int) Math.min(xc, xd), emax = (int) Math.max(xc, xd);
		if (sx < smin) sx = smin;
		if (sx > smax) sx = smax;
		if (ex < emin) ex = emin;
		if (ex > emax) ex = emax;

		if (sx == ex) {
			return; // No pixel would be drawn on this line
		}

		boolean frustum = perspectiveCtx.getPerspectiveType() == PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM;

		// Depth at the 4 corners of this scan line's two edges.
		// Frustum: W of the projected position (= -Z in camera space before projection).
		// Orthographic: Z directly (W is always 1 in an orthographic projection).
		float za = frustum ? va.vertex.getProjPos().getW() : va.vertex.getProjPos().getZ();
		float zb = frustum ? vb.vertex.getProjPos().getW() : vb.vertex.getProjPos().getZ();
		float zc = frustum ? vc.vertex.getProjPos().getW() : vc.vertex.getProjPos().getZ();
		float zd = frustum ? vd.vertex.getProjPos().getW() : vd.vertex.getProjPos().getZ();

		float z1 = frustum ? 1 / Tools.interpolate(1 / za, 1 / zb, gradient1) : Tools.interpolate(za, zb, gradient1);
		float z2 = frustum ? 1 / Tools.interpolate(1 / zc, 1 / zd, gradient2) : Tools.interpolate(zc, zd, gradient2);

		// Perspective-correction weight for attribute interpolation: under Frustum projection,
		// attributes must be divided by z at the edges then multiplied back by the pixel's
		// interpolated z (hyperbolic interpolation), otherwise textures/shading warp incorrectly.
		// Under Orthographic projection there is no foreshortening, so the weight is simply 1
		// (no correction) — unlike the legacy code, which applied the same z-weighting formula to
		// both cases; see the note in the accompanying message about that latent inconsistency.
		float wa = frustum ? 1 / za : 1;
		float wb = frustum ? 1 / zb : 1;
		float wc = frustum ? 1 / zc : 1;
		float wd = frustum ? 1 / zd : 1;

		Vector4 worldEdge1 = Tools.interpolate(va.vertex.getWorldPos().times(wa), vb.vertex.getWorldPos().times(wb), gradient1);
		Vector4 worldEdge2 = Tools.interpolate(vc.vertex.getWorldPos().times(wc), vd.vertex.getWorldPos().times(wd), gradient2);

		Vector3 normalEdge1 = lerpV3(va.normal.times(wa), vb.normal.times(wb), gradient1);
		Vector3 normalEdge2 = lerpV3(vc.normal.times(wc), vd.normal.times(wd), gradient2);

		boolean hasTexture = va.texCoord != null;
		Vector4 texEdge1 = null, texEdge2 = null;
		if (hasTexture) {
			texEdge1 = Tools.interpolate(va.texCoord.times(wa), vb.texCoord.times(wb), gradient1);
			texEdge2 = Tools.interpolate(vc.texCoord.times(wc), vd.texCoord.times(wd), gradient2);
		}

		int startX = Math.min(sx, ex);
		int endX = Math.max(sx, ex);

		for (int x = startX; x < endX; x++) {

			if (!isInScreenX(x)) {
				continue;
			}

			float gradient = (float) (x - sx) / (float) (ex - sx);

			float z = frustum
					? 1 / Tools.interpolate(1 / z1, 1 / z2, gradient)
					: Tools.interpolate(z1, z2, gradient);

			// Depth test first, before any interpolation work below, so a discarded pixel costs
			// as little as possible.
			if (!zBuffer.test(x, y, z)) {
				discardedPixels++;
				continue;
			}

			float wPixel = frustum ? z : 1; // undoes the /z weighting above; no-op under Orthographic

			Vector4 worldPos = Tools.interpolate(worldEdge1, worldEdge2, gradient).times(wPixel);
			Vector3 normal = lerpV3(normalEdge1, normalEdge2, gradient).times(wPixel);

			fragment.setScreen(x, y, z);
			fragment.setWorldPosition(worldPos.getX(), worldPos.getY(), worldPos.getZ());
			fragment.setNormal(normal.getX(), normal.getY(), normal.getZ());

			if (hasTexture) {
				// Left un-divided on purpose: Fragment exposes raw (u, v, w) and it is up to
				// Material (TexturedMaterial) to apply the projective divide appropriate to this
				// triangle's texture orientation — this class has no notion of it.
				Vector4 tex = Tools.interpolate(texEdge1, texEdge2, gradient).times(wPixel);
				fragment.setTexCoord(tex.getX(), tex.getY(), tex.getW());
			} else {
				fragment.clearTexCoord();
			}

			consumer.consume(fragment);
			renderedPixels++;
		}
	}

	//
	// Small helpers
	//

	/** Linear interpolation for Vector3, mirroring Tools.interpolate's Vector4 overload.
	 *  ASSUMPTION: Vector3 exposes plus()/minus()/times(float) — adjust if the real API differs. */
	private Vector3 lerpV3(Vector3 a, Vector3 b, float gradient) {
		return a.plus(b.minus(a).times(gradient));
	}

	private float xScreen(Vertex v) {
		return v.getProjPos().get3DX() * perspectiveCtx.getPixelHalfWidth();
	}

	private float yScreen(Vertex v) {
		return v.getProjPos().get3DY() * perspectiveCtx.getPixelHalfHeight();
	}

	private boolean isInScreenX(int x) {
		return Math.abs(x) <= perspectiveCtx.getPixelHalfWidth();
	}

	private boolean isInScreenY(int y) {
		return Math.abs(y) <= perspectiveCtx.getPixelHalfHeight();
	}

	/**
	 * Sorts 3 corners by ascending screen Y (v1 lowest Y, v3 highest Y), preserving the exact
	 * comparison structure of the legacy vertex-ordering logic.
	 */
	private RasterVertex[] sortByScreenY(RasterVertex a, RasterVertex b, RasterVertex c) {

		float ya = yScreen(a.vertex);
		float yb = yScreen(b.vertex);
		float yc = yScreen(c.vertex);

		if (yb < ya) {
			if (yc < yb) {
				return new RasterVertex[] { c, b, a };
			} else {
				if (yc < ya) {
					return new RasterVertex[] { b, c, a };
				} else {
					return new RasterVertex[] { b, a, c };
				}
			}
		} else {
			if (yc < ya) {
				return new RasterVertex[] { c, a, b };
			} else {
				if (yc < yb) {
					return new RasterVertex[] { a, c, b };
				} else {
					return new RasterVertex[] { a, b, c };
				}
			}
		}
	}

	/** Bundles one triangle corner's geometry with the attributes the caller wants interpolated. */
	private static class RasterVertex {
		final Vertex vertex;
		final Vector3 normal;
		final Vector4 texCoord; // null if this triangle has no texture

		RasterVertex(Vertex vertex, Vector3 normal, Vector4 texCoord) {
			this.vertex = vertex;
			this.normal = normal;
			this.texCoord = texCoord;
		}
	}
}