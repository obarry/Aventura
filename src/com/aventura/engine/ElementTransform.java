package com.aventura.engine;

import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.NotInvertibleMatrixException;
import com.aventura.math.vector.Vector3;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------
 * The mutating per-Element transformation pipeline: given a fixed view +
 * projection pair (constant for this object's lifetime) and a current Model
 * matrix (changes for every Element, potentially every frame), computes each
 * Vertex's clip-space position and world-space normal in place.
 *
 * Replaces the model/model_normals/full/full_normals fields and the
 * setModel()+calculateNormalMatrix()+calculateMVPMatrix() trio that used to
 * live on ModelViewProjection: setModel(model, withNormals) now does
 * everything in one atomic call -- there is no intermediate state where
 * calling transformVertex() would use a stale or half-computed matrix.
 *
 * @author Olivier BARRY
 * @since 2026 (extracted from ModelViewProjection)
 *
 */
public class ElementTransform {

	private final Matrix4 view;
	private final Matrix4 projection;

	private Matrix4 model;
	private Matrix4 modelNormals; // null whenever the current Model was set with withNormals=false
	private Matrix4 full;         // projection * view * model
	private Matrix4 fullNormals;  // projection * view * modelNormals -- null whenever modelNormals is null

	public ElementTransform(Matrix4 view, Matrix4 projection) {
		this.view = view;
		this.projection = projection;
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
		this.model = model;
		this.full = projection.times(view.times(model));

		if (withNormals) {
			this.modelNormals = computeNormalMatrix(model);
			this.fullNormals = projection.times(view.times(modelNormals));
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation normal matrix:\n" + fullNormals);
		} else {
			this.modelNormals = null;
			this.fullNormals = null;
		}
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Full transformation matrix:\n" + full);
	}

	/**
	 * Model matrix for orthogonal transformations (preserve lengths/angles) directly; its inverse
	 * transpose otherwise (needed for correct normal transformation under non-uniform scaling).
	 * Orthogonality test: Transpose(A).A = I, within a rounding-error tolerance margin.
	 */
	private Matrix4 computeNormalMatrix(Matrix4 model) {
		Matrix3 model3 = model.getMatrix3();
		if (model3.times(model3.transpose()).equals(Matrix3.IDENTITY)) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Model normals matrix = Model matrix");
			return model;
		}
		try {
			return model.transpose().inverse();
		} catch (NotInvertibleMatrixException e) {
			// Should never happen but fall back to the Model matrix rather than propagate, exactly
			// like the legacy behavior.
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Error inverting model matrix for normals, falling back to Model matrix");
			e.printStackTrace();
			return model;
		}
	}

	/**
	 * Transforms one Vertex in place: clip-space position always; world-space (and clip-space)
	 * normal only if normals is true AND the current Model was set with withNormals=true.
	 */
	public void transformVertex(Vertex v, boolean normals) {
		v.setProjPos(full.times(v.getPos()));
		if (normals && v.getNormal() != null) {
			v.setWorldNormal(transformNormalToWorld(v.getNormal()));
			v.setProjNormal(transformNormalToClip(v.getNormal()));
		}
	}

	/** Transforms every Vertex of an Element in place. */
	public void transformElement(Element e, boolean normals) {
		for (int i = 0; i < e.getNbVertices(); i++) {
			transformVertex(e.getVertex(i), normals);
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
		return modelNormals.times(n.V4()).V3();
	}

	private Vector3 transformNormalToClip(Vector3 n) {
		return fullNormals.times(n.V4()).V3();
	}
}