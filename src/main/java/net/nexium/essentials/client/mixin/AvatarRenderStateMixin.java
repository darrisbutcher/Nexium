package net.nexium.essentials.client.mixin;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.client.WingsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
abstract class AvatarRenderStateMixin implements WingsRenderState {
    @Unique
    private ItemStack nexium$wings = ItemStack.EMPTY;

    @Override
    public ItemStack nexium$wings() {
        return nexium$wings;
    }

    @Override
    public void nexium$setWings(ItemStack wings) {
        nexium$wings = wings;
    }
}
