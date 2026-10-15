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

/** 自动生成自原版 ModelYuukaParasol（Techne → LayerDefinition） */
public class ModelYuukaParasol<T extends Entity> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelYuukaParasol(ModelPart root)
    {
        this.root = root;
    }

public static LayerDefinition createBodyLayer()
{
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    PartDefinition stick_0 = root.addOrReplaceChild("stick_0",
        CubeListBuilder.create().texOffs(0, 0).addBox(-2, 0, -2, 4, 28, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition stick_1 = root.addOrReplaceChild("stick_1",
        CubeListBuilder.create().texOffs(0, 0).addBox(-2, 28, -2, 4, 28, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition stick_2 = root.addOrReplaceChild("stick_2",
        CubeListBuilder.create().texOffs(0, 0).addBox(-2, 56, -2, 4, 28, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition stick_3 = root.addOrReplaceChild("stick_3",
        CubeListBuilder.create().texOffs(0, 0).addBox(-2, 84, -2, 4, 28, 4),
        PartPose.offset(0, 0, 0));
    PartDefinition handle_0 = root.addOrReplaceChild("handle_0",
        CubeListBuilder.create().texOffs(32, 16).addBox(-3, -10, -3, 6, 10, 6),
        PartPose.offset(0, 0, 0));
    PartDefinition handle_1 = root.addOrReplaceChild("handle_1",
        CubeListBuilder.create().texOffs(32, 16).addBox(0, 0, -6, 6, 10, 6),
        PartPose.offsetAndRotation(-3, -10, -3, 2.0944F, 0, 0));
    PartDefinition handle_2 = root.addOrReplaceChild("handle_2",
        CubeListBuilder.create().texOffs(32, 16).addBox(0, 0, -6, 6, 10, 6),
        PartPose.offsetAndRotation(-3, -15, 5.66025F, 1.0472F, 0, 0));

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