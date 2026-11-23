package net.nexium.client.jetpack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.nexium.Nexium;
import net.nexium.essentials.client.WingsRenderState;
import net.nexium.jetpack.JetpackItem;

/** Draws the Jetpack on the back of a player who wears one, in the wings slot or the chest slot. */
public class JetpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEXTURE = Nexium.id("textures/entity/jetpack.png");

    private final JetpackModel model;

    public JetpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new JetpackModel(modelSet.bakeLayer(JetpackModel.LAYER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        ItemStack wings = state instanceof WingsRenderState w ? w.nexium$wings() : ItemStack.EMPTY;
        if (!(wings.getItem() instanceof JetpackItem) && !(state.chestEquipment.getItem() instanceof JetpackItem)) return;
        poseStack.pushPose();
        // Against the torso, so it follows a sneak and the lean of a flight
        getParentModel().body.translateAndRotate(poseStack);
        renderColoredCutoutModel(model, TEXTURE, poseStack, collector, light, state, -1, 1);
        poseStack.popPose();
    }
}
