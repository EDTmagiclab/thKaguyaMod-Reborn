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

/** 自动生成自原版 ModelToziko（Techne → LayerDefinition） */
public class ModelToziko<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelToziko(ModelPart root)
    {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition Body = root.addOrReplaceChild("Body",
            CubeListBuilder.create().texOffs(32, 6).mirror().addBox(-3F, 0F, -2F, 6, 9, 4),
            PartPose.offset(0F, 2F, 0F));
        PartDefinition RightArm = root.addOrReplaceChild("RightArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-2F, -1F, -1F, 2, 8, 2),
            PartPose.offsetAndRotation(-3F, 3F, 0F, -1.570796F, 0F, 0F));
        PartDefinition LeftArm = root.addOrReplaceChild("LeftArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(0F, -1F, -1F, 2, 8, 2),
            PartPose.offsetAndRotation(3F, 3F, 0F, -1.570796F, 0F, 0F));
        PartDefinition Leg1 = root.addOrReplaceChild("Leg1",
            CubeListBuilder.create().texOffs(32, 17).mirror().addBox(-4F, 0F, -4F, 8, 7, 8),
            PartPose.offset(0F, 12F, 0F));
        PartDefinition Leg2 = root.addOrReplaceChild("Leg2",
            CubeListBuilder.create().texOffs(32, 17).mirror().addBox(-3F, 0F, -3F, 6, 4, 6),
            PartPose.offset(0F, 17F, 0F));
        PartDefinition Leg3 = root.addOrReplaceChild("Leg3",
            CubeListBuilder.create().texOffs(32, 17).mirror().addBox(-2F, 0F, -2F, 4, 4, 4),
            PartPose.offset(0F, 20F, 0F));
        PartDefinition SkirtTop = root.addOrReplaceChild("SkirtTop",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, 0F, -4F, 8, 4, 8),
            PartPose.offset(0F, 8F, 0F));
        PartDefinition SkirtBottom = root.addOrReplaceChild("SkirtBottom",
            CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-5F, 0F, -5F, 10, 7, 10),
            PartPose.offset(0F, 12F, 0F));
        PartDefinition HeadBase = root.addOrReplaceChild("HeadBase",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
            PartPose.offset(0F, 2F, 0F));
            PartDefinition BackHair = HeadBase.addOrReplaceChild("BackHair",
                CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-4F, -2F, -3F, 8, 2, 3),
                PartPose.offset(0F, 2F, 4F));
            PartDefinition LeftHair = HeadBase.addOrReplaceChild("LeftHair",
                CubeListBuilder.create().texOffs(11, 56).mirror().addBox(-4F, -2F, 0F, 8, 5, 1),
                PartPose.offsetAndRotation(5F, -3F, 0F, 0F, -1.570796F, 0F));
            PartDefinition RightHair = HeadBase.addOrReplaceChild("RightHair",
                CubeListBuilder.create().texOffs(11, 56).mirror().addBox(-4F, -2F, 0F, 8, 5, 1),
                PartPose.offsetAndRotation(-5F, -3F, 0F, 0F, 1.570796F, 0F));
            PartDefinition Cup = HeadBase.addOrReplaceChild("Cup",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2F, -8F, -2F, 4, 6, 4),
                PartPose.offset(0F, -6F, 0F));
            PartDefinition CupLeft = Cup.addOrReplaceChild("CupLeft",
                CubeListBuilder.create().texOffs(0, 42).mirror().addBox(0F, 4F, 0F, 4, 1, 1),
                PartPose.offset(2F, -7F, 0F));
            PartDefinition CupRight = Cup.addOrReplaceChild("CupRight",
                CubeListBuilder.create().texOffs(0, 42).mirror().addBox(-4F, 4F, 0F, 4, 1, 1),
                PartPose.offset(-2F, -7F, 0F));
            PartDefinition CupRibbon = Cup.addOrReplaceChild("CupRibbon",
                CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-2F, 2F, 0F, 5, 4, 1),
                PartPose.offsetAndRotation(2F, -7F, 1F, 0F, -0.4833219F, -0.5205006F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}