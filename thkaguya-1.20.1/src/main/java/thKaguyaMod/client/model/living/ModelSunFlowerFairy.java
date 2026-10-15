package thKaguyaMod.client.model.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import thKaguyaMod.entity.living.EntityDanmakuMob;

/** 自动生成自原版 ModelSunFlowerFairy（Techne → LayerDefinition） */
public class ModelSunFlowerFairy<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart bipedHead;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;

    public ModelSunFlowerFairy(ModelPart root)
    {
        this.root = root;
        this.rightWing = root.getChild("bipedBody").getChild("rightWing");
        this.leftWing = root.getChild("bipedBody").getChild("leftWing");
        this.bipedHead = root.getChild("bipedHead");
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 0F, -2F, 6, 7, 4),
            PartPose.offset(0F, 5F, 0F));
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1F, -1F, -1F, 2, 8, 2),
            PartPose.offsetAndRotation(-4F, 6F, 0F, -0.7679449F, 0F, -0.6457718F));
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
            CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1F, -1F, -1F, 2, 8, 2),
            PartPose.offsetAndRotation(4F, 6F, 0F, -0.7679449F, 0F, 0.6457718F));
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
            CubeListBuilder.create().texOffs(50, 16).mirror().addBox(-1F, 0F, -2F, 3, 11, 4),
            PartPose.offset(-2F, 12F, 0F));
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
            CubeListBuilder.create().texOffs(50, 16).mirror().addBox(-2F, 0F, -2F, 3, 11, 4),
            PartPose.offset(2F, 12F, 0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, 5F, -4F, 8, 4, 8),
                PartPose.offset(0F, 0F, 0F));
            PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                CubeListBuilder.create().texOffs(32, 32).mirror().addBox(-5F, 0F, -5F, 10, 6, 10),
                PartPose.offset(0F, 9F, 0F));
            PartDefinition leftWing = bipedBody.addOrReplaceChild("leftWing",
                CubeListBuilder.create().texOffs(64, 0).addBox(-1F, -16F, 0F, 20, 26, 1),
                PartPose.offsetAndRotation(2F, 3F, 2F, 0F, -0.4886922F, 0.0167304F));
            PartDefinition rightWing = bipedBody.addOrReplaceChild("rightWing",
                CubeListBuilder.create().texOffs(72, 32).addBox(-1F, -16F, 0F, 20, 26, 1),
                PartPose.offsetAndRotation(-2F, 3F, 2F, 0F, -2.6529F, 0.0167304F));
            PartDefinition himawari1 = bipedLeftArm.addOrReplaceChild("himawari1",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-7F, -20F, 0F, 12, 31, 1),
                PartPose.offsetAndRotation(0F, 6F, -2F, 0.4089647F, -0.7435722F, 0.3666439F));
            PartDefinition himawari2 = bipedLeftArm.addOrReplaceChild("himawari2",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-7F, -20F, 0F, 12, 31, 1),
                PartPose.offsetAndRotation(-1F, 6F, -2F, 0.2230717F, -0.669215F, 0.2551081F));
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
            PartPose.offset(0F, 5F, 0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-4F, 4F, -3F, 8, 5, 3),
                PartPose.offset(0F, -5F, 4F));
            PartDefinition rightRibbon = bipedHead.addOrReplaceChild("rightRibbon",
                CubeListBuilder.create().texOffs(32, 19).mirror().addBox(-3F, -2F, -1F, 5, 4, 1),
                PartPose.offsetAndRotation(-4F, -7F, 0F, -0.4712389F, 1.570796F, 0F));
            PartDefinition rightTail = bipedHead.addOrReplaceChild("rightTail",
                CubeListBuilder.create().texOffs(32, 24).mirror().addBox(-2F, -1F, -2F, 3, 8, 2),
                PartPose.offsetAndRotation(-5F, -7F, 0F, -0.2617994F, 1.570796F, 0F));
            PartDefinition leftRibbon = bipedHead.addOrReplaceChild("leftRibbon",
                CubeListBuilder.create().texOffs(32, 19).mirror().addBox(-3F, -2F, 0F, 5, 4, 1),
                PartPose.offsetAndRotation(4F, -7F, 0F, 0.4712389F, 1.570796F, 0F));
            PartDefinition leftTail = bipedHead.addOrReplaceChild("leftTail",
                CubeListBuilder.create().texOffs(32, 24).mirror().addBox(-2F, -1F, 0F, 3, 8, 2),
                PartPose.offsetAndRotation(5F, -7F, 0F, 0.2617994F, 1.570796F, 0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if(bipedHead != null) { bipedHead.yRot = netHeadYaw * (float)Math.PI / 180F; bipedHead.xRot = headPitch * (float)Math.PI / 180F; }
        if(rightWing != null) { rightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(leftWing != null) { leftWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(bipedRightArm != null) { bipedRightArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(bipedLeftArm != null) { bipedLeftArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(bipedRightLeg != null) { bipedRightLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }
        if(bipedLeftLeg != null) { bipedLeftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}