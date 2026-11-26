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
 * Draws a conduit bundle's facade, or, without one, a plate on each arm that meets a machine. The plate shows, for each of the three
 * strands (energy, item, fluid, top lane first), what that side does: a direction mark (a triangle pointing in toward the strand for
 * Insert, out for Extract, one each way for Both), the insert colour as one pip, and the extract colours as up to four pips.
 * Conduit-to-conduit arms have no plate.
 */
public class FacadeRenderer implements BlockEntityRenderer<ConduitBundleBlockEntity, FacadeRenderer.FacadeState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final RenderType MARKER = RenderTypes.entitySolid(Nexium.id("textures/misc/white.png"));
    /** The plate sits on the machine's face: from {@code PLATE_NEAR} to the block edge, 14 px square. */
    private static final float PLATE_NEAR = 7.4f / 16, PLATE_FAR = 8.0f / 16, PLATE_HALF = 7.0f / 16;
    /** How far apart the strands are (their height), measured along the lane axis. */
    private static final float LANE_STEP = 2.5f / 16;
    private static final int PLATE_T1 = 0xFF3C3C46, PLATE_T2 = 0xFFB4BAC4, PLATE_EDGE_T1 = 0xFF6A6A78, PLATE_EDGE_T2 = 0xFFE6EBF2;
    /** Which conduit type each strand carries, top lane first. */
    private static final int[] LANE_TYPE = {ConduitType.ENERGY.ordinal(), ConduitType.ITEM.ordinal(), ConduitType.FLUID.ordinal()};
    /** TEMPORARY: preview of design 6 (light bar on the collar, plain arrows on the strands) instead of the plate matrix. */
    public static volatile boolean design6 = false;
    private static final int EMPTY_PIP = 0xFF2A2A32, WHITE = 0xFFE9EEF5;

    public static class FacadeState extends BlockEntityRenderState {
        /** Whether the conduit is Tier 2 (silver plates). */
        public boolean tier2;
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
        state.tier2 = false;
        for (ConduitType t : ConduitType.VALUES) state.tier2 |= be.tier(t) >= 2;
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
            for (Direction d : Direction.values()) {
                boolean connected = false;
                for (int t = 0; t < 3; t++) connected |= state.channels[t][d.get3DDataValue()] >= 0;
                if (connected && design6) design6(p, buffer, Arm.of(d), state, light);
                else if (connected) plate(p, buffer, Arm.of(d), state, light);
            }
        });
    }

    /** Axis bookkeeping for one arm: the arm axis, the axis the strands stack along, the other one, and which way energy sits. */
    private record Arm(Direction d, int axis, int step, int lane, int side, float laneSign) {
        static Arm of(Direction d) {
            int axis = d.getAxis().ordinal();
            return new Arm(d, axis, d.getAxisDirection().getStep(), axis == 1 ? 2 : 1, axis == 0 ? 2 : 0, d == Direction.DOWN ? -1 : 1);
        }

        /** Where strand {@code t} (0 energy, 1 item, 2 fluid) is along the lane axis, in blocks from the middle. */
        float laneOffset(int t) { return laneSign * (1 - t) * LANE_STEP; }
    }

    private static int channelColor(int c) {
        return 0xFF000000 | ConduitConfigScreen.CHANNEL_COLORS[c & 15];
    }

    /** The plate on one arm with, per strand, the direction mark and the insert and extract colours. */
    private static void plate(PoseStack.Pose p, VertexConsumer b, Arm a, FacadeState st, int light) {
        float na = 0.5f + a.step * PLATE_NEAR, fa = 0.5f + a.step * PLATE_FAR;
        float[] min = new float[3], max = new float[3];
        min[a.axis] = Math.min(na, fa);
        max[a.axis] = Math.max(na, fa);
        min[a.lane] = 0.5f - PLATE_HALF; max[a.lane] = 0.5f + PLATE_HALF;
        min[a.side] = 0.5f - PLATE_HALF; max[a.side] = 0.5f + PLATE_HALF;
        box(p, b, min[0], min[1], min[2], max[0], max[1], max[2], st.tier2 ? PLATE_T2 : PLATE_T1, light);
        // a lighter inner face, with an edge of plate round it
        float inner = 0.5f + a.step * (PLATE_NEAR - 0.15f / 16);
        float[] imin = min.clone(), imax = max.clone();
        imin[a.axis] = Math.min(inner, na); imax[a.axis] = Math.max(inner, na);
        for (int ax : new int[]{a.lane, a.side}) { imin[ax] += 0.5f / 16; imax[ax] -= 0.5f / 16; }
        box(p, b, imin[0], imin[1], imin[2], imax[0], imax[1], imax[2], st.tier2 ? PLATE_EDGE_T2 : PLATE_EDGE_T1, light);
        for (int t = 0; t < 3; t++) {
            int marker = st.channels[LANE_TYPE[t]][a.d.get3DDataValue()];
            boolean on = marker >= 0;
            SideConfig.Mode mode = on ? SideConfig.Mode.values()[(marker >> 4) & 3] : SideConfig.Mode.DISABLED;
            float row = 0.5f + a.laneOffset(t);
            // direction mark and insert pip (the band on one side of the strands)
            mark(p, b, a, inner, row, mode, light);
            pip(p, b, a, inner, row, 0.5f - 4.55f / 16, 1.7f / 16, mode.inserts() ? channelColor(marker & 15) : EMPTY_PIP, light);
            // extract pips (the band on the other side): up to four, two by two
            int extract = mode.extracts() ? marker >>> 6 & 0xFFFF : 0, shown = 0;
            for (int c = 0; c < 16 && shown < 4; c++) {
                if ((extract >> c & 1) == 0) continue;
                pip(p, b, a, inner, row + a.laneSign * (shown / 2 == 0 ? 0.63f : -0.63f) / 16, 0.5f + (4.45f + (shown % 2) * 1.45f) / 16, 1.15f / 16, channelColor(c), light);
                shown++;
            }
            for (; shown < 4; shown++) {
                pip(p, b, a, inner, row + a.laneSign * (shown / 2 == 0 ? 0.63f : -0.63f) / 16, 0.5f + (4.45f + (shown % 2) * 1.45f) / 16, 1.15f / 16, EMPTY_PIP, light);
            }
        }
    }

    // ---- design 6 preview: the settings shown on the collar's side faces, plain arrows on the strands ---------------

    private static void design6(PoseStack.Pose p, VertexConsumer b, Arm a, FacadeState st, int light) {
        // a plain plate on the machine
        float half = 5.0f / 16;
        float na = 0.5f + a.step * PLATE_NEAR, fa = 0.5f + a.step * PLATE_FAR;
        float[] min = new float[3], max = new float[3];
        min[a.axis] = Math.min(na, fa); max[a.axis] = Math.max(na, fa);
        min[a.lane] = 0.5f - half; max[a.lane] = 0.5f + half;
        min[a.side] = 0.5f - half; max[a.side] = 0.5f + half;
        box(p, b, min[0], min[1], min[2], max[0], max[1], max[2], st.tier2 ? PLATE_T2 : PLATE_T1, light);
        for (int t = 0; t < 3; t++) {
            int marker = st.channels[LANE_TYPE[t]][a.d.get3DDataValue()];
            boolean on = marker >= 0;
            SideConfig.Mode mode = on ? SideConfig.Mode.values()[(marker >> 4) & 3] : SideConfig.Mode.DISABLED;
            float row = 0.5f + a.laneOffset(t);
            for (int sign = -1; sign <= 1; sign += 2) {
                float face = 0.5f + sign * 4.25f / 16;
                // in pip then up to four extract pips, along the arm from the middle of the collar outward
                collarPip(p, b, a, face, sign, row, 0.9f, 1.2f, mode.inserts() ? channelColor(marker & 15) : EMPTY_PIP, light);
                int extract = mode.extracts() ? marker >>> 6 & 0xFFFF : 0, shown = 0;
                for (int c = 0; c < 16 && shown < 4; c++) {
                    if ((extract >> c & 1) == 0) continue;
                    collarPip(p, b, a, face, sign, row + a.laneSign * (shown / 2 == 0 ? 0.63f : -0.63f) / 16, 2.3f + (shown % 2) * 1.2f, 1.0f, channelColor(c), light);
                    shown++;
                }
                for (; shown < 4; shown++) {
                    collarPip(p, b, a, face, sign, row + a.laneSign * (shown / 2 == 0 ? 0.63f : -0.63f) / 16, 2.3f + (shown % 2) * 1.2f, 1.0f, EMPTY_PIP, light);
                }
            }
            // plain arrows on the strand's side faces
            if (on) {
                if (mode.inserts() && mode.extracts()) {
                    laneArrow(p, b, a, t, 6.0f, 5.0f, light);
                    laneArrow(p, b, a, t, 6.1f, 7.1f, light);
                } else if (mode.inserts()) laneArrow(p, b, a, t, 5.2f, 6.9f, light);
                else if (mode.extracts()) laneArrow(p, b, a, t, 6.9f, 5.2f, light);
            }
        }
    }

    /** A pip on the collar's side face: {@code along} px from the middle toward the arm, {@code size} px square. */
    private static void collarPip(PoseStack.Pose p, VertexConsumer b, Arm a, float face, int sign, float lane, float along, float size, int color, int light) {
        float[] min = new float[3], max = new float[3];
        float s0 = face, s1 = face + sign * 0.2f / 16;
        min[a.side] = Math.min(s0, s1); max[a.side] = Math.max(s0, s1);
        float c = 0.5f + a.step * (along + size / 2 - 0.0f) / 16;
        min[a.axis] = c - size / 32; max[a.axis] = c + size / 32;
        float c0 = 0.5f + a.step * (along) / 16, c1 = 0.5f + a.step * (along + size) / 16;
        min[a.axis] = Math.min(c0, c1); max[a.axis] = Math.max(c0, c1);
        min[a.lane] = lane - size / 32; max[a.lane] = lane + size / 32;
        box(p, b, min[0], min[1], min[2], max[0], max[1], max[2], color, light);
    }

    private static void laneArrow(PoseStack.Pose p, VertexConsumer b, Arm a, int t, float from, float to, int light) {
        float centre = 0.5f + a.laneOffset(t), half = 0.9f / 16;
        for (int sign = -1; sign <= 1; sign += 2) {
            float plane = 0.5f + sign * (3.75f / 16 + 0.03f / 16);
            float[][] pts = new float[4][3];
            float[] along = {0.5f + a.step * from / 16, 0.5f + a.step * from / 16, 0.5f + a.step * to / 16, 0.5f + a.step * to / 16};
            float[] lane = {centre - half, centre + half, centre, centre};
            for (int i = 0; i < 4; i++) { pts[i][a.axis] = along[i]; pts[i][a.lane] = lane[i]; pts[i][a.side] = plane; }
            float[] n = new float[3];
            n[a.side] = sign;
            quad(p, b, WHITE, light, n[0], n[1], n[2], pts[0][0], pts[0][1], pts[0][2], pts[1][0], pts[1][1], pts[1][2],
                    pts[2][0], pts[2][1], pts[2][2], pts[3][0], pts[3][1], pts[3][2]);
        }
    }

    private static void pip(PoseStack.Pose p, VertexConsumer b, Arm a, float plane, float lane, float side, float size, int color, int light) {
        float raise = 0.2f / 16;
        float[] min = new float[3], max = new float[3];
        float na = plane - a.step * raise;
        min[a.axis] = Math.min(na, plane); max[a.axis] = Math.max(na, plane);
        min[a.lane] = lane - size / 2; max[a.lane] = lane + size / 2;
        min[a.side] = side - size / 2; max[a.side] = side + size / 2;
        box(p, b, min[0], min[1], min[2], max[0], max[1], max[2], color, light);
    }

    /**
     * The direction mark of a strand, flat on the plate beside its insert pip: a triangle pointing in toward the strand when the side
     * inserts (the energy goes in to the machine), out and away from it when it extracts, and one of each for Both. Nothing when off.
     */
    private static void mark(PoseStack.Pose p, VertexConsumer b, Arm a, float plane, float lane, SideConfig.Mode mode, int light) {
        float centre = 0.5f - 6.2f / 16;
        float half = 0.75f / 16;
        float base = 0.55f / 16;
        if (mode.inserts() && mode.extracts()) {
            triangle(p, b, a, plane, lane, centre - base, centre - base - 1.0f / 16, half, light);
            triangle(p, b, a, plane, lane, centre + base, centre + base + 1.0f / 16, half, light);
        } else if (mode.inserts()) {
            triangle(p, b, a, plane, lane, centre - 0.6f / 16, centre + 0.6f / 16, half, light);
        } else if (mode.extracts()) {
            triangle(p, b, a, plane, lane, centre + 0.6f / 16, centre - 0.6f / 16, half, light);
        }
    }

    /** A flat triangle on the plate: base at {@code fromSide}, point at {@code toSide}, {@code half} either way along the lane. */
    private static void triangle(PoseStack.Pose p, VertexConsumer b, Arm a, float plane, float lane, float fromSide, float toSide, float half, int light) {
        float z = plane - a.step * 0.05f / 16;
        float[][] pts = new float[4][3];
        float[] ln = {lane - half, lane + half, lane, lane};
        float[] sd = {fromSide, fromSide, toSide, toSide};
        for (int i = 0; i < 4; i++) { pts[i][a.axis] = z; pts[i][a.lane] = ln[i]; pts[i][a.side] = sd[i]; }
        float[] n = new float[3];
        n[a.axis] = -a.step;
        quad(p, b, WHITE, light, n[0], n[1], n[2], pts[0][0], pts[0][1], pts[0][2], pts[1][0], pts[1][1], pts[1][2],
                pts[2][0], pts[2][1], pts[2][2], pts[3][0], pts[3][1], pts[3][2]);
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
