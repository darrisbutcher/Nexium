package net.storagenexus.essentials.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Arrows a player picks up (or is given) go to the arrow slot first, so they don't use up inventory space. */
@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void nexusessentials$arrowsToTheSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Only "put it wherever it fits" (slot -1), never a specific slot, and only on the server
        if (slot != -1 || stack.isEmpty() || !ExtraSlots.isArrows(stack) || player.level().isClientSide()) return;
        ExtraSlots.of(player).moveArrowsIn(stack);
        if (stack.isEmpty()) cir.setReturnValue(true);
    }
}
