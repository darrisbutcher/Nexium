package net.storagenexus.essentials.mixin;

import net.storagenexus.essentials.MobGriefing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Endermen never pick up blocks. One already carrying a block (from before the mod) can still put it down. */
@Mixin(targets = "net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal")
abstract class EndermanTakeBlockGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void nexusessentials$neverTakeBlocks(CallbackInfoReturnable<Boolean> cir) {
        if (!MobGriefing.endermenTakeBlocks()) cir.setReturnValue(false);
    }
}
