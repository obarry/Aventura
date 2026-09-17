package com.aventura.engine;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for ZBuffer, in particular the test()/update() buffer-coordinate caching added to avoid
 * translating the same (x, y) -> (bx, by) twice per surviving pixel (test() then update(), the
 * documented common usage pattern - see ZBuffer's Javadoc). These tests exist specifically to
 * protect that caching: they check that the fast path (update() right after a matching test())
 * behaves identically to every other path (update() with no preceding test(), or preceded by a
 * test() for a DIFFERENT pixel), since a caching bug there would silently write depth values to
 * the wrong pixel.
 */
public class TestZBuffer {

	private static final float FAR = 1000f;

	@Test
	public void testZBuffer_testThenUpdate_normalPath() {
		System.out.println("***** Test ZBuffer : test() then update() for the same pixel (documented common path) *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		assertTrue(zb.test(0, 0, 5f)); // 5 <= FAR : passes
		zb.update(0, 0, 5f);

		assertEquals(5f, zb.get(0, 0), 0f);
	}

	@Test
	public void testZBuffer_update_reusesCachedTranslation_writesCorrectCell() {
		System.out.println("***** Test ZBuffer : cached translation still targets the right buffer cell (asymmetric coords) *****");

		// Asymmetric halfWidth/halfHeight and non-zero, non-equal x/y: catches an x/y swap bug in
		// the cache just as easily as a wrong-cell bug.
		ZBuffer zb = new ZBuffer(21, 11, 10, 5, FAR);

		assertTrue(zb.test(3, -2, 7f));
		zb.update(3, -2, 7f);

		assertEquals(7f, zb.get(3, -2), 0f);
		// Neighboring cells must be untouched.
		assertEquals(FAR, zb.get(2, -2), 0f);
		assertEquals(FAR, zb.get(3, -1), 0f);
		assertEquals(FAR, zb.get(4, -2), 0f);
	}

	@Test
	public void testZBuffer_update_withoutPrecedingTest_fallsBackCorrectly() {
		System.out.println("***** Test ZBuffer : update() called with no preceding test() must still write the right cell *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		// No test() called at all on this fresh instance -- lastValid is false, must take the
		// recompute fallback rather than use stale/uninitialized cache fields.
		zb.update(2, 1, 3f);

		assertEquals(3f, zb.get(2, 1), 0f);
	}

	@Test
	public void testZBuffer_update_afterTestForDifferentPixel_fallsBackCorrectly() {
		System.out.println("***** Test ZBuffer : update() for a pixel different from the last test() must not reuse that cache *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		assertTrue(zb.test(4, 4, 1f));   // caches (4,4) -> its buffer cell, but is NOT followed by update() for (4,4)
		zb.update(-3, 0, 2f);            // update() for a DIFFERENT pixel: must not reuse (4,4)'s cached indices

		assertEquals(2f, zb.get(-3, 0), 0f);
		assertEquals(FAR, zb.get(4, 4), 0f); // untouched: only tested, never updated
	}

	@Test
	public void testZBuffer_test_isPure_doesNotMutateBuffer() {
		System.out.println("***** Test ZBuffer : test() alone never writes to the buffer *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		assertTrue(zb.test(0, 0, 1f));
		assertEquals(FAR, zb.get(0, 0), 0f); // still the init value: test() must not have written it
	}

	@Test
	public void testZBuffer_test_fartherDepth_fails() {
		System.out.println("***** Test ZBuffer : test() rejects a fragment farther than what's stored *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		assertTrue(zb.test(0, 0, 5f));
		zb.update(0, 0, 5f);

		assertFalse(zb.test(0, 0, 6f)); // farther than the 5f already stored: must fail
		zb.update(0, 0, 6f); // caller ignoring the failed test() must not corrupt anything either
		assertEquals(6f, zb.get(0, 0), 0f); // update() unconditionally overwrites, as documented
	}

	@Test
	public void testZBuffer_test_outOfBounds_returnsFalse() {
		System.out.println("***** Test ZBuffer : test() out of bounds returns false rather than throwing *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		assertFalse(zb.test(100, 100, 0f));
	}

	@Test
	public void testZBuffer_update_outOfBounds_doesNotThrow() {
		System.out.println("***** Test ZBuffer : update() out of bounds does not throw *****");

		ZBuffer zb = new ZBuffer(11, 11, 5, 5, FAR);

		zb.test(100, 100, 0f); // caches an out-of-bounds translation
		zb.update(100, 100, 0f); // must silently no-op, not throw ArrayIndexOutOfBoundsException
	}
}
