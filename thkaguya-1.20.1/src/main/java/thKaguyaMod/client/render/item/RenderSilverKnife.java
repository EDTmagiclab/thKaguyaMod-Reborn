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
 * 阶段 4-6 再迁完整模型；当前用刀形贴图（textures/shot/silverknife_*.png）画
 * 沿弹道方向的长条四边形，按 knifeColor 选 4 张贴图，保留投掷朝向。
 */
public class RenderSilverKnife extends EntityRenderer<EntitySilverKnife>
{
    private static final ResourceLocation TEX_BLUE  = tex("textures/shot/silverknife_blue.png");
    private static final ResourceLocation TEX_RED   = tex("textures/shot/silverknife_red.png");
    private static final ResourceLocation TEX_GREEN = tex("textures/shot/silverknife_green.png");
    private static final ResourceLocation TEX_WHITE = tex("textures/entity/silver_knife_white.png");

    private static ResourceLocation tex(String path)
    {
        return new ResourceLocation(THKaguyaCore.MODID, path);
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

        // 顶部十字双面四边形（0.8x0.8 平面，近似刀形）
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();
        float half = 0.4F;

        consumer.vertex(matrix,  half,  half, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half,  half, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half, -half, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix,  half, -half, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
        super.render(knife, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
