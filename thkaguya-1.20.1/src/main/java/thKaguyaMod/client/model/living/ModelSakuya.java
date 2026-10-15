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

/** 自动生成自原版 ModelSakuya（Techne → LayerDefinition） */
public class ModelSakuya<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart bipedHead;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;

    public ModelSakuya(ModelPart root)
    {
        this.root = root;
        this.bipedHead = root.getChild("bipedBody").getChild("bipedHead");
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedBody").getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedBody").getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedBody").getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedBody").getChild("bipedLeftLeg");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 0F, -2F, 6, 9, 4),
            PartPose.offset(0F, 1F, 0F));
            PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1F, -1F, -1F, 2, 8, 2),
                PartPose.offsetAndRotation(-4F, 2F, 0F, -0.7679449F, 0F, -0.6457718F));
            PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1F, -1F, -1F, 2, 8, 2),
                PartPose.offsetAndRotation(4F, 2F, 0F, -0.7679449F, 0F, 0.6457718F));
            PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-1F, 0F, -2F, 3, 14, 4),
                PartPose.offset(-2F, 10F, 0F));
            PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-2F, 0F, -2F, 3, 14, 4),
                PartPose.offset(2F, 10F, 0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, 0F, -4F, 8, 4, 8),
                PartPose.offset(0F, 6F, 0F));
            PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-5F, 0F, -5F, 10, 5, 10),
                PartPose.offset(0F, 4F, 0F));
            PartDefinition leftRibbon = bipedBody.addOrReplaceChild("leftRibbon",
                CubeListBuilder.create().texOffs(0, 38).mirror().addBox(0F, 0F, 0F, 5, 4, 1),
                PartPose.offsetAndRotation(0F, 5F, 3F, 0.4886922F, -0.3F, -0.296706F));
            PartDefinition rightRibbon = bipedBody.addOrReplaceChild("rightRibbon",
                CubeListBuilder.create().texOffs(0, 38).mirror().addBox(0F, 0F, -1F, 5, 4, 1),
                PartPose.offsetAndRotation(0F, 5F, 3F, -0.4886922F, -3.141593F + 0.3F, 0.296706F));
            PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
                PartPose.offset(0F, 1F, 0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-4F, -2F, -4F, 8, 5, 3),
                PartPose.offsetAndRotation(0F, 0F, 4F, 0.2094395F, 0F, 0F));
            PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0F, -1F, -1F, 1, 5, 1),
                PartPose.offsetAndRotation(3F, -1F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1F, -1F, -1F, 1, 5, 1),
                PartPose.offsetAndRotation(-3F, -1F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition leftKatyusha = bipedHead.addOrReplaceChild("leftKatyusha",
                CubeListBuilder.create().texOffs(4, 32).mirror().addBox(0F, -1F, 0F, 4, 1, 1),
                PartPose.offsetAndRotation(0F, -8F, -5F, 0F, -0.1745329F, 0.2268928F));
            PartDefinition rightKatyusha = bipedHead.addOrReplaceChild("rightKatyusha",
                CubeListBuilder.create().texOffs(4, 32).mirror().addBox(-4F, -1F, 0F, 4, 1, 1),
                PartPose.offsetAndRotation(0F, -8F, -5F, 0F, 0.1745329F, -0.2268928F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if(bipedHead != null) { bipedHead.yRot = netHeadYaw * (float)Math.PI / 180F; bipedHead.xRot = headPitch * (float)Math.PI / 180F; }
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