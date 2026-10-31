package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Draws the facade block over a conduit bundle that has one. */
public class FacadeRenderer implements BlockEntityRenderer<ConduitBundleBlockEntity, FacadeRenderer.FacadeState> {
    public static class FacadeState extends BlockEntityRenderState {
        public @Nullable BlockState facade;
    }

    public FacadeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public FacadeState createRenderState() {
        return new FacadeState();
    }

    @Override
    public void extractRenderState(ConduitBundleBlockEntity be, FacadeState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facade = be.facade();
    }

    @Override
    public void submit(FacadeState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.facade == null) return;
        collector.submitBlock(pose, state.facade, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
