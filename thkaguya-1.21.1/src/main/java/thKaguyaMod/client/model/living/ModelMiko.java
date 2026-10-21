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

/** 自动生成自原版 ModelMiko（Techne → LayerDefinition） */
public class ModelMiko<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart bipedHead;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;
    private final ModelPart manto1;
    private final ModelPart manto2;
    private final ModelPart manto3;
    private final ModelPart manto4;
    private final ModelPart skirtTop;
    private final ModelPart skirtBottom;

    public ModelMiko(ModelPart root)
    {
        this.root = root;
        this.bipedHead = root.getChild("bipedBody").getChild("bipedHead");
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedBody").getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedBody").getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedBody").getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedBody").getChild("bipedLeftLeg");
        this.manto1 = root.getChild("bipedBody").getChild("Manto1");
        this.manto2 = this.manto1.getChild("Manto2");
        this.manto3 = this.manto2.getChild("Manto3");
        this.manto4 = this.manto3.getChild("Manto4");
        this.skirtTop = root.getChild("bipedBody").getChild("skirtTop");
        this.skirtBottom = this.skirtTop.getChild("skirtBottom");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 0F, -2F, 6, 9, 4),
            PartPose.offset(0F, 3F, 0F));
            PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-0.8F, -1F, -1F, 2, 8, 2),
                PartPose.offset(-4F, 4F, 0F));
            PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.2F, -1F, -1F, 2, 8, 2),
                PartPose.offset(4F, 4F, 0F));
            PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-1F, 0F, -2F, 3, 12, 4),
                PartPose.offset(-2F, 9F, 0F));
            PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-2F, 0F, -2F, 3, 12, 4),
                PartPose.offset(2F, 0F, 0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, 0F, -6F, 8, 4, 8),
                PartPose.offset(0F, 6F, 2F));
            PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                CubeListBuilder.create().texOffs(16, 31).mirror().addBox(-5F, 0F, -10F, 10, 8, 10),
                PartPose.offset(0F, 4F, 3F));
            PartDefinition cape = bipedBody.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(30, 54).mirror().addBox(-5F, -4F, -2.5F, 10, 3, 7),
                PartPose.offset(0F, 2F, 0F));
            PartDefinition Manto1 = bipedBody.addOrReplaceChild("Manto1",
                CubeListBuilder.create().texOffs(0, 49).mirror().addBox(-5.5F, 0F, -1F, 11, 3, 1),
                PartPose.offset(0F, 0F, 3F));
            PartDefinition Manto2 = Manto1.addOrReplaceChild("Manto2",
                CubeListBuilder.create().texOffs(0, 53).mirror().addBox(-6.0F, 0F, -1F, 12, 4, 1),
                PartPose.offset(0F, 3F, 0F));
            PartDefinition Manto3 = Manto2.addOrReplaceChild("Manto3",
                CubeListBuilder.create().texOffs(0, 58).mirror().addBox(-6.5F, 0F, -1F, 13, 5, 1),
                PartPose.offset(0F, 4F, 0F));
            PartDefinition Manto4 = Manto3.addOrReplaceChild("Manto4",
                CubeListBuilder.create().texOffs(0, 58).mirror().addBox(-6.5F, 0F, -1F, 13, 5, 1),
                PartPose.offset(0F, 5F, 0F));
            PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
                PartPose.offset(0F, 3F, 0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(0, 28).mirror().addBox(-4F, -2F, -3F, 8, 3, 3),
                PartPose.offsetAndRotation(0F, 0F, 4F, 0.2094395F, 0F, 0F));
            PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0F, -3F, -1F, 1, 4, 1),
                PartPose.offsetAndRotation(3F, 0F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1F, -3F, -1F, 1, 4, 1),
                PartPose.offsetAndRotation(-3F, 0F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition MimiateRight = bipedHead.addOrReplaceChild("MimiateRight",
                CubeListBuilder.create().texOffs(0, 41).mirror().addBox(-2F, -5F, -1F, 4, 4, 1),
                PartPose.offsetAndRotation(-4F, -1F, 0F, 0F, 1.570796F, 0F));
            PartDefinition MimiateLeft = bipedHead.addOrReplaceChild("MimiateLeft",
                CubeListBuilder.create().texOffs(0, 41).mirror().addBox(-2F, -5F, -1F, 4, 4, 1),
                PartPose.offsetAndRotation(4F, -1F, 0F, 0F, -1.570796F, 0F));
            PartDefinition HearLeft1 = bipedHead.addOrReplaceChild("HearLeft1",
                CubeListBuilder.create().texOffs(24, 0).mirror().addBox(0F, -5F, -2F, 4, 2, 4),
                PartPose.offsetAndRotation(0F, -5F, 0F, 0F, 0F, 0.1745329F));
            PartDefinition HearLeft2 = HearLeft1.addOrReplaceChild("HearLeft2",
                CubeListBuilder.create().texOffs(38, 1).mirror().addBox(0F, -1F, -1.5F, 3, 3, 3),
                PartPose.offsetAndRotation(1F, -7F, 0F, 0F, 0F, 0.1745329F));
            PartDefinition HearRight1 = bipedHead.addOrReplaceChild("HearRight1",
                CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-4F, -5F, -2F, 4, 2, 4),
                PartPose.offsetAndRotation(0F, -5F, 0F, 0F, 0F, -0.1745329F));
            PartDefinition HearRight2 = HearRight1.addOrReplaceChild("HearRight2",
                CubeListBuilder.create().texOffs(38, 1).mirror().addBox(-3F, -1F, -1.5F, 3, 3, 3),
                PartPose.offsetAndRotation(-1F, -7F, 0F, 0F, 0F, -0.1745329F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        // 原版 setRotationAngles の再現（Mth.cos(limbSwing * 0.6662F)）
        final float swing = limbSwing * 0.6662F;
        final float armZ = 20 / 180F * (float)Math.PI;

        // ---- 关节枢轴（原版 setDefaultPause 的 rotationPoint 覆盖）----
        // 原版 ModelMiko 只在骑乘分支改 bipedBody.rotationPointY = -1，其余枢轴全部来自
        // setDefaultPause：body 3 / head 0 / 手 (±3, 1) / 腿 (±2, bodyHeight=9)。
        // 移植版把左腿的枢轴写成了 y=0（应 9），左腿会挂到腰上；一并按原版校正。
        bipedBody.y = 3.0F;
        bipedHead.y = 0.0F;
        bipedRightArm.x = -3.0F;
        bipedLeftArm.x = 3.0F;
        bipedRightArm.y = 1.0F;
        bipedLeftArm.y = 1.0F;
        bipedRightLeg.x = -2.0F;
        bipedLeftLeg.x = 2.0F;
        bipedRightLeg.y = 9.0F;
        bipedLeftLeg.y = 9.0F;

        if (entity.isPassenger())
        {
            bipedBody.y = -1.0F;
        }

        bipedHead.yRot = netHeadYaw * (float)Math.PI / 180F;
        bipedHead.xRot = headPitch * (float)Math.PI / 180F;

        bipedBody.xRot = 0.0F;
        bipedBody.yRot = 0.0F;
        bipedBody.zRot = 0.0F;

        bipedRightArm.xRot = Mth.cos(swing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        bipedLeftArm.xRot  = Mth.cos(swing) * 2.0F * limbSwingAmount * 0.5F;
        bipedRightArm.zRot = armZ;
        bipedLeftArm.zRot = -armZ;
        bipedRightArm.yRot = 90F / 180F * (float)Math.PI;
        bipedLeftArm.yRot = 90F / 180F * (float)Math.PI;

        bipedRightLeg.xRot = Mth.cos(swing) * 0.5F * limbSwingAmount;
        bipedLeftLeg.xRot  = Mth.cos(swing + (float)Math.PI) * 0.5F * limbSwingAmount;
        bipedRightLeg.yRot = bipedLeftLeg.yRot = bipedRightLeg.zRot = bipedLeftLeg.zRot = 0.0F;

        skirtTop.xRot = 0.0F;
        skirtBottom.xRot = 0.0F;

        // 披风（マント）：原版 ModelMiko 的 4 片用 sin 摇摆
        manto1.xRot = Mth.sin((limbSwing + ageInTicks) / 10.0F) * 0.1F + 0.3F;
        manto2.xRot = Mth.sin((limbSwing * 1.2F + ageInTicks) / 7.8F) * 0.25F + 0.25F;
        manto3.xRot = Mth.sin((limbSwing * 1.5F + ageInTicks) / 5.3F) * 0.3F + 0.20F;
        manto4.xRot = Mth.sin((limbSwing * 2.0F + ageInTicks) / 4.1F) * 0.34F + 0.0F;

        if (entity.isCrouching())
        {
            bipedBody.yRot = Mth.cos(swing + (float)Math.PI) * 2.4F * limbSwingAmount;
            skirtTop.yRot = bipedBody.yRot;
            bipedRightArm.xRot = Mth.cos(swing + (float)Math.PI) * 1.4F * limbSwingAmount;
            bipedLeftArm.xRot = Mth.cos(swing) * 1.4F * limbSwingAmount;
            bipedHead.xRot -= 0.5F;
        }
        else
        {
            skirtTop.yRot = 0.0F;
        }

        if (entity.isPassenger())
        {
            bipedRightArm.xRot += -(float)Math.PI / 5F;
            bipedLeftArm.xRot += -(float)Math.PI / 5F;
            bipedRightLeg.xRot = -(float)Math.PI * 2F / 5F;
            bipedLeftLeg.xRot = -(float)Math.PI * 2F / 5F;
            bipedRightLeg.yRot = (float)Math.PI / 14F;
            bipedLeftLeg.yRot = -(float)Math.PI / 14F;
            bipedRightLeg.zRot = (float)Math.PI / 14F;
            bipedLeftLeg.zRot = -(float)Math.PI / 14F;
            skirtTop.xRot = -(float)Math.PI / 20;
            skirtBottom.xRot = -(float)Math.PI / 30;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
