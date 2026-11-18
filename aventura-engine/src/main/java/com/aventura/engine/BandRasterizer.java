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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import com.aventura.context.PerspectiveContext;

/**
 * Rasterizes the faces of a DrawList with several threads (see docs/PERFORMANCE_AUDIT.md, multithreading): the
 * main pass of the RenderEngine, and the shadow maps (ShadowingLight, in the screen of the light). The same threads
 * also run the post-processes of the image by bands (runBands(): atmosphere, visible lights).
 *
 * The screen is cut into horizontal bands of rows. A thread takes a band, goes through ALL the faces of the list,
 * in their order, and draws only the pixels of its band (TriangleRasterizer.setRows()); then it takes the next free
 * band, until there is none left. So:
 * <ul>
 * <li>no two threads ever write the same pixel, of the image or of the ZBuffer: no lock is needed;</li>
 * <li>each pixel sees the faces in the same order as with a single thread: the image is exactly the same,
 *     whatever the number of threads;</li>
 * <li>there are several bands per thread (BANDS_PER_THREAD), handed out as the threads become free: a thread whose
 *     bands are empty (sky) takes more of them, and they all finish at about the same time.</li>
 * </ul>
 *
 * The threads are created once, here, and reused from frame to frame (a fixed pool): creating threads at each frame
 * would cost more than it saves. They are daemon threads, named "aventura-raster-N", so they never prevent the
 * program from ending; close() stops them earlier.
 *
 * Each thread has its own Worker: its own TriangleRasterizer (with its Fragment), and its own counters. The objects
 * read by all the threads (the DrawList, the triangles, the lights, the materials, the shadow maps) are only read
 * during the rasterization: they were prepared by the rendering thread before rasterize() is called.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public class BandRasterizer {

	/**
	 * Draws one face of the DrawList with the given TriangleRasterizer (the one of the calling thread). Implemented
	 * by RenderEngine.rasterizeFace(), which creates the FragmentConsumer of the face.
	 */
	public interface FaceRenderer {
		void rasterizeFace(DrawList.Command command, TriangleRasterizer rasterizer);
	}

	/** Number of bands per thread: more bands balance the load better, but each band goes through all the faces */
	static final int BANDS_PER_THREAD = 4;

	private final ExecutorService pool;
	private final List<Worker> workers = new ArrayList<Worker>();

	// The work of the current rasterize() call, set by the rendering thread before the workers start (handing the
	// tasks to the pool makes these writes visible to the workers), and only read by them
	private DrawList drawList;
	private FaceRenderer renderer;
	private PerspectiveContext perspectiveCtx;
	private ZBuffer zBuffer;
	private int firstRow, endRow, bandHeight, bandCount;
	private final AtomicInteger nextBand = new AtomicInteger();

	/**
	 * Creates the threads.
	 * @param threads the number of threads, at least 1
	 */
	public BandRasterizer(int threads) {
		if (threads < 1) throw new IllegalArgumentException("At least one thread is needed, got " + threads);
		pool = Executors.newFixedThreadPool(threads, new RasterThreadFactory());
		for (int i = 0; i < threads; i++) {
			workers.add(new Worker());
		}
	}

	/** @return the number of threads */
	public int getThreads() {
		return workers.size();
	}

	/**
	 * Rasterizes all the faces of the list, with all the threads, and returns when they are all drawn. The faces'
	 * statistics are then recorded into stats, face by face in the order of the list, as a single thread would.
	 *
	 * @param drawList       the faces to draw (no edges or normals: see DrawList.hasOnlyFaces())
	 * @param perspectiveCtx the size of the screen
	 * @param zBuffer        the ZBuffer of the screen
	 * @param renderer       draws one face, called by the threads
	 * @param stats          receives the statistics of each face
	 */
	public void rasterize(DrawList drawList, PerspectiveContext perspectiveCtx, ZBuffer zBuffer, FaceRenderer renderer, RasterizerStats stats) {
		this.drawList = drawList;
		this.renderer = renderer;
		this.perspectiveCtx = perspectiveCtx;
		this.zBuffer = zBuffer;

		// Rows of the screen, from -halfHeight to halfHeight (see TriangleRasterizer), cut into bands of equal height
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		firstRow = -halfHeight;
		endRow = halfHeight + 1;
		int rows = endRow - firstRow;
		bandHeight = (rows + workers.size() * BANDS_PER_THREAD - 1) / (workers.size() * BANDS_PER_THREAD);
		bandCount = (rows + bandHeight - 1) / bandHeight;
		nextBand.set(0);

		// Start all the workers and wait until they have all finished (the end of the frame's rasterization)
		invokeAll(workers);

		// Statistics: each worker has counted the lines and pixels of each face in its own bands; their sums are
		// the counts of the face on the whole screen
		for (int i = 0; i < drawList.size(); i++) {
			int lines = 0, rendered = 0, discarded = 0;
			for (Worker worker : workers) {
				lines += worker.lines[i];
				rendered += worker.renderedPixels[i];
				discarded += worker.discardedPixels[i];
			}
			stats.recordTriangle(lines, rendered, discarded);
		}

		// Do not keep the objects of this frame alive
		this.drawList = null;
		this.renderer = null;
		this.zBuffer = null;
	}

	/**
	 * Work done on a band of rows of the image (a post-process: atmosphere, visible lights): see runBands().
	 */
	public interface BandTask {
		/**
		 * Processes the rows from firstRow (inclusive) to endRow (exclusive). Called by several threads at the same
		 * time, for different bands: it must only write into its own rows.
		 */
		void run(int firstRow, int endRow);
	}

	/**
	 * Runs a task on all the rows from firstRow to endRow (exclusive), cut into bands shared by the threads (as for
	 * rasterize()), and returns when all the bands are done. Each row being processed by one thread, as it would be
	 * by a single one, the result does not depend on the number of threads.
	 *
	 * @param bands    the threads, or null to run the task in the calling thread, on all the rows at once
	 * @param firstRow first row (inclusive), in whatever coordinates the task uses
	 * @param endRow   end row (exclusive)
	 * @param task     the work on one band
	 */
	public static void runBands(BandRasterizer bands, int firstRow, int endRow, BandTask task) {
		if (endRow <= firstRow) return;
		if (bands == null) {
			task.run(firstRow, endRow);
		} else {
			bands.runBands(firstRow, endRow, task);
		}
	}

	/**
	 * Same as runBands(), for a task written row by row: row.accept(r) is called once for each row r, from firstRow
	 * to endRow (exclusive), by the thread of its band.
	 */
	public static void runRows(BandRasterizer bands, int firstRow, int endRow, IntConsumer row) {
		runBands(bands, firstRow, endRow, (first, end) -> {
			for (int r = first; r < end; r++) {
				row.accept(r);
			}
		});
	}

	private void runBands(int first, int end, BandTask task) {
		int rows = end - first;
		int height = (rows + workers.size() * BANDS_PER_THREAD - 1) / (workers.size() * BANDS_PER_THREAD);
		int count = (rows + height - 1) / height;
		AtomicInteger next = new AtomicInteger();
		List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
		for (int i = 0; i < workers.size(); i++) {
			tasks.add(() -> {
				int band;
				while ((band = next.getAndIncrement()) < count) {
					int bandFirst = first + band * height;
					task.run(bandFirst, Math.min(bandFirst + height, end));
				}
				return null;
			});
		}
		invokeAll(tasks);
	}

	/** Runs the tasks with all the threads and waits until they have all finished, passing on their exceptions */
	private void invokeAll(List<? extends Callable<Void>> tasks) {
		try {
			for (Future<Void> done : pool.invokeAll(tasks)) {
				done.get(); // throws the exception of a task, if any
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while rendering in several threads", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Rendering failed in a worker thread", e.getCause());
		}
	}

	/**
	 * Stops the threads (after the end of a rasterize() call in progress, if any). This BandRasterizer cannot be
	 * used anymore.
	 */
	public void close() {
		pool.shutdown();
	}

	/**
	 * The work of one thread: takes the free bands one by one and draws all the faces in each of them. Each
	 * worker is used by one thread at a time, so its rasterizer and counters need no synchronization.
	 */
	private class Worker implements Callable<Void> {

		// The rasterizer of the last call, kept while the screen stays the same: the main pass of the next frame
		// (a new one after a resize of the view, or for a shadow map, which has its own screen and depth buffer)
		private TriangleRasterizer rasterizer;
		private PerspectiveContext rasterizerPerspective;
		private ZBuffer rasterizerZBuffer;

		// Lines and pixels counted for each face of the list (index in the list), in the bands of this worker
		private int[] lines = new int[0];
		private int[] renderedPixels = new int[0];
		private int[] discardedPixels = new int[0];

		@Override
		public Void call() {
			if (rasterizer == null || rasterizerPerspective != perspectiveCtx || rasterizerZBuffer != zBuffer) {
				rasterizer = new TriangleRasterizer(perspectiveCtx, zBuffer);
				rasterizerPerspective = perspectiveCtx;
				rasterizerZBuffer = zBuffer;
			}
			int size = drawList.size();
			if (lines.length < size) {
				lines = new int[size];
				renderedPixels = new int[size];
				discardedPixels = new int[size];
			} else {
				Arrays.fill(lines, 0, size, 0);
				Arrays.fill(renderedPixels, 0, size, 0);
				Arrays.fill(discardedPixels, 0, size, 0);
			}

			int band;
			while ((band = nextBand.getAndIncrement()) < bandCount) {
				int bandFirstRow = firstRow + band * bandHeight;
				rasterizer.setRows(bandFirstRow, Math.min(bandFirstRow + bandHeight, endRow));
				for (int i = 0; i < size; i++) {
					rasterizer.resetStats();
					renderer.rasterizeFace(drawList.get(i), rasterizer);
					lines[i] += rasterizer.getRasterizedLines();
					renderedPixels[i] += rasterizer.getRenderedPixels();
					discardedPixels[i] += rasterizer.getDiscardedPixels();
				}
			}
			return null;
		}
	}

	/** Creates the threads of the pool: daemon threads named aventura-raster-1, -2, ... */
	private static class RasterThreadFactory implements ThreadFactory {

		private final AtomicInteger count = new AtomicInteger();

		@Override
		public Thread newThread(Runnable task) {
			Thread thread = new Thread(task, "aventura-raster-" + count.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
	}
}
