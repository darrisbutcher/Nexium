package net.nexium.client.dog;

import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.nexium.Nexium;
import net.nexium.dog.DogEntity;

/** Draws the dog with its own model and skin, and a wolf's collar. */
public class DogRenderer extends AgeableMobRenderer<DogEntity, WolfRenderState, WolfModel> {
    public static final Identifier TEXTURE = Nexium.id("textures/entity/dog/dog.png");

    public DogRenderer(EntityRendererProvider.Context context) {
        super(context, new DogModel(context.bakeLayer(DogModel.LAYER)), new DogModel(context.bakeLayer(DogModel.LAYER)), 0.5F);
        addLayer(new DogArmorLayer(this, context.getModelSet()));
        addLayer(new DogCollarLayer(this));
    }

    @Override
    protected int getModelTint(WolfRenderState state) {
        float shade = state.wetShade;
        return shade == 1.0F ? -1 : ARGB.colorFromFloat(1.0F, shade, shade, shade);
    }

    /** A little smaller than the model's size, which was drawn for a stocky dog with tall ears. */
    @Override
    protected void scale(WolfRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public Identifier getTextureLocation(WolfRenderState state) {
        return TEXTURE;
    }

    @Override
    public WolfRenderState createRenderState() {
        return new WolfRenderState();
    }

    @Override
    public void extractRenderState(DogEntity dog, WolfRenderState state, float partialTick) {
        super.extractRenderState(dog, state, partialTick);
        state.isAngry = dog.isAngry();
        state.isSitting = dog.isInSittingPose();
        state.tailAngle = dog.getTailAngle();
        state.headRollAngle = dog.getHeadRollAngle(partialTick);
        state.shakeAnim = dog.getShakeAnim(partialTick);
        state.texture = TEXTURE;
        state.wetShade = dog.getWetShade(partialTick);
        state.collarColor = dog.isTame() ? dog.getCollarColor() : null;
        state.bodyArmorItem = dog.getBodyArmorItem().copy();
    }
}
