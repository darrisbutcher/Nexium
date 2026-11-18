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
 * The dog's model: a stocky cattle dog built from many small pieces rather than a wolf's few boxes. A chest and a separate rump,
 * a neck that leans forward, a skull with a tapered muzzle and nose, tall pointed ears made of four narrowing pieces,
 * legs with paws and a haunch on the hind legs, and a tail in two pieces. It keeps a wolf's part names (head, body, the
 * four legs, tail) so a wolf's walking, head turns and tail wag drive it, and lies down when told to sit.
 * The skin is a 128x64 layout drawn at 4x (512x256): texture coordinates are fractions of the picture, so a finer picture just shows more.
 */
public class DogModel extends WolfModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Nexium.id("dog"), "main");
    public static final ModelLayerLocation ARMOR_LAYER = new ModelLayerLocation(Nexium.id("dog"), "armor");

    private final ModelPart neck;

    public DogModel(ModelPart root) {
        super(root);
        this.neck = root.getChild("neck");
    }

    /** A box with its place on the skin given in the 128x64 layout. */
    private static CubeListBuilder box(CubeListBuilder b, int u, int v, float x, float y, float z, float w, float h, float d, CubeDeformation grow) {
        return b.texOffs(u, v).addBox(x, y, z, w, h, d, grow);
    }

    private static CubeListBuilder mirrored(CubeListBuilder b, int u, int v, float x, float y, float z, float w, float h, float d, CubeDeformation grow) {
        return b.mirror().texOffs(u, v).addBox(x, y, z, w, h, d, grow).mirror(false);
    }

    public static LayerDefinition createLayer(CubeDeformation grow) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Chest and rump
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, 0.0F));
        body.addOrReplaceChild("chest", box(CubeListBuilder.create(), 0, 0, -3.5F, -4.0F, -7.0F, 7, 8, 7, grow), PartPose.ZERO);
        body.addOrReplaceChild("rump", box(CubeListBuilder.create(), 28, 0, -3.0F, -3.0F, -0.5F, 6, 6, 6, grow), PartPose.ZERO);

        // The neck leans forward from the shoulders; the head sits on its top
        root.addOrReplaceChild("neck", box(CubeListBuilder.create(), 52, 0, -2.5F, -4.0F, -2.5F, 5, 5, 5, grow),
                PartPose.offsetAndRotation(0.0F, 12.5F, -6.0F, 0.5F, 0.0F, 0.0F));

        // The head: skull, a tapered muzzle with the nose, and the ears
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 9.6F, -8.2F));
        head.addOrReplaceChild("skull", box(CubeListBuilder.create(), 74, 0, -3.5F, -3.0F, -3.0F, 7, 6, 6, grow), PartPose.ZERO);
        head.addOrReplaceChild("muzzle", box(CubeListBuilder.create(), 100, 0, -2.0F, -0.3F, -7.0F, 4, 3, 4, grow), PartPose.ZERO);
        head.addOrReplaceChild("nose", box(CubeListBuilder.create(), 116, 0, -1.0F, -0.6F, -7.8F, 2, 1, 1, grow), PartPose.ZERO);
        addEar(head, "right_ear", -2.3F, -0.14F, false, grow);
        addEar(head, "left_ear", 2.3F, 0.14F, true, grow);

        // Legs: front leg and paw; hind leg with its haunch, lower leg and paw
        for (int side = 0; side < 2; side++) {
            float x = side == 0 ? -1.9F : 1.9F;
            String name = side == 0 ? "right" : "left";
            PartDefinition front = root.addOrReplaceChild(name + "_front_leg", box(CubeListBuilder.create(), 28, 16, -1.5F, 0.0F, -1.5F, 3, 5, 3, grow),
                    PartPose.offset(x, 18.0F, -4.5F));
            front.addOrReplaceChild("paw", box(CubeListBuilder.create(), 46, 16, -1.5F, 5.0F, -2.5F, 3, 1, 4, grow), PartPose.ZERO);
            PartDefinition hind = root.addOrReplaceChild(name + "_hind_leg", box(CubeListBuilder.create(), 62, 16, -2.0F, -1.0F, -2.0F, 4, 4, 4, grow),
                    PartPose.offset(x * 1.3F, 17.5F, 4.5F));
            hind.addOrReplaceChild("lower", box(CubeListBuilder.create(), 78, 16, -1.5F, 3.0F, -1.5F, 3, 3, 3, grow), PartPose.ZERO);
            hind.addOrReplaceChild("paw", box(CubeListBuilder.create(), 46, 16, -1.5F, 6.0F, -2.5F, 3, 1, 4, grow), PartPose.ZERO);
        }

        // The tail, in two pieces
        PartDefinition tail = root.addOrReplaceChild("tail", box(CubeListBuilder.create(), 90, 16, -1.0F, 0.0F, -1.0F, 2, 4, 2, grow),
                PartPose.offsetAndRotation(0.0F, 14.0F, 5.5F, 0.55F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tip", box(CubeListBuilder.create(), 98, 16, -1.0F, 0.0F, -1.0F, 2, 3, 2, grow),
                PartPose.offsetAndRotation(0.0F, 3.8F, 0.0F, -0.3F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Four narrowing pieces make a tall pointed ear, leaning out a little. */
    private static void addEar(PartDefinition head, String name, float x, float lean, boolean mirror, CubeDeformation grow) {
        CubeListBuilder b = CubeListBuilder.create();
        float[][] pieces = {{0, 3, 1.5F}, {8, 3, 1.5F}, {16, 2, 1.0F}, {22, 1, 1.0F}}; // skin u, width, height
        float y = 0.0F;
        for (float[] p : pieces) {
            float w = p[1], h = p[2];
            float x0 = -w / 2.0F;
            b = mirror ? mirrored(b, (int) p[0], 16, x0, y - h, -0.5F, w, h, 1, grow) : box(b, (int) p[0], 16, x0, y - h, -0.5F, w, h, 1, grow);
            y -= h;
        }
        head.addOrReplaceChild(name, b, PartPose.offsetAndRotation(x, -2.7F, -0.8F, 0.1F, 0.0F, lean * 1.6F));
    }

    /** Lying down, front paws out in front and the head resting on them. */
    @Override
    protected void setSittingPose(WolfRenderState state) {
        body.y = 20.6F;
        body.z = 0.0F;
        neck.y = 20.2F;
        neck.z = -6.5F;
        neck.xRot = 1.1F;
        head.y = 20.4F;
        head.z = -11.0F;
        rightFrontLeg.y = 22.6F;
        rightFrontLeg.z = -4.0F;
        rightFrontLeg.xRot = (float) (-Math.PI / 2);
        leftFrontLeg.y = rightFrontLeg.y;
        leftFrontLeg.z = rightFrontLeg.z;
        leftFrontLeg.xRot = rightFrontLeg.xRot;
        rightHindLeg.y = 21.5F;
        rightHindLeg.z = 4.0F;
        rightHindLeg.x = -3.3F;
        rightHindLeg.xRot = -1.3F;
        leftHindLeg.y = 21.5F;
        leftHindLeg.z = 4.0F;
        leftHindLeg.x = 3.3F;
        leftHindLeg.xRot = -1.3F;
        tail.y = 21.0F;
        tail.z = 6.0F;
    }

    @Override
    protected void shakeOffWater(WolfRenderState state) {
        head.zRot = state.headRollAngle;
        body.zRot = state.getBodyRollAngle(-0.16F);
        tail.zRot = state.getBodyRollAngle(-0.2F);
    }
}
