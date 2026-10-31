package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.dimension.LinkedBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Floating link name above Dimensional Inputs and Outputs, shown within 12 blocks. */
public class LinkLabelRenderer<T extends LinkedBlockEntity> implements BlockEntityRenderer<T, LinkLabelRenderer.LabelState> {
    private static final double MAX_DIST_SQ = 12 * 12;

    public static class LabelState extends BlockEntityRenderState {
        public String name = "";
        public boolean visible;
    }

    private final Font font;

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
        state.visible = !state.name.isEmpty() && cameraPos.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) < MAX_DIST_SQ;
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
