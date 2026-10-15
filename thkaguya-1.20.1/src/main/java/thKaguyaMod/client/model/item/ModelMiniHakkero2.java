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

/** 自动生成自原版 ModelMiniHakkero2（Techne → LayerDefinition） */
public class ModelMiniHakkero2<T extends Entity> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelMiniHakkero2(ModelPart root)
    {
        this.root = root;
    }

public static LayerDefinition createBodyLayer()
{
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    PartDefinition miniHakkero_0 = root.addOrReplaceChild("miniHakkero_0",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offset(0, 0, 0));
    PartDefinition miniHakkero_1 = root.addOrReplaceChild("miniHakkero_1",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.785398F));
    PartDefinition miniHakkero_2 = root.addOrReplaceChild("miniHakkero_2",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 1.5708F));
    PartDefinition miniHakkero_3 = root.addOrReplaceChild("miniHakkero_3",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 2.35619F));
    PartDefinition miniHakkero_4 = root.addOrReplaceChild("miniHakkero_4",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 3.14159F));
    PartDefinition miniHakkero_5 = root.addOrReplaceChild("miniHakkero_5",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 3.92699F));
    PartDefinition miniHakkero_6 = root.addOrReplaceChild("miniHakkero_6",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 4.71239F));
    PartDefinition miniHakkero_7 = root.addOrReplaceChild("miniHakkero_7",
        CubeListBuilder.create().texOffs(32, 0).addBox(-4, -10, -0.001F, 8, 2, 6),
        PartPose.offsetAndRotation(0, 0, 0, 0, 0, 5.49779F));

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