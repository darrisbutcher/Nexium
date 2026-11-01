package net.storagenexus.essentials.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.client.WingsRenderState;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Draws the elytra from the wings slot when the chest slot holds a chestplate (or nothing). */
@Mixin(WingsLayer.class)
abstract class WingsLayerMixin {
    @ModifyExpressionValue(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nexusessentials$useWingsSlot(ItemStack chest, @Local(argsOnly = true) HumanoidRenderState state) {
        if (ExtraSlots.isWings(chest) || !(state instanceof WingsRenderState wingsState)) return chest;
        ItemStack wings = wingsState.nexusessentials$wings();
        return wings.isEmpty() ? chest : wings;
    }
}
