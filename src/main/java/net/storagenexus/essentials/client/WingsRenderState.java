package net.storagenexus.essentials.client;

import net.minecraft.world.item.ItemStack;

/** Added to the player render state by a mixin: what the player wears in the wings slot. */
public interface WingsRenderState {
    ItemStack nexusessentials$wings();

    void nexusessentials$setWings(ItemStack wings);
}
