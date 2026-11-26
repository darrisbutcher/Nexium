package net.nexium.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.registry.ModBlocks;

/**
 * The Conduit: one item that carries items, fluids and Nexus Energy together. It places a Tier 1 conduit that has all three; an
 * {@link ConduitUpgradeItem Advanced Conduit Upgrade} makes it Tier 2. Used on a conduit from before the three became one (which can
 * have only some of them), it adds the ones that are missing.
 */
public class ConduitItem extends Item {
    public ConduitItem(Properties properties) {
        super(properties);
    }

    /** Gives a conduit every type it lacks at Tier 1. Returns whether anything was added. */
    public static boolean complete(ConduitBundleBlockEntity be) {
        boolean added = false;
        for (ConduitType t : ConduitType.VALUES) {
            if (be.has(t)) continue;
            be.addConduit(t, 1);
            added = true;
        }
        return added;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // A conduit that lacks some type (placed before the Conduit became one item) gets the missing ones
        if (level.getBlockEntity(context.getClickedPos()) instanceof ConduitBundleBlockEntity be && be.typeMask() != 7) {
            if (!level.isClientSide()) {
                complete(be);
                if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, context.getClickedPos(), SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }

        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos pos = place.getClickedPos();
        if (level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity existing) {
            if (existing.typeMask() == 7) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                complete(existing);
                if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        if (!place.canPlace()) return InteractionResult.FAIL;
        BlockState state = ModBlocks.CONDUIT_BUNDLE.defaultBlockState();
        if (!level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlock(pos, state, 3);
            if (level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be) complete(be);
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1f);
        }
        return InteractionResult.SUCCESS;
    }
}
