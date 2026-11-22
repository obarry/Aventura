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
package com.aventura.demo.ship;

import java.awt.Color;

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * A technical cabinet against a wall of a corridor: a Box of painted metal, and on its front a Panel with the texture of
 * its doors (see ShipTextures.createCabinetDoors()).
 * <pre>
 *        wall                                       LOW: on the floor, its front where the flat floor ends:
 *         |   __                                    no deeper than the lower chamfer (0.35 m), it does not
 *         |  |  |  HIGH, at the height of the        reach the part of the floor where people walk.
 *         |  |__|  shoulders, under the pipes
 *         |                                         HIGH: on the upright part of the wall, at the height of the
 *         |  ___                                    shoulders, no deeper than 0.3 m.
 *         | |   |
 *         | |   |  LOW
 *    _____|_|___|___ floor
 * </pre>
 * Like a Console, the cabinet is built at the origin, its front towards +X, its back on the plane x = 0, then turned
 * and moved against the wall ({@link #place}): built where it stands, each part keeps a simple position.
 *
 * @since Nov 2026
 */
public class WallCabinet extends Element {

	/** The two kinds of cabinets. */
	public enum Type {
		LOW(0.33f, 1.05f, 0f, 2), HIGH(0.3f, 0.62f, 1.25f, 2);

		/** Depth from the wall, height, altitude of its bottom, number of doors */
		final float depth, height, bottom;
		final int doors;

		Type(float depth, float height, float bottom, int doors) {
			this.depth = depth;
			this.height = height;
			this.bottom = bottom;
			this.doors = doors;
		}

		public float getDepth() {
			return depth;
		}
	}

	public static final Color[] COLORS = { new Color(120, 130, 124), new Color(150, 146, 132), new Color(104, 112, 122) };

	protected final Type type;
	protected final float width;

	/**
	 * @param name     name of the cabinet
	 * @param type     low or high
	 * @param width    width along the wall
	 * @param doors    the texture of its front (its doors), stretched once over it
	 * @param textures the textures of the ship
	 * @param color    the color of its paint
	 */
	public WallCabinet(String name, Type type, float width, Texture doors, ShipTextures textures, Color color) {
		super(name);
		this.type = type;
		this.width = width;
		setColor(color);

		Box body = new Box(type.depth, width, type.height, textures.paintedMetal, "body");
		body.setTransformation(new Translation(new Vector3(type.depth / 2, 0, type.bottom + type.height / 2)));
		addElement(body);

		// The front: a Panel seen from +X, 1 cm in front of the body (in its plane, the two would fight in the Z-buffer).
		// Its axes are Y (along the bottom edge, from left to right seen from the front) and Z, and Y x Z = X
		Panel front = new Panel("doors", new Vector4(type.depth + 0.01f, -width / 2, type.bottom, 1), Vector3.yAxis(), Vector3.zAxis(), width,
				type.height, null, doors, 0);
		addElement(front);
	}

	/**
	 * Puts the cabinet against a wall: its back at the given point, its front turned to the given direction (the inside
	 * of the corridor). The rotation comes first, around its own origin, then the translation.
	 */
	public WallCabinet place(Vector4 back, Vector3 facing) {
		float heading = (float) Math.atan2(facing.getY(), facing.getX());
		setTransformation(new Rotation(heading, Vector3.zAxis()));
		combineTransformation(new Translation(new Vector3(back.getX(), back.getY(), 0)));
		return this;
	}

	public Type getType() {
		return type;
	}

	public float getWidth() {
		return width;
	}
}
