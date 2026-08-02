package com.mixifyblocks.mixin;

import com.mixifyblocks.MixifyBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
			MixifyBlocks.requestSwitch((Item) (Object) this);
		}
	}
}
