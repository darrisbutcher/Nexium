package net.storagenexus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
import net.storagenexus.StorageNexus;
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

    /** Plain white texture tinted per vertex, for the fill bars. */
    private static final RenderType BAR = RenderTypes.entitySolid(StorageNexus.id("textures/misc/white.png"));

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

    protected static float fraction(long amount, long capacity) {
        return capacity <= 0 ? 0 : (float) Math.min(1.0, (double) amount / capacity);
    }

    @Override
    public void extractRenderState(T be, FrontRenderState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.frontLight = be.getLevel() != null
                ? LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos().relative(state.facing))
                : state.lightCoords;
        for (int i = 0; i < 4; i++) {
            state.present[i] = false;
            state.fill[i] = -1;
        }
        fill(be, state);
        state.locked = be instanceof Upgradeable u && u.hasUpgrade(UpgradeItem.Kind.LOCK);
        if (state.locked) {
            itemModels.updateForTopItem(state.lockIcon, new ItemStack(ModItems.LOCK_UPGRADE), ItemDisplayContext.GUI, be.getLevel(), null, 0);
        } else {
            state.lockIcon.clear();
        }
        state.voiding = be instanceof Upgradeable u && u.hasUpgrade(UpgradeItem.Kind.VOID);
        if (state.voiding) {
            itemModels.updateForTopItem(state.voidIcon, new ItemStack(ModItems.VOID_UPGRADE), ItemDisplayContext.GUI, be.getLevel(), null, 0);
        } else {
            state.voidIcon.clear();
        }
    }

    @Override
    public void submit(FrontRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.slots <= 0 || state.slots > 4) return;
        double[][] layout = LAYOUT[state.slots];
        // Each cell: the item in its upper part and the count below it, with a gap between.
        // (Layout y is 0.01 above the cell's middle.)
        boolean single = state.slots == 1;
        float itemScale = single ? 0.45f : 0.26f;
        float itemLift = single ? 0.02f : 0.05f;
        float textScale = single ? 0.011f : 0.007f;
        // Multi-section fronts lift the count a little so the fill bar fits above the trim
        float countDrop = single ? 0.26f : 0.115f;
        float barWidth = single ? 0.5f : 0.32f, barHeight = single ? 0.018f : 0.012f;
        float barGap = single ? 0.012f : 0.006f;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
        pose.translate(0, 0, 0.5 + 0.005);
        for (int i = 0; i < layout.length; i++) {
            if (!state.present[i]) continue;
            float x = (float) layout[i][0] - 0.5f;
            float y = (float) layout[i][1] - 0.5f;

            pose.pushPose();
            pose.translate(x, y + itemLift, 0);
            pose.scale(itemScale, itemScale, itemScale * 0.05f);
            state.items[i].submit(pose, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();

            String count = state.counts[i];
            if (count != null && !count.isEmpty()) {
                pose.pushPose();
                pose.translate(x, y - countDrop, 0.002f);
                pose.scale(textScale, -textScale, textScale);
                float w = font.width(count);
                collector.submitText(pose, -w / 2f, 0, Component.literal(count).getVisualOrderText(), false,
                        Font.DisplayMode.POLYGON_OFFSET, state.frontLight, 0xFFFFFFFF, 0, 0);
                pose.popPose();
            }
        }
        // Fill bars: one under each section's count
        final float top0 = 9 * textScale + barGap;
        collector.submitCustomGeometry(pose, BAR, (p, buffer) -> {
            for (int i = 0; i < layout.length; i++) {
                float f = state.fill[i];
                if (f < 0) continue;
                float cx = (float) layout[i][0] - 0.5f;
                float top = (float) layout[i][1] - 0.5f - countDrop - top0;
                float left = cx - barWidth / 2, right = cx + barWidth / 2, bottom = top - barHeight;
                float edge = barHeight * 0.35f;
                rect(p, buffer, left - edge, top + edge, right + edge, bottom - edge, 0.001f, 0xFF141414, state.frontLight);
                rect(p, buffer, left, top, right, bottom, 0.002f, 0xFF3C3C3C, state.frontLight);
                if (f > 0) {
                    int color = f < 0.75f ? 0xFF4CAF50 : f < 0.9f ? 0xFFE0C040 : 0xFFE05050;
                    rect(p, buffer, left, top, left + barWidth * Math.max(f, 0.02f), bottom, 0.003f, color, state.frontLight);
                }
            }
        });
        if (state.voiding) {
            pose.pushPose();
            pose.translate(-0.44f, 0.44f, 0.001f); // top-left corner: this drawer deletes overflow
            pose.scale(0.08f, 0.08f, 0.08f * 0.05f);
            state.voidIcon.submit(pose, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (state.locked) {
            pose.pushPose();
            pose.translate(0.44f, 0.44f, 0.001f); // top-right corner of the front, clear of the items
            pose.scale(0.08f, 0.08f, 0.08f * 0.05f);
            state.lockIcon.submit(pose, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    /** A flat rectangle on the front, facing out (+z), drawn from both sides so culling can't hide it. */
    private static void rect(PoseStack.Pose p, VertexConsumer b, float x0, float y0, float x1, float y1, float z,
                             int color, int light) {
        vertex(p, b, x0, y0, z, 0, 0, color, light);
        vertex(p, b, x0, y1, z, 0, 1, color, light);
        vertex(p, b, x1, y1, z, 1, 1, color, light);
        vertex(p, b, x1, y0, z, 1, 0, color, light);
        vertex(p, b, x0, y0, z, 0, 0, color, light);
        vertex(p, b, x1, y0, z, 1, 0, color, light);
        vertex(p, b, x1, y1, z, 1, 1, color, light);
        vertex(p, b, x0, y1, z, 0, 1, color, light);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer b, float x, float y, float z, float u, float v, int color, int light) {
        b.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
    }

    protected static Direction facingOf(BlockEntity be) {
        return be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
}
