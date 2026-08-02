package com.mixifyblocks;

/**
 * Telt hoe vaak achter elkaar hetzelfde bloktype geplaatst is.
 * Vergelijkt op identiteit, dus Minecraft-types zijn hier niet nodig; dat houdt deze klasse
 * testbaar zonder het spel te bootstrappen.
 */
public final class PlacementStreak {
	private Object lastItem;
	private int count;

	/** Registreert een plaatsing. Een ander item laat de telling opnieuw op 1 beginnen. */
	public void record(Object item) {
		if (item != null && item == lastItem) {
			count++;
		} else {
			lastItem = item;
			count = 1;
		}
	}

	/** Is het toegestane aantal identieke plaatsingen op rij bereikt? */
	public boolean limitReached(int maxSameInRow) {
		return lastItem != null && count >= maxSameInRow;
	}

	/** Het laatst geplaatste item, of null als er nog niets geplaatst is. */
	public Object lastItem() {
		return lastItem;
	}

	public void reset() {
		lastItem = null;
		count = 0;
	}
}
