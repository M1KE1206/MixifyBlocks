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
				.startIntSlider(Component.translatable("option.mixifyblocks.max_same_in_row"), config.maxSameInRow, 1, 9)
				.setDefaultValue(3)
				.setTooltip(Component.translatable("tooltip.mixifyblocks.max_same_in_row"))
				.setSaveConsumer(value -> config.maxSameInRow = value)
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
