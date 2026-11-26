package net.nexium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.nexium.fluid.FluidTankBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Draws the fluid inside a Fluid Tank, filled to its level, in the fluid's own texture and colour (lava glows). */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.TankState> {
    /** How far the fluid is kept in from the frame, in blocks, and the lowest and highest it reaches. */
    private static final float INSET = 1.5f / 16, FLOOR = 1.5f / 16, CEILING = 14.5f / 16;

    public static class TankState extends BlockEntityRenderState {
        @Nullable public TextureAtlasSprite sprite;
        public int color = 0xFFFFFFFF;
        public float level;
        public boolean glows;
    }

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TankState createRenderState() {
        return new TankState();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity be, TankState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        FluidVariant variant = be.fluid();
        long amount = be.amount();
        if (variant.isBlank() || amount <= 0) {
            state.sprite = null;
            state.level = 0;
            return;
        }
        state.sprite = net.minecraft.client.Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(variant.getFluid().defaultFluidState()).stillMaterial().sprite();
        state.color = 0xFF000000 | FluidVariantRendering.getColor(variant);
        state.level = (float) Math.min(1.0, (double) amount / be.capacity());
        state.glows = FluidVariantAttributes.getLuminance(variant) > 8;
    }

    @Override
    public void submit(TankState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null || state.level <= 0) return;
        int light = state.glows ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
        float top = FLOOR + (CEILING - FLOOR) * state.level;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS), (p, buffer) -> {
            float x0 = INSET, x1 = 1 - INSET;
            float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
            float vTop = v0 + (v1 - v0) * (top - FLOOR) / (CEILING - FLOOR);
            // sides (the texture's lower part, so a half full tank shows the lower half of the sprite), top and bottom
            quad(p, buffer, state.color, light, 0, 0, -1, x0, FLOOR, x0, x1, FLOOR, x0, x1, top, x0, x0, top, x0, u0, v1, u1, v1, u1, vTop, u0, vTop);
            quad(p, buffer, state.color, light, 0, 0, 1, x1, FLOOR, x1, x0, FLOOR, x1, x0, top, x1, x1, top, x1, u0, v1, u1, v1, u1, vTop, u0, vTop);
            quad(p, buffer, state.color, light, -1, 0, 0, x0, FLOOR, x1, x0, FLOOR, x0, x0, top, x0, x0, top, x1, u0, v1, u1, v1, u1, vTop, u0, vTop);
            quad(p, buffer, state.color, light, 1, 0, 0, x1, FLOOR, x0, x1, FLOOR, x1, x1, top, x1, x1, top, x0, u0, v1, u1, v1, u1, vTop, u0, vTop);
            quad(p, buffer, state.color, light, 0, 1, 0, x0, top, x0, x0, top, x1, x1, top, x1, x1, top, x0, u0, v0, u0, v1, u1, v1, u1, v0);
            quad(p, buffer, state.color, light, 0, -1, 0, x0, FLOOR, x1, x0, FLOOR, x0, x1, FLOOR, x0, x1, FLOOR, x1, u0, v0, u0, v1, u1, v1, u1, v0);
        });
    }

    /** One face, given as four corners (x, y, z each) and the texture coordinates of each; the sides run bottom to top. */
    private static void quad(PoseStack.Pose p, VertexConsumer b, int color, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float au, float av, float bu, float bv, float cu, float cv, float du, float dv) {
        vertex(p, b, color, light, nx, ny, nz, ax, ay, az, au, av);
        vertex(p, b, color, light, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(p, b, color, light, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(p, b, color, light, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer b, int color, int light, float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        b.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
