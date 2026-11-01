package net.storagenexus.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
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
 * <p>
 * An anchor keeps its 3x3 chunks loaded with a chunk ticket of its own ({@link #TICKET}), never with vanilla's
 * forced-chunk flag: that flag is one switch per chunk shared with {@code /forceload} and other mods, so releasing
 * it would also release chunks someone else forced.
 */
public class AnchorRegistry extends SavedData {
    /**
     * Keeps an anchor's chunks loaded and ticking, and its dimension running, like a forced chunk. Not saved with the
     * world: {@link #reconcile} puts the tickets back when the server starts, so nothing stays loaded if the mod is removed.
     */
    public static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, StorageNexus.id("dimensional_anchor"),
            new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    /**
     * One ticket on the chunk that holds the anchor: radius 3 puts that chunk at level 30 and the 8 around it at 31
     * (entity ticking), the same 3x3 area, ticking the same way, as forcing each of the 9 chunks.
     */
    public static final int TICKET_RADIUS = 3;

    private record Entry(GlobalPos pos, boolean onDemand) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.BOOL.fieldOf("on_demand").forGetter(Entry::onDemand)
        ).apply(i, Entry::new));
    }

    private static final Codec<AnchorRegistry> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(i -> i.group(
                    Entry.CODEC.listOf().fieldOf("anchors").forGetter(r -> List.copyOf(r.anchors.values())),
                    Codec.BOOL.optionalFieldOf("uses_tickets", false).forGetter(r -> r.usesTickets)
            ).apply(i, AnchorRegistry::new)),
            // Before 1.1.4 the save was just the list of anchors, and anchors used vanilla's forced flag
            Entry.CODEC.listOf().xmap(list -> new AnchorRegistry(list, false), r -> List.copyOf(r.anchors.values())));

    public static final SavedDataType<AnchorRegistry> TYPE =
            new SavedDataType<>(StorageNexus.id("anchors"), AnchorRegistry::new, CODEC, null);

    private final Map<GlobalPos, Entry> anchors = new HashMap<>();
    /** Not saved: how many open tablets are holding each on-demand anchor awake. */
    private final Map<GlobalPos, Integer> demand = new HashMap<>();
    /** False for a world whose anchors still hold vanilla forced chunks from before 1.1.4; {@link #reconcile} releases them once. */
    private boolean usesTickets;

    public AnchorRegistry() {
        usesTickets = true;
    }

    private AnchorRegistry(List<Entry> entries, boolean usesTickets) {
        for (Entry e : entries) anchors.put(e.pos(), e);
        this.usesTickets = usesTickets;
    }

    /** Registers {@link #TICKET}; called from the mod initializer, while registries are still open. */
    public static void init() {}

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
     * Re-decides the ticket for the chunk holding an anchor. The ticket stays while any always-on anchor, or any
     * on-demand anchor a tablet is using, is in that chunk: tickets of the same kind on the same chunk are one ticket,
     * so anchors sharing a chunk never release each other's. Call after any anchor change.
     */
    public void refreshArea(ServerLevel level, BlockPos anchor) {
        ChunkPos center = ChunkPos.containing(anchor);
        if (wanted(level, center)) level.getChunkSource().addTicketWithRadius(TICKET, center, TICKET_RADIUS);
        else level.getChunkSource().removeTicketWithRadius(TICKET, center, TICKET_RADIUS);
    }

    private boolean wanted(ServerLevel level, ChunkPos center) {
        for (Entry e : anchors.values()) {
            if (!e.pos().dimension().equals(level.dimension()) || !ChunkPos.containing(e.pos().pos()).equals(center)) continue;
            if (!e.onDemand() || demand.getOrDefault(e.pos(), 0) > 0) return true;
        }
        return false;
    }

    private static boolean covers(BlockPos anchor, ChunkPos chunk) {
        ChunkPos ac = ChunkPos.containing(anchor);
        return Math.abs(ac.x() - chunk.x()) <= 1 && Math.abs(ac.z() - chunk.z()) <= 1;
    }

    /**
     * On server start: no tablet is open, so on-demand anchors sleep, and always-on anchors get their tickets back
     * (tickets aren't saved). The first time a world from before 1.1.4 starts, this also releases the vanilla forced
     * chunks its anchors used to hold, which are saved with the world and would otherwise stay loaded forever.
     */
    public void reconcile(MinecraftServer server) {
        demand.clear();
        if (!usesTickets) {
            for (Entry e : anchors.values()) {
                ServerLevel level = server.getLevel(e.pos().dimension());
                if (level == null) continue;
                ChunkPos center = ChunkPos.containing(e.pos().pos());
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) level.setChunkForced(center.x() + dx, center.z() + dz, false);
                }
            }
            usesTickets = true;
            setDirty();
        }
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
