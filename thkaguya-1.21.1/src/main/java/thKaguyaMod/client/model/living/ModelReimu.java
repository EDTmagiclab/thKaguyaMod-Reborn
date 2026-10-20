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

    public ModelReimu(ModelPart root)
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
        if(bipedHead != null) { bipedHead.yRot = netHeadYaw * (float)Math.PI / 180F; bipedHead.xRot = headPitch * (float)Math.PI / 180F; }
        if(bipedRightArm != null) { bipedRightArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(bipedLeftArm != null) { bipedLeftArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(bipedRightLeg != null) { bipedRightLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }
        if(bipedLeftLeg != null) { bipedLeftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
