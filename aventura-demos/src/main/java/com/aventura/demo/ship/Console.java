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
import java.util.Random;

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * A control console: a cabinet, a sloping desk with rows of buttons, and a screen above it.
 * <pre>
 *            ___
 *           |   |  screen (a Panel with the texture of a screen, in front of a Box)
 *           |___|
 *          /    /  desk, sloping towards the operator, with buttons
 *    _____/____/
 *   |          |   cabinet
 *   |__________|          the operator stands on the +X side
 * </pre>
 * The console is built at the origin, its front towards +X, then the whole assembly is turned and moved into place:
 * {@link #place(float, float, float)} gives it a Rotation and a Translation. The engine composes the transformation
 * of the console with those of its parts (each part is built where it stands in the console): moving the console moves
 * all its parts.
 *
 * @since Nov 2026
 */
public class Console extends Element {

	public static final float DEPTH = 0.7f, HEIGHT = 0.85f;
	public static final Color BODY_COLOR = new Color(108, 118, 124);
	public static final Color DESK_COLOR = new Color(70, 74, 78);
	public static final Color[] BUTTON_COLORS = { new Color(230, 60, 40), new Color(250, 190, 40), new Color(70, 200, 90),
			new Color(80, 150, 255), new Color(235, 235, 225) };

	/**
	 * @param name     name of the console
	 * @param width    width of the console, across its front
	 * @param screen   the texture of its screen
	 * @param textures the textures of the ship
	 * @param seed     seed of the layout of the buttons
	 */
	public Console(String name, float width, Texture screen, ShipTextures textures, long seed) {
		super(name);
		Random random = new Random(seed);

		Box cabinet = new Box(DEPTH, width, HEIGHT, textures.paintedMetal, "cabinet");
		cabinet.setColor(BODY_COLOR);
		cabinet.setTransformation(new Translation(new Vector3(0, 0, HEIGHT / 2)));
		addElement(cabinet);

		// The desk: a thin Box tilted around the Y axis, its front edge lower, towards the operator. A rotation by a
		// positive angle around Y turns +X towards -Z: the front edge (+X) goes down
		float tilt = (float) Math.toRadians(22);
		Box desk = new Box(DEPTH * 0.85f, width, 0.05f, textures.paintedMetal, "desk");
		desk.setColor(DESK_COLOR);
		desk.setTransformation(new Rotation(tilt, Vector3.yAxis()));
		desk.combineTransformation(new Translation(new Vector3(0.02f, 0, HEIGHT + 0.12f)));
		addElement(desk);

		// The buttons: small boxes in rows on the desk, tilted with it
		Element buttons = new Element("buttons");
		int rows = 2, columns = Math.max(3, (int) (width / 0.12f));
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				if (random.nextFloat() < 0.3f) continue;
				Box button = new Box(0.05f, 0.05f, 0.025f, "button");
				button.setColor(BUTTON_COLORS[random.nextInt(BUTTON_COLORS.length)]);
				float x = -0.12f + r * 0.14f, y = -width / 2 + (c + 0.5f) * width / columns;
				button.setTransformation(new Translation(new Vector3(x, y, 0.035f)));
				button.combineTransformation(new Rotation(tilt, Vector3.yAxis()));
				button.combineTransformation(new Translation(new Vector3(0.02f, 0, HEIGHT + 0.12f)));
				buttons.addElement(button);
			}
		}
		addElement(buttons);

		// The screen: a thin box, leaning back, at the back of the desk; its front is a Panel with the texture of a screen
		float screenWidth = width * 0.8f, screenHeight = Math.min(0.5f, screenWidth * 0.62f);
		Element display = new Element("display");
		Box back = new Box(0.06f, screenWidth + 0.06f, screenHeight + 0.06f, textures.paintedMetal, "screen case");
		back.setColor(DESK_COLOR);
		display.addElement(back);
		// The Panel is seen from +X, 1 cm in front of the case (in its plane, the two would fight in the Z-buffer): its
		// axes are Y (along the bottom edge: seen from the front, it goes from left to right)
		// and Z, and Y x Z = X
		Panel face = new Panel("screen", new Vector4(0.04f, -screenWidth / 2, -screenHeight / 2, 1), Vector3.yAxis(), Vector3.zAxis(),
				screenWidth, screenHeight, null, screen, 0); // Tile 0: the texture of the screen is stretched once over it
		display.addElement(face);
		// Leaning back: the opposite rotation, its top (+Z) turned towards -X
		display.setTransformation(new Rotation((float) Math.toRadians(-12), Vector3.yAxis()));
		display.combineTransformation(new Translation(new Vector3(-DEPTH / 2 + 0.05f, 0, HEIGHT + 0.2f + screenHeight / 2)));
		addElement(display);
	}

	/**
	 * Turns the console to face the given direction, and puts it at (x, y) on the floor. The rotation comes first: the
	 * console turns around its own origin, then the translation moves it into place (S, R, T order).
	 *
	 * @param heading the direction its front faces, in radians around Z from the X axis
	 */
	public Console place(float x, float y, float heading) {
		setTransformation(new Rotation(heading, Vector3.zAxis()));
		combineTransformation(new Translation(new Vector3(x, y, 0)));
		return this;
	}
}
