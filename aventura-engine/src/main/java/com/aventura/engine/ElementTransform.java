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

import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

/**
 * The mutating per-Element transformation pipeline: given a fixed view +
 * projection pair (constant for this object's lifetime) and a current Model
 * matrix (changes for every Element, potentially every frame), computes each
 * Vertex's clip-space position and world-space normal in place.
 *
 * The normals are directions: no translation applies to them. They are therefore transformed by Matrix3s (the
 * linear 3x3 part of the transformation), not by the Matrix4s that transform the points.
 *
 * Replaces the model/model_normals/full/full_normals fields and the
 * setModel()+calculateNormalMatrix()+calculateMVPMatrix() trio that used to
 * live on ModelViewProjection: setModel(model, withNormals) now does
 * everything in one atomic call -- there is no intermediate state where
 * calling transformVertex() would use a stale or half-computed matrix.
 *
 * Takes a ViewProjection (rather than raw view/projection matrices) so that
 * the vp = projection*view computation is shared with anything else
 * projecting points for the same camera/light (e.g. ScreenLineRenderer),
 * instead of being computed redundantly here too -- and so that setModel()
 * only needs one matrix multiplication (vp.times(model)) instead of two
 * (projection.times(view.times(model))).
 *
 * @author Olivier BARRY
 * @since 2026 (extracted from ModelViewProjection)
 *
 */
public class ElementTransform {

	// Held live (not just its matrix snapshotted once) so that a viewProjection.refresh() call
	// (e.g. after the camera moves) is picked up on the next setModel() -- see the bug this fixed,
	// where caching vp as a final field here made ViewProjection.refresh() ineffective.
	private final ViewProjection viewProjection;

	private Matrix4 model;
	private Matrix3 modelNormals; // null whenever the current Model was set with withNormals=false
	private Matrix4 full;         // vp * model
	private Matrix3 fullNormals;  // linear part of vp * modelNormals -- null whenever modelNormals is null

	/**
	 * @param viewProjection the same ViewProjection instance shared with anything else that needs
	 *                       to project points for this camera/light (e.g. ScreenLineRenderer) --
	 *                       reusing it here means vp = projection*view is computed only once for
	 *                       both purposes, rather than redundantly by each.
	 */
	public ElementTransform(ViewProjection viewProjection) {
		this.viewProjection = viewProjection;
	}

	/**
	 * Sets the current Element's Model matrix and (re)computes every derived matrix needed to
	 * transform its vertices -- in one call, so this object is never left in a partially-updated
	 * state (the legacy ModelViewProjection required 3 separate calls, in a specific order, with
	 * no protection against forgetting one).
	 *
	 * @param model       the Element's Model matrix (Element/local -> World)
	 * @param withNormals whether normals are needed for this Element/pass. Pass false to skip the
	 *                    (more expensive) normal-matrix computation entirely when normals won't be
	 *                    used at all (e.g. shadow map generation).
	 */
	public void setModel(Matrix4 model, boolean withNormals) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "setModel(model, withNormals=" + withNormals + ")");
		Matrix4 vp = viewProjection.getMatrix(); // re-fetched every call -- reflects the latest refresh()
		this.model = model;
		this.full = vp.times(model); // was projection.times(view.times(model)) -- one multiplication instead of two, see class Javadoc

		if (withNormals) {
			this.modelNormals = computeNormalMatrix(model);
			this.fullNormals = vp.getMatrix3().times(modelNormals);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation normal matrix:\n" + fullNormals);
		} else {
			this.modelNormals = null;
			this.fullNormals = null;
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation matrix:\n" + full);
	}

	/**
	 * Matrix transforming the normals: the inverse transpose of the linear 3x3 part of the Model matrix, which keeps
	 * the normals perpendicular to the surfaces under any transformation (a non-uniform scaling included). For a
	 * rotation, it is the rotation itself, so there is a single computation for every Model matrix.
	 *
	 * Only the 3x3 part is used: the translation of the Model matrix does not apply to a direction. (When the normals
	 * were transformed by the inverse transpose of the whole Matrix4, the translation ended up in its bottom row and
	 * gave the normals a non-zero w, which corrupted the ORTHOGRAPHIC backface test; it had to be removed by hand.)
	 *
	 * The inverse transpose is computed without any inversion: transpose(inverse(A)) = cofactors(A) / det(A).
	 * (There used to be two paths, the Model matrix itself when it was orthogonal within an epsilon, the inverse
	 * transpose otherwise: a matrix drifting by rounding errors could jump from one to the other between two frames.)
	 */
	private Matrix3 computeNormalMatrix(Matrix4 model) {
		Matrix3 model3 = model.getMatrix3();
		float det = model3.determinant();
		if (det == 0) {
			// A flattening transformation (e.g. a scaling by 0 on one axis): no inverse. Fall back to the Model
			// matrix rather than fail, as before.
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Singular model matrix, falling back to Model matrix for normals");
			return model3;
		}
		return model3.cofactors().times(1 / det);
	}

	/**
	 * Transforms one Vertex in place: clip-space position always; world-space and clip-space
	 * normals only if normals is true AND the current Model was set with withNormals=true.
	 */
	public void transformVertex(Vertex v, boolean normals) {
		transformVertex(v, normals, true);
	}

	/**
	 * Transforms one Vertex in place: clip-space position always; world-space normal only if normals is true AND
	 * the current Model was set with withNormals=true; clip-space normal only if clipNormals is also true (it is
	 * set to null otherwise, so that no stale value can be read).
	 *
	 * The clip-space normal is only read by the backface test of an ORTHOGRAPHIC projection (see
	 * RenderEngine.isBackFace()): under a FRUSTUM projection, computing it for every Vertex would be wasted.
	 */
	public void transformVertex(Vertex v, boolean normals, boolean clipNormals) {
		v.setProjPos(full.times(v.getPos()));
		if (normals && v.getNormal() != null) {
			v.setWorldNormal(transformNormalToWorld(v.getNormal()));
			v.setProjNormal(clipNormals ? transformNormalToClip(v.getNormal()) : null);
		}
	}

	/** Transforms every Vertex of an Element in place (world-space and clip-space normals included if normals is true). */
	public void transformElement(Element e, boolean normals) {
		transformElement(e, normals, true);
	}

	/** Transforms every Vertex of an Element in place: see transformVertex(Vertex, boolean, boolean). */
	public void transformElement(Element e, boolean normals, boolean clipNormals) {
		for (int i = 0; i < e.getNbVertices(); i++) {
			transformVertex(e.getVertex(i), normals, clipNormals);
		}
	}

	/**
	 * Transforms a Triangle's own (flat) normal in place -- shares the same normal-transform math
	 * as transformVertex() via transformNormalToWorld(), rather than duplicating it as the legacy
	 * ModelViewProjection did between transformVertex() and this method.
	 */
	public void transformNormal(Triangle t) {
		t.setWorldNormal(t.getNormal() != null ? transformNormalToWorld(t.getNormal()) : null);
	}

	/**
	 * Pure (non-mutating) clip-space projection of a Triangle's own (flat) normal -- used e.g. for
	 * a Z-sign backface test. Returns null if the Triangle has no normal at Triangle level.
	 */
	public Vector3 projectNormal(Triangle t) {
		return t.getNormal() != null ? transformNormalToClip(t.getNormal()) : null;
	}

	private Vector3 transformNormalToWorld(Vector3 n) {
		return modelNormals.times(n);
	}

	private Vector3 transformNormalToClip(Vector3 n) {
		return fullNormals.times(n);
	}
}