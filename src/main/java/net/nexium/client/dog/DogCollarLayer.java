package net.nexium.client.dog;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.nexium.Nexium;

/** The dog's collar: a white band round the neck, tinted with the collar's dye colour (green for Zoe until it is dyed). */
public class DogCollarLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private static final Identifier COLLAR = Nexium.id("textures/entity/dog/dog_collar.png");

    public DogCollarLayer(RenderLayerParent<WolfRenderState, WolfModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WolfRenderState state, float yRot, float xRot) {
        if (state.collarColor == null || state.isInvisible) return;
        int color = ARGB.opaque(state.collarColor.getTextureDiffuseColor());
        renderColoredCutoutModel(getParentModel(), COLLAR, poseStack, collector, light, state, color, 1);
    }
}
