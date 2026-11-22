package net.nexium.aiot.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.nexium.aiot.module.AiotMining;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The extra blocks of a vein, 3x3 or tunnel swing drop their items where the player broke the first block (when the
 * {@code aiotDropsAtFirstBlock} setting is on), instead of each at its own spot. This only moves where a dropped stack appears.
 */
@Mixin(Block.class)
abstract class AiotDropsBlockMixin {
    @ModifyVariable(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), argsOnly = true)
    private static BlockPos nexium$dropAtFirstBlock(BlockPos pos, Level level) {
        BlockPos origin = AiotMining.dropOrigin();
        return origin != null && !level.isClientSide() ? origin : pos;
    }
}
