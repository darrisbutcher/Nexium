package net.storagenexus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.placement.DiagonalPlacement;
import org.jspecify.annotations.Nullable;

/**
 * With the Diagonal Placement key held and a block in hand, an outline around the cell the block would go in, by the same rule
 * the server places by ({@link DiagonalPlacement#target}): a bright white outline where it would be placed, a red one where the cell
 * is already taken and nothing would be. {@code diagonalPreview} (client) switches it off.
 */
public final class DiagonalPreview {
    /** Where the block would go: a bright white line and a faint fill, drawn through blocks so it is seen even behind the wall. */
    private static final GizmoStyle PLACEABLE = GizmoStyle.strokeAndFill(0xD8FFFFFF, 1.6F, 0x28FFFFFF);
    /** The cell is taken: red. */
    private static final GizmoStyle TAKEN = GizmoStyle.strokeAndFill(0xD8FF5252, 1.6F, 0x28FF4040);

    private static @Nullable BlockPos target;
    private static boolean placeable;

    /** The cell outlined right now, or null (for the client test). */
    public static @Nullable BlockPos target() {
        return target;
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
            }
        });
    }

    private static void update(Minecraft client) {
        target = null;
        placeable = false;
        if (client.player == null || client.level == null || client.gui.screen() != null || client.player.isSpectator()) return;
        if (!NexusConfig.get().diagonalPreview || !NexusEssentialsClient.DIAGONAL_PLACE.isDown()) return;
        // The same hand the click would use: the main hand, or the offhand if the main hand holds no block
        if (!(client.player.getMainHandItem().getItem() instanceof BlockItem) && !(client.player.getOffhandItem().getItem() instanceof BlockItem)) return;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos spot = DiagonalPlacement.target(hit.getBlockPos(), hit.getDirection(), hit.getLocation());
        if (!client.level.isInWorldBounds(spot)) return;
        target = spot;
        placeable = client.level.getBlockState(spot).canBeReplaced();
    }

    private DiagonalPreview() {}
}
