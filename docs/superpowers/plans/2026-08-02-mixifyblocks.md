# MixifyBlocks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Een client-side Fabric 1.21.7 mod die, wanneer je hem met een toets aanzet, na elke geplaatste block naar een willekeurig block-slot binnen een instelbaar hotbar-bereik springt.

**Architecture:** Alle logica draait client-side. Een mixin op `BlockItem.place` detecteert een geslaagde plaatsing en zet een vlag; de volgende client-tick voert de slotwissel uit. De keuze van het slot zit in `SlotPicker`, een pure Java-klasse zonder Minecraft-afhankelijkheden, zodat de kernlogica met gewone JUnit te testen is. Config is een Gson-POJO met een Cloth Config-scherm eroverheen.

**Tech Stack:** Java 21, Fabric Loader 0.19.3, Fabric API, Mixin, Cloth Config 19.0.147, ModMenu 15.0.2, Gson, JUnit 5.

## Global Constraints

- Minecraft `1.21.7`, Mojang mappings (`loom.officialMojangMappings()`), Java release `21`.
- Mod id: `mixifyblocks`. Package: `com.mixifyblocks`. Maven group: `com.mixifyblocks`.
- Client-only mod: `"environment": "client"` in `fabric.mod.json`. Geen server-side code, geen eigen netwerkpakketten.
- Eén source set (`src/main`). **Geen** `splitEnvironmentSourceSets()` — dat zou `src/test` de mod-klassen niet laten zien.
- Exacte dependency-versies: `me.shedaniel.cloth:cloth-config-fabric:19.0.147`, `com.terraformersmc:modmenu:15.0.2`, `net.fabricmc.fabric-api:fabric-api:0.129.0+1.21.7`.
- Slotnummers zijn 1-9 in de config (zoals de speler ze ziet) en 0-8 intern. Conversie uitsluitend via `MixifyConfig.minIndex()` / `maxIndex()`.
- Alle mixin-members krijgen het prefix `mixifyblocks$`.
- Gebruikersteksten in het Engels via `Component.translatable` en `en_us.json`. Geen emoji.
- Deze signaturen zijn geverifieerd tegen de gedecompileerde 1.21.7-jar en mogen niet "gecorrigeerd" worden naar oudere varianten:
  - `BlockItem.place(BlockPlaceContext)` → `InteractionResult`
  - `InteractionResult` is een **interface** met een default-methode `consumesAction()` (sinds 1.21.2 geen enum meer)
  - `Inventory.getSelectedSlot()` / `Inventory.setSelectedSlot(int)` / `Inventory.getItem(int)` / `Inventory.getSelectionSize()` (static)
  - `LocalPlayer.displayClientMessage(Component, boolean)` — tweede argument `true` = actionbar

---

### Task 1: Projectskelet dat bouwt

**Files:**
- Create: `settings.gradle`, `gradle.properties`, `build.gradle`
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat` (kopie uit `../ToggleEnch`)
- Create: `src/main/resources/fabric.mod.json`
- Create: `src/main/resources/mixifyblocks.mixins.json`
- Create: `src/main/resources/assets/mixifyblocks/lang/en_us.json`
- Create: `src/main/java/com/mixifyblocks/MixifyBlocks.java` (nog leeg van gedrag)

**Interfaces:**
- Consumes: niets.
- Produces: `com.mixifyblocks.MixifyBlocks` met `public static final String MOD_ID = "mixifyblocks"` en `public static final Logger LOGGER`. Een werkende `./gradlew build`.

- [ ] **Step 1: Gradle-wrapper kopiëren**

De wrapper wordt niet met de hand geschreven; kopieer de werkende versie uit de zustermod.

```bash
cp -r ../ToggleEnch/gradle ./gradle
cp ../ToggleEnch/gradlew ../ToggleEnch/gradlew.bat ./
```

- [ ] **Step 2: `settings.gradle` schrijven**

```gradle
pluginManagement {
	repositories {
		maven {
			name = 'Fabric'
			url = 'https://maven.fabricmc.net/'
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

rootProject.name = 'mixifyblocks'
```

- [ ] **Step 3: `gradle.properties` schrijven**

```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

# Fabric
minecraft_version=1.21.7
loader_version=0.19.3
loom_version=1.17-SNAPSHOT

# Mod
mod_version=1.0.0
maven_group=com.mixifyblocks

# Dependencies
fabric_api_version=0.129.0+1.21.7
cloth_config_version=19.0.147
modmenu_version=15.0.2
```

- [ ] **Step 4: `build.gradle` schrijven**

De `exclude`-blokken voorkomen dat Cloth Config en ModMenu een afwijkende Fabric API-versie meeslepen.

```gradle
plugins {
	id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group

repositories {
	maven {
		name = 'Shedaniel'
		url = 'https://maven.shedaniel.me/'
	}
	maven {
		name = 'TerraformersMC'
		url = 'https://maven.terraformersmc.com/releases/'
	}
}

loom {
	mods {
		"mixifyblocks" {
			sourceSet sourceSets.main
		}
	}
}

dependencies {
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	mappings loom.officialMojangMappings()
	modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
	modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"

	modApi("me.shedaniel.cloth:cloth-config-fabric:${project.cloth_config_version}") {
		exclude(group: "net.fabricmc.fabric-api")
	}
	modImplementation("com.terraformersmc:modmenu:${project.modmenu_version}") {
		exclude(group: "net.fabricmc.fabric-api")
	}

	testImplementation platform("org.junit:junit-bom:5.10.2")
	testImplementation "org.junit.jupiter:junit-jupiter"
	testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}

processResources {
	def version = project.version
	inputs.property "version", version

	filesMatching("fabric.mod.json") {
		expand "version": version
	}
}

tasks.withType(JavaCompile).configureEach {
	it.options.release = 21
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

test {
	useJUnitPlatform()
}
```

- [ ] **Step 5: `src/main/resources/fabric.mod.json` schrijven**

```json
{
	"schemaVersion": 1,
	"id": "mixifyblocks",
	"version": "${version}",
	"name": "MixifyBlocks",
	"description": "Automatically shuffles your hotbar between a configurable range of block slots after every block you place.",
	"authors": [
		"M1KE1206"
	],
	"contact": {
		"sources": "https://github.com/M1KE1206/MixifyBlocks",
		"issues": "https://github.com/M1KE1206/MixifyBlocks/issues"
	},
	"license": "MIT",
	"environment": "client",
	"entrypoints": {
		"client": [
			"com.mixifyblocks.MixifyBlocks"
		]
	},
	"mixins": [
		{
			"config": "mixifyblocks.mixins.json",
			"environment": "client"
		}
	],
	"depends": {
		"fabricloader": ">=0.19.3",
		"minecraft": "~1.21.7",
		"java": ">=21",
		"fabric-api": "*"
	}
}
```

De entrypoints voor ModMenu en de dependencies op Cloth Config en ModMenu komen er in Task 6 bij, wanneer die klassen bestaan.

- [ ] **Step 6: `src/main/resources/mixifyblocks.mixins.json` schrijven**

```json
{
	"required": true,
	"package": "com.mixifyblocks.mixin",
	"compatibilityLevel": "JAVA_21",
	"client": [],
	"injectors": {
		"defaultRequire": 1
	}
}
```

- [ ] **Step 7: `src/main/resources/assets/mixifyblocks/lang/en_us.json` schrijven**

```json
{
	"key.categories.mixifyblocks": "MixifyBlocks",
	"key.mixifyblocks.toggle": "Toggle block mixing",
	"text.mixifyblocks.enabled": "MixifyBlocks: ON (slots %s-%s)",
	"text.mixifyblocks.disabled": "MixifyBlocks: OFF"
}
```

- [ ] **Step 8: `MixifyBlocks.java` schrijven (kaal)**

```java
package com.mixifyblocks;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixifyBlocks implements ClientModInitializer {
	public static final String MOD_ID = "mixifyblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger("MixifyBlocks");

	@Override
	public void onInitializeClient() {
		LOGGER.info("MixifyBlocks loaded");
	}
}
```

- [ ] **Step 9: Bouwen om te verifiëren dat het skelet klopt**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. De eerste keer duurt dit een paar minuten omdat Minecraft gedownload en geremapt wordt.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "Projectskelet voor Fabric 1.21.7"
```

---

### Task 2: SlotPicker met tests

Dit is de kern van de mod. Bewust zonder enige Minecraft-import, zodat hij zonder draaiend spel te testen is.

**Files:**
- Create: `src/main/java/com/mixifyblocks/SlotPicker.java`
- Test: `src/test/java/com/mixifyblocks/SlotPickerTest.java`

**Interfaces:**
- Consumes: niets.
- Produces: `SlotPicker.pick(boolean[] hasBlock, int minIndex, int maxIndex, int currentIndex, RandomGenerator random)` → `int` (0-gebaseerde slotindex, of `SlotPicker.NO_CHOICE` = `-1`). Alle indexen zijn 0-gebaseerd.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/mixifyblocks/SlotPickerTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mixifyblocks.SlotPickerTest"`
Expected: compilatiefout — `SlotPicker` bestaat nog niet.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/mixifyblocks/SlotPicker.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mixifyblocks.SlotPickerTest"`
Expected: alle 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mixifyblocks/SlotPicker.java src/test/java/com/mixifyblocks/SlotPickerTest.java
git commit -m "SlotPicker met tests"
```

---

### Task 3: MixifyConfig met tests

**Files:**
- Create: `src/main/java/com/mixifyblocks/MixifyConfig.java`
- Test: `src/test/java/com/mixifyblocks/MixifyConfigTest.java`

**Interfaces:**
- Consumes: niets.
- Produces: `MixifyConfig` met publieke velden `int minSlot` (1), `int maxSlot` (9), `boolean enabledOnJoin` (false), `boolean showActionbar` (true); methodes `void normalize()`, `int minIndex()`, `int maxIndex()`, `static MixifyConfig load(Path)`, `void save(Path)`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/mixifyblocks/MixifyConfigTest.java`:

```java
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mixifyblocks.MixifyConfigTest"`
Expected: compilatiefout — `MixifyConfig` bestaat nog niet.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/mixifyblocks/MixifyConfig.java`:

```java
package com.mixifyblocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

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

	/** Klemt beide grenzen op 1-9 en wisselt ze om als ze omgekeerd staan. */
	public void normalize() {
		minSlot = clamp(minSlot);
		maxSlot = clamp(maxSlot);
		if (minSlot > maxSlot) {
			int swap = minSlot;
			minSlot = maxSlot;
			maxSlot = swap;
		}
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
				LOGGER.warn("Kon {} niet lezen, standaardwaarden worden gebruikt", path, e);
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
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			LOGGER.error("Kon {} niet opslaan", path, e);
		}
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mixifyblocks.MixifyConfigTest"`
Expected: alle 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mixifyblocks/MixifyConfig.java src/test/java/com/mixifyblocks/MixifyConfigTest.java
git commit -m "MixifyConfig met tests"
```

---

### Task 4: Keybind, toggle en de slotwissel

Na deze taak is de mod functioneel op één ding na: er is nog geen detectie van block-plaatsing. De wissel wordt daarom tijdelijk met een tweede toets getest.

**Files:**
- Modify: `src/main/java/com/mixifyblocks/MixifyBlocks.java` (volledig herschreven)

**Interfaces:**
- Consumes: `SlotPicker.pick(boolean[], int, int, int, RandomGenerator)`, `SlotPicker.NO_CHOICE`, `MixifyConfig.load(Path)`, `MixifyConfig.minIndex()`, `MixifyConfig.maxIndex()`.
- Produces: `MixifyBlocks.requestSwitch()` (aangeroepen door de mixin in Task 5), `MixifyBlocks.config()` → `MixifyConfig`, `MixifyBlocks.configPath()` → `Path` (beide gebruikt door Task 6).

- [ ] **Step 1: `MixifyBlocks.java` herschrijven**

```java
package com.mixifyblocks;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Random;
import java.util.random.RandomGenerator;

public class MixifyBlocks implements ClientModInitializer {
	public static final String MOD_ID = "mixifyblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger("MixifyBlocks");

	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	private static final RandomGenerator RANDOM = new Random();

	private static MixifyConfig config = new MixifyConfig();
	private static KeyMapping toggleKey;

	/** Sessie-status: staat het mengen aan? Wordt niet opgeslagen. */
	private static boolean enabled;
	/** Er is een block geplaatst; de wissel gebeurt aan het eind van deze tick. */
	private static boolean switchPending;

	@Override
	public void onInitializeClient() {
		config = MixifyConfig.load(CONFIG_PATH);

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.mixifyblocks.toggle", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_N, "key.categories.mixifyblocks"));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			enabled = config.enabledOnJoin;
			switchPending = false;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			enabled = false;
			switchPending = false;
		});

		ClientTickEvents.END_CLIENT_TICK.register(MixifyBlocks::onEndClientTick);
	}

	private static void onEndClientTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			announce(client);
		}

		if (switchPending) {
			switchPending = false;
			performSwitch(client);
		}
	}

	/**
	 * Meldt dat er zojuist een block geplaatst is. De wissel zelf wordt een tick uitgesteld
	 * zodat de inventory niet gemuteerd wordt terwijl de interactie nog afgehandeld wordt.
	 */
	public static void requestSwitch() {
		if (enabled) {
			switchPending = true;
		}
	}

	private static void performSwitch(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		Inventory inventory = player.getInventory();
		boolean[] hasBlock = new boolean[Inventory.getSelectionSize()];
		for (int i = 0; i < hasBlock.length; i++) {
			hasBlock[i] = inventory.getItem(i).getItem() instanceof BlockItem;
		}

		int chosen = SlotPicker.pick(hasBlock, config.minIndex(), config.maxIndex(),
				inventory.getSelectedSlot(), RANDOM);
		if (chosen != SlotPicker.NO_CHOICE) {
			// Vanilla stuurt de wijziging vanzelf naar de server via ensureHasSentCarriedItem().
			inventory.setSelectedSlot(chosen);
		}
	}

	private static void announce(Minecraft client) {
		if (!config.showActionbar || client.player == null) {
			return;
		}
		Component message = enabled
				? Component.translatable("text.mixifyblocks.enabled", config.minSlot, config.maxSlot)
				: Component.translatable("text.mixifyblocks.disabled");
		client.player.displayClientMessage(message, true);
	}

	public static MixifyConfig config() {
		return config;
	}

	public static Path configPath() {
		return CONFIG_PATH;
	}
}
```

- [ ] **Step 2: Compileren**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. Faalt het op `getSelectionSize`, `getSelectedSlot` of `setSelectedSlot`, controleer dan de exacte naam met:
`javap -cp <loom-cache>/minecraft-merged-1.21.7-*.jar net.minecraft.world.entity.player.Inventory`

- [ ] **Step 3: In het spel verifiëren dat de toggle werkt**

Run: `./gradlew runClient`

Maak een wereld in creative, leg blocks in slot 1 tot en met 4, en druk op `N`.
Expected: boven de hotbar verschijnt `MixifyBlocks: ON (slots 1-9)`. Nog een keer `N` geeft `MixifyBlocks: OFF`. Er wordt nog niet van slot gewisseld — dat komt in Task 5.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mixifyblocks/MixifyBlocks.java
git commit -m "Keybind, toggle en slotwissel"
```

---

### Task 5: Block-plaatsing detecteren

**Files:**
- Create: `src/main/java/com/mixifyblocks/mixin/BlockItemMixin.java`
- Modify: `src/main/resources/mixifyblocks.mixins.json` (mixin registreren)

**Interfaces:**
- Consumes: `MixifyBlocks.requestSwitch()`.
- Produces: niets voor andere taken.

`BlockItem.place` is gekozen boven `MultiPlayerGameMode.useItemOn` omdat die laatste ook `SUCCESS` teruggeeft wanneer je met een block in je hand een kist of deur opent — dat zou een ongewenste wissel geven. `place` wordt alleen aangeroepen wanneer er echt een block geplaatst wordt.

- [ ] **Step 1: De mixin schrijven**

`src/main/java/com/mixifyblocks/mixin/BlockItemMixin.java`:

```java
package com.mixifyblocks.mixin;

import com.mixifyblocks.MixifyBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meldt aan {@link MixifyBlocks} dat de lokale speler zojuist een block geplaatst heeft.
 * De client draait deze code ook als voorspelling van wat de server gaat doen, dus dit
 * vuurt zowel in singleplayer als op een server.
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

	@Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
			at = @At("RETURN"))
	private void mixifyblocks$afterPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		// In singleplayer draait deze methode ook op de geïntegreerde server; die kant negeren we.
		if (!context.getLevel().isClientSide()) {
			return;
		}
		if (context.getPlayer() != Minecraft.getInstance().player) {
			return;
		}
		// De offhand staat los van het geselecteerde hotbar-slot.
		if (context.getHand() != InteractionHand.MAIN_HAND) {
			return;
		}

		InteractionResult result = cir.getReturnValue();
		if (result != null && result.consumesAction()) {
			MixifyBlocks.requestSwitch();
		}
	}
}
```

- [ ] **Step 2: De mixin registreren**

Vervang in `src/main/resources/mixifyblocks.mixins.json` de lege `"client"`-lijst:

```json
	"client": [
		"BlockItemMixin"
	],
```

- [ ] **Step 3: Compileren**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. Een `InvalidInjectionException` bij het starten betekent dat de methode-descriptor niet klopt; verifieer met
`javap -p -cp <loom-cache>/minecraft-merged-1.21.7-*.jar net.minecraft.world.item.BlockItem`

- [ ] **Step 4: In het spel verifiëren dat het mengen werkt**

Run: `./gradlew runClient`

1. Creative wereld, vier verschillende blocks in slot 1 tot en met 4, selecteer slot 1.
2. Druk op `N`, bouw dan een muur van tien blocks.
   Expected: na elke geplaatste block springt het geselecteerde slot naar een willekeurig slot binnen 1-9, en de muur bestaat uit gemengde blocks.
3. Houd de rechtermuisknop ingedrukt om een rij te plaatsen.
   Expected: ook dan wisselt hij per geplaatste block.
4. Open een kist terwijl je een block vasthoudt.
   Expected: de kist gaat open en het slot verandert **niet**.
5. Druk op `N` om uit te zetten en bouw verder.
   Expected: het slot verandert niet meer.
6. Herhaal stap 2 in survival.
   Expected: hetzelfde gedrag; raakt een stack op, dan blijft dat slot vanzelf buiten de keuze.

Gebeurt er bij stap 2 niets, dan wordt `place` op deze client niet aangeroepen. Val in dat geval terug op een `@Inject` op `HEAD` en `RETURN` van `MultiPlayerGameMode.useItemOn(LocalPlayer, InteractionHand, BlockHitResult)`: onthoud op `HEAD` of `player.getItemInHand(hand).getItem() instanceof BlockItem` en roep op `RETURN` `requestSwitch()` aan wanneer `cir.getReturnValue().consumesAction()` waar is. Noteer dan wel dat het openen van een kist met een block in de hand een extra wissel kan geven.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mixifyblocks/mixin/BlockItemMixin.java src/main/resources/mixifyblocks.mixins.json
git commit -m "Block-plaatsing detecteren via BlockItem.place"
```

---

### Task 6: Configscherm via Cloth Config en ModMenu

**Files:**
- Create: `src/main/java/com/mixifyblocks/MixifyConfigScreen.java`
- Create: `src/main/java/com/mixifyblocks/ModMenuIntegration.java`
- Modify: `src/main/resources/fabric.mod.json` (modmenu-entrypoint en dependencies)
- Modify: `src/main/resources/assets/mixifyblocks/lang/en_us.json` (schermteksten)

**Interfaces:**
- Consumes: `MixifyBlocks.config()`, `MixifyBlocks.configPath()`, `MixifyConfig.save(Path)`.
- Produces: `MixifyConfigScreen.create(Screen parent)` → `Screen`.

- [ ] **Step 1: Het configscherm schrijven**

`src/main/java/com/mixifyblocks/MixifyConfigScreen.java`:

```java
package com.mixifyblocks;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MixifyConfigScreen {

	private MixifyConfigScreen() {
	}

	public static Screen create(Screen parent) {
		MixifyConfig config = MixifyBlocks.config();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("title.mixifyblocks.config"))
				.setSavingRunnable(() -> config.save(MixifyBlocks.configPath()));

		ConfigCategory general = builder.getOrCreateCategory(
				Component.translatable("category.mixifyblocks.general"));
		ConfigEntryBuilder entries = builder.entryBuilder();

		general.addEntry(entries
				.startIntSlider(Component.translatable("option.mixifyblocks.min_slot"), config.minSlot, 1, 9)
				.setDefaultValue(1)
				.setTooltip(Component.translatable("tooltip.mixifyblocks.min_slot"))
				.setSaveConsumer(value -> config.minSlot = value)
				.build());

		general.addEntry(entries
				.startIntSlider(Component.translatable("option.mixifyblocks.max_slot"), config.maxSlot, 1, 9)
				.setDefaultValue(9)
				.setTooltip(Component.translatable("tooltip.mixifyblocks.max_slot"))
				.setSaveConsumer(value -> config.maxSlot = value)
				.build());

		general.addEntry(entries
				.startBooleanToggle(Component.translatable("option.mixifyblocks.enabled_on_join"), config.enabledOnJoin)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("tooltip.mixifyblocks.enabled_on_join"))
				.setSaveConsumer(value -> config.enabledOnJoin = value)
				.build());

		general.addEntry(entries
				.startBooleanToggle(Component.translatable("option.mixifyblocks.show_actionbar"), config.showActionbar)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("tooltip.mixifyblocks.show_actionbar"))
				.setSaveConsumer(value -> config.showActionbar = value)
				.build());

		return builder.build();
	}
}
```

- [ ] **Step 2: De ModMenu-koppeling schrijven**

`src/main/java/com/mixifyblocks/ModMenuIntegration.java`:

```java
package com.mixifyblocks;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return MixifyConfigScreen::create;
	}
}
```

- [ ] **Step 3: `fabric.mod.json` bijwerken**

Voeg het entrypoint toe:

```json
	"entrypoints": {
		"client": [
			"com.mixifyblocks.MixifyBlocks"
		],
		"modmenu": [
			"com.mixifyblocks.ModMenuIntegration"
		]
	},
```

En de dependencies:

```json
	"depends": {
		"fabricloader": ">=0.19.3",
		"minecraft": "~1.21.7",
		"java": ">=21",
		"fabric-api": "*",
		"cloth-config": ">=19.0.0",
		"modmenu": ">=15.0.0"
	}
```

- [ ] **Step 4: De taalregels aanvullen**

Vervang `src/main/resources/assets/mixifyblocks/lang/en_us.json` door:

```json
{
	"key.categories.mixifyblocks": "MixifyBlocks",
	"key.mixifyblocks.toggle": "Toggle block mixing",
	"text.mixifyblocks.enabled": "MixifyBlocks: ON (slots %s-%s)",
	"text.mixifyblocks.disabled": "MixifyBlocks: OFF",
	"title.mixifyblocks.config": "MixifyBlocks",
	"category.mixifyblocks.general": "General",
	"option.mixifyblocks.min_slot": "Lowest slot",
	"tooltip.mixifyblocks.min_slot": "First hotbar slot that takes part in the mixing.",
	"option.mixifyblocks.max_slot": "Highest slot",
	"tooltip.mixifyblocks.max_slot": "Last hotbar slot that takes part in the mixing. If this is lower than the first slot, the two are swapped automatically.",
	"option.mixifyblocks.enabled_on_join": "Enabled on join",
	"tooltip.mixifyblocks.enabled_on_join": "Turn mixing on automatically when you join a world or server.",
	"option.mixifyblocks.show_actionbar": "Show action bar message",
	"tooltip.mixifyblocks.show_actionbar": "Show a short message above your hotbar when you toggle mixing."
}
```

- [ ] **Step 5: In het spel verifiëren**

Run: `./gradlew runClient`

1. Open Mods, zoek MixifyBlocks, klik op het tandwiel.
   Expected: het configscherm opent met twee sliders en twee schakelaars.
2. Zet het bereik op 1 tot en met 4 en sla op.
   Expected: `.minecraft/config/mixifyblocks.json` bevat `"minSlot": 1, "maxSlot": 4`. In de ontwikkelomgeving staat dit bestand in `run/config/`.
3. Zet blocks in slot 1 tot en met 6, ga op slot 2 staan, zet de mod aan en bouw.
   Expected: hij wisselt alleen tussen slot 1 tot en met 4.
4. Ga op slot 6 staan en bouw.
   Expected: het slot verandert niet.
5. Zet in het scherm de laagste slot op 8 en de hoogste op 3, sla op en heropen het scherm.
   Expected: er staat nu laagste 3 en hoogste 8.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Configscherm via Cloth Config en ModMenu"
```

---

### Task 7: Licentie, README en releasebuild

**Files:**
- Create: `LICENSE`
- Create: `README.md`
- Create: `src/main/resources/assets/mixifyblocks/icon.png` (128x128 of 256x256)
- Modify: `src/main/resources/fabric.mod.json` (`"icon"`-veld)

**Interfaces:**
- Consumes: niets.
- Produces: `build/libs/mixifyblocks-1.0.0.jar`, klaar om te uploaden.

- [ ] **Step 1: De MIT-licentie toevoegen**

Zet in `LICENSE` de standaard MIT-tekst met `Copyright (c) 2026 M1KE1206`. Dit moet overeenkomen met `"license": "MIT"` in `fabric.mod.json` en met de licentie die op Modrinth gekozen is.

- [ ] **Step 2: README schrijven**

`README.md` met: wat de mod doet, de standaardtoets `N`, het gedrag (wisselt na elke geplaatste block, alleen als je huidige slot binnen het bereik ligt, alleen slots met blocks), de instellingen uit de tabel in de spec, de vereiste mods met versies, en een bouwinstructie (`./gradlew build`, jar in `build/libs/`).

- [ ] **Step 3: Icoon toevoegen**

Zet een vierkante PNG op `src/main/resources/assets/mixifyblocks/icon.png` en voeg toe aan `fabric.mod.json`:

```json
	"icon": "assets/mixifyblocks/icon.png",
```

- [ ] **Step 4: Volledige build met alle tests**

Run: `./gradlew clean build`
Expected: `BUILD SUCCESSFUL`, alle 14 tests slagen, en `build/libs/mixifyblocks-1.0.0.jar` bestaat. Upload de jar **zonder** `-sources` en **zonder** `-dev` in de naam.

- [ ] **Step 5: Commit en pushen**

```bash
git add -A
git commit -m "Licentie, README en icoon"
git push
```
