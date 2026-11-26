package net.nexium.essentials.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.nexium.essentials.backpack.BackpackRestocking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the Restocking module a player clicked in a menu this tick, so a hotbar stack of one or two that was shift-clicked or picked up
 * is not taken for the last blocks used up and handed straight back.
 */
@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void nexium$noteClick(int slotIndex, int buttonNum, ContainerInput input, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) BackpackRestocking.noteMenuClick(serverPlayer);
    }
}
