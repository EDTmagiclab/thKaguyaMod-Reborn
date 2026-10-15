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

/** 自动生成自原版 ModelCursedDecoyDoll（Techne → LayerDefinition） */
public class ModelCursedDecoyDoll<T extends Entity> extends EntityModel<T>
{
    private final ModelPart root;

    public ModelCursedDecoyDoll(ModelPart root)
    {
        this.root = root;
    }

public static LayerDefinition createBodyLayer()
{
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
        CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3, 0, -2, 6, 6, 4),
        PartPose.offset(0, 10, 0));
    PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
        CubeListBuilder.create().texOffs(32, 0).mirror().addBox(0, -1, -1, 4, 6, 2),
        PartPose.offsetAndRotation(-4, 11, 0, 0, 0, 0.523599F));
    PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
        CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-4, -1, -1, 4, 6, 2),
        PartPose.offsetAndRotation(4, 11, 0, 0, 0, -0.523599F));
    PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
        CubeListBuilder.create().texOffs(36, 22).mirror().addBox(-1, 0, -2, 3, 7, 4),
        PartPose.offset(-2, 17, 0));
    PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
        CubeListBuilder.create().texOffs(36, 22).mirror().addBox(-2, 0, -2, 3, 7, 4),
        PartPose.offset(2, 17, 0));
    PartDefinition skirtTop = root.addOrReplaceChild("skirtTop",
        CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4, 0, -5, 8, 7, 6),
        PartPose.offset(0, 16, 2));
    PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
        CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4, -8, -4, 8, 8, 8),
        PartPose.offset(0, 10, 0));
    PartDefinition longHair = root.addOrReplaceChild("longHair",
        CubeListBuilder.create().texOffs(0, 49).mirror().addBox(-5, 0, -5, 10, 7, 8),
        PartPose.offset(0, 4, 2));
    PartDefinition leftBraid = root.addOrReplaceChild("leftBraid",
        CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0, 0, -1, 1, 6, 1),
        PartPose.offsetAndRotation(3, 5, -3, -0.244346F, 0, 0));
    PartDefinition rightBraid = root.addOrReplaceChild("rightBraid",
        CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1, 0, -1, 1, 6, 1),
        PartPose.offsetAndRotation(-3, 5, -3, -0.244346F, 0, 0));
    PartDefinition leftRibbon = root.addOrReplaceChild("leftRibbon",
        CubeListBuilder.create().texOffs(46, 59).mirror().addBox(0, -3, 0, 5, 4, 1),
        PartPose.offsetAndRotation(-4, 4, 0, 0.446143F, 0, -0.785398F));
    PartDefinition rightRibbon = root.addOrReplaceChild("rightRibbon",
        CubeListBuilder.create().texOffs(46, 59).mirror().addBox(0, -3, -1, 5, 4, 1),
        PartPose.offsetAndRotation(-4, 4, 0, -0.446141F, 3.14159F, 0.349066F));

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