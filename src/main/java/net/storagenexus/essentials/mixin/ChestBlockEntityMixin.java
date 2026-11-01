package net.storagenexus.essentials.mixin;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player closing a "Nexus Iron Farm" chest that isn't ready is told what's missing. */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {
    @Inject(method = "stopOpen", at = @At("TAIL"))
    private void nexusessentials$ironFarmChestClosed(ContainerUser user, CallbackInfo ci) {
        IronFarmChest.closed((ChestBlockEntity) (Object) this, user);
    }
}
