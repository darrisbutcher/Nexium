package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.client.WingsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
abstract class AvatarRenderStateMixin implements WingsRenderState {
    @Unique
    private ItemStack nexusessentials$wings = ItemStack.EMPTY;

    @Override
    public ItemStack nexusessentials$wings() {
        return nexusessentials$wings;
    }

    @Override
    public void nexusessentials$setWings(ItemStack wings) {
        nexusessentials$wings = wings;
    }
}
