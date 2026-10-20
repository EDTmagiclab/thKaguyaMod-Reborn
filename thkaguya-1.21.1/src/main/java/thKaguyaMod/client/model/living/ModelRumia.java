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

/** 自动生成自原版 ModelRumia（Techne → LayerDefinition） */
public class ModelRumia<T extends EntityDanmakuMob> extends EntityModel<T>
{
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ModelRumia(ModelPart root)
    {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("rightArm");
        this.leftArm = root.getChild("leftArm");
        this.rightLeg = root.getChild("rightLeg");
        this.leftLeg = root.getChild("leftLeg");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -3F, -4F, 8, 8, 8),
            PartPose.offset(0F, 0F, 0F));
            PartDefinition longHair = head.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(24, 0).addBox(-4F, 4F, -3F, 8, 5, 3),
                PartPose.offset(0F, 0F, 4F));
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(32, 8).mirror().addBox(-3F, 5F, -2F, 6, 7, 4),
            PartPose.offset(0F, 0F, 0F));
        PartDefinition skirt = root.addOrReplaceChild("skirt",
            CubeListBuilder.create().texOffs(0, 16).addBox(-4F, 0F, -4F, 8, 5, 8),
            PartPose.offset(0F, 10F, 0F));
            PartDefinition skirt2 = skirt.addOrReplaceChild("skirt2",
                CubeListBuilder.create().texOffs(24, 32).addBox(-5F, -5F, -5F, 10, 6, 10),
                PartPose.offset(0F, 9F, 0F));
        PartDefinition rightArm = root.addOrReplaceChild("rightArm",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1F, -1F, -1F, 2, 8, 2),
            PartPose.offset(-4F, 6F, 0F));
        PartDefinition leftArm = root.addOrReplaceChild("leftArm",
            CubeListBuilder.create().texOffs(56, 0).addBox(-1F, -1F, -1F, 2, 8, 2),
            PartPose.offset(4F, 6F, 0F));
        PartDefinition rightLeg = root.addOrReplaceChild("rightLeg",
            CubeListBuilder.create().texOffs(50, 16).addBox(-1F, 0F, -2F, 3, 12, 4),
            PartPose.offset(-2F, 12F, 0F));
        PartDefinition leftLeg = root.addOrReplaceChild("leftLeg",
            CubeListBuilder.create().texOffs(50, 16).addBox(-2F, 0F, -2F, 3, 12, 4),
            PartPose.offset(2F, 12F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if(head != null) { head.yRot = netHeadYaw * (float)Math.PI / 180F; head.xRot = headPitch * (float)Math.PI / 180F; }
        if(rightArm != null) { rightArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(leftArm != null) { leftArm.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }
        if(rightLeg != null) { rightLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }
        if(leftLeg != null) { leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }

        // スペルカードの宣言をする（原版 ModelRumia#setRotationAngles L135-147 の移植）
        byte spellCardMotion = entity.getSpellCardMotion();
        if(rightArm != null)
        {
            if(spellCardMotion < -15)
            {
                rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(30F + spellCardMotion) / 180F * (float)Math.PI) * 4F;
            }
            else if(spellCardMotion < 0)
            {
                rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(-spellCardMotion) / 180F * (float)Math.PI) * 4F;
            }
            else
            {
                rightArm.zRot = 90F / 180F * (float)Math.PI;
                if(leftArm != null) { leftArm.zRot = -rightArm.zRot; }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
