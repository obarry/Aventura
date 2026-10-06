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

/**
 * Wall-clock time of each phase of the last frame rendered by the RenderEngine, measured with
 * System.nanoTime(). It answers "where does the time go?" without a profiler, and it is the reference
 * for the performance work (see docs/PERFORMANCE_AUDIT.md, section 7).
 *
 * Usage, by the RenderEngine only: startFrame(), then endPhase(phase) at the end of each phase, in the
 * order of the frame (a phase that does nothing in this frame simply measures ~0), then endFrame().
 * Readers (demos, benchmarks) call getMillis(phase) or toString() after render() returns.
 *
 * Not thread-safe: written and read by the rendering thread.
 *
 * @author Olivier BARRY
 * @since October 2026
 */
public class FrameTimer {

	/** The phases of a frame, in their order in RenderEngine.render() */
	public enum Phase {
		/** View*Projection refresh, world coordinates of the vertices, back buffer and Z-buffer clearing */
		SETUP,
		/** Shadow map(s) of the shadowing lights */
		SHADOWS,
		/** Main pass: transformation, culling and rasterization of every triangle, with shading */
		MAIN_PASS,
		/** Landmarks, light vectors and visible lights (post-processing of the finished image) */
		OVERLAYS,
		/** Swap of the back and front buffers and GUI notification */
		PRESENT
	}

	private static final Phase[] PHASES = Phase.values();

	private final long[] phaseNanos = new long[PHASES.length];
	private long frameStart;
	private long phaseStart;
	private long frameNanos;

	/** To be called at the very beginning of a frame: resets the times of the previous frame */
	public void startFrame() {
		for (int i = 0; i < phaseNanos.length; i++) {
			phaseNanos[i] = 0;
		}
		frameStart = System.nanoTime();
		phaseStart = frameStart;
	}

	/** To be called at the end of a phase: the time since the end of the previous phase is assigned to this one */
	public void endPhase(Phase phase) {
		long now = System.nanoTime();
		phaseNanos[phase.ordinal()] += now - phaseStart;
		phaseStart = now;
	}

	/** To be called at the very end of a frame */
	public void endFrame() {
		frameNanos = System.nanoTime() - frameStart;
	}

	/** @return the duration of this phase in the last frame, in milliseconds */
	public float getMillis(Phase phase) {
		return phaseNanos[phase.ordinal()] / 1e6f;
	}

	/** @return the duration of the last frame, in milliseconds */
	public float getFrameMillis() {
		return frameNanos / 1e6f;
	}

	/** One line, e.g. "Frame 84.2 ms (SETUP 1.0, SHADOWS 0.0, MAIN_PASS 81.5, OVERLAYS 0.0, PRESENT 1.7)" */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Frame %.1f ms (", getFrameMillis()));
		for (int i = 0; i < PHASES.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(PHASES[i]).append(String.format(" %.1f", phaseNanos[i] / 1e6f));
		}
		return sb.append(')').toString();
	}
}
