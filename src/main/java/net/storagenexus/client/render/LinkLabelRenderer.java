package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.dimension.LinkedBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Floating link name above a Dimensional Input or Output: looking straight at it shows the
 * name for 3 seconds (it stays if you look away), and again each time you look back.
 */
public class LinkLabelRenderer<T extends LinkedBlockEntity> implements BlockEntityRenderer<T, LinkLabelRenderer.LabelState> {
    private static final double MAX_DIST_SQ = 12 * 12;
    private static final long SHOW_TICKS = 3 * 20;

    public static class LabelState extends BlockEntityRenderState {
        public String name = "";
        public boolean visible;
    }

    private final Font font;
    /** The block the crosshair is on, and until when each recently looked-at name shows. */
    private @Nullable BlockPos lookedAt;
    private final Map<BlockPos, Long> showUntil = new HashMap<>();

    public LinkLabelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public LabelState createRenderState() {
        return new LabelState();
    }

    @Override
    public void extractRenderState(T be, LabelState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.name = be.displayName();
        BlockPos pos = be.getBlockPos();
        boolean looking = Minecraft.getInstance().hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
        long now = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        if (looking && !pos.equals(lookedAt)) {
            // A new look: show the name for the next 3 seconds, even if you look away
            lookedAt = pos.immutable();
            showUntil.put(lookedAt, now + SHOW_TICKS);
        } else if (!looking && pos.equals(lookedAt)) {
            lookedAt = null; // looking back later shows the name again
        }
        Long until = showUntil.get(pos);
        if (until != null && now >= until) showUntil.remove(pos);
        state.visible = until != null && now < until && !state.name.isEmpty()
                && cameraPos.distanceToSqr(Vec3.atCenterOf(pos)) < MAX_DIST_SQ;
    }

    @Override
    public void submit(LabelState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) return;
        pose.pushPose();
        pose.translate(0.5, 1.35, 0.5);
        pose.rotate(camera.orientation);
        pose.scale(0.025f, -0.025f, 0.025f);
        float w = font.width(state.name);
        collector.submitText(pose, -w / 2f, 0, Component.literal(state.name).getVisualOrderText(), false,
                Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, 0xFFFFFFFF, 0x40000000, 0);
        pose.popPose();
    }
}
