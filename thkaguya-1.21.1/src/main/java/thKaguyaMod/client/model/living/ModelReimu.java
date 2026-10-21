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

/** 自动生成自原版 ModelReimu（Techne → LayerDefinition） */
public class ModelReimu<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart bipedHead;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;
    private final ModelPart skirtTop;
    private final ModelPart skirtBottom;

    public ModelReimu(ModelPart root)
    {
        this.root = root;
        this.bipedHead = root.getChild("bipedBody").getChild("bipedHead");
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedBody").getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedBody").getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedBody").getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedBody").getChild("bipedLeftLeg");
        this.skirtTop = root.getChild("bipedBody").getChild("skirtTop");
        this.skirtBottom = this.skirtTop.getChild("skirtBottom");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, -1F, -2F, 6, 9, 4),
            PartPose.offset(0F, 3F, 0F));
            PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1F, 0F, -1F, 4, 7, 2),
                PartPose.offset(-3F, 3F, 0F));
            PartDefinition handRight = bipedRightArm.addOrReplaceChild("handRight",
                CubeListBuilder.create().texOffs(52, 9).mirror().addBox(-1F, 1F, -1F, 2, 1, 2),
                PartPose.offset(-0F, 6F, 0F));
            PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-3F, 0F, -1F, 4, 7, 2),
                PartPose.offset(3F, 3F, 0F));
            PartDefinition handLeft = bipedLeftArm.addOrReplaceChild("handLeft",
                CubeListBuilder.create().texOffs(52, 9).mirror().addBox(-1F, 1F, -1F, 2, 1, 2),
                PartPose.offset(0F, 6F, 0F));
            PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-1F, 0F, -2F, 3, 12, 4),
                PartPose.offset(-2F, 12F, 0F));
            PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-2F, 0F, -2F, 3, 12, 4),
                PartPose.offset(2F, 12F, 0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, 0F, -4F, 8, 4, 8),
                PartPose.offset(0F, 9F, 0F));
            PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                CubeListBuilder.create().texOffs(16, 33).mirror().addBox(-5F, 0F, -5F, 10, 7, 10),
                PartPose.offset(0F, 13F, 0F));
            PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
                PartPose.offset(0F, 3F, 0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(0, 50).mirror().addBox(-4F, -2F, -3F, 8, 9, 3),
                PartPose.offsetAndRotation(0F, 0F, 4F, 0.2094395F, 0F, 0F));
            PartDefinition braidLeft = bipedHead.addOrReplaceChild("braidLeft",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0F, -3F, -2F, 1, 6, 1),
                PartPose.offsetAndRotation(3F, 0F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition braidRight = bipedHead.addOrReplaceChild("braidRight",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1F, -3F, -2F, 1, 6, 1),
                PartPose.offsetAndRotation(-3F, 0F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition leftRibbon = bipedHead.addOrReplaceChild("leftRibbon",
                CubeListBuilder.create().texOffs(32, 56).mirror().addBox(1F, -6F, 1F, 8, 7, 1),
                PartPose.offsetAndRotation(0F, -3F, 4F, 0.4461433F, 0F, -0.4089647F));
            PartDefinition rightRibbon = bipedHead.addOrReplaceChild("rightRibbon",
                CubeListBuilder.create().texOffs(32, 56).mirror().addBox(1F, -6F, -2F, 8, 7, 1),
                PartPose.offsetAndRotation(0F, -3F, 4F, -0.4461411F, 3.141593F, -0.4089647F + 3.141593F / 4.0F));
            PartDefinition rightRibbon2 = bipedHead.addOrReplaceChild("rightRibbon2",
                CubeListBuilder.create().texOffs(32, 56).mirror().addBox(-1F, -5F, -1F, 8, 3, 1),
                PartPose.offsetAndRotation(0F, -3F, 5F, -0.260248F, 3.141593F, -0.7063936F));
            PartDefinition leftRibbon2 = bipedHead.addOrReplaceChild("leftRibbon2",
                CubeListBuilder.create().texOffs(32, 56).mirror().addBox(-1F, -5F, 0F, 8, 3, 1),
                PartPose.offsetAndRotation(0F, -3F, 5F, 0.260246F, 0F, 0.7063936F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        // 原版 setRotationAngles の再現（Mth.cos(limbSwing * 0.6662F)）
        final float swing = limbSwing * 0.6662F;
        final float armZ = 30 / 180F * (float)Math.PI;

        // ---- 关节枢轴（原版 setDefaultPause + setRotationAngles 的 rotationPoint 覆盖）----
        // 1.7.10 每帧先 setDefaultPause() 改一遍枢轴，setRotationAngles 再逐个覆盖；
        // 移植时只搬了旋转角，枢轴一直停在 Techne 构造器的初值上：
        //   裙腰 9（应 5）、裙摆 13（应 4，相对裙腰）、腿 12（应 7）、头/手 3（应 -1）。
        // 于是裙摆比原版低了 11 像素，悬停到高处时看起来像“裙子掉到脚上”。此处逐值复刻。
        bipedBody.y = 5.0F;              // setDefaultPause(-5) → setRotationAngles 5
        bipedHead.y = -1.0F;             // setDefaultPause(5)  → setRotationAngles -1
        bipedRightArm.y = -1.0F;         // setDefaultPause(6)  → setRotationAngles -1
        bipedLeftArm.y = -1.0F;
        bipedRightArm.x = -3.0F;
        bipedLeftArm.x = 3.0F;
        bipedRightLeg.x = -2.0F;
        bipedLeftLeg.x = 2.0F;

        if (entity.isPassenger())
        {
            bipedBody.y = -1.0F;
            bipedRightLeg.y = 5.0F;      // 骑行：腿收到 5
            bipedLeftLeg.y = 5.0F;
        }
        else
        {
            bipedRightLeg.y = 7.0F;      // 站立：腿放到 7
            bipedLeftLeg.y = 7.0F;
        }

        // 裙腰 / 裙摆：原版 if / else 两个分支都写 5 与 4，值相同
        skirtTop.y = 5.0F;
        skirtTop.z = 0.0F;
        skirtBottom.y = 4.0F;
        skirtBottom.z = 0.0F;

        bipedHead.yRot = netHeadYaw * (float)Math.PI / 180F;
        bipedHead.xRot = headPitch * (float)Math.PI / 180F;

        bipedBody.xRot = 0.0F;
        bipedBody.yRot = 0.0F;
        bipedBody.zRot = 0.0F;

        bipedRightArm.xRot = Mth.cos(swing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        bipedLeftArm.xRot  = Mth.cos(swing) * 2.0F * limbSwingAmount * 0.5F;
        bipedRightArm.zRot = armZ;
        bipedLeftArm.zRot = -armZ;
        bipedRightArm.yRot = 0.0F;
        bipedLeftArm.yRot = 0.0F;

        bipedRightLeg.xRot = Mth.cos(swing) * 0.5F * limbSwingAmount;
        bipedLeftLeg.xRot  = Mth.cos(swing + (float)Math.PI) * 0.5F * limbSwingAmount;
        bipedRightLeg.yRot = bipedLeftLeg.yRot = bipedRightLeg.zRot = bipedLeftLeg.zRot = 0.0F;

        skirtTop.xRot = 0.0F;
        skirtBottom.xRot = 0.0F;

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
            skirtTop.xRot = -(float)Math.PI / 5;
            skirtBottom.xRot = -(float)Math.PI / 4;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
