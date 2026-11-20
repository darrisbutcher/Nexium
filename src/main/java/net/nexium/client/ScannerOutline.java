package net.nexium.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.nexium.conduit.ConduitScan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The glowing outlines the Conduit Scanner window puts in the world when you click a connection: the conduit it belongs to (blue) and
 * the block it ends at (orange where it takes things out, green where it puts them in, gold for both). They stay for ten seconds, so
 * you can close the window and walk there; click the row again to take them away.
 */
public final class ScannerOutline {
    private static final int LIFETIME_TICKS = 200;
    private static final GizmoStyle CONDUIT = GizmoStyle.strokeAndFill(0xC860A0FF, 2.0F, 0x2060A0FF);
    private static final GizmoStyle OUT = GizmoStyle.strokeAndFill(0xC8FFA23C, 2.0F, 0x20FFA23C);
    private static final GizmoStyle IN = GizmoStyle.strokeAndFill(0xC855D86B, 2.0F, 0x2055D86B);
    private static final GizmoStyle BOTH = GizmoStyle.strokeAndFill(0xC8FFD23C, 2.0F, 0x20FFD23C);

    private record Mark(GizmoStyle style, long until) {}

    private static final Map<BlockPos, Mark> MARKS = new HashMap<>();
    /** Which conduit each outlined block was marked from, so the conduit's outline goes when its last block's does. */
    private static final Map<BlockPos, BlockPos> OWNERS = new HashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                clear();
                return;
            }
            long now = client.level.getGameTime();
            MARKS.values().removeIf(m -> m.until() < now);
            OWNERS.keySet().removeIf(block -> !MARKS.containsKey(block));
        });
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            if (MARKS.isEmpty()) return;
            try (var ignored = Minecraft.getInstance().levelRenderer.collectPerFrameRenderThreadGizmos()) {
                for (Map.Entry<BlockPos, Mark> e : MARKS.entrySet()) Gizmos.cuboid(e.getKey(), e.getValue().style()).setAlwaysOnTop();
            }
        });
    }

    /**
     * Outlines this connection's block, and the conduit it is on, or takes the block's outline away if it is already showing (the
     * conduit's goes when no other outlined block belongs to it). Returns whether it is showing now.
     */
    public static boolean toggle(ConduitScan.Link link) {
        if (MARKS.containsKey(link.target())) {
            MARKS.remove(link.target());
            BlockPos conduit = OWNERS.remove(link.target());
            if (conduit != null && !OWNERS.containsValue(conduit)) MARKS.remove(conduit);
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        long until = (client.level == null ? 0 : client.level.getGameTime()) + LIFETIME_TICKS;
        MARKS.put(link.conduit(), new Mark(CONDUIT, until));
        GizmoStyle style = link.mode().inserts() && link.mode().extracts() ? BOTH : link.mode().extracts() ? OUT : IN;
        MARKS.put(link.target(), new Mark(style, until));
        OWNERS.put(link.target(), link.conduit());
        return true;
    }

    public static void clear() {
        MARKS.clear();
        OWNERS.clear();
    }

    /** What is outlined now (for tests). */
    public static List<BlockPos> outlined() {
        return new ArrayList<>(MARKS.keySet());
    }

    private ScannerOutline() {}
}
