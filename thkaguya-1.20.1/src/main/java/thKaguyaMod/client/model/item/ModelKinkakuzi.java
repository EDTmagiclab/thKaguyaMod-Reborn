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

/** 自动生成自原版 ModelKinkakuzi（Techne → LayerDefinition） */
public class ModelKinkakuzi<T extends Entity> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelKinkakuzi(ModelPart root)
    {
        this.root = root;
    }

public static LayerDefinition createBodyLayer()
{
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    PartDefinition kinkakuzi = root.addOrReplaceChild("kinkakuzi",
        CubeListBuilder.create().texOffs(0, 0).addBox(-15, -15, -2, 30, 30, 2),
        PartPose.offsetAndRotation(0, 0, 0, 1.5708F, 0, 0));

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