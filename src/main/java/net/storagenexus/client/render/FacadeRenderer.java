package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.StorageNexus;
import net.storagenexus.client.screen.ConduitConfigScreen;
import net.storagenexus.conduit.SideConfig;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a conduit bundle's facade, or, without one, a small ring on each connected arm in
 * that side's colour channels: an inner ring in the insert colour and an outer ring, nearer the
 * block, in the extract colour (item rings before fluid rings). Conduit-to-conduit arms have none.
 */
public class FacadeRenderer implements BlockEntityRenderer<ConduitBundleBlockEntity, FacadeRenderer.FacadeState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final RenderType MARKER = RenderTypes.entitySolid(StorageNexus.id("textures/misc/white.png"));
    /** Ring cross-section, just wider than an arm (6.75-9.25 px). */
    private static final float RING_MIN = 6.6f / 16, RING_MAX = 9.4f / 16;
    /** Distance from the block centre along the arm, per conduit type (px). */
    /** How much further out the extract ring sits than the insert ring. */
    private static final float EXTRACT_OFFSET = 2.6f / 16;
    private static final float[][] RING_SPAN = {{2.0f / 16, 2.6f / 16}, {3.0f / 16, 3.6f / 16}};

    public static class FacadeState extends BlockEntityRenderState {
        public final BlockModelRenderState facade = new BlockModelRenderState();
        /** [type][side] colour channel, -1 for none. */
        public final int[][] channels = new int[ConduitType.VALUES.length][6];
    }

    private final BlockModelResolver blockModels;

    public FacadeRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModels = context.blockModelResolver();
    }

    @Override
    public FacadeState createRenderState() {
        return new FacadeState();
    }

    @Override
    public void extractRenderState(ConduitBundleBlockEntity be, FacadeState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        @Nullable BlockState facade = be.facade();
        if (facade != null) blockModels.update(state.facade, facade, DISPLAY_CONTEXT);
        else state.facade.clear();
        long now = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        int[][] markers = be.channelMarkers(now);
        for (int t = 0; t < markers.length; t++) System.arraycopy(markers[t], 0, state.channels[t], 0, 6);
    }

    @Override
    public void submit(FacadeState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.facade.isEmpty()) {
            state.facade.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            return; // the facade hides the conduits
        }
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, MARKER, (p, buffer) -> {
            for (int t = 0; t < state.channels.length; t++) {
                for (Direction d : Direction.values()) {
                    int marker = state.channels[t][d.get3DDataValue()];
                    if (marker < 0) continue;
                    SideConfig.Mode mode = SideConfig.Mode.values()[(marker >> 8) & 3];
                    // Inner ring: the insert colour. Outer ring, nearer the block: the extract colour.
                    if (mode.inserts()) {
                        ring(p, buffer, d, RING_SPAN[t][0], RING_SPAN[t][1], ConduitConfigScreen.CHANNEL_COLORS[marker & 15], light);
                    }
                    if (mode.extracts()) {
                        ring(p, buffer, d, RING_SPAN[t][0] + EXTRACT_OFFSET, RING_SPAN[t][1] + EXTRACT_OFFSET,
                                ConduitConfigScreen.CHANNEL_COLORS[(marker >> 4) & 15], light);
                    }
                }
            }
        });
    }

    /** A box around the arm on side {@code d}, from {@code near} to {@code far} out from the centre. */
    private static void ring(PoseStack.Pose p, VertexConsumer b, Direction d, float near, float far, int color, int light) {
        float[] min = {RING_MIN, RING_MIN, RING_MIN}, max = {RING_MAX, RING_MAX, RING_MAX};
        int axis = d.getAxis().ordinal(); // X=0, Y=1, Z=2
        int step = d.getAxisDirection().getStep();
        float a = 0.5f + step * near, c = 0.5f + step * far;
        min[axis] = Math.min(a, c);
        max[axis] = Math.max(a, c);
        box(p, b, min[0], min[1], min[2], max[0], max[1], max[2], color, light);
    }

    private static void box(PoseStack.Pose p, VertexConsumer b, float x0, float y0, float z0, float x1, float y1, float z1,
                            int color, int light) {
        quad(p, b, color, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // down
        quad(p, b, color, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); // up
        quad(p, b, color, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        quad(p, b, color, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // south
        quad(p, b, color, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // west
        quad(p, b, color, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); // east
    }

    private static void quad(PoseStack.Pose p, VertexConsumer b, int color, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(p, b, color, light, nx, ny, nz, ax, ay, az, 0, 0);
        vertex(p, b, color, light, nx, ny, nz, bx, by, bz, 0, 1);
        vertex(p, b, color, light, nx, ny, nz, cx, cy, cz, 1, 1);
        vertex(p, b, color, light, nx, ny, nz, dx, dy, dz, 1, 0);
        // and the other way round, so the face shows whichever way culling treats it
        vertex(p, b, color, light, nx, ny, nz, ax, ay, az, 0, 0);
        vertex(p, b, color, light, nx, ny, nz, dx, dy, dz, 1, 0);
        vertex(p, b, color, light, nx, ny, nz, cx, cy, cz, 1, 1);
        vertex(p, b, color, light, nx, ny, nz, bx, by, bz, 0, 1);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer b, int color, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        b.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
