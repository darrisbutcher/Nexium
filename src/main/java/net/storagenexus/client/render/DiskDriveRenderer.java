package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.StorageNexus;
import net.storagenexus.disk.DiskDriveBlock;
import net.storagenexus.disk.DiskDriveBlockEntity;
import net.storagenexus.disk.DiskTier;
import net.storagenexus.disk.StorageDiskItem;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the disks in a Disk Drive's 8 bays (2 across, 4 down, matching the front texture): each disk's edge with its
 * size's colour, and a light beside it, green with room, yellow over 75% full and red when full.
 */
public class DiskDriveRenderer implements BlockEntityRenderer<DiskDriveBlockEntity, DiskDriveRenderer.State> {
    private static final RenderType FLAT = RenderTypes.entitySolid(StorageNexus.id("textures/misc/white.png"));
    /** Label colours by size, as on the disk items. */
    private static final int[] TIER_COLORS = {0xFFD0773A, 0xFFF2C230, 0xFF4FD8E0, 0xFF3FD06A, 0xFF2CE59F};
    private static final int BODY = 0xFF8A8D95, BODY_EDGE = 0xFF5A5C63, GREEN = 0xFF5DFF6A, YELLOW = 0xFFFFD23A, RED = 0xFFFF4A3A;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int frontLight;
        final int[] label = new int[DiskDriveBlockEntity.SLOTS];
        final int[] led = new int[DiskDriveBlockEntity.SLOTS];
        final boolean[] fluid = new boolean[DiskDriveBlockEntity.SLOTS];
    }

    public DiskDriveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DiskDriveBlockEntity be, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(DiskDriveBlock.FACING);
        state.frontLight = be.getLevel() != null
                ? LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos().relative(state.facing)) : state.lightCoords;
        for (int i = 0; i < DiskDriveBlockEntity.SLOTS; i++) {
            ItemStack disk = be.disks().getItem(i);
            if (disk.getItem() instanceof StorageDiskItem d) {
                DiskTier tier = d.tier();
                state.label[i] = TIER_COLORS[tier.ordinal()];
                float f = StorageDiskItem.fill(disk);
                state.led[i] = f >= 1 ? RED : f >= 0.75f ? YELLOW : GREEN;
                state.fluid[i] = d.isFluid();
            } else {
                state.label[i] = 0;
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
        pose.translate(0, 0, 0.5 + 0.002);
        collector.submitCustomGeometry(pose, FLAT, (p, buffer) -> {
            for (int i = 0; i < DiskDriveBlockEntity.SLOTS; i++) {
                if (state.label[i] == 0) continue;
                // The bay's top-left corner, in the 64-pixel front texture
                int bx = 8 + (i % 2) * 25, by = 9 + (i / 2) * 12;
                px(p, buffer, bx + 1, by + 1, bx + 17, by + 9, 0.001f, BODY_EDGE, state.frontLight);
                px(p, buffer, bx + 1, by + 1, bx + 16, by + 8, 0.002f, BODY, state.frontLight);
                px(p, buffer, bx + 3, by + 3, bx + 13, by + 7, 0.003f, state.label[i], state.frontLight);
                if (state.fluid[i]) px(p, buffer, bx + 5, by + 4, bx + 11, by + 6, 0.004f, 0xFF2F6FE0, state.frontLight);
                // The light glows whatever the light level
                px(p, buffer, bx + 18, by + 3, bx + 21, by + 6, 0.003f, state.led[i], LightCoordsUtil.FULL_BRIGHT);
            }
        });
        pose.popPose();
    }

    /** A rectangle given in texture pixels (0-64, top-left origin) on the front face. */
    private static void px(PoseStack.Pose p, VertexConsumer b, int x0, int y0, int x1, int y1, float z, int color, int light) {
        // Seen from the front, +x is to the viewer's right; texture v runs downwards
        float l = x0 / 64f - 0.5f, r = x1 / 64f - 0.5f, t = 0.5f - y0 / 64f, bt = 0.5f - y1 / 64f;
        vertex(p, b, l, t, z, color, light);
        vertex(p, b, l, bt, z, color, light);
        vertex(p, b, r, bt, z, color, light);
        vertex(p, b, r, t, z, color, light);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer b, float x, float y, float z, int color, int light) {
        b.addVertex(p, x, y, z).setColor(color).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
    }
}
