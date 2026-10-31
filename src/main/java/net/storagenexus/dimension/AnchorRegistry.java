package net.storagenexus.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers every Dimensional Anchor so that on-demand anchors can be woken up from another
 * dimension, without their chunks needing to be loaded first.
 */
public class AnchorRegistry extends SavedData {
    private record Entry(GlobalPos pos, boolean onDemand) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.BOOL.fieldOf("on_demand").forGetter(Entry::onDemand)
        ).apply(i, Entry::new));
    }

    private static final Codec<AnchorRegistry> CODEC = Entry.CODEC.listOf().xmap(AnchorRegistry::new, r -> List.copyOf(r.anchors.values()));

    public static final SavedDataType<AnchorRegistry> TYPE =
            new SavedDataType<>("storagenexus_anchors", AnchorRegistry::new, CODEC, null);

    private final Map<GlobalPos, Entry> anchors = new HashMap<>();
    /** Not saved: how many open tablets are holding each on-demand anchor awake. */
    private final Map<GlobalPos, Integer> demand = new HashMap<>();

    public AnchorRegistry() {}

    private AnchorRegistry(List<Entry> entries) {
        for (Entry e : entries) anchors.put(e.pos(), e);
    }

    public static AnchorRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(GlobalPos pos, boolean onDemand) {
        anchors.put(pos, new Entry(pos, onDemand));
        setDirty();
    }

    public void remove(GlobalPos pos) {
        if (anchors.remove(pos) != null) setDirty();
        demand.remove(pos);
    }

    /** Forces (or releases) the 3x3 chunks around an anchor. */
    public static void forceArea(ServerLevel level, BlockPos anchor, boolean force) {
        ChunkPos center = new ChunkPos(anchor);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced(center.x + dx, center.z + dz, force);
            }
        }
    }

    /**
     * If an on-demand anchor covers {@code target}, wakes it, loads the chunk right away and
     * returns a callback that puts it back to sleep. Otherwise returns null.
     */
    public @Nullable Runnable loadOnDemand(ServerLevel level, BlockPos target) {
        ChunkPos tc = new ChunkPos(target);
        for (Entry e : anchors.values()) {
            if (!e.onDemand() || !e.pos().dimension().equals(level.dimension())) continue;
            ChunkPos ac = new ChunkPos(e.pos().pos());
            if (Math.abs(ac.x - tc.x) > 1 || Math.abs(ac.z - tc.z) > 1) continue;
            GlobalPos key = e.pos();
            int count = demand.merge(key, 1, Integer::sum);
            if (count == 1) forceArea(level, key.pos(), true);
            level.getChunk(tc.x, tc.z); // load now, synchronously
            return () -> {
                int left = demand.merge(key, -1, Integer::sum);
                if (left <= 0) {
                    demand.remove(key);
                    Entry still = anchors.get(key);
                    if (still != null && still.onDemand()) forceArea(level, key.pos(), false);
                }
            };
        }
        return null;
    }

    public List<GlobalPos> all() {
        return new ArrayList<>(anchors.keySet());
    }
}
