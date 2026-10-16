package thKaguyaMod.client.model.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/** 自动生成自原版 ModelHisou（Techne → LayerDefinition） */
public class ModelHisou<T extends Entity> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelHisou(ModelPart root)
    {
        this.root = root;
    }

public static LayerDefinition createBodyLayer()
{
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    PartDefinition hisou_0 = root.addOrReplaceChild("hisou_0",
        CubeListBuilder.create().texOffs(32, 0).addBox(-2, -12, -2, 4, 8, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition hisou_1 = root.addOrReplaceChild("hisou_1",
        CubeListBuilder.create().texOffs(0, 0).addBox(-2, -4, -2, 4, 28, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition hisou_2 = root.addOrReplaceChild("hisou_2",
        CubeListBuilder.create().texOffs(32, 16).addBox(-2, -20, 0, 12, 12, 0),
        PartPose.offset(0, 0, 0));

    // 原版 HisouTexture.png 64×32（Techne 默认尺寸），LayerDefinition 必须 64×32
    // 否则贴图 UV 纵向拉伸导致模型显示错乱（剑刃与把手错位）
    return LayerDefinition.create(mesh, 64, 32);
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