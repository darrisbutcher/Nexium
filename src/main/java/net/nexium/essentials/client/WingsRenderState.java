package net.nexium.essentials.client;

import net.minecraft.world.item.ItemStack;

/** Added to the player render state by a mixin: what the player wears in the wings slot. */
public interface WingsRenderState {
    ItemStack nexium$wings();

    void nexium$setWings(ItemStack wings);
}
