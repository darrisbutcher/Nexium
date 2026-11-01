package net.storagenexus.essentials.bedrock;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.essentials.registry.ModEffects;
import net.storagenexus.essentials.registry.ModTags;

/**
 * With the Bedrock Breaker effect, bedrock mines like obsidian: it needs a diamond or netherite pickaxe
 * (anything that can mine obsidian), and Efficiency, Haste and Mining Fatigue apply as usual. It drops nothing.
 */
public final class BedrockBreaking {
    /** Obsidian's hardness. */
    public static final float HARDNESS = 50.0F;

    public static boolean applies(BlockState state, Player player) {
        return state.is(ModTags.BEDROCK_BREAKER_MINEABLE) && player.hasEffect(ModEffects.BEDROCK_BREAKER);
    }

    /** Fraction of the block broken per tick, as vanilla computes it for a block of obsidian. */
    public static float destroyProgress(Player player) {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        if (!player.hasCorrectToolForDrops(obsidian)) return 0.0F;
        return player.getDestroySpeed(obsidian) / HARDNESS / 30.0F;
    }

    private BedrockBreaking() {}
}
