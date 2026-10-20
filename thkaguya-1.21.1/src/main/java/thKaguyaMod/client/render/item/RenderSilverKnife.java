package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntitySilverKnife;

/**
 * 銀のナイフの描画（1.20.1 移植版，阶段 3d-4 可见性修复）
 *
 * 原版 RenderSilverKnife 用 ModelSilverKnife 立体模型（护手/柄/刃尖三盒）。
 * 阶段 4-6 再迁完整模型；当前用物品图标（textures/item/silver_knife_*.png，16x16
 * 完整刀形图）画沿弹道方向的细长四边形，按 knifeColor 选 4 张贴图，保留投掷朝向。
 * 注意：不能用 textures/shot/silverknife_*.png（64x32 弹幕精灵图，作 billboard 会显示展开碎片）。
 */
public class RenderSilverKnife extends EntityRenderer<EntitySilverKnife>
{
    private static final ResourceLocation TEX_BLUE  = tex("textures/item/silver_knife_blue.png");
    private static final ResourceLocation TEX_RED   = tex("textures/item/silver_knife_red.png");
    private static final ResourceLocation TEX_GREEN = tex("textures/item/silver_knife_green.png");
    private static final ResourceLocation TEX_WHITE = tex("textures/item/silver_knife_white.png");

    private static ResourceLocation tex(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, path);
    }

    public RenderSilverKnife(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySilverKnife entity)
    {
        return getKnifeTexture(entity.getKnifeColor());
    }

    private static ResourceLocation getKnifeTexture(int color)
    {
        switch(color)
        {
            case 0:  return TEX_BLUE;
            case 1:  return TEX_RED;
            case 2:  return TEX_GREEN;
            default: return TEX_WHITE;
        }
    }

    @Override
    public void render(EntitySilverKnife knife, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 原版：glRotatef(180F - yaw, 0,1,0); glRotatef(-rotationPitch, 1,0,0); glRotatef(knifeZAngle, 0,0,1)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-knife.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(knife.getKnifeZAngle()));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        ResourceLocation texture = getKnifeTexture(knife.getKnifeColor());
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        // 刀形物品图标 billboard（16x16 物品贴图，细长条比例，保留投掷朝向）
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();
        float hw = 0.11F;  // 刀身宽度的一半（细长条，接近原版刀形观感）
        float hh = 0.40F;  // 刀身长度的一半

        consumer.addVertex(matrix,  hw,  hh, 0.0F).setColor(255, 255, 255, 255)
                .setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, -hw,  hh, 0.0F).setColor(255, 255, 255, 255)
                .setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, -hw, -hh, 0.0F).setColor(255, 255, 255, 255)
                .setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix,  hw, -hh, 0.0F).setColor(255, 255, 255, 255)
                .setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);

        poseStack.popPose();
        super.render(knife, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
