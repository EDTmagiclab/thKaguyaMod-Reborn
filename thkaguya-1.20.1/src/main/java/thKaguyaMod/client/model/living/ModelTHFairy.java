package thKaguyaMod.client.model.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.living.EntityTHFairy;

/**
 * 妖精模型（1.20.1 移植版）
 * 原版 Techne 生成模型（ModelBase/ModelRenderer），迁移为 LayerDefinition 体系。
 * 头/身体/手臂/腿/裙子/长发/蝴蝶结/花/翅膀，完整保留。
 */
public class ModelTHFairy extends EntityModel<EntityTHFairy>
{
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(THKaguyaCore.MODID, "th_fairy"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart rightLeg;
    private final ModelPart leftArm;
    private final ModelPart leftLeg;
    private final ModelPart skirt;
    private final ModelPart rightWing;
    private final ModelPart rightWing2;
    private final ModelPart leftWing;
    private final ModelPart leftWing2;

    public ModelTHFairy(ModelPart root)
    {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("rightArm");
        this.rightLeg = root.getChild("rightLeg");
        this.leftArm = root.getChild("leftArm");
        this.leftLeg = root.getChild("leftLeg");
        this.skirt = root.getChild("skirt");
        this.rightWing = root.getChild("rightWing");
        this.rightWing2 = root.getChild("rightWing2");
        this.leftWing = root.getChild("leftWing");
        this.leftWing2 = root.getChild("leftWing2");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8, 8, 8),
                PartPose.offset(0F, 7F, 0F));
        // 长发
        head.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(24, 0).addBox(-4F, -7F, -3F, 8, 5, 3),
                PartPose.offset(0F, 7F, 4F));
        // 蝴蝶结
        head.addOrReplaceChild("ribbonCenter",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1F, -1F, 0F, 2, 2, 1),
                PartPose.offsetAndRotation(0F, -8F, 4F, 0.2443461F, 0F, 0F));
        head.addOrReplaceChild("ribbonLeft",
                CubeListBuilder.create().texOffs(6, 32).addBox(0F, -2F, 0F, 5, 4, 1),
                PartPose.offset(1F, -8F, 4F));
        head.addOrReplaceChild("ribbonRight",
                CubeListBuilder.create().texOffs(6, 32).addBox(-5F, -2F, 0F, 5, 4, 1),
                PartPose.offset(-1F, -8F, 4F));
        // 花
        head.addOrReplaceChild("flowerCenter",
                CubeListBuilder.create().texOffs(18, 32).addBox(-1F, -1F, -1F, 2, 2, 1),
                PartPose.offsetAndRotation(3F, -8F, -4F, -0.3490659F, 0F, 0F));
        head.addOrReplaceChild("flower1",
                CubeListBuilder.create().texOffs(24, 32).addBox(1F, -1F, -1F, 1, 2, 1),
                PartPose.offsetAndRotation(3F, -8F, -4F, -0.3490659F, 0F, 0F));
        head.addOrReplaceChild("flower2",
                CubeListBuilder.create().texOffs(24, 32).addBox(-2F, -1F, -1F, 1, 2, 1),
                PartPose.offsetAndRotation(3F, -8F, -4F, -0.3490659F, 0F, 0F));
        head.addOrReplaceChild("flower3",
                CubeListBuilder.create().texOffs(24, 32).addBox(-1F, -2F, 0F, 2, 1, 1),
                PartPose.offsetAndRotation(3F, -8F, -5F, -0.3490659F, 0F, 0F));
        head.addOrReplaceChild("flower4",
                CubeListBuilder.create().texOffs(24, 32).addBox(-1F, 1F, -1F, 2, 1, 1),
                PartPose.offsetAndRotation(3F, -8F, -4F, -0.3490659F, 0F, 0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(32, 8).addBox(-3F, 0F, -2F, 6, 7, 4),
                PartPose.offset(0F, 7F, 0F));
        root.addOrReplaceChild("rightArm",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, -1F, -1F, 2, 8, 2),
                PartPose.offsetAndRotation(-4F, 8F, 0F, -0.7679449F, 0F, -0.6457718F));
        root.addOrReplaceChild("rightLeg",
                CubeListBuilder.create().texOffs(32, 19).addBox(-1F, 0F, -2F, 3, 9, 4),
                PartPose.offset(-2F, 14F, 0F));
        root.addOrReplaceChild("leftArm",
                CubeListBuilder.create().texOffs(56, 0).addBox(-1F, -1F, -1F, 2, 8, 2),
                PartPose.offsetAndRotation(4F, 8F, 0F, -0.7679449F, 0F, 0.6457718F));
        root.addOrReplaceChild("leftLeg",
                CubeListBuilder.create().texOffs(32, 19).addBox(-2F, 0F, -2F, 3, 9, 4),
                PartPose.offset(2F, 14F, 0F));
        root.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4F, 0F, -4F, 8, 8, 8),
                PartPose.offset(0F, 12F, 0F));

        root.addOrReplaceChild("rightWing2",
                CubeListBuilder.create().texOffs(0, 48).addBox(-14F, 0F, 0F, 14, 10, 1),
                PartPose.offsetAndRotation(-1F, 7F, 2F, 0F, 0.4886922F, -0.3141593F));
        root.addOrReplaceChild("leftWing",
                CubeListBuilder.create().texOffs(0, 48).addBox(0F, 0F, 0F, 14, 10, 1),
                PartPose.offsetAndRotation(0F, 4F, 2F, 0F, -0.4886922F, -0.3141593F));
        root.addOrReplaceChild("leftWing2",
                CubeListBuilder.create().texOffs(0, 48).addBox(0F, 0F, 0F, 14, 10, 1),
                PartPose.offsetAndRotation(1F, 7F, 2F, 0F, -0.4886922F, 0.3141593F));
        root.addOrReplaceChild("rightWing",
                CubeListBuilder.create().texOffs(0, 48).addBox(-14F, 0F, 0F, 14, 10, 1),
                PartPose.offsetAndRotation(0F, 4F, 2F, 0F, 0.4886922F, 0.3141593F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EntityTHFairy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);
        this.body.yRot = Mth.sin(Mth.sqrt(limbSwing) * (float)Math.PI * 2.0F) * 0.2F;
        this.skirt.xRot = 0F;

        // 飞行时翅膀扇动
        if(entity.isAlive())
        {
            this.rightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F;
            this.leftWing.yRot = -this.rightWing.yRot;
            this.rightWing2.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F;
            this.leftWing2.yRot = -this.rightWing2.yRot;
        }
        else
        {
            this.rightWing.yRot = -25F;
            this.leftWing.yRot = 25F;
            this.rightWing2.yRot = -25F;
            this.leftWing2.yRot = 25F;
        }

        // 腿/手臂摆动
        this.rightLeg.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount;
        this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
        this.rightArm.zRot = -0.6457718F;
        this.leftArm.xRot = this.rightArm.xRot;
        this.leftArm.zRot = 0.6457718F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        skirt.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightWing2.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftWing2.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
