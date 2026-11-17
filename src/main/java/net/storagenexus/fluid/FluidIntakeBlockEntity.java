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
 * Takes up to {@value #BUCKETS_PER_SECOND} buckets per second from the touching source blocks (optionally only one chosen fluid).
 * Infinite water sources (two or more neighbouring water sources) and the Infinite Lava Source
 * are never used up; everything else is drained block by block.
 */
public class FluidIntakeBlockEntity extends BlockEntity {
    /** The buffer holds {@value #BUFFER_BUCKETS} buckets. */
    public static final int BUFFER_BUCKETS = 32;
    /** Buckets taken a second: {@value #BUCKETS_PER_PULL} every {@value #PULL_INTERVAL} ticks. */
    public static final int BUCKETS_PER_SECOND = 8;
    private static final long BUFFER = BUFFER_BUCKETS * FluidConstants.BUCKET;
    private static final int PULL_INTERVAL = 5;
    private static final int BUCKETS_PER_PULL = BUCKETS_PER_SECOND * PULL_INTERVAL / 20;
    /** Pushing as often as pulling keeps the buffer from filling up while something can take the fluid. */
    private static final int PUSH_INTERVAL = 5;

    private int scanIndex;
    /** Only this fluid is taken; blank means anything. */
    private FluidVariant filter = FluidVariant.blank();

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
    public FluidVariant filter() { return filter; }

    public void setFilter(FluidVariant value) {
        filter = value;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidIntakeBlockEntity be) {
        long time = level.getGameTime();
        if (time % PULL_INTERVAL == 0) be.pull(level);
        if (be.buffer.amount > 0 && time % PUSH_INTERVAL == 0) be.push(level);
    }

    /** Runs one pull straight away (tests and the tick both use it). */
    public void pullNow(Level level) {
        pull(level);
    }

    private List<BlockPos> candidates() {
        List<BlockPos> out = new ArrayList<>();
        for (Direction d : Direction.values()) out.add(worldPosition.relative(d));
        return out;
    }

    /** Takes {@value #BUCKETS_PER_PULL} buckets, from as many of the touching sources as it takes (an infinite source gives them all). */
    private void pull(Level level) {
        for (int i = 0; i < BUCKETS_PER_PULL; i++) {
            if (!pullBucket(level)) return;
        }
    }

    /** Takes one bucket if the buffer has room for it and a touching source has one to give. */
    private boolean pullBucket(Level level) {
        if (BUFFER - buffer.amount < FluidConstants.BUCKET) return false;
        List<BlockPos> list = candidates();
        for (int i = 0; i < list.size(); i++) {
            BlockPos p = list.get((scanIndex + i) % list.size());
            BlockState s = level.getBlockState(p);
            FluidState fs = s.getFluidState();
            boolean spring = s.getBlock() instanceof LavaSpringBlock;
            if (!spring && (!fs.isSource() || !(s.getBlock() instanceof LiquidBlock))) continue;
            FluidVariant variant = spring ? FluidVariant.of(Fluids.LAVA) : FluidVariant.of(fs.getType());
            if (!filter.isBlank() && !filter.equals(variant)) continue;
            if (!buffer.isResourceBlank() && !buffer.getResource().equals(variant)) continue;
            // Fill the internal buffer directly (it refuses outside insertion).
            buffer.variant = variant;
            buffer.amount += FluidConstants.BUCKET;
            setChanged();
            if (!spring && !isInfiniteWater(level, p, fs)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            scanIndex = (scanIndex + i + 1) % list.size();
            return true;
        }
        return false;
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
        output.store("filter", FluidVariant.CODEC, filter);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.variant = input.read("fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        buffer.amount = input.getLongOr("amount", 0L);
        filter = input.read("filter", FluidVariant.CODEC).orElse(FluidVariant.blank());
    }
}
