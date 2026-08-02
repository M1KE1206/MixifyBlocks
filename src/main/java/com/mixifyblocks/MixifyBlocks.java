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
