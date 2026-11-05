package net.storagenexus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.aiot.module.AiotMining;
import net.storagenexus.essentials.config.NexusConfig;

import java.util.List;

/**
 * A faint glowing outline around the blocks an AIOT would mine along with the one you look at: the rest of a vein
 * (with the Vein Mine key held), a 3x3, or a tunnel. It uses the same rules the server mines by
 * ({@link AiotMining#extraBlocks}), so what is outlined is what goes, and it can be switched off with the client's
 * {@code aiotMiningPreview} setting. Sneaking mines one block, so nothing is outlined.
 */
public final class AiotPreview {
    /** Most blocks outlined at once; a long tunnel is 9 blocks a layer. */
    private static final int MAX_OUTLINED = 600;
    /** A soft pale blue line (about 55% opaque) and a barely-there fill, so a long tunnel does not turn into a wall of colour. */
    private static final GizmoStyle STYLE = GizmoStyle.strokeAndFill(0x8CBFEFFF, 1.2F, 0x087FD8FF);

    private static List<BlockPos> blocks = List.of();

    /** The blocks outlined right now (for the client test). */
    public static List<BlockPos> outlined() {
        return blocks;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(AiotPreview::update);
        // Gizmos are collected during the frame; this runs just before they are drawn
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            if (blocks.isEmpty()) return;
            // Add them to the level renderer's own collector, whichever collector is current on this thread
            try (var ignored = Minecraft.getInstance().levelRenderer.collectPerFrameRenderThreadGizmos()) {
                for (BlockPos pos : blocks) Gizmos.cuboid(pos, STYLE).setAlwaysOnTop();
            }
        });
    }

    private static void update(Minecraft client) {
        blocks = List.of();
        if (client.player == null || client.level == null || client.gui.screen() != null) return;
        if (!NexusConfig.get().aiotMiningPreview) return;
        ItemStack tool = client.player.getMainHandItem();
        if (!(tool.getItem() instanceof AiotItem)) return;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        List<BlockPos> extra = AiotMining.extraBlocks(client.level, client.player, tool, pos,
                client.level.getBlockState(pos), ToolMenuClient.VEIN_MINE.isDown());
        blocks = extra.size() > MAX_OUTLINED ? extra.subList(0, MAX_OUTLINED) : extra;
    }

    private AiotPreview() {}
}
