package net.storagenexus.essentials.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.storagenexus.essentials.MobGriefing;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item frames, paintings, armor stands, dropped items, minecarts and boats already ignore explosions that
 * vanilla says shouldn't affect "block-like" entities; say so for creeper and ghast explosions.
 */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private @Nullable Entity source;

    @Inject(method = "shouldAffectBlocklikeEntities", at = @At("HEAD"), cancellable = true)
    private void nexusessentials$spareDecorations(CallbackInfoReturnable<Boolean> cir) {
        if (MobGriefing.sparesDecorations(source)) cir.setReturnValue(false);
    }
}
