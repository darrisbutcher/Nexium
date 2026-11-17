package net.storagenexus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.placement.DiagonalPlacement;
import org.jspecify.annotations.Nullable;

/**
 * With the Diagonal Placement key held and a block in hand, an outline around the cell the block would go in, by the same rule
 * the server places by ({@link DiagonalPlacement#plan}): a bright white outline where it would be placed, a red one where the cell
 * is already taken and nothing would be. On the block you aim at, a thick gold line shows the edge of the face the placement is
 * worked out from (diagonal), or the whole face is outlined when you aim at its middle (straight across with a gap).
 * {@code diagonalPreview} (client) switches it off.
 */
public final class DiagonalPreview {
    /** Where the block would go: a bright white line and a faint fill, drawn through blocks so it is seen even behind the wall. */
    private static final GizmoStyle PLACEABLE = GizmoStyle.strokeAndFill(0xA0FFFFFF, 1.4F, 0x10FFFFFF);
    /** The cell is taken: red. */
    private static final GizmoStyle TAKEN = GizmoStyle.strokeAndFill(0xA0FF5252, 1.4F, 0x10FF4040);

    /**
     * The aimed face drawn in zones, as Tweakeroo does: the middle square (straight across with a gap) and a trapezoid toward each
     * edge (diagonal). The zone that would be used is filled and drawn thick; the others are only faint lines.
     */
    private static final GizmoStyle ZONE = GizmoStyle.stroke(0x70FFC83A, 1.5F);
    private static final GizmoStyle ZONE_USED = GizmoStyle.strokeAndFill(0xFFFFC83A, 3.0F, 0x58FFC83A);

    private static @Nullable BlockPos target;
    private static boolean placeable;
    /** The aimed block and face, and the edge in use (null: the middle was aimed at, so the whole face is marked). */
    private static @Nullable BlockPos clicked;
    private static @Nullable Direction face;
    private static @Nullable Direction edge;

    /** The cell outlined right now, or null (for the client test). */
    public static @Nullable BlockPos target() {
        return target;
    }

    /** The edge of the aimed face in use, or null when the straight-across placement is (the face is marked instead) or there is no preview. */
    public static @Nullable Direction edge() {
        return edge;
    }

    /** The face being marked on the aimed block, or null. */
    public static @Nullable Direction face() {
        return face;
    }

    /** Whether the outlined cell is free to place in. */
    public static boolean placeable() {
        return placeable;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(DiagonalPreview::update);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            if (target == null) return;
            try (var ignored = Minecraft.getInstance().levelRenderer.collectPerFrameRenderThreadGizmos()) {
                Gizmos.cuboid(target, placeable ? PLACEABLE : TAKEN).setAlwaysOnTop();
                if (clicked != null && face != null) mark(clicked, face, edge);
            }
        });
    }

    /**
     * Draws the zones of the aimed face a hair off the block (so they are not lost in its surface): the middle square and, toward
     * each edge, the trapezoid between that edge and the square. The zone in use is the edge's trapezoid, or the square with no edge.
     */
    private static void mark(BlockPos block, Direction face, @Nullable Direction used) {
        Vec3 centre = Vec3.atCenterOf(block).add(face.getStepX() * 0.503, face.getStepY() * 0.503, face.getStepZ() * 0.503);
        Direction.Axis[] across = java.util.Arrays.stream(Direction.Axis.values()).filter(a -> a != face.getAxis()).toArray(Direction.Axis[]::new);
        double inner = DiagonalPlacement.CENTER;
        // The middle square
        Vec3 a = unit(across[0]), b = unit(across[1]);
        zone(used == null, centre.add(a.scale(inner)).add(b.scale(inner)), centre.add(a.scale(inner)).subtract(b.scale(inner)),
                centre.subtract(a.scale(inner)).subtract(b.scale(inner)), centre.subtract(a.scale(inner)).add(b.scale(inner)));
        // A trapezoid toward each of the four edges: its outer side is the whole edge, its inner side is the square's
        for (Direction.Axis axis : across) {
            Direction.Axis other = axis == across[0] ? across[1] : across[0];
            for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
                Direction edge = Direction.fromAxisAndDirection(axis, sign);
                Vec3 n = new Vec3(edge.getStepX(), edge.getStepY(), edge.getStepZ());
                Vec3 t = unit(other);
                zone(edge == used, centre.add(n.scale(0.5)).add(t.scale(0.5)), centre.add(n.scale(0.5)).subtract(t.scale(0.5)),
                        centre.add(n.scale(inner)).subtract(t.scale(inner)), centre.add(n.scale(inner)).add(t.scale(inner)));
            }
        }
    }

    private static void zone(boolean inUse, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4) {
        Gizmos.rect(p1, p2, p3, p4, inUse ? ZONE_USED : ZONE).setAlwaysOnTop();
    }

    private static Vec3 unit(Direction.Axis axis) {
        return new Vec3(axis == Direction.Axis.X ? 1 : 0, axis == Direction.Axis.Y ? 1 : 0, axis == Direction.Axis.Z ? 1 : 0);
    }

    private static void update(Minecraft client) {
        target = null;
        placeable = false;
        clicked = null;
        face = null;
        edge = null;
        if (client.player == null || client.level == null || client.gui.screen() != null || client.player.isSpectator()) return;
        if (!NexusConfig.get().diagonalPreview || !NexusEssentialsClient.DIAGONAL_PLACE.isDown()) return;
        // The same hand the click would use: the main hand, or the offhand if the main hand holds no block
        if (!(client.player.getMainHandItem().getItem() instanceof BlockItem) && !(client.player.getOffhandItem().getItem() instanceof BlockItem)) return;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        DiagonalPlacement.Plan plan = DiagonalPlacement.plan(hit.getBlockPos(), hit.getDirection(), hit.getLocation());
        if (!client.level.isInWorldBounds(plan.spot())) return;
        target = plan.spot();
        placeable = client.level.getBlockState(plan.spot()).canBeReplaced();
        clicked = hit.getBlockPos();
        face = hit.getDirection();
        edge = plan.edge();
    }

    private DiagonalPreview() {}
}
