package net.storagenexus.client;

import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.freezer.FreezerBlockEntity;
import net.storagenexus.registry.ModBlocks;

import java.util.List;

/** Colours the Freezer's body and frame with its dye colour; the frost, glow, water and lights are not tinted. */
public final class FreezerTint implements BlockTintSource {
    public static void init() {
        BlockColorRegistry.register(List.of(new FreezerTint()), ModBlocks.FREEZER);
    }

    @Override
    public int color(BlockState state) {
        return 0xFF000000 | FreezerBlockEntity.UNDYED;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FreezerBlockEntity freezer ? 0xFF000000 | freezer.color() : color(state);
    }

    private FreezerTint() {}
}
