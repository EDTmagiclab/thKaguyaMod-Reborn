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

/** 自动生成自原版 ModelPrivateSquare（Techne → LayerDefinition） */
public class ModelPrivateSquare<T extends Entity> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelPrivateSquare(ModelPart root)
    {
        this.root = root;
    }

public static LayerDefinition createBodyLayer()
{
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    PartDefinition watchBase = root.addOrReplaceChild("watchBase",
        CubeListBuilder.create().texOffs(0, 0).addBox(-6, -6, -2, 12, 12, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition watchCenter = root.addOrReplaceChild("watchCenter",
        CubeListBuilder.create().texOffs(28, 14).addBox(-8, -8, -1, 16, 16, 2),
        PartPose.offset(0, 0, 0));
    PartDefinition watchHandle = root.addOrReplaceChild("watchHandle",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, 8, 0, 8, 6, 0),
        PartPose.offset(0, 0, 0));
    PartDefinition watchCover = root.addOrReplaceChild("watchCover",
        CubeListBuilder.create().texOffs(48, 16).addBox(-8, -8, 0, 16, 16, 0),
        PartPose.offsetAndRotation(0, -16, -4, 0.523599F, 0, 0));

    // 原版 SakuyaStopWatchTexture.png / SakuyaWatchTexture.png 都是 64×32（Techne 默认）
    // LayerDefinition 必须 64×32，否则钟表表面 UV 错位出现\"破碎\"感
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