package com.mixifyblocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Instellingen van de mod. Puur Java plus Gson, zodat dit zonder draaiend spel te testen is.
 * Slotnummers zijn hier 1-9 zoals de speler ze ziet; {@link #minIndex()} en {@link #maxIndex()}
 * zijn de enige plek waar naar 0-gebaseerde indexen wordt omgerekend.
 */
public class MixifyConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("MixifyBlocks");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final int FIRST_SLOT = 1;
	private static final int LAST_SLOT = 9;

	public int minSlot = FIRST_SLOT;
	public int maxSlot = LAST_SLOT;
	public boolean enabledOnJoin = false;
	public boolean showActionbar = true;
	public int maxSameInRow = 3;

	/** Klemt beide grenzen op 1-9 en wisselt ze om als ze omgekeerd staan. */
	public void normalize() {
		minSlot = clamp(minSlot);
		maxSlot = clamp(maxSlot);
		if (minSlot > maxSlot) {
			int swap = minSlot;
			minSlot = maxSlot;
			maxSlot = swap;
		}
		maxSameInRow = clamp(maxSameInRow);
	}

	private static int clamp(int slot) {
		return Math.max(FIRST_SLOT, Math.min(LAST_SLOT, slot));
	}

	public int minIndex() {
		return minSlot - 1;
	}

	public int maxIndex() {
		return maxSlot - 1;
	}

	public static MixifyConfig load(Path path) {
		MixifyConfig config = new MixifyConfig();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				MixifyConfig read = GSON.fromJson(reader, MixifyConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (Exception e) {
				LOGGER.warn("Could not read {}, falling back to default values: {}", path, e.getMessage());
				config = new MixifyConfig();
			}
		}
		config.normalize();
		return config;
	}

	public void save(Path path) {
		normalize();
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			// Write to a sibling temp file first and move it into place, so a failure mid-write
			// (disk full, permission error, ...) can never leave a truncated config on disk.
			Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(this, writer);
			}
			try {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			LOGGER.error("Could not save {}", path, e);
		}
	}
}
