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
import net.nexium.conduit.SideConfig;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a conduit bundle's facade, or, without one, a plate on each arm that meets a machine. It spells what each strand's side does (INSERT, EXTRACT, BOTH or OFF) in that strand's own colour (red,
 * green, blue): energy across the top, item up the left edge, fluid up the right. The channel colours are not drawn here; they show in the conduit's menu. Conduit-to-conduit arms have no plate.
 */
public class FacadeRenderer implements BlockEntityRenderer<ConduitBundleBlockEntity, FacadeRenderer.FacadeState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final RenderType MARKER = RenderTypes.entitySolid(Nexium.id("textures/misc/white.png"));
    /** The plate sits on the machine's face: from {@code PLATE_NEAR} to the block edge, covering the whole face. */
    private static final float PLATE_NEAR = 7.4f / 16, PLATE_FAR = 8.0f / 16, PLATE_HALF = 7.9f / 16;
    /** How far apart the strands are (their height), measured along the lane axis. */
    private static final float LANE_STEP = 1.5f / 16;
    private static final int PLATE_T1 = 0xFF3C3C46, PLATE_T2 = 0xFFB4BAC4, PLATE_EDGE_T1 = 0xFF6A6A78, PLATE_EDGE_T2 = 0xFFE6EBF2;
    /** Which conduit type each strand carries, top lane first. */
    private static final int[] LANE_TYPE = {ConduitType.ENERGY.ordinal(), ConduitType.ITEM.ordinal(), ConduitType.FLUID.ordinal()};
    private static final int WORD_OFF = 0xFF70707C;
    /** The colour of each strand's word, by conduit type: item green, fluid blue, energy red. */
    private static final int[] STRAND_COLOR = new int[3];
    static {
        STRAND_COLOR[ConduitType.ITEM.ordinal()] = 0xFF48E060;
        STRAND_COLOR[ConduitType.FLUID.ordinal()] = 0xFF4D8CFF;
        STRAND_COLOR[ConduitType.ENERGY.ordinal()] = 0xFFFF4040;
    }
    /** Where each strand's word goes, in sixteenths from the middle: energy across the top, item up the left edge, fluid up the right. */
    private static final float[] TEXT_POS = {6.2f, -5.95f, 5.95f};
    private static final float PIXEL = 0.42f;

    public static class FacadeState extends BlockEntityRenderState {
        /** Whether the conduit is Tier 2 (silver plates). */
        public boolean tier2;
        public final BlockModelRenderState facade = new BlockModelRenderState();
        /** [type][side] packed mode, -1 for none. */
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
                if (connected) plate(p, buffer, Arm.of(d), state, light);
            }
        });
    }

    /** Axis bookkeeping for one arm: the arm axis, the axis the strands stack along, the other one, and which way energy sits. */
    private record Arm(Direction d, int axis, int step, int lane, int side, float laneSign) {
        static Arm of(Direction d) {
            int axis = d.getAxis().ordinal();
            return new Arm(d, axis, d.getAxisDirection().getStep(), axis == 1 ? 2 : 1, axis == 0 ? 2 : 0, 1);
        }

        /** Where strand {@code t} (0 energy, 1 item, 2 fluid) is along the lane axis, in blocks from the middle. */
        float laneOffset(int t) { return laneSign * (1 - t) * LANE_STEP; }
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
        float[] up = new float[3];
        up[a.lane] = a.laneSign;
        float[] fwd = new float[3];
        fwd[a.axis] = a.step;
        // text runs to the viewer's right, which is (arm direction) x (up); only the sign along the side axis matters
        float rightSign = fwd[1] * up[2] - fwd[2] * up[1] + fwd[2] * up[0] - fwd[0] * up[2] + fwd[0] * up[1] - fwd[1] * up[0];
        for (int t = 0; t < 3; t++) {
            int type = LANE_TYPE[t];
            int marker = st.channels[type][a.d.get3DDataValue()];
            boolean on = marker >= 0;
            SideConfig.Mode mode = on ? SideConfig.Mode.values()[(marker >> 4) & 3] : SideConfig.Mode.DISABLED;
            String word = !on || mode == SideConfig.Mode.DISABLED ? "OFF" : mode.inserts() && mode.extracts() ? "BOTH" : mode.inserts() ? "INSERT" : "EXTRACT";
            int colour = on && mode != SideConfig.Mode.DISABLED ? STRAND_COLOR[type] : WORD_OFF;
            // the words sit above and below the strands, which hide the middle of the plate
            int lit = net.minecraft.util.LightCoordsUtil.FULL_BRIGHT;
            text(p, b, a, inner, rightSign, t > 0, TEXT_POS[t], word, colour, lit);
        }
    }

    /** A flat rectangle on the plate; {@code u} runs along the text (to the viewer's right), {@code v} up, both in sixteenths from the middle. */
    private static void rect(PoseStack.Pose p, VertexConsumer b, Arm a, float plane, float rightSign, float u0, float v0, float u1, float v1, int color, int light) {
        float z = plane - a.step * 0.12f / 16;
        float s0 = 0.5f + rightSign * u0 / 16, s1 = 0.5f + rightSign * u1 / 16;
        float l0 = 0.5f + a.laneSign * v0 / 16, l1 = 0.5f + a.laneSign * v1 / 16;
        float[][] pts = new float[4][3];
        float[] sd = {s0, s1, s1, s0};
        float[] ln = {l0, l0, l1, l1};
        for (int i = 0; i < 4; i++) { pts[i][a.axis] = z; pts[i][a.lane] = ln[i]; pts[i][a.side] = sd[i]; }
        // an upward normal, so the letters are lit like a top face whichever way the plate faces
        quad(p, b, color, light, 0, 1, 0, pts[0][0], pts[0][1], pts[0][2], pts[1][0], pts[1][1], pts[1][2],
                pts[2][0], pts[2][1], pts[2][2], pts[3][0], pts[3][1], pts[3][2]);
    }

    /**
     * {@code word} in 3x5 pixel letters. Horizontal: centred across the plate with its middle at height {@code pos}. Vertical: reading
     * upward in the column {@code pos} from the middle, starting at the bottom edge.
     */
    private static void text(PoseStack.Pose p, VertexConsumer b, Arm a, float plane, float rightSign, boolean vertical, float pos, String word, int color, int light) {
        float px = PIXEL;
        float left = -(word.length() * 4 - 1) * px / 2;
        for (int i = 0; i < word.length(); i++) {
            String[] g = GLYPHS.get(word.charAt(i));
            for (int row = 0; row < 5; row++) {
                String bits = g[row];
                for (int col = 0; col < 3; ) {
                    if (bits.charAt(col) != '1') { col++; continue; }
                    int end = col;
                    while (!vertical && end < 3 && bits.charAt(end) == '1') end++;
                    if (vertical) end = col + 1;
                    if (vertical) {
                        float v = -7.0f + (i * 4 + col) * px, u = pos - 2.5f * px + row * px;
                        rect(p, b, a, plane, rightSign, u, v, u + px, v + px, color, light);
                    } else {
                        float u = left + (i * 4 + col) * px;
                        float top = pos + 2.5f * px - row * px;
                        rect(p, b, a, plane, rightSign, u, top - px, u + (end - col) * px, top, color, light);
                    }
                    col = end;
                }
            }
        }
    }

    private static final java.util.Map<Character, String[]> GLYPHS = java.util.Map.ofEntries(
            java.util.Map.entry('I', new String[]{"111", "010", "010", "010", "111"}),
            java.util.Map.entry('N', new String[]{"111", "101", "101", "101", "101"}),
            java.util.Map.entry('S', new String[]{"111", "100", "111", "001", "111"}),
            java.util.Map.entry('E', new String[]{"111", "100", "110", "100", "111"}),
            java.util.Map.entry('R', new String[]{"110", "101", "110", "101", "101"}),
            java.util.Map.entry('T', new String[]{"111", "010", "010", "010", "010"}),
            java.util.Map.entry('X', new String[]{"101", "101", "010", "101", "101"}),
            java.util.Map.entry('A', new String[]{"010", "101", "111", "101", "101"}),
            java.util.Map.entry('C', new String[]{"111", "100", "100", "100", "111"}),
            java.util.Map.entry('B', new String[]{"110", "101", "110", "101", "110"}),
            java.util.Map.entry('O', new String[]{"111", "101", "101", "101", "111"}),
            java.util.Map.entry('H', new String[]{"101", "101", "111", "101", "101"}),
            java.util.Map.entry('F', new String[]{"111", "100", "110", "100", "100"}));

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
