package net.storagenexus.essentials.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.aiot.module.NexusLink;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A block mined by a player whose AIOT has an active Nexus Link drops into the linked storage network instead of onto
 * the ground (an AIOT feature; it lives here because this is the mod's one mixin package).
 */
@Mixin(Block.class)
abstract class BlockMixin {
    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void storagenexus$nexusLink(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity,
                                               @Nullable Entity breaker, ItemStack tool, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && breaker instanceof ServerPlayer player
                && NexusLink.dropInto(state, serverLevel, pos, blockEntity, player, tool)) {
            ci.cancel();
        }
    }
}
