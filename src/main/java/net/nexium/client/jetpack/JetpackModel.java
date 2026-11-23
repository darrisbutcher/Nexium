package net.nexium.client.jetpack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.nexium.Nexium;

/**
 * The Jetpack as worn: two tanks with domes, a red band and nozzles that glow, a plate between them with a green battery light, and a strap
 * round the chest. Its pieces are placed against the body part of the player (the origin is the top of the torso, the back is +z), and the
 * 64x64 skin is generated to match, see the picture {@code textures/entity/jetpack.png}.
 */
public class JetpackModel extends EntityModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Nexium.id("jetpack"), "main");

    public JetpackModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition pack = mesh.getRoot().addOrReplaceChild("pack", CubeListBuilder.create(), PartPose.ZERO);
        pack.addOrReplaceChild("tank_left", CubeListBuilder.create().texOffs(0, 0).addBox(-3.8F, 1.0F, 2.1F, 3F, 9F, 3F), PartPose.ZERO);
        pack.addOrReplaceChild("tank_right", CubeListBuilder.create().texOffs(12, 0).addBox(0.8F, 1.0F, 2.1F, 3F, 9F, 3F), PartPose.ZERO);
        pack.addOrReplaceChild("dome_left", CubeListBuilder.create().texOffs(24, 0).addBox(-3.3F, 0.0F, 2.6F, 2F, 1F, 2F), PartPose.ZERO);
        pack.addOrReplaceChild("dome_right", CubeListBuilder.create().texOffs(32, 0).addBox(1.3F, 0.0F, 2.6F, 2F, 1F, 2F), PartPose.ZERO);
        pack.addOrReplaceChild("nozzle_left", CubeListBuilder.create().texOffs(40, 0).addBox(-3.3F, 10.0F, 2.6F, 2F, 2F, 2F), PartPose.ZERO);
        pack.addOrReplaceChild("nozzle_right", CubeListBuilder.create().texOffs(48, 0).addBox(1.3F, 10.0F, 2.6F, 2F, 2F, 2F), PartPose.ZERO);
        pack.addOrReplaceChild("flame_left", CubeListBuilder.create().texOffs(56, 0).addBox(-2.8F, 12.0F, 3.1F, 1F, 2F, 1F), PartPose.ZERO);
        pack.addOrReplaceChild("flame_right", CubeListBuilder.create().texOffs(60, 0).addBox(1.8F, 12.0F, 3.1F, 1F, 2F, 1F), PartPose.ZERO);
        pack.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 1.5F, 2.6F, 4F, 8F, 1F), PartPose.ZERO);
        pack.addOrReplaceChild("light", CubeListBuilder.create().texOffs(10, 12).addBox(-1.0F, 3.0F, 3.1F, 2F, 1F, 1F), PartPose.ZERO);
        pack.addOrReplaceChild("strap", CubeListBuilder.create().texOffs(16, 12).addBox(-4.5F, 2.5F, -2.5F, 9F, 2F, 5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
