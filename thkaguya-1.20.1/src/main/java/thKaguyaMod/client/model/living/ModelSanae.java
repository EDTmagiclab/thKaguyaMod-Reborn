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

/** 自动生成自原版 ModelSanae（Techne → LayerDefinition） */
public class ModelSanae<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart bipedHead;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;

    public ModelSanae(ModelPart root)
    {
        this.root = root;
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
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 0F, -2F, 6, 9, 4),
            PartPose.offset(0F, 2F, 0F));
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(0F, 0F, -1F, 4, 7, 2),
            PartPose.offset(-3F, 2F, 0F));
            PartDefinition rightHand = bipedRightArm.addOrReplaceChild("rightHand",
                CubeListBuilder.create().texOffs(52, 9).mirror().addBox(0F, 1F, -1F, 2, 1, 2),
                PartPose.offset(-0F, 6F, 0F));
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-4F, 0F, -1F, 4, 7, 2),
            PartPose.offset(3F, 2F, 0F));
            PartDefinition leftHand = bipedLeftArm.addOrReplaceChild("leftHand",
                CubeListBuilder.create().texOffs(52, 9).mirror().addBox(-2F, 1F, -1F, 2, 1, 2),
                PartPose.offset(0F, 6F, 0F));
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
            CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-1F, 0F, -2F, 3, 13, 4),
            PartPose.offset(-2F, 11F, 0F));
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
            CubeListBuilder.create().texOffs(50, 18).mirror().addBox(-2F, 0F, -2F, 3, 13, 4),
            PartPose.offset(2F, 11F, 0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, -2F, -4F, 8, 4, 8),
                PartPose.offset(0F, 8F, 0F));
            PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-5F, -2F, -5F, 10, 8, 10),
                PartPose.offset(0F, 4F, 0F));
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -6F, -4F, 8, 8, 8),
            PartPose.offset(0F, 2F, 0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(0, 50).mirror().addBox(-4F, 1F, -3F, 8, 9, 3),
                PartPose.offsetAndRotation(0F, -1F, 4F, 0.2094395F, 0F, 0F));
            PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                CubeListBuilder.create().texOffs(4, 32).mirror().addBox(0F,  1F, -1F, 1, 5, 1),
                PartPose.offsetAndRotation(3F, -3F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1F, 1F, -1F, 1, 5, 1),
                PartPose.offsetAndRotation(-3F, -3F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition leftBraid2 = bipedHead.addOrReplaceChild("leftBraid2",
                CubeListBuilder.create().texOffs(8, 31).mirror().addBox(-1F, 2F, -1F, 2, 5, 2),
                PartPose.offsetAndRotation(3F, 0F, -4F, -0.2443461F, 0F, 0F));
            PartDefinition kaeruBase = bipedHead.addOrReplaceChild("kaeruBase",
                CubeListBuilder.create().texOffs(0, 38).mirror().addBox(-2F,  1F, -1F, 3, 2, 1),
                PartPose.offsetAndRotation(3F, -7F, -4F, 0F, 0F, 0.3839724F));
            PartDefinition kaeruEyeRight = bipedHead.addOrReplaceChild("kaeruEyeRight",
                CubeListBuilder.create().texOffs(8, 38).mirror().addBox(-1F, 2F, -1F, 1, 1, 1),
                PartPose.offsetAndRotation(2.7F, -9F, -4F, 0F, 0F, 0.3839724F));
            PartDefinition kaeruEyeLeft = bipedHead.addOrReplaceChild("kaeruEyeLeft",
                CubeListBuilder.create().texOffs(8, 38).mirror().addBox(0F, 2F, -1F, 1, 1, 1),
                PartPose.offsetAndRotation(3.7F, -8.7F, -4F, 0F, 0F, 0.3839724F));

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