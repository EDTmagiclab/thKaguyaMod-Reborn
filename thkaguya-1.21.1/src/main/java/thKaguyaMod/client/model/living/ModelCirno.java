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

/** 自动生成自原版 ModelCirno（Techne → LayerDefinition） */
public class ModelCirno<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightWing2;
    private final ModelPart leftWing2;
    private final ModelPart rightWing3;
    private final ModelPart leftWing3;

    public ModelCirno(ModelPart root)
    {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("rightArm");
        this.leftArm = root.getChild("leftArm");
        this.rightLeg = root.getChild("rightLeg");
        this.leftLeg = root.getChild("leftLeg");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
        this.rightWing2 = root.getChild("rightWing2");
        this.leftWing2 = root.getChild("leftWing2");
        this.rightWing3 = root.getChild("rightWing3");
        this.leftWing3 = root.getChild("leftWing3");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
            PartPose.offset(0F, 5F, 0F));
            PartDefinition longHair = head.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(24, 0).addBox(-4F, 0F, -3F, 8, 5, 3),
                PartPose.offset(0F, 0F, 4F));
            PartDefinition ribbonCenter = head.addOrReplaceChild("ribbonCenter",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1F, -1F, 0F, 2, 2, 1),
                PartPose.offsetAndRotation(0F, -8F, 4F, 0.2443461F, 0F, 0F));
            PartDefinition ribbonLeft = head.addOrReplaceChild("ribbonLeft",
                CubeListBuilder.create().texOffs(6, 32).addBox(0F, -2F, 0F, 5, 4, 1),
                PartPose.offsetAndRotation(1F, -8F, 4F, 0.418879F, 0F, 0F));
            PartDefinition ribbonRight = head.addOrReplaceChild("ribbonRight",
                CubeListBuilder.create().texOffs(6, 32).addBox(-5F, -2F, 0F, 5, 4, 1),
                PartPose.offset(-1F, -8F, 4F));
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 0F, -2F, 6, 7, 4),
            PartPose.offset(0F, 5F, 0F));
        PartDefinition skirt = root.addOrReplaceChild("skirt",
            CubeListBuilder.create().texOffs(0, 16).addBox(-4F, 0F, -4F, 8, 5, 8),
            PartPose.offset(0F, 10F, 0F));
            PartDefinition skirt2 = skirt.addOrReplaceChild("skirt2",
                CubeListBuilder.create().texOffs(24, 32).addBox(-5F, 0F, -5F, 10, 6, 10),
                PartPose.offset(0F, 4F, 0F));
        PartDefinition rightArm = root.addOrReplaceChild("rightArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1F, -1F, -1F, 2, 8, 2),
            PartPose.offsetAndRotation(-4F, 6F, 0F, -0.7679449F, 0F, -0.6457718F));
        PartDefinition leftArm = root.addOrReplaceChild("leftArm",
            CubeListBuilder.create().texOffs(56, 0).addBox(-1F, -1F, -1F, 2, 8, 2),
            PartPose.offsetAndRotation(4F, 6F, 0F, -0.7679449F, 0F, 0.6457718F));
        PartDefinition rightLeg = root.addOrReplaceChild("rightLeg",
            CubeListBuilder.create().texOffs(50, 16).addBox(-1F, 0F, -2F, 3, 12, 4),
            PartPose.offset(-2F, 12F, 0F));
        PartDefinition leftLeg = root.addOrReplaceChild("leftLeg",
            CubeListBuilder.create().texOffs(50, 16).addBox(-2F, 0F, -2F, 3, 12, 4),
            PartPose.offset(2F, 12F, 0F));
        PartDefinition rightWing = root.addOrReplaceChild("rightWing",
            CubeListBuilder.create().texOffs(0, 48).addBox(-14F, 0F, 0F, 14, 3, 3),
            PartPose.offsetAndRotation(-1F, 3F, 2F, 0F, 0.4886922F, 0.3141593F));
        PartDefinition leftWing = root.addOrReplaceChild("leftWing",
            CubeListBuilder.create().texOffs(0, 48).addBox(0F, 0F, 0F, 14, 3, 3),
            PartPose.offsetAndRotation(0F, 3F, 2F, 0F, -0.4886922F, -0.3141593F));
        PartDefinition rightWing2 = root.addOrReplaceChild("rightWing2",
            CubeListBuilder.create().texOffs(0, 48).addBox(-14F, 0F, 0F, 14, 3, 3),
            PartPose.offsetAndRotation(-1F, 6F, 2F, 0F, 0.4886922F, -0.3141593F));
        PartDefinition leftWing2 = root.addOrReplaceChild("leftWing2",
            CubeListBuilder.create().texOffs(0, 48).addBox(0F, 0F, 0F, 14, 3, 3),
            PartPose.offsetAndRotation(1F, 6F, 2F, 0F, -0.4886922F, 0.3141593F));
        PartDefinition rightWing3 = root.addOrReplaceChild("rightWing3",
            CubeListBuilder.create().texOffs(0, 48).addBox(-14F, 0F, 0F, 14, 3, 3),
            PartPose.offsetAndRotation(-1F, 5F, 2F, 0F, 0.4886922F, 0F));
        PartDefinition leftWing3 = root.addOrReplaceChild("leftWing3",
            CubeListBuilder.create().texOffs(0, 48).addBox(0F, 0F, 0F, 14, 3, 3),
            PartPose.offsetAndRotation(1F, 5F, 2F, 0F, -0.4886922F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if(head != null) { head.yRot = netHeadYaw * (float)Math.PI / 180F; head.xRot = headPitch * (float)Math.PI / 180F; }
        if(rightWing != null) { rightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(leftWing != null) { leftWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(rightWing2 != null) { rightWing2.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(leftWing2 != null) { leftWing2.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(rightWing3 != null) { rightWing3.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(leftWing3 != null) { leftWing3.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }
        if(rightArm != null) { rightArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(leftArm != null) { leftArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(rightLeg != null) { rightLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }
        if(leftLeg != null) { leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }

        // スペルカードの宣言をする（原版 ModelCirno#setRotationAngles L223-235 の移植）
        byte spellCardMotion = entity.getSpellCardMotion();
        if(rightArm != null)
        {
            if(spellCardMotion < -15)
            {
                rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(30F + spellCardMotion) / 180F * (float)Math.PI) * 4F;
            }
            else if(spellCardMotion < 0)
            {
                rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(-spellCardMotion) / 180F * (float)Math.PI) * 4F;
            }
            else
            {
                rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                if(leftArm != null) { leftArm.xRot = rightArm.xRot; }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
