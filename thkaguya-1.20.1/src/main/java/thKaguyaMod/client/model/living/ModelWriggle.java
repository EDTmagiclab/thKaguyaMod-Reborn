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

/** 自动生成自原版 ModelWriggle（Techne → LayerDefinition） */
public class ModelWriggle<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart bipedHead;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;

    public ModelWriggle(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.bipedHead = bipedBody.getChild("bipedHead");
        this.bipedRightArm = bipedBody.getChild("bipedRightArm");
        this.bipedLeftArm = bipedBody.getChild("bipedLeftArm");
        this.bipedRightLeg = bipedBody.getChild("bipedRightLeg");
        this.bipedLeftLeg = bipedBody.getChild("bipedLeftLeg");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 0F, -2F, 6, 7, 4),
            PartPose.offset(0F, 5F, 0F));
        PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
            CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-2F, -1F, -1F, 2, 8, 2),
            PartPose.offset(-2F, 6F, 0F));
        PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
            CubeListBuilder.create().texOffs(56, 0).mirror().addBox(0F, -1F, -1F, 2, 8, 2),
            PartPose.offset(2F, 6F, 0F));
        PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
            CubeListBuilder.create().texOffs(32, 19).mirror().addBox(-1F, 0F, -2F, 3, 11, 4),
            PartPose.offset(-2F, 13F, 0F));
        PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
            CubeListBuilder.create().texOffs(32, 19).mirror().addBox(-2F, 0F, -2F, 3, 11, 4),
            PartPose.offset(2F, 13F, 0F));
        PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
            PartPose.offset(0F, 5F, 0F));
        PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
            CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-4F, 0F, -3F, 8, 2, 3),
            PartPose.offset(0F, 0F, 4F));
        PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0F, 0F, -1F, 1, 4, 1),
            PartPose.offsetAndRotation(2.95F, -3F, -3F, -0.2443461F, 0F, 0F));
        PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1F, 0F, -1F, 1, 4, 1),
            PartPose.offsetAndRotation(-2.95F, -3F, -3F, -0.2443461F, 0F, 0F));
            PartDefinition rightTouch = bipedHead.addOrReplaceChild("rightTouch",
                CubeListBuilder.create().texOffs(4, 16).mirror().addBox(0F, -6F, 0F, 1, 6, 1),
                PartPose.offset(-3F, -8F, -4F));
            PartDefinition leftTouch = bipedHead.addOrReplaceChild("leftTouch",
                CubeListBuilder.create().texOffs(4, 16).mirror().addBox(-1F, -6F, 0F, 1, 6, 1),
                PartPose.offset(3F, -8F, -4F));
            PartDefinition Manto1 = bipedBody.addOrReplaceChild("Manto1",
                CubeListBuilder.create().texOffs(38, 47).mirror().addBox(-6F, 0F, -1F, 12, 6, 1),
                PartPose.offset(0F, 0F, 2F));
            PartDefinition Manto2 = Manto1.addOrReplaceChild("Manto2",
                CubeListBuilder.create().texOffs(38, 54).mirror().addBox(-6F, 0F, -1F, 12, 4, 1),
                PartPose.offset(0F, 6F, 0F));
            PartDefinition Manto3 = Manto2.addOrReplaceChild("Manto3",
                CubeListBuilder.create().texOffs(38, 59).mirror().addBox(-6F, 0F, -1F, 12, 4, 1),
                PartPose.offset(0F, 4F, 0F));
            PartDefinition rightZubon = bipedRightLeg.addOrReplaceChild("rightZubon",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2F, 0F, -2.5F, 4, 6, 5),
                PartPose.offset(0F, -2.5F, 0F));
            PartDefinition leftZubon = bipedLeftLeg.addOrReplaceChild("leftZubon",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2F, 0F, -2.5F, 4, 6, 5),
                PartPose.offset(0F, -2.5F, 0F));

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