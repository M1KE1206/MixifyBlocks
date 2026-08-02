package com.mixifyblocks;

import java.util.random.RandomGenerator;

/**
 * Kiest een willekeurig hotbar-slot dat een block bevat, binnen een bereik.
 * Bewust vrij van Minecraft-afhankelijkheden zodat dit los te testen is.
 * Alle indexen zijn 0-gebaseerd.
 */
public final class SlotPicker {
	/** Er valt niets te kiezen; de aanroeper moet het huidige slot laten staan. */
	public static final int NO_CHOICE = -1;

	private SlotPicker() {
	}

	public static int pick(boolean[] hasBlock, int minIndex, int maxIndex, int currentIndex, RandomGenerator random) {
		if (currentIndex < minIndex || currentIndex > maxIndex) {
			return NO_CHOICE;
		}

		int candidates = 0;
		for (int i = minIndex; i <= maxIndex; i++) {
			if (hasBlock[i]) {
				candidates++;
			}
		}
		if (candidates == 0) {
			return NO_CHOICE;
		}

		// Loop de kandidaten langs tot de willekeurig gekozen n-de bereikt is.
		int nth = random.nextInt(candidates);
		for (int i = minIndex; i <= maxIndex; i++) {
			if (hasBlock[i] && nth-- == 0) {
				return i;
			}
		}
		return NO_CHOICE;
	}
}
