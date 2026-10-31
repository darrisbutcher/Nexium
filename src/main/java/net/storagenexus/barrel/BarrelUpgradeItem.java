package net.storagenexus.barrel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.registry.ModBlocks;

/** Right-click a barrel of the tier below (or a vanilla barrel, for iron) to upgrade it in place, keeping what is inside. */
public class BarrelUpgradeItem extends Item {
    private final BarrelTier tier;

    public BarrelUpgradeItem(Properties properties, BarrelTier tier) {
        super(properties);
        this.tier = tier;
    }

    public BarrelTier tier() { return tier; }

    /** The block this upgrade applies to. */
    public Block from() {
        BarrelTier prev = tier.previous();
        return prev == null ? Blocks.BARREL : ModBlocks.BARRELS.get(prev);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.getBlockState(pos).is(from())) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (!apply(server, pos)) return InteractionResult.FAIL;
            server.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
            if (player != null && !player.isCreative()) context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** Swaps the barrel at {@code pos} for this tier's, keeping its contents and facing. False if it is not the right barrel. */
    public boolean apply(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(from())) return false;
        BlockEntity old = level.getBlockEntity(pos);
        if (!(old instanceof Container c)) return false;
        // Take everything out first so removing the old block drops nothing
        NonNullList<ItemStack> contents = NonNullList.create();
        for (int i = 0; i < c.getContainerSize(); i++) {
            contents.add(c.getItem(i));
            c.setItem(i, ItemStack.EMPTY);
        }
        BlockState next = ModBlocks.BARRELS.get(tier).defaultBlockState()
                .setValue(TieredBarrelBlock.FACING, state.getValue(BarrelBlock.FACING));
        level.setBlock(pos, next, 3);
        if (level.getBlockEntity(pos) instanceof TieredBarrelBlockEntity be) {
            for (int i = 0; i < contents.size() && i < be.getContainerSize(); i++) be.setItem(i, contents.get(i));
        }
        return true;
    }
}
