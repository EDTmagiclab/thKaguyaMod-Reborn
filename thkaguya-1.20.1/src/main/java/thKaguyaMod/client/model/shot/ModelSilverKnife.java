package thKaguyaMod.client.model.shot;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import thKaguyaMod.THKaguyaCore;

/**
 * 銀のナイフの 3D モデル（1.7.10 原版 {@code client/model/ModelSilverKnife} の 1:1 移植）。
 *
 * <p>原版は ModelBase + ModelRenderer 3 個（鍔 / 柄 / 刃部）。すべて
 * {@code rotateAngleX = PI/2} で、0.0625 スケールで描画されていた
 * （{@code modelSilverKnife.render(thShot, 0, 0, -0.1F, 0, 0, 0.0625F)}）。
 * 1.20.1 の LayerDefinition / ModelPart は同じ 1/16 単位系なので、addBox の数値は
 * そのまま、回転は PartPose の xRot に写せば等価になる。
 *
 * <p>テクスチャは {@code textures/shot/silverknife_<color>.png}（64×32、8 色分）。
 * FORM_KNIFE（銀のナイフ弾）は BrilliantDragonBullet / EternalMeek / Fafurotskies /
 * SatuzinDoll の 4 符卡で使われる。
 */
@OnlyIn(Dist.CLIENT)
public class ModelSilverKnife
{
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(THKaguyaCore.MODID, "silver_knife_shot"), "main");

    private final ModelPart root;

    public ModelSilverKnife(ModelPart root)
    {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 鍔（原版 knife[0]：texOffs 0,0 / addBox(-4, -5, -1, 7, 1, 3)）
        root.addOrReplaceChild("guard",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -5, -1, 7, 1, 3),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.PI / 2F, 0.0F, 0.0F));

        // 柄（原版 knife[1]：texOffs 0,16 / addBox(-2, -9, 0, 3, 14, 1)）
        root.addOrReplaceChild("handle",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2, -9, 0, 3, 14, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.PI / 2F, 0.0F, 0.0F));

        // 刃部 小（原版 knife[2]：texOffs 32,16 / addBox(-1, 5, 0, 1, 1, 1)）
        root.addOrReplaceChild("bladeTip",
                CubeListBuilder.create().texOffs(32, 16).addBox(-1, 5, 0, 1, 1, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.PI / 2F, 0.0F, 0.0F));

        // 原版テクスチャは 64×32
        return LayerDefinition.create(mesh, 64, 32);
    }

    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack,
                       com.mojang.blaze3d.vertex.VertexConsumer consumer,
                       int packedLight, int packedOverlay)
    {
        this.root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
