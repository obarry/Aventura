package com.aventura.cookbook;

import java.awt.Color;

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.ClosedCylinder;
import com.aventura.model.world.shape.Sphere;

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
 * Cookbook recipe 4 (see docs/GEOMETRY_COOKBOOK.md): assembling Elements into a complex object.
 *
 * A street lamp, as a tree of Elements. StreetLamp itself is a group: it has no geometry of its
 * own (it extends Element, not GenerativeElement) and only assembles sub-Elements.
 *
 *     StreetLamp (group)           origin: center of the foot, on the ground; Z up
 *       +-- base    ClosedConeFrustum   translated so that it stands on z = 0
 *       +-- pole    ClosedCylinder      translated on top of the base
 *       +-- arm     (group)             PIVOT at the top of the pole: translated there, then tilted
 *             +-- bar    Box            along the arm's local +X axis
 *             +-- shade  PyramidFrustum turned upside down, at the end of the bar
 *             +-- bulb   Sphere         under the shade
 *
 * The intermediate "arm" group is the point of this example: its origin is the joint at the top of
 * the pole, so tilting the arm is ONE rotation of the group, around that joint, and the bar, the
 * shade and the bulb follow. Without the group, each of the 3 parts would need its own rotation
 * around a point that is not its own center (i.e. translate - rotate - translate back).
 *
 * World position of a point p of the shade:
 *     worldPos = M(lamp) . M(arm) . M(shade) . p      with each M = T . R . S
 *
 * Colors: the lamp's color is inherited by every part that has none (base, pole, bar, shade);
 * the bulb and the inside of the shade (its bottom face, once turned over) override it.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class StreetLamp extends Element {

	public static final Color LAMP_COLOR = new Color(40, 70, 60);
	public static final Color BULB_COLOR = new Color(255, 235, 170);

	// Dimensions, in meters
	static final float BASE_HEIGHT = 0.4f, BASE_RAY = 0.22f;
	static final float POLE_RAY = 0.06f;
	static final float ARM_LENGTH = 1.2f, ARM_THICKNESS = 0.07f;
	static final float SHADE_HEIGHT = 0.25f;
	static final int HALF_SEG = 12;

	protected final Element arm;
	protected final Element shade;
	protected final Element bulb;

	/**
	 * @param poleHeight height of the pole, above the base
	 * @param armTilt    angle of the arm above the horizontal, in radians (0: horizontal)
	 */
	public StreetLamp(float poleHeight, float armTilt) {
		super("street lamp");
		setColor(LAMP_COLOR); // inherited by the parts without a color of their own

		// Base: a truncated cone, 2/3 of the full cone. Its frame is centered on the full cone,
		// so its bottom lies at z = -coneHeight/2: translate it up by coneHeight/2 to stand on z = 0.
		float coneHeight = BASE_HEIGHT * 1.5f;
		ClosedConeFrustum base = new ClosedConeFrustum(coneHeight, BASE_HEIGHT, BASE_RAY, HALF_SEG);
		base.setTransformation(new Translation(new Vector3(0, 0, coneHeight / 2)));
		addElement(base);

		// Pole: a cylinder is centered on its origin: its center goes to mid-height of the pole.
		ClosedCylinder pole = new ClosedCylinder(poleHeight, POLE_RAY, HALF_SEG);
		pole.setTransformation(new Translation(new Vector3(0, 0, BASE_HEIGHT + poleHeight / 2)));
		addElement(pole);

		// Arm: a group whose origin is the joint at the top of the pole. First tilted around its
		// own origin (rotation around -Y lifts the +X axis upwards), then moved to the joint.
		arm = new Element("arm");
		arm.setTransformation(new Rotation(armTilt, Vector3.yOppAxis()));
		arm.combineTransformation(new Translation(new Vector3(0, 0, BASE_HEIGHT + poleHeight)));
		addElement(arm);

		// In the arm's frame, everything is simple: the arm is the +X axis, horizontal.
		Box bar = new Box(ARM_LENGTH, ARM_THICKNESS, ARM_THICKNESS, "bar");
		bar.setTransformation(new Translation(new Vector3(ARM_LENGTH / 2, 0, 0)));
		arm.addElement(bar);

		// Shade: wide at the bottom once turned upside down (the narrow face, "bottom" before the
		// half-turn, ends up on top, against the bar). Its inside (the wide face) is lit by the bulb.
		PyramidFrustum shadeFrustum = new PyramidFrustum(0.16f, 0.12f, 0.5f, 0.36f, SHADE_HEIGHT);
		shadeFrustum.setTopColor(BULB_COLOR); // the "top" face ends up at the bottom: the glowing glass
		shade = shadeFrustum;
		shade.setTransformation(new Rotation((float) Math.PI, Vector3.xAxis()));
		shade.combineTransformation(new Translation(new Vector3(ARM_LENGTH, 0, -SHADE_HEIGHT / 2)));
		arm.addElement(shade);

		bulb = new Sphere(0.07f, 8);
		bulb.setColor(BULB_COLOR);
		bulb.setTransformation(new Translation(new Vector3(ARM_LENGTH, 0, -SHADE_HEIGHT - 0.03f)));
		arm.addElement(bulb);
	}

	public Element getArm() {
		return arm;
	}

	public Element getShade() {
		return shade;
	}

	public Element getBulb() {
		return bulb;
	}
}
