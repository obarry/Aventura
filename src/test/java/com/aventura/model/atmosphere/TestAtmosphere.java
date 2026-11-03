package com.aventura.model.atmosphere;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.aventura.math.vector.Vector4;
import com.aventura.model.world.World;

/**
 * Tests of the Atmosphere, the container of the haze and of the fogs: the branches, their switches and the effects
 * that are drawn.
 */
public class TestAtmosphere {

	private static SphericalFog fog(float x) {
		return new SphericalFog(new Vector4(x, 0f, 0f, 1f), 5f);
	}

	@Test
	public void testEmptyAtmosphere() {
		System.out.println("***** Test Atmosphere : an empty atmosphere draws nothing *****");
		Atmosphere a = new Atmosphere();
		assertNull(a.getHaze());
		assertTrue(a.getFogs().isEmpty());
		assertFalse(a.isHaze());
		assertFalse(a.isFog());
		assertFalse(a.isActive());
		assertTrue(a.getEffects().isEmpty());
		assertNull("Clear air by default", new World().getAtmosphere());
		assertTrue(a.toString().contains("none"));
	}

	@Test
	public void testHazeBranch() {
		System.out.println("***** Test Atmosphere : the branch of the haze and its switches *****");
		Haze haze = new Haze(100f);
		Atmosphere a = new Atmosphere(haze);
		assertSame(haze, a.getHaze());
		assertTrue("On by default", a.isHaze());
		assertTrue(a.isActive());

		a.setHaze(false);
		assertFalse("The switch of the branch", a.isHaze());
		assertTrue("The haze is kept", haze == a.getHaze());
		assertFalse(a.isActive());
		a.setHaze(true);
		assertTrue(a.isHaze());

		haze.setEnabled(false);
		assertFalse("The switch of the effect", a.isHaze());
		haze.setEnabled(true);

		a.setHaze((Haze) null);
		assertFalse("No haze", a.isHaze());
		a.setHaze(new Haze(50f));
		assertTrue("A new haze, the switch of the branch is still on", a.isHaze());
	}

	@Test
	public void testFogBranch() {
		System.out.println("***** Test Atmosphere : the branch of the fogs and its switches *****");
		Atmosphere a = new Atmosphere();
		Fog f1 = fog(0f), f2 = fog(10f);
		a.addFog(f1).addFog(f2);
		assertEquals(2, a.getFogs().size());
		assertTrue(a.isFog());
		assertTrue(a.isActive());
		assertEquals(2, a.getActiveFogs().size());
		assertFalse("No haze in that atmosphere", a.isHaze());

		f1.setEnabled(false);
		assertEquals("A fog put out", 1, a.getActiveFogs().size());
		assertSame(f2, a.getActiveFogs().get(0));
		assertEquals("Still in the list", 2, a.getFogs().size());
		assertTrue(a.isFog());

		a.setFog(false);
		assertFalse("The switch of the branch puts all the fogs out", a.isFog());
		assertTrue(a.getActiveFogs().isEmpty());
		assertFalse(a.isActive());
		assertEquals(2, a.getFogs().size());

		a.setFog(true);
		assertTrue(a.isFog());
		f2.setEnabled(false);
		assertFalse("All the fogs are disabled", a.isFog());

		a.removeFog(f1);
		assertEquals(1, a.getFogs().size());
		a.clearFogs();
		assertTrue(a.getFogs().isEmpty());
	}

	@Test
	public void testFogsListIsReadOnlyAndNotNull() {
		System.out.println("***** Test Atmosphere : the list of the fogs is read-only, a fog cannot be null *****");
		Atmosphere a = new Atmosphere().addFog(fog(0f));
		try { a.getFogs().clear(); fail("read-only"); } catch (UnsupportedOperationException expected) { }
		try { a.addFog(null); fail("not null"); } catch (IllegalArgumentException expected) { }
		a.getActiveFogs().clear(); // A copy
		assertEquals(1, a.getFogs().size());
	}

	@Test
	public void testBranchesAreIndependent() {
		System.out.println("***** Test Atmosphere : the haze and the fogs are switched independently *****");
		Atmosphere a = new Atmosphere(new Haze(100f)).addFog(fog(0f));
		a.setHaze(false);
		assertFalse(a.isHaze());
		assertTrue(a.isFog());
		assertTrue(a.isActive());
		a.setHaze(true).setFog(false);
		assertTrue(a.isHaze());
		assertFalse(a.isFog());
		assertTrue(a.isActive());
		a.setHaze(false);
		assertFalse(a.isActive());
	}

	@Test
	public void testEffects() {
		System.out.println("***** Test Atmosphere : the list of all the effects *****");
		Haze haze = new Haze(100f);
		Fog f1 = fog(0f), f2 = fog(10f);
		Atmosphere a = new Atmosphere(haze).addFog(f1).addFog(f2);
		List<AtmosphericEffect> effects = a.getEffects();
		assertEquals(3, effects.size());
		assertSame(haze, effects.get(0));
		assertSame(f1, effects.get(1));
		assertSame(f2, effects.get(2));
		a.setHaze(false).setFog(false);
		assertEquals("Drawn or not", 3, a.getEffects().size());
		assertTrue(a.toString().contains("2 fog"));
	}
}
