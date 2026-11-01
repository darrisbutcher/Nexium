package net.storagenexus.essentials.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the Iron Farm chest check whenever a chest's contents change, whoever changed them. */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void nexusessentials$ironFarmChestChanged(CallbackInfo ci) {
        if ((Object) this instanceof ChestBlockEntity chest) IronFarmChest.changed(chest);
    }
}
