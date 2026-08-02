package com.mixifyblocks;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotPickerTest {

	/** Alle negen slots bevatten een block. */
	private static boolean[] allBlocks() {
		boolean[] hasBlock = new boolean[9];
		java.util.Arrays.fill(hasBlock, true);
		return hasBlock;
	}

	private static RandomGenerator seeded() {
		return new Random(1234L);
	}

	@Test
	void kiestAlleenSlotsBinnenHetBereik() {
		for (int i = 0; i < 200; i++) {
			int chosen = SlotPicker.pick(allBlocks(), 0, 3, 2, seeded());
			assertTrue(chosen >= 0 && chosen <= 3, "slot buiten bereik: " + chosen);
		}
	}

	@Test
	void slaatLegeSlotsOver() {
		boolean[] hasBlock = new boolean[9];
		hasBlock[1] = true;
		hasBlock[3] = true;
		Set<Integer> seen = new HashSet<>();
		RandomGenerator random = seeded();
		for (int i = 0; i < 200; i++) {
			seen.add(SlotPicker.pick(hasBlock, 0, 3, 0, random));
		}
		assertEquals(Set.of(1, 3), seen);
	}

	@Test
	void geeftGeenKeuzeAlsErGeenBlocksZijn() {
		assertEquals(SlotPicker.NO_CHOICE, SlotPicker.pick(new boolean[9], 0, 3, 1, seeded()));
	}

	@Test
	void geeftHetEnigeBlockSlotTerug() {
		boolean[] hasBlock = new boolean[9];
		hasBlock[2] = true;
		assertEquals(2, SlotPicker.pick(hasBlock, 0, 3, 0, seeded()));
	}

	@Test
	void geeftGeenKeuzeAlsHuidigSlotBuitenHetBereikLigt() {
		assertEquals(SlotPicker.NO_CHOICE, SlotPicker.pick(allBlocks(), 0, 3, 6, seeded()));
	}

	@Test
	void magHetzelfdeSlotOpnieuwKiezen() {
		RandomGenerator random = seeded();
		boolean sameAsCurrent = false;
		for (int i = 0; i < 200; i++) {
			if (SlotPicker.pick(allBlocks(), 0, 3, 2, random) == 2) {
				sameAsCurrent = true;
				break;
			}
		}
		assertTrue(sameAsCurrent, "herhaling van het huidige slot moet mogelijk zijn");
	}

	@Test
	void bereiktElkKandidaatSlotOverVeelTrekkingen() {
		RandomGenerator random = seeded();
		Set<Integer> seen = new HashSet<>();
		for (int i = 0; i < 500; i++) {
			seen.add(SlotPicker.pick(allBlocks(), 5, 8, 6, random));
		}
		assertEquals(Set.of(5, 6, 7, 8), seen);
	}
}
