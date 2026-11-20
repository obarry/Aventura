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

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * The door of an airlock, in a doorway of the shape of a corridor: a heavy frame painted with hazard stripes, and two
 * leaves that slide sideways into it.
 * <pre>
 *   AirlockDoor
 *    ├── frame    SectionFrame: from the outline of the corridor to the smaller outline of the door
 *    ├── left     Box: a leaf, moved by a Translation
 *    └── right    Box: the other leaf
 *
 *        closed                 open
 *     ____________          ____________
 *    | |   ||   | |        |  |      |  |       The leaves slide behind the frame and into the walls,
 *    | |   ||   | |        |  |      |  |       where they are hidden: an open door shows nothing but
 *    | |___||___| |        |__|      |__|       its frame.
 * </pre>
 * The door is built where it stands, across a direction (the direction of the passage). Only the leaves move: the
 * opening of the door ({@link #setOpening}) changes their Translation. Since the leaves cast shadows, a light that keeps
 * its shadow map from one image to the next (static shadow casters) must be told when a door moves (see
 * ShadowingLight.invalidateShadowMap()).
 *
 * @since Nov 2026
 */
public class AirlockDoor extends Element {

	public static final float FRAME_THICKNESS = 0.36f;
	public static final float LEAF_THICKNESS = 0.08f;
	/** The leaves go this far into the frame, and travel this far when the door opens. */
	static final float LEAF_OVERLAP = 0.02f;
	static final float LEAF_TRAVEL = ShipSections.DOOR_WIDTH / 2 + 0.08f;

	public static final Color FRAME_COLOR = new Color(84, 92, 90);
	public static final Color LEAF_COLOR = new Color(176, 178, 172);

	protected final Vector4 center;
	protected final Vector3 across;   // Horizontal, perpendicular to the passage: the direction the leaves slide in
	protected final Box leftLeaf, rightLeaf;
	protected float opening;

	/**
	 * @param name     name of the door
	 * @param center   the middle of the doorway, on the floor
	 * @param passage  the direction of the passage through the door (horizontal)
	 * @param opening  0 for a closed door, 1 for an open one
	 * @param textures the textures of the ship
	 */
	public AirlockDoor(String name, Vector4 center, Vector3 passage, float opening, ShipTextures textures) {
		super(name);
		this.center = center;
		Vector3 forward = new Vector3(passage).normalize();
		this.across = Vector3.zAxis().cross(forward); // The "left" of the passage

		SectionFrame frame = new SectionFrame("frame", center, forward, ShipSections.corridor(), ShipSections.door(0), FRAME_THICKNESS,
				textures.paintedMetal, 0.5f);
		frame.setOpen(true); // No sill: the floor goes through the door
		frame.setBand(textures.hazardStripes, Color.WHITE); // The edge of the doorway: stripes, in their own colors
		frame.setColor(FRAME_COLOR);
		addElement(frame);

		// A leaf: a Box whose sides are along the axes of the World (the doors of the ship are along X or Y)
		float width = ShipSections.DOOR_WIDTH / 2 + LEAF_OVERLAP, height = ShipSections.DOOR_HEIGHT;
		boolean alongX = Math.abs(forward.getX()) > Math.abs(forward.getY());
		float dx = alongX ? LEAF_THICKNESS : width, dy = alongX ? width : LEAF_THICKNESS;
		leftLeaf = new Box(dx, dy, height, textures.doorLeaf, "left leaf");
		rightLeaf = new Box(dx, dy, height, textures.doorLeaf, "right leaf");
		for (Box leaf : new Box[] { leftLeaf, rightLeaf }) {
			leaf.setColor(LEAF_COLOR);
			addElement(leaf);
		}
		setOpening(opening);
	}

	/**
	 * Opens the door by a fraction: 0 closed, 1 open. The leaves are moved by their Translation; the World must then
	 * be transformed again, which RenderEngine.render() does at each image.
	 */
	public void setOpening(float opening) {
		this.opening = Math.max(0, Math.min(1, opening));
		// Closed, the two leaves meet in the middle; their outer edge goes into the frame by LEAF_OVERLAP
		float offset = (ShipSections.DOOR_WIDTH / 2 + LEAF_OVERLAP) / 2 + this.opening * LEAF_TRAVEL;
		float z = ShipSections.DOOR_HEIGHT / 2; // A Box is centered on its origin
		leftLeaf.setTransformation(new Translation(new Vector3(center.getX() + across.getX() * offset, center.getY() + across.getY() * offset, z)));
		rightLeaf.setTransformation(new Translation(new Vector3(center.getX() - across.getX() * offset, center.getY() - across.getY() * offset, z)));
	}

	public float getOpening() {
		return opening;
	}

	public Vector4 getCenter() {
		return center;
	}

	/** The horizontal direction across the doorway, the one the leaves slide in. */
	public Vector3 getAcross() {
		return across;
	}
}
