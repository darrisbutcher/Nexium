package net.nexium.client.dog;

import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.nexium.Nexium;

/**
 * The dog's model. It has a wolf's bones (so a wolf's walking, head turns, tail and collar all work) and the wolf's
 * texture layout for the head, body, legs and tail (so Wolf Armor still sits right), but a heeler's look: big upright ears, a
 * shorter muzzle, and a lying-down pose where a wolf would sit.
 */
public class DogModel extends WolfModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Nexium.id("dog"), "main");

    private final ModelPart realHead, upperBody, realTail;

    public DogModel(ModelPart root) {
        super(root);
        this.realHead = head.getChild("real_head");
        this.upperBody = root.getChild("upper_body");
        this.realTail = tail.getChild("real_tail");
    }

    public static LayerDefinition createLayer(CubeDeformation grow) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0F, 13.5F, -7.0F));
        PartDefinition realHead = head.addOrReplaceChild("real_head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, grow)
                        // A shorter muzzle than a wolf's
                        .texOffs(0, 10).addBox(-0.5F, -0.001F, -4.0F, 3.0F, 3.0F, 3.0F, grow),
                PartPose.ZERO);
        // Big upright ears, leaning out a little, with their own place on the texture
        realHead.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(44, 14).addBox(-1.5F, -4.0F, 0.0F, 3.0F, 4.0F, 1.0F, grow),
                PartPose.offsetAndRotation(-0.5F, -3.0F, 0.0F, 0.0F, 0.0F, -0.14F));
        realHead.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(44, 20).addBox(-1.5F, -4.0F, 0.0F, 3.0F, 4.0F, 1.0F, grow),
                PartPose.offsetAndRotation(2.5F, -3.0F, 0.0F, 0.0F, 0.0F, 0.14F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, grow),
                PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(21, 0).addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, grow),
                PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, grow);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(0.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(0.5F, 16.0F, -4.0F));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, 0.62831855F, 0.0F, 0.0F));
        tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, grow), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Lying down, front paws out in front and the head resting on them, instead of a wolf's sit. */
    @Override
    protected void setSittingPose(WolfRenderState state) {
        float scale = state.ageScale;
        body.y = 21.0F * scale + (1.0F - scale) * 24.0F;
        body.z = 1.0F * scale;
        upperBody.y = 21.0F * scale + (1.0F - scale) * 24.0F;
        upperBody.z = -3.0F * scale;
        upperBody.xRot = (float) (Math.PI / 2);
        upperBody.yRot = 0.0F;
        head.y = 19.0F * scale + (1.0F - scale) * 24.0F;
        head.z = -8.0F * scale;
        // Front legs stretched forward along the ground
        rightFrontLeg.y = 22.0F * scale + (1.0F - scale) * 24.0F;
        rightFrontLeg.z = -4.0F * scale;
        rightFrontLeg.xRot = (float) (-Math.PI / 2);
        leftFrontLeg.y = rightFrontLeg.y;
        leftFrontLeg.z = rightFrontLeg.z;
        leftFrontLeg.xRot = (float) (-Math.PI / 2);
        // Hind legs folded up beside the body
        rightHindLeg.y = 21.5F * scale + (1.0F - scale) * 24.0F;
        rightHindLeg.z = 6.0F * scale;
        rightHindLeg.xRot = -1.2F;
        leftHindLeg.y = rightHindLeg.y;
        leftHindLeg.z = rightHindLeg.z;
        leftHindLeg.xRot = -1.2F;
        // Tail resting on the ground behind
        tail.y = 22.0F * scale + (1.0F - scale) * 24.0F;
        tail.z = 9.0F * scale;
        tail.xRot = 1.35F;
    }

    @Override
    protected void shakeOffWater(WolfRenderState state) {
        realHead.zRot = state.headRollAngle;
        body.zRot = state.getBodyRollAngle(-0.16F);
        upperBody.zRot = state.getBodyRollAngle(-0.08F);
        realTail.zRot = state.getBodyRollAngle(-0.2F);
    }
}
