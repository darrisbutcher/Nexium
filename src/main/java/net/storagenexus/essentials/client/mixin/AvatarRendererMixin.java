package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.client.WingsRenderState;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the wings slot into the render state so the elytra layer can draw it over a chestplate. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void nexusessentials$extractWings(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        ItemStack wings = entity instanceof Player player ? ExtraSlots.of(player).wings() : ItemStack.EMPTY;
        ((WingsRenderState) state).nexusessentials$setWings(wings.isEmpty() ? ItemStack.EMPTY : wings.copy());
    }
}
