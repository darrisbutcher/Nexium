package net.storagenexus.conduit;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.dimension.DimensionalOutputBlock;
import net.storagenexus.filter.FilterData;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.registry.ModItems;
import net.storagenexus.util.StorageEvents;
import net.storagenexus.util.SyncedBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * One block space holding item and/or fluid conduits (Ender IO style).
 * Every side has its own settings per conduit type: mode, colour channel, redstone control,
 * priority, round-robin, filter and speed upgrades.
 */
public class ConduitBundleBlockEntity extends SyncedBlockEntity {
    private static final int MAX_NETWORK = 1024;
    private static final int FLUID_INTERVAL = 5;
    private static final long DROPLETS_PER_MB = 81;

    private final int[] tiers = new int[ConduitType.VALUES.length];
    private final SideConfig[][] configs = new SideConfig[ConduitType.VALUES.length][6];
    private @Nullable BlockState facade;

    private record Target(BlockPos invPos, Direction side, SideConfig cfg, int distance) {}

    private final Map<Integer, List<Target>> targetCache = new HashMap<>();
    // Client-side cache for the channel markers drawn on each arm
    private final int[][] markers = new int[ConduitType.VALUES.length][6];
    private long markersAt = -1;
    private long cacheVersion = -1;

    public ConduitBundleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDUIT_BUNDLE, pos, state);
        for (ConduitType t : ConduitType.VALUES) {
            for (Direction d : Direction.values()) configs[t.ordinal()][d.get3DDataValue()] = new SideConfig(t);
        }
    }

    // ---- Contents ---------------------------------------------------------------------------

    public boolean has(ConduitType t) { return tiers[t.ordinal()] > 0; }
    public int tier(ConduitType t) { return tiers[t.ordinal()]; }
    public SideConfig config(ConduitType t, Direction d) { return configs[t.ordinal()][d.get3DDataValue()]; }
    public @Nullable BlockState facade() { return facade; }
    public int typeMask() { return (has(ConduitType.ITEM) ? 1 : 0) | (has(ConduitType.FLUID) ? 2 : 0); }

    /** Adds or upgrades a conduit. Returns the tier that was there before (0 = none). */
    public int addConduit(ConduitType t, int tier) {
        int old = tiers[t.ordinal()];
        tiers[t.ordinal()] = Math.max(old, tier);
        changed();
        return old;
    }

    public void setFacade(@Nullable BlockState state) {
        facade = state;
        changed();
    }

    /** Call after any configuration change. */
    public void changed() {
        StorageEvents.conduitsChanged();
        if (level != null && !level.isClientSide()) {
            BlockState updated = ConduitBundleBlock.computeState(getBlockState(), level, worldPosition, this);
            if (updated != getBlockState()) level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        sync();
    }

    // ---- Connections ------------------------------------------------------------------------

    public boolean connectsTo(ConduitType t, Direction dir) {
        if (!has(t) || level == null) return false;
        if (config(t, dir).mode == SideConfig.Mode.DISABLED) return false;
        BlockPos np = worldPosition.relative(dir);
        BlockEntity nbe = level.getBlockEntity(np);
        if (nbe instanceof ConduitBundleBlockEntity other) {
            return other.has(t) && other.config(t, dir.getOpposite()).mode != SideConfig.Mode.DISABLED;
        }
        return canAttach(t, level, np, dir.getOpposite());
    }

    private static boolean canAttach(ConduitType t, Level level, BlockPos pos, Direction side) {
        return switch (t) {
            // Dimensional Outputs have no inventory, but push into adjacent conduits
            case ITEM -> ItemStorage.SIDED.find(level, pos, side) != null
                    || level.getBlockState(pos).getBlock() instanceof DimensionalOutputBlock;
            case FLUID -> FluidStorage.SIDED.find(level, pos, side) != null;
        };
    }

    /**
     * Client display: the colour channels of each connected side per conduit type
     * ([type][side], -1 when that type does not connect there or the neighbour is another conduit), packed by {@link #packMarker}.
     * Re-checked every half second.
     */
    public int[][] channelMarkers(long gameTime) {
        if (gameTime - markersAt >= 10 || markersAt < 0) {
            markersAt = gameTime;
            for (ConduitType t : ConduitType.VALUES) {
                for (Direction d : Direction.values()) {
                    markers[t.ordinal()][d.get3DDataValue()] = connectsTo(t, d) && !isBundle(worldPosition.relative(d))
                            ? packMarker(config(t, d)) : -1;
                }
            }
        }
        return markers;
    }

    /** Insert channel in bits 0-3, extract channel in bits 4-7, mode in bits 8-9. */
    public static int packMarker(SideConfig cfg) {
        return cfg.channel | cfg.extractChannel << 4 | cfg.mode.ordinal() << 8;
    }

    public int connectionMask() {
        int mask = 0;
        for (Direction d : Direction.values()) {
            for (ConduitType t : ConduitType.VALUES) {
                if (connectsTo(t, d)) {
                    mask |= 1 << d.get3DDataValue();
                    break;
                }
            }
        }
        return mask;
    }

    private boolean isBundle(BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity;
    }

    // ---- Ticking ----------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConduitBundleBlockEntity be) {
        long time = level.getGameTime();
        // Bundles saved before tiers were part of the block state show up untiered; fix once.
        if (time % 20 == 0 && (state.getValue(ConduitBundleBlock.ITEM_TIER) != be.tier(ConduitType.ITEM)
                || state.getValue(ConduitBundleBlock.FLUID_TIER) != be.tier(ConduitType.FLUID))) {
            be.changed();
        }
        int phase = (int) Math.floorMod(pos.asLong(), 20L);
        boolean powered = level.hasNeighborSignal(pos);

        if (be.has(ConduitType.ITEM)) {
            for (Direction d : Direction.values()) {
                SideConfig cfg = be.config(ConduitType.ITEM, d);
                if (!cfg.mode.extracts() || !cfg.redstone.allows(powered) || be.isBundle(pos.relative(d))) continue;
                int speed = Math.min(4, be.tier(ConduitType.ITEM) - 1 + cfg.speedUpgrades());
                int interval = new int[]{10, 10, 5, 2, 1}[speed];
                int batch = new int[]{4, 16, 32, 64, 64}[speed];
                if ((time + phase) % interval != 0) continue;
                be.transferItems(d, cfg, batch);
            }
        }
        if (be.has(ConduitType.FLUID) && (time + phase) % FLUID_INTERVAL == 0) {
            for (Direction d : Direction.values()) {
                SideConfig cfg = be.config(ConduitType.FLUID, d);
                if (!cfg.mode.extracts() || !cfg.redstone.allows(powered) || be.isBundle(pos.relative(d))) continue;
                int tier = be.tier(ConduitType.FLUID);
                long mbPerTick = (tier >= 2 ? 2000 : 100) * (1L << cfg.speedUpgrades());
                be.transferFluids(d, cfg, mbPerTick * FLUID_INTERVAL * DROPLETS_PER_MB);
            }
        }
    }

    private List<Target> targets(ConduitType type, int channel) {
        long v = StorageEvents.conduitVersion();
        if (v != cacheVersion) {
            targetCache.clear();
            cacheVersion = v;
        }
        int key = type.ordinal() * 16 + channel;
        List<Target> cached = targetCache.get(key);
        if (cached != null) return cached;

        List<Target> out = new ArrayList<>();
        Map<BlockPos, Integer> dist = new HashMap<>();
        Deque<ConduitBundleBlockEntity> queue = new ArrayDeque<>();
        dist.put(worldPosition, 0);
        queue.add(this);
        while (!queue.isEmpty() && dist.size() < MAX_NETWORK) {
            ConduitBundleBlockEntity b = queue.poll();
            int d0 = dist.get(b.worldPosition);
            for (Direction d : Direction.values()) {
                if (!b.connectsTo(type, d)) continue;
                BlockPos np = b.worldPosition.relative(d);
                if (level.getBlockEntity(np) instanceof ConduitBundleBlockEntity nb) {
                    if (!dist.containsKey(np)) {
                        dist.put(np, d0 + 1);
                        queue.add(nb);
                    }
                } else {
                    SideConfig cfg = b.config(type, d);
                    if (cfg.mode.inserts() && cfg.channel == channel) out.add(new Target(np, d.getOpposite(), cfg, d0));
                }
            }
        }
        out.sort(Comparator.comparingInt((Target t) -> -t.cfg.priority).thenComparingInt(Target::distance));
        targetCache.put(key, out);
        return out;
    }

    private static @Nullable FilterData filterOf(SideConfig cfg) {
        return cfg.filter.getItem() instanceof FilterItem fi ? fi.data(cfg.filter) : null;
    }

    private List<Target> ordered(List<Target> all, BlockPos sourcePos, SideConfig cfg) {
        List<Target> list = new ArrayList<>();
        for (Target t : all) if (!t.invPos.equals(sourcePos)) list.add(t);
        if (cfg.roundRobin && !list.isEmpty()) {
            int start = Math.floorMod(cfg.rrIndex, list.size());
            List<Target> rotated = new ArrayList<>(list.subList(start, list.size()));
            rotated.addAll(list.subList(0, start));
            return rotated;
        }
        return list;
    }

    private void transferItems(Direction dir, SideConfig cfg, long budget) {
        BlockPos sourcePos = worldPosition.relative(dir);
        Storage<ItemVariant> source = ItemStorage.SIDED.find(level, sourcePos, dir.getOpposite());
        if (source == null) return;
        FilterData extractFilter = filterOf(cfg);
        for (Target t : ordered(targets(ConduitType.ITEM, cfg.extractChannel), sourcePos, cfg)) {
            if (budget <= 0) break;
            Storage<ItemVariant> dest = ItemStorage.SIDED.find(level, t.invPos, t.side);
            if (dest == null) continue;
            FilterData insertFilter = filterOf(t.cfg);
            Predicate<ItemVariant> ok = v -> (extractFilter == null || extractFilter.matches(v))
                    && (insertFilter == null || insertFilter.matches(v));
            long moved = StorageUtil.move(source, dest, ok, budget, null);
            budget -= moved;
            if (moved > 0 && cfg.roundRobin) {
                cfg.rrIndex++;
                break;
            }
        }
    }

    /**
     * Items pushed in through {@code side} by a neighbour (a Dimensional Output) travel the
     * item conduit line like extracted items would: to Insert sides on the same colour channel,
     * through both sides' filters. Disabled sides refuse. Returns how many went in.
     */
    public long acceptPushed(Direction side, ItemVariant variant, long amount, TransactionContext tx) {
        if (!has(ConduitType.ITEM) || level == null) return 0;
        SideConfig cfg = config(ConduitType.ITEM, side);
        if (cfg.mode == SideConfig.Mode.DISABLED) return 0;
        FilterData sourceFilter = filterOf(cfg);
        if (sourceFilter != null && !sourceFilter.matches(variant)) return 0;
        BlockPos sourcePos = worldPosition.relative(side);
        long moved = 0;
        for (Target t : ordered(targets(ConduitType.ITEM, cfg.sourceChannel()), sourcePos, cfg)) {
            if (moved >= amount) break;
            FilterData targetFilter = filterOf(t.cfg);
            if (targetFilter != null && !targetFilter.matches(variant)) continue;
            Storage<ItemVariant> dest = ItemStorage.SIDED.find(level, t.invPos, t.side);
            if (dest == null) continue;
            long n = dest.insert(variant, amount - moved, tx);
            moved += n;
            if (n > 0 && cfg.roundRobin) {
                cfg.rrIndex++;
                break;
            }
        }
        return moved;
    }

    private void transferFluids(Direction dir, SideConfig cfg, long budget) {
        BlockPos sourcePos = worldPosition.relative(dir);
        Storage<FluidVariant> source = FluidStorage.SIDED.find(level, sourcePos, dir.getOpposite());
        if (source == null) return;
        FilterData extractFilter = filterOf(cfg);
        for (Target t : ordered(targets(ConduitType.FLUID, cfg.extractChannel), sourcePos, cfg)) {
            if (budget <= 0) break;
            Storage<FluidVariant> dest = FluidStorage.SIDED.find(level, t.invPos, t.side);
            if (dest == null) continue;
            FilterData insertFilter = filterOf(t.cfg);
            Predicate<FluidVariant> ok = v -> (extractFilter == null || extractFilter.matches(v))
                    && (insertFilter == null || insertFilter.matches(v));
            long moved = StorageUtil.move(source, dest, ok, budget, null);
            budget -= moved;
            if (moved > 0 && cfg.roundRobin) {
                cfg.rrIndex++;
                break;
            }
        }
    }

    // ---- Save / load / drops ------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (ConduitType t : ConduitType.VALUES) {
            output.putInt(t.key() + "_tier", tiers[t.ordinal()]);
            output.store(t.key() + "_sides", SideConfig.codec(t).listOf(), List.of(configs[t.ordinal()]));
        }
        if (facade != null) output.store("facade", BlockState.CODEC, facade);
        output.putInt("tier_format", 2);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (ConduitType t : ConduitType.VALUES) {
            tiers[t.ordinal()] = input.getIntOr(t.key() + "_tier", 0);
            input.read(t.key() + "_sides", SideConfig.codec(t).listOf()).ifPresent(list -> {
                for (int i = 0; i < 6 && i < list.size(); i++) configs[t.ordinal()][i] = list.get(i);
            });
        }
        facade = input.read("facade", BlockState.CODEC).orElse(null);
        // Before 1.0.14 fluid conduits had three tiers; the old Tier 2 and 3 are now Tier 2.
        if (input.getIntOr("tier_format", 1) < 2) {
            tiers[ConduitType.FLUID.ordinal()] = Math.min(tiers[ConduitType.FLUID.ordinal()], 2);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        List<ItemStack> drops = new ArrayList<>();
        for (ConduitType t : ConduitType.VALUES) {
            if (has(t)) drops.add(new ItemStack(ModItems.conduitFor(t, tier(t))));
            for (SideConfig c : configs[t.ordinal()]) {
                if (!c.filter.isEmpty()) drops.add(c.filter);
                if (!c.upgrades.isEmpty()) drops.add(c.upgrades);
            }
        }
        if (facade != null) drops.add(new ItemStack(facade.getBlock()));
        for (ItemStack s : drops) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
        StorageEvents.conduitsChanged();
    }
}
