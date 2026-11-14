package net.storagenexus.essentials.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The creative inventory sends every slot change as a "set creative slot" packet, which vanilla only accepts
 * for slots 1-45. Accept the trinket, wings, arrow, totem and backpack slots too, or creative players couldn't use them.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;itemStack()Lnet/minecraft/world/item/ItemStack;"),
            cancellable = true)
    private void nexusessentials$acceptExtraSlots(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        int index = packet.slotNum();
        if (index < ExtraSlots.MENU_TRINKET || index > ExtraSlots.MENU_BACKPACK) return;
        ci.cancel();
        ItemStack stack = packet.itemStack();
        if (index >= player.inventoryMenu.slots.size() || !stack.isItemEnabled(player.level().enabledFeatures())) return;
        Slot slot = player.inventoryMenu.getSlot(index);
        if (!stack.isEmpty() && (!slot.mayPlace(stack) || stack.getCount() > slot.getMaxStackSize())) return;
        slot.setByPlayer(stack);
        player.inventoryMenu.setRemoteSlot(index, stack);
        player.inventoryMenu.broadcastChanges();
    }
}
