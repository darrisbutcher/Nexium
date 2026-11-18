package net.nexium.client.dog;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.Nexium;
import net.nexium.registry.ModItems;

/**
 * The dog's body armor. A wolf's armor is made for a wolf's shape, so the dog has its own armor model: its own pieces,
 * a little larger, with a plate texture over the chest, hips, neck and upper legs. Wolf Armor and the Nexium Dog Armor each have their look.
 */
public class DogArmorLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private static final Identifier WOLF_ARMOR = Nexium.id("textures/entity/dog/armor_wolf.png");
    private static final Identifier NEXIUM_ARMOR = Nexium.id("textures/entity/dog/armor_nexium.png");

    private final DogModel armorModel;

    public DogArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.armorModel = new DogModel(modelSet.bakeLayer(DogModel.ARMOR_LAYER));
    }

    /** The texture for what the dog wears, or null for nothing a dog can wear. */
    static Identifier textureFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ModItems.NEXIUM_DOG_ARMOR)) return NEXIUM_ARMOR;
        if (stack.is(Items.WOLF_ARMOR)) return WOLF_ARMOR;
        return null;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WolfRenderState state, float yRot, float xRot) {
        Identifier texture = textureFor(state.bodyArmorItem);
        if (texture == null || state.isInvisible) return;
        armorModel.setupAnim(state);
        renderColoredCutoutModel(armorModel, texture, poseStack, collector, light, state, -1, 1);
    }
}
