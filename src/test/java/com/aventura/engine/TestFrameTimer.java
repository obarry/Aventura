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

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

/**
 * Tests of the report of the FrameTimer: the time of a frame and of each of its phases, on one line.
 */
public class TestFrameTimer {

	@Test
	public void testReportUsesADecimalPointWhateverTheLocale() {
		Locale systemLocale = Locale.getDefault();
		try {
			// In French a number is written with a decimal comma: "3,9". The phases being separated by commas, the
			// report would read "SETUP 3,9, SHADOWS 1,4"
			Locale.setDefault(Locale.FRANCE);
			FrameTimer timer = new FrameTimer();
			timer.startFrame();
			for (FrameTimer.Phase phase : FrameTimer.Phase.values()) timer.endPhase(phase);
			timer.endFrame();
			String report = timer.toString();

			assertTrue(report, report.matches("Frame \\d+\\.\\d ms \\(.*\\)"));
			// One time per phase, each with a decimal point, separated by a comma and a space
			String[] phases = report.substring(report.indexOf('(') + 1, report.length() - 1).split(", ");
			assertEquals(report, FrameTimer.Phase.values().length, phases.length);
			for (String phase : phases) {
				assertTrue(phase, phase.matches("[A-Z_]+ \\d+\\.\\d"));
			}
		} finally {
			Locale.setDefault(systemLocale);
		}
	}
}
