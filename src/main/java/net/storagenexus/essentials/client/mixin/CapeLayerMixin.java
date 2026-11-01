package net.storagenexus.essentials.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.client.WingsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vanilla hides the cape under an elytra; do the same for an elytra in the wings slot. */
@Mixin(CapeLayer.class)
abstract class CapeLayerMixin {
    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;)Z"))
    private boolean nexusessentials$hideCapeUnderWings(CapeLayer layer, ItemStack stack, EquipmentClientInfo.LayerType type,
                                                       Operation<Boolean> original, @Local(argsOnly = true) AvatarRenderState state) {
        if (type == EquipmentClientInfo.LayerType.WINGS && !((WingsRenderState) state).nexusessentials$wings().isEmpty()) return true;
        return original.call(layer, stack, type);
    }
}
