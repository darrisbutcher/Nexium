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
import net.storagenexus.StorageNexus;
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
            new SavedDataType<>(StorageNexus.id("anchors"), AnchorRegistry::new, CODEC, null);

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

    /**
     * Re-decides forcing for the 3x3 chunks around an anchor. A chunk stays forced while any
     * always-on anchor, or any on-demand anchor a tablet is using, covers it, so overlapping
     * anchors never release each other's chunks. Call after any anchor change.
     */
    public void refreshArea(ServerLevel level, BlockPos anchor) {
        ChunkPos center = ChunkPos.containing(anchor);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos chunk = new ChunkPos(center.x() + dx, center.z() + dz);
                level.setChunkForced(chunk.x(), chunk.z(), wanted(level, chunk));
            }
        }
    }

    private boolean wanted(ServerLevel level, ChunkPos chunk) {
        for (Entry e : anchors.values()) {
            if (!e.pos().dimension().equals(level.dimension()) || !covers(e.pos().pos(), chunk)) continue;
            if (!e.onDemand() || demand.getOrDefault(e.pos(), 0) > 0) return true;
        }
        return false;
    }

    private static boolean covers(BlockPos anchor, ChunkPos chunk) {
        ChunkPos ac = ChunkPos.containing(anchor);
        return Math.abs(ac.x() - chunk.x()) <= 1 && Math.abs(ac.z() - chunk.z()) <= 1;
    }

    /**
     * On server start no tablet is open, so on-demand areas are released. Vanilla saves forced
     * chunks, so this repairs any left forced by a crash while a tablet was open.
     */
    public void reconcile(MinecraftServer server) {
        demand.clear();
        for (Entry e : List.copyOf(anchors.values())) {
            ServerLevel level = server.getLevel(e.pos().dimension());
            if (level != null) refreshArea(level, e.pos().pos());
        }
    }

    /**
     * If an on-demand anchor covers {@code target}, wakes it, loads the chunk right away and
     * returns a callback that puts it back to sleep. Otherwise returns null.
     */
    public @Nullable Runnable loadOnDemand(ServerLevel level, BlockPos target) {
        ChunkPos tc = ChunkPos.containing(target);
        for (Entry e : anchors.values()) {
            if (!e.onDemand() || !e.pos().dimension().equals(level.dimension()) || !covers(e.pos().pos(), tc)) continue;
            GlobalPos key = e.pos();
            int count = demand.merge(key, 1, Integer::sum);
            if (count == 1) refreshArea(level, key.pos());
            level.getChunk(tc.x(), tc.z()); // load now, synchronously
            return () -> {
                int left = demand.merge(key, -1, Integer::sum);
                if (left <= 0) {
                    demand.remove(key);
                    refreshArea(level, key.pos());
                }
            };
        }
        return null;
    }

    public List<GlobalPos> all() {
        return new ArrayList<>(anchors.keySet());
    }
}
