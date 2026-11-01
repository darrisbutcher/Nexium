package net.storagenexus.essentials.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Netherite armor counts as gold armor: one piece keeps piglins calm. */
@Mixin(PiglinAi.class)
abstract class PiglinAiMixin {
    @Inject(method = "isWearingSafeArmor", at = @At("RETURN"), cancellable = true)
    private static void nexusessentials$netheriteCountsAsGold(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !NexusConfig.get().netheriteArmorCalmsPiglins) return;
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if (entity.getItemBySlot(slot).is(ModTags.CALMS_PIGLINS)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
