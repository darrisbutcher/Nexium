package net.storagenexus.network;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.storagenexus.util.FluidItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/**
 * One item, an amount, and whether to fire when the network has at least or fewer than that amount.
 * A filled bucket also counts that fluid stored in the network, in buckets.
 */
public class LevelEmitterBlockEntity extends BlockEntity {
    public static final int MAX_AMOUNT = 100_000_000;
    private static final int INTERVAL = 10;

    private ItemStack item = ItemStack.EMPTY;
    private int amount = 1;
    private boolean below;
    private long lastCount;

    public LevelEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEVEL_EMITTER, pos, state);
    }

    public ItemStack item() { return item; }
    public int amount() { return amount; }
    public boolean below() { return below; }
    public long lastCount() { return lastCount; }

    public void setItem(ItemStack stack) {
        item = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        changed();
    }

    public void setAmount(int value) {
        amount = Math.clamp(value, 0, MAX_AMOUNT);
        changed();
    }

    public void toggleBelow() {
        below = !below;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) evaluate(level, worldPosition, getBlockState());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LevelEmitterBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        be.evaluate(level, pos, state);
    }

    /** Re-reads the network and updates the signal; public so tests can run it directly. */
    public void evaluate(Level level, BlockPos pos, BlockState state) {
        boolean on = false;
        if (!item.isEmpty()) {
            StorageNetwork net = StorageNetwork.find(level, pos);
            if (net != null) {
                // A bucket of something counts that fluid too, in whole buckets
                FluidVariant fluid = FluidItems.fluidIn(item);
                lastCount = net.countItem(ItemVariant.of(item))
                        + (fluid == null ? 0 : net.countFluid(fluid) / FluidConstants.BUCKET);
                on = below ? lastCount < amount : lastCount >= amount;
            }
        } else {
            lastCount = 0;
        }
        if (on != state.getValue(LevelEmitterBlock.POWERED)) {
            level.setBlock(pos, state.setValue(LevelEmitterBlock.POWERED, on), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("item", ItemStack.OPTIONAL_CODEC, item);
        output.putInt("amount", amount);
        output.putBoolean("below", below);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        item = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        amount = input.getIntOr("amount", 1);
        below = input.getBooleanOr("below", false);
    }
}
