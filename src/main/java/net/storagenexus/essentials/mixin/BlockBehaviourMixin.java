package net.storagenexus.essentials.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.essentials.bedrock.BedrockBreaking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets bedrock be mined under the Bedrock Breaker effect. Runs on both sides, so the crack animation matches the server. */
@Mixin(BlockBehaviour.class)
abstract class BlockBehaviourMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void nexusessentials$bedrockBreaker(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (BedrockBreaking.applies(state, player)) cir.setReturnValue(BedrockBreaking.destroyProgress(player));
    }
}
