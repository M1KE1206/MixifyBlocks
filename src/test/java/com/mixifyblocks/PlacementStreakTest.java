package com.mixifyblocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementStreakTest {

	@Test
	void eenVerseTellerHeeftDeLimietNietBereikt() {
		PlacementStreak streak = new PlacementStreak();
		assertFalse(streak.limitReached(3));
	}

	@Test
	void eenPlaatsingBereiktDeLimietNietBijEenLimietVanDrie() {
		PlacementStreak streak = new PlacementStreak();
		streak.record(new String("stone"));
		assertFalse(streak.limitReached(3));
	}

	@Test
	void drieIdentiekePlaatsingenBereikenDeLimietVanDrieWel() {
		PlacementStreak streak = new PlacementStreak();
		Object stone = new String("stone");
		streak.record(stone);
		streak.record(stone);
		streak.record(stone);
		assertTrue(streak.limitReached(3));
	}

	@Test
	void tweeIdentiekePlaatsingenBereikenDeLimietVanDrieNiet() {
		PlacementStreak streak = new PlacementStreak();
		Object stone = new String("stone");
		streak.record(stone);
		streak.record(stone);
		assertFalse(streak.limitReached(3));
	}

	@Test
	void eenAnderItemLaatDeTellingOpnieuwBeginnenOokNaHetBereikenVanDeLimiet() {
		PlacementStreak streak = new PlacementStreak();
		Object stone = new String("stone");
		Object dirt = new String("dirt");
		streak.record(stone);
		streak.record(stone);
		streak.record(stone);
		assertTrue(streak.limitReached(3));

		streak.record(dirt);
		assertFalse(streak.limitReached(3));
		assertEquals(dirt, streak.lastItem());
	}

	@Test
	void bijEenLimietVanEenIsDeLimietAlNaEenPlaatsingBereikt() {
		PlacementStreak streak = new PlacementStreak();
		streak.record(new String("stone"));
		assertTrue(streak.limitReached(1));
	}

	@Test
	void resetZetDeTellingTerugEnLastItemWeerOpNull() {
		PlacementStreak streak = new PlacementStreak();
		Object stone = new String("stone");
		streak.record(stone);
		streak.record(stone);
		streak.record(stone);
		assertTrue(streak.limitReached(3));

		streak.reset();
		assertNull(streak.lastItem());
		assertFalse(streak.limitReached(1));
	}

	@Test
	void lastItemGeeftHetLaatstGeregistreerdeItemTerug() {
		PlacementStreak streak = new PlacementStreak();
		Object stone = new String("stone");
		Object dirt = new String("dirt");
		streak.record(stone);
		streak.record(dirt);
		assertEquals(dirt, streak.lastItem());
	}
}
