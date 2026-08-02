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
import net.minecraft.world.item.Item;
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
	private static final PlacementStreak STREAK = new PlacementStreak();

	/** Sessie-status: staat het mengen aan? Wordt niet opgeslagen. */
	private static boolean enabled;
	/**
	 * Er is een block geplaatst; de wissel gebeurt aan het eind van deze tick.
	 * Bewust een boolean en geen teller: {@code Minecraft.handleKeybinds()} verwerkt
	 * gebufferde rechtermuisklikken in een onbegrensde while-lus binnen dezelfde tick, dus
	 * meerdere plaatsingen in één tick smelten samen tot één wissel. Dat is geaccepteerd
	 * gedrag, geen bug. Eager wisselen binnen {@link #requestSwitch(Item)} zou erger zijn:
	 * {@code Minecraft.startUseItem()} leest het vastgehouden item opnieuw uit nadat
	 * {@code useItemOn} teruggekeerd is, dus het slot midden in de interactie muteren
	 * riskeert plaatsen vanuit het nieuwe slot.
	 */
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
			STREAK.reset();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			enabled = false;
			switchPending = false;
			STREAK.reset();
		});

		ClientTickEvents.END_CLIENT_TICK.register(MixifyBlocks::onEndClientTick);

		LOGGER.info("MixifyBlocks loaded (slot range {}-{})", config.minSlot, config.maxSlot);
	}

	private static void onEndClientTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			STREAK.reset();
			announce(client);
		}

		if (switchPending) {
			switchPending = false;
			if (enabled) {
				performSwitch(client);
			}
		}
	}

	/**
	 * Meldt dat er zojuist een block geplaatst is. De wissel zelf gebeurt pas aan het eind
	 * van dezelfde client-tick, zodat de inventory niet gemuteerd wordt terwijl de interactie
	 * nog afgehandeld wordt. {@code placedItem} voedt de {@link PlacementStreak}, zodat die
	 * op bloktype kan tellen ongeacht welk slot het lag.
	 */
	public static void requestSwitch(Item placedItem) {
		if (!enabled) {
			return;
		}
		STREAK.record(placedItem);
		switchPending = true;
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

		boolean[] candidates = hasBlock;
		if (STREAK.limitReached(config.maxSameInRow)) {
			candidates = new boolean[hasBlock.length];
			for (int i = 0; i < candidates.length; i++) {
				candidates[i] = hasBlock[i] && inventory.getItem(i).getItem() != STREAK.lastItem();
			}
		}

		int chosen = SlotPicker.pick(candidates, config.minIndex(), config.maxIndex(),
				inventory.getSelectedSlot(), RANDOM);
		if (chosen == SlotPicker.NO_CHOICE && candidates != hasBlock) {
			// Alleen dit bloktype beschikbaar: liever herhalen dan helemaal niet wisselen.
			chosen = SlotPicker.pick(hasBlock, config.minIndex(), config.maxIndex(),
					inventory.getSelectedSlot(), RANDOM);
		}
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
