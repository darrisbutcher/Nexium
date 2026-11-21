package net.nexium.client.render;

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
import net.nexium.Nexium;
import net.nexium.client.screen.ConduitConfigScreen;
import net.nexium.conduit.SideConfig;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a conduit bundle's facade, or, without one, a small ring on each connected arm in
 * that side's colour channels: an inner ring in the insert colour and an outer ring, nearer the
 * block, in the extract colour (item rings before fluid rings). Conduit-to-conduit arms have none.
 */
public class FacadeRenderer implements BlockEntityRenderer<ConduitBundleBlockEntity, FacadeRenderer.FacadeState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final RenderType MARKER = RenderTypes.entitySolid(Nexium.id("textures/misc/white.png"));
    /** Ring cross-section, just wider than an arm (6.75-9.25 px). */
    private static final float RING_MIN = 6.6f / 16, RING_MAX = 9.4f / 16;
    /** Distance from the block centre along the arm, per conduit type (px). */
    /** How much further out the extract ring sits than the insert ring. */
    private static final float EXTRACT_OFFSET = 2.6f / 16;
    /** Each further extract colour is another ring this much farther out, as long as it still fits on the arm. */
    private static final float EXTRACT_STEP = 0.9f / 16, ARM_END = 7.9f / 16;
    private static final float[][] RING_SPAN = {{2.0f / 16, 2.6f / 16}, {3.0f / 16, 3.6f / 16}, {4.0f / 16, 4.6f / 16}};
    /**
     * The direction arrow, one for each arm: a flat arrowhead on each of the arm's four faces, light so it stands out from the dark arm,
     * that points the way the items go: out to the block when the arm inserts, in to the conduit when it extracts, and a double arrow
     * when it does both (whichever conduit types it carries). It goes where the rings leave room ({@link ArrowPlacement}).
     */
    private static final int ARROW_COLOR = 0xFFE9EEF5;
    /** A double arrow's heads and shaft as fractions of its length, half the width of a head's base and of the shaft, in blocks. */
    private static final float HEAD_PART = 0.353f, SHAFT_PART = 0.294f, HEAD_HALF = 0.85f / 16, SHAFT_HALF = 0.25f / 16;
    /** Half the width of a single arrow's base, the arm's half-width, and how far the arrow floats off the arm's face (blocks). */
    private static final float ARROW_HALF = 1.0f / 16, ARM_HALF = 1.25f / 16, ARROW_LIFT = 0.02f / 16;

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
            // The stretches of one arm that the rings cover, in pixels from the centre: at most an insert ring and 16 extract rings per type
            float[] rings = new float[2 * state.channels.length * 17];
            for (Direction d : Direction.values()) {
                int ringCount = 0;
                boolean inserts = false, extracts = false;
                for (int t = 0; t < state.channels.length; t++) {
                    int marker = state.channels[t][d.get3DDataValue()];
                    if (marker < 0) continue;
                    SideConfig.Mode mode = SideConfig.Mode.values()[(marker >> 4) & 3];
                    // Inner ring: the insert colour. Outer ring, nearer the block: the extract colour, and one more ring
                    // farther out for each further extract colour that is on (as many as fit on the arm).
                    if (mode.inserts()) {
                        ring(p, buffer, d, RING_SPAN[t][0], RING_SPAN[t][1], ConduitConfigScreen.CHANNEL_COLORS[marker & 15], light);
                        rings[2 * ringCount] = RING_SPAN[t][0] * 16;
                        rings[2 * ringCount + 1] = RING_SPAN[t][1] * 16;
                        ringCount++;
                        inserts = true;
                    }
                    if (mode.extracts()) {
                        int extract = marker >>> 6 & 0xFFFF, shown = 0;
                        for (int c = 0; c < 16; c++) {
                            if ((extract >> c & 1) == 0) continue;
                            float shift = EXTRACT_OFFSET + shown * EXTRACT_STEP;
                            if (shown > 0 && RING_SPAN[t][1] + shift > ARM_END) break;
                            ring(p, buffer, d, RING_SPAN[t][0] + shift, RING_SPAN[t][1] + shift, ConduitConfigScreen.CHANNEL_COLORS[c], light);
                            rings[2 * ringCount] = (RING_SPAN[t][0] + shift) * 16;
                            rings[2 * ringCount + 1] = (RING_SPAN[t][1] + shift) * 16;
                            ringCount++;
                            shown++;
                        }
                        extracts = true;
                    }
                }
                if (inserts || extracts) arrow(p, buffer, d, ArrowPlacement.zone(rings, ringCount, inserts && extracts), inserts, extracts, light);
            }
        });
    }

    /**
     * The direction arrow in {@code zone} ({@code {from, to}} in pixels out from the centre, null for none): pointing out to the block
     * when the arm only inserts, in to the conduit when it only extracts, and a double arrow (a head each way, joined by a short shaft)
     * when it does both.
     */
    private static void arrow(PoseStack.Pose p, VertexConsumer b, Direction d, float @Nullable [] zone, boolean inserts, boolean extracts, int light) {
        if (zone == null) return;
        float from = zone[0] / 16, to = zone[1] / 16;
        if (inserts && extracts) {
            float length = to - from, head = length * HEAD_PART, shaft = length * SHAFT_PART;
            decal(p, b, d, from + head, from, HEAD_HALF, 0, light); // the head toward the conduit
            decal(p, b, d, from + head, from + head + shaft, SHAFT_HALF, SHAFT_HALF, light);
            decal(p, b, d, from + head + shaft, to, HEAD_HALF, 0, light); // the head toward the block
        } else if (inserts) {
            decal(p, b, d, from, to, ARROW_HALF, 0, light);
        } else {
            decal(p, b, d, to, from, ARROW_HALF, 0, light);
        }
    }

    /**
     * A flat shape on each of the four faces of the arm on side {@code d}, from {@code from} to {@code to} out from the centre: an edge
     * of half-width {@code halfFrom} at the first and one of {@code halfTo} at the second (an arrowhead has a point at one end, so a
     * half-width of 0 there; a shaft has the same at both).
     */
    private static void decal(PoseStack.Pose p, VertexConsumer b, Direction d, float from, float to, float halfFrom, float halfTo, int light) {
        int axis = d.getAxis().ordinal();
        int step = d.getAxisDirection().getStep();
        int u = (axis + 1) % 3, v = (axis + 2) % 3;
        float[] along = {0.5f + step * from, 0.5f + step * from, 0.5f + step * to, 0.5f + step * to};
        float[] wide = {-halfFrom, halfFrom, halfTo, -halfTo};
        // Each face has its outward normal along u or v; the shape lies in the plane of the arm face, its width along the other one.
        for (int face = 0; face < 4; face++) {
            int normalAxis = face < 2 ? u : v, widthAxis = face < 2 ? v : u;
            int sign = face % 2 == 0 ? 1 : -1;
            float plane = 0.5f + sign * (ARM_HALF + ARROW_LIFT);
            float[][] pts = new float[4][3];
            for (int i = 0; i < 4; i++) {
                pts[i][axis] = along[i];
                pts[i][widthAxis] = 0.5f + wide[i];
                pts[i][normalAxis] = plane;
            }
            float[] n = new float[3];
            n[normalAxis] = sign;
            quad(p, b, ARROW_COLOR, light, n[0], n[1], n[2], pts[0][0], pts[0][1], pts[0][2], pts[1][0], pts[1][1], pts[1][2],
                    pts[2][0], pts[2][1], pts[2][2], pts[3][0], pts[3][1], pts[3][2]);
        }
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
