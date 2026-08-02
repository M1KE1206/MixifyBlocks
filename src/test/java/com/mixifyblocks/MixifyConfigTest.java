package com.mixifyblocks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixifyConfigTest {

	@Test
	void standaardwaardenBeslaanDeHeleHotbar() {
		MixifyConfig config = new MixifyConfig();
		assertEquals(1, config.minSlot);
		assertEquals(9, config.maxSlot);
		assertFalse(config.enabledOnJoin);
		assertTrue(config.showActionbar);
	}

	@Test
	void indexenZijnNulGebaseerd() {
		MixifyConfig config = new MixifyConfig();
		config.minSlot = 6;
		config.maxSlot = 9;
		assertEquals(5, config.minIndex());
		assertEquals(8, config.maxIndex());
	}

	@Test
	void omgekeerdBereikWordtOmgewisseld() {
		MixifyConfig config = new MixifyConfig();
		config.minSlot = 7;
		config.maxSlot = 2;
		config.normalize();
		assertEquals(2, config.minSlot);
		assertEquals(7, config.maxSlot);
	}

	@Test
	void waardenBuitenDeHotbarWordenGeklemd() {
		MixifyConfig config = new MixifyConfig();
		config.minSlot = -4;
		config.maxSlot = 42;
		config.normalize();
		assertEquals(1, config.minSlot);
		assertEquals(9, config.maxSlot);
	}

	@Test
	void opslaanEnLadenBehoudtWaarden(@TempDir Path dir) {
		Path path = dir.resolve("mixifyblocks.json");
		MixifyConfig config = new MixifyConfig();
		config.minSlot = 3;
		config.maxSlot = 6;
		config.enabledOnJoin = true;
		config.showActionbar = false;
		config.save(path);

		MixifyConfig loaded = MixifyConfig.load(path);
		assertEquals(3, loaded.minSlot);
		assertEquals(6, loaded.maxSlot);
		assertTrue(loaded.enabledOnJoin);
		assertFalse(loaded.showActionbar);
	}

	@Test
	void ontbrekendBestandGeeftStandaardwaarden(@TempDir Path dir) {
		MixifyConfig loaded = MixifyConfig.load(dir.resolve("bestaat-niet.json"));
		assertEquals(1, loaded.minSlot);
		assertEquals(9, loaded.maxSlot);
	}

	@Test
	void beschadigdBestandGeeftStandaardwaarden(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("mixifyblocks.json");
		Files.writeString(path, "{ dit is geen geldige json");
		MixifyConfig loaded = MixifyConfig.load(path);
		assertEquals(1, loaded.minSlot);
		assertEquals(9, loaded.maxSlot);
	}

	@Test
	void handmatigBewerktBestandMetWaardenBuitenBereikWordtGeklemd(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("mixifyblocks.json");
		Files.writeString(path, "{\"minSlot\": 40, \"maxSlot\": -3}");
		MixifyConfig loaded = MixifyConfig.load(path);
		assertEquals(1, loaded.minSlot);
		assertEquals(9, loaded.maxSlot);
	}
}
