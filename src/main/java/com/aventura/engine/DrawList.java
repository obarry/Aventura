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

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.math.vector.Vector3;
import com.aventura.model.material.Material;
import com.aventura.model.world.triangle.Triangle;

/**
 * The list of the drawing commands of a frame, filled by the geometry stage of the RenderEngine and executed, in
 * the same order, by its rasterization stage. See docs/PERFORMANCE_AUDIT.md, D9. The shadow maps are generated the
 * same way (ShadowingLight), with DEPTH commands.
 *
 * A GPU works in the same two stages: the vertices are transformed and the triangles assembled, culled and
 * prepared first; the prepared triangles are rasterized and shaded afterwards. Separating them is what allows the
 * rasterization to be shared between several threads (each one drawing its own part of the screen), or the
 * triangles to be sorted before being drawn.
 *
 * Memory: a command holds references only (the Triangle, whose vertices already carry their projected positions,
 * the normals to interpolate, the Material), about 60 bytes, and only for the triangles that passed the culling.
 * The commands are reused from one frame to the next: clear() keeps them, next() recycles them, so after the first
 * frames the list allocates nothing.
 *
 * Not thread-safe: filled by the rendering thread. The rasterization stage reads it once it is complete.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public class DrawList {

	/** What a Command does */
	public enum Kind {
		/** Rasterize the triangle with lighting (ShadingConsumer): FLAT and INTERPOLATE */
		FILL_SHADED,
		/** Rasterize the triangle without lighting (UnlitConsumer): UNLIT and MONOCHROME */
		FILL_UNLIT,
		/** Draw the 3 edges of the triangle, without depth test (LINE, and the lines over FLAT and INTERPOLATE) */
		EDGES,
		/** Draw the 3 edges of the triangle, depth-tested (MONOCHROME, and UNLIT with lines) */
		EDGES_DEPTH_TESTED,
		/** Draw the normal vectors of the triangle (RenderContext.isDisplayNormals()) */
		NORMALS,
		/** Rasterize the triangle into a depth buffer only (DepthOnlyConsumer): the shadow maps (ShadowingLight) */
		DEPTH;

		/** @return true for the kinds that fill the face of the triangle (FILL_SHADED, FILL_UNLIT, DEPTH) */
		public boolean isFace() {
			return this == FILL_SHADED || this == FILL_UNLIT || this == DEPTH;
		}
	}

	/**
	 * One drawing command: a triangle and what to do with it. Prepared by the geometry stage, read by the
	 * rasterization stage. The fields that a kind does not use stay null.
	 */
	public static final class Command {
		Kind kind;
		Triangle triangle;
		/** Normals to interpolate (FILL_SHADED): the vertex normals, or 3 times the face normal */
		Vector3 normal1, normal2, normal3;
		/** Material of the triangle (FILL_SHADED, FILL_UNLIT) */
		Material material;
		/** True to interpolate the texture coordinates of the triangle (FILL_SHADED, FILL_UNLIT) */
		boolean textured;
		/** True to look up the shadow maps (FILL_SHADED) */
		boolean shadows;
		/** Color of the lines (EDGES, EDGES_DEPTH_TESTED) */
		Color lineColor;

		/** @return the triangle of this command */
		public Triangle getTriangle() {
			return triangle;
		}

		private void clear() {
			kind = null;
			triangle = null;
			normal1 = normal2 = normal3 = null;
			material = null;
			lineColor = null;
		}
	}

	private final ArrayList<Command> commands = new ArrayList<Command>();
	private int size = 0;
	private int lineCommands = 0; // Commands that are not faces (edges, normals), see hasOnlyFaces()

	/**
	 * @return a command to fill, appended at the end of the list (a recycled one when possible)
	 */
	public Command next(Kind kind, Triangle triangle) {
		if (size == commands.size()) {
			commands.add(new Command());
		}
		Command command = commands.get(size++);
		command.kind = kind;
		command.triangle = triangle;
		if (!kind.isFace()) lineCommands++;
		return command;
	}

	/**
	 * @return true if the list contains faces only (no edges, no normals): the faces can then be rasterized by
	 *         several threads (see BandRasterizer). The lines are drawn through the Graphics2D of the GUIView, which
	 *         is not thread-safe, so a list with lines is executed by the rendering thread alone.
	 */
	public boolean hasOnlyFaces() {
		return lineCommands == 0;
	}

	/** @return the number of commands in the list */
	public int size() {
		return size;
	}

	/** @return the command of index i, in the order they were added */
	public Command get(int i) {
		return commands.get(i);
	}

	/**
	 * Empties the list. The commands are kept for the next frame, but their references are released, so that the
	 * list does not keep the objects of a former frame alive.
	 */
	public void clear() {
		for (int i = 0; i < size; i++) {
			commands.get(i).clear();
		}
		size = 0;
		lineCommands = 0;
	}
}
