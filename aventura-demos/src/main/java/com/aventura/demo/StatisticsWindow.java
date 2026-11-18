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
package com.aventura.demo;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import com.aventura.engine.RenderEngine;
import com.aventura.model.world.GeometryReport;
import com.aventura.model.world.World;

/**
 * The statistics of a demo application, for any demo with a Swing window: the size of its scene and what the engine
 * did of it for the last image, displayed in a window of their own and printed in the console.
 *
 * <p>Three lines are enough to add them to a demo:
 * <pre>
 *   StatisticsWindow statistics = new StatisticsWindow(frame, "My demo", world, renderer);
 *   menu.add(statistics.createMenuItem());   // A "Statistics" menu item (key I): displays and prints them
 *   statistics.printOnClose();               // Prints them in the console when the window of the demo is closed
 * </pre>
 * The statistics are a text in two parts ({@link #report()}):
 * <ul>
 * <li><b>The scene</b>, from {@link GeometryReport}: the composition of the geometry (Elements, triangles and vertices
 *     of each branch of the tree of the World) and the textures (size, memory, triangles that use them).</li>
 * <li><b>The last image</b>, from the {@link RenderEngine}: the time of each phase of the rendering
 *     ({@link RenderEngine#getFrameTimer()}) and the number of triangles processed, displayed, out of the view and
 *     facing away ({@link RenderEngine#renderStats()}). Unlike the scene, it depends on the camera and on the
 *     rendering options: asking the statistics again updates it.</li>
 * </ul>
 * The report is computed when asked, never during the rendering: it costs nothing to a demo that does not ask for it.
 *
 * @since Nov 2026
 */
public class StatisticsWindow {

	private final JFrame owner;
	private final String title;
	private final World world;
	private final RenderEngine renderer;
	private Supplier<String> imageDescription = null;
	private Supplier<String> extraSection = null;

	// The window, created the first time the statistics are displayed
	private JDialog dialog;
	private JTextArea text;

	/**
	 * @param owner    the window of the demo, or null for an application without a window (report() and print() only)
	 * @param title    the name of the demo, for the title of the statistics
	 * @param world    the World of the demo, already built
	 * @param renderer the RenderEngine of the demo
	 */
	public StatisticsWindow(JFrame owner, String title, World world, RenderEngine renderer) {
		this.owner = owner;
		this.title = title;
		this.world = world;
		this.renderer = renderer;
	}

	/**
	 * Adds a description of the last image to its statistics, asked each time they are computed: for instance the size
	 * of the image and the camera mode of the demo.
	 *
	 * @return this, to chain the call
	 */
	public StatisticsWindow setImageDescription(Supplier<String> imageDescription) {
		this.imageDescription = imageDescription;
		return this;
	}

	/**
	 * Adds a section of its own to the end of the report, for what only the demo knows: e.g. the average rendering
	 * time over a lap of a flight (see FrameTimeStatistics). The text is asked each time the report is computed; a
	 * null or empty text adds nothing.
	 *
	 * @param extraSection gives the text of the section, title line included
	 */
	public StatisticsWindow setExtraSection(Supplier<String> extraSection) {
		this.extraSection = extraSection;
		return this;
	}

	/** The statistics, as a text: the scene (see GeometryReport), the last image rendered, then the section of the demo if any. */
	public String report() {
		StringBuilder report = new StringBuilder();
		report.append(title).append(" - statistics\n\n");
		report.append(GeometryReport.of(world)).append('\n');
		report.append("LAST IMAGE");
		if (imageDescription != null) report.append(" (").append(imageDescription.get()).append(')');
		report.append('\n');
		report.append(renderer.getFrameTimer()).append('\n');
		// The first line of the statistics of the engine: the triangles processed, displayed and culled (the next ones
		// detail the work of the rasterizer)
		report.append(renderer.renderStats().split("\n")[0]).append('\n');
		String extra = extraSection != null ? extraSection.get() : null;
		if (extra != null && !extra.isEmpty()) report.append('\n').append(extra).append('\n');
		return report.toString();
	}

	/** Prints the statistics in the console. */
	public void print() {
		System.out.println(report());
	}

	/**
	 * Displays the statistics in a window of their own, which does not block the demo (an animation goes on behind
	 * it), and prints them in the console. Asked again, the same window is updated.
	 */
	public void show() {
		String report = report();
		System.out.println(report);

		if (dialog == null) {
			text = new JTextArea();
			text.setEditable(false);
			// The report is a table made of spaces: it needs a font whose characters all have the same width
			text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			dialog = new JDialog(owner, title + " - statistics", false); // false: not modal
			dialog.getContentPane().add(new JScrollPane(text));
		}
		text.setText(report);
		text.setCaretPosition(0); // Scrolled to the top
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
	}

	/** A "Statistics" menu item, with the key I as its accelerator, that displays the statistics (see show()). */
	public JMenuItem createMenuItem() {
		JMenuItem item = new JMenuItem("Statistics");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0));
		item.addActionListener(e -> show());
		return item;
	}

	/** Prints the statistics in the console when the window of the demo is closed by the user. */
	public void printOnClose() {
		owner.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				print();
			}
		});
	}
}
