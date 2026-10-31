package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.drawer.DrawerInteraction;
import net.storagenexus.registry.ModItems;
import net.storagenexus.upgrade.UpgradeItem;
import net.storagenexus.upgrade.Upgradeable;
import org.jetbrains.annotations.Nullable;

/**
 * Renders items and counts on the front face of drawers, using the same slot layout as
 * {@link DrawerInteraction#slotAt}.
 */
public abstract class AbstractFrontRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, FrontRenderState> {
    private static final double[][][] LAYOUT = {
            {},
            {{0.5, 0.56}},
            {{0.5, 0.76}, {0.5, 0.26}},
            {{0.5, 0.76}, {0.25, 0.26}, {0.75, 0.26}},
            {{0.25, 0.76}, {0.75, 0.76}, {0.25, 0.26}, {0.75, 0.26}}
    };

    protected final ItemModelResolver itemModels;
    protected final Font font;

    protected AbstractFrontRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
        this.font = context.font();
    }

    @Override
    public FrontRenderState createRenderState() {
        return new FrontRenderState();
    }

    /** Fill slots/present/counts and call {@link #setItem} for each visible slot. */
    protected abstract void fill(T be, FrontRenderState state);

    protected void setItem(T be, FrontRenderState state, int slot, ItemStack stack, String count) {
        state.present[slot] = !stack.isEmpty();
        state.counts[slot] = count;
        if (stack.isEmpty()) state.items[slot].clear();
        // GUI context: blocks look like their inventory icons instead of a dim flat face
        else itemModels.updateForTopItem(state.items[slot], stack, ItemDisplayContext.GUI, be.getLevel(), null, 0);
    }

    @Override
    public void extractRenderState(T be, FrontRenderState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.frontLight = be.getLevel() != null
                ? LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos().relative(state.facing))
                : state.lightCoords;
        for (int i = 0; i < 4; i++) state.present[i] = false;
        fill(be, state);
        state.locked = be instanceof Upgradeable u && u.hasUpgrade(UpgradeItem.Kind.LOCK);
        if (state.locked) {
            itemModels.updateForTopItem(state.lockIcon, new ItemStack(ModItems.LOCK_UPGRADE), ItemDisplayContext.GUI, be.getLevel(), null, 0);
        } else {
            state.lockIcon.clear();
        }
    }

    @Override
    public void submit(FrontRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.slots <= 0 || state.slots > 4) return;
        double[][] layout = LAYOUT[state.slots];
        float itemScale = state.slots == 1 ? 0.5f : 0.3f;
        float textScale = state.slots == 1 ? 0.012f : 0.008f;
        float textDrop = state.slots == 1 ? 0.33f : 0.17f;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
        pose.translate(0, 0, 0.5 + 0.005);
        for (int i = 0; i < layout.length; i++) {
            if (!state.present[i]) continue;
            float x = (float) layout[i][0] - 0.5f;
            float y = (float) layout[i][1] - 0.5f;

            pose.pushPose();
            pose.translate(x, y + 0.03f, 0);
            pose.scale(itemScale, itemScale, itemScale * 0.05f);
            state.items[i].submit(pose, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();

            String count = state.counts[i];
            if (count != null && !count.isEmpty()) {
                pose.pushPose();
                pose.translate(x, y - textDrop + 0.08f, 0.002f);
                pose.scale(textScale, -textScale, textScale);
                float w = font.width(count);
                collector.submitText(pose, -w / 2f, 0, Component.literal(count).getVisualOrderText(), false,
                        Font.DisplayMode.POLYGON_OFFSET, state.frontLight, 0xFFFFFFFF, 0, 0);
                pose.popPose();
            }
        }
        if (state.locked) {
            pose.pushPose();
            pose.translate(0.40f, 0.40f, 0.001f); // top-right corner of the front
            pose.scale(0.12f, 0.12f, 0.12f * 0.05f);
            state.lockIcon.submit(pose, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    protected static Direction facingOf(BlockEntity be) {
        return be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
}
