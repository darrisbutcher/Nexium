package net.storagenexus.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.network.NetworkBlock;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes one bucket per second from a nearby source. Infinite water sources (two or more
 * neighbouring water sources) are never removed; everything else is drained block by block.
 */
public class FluidIntakeBlockEntity extends BlockEntity {
    private static final long BUFFER = 4 * FluidConstants.BUCKET;
    private static final int PULL_INTERVAL = 20;
    /** The buffer holds 4 buckets and fills by one a second, so pushing every half second keeps up. */
    private static final int PUSH_INTERVAL = 10;

    private boolean rangeUpgrade;
    private int scanIndex;

    private final SingleVariantStorage<FluidVariant> buffer = new SingleVariantStorage<>() {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return BUFFER; }
        @Override protected void onFinalCommit() { setChanged(); }
        @Override public boolean supportsInsertion() { return false; }
    };

    /** Outside view: extract only. */
    private final Storage<FluidVariant> exposed = new net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage<>(buffer) {
        @Override protected boolean canInsert(FluidVariant resource) { return false; }
    };

    public FluidIntakeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_INTAKE, pos, state);
    }

    public Storage<FluidVariant> getFluidStorage() { return exposed; }
    public long bufferAmount() { return buffer.amount; }
    public boolean hasRangeUpgrade() { return rangeUpgrade; }

    public boolean installRangeUpgrade() {
        if (rangeUpgrade) return false;
        rangeUpgrade = true;
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidIntakeBlockEntity be) {
        long time = level.getGameTime();
        if (time % PULL_INTERVAL == 0) be.pull(level);
        if (be.buffer.amount > 0 && time % PUSH_INTERVAL == 0) be.push(level);
    }

    private List<BlockPos> candidates() {
        List<BlockPos> out = new ArrayList<>();
        for (Direction d : Direction.values()) out.add(worldPosition.relative(d));
        if (rangeUpgrade) {
            for (int dy = 0; dy >= -1; dy--) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos p = worldPosition.offset(dx, dy, dz);
                        if (!p.equals(worldPosition) && !out.contains(p)) out.add(p);
                    }
                }
            }
        }
        return out;
    }

    private void pull(Level level) {
        if (BUFFER - buffer.amount < FluidConstants.BUCKET) return;
        List<BlockPos> list = candidates();
        for (int i = 0; i < list.size(); i++) {
            BlockPos p = list.get((scanIndex + i) % list.size());
            BlockState s = level.getBlockState(p);
            FluidState fs = s.getFluidState();
            if (!fs.isSource() || !(s.getBlock() instanceof LiquidBlock)) continue;
            FluidVariant variant = FluidVariant.of(fs.getType());
            if (!buffer.isResourceBlank() && !buffer.getResource().equals(variant)) continue;
            // Fill the internal buffer directly (it refuses outside insertion).
            buffer.variant = variant;
            buffer.amount += FluidConstants.BUCKET;
            setChanged();
            if (!isInfiniteWater(level, p, fs)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            scanIndex = (scanIndex + i + 1) % list.size();
            return;
        }
    }

    private static boolean isInfiniteWater(Level level, BlockPos pos, FluidState fs) {
        if (!fs.getType().isSame(Fluids.WATER)) return false;
        int sources = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            FluidState n = level.getFluidState(pos.relative(d));
            if (n.isSource() && n.getType().isSame(Fluids.WATER)) sources++;
        }
        return sources >= 2;
    }

    private void push(Level level) {
        for (Direction d : Direction.values()) {
            if (buffer.amount <= 0) return;
            BlockPos np = worldPosition.relative(d);
            try (Transaction tx = Transaction.openOuter()) {
                long moved;
                if (level.getBlockState(np).getBlock() instanceof NetworkBlock) {
                    StorageNetwork net = StorageNetwork.find(level, np);
                    if (net == null) continue;
                    moved = net.insertFluid(buffer.variant, buffer.amount, tx);
                } else {
                    Storage<FluidVariant> target = FluidStorage.SIDED.find(level, np, d.getOpposite());
                    if (target == null) continue;
                    moved = target.insert(buffer.variant, buffer.amount, tx);
                }
                if (moved > 0) {
                    buffer.extract(buffer.variant, moved, tx);
                    tx.commit();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("fluid", FluidVariant.CODEC, buffer.variant);
        output.putLong("amount", buffer.amount);
        output.putBoolean("range_upgrade", rangeUpgrade);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.variant = input.read("fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        buffer.amount = input.getLongOr("amount", 0L);
        rangeUpgrade = input.getBooleanOr("range_upgrade", false);
    }
}
