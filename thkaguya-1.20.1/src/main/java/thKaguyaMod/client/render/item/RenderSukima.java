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
import net.minecraft.util.Mth;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntitySukima;

/**
 * スキマ（隙間）の描画（1.20.1 移植版，阶段 3d-4 可见性修复）
 *
 * 原版 RenderSukima：本体四边形 + 黑色半透明内衬 + 两条彩色缎带（正面/背面），
 * 顶部悬浮上下漂动（sin(ticksExisted/20)*0.1）。
 * 颜色表沿用原版 17 色定义，特殊色（>=16）按 openRate 收缩开关动画。
 * 贴图用原版 SukimaTexture3.png（64x32，重命名为 sukima_texture3.png），
 * UV 布局与原版完全一致（本体取 U 0..0.75，缎带取 U 0.75..1.0）。
 * 注意：textures/item/gap.png 是物品图标，不是隙间本体贴图。
 */
public class RenderSukima extends EntityRenderer<EntitySukima>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/sukima_texture3.png");

    // 原版 17 色 RGB 表
    private static final float[] COLOR_R = { 30,179, 59, 81, 37,123, 40,128, 51,216, 65,222,102,211,237,240,179};
    private static final float[] COLOR_G = { 27, 49, 81, 48, 49, 47,118,128, 51,129,203,207,133, 84,136,240, 49};
    private static final float[] COLOR_B = { 27, 44, 26, 26,146,190,151,128, 51,152, 51, 42,219,205, 68,240, 44};

    public RenderSukima(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySukima entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntitySukima sukima, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 原版悬浮：y + 1.0F + sin(ticksExisted/20)*0.1（注意：原版从实体原点上方 1.0 处开始画）
        poseStack.translate(0.0D, 1.0F + Mth.sin(sukima.tickCount / 20.0F) * 0.1F, 0.0D);

        int rawColor = sukima.getColor();
        int color = rawColor;
        float openRate = Mth.sin((float)sukima.tickCount * 15.0F / 360.0F * (float)Math.PI);
        if(color == 17)
        {
            openRate = Mth.sin((float)sukima.tickCount * 40.0F / 360.0F * (float)Math.PI);
            color = 16;
        }
        float xl = 1.0F;
        float yl = -0.75F;
        float xr = -1.0F;
        float yr = 0.75F;
        if(color >= 16)
        {
            xl *= openRate;
            yl *= openRate;
            xr *= openRate;
            yr *= openRate;
        }
        color = color % 32;
        if(color >= COLOR_R.length)
        {
            color = COLOR_R.length - 1;
        }
        float cr = COLOR_R[color] / 255.0F;
        float cg = COLOR_G[color] / 255.0F;
        float cb = COLOR_B[color] / 255.0F;

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        // 本体用贴图 cutout；黑衬/缎带 alpha 0.7 需 translucent RenderType
        // （cutout 只做 alpha 阈值丢弃不做混合，半透明顶点走 cutout 显示异常，实测踩坑）
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer translucent = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));

        // ===== 本体（UV 与原版一致：SukimaTexture3 布局）=====
        vertexUV(consumer, poseStack, -1.0F, -0.75F, 0.0F,     0.00F, 1.00F);
        vertexUV(consumer, poseStack,  xl,     yl,   0.0F,     0.75F, 1.00F);
        vertexUV(consumer, poseStack,  1.0F,   0.75F, 0.0F,    0.75F, 0.00F);
        vertexUV(consumer, poseStack,  xr,     yr,   0.0F,     0.00F, 0.00F);

        // ===== 黑色内衬（半透明，z=-0.0009）=====
        vertexTint(translucent, poseStack,  xl,     yl,  -0.0009F, 0.75F, 1.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack, -1.0F,  -0.75F,-0.0009F, 0.00F, 1.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack,  xr,     yr,  -0.0009F, 0.00F, 0.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack,  1.0F,   0.75F,-0.0009F, 0.75F, 0.00F, 0, 0, 0, 0.7F);

        // ===== 缎带：绕 Z 轴旋转 30° =====
        poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F));
        // 右上 表
        vertexTint(translucent, poseStack,  1.00F, -0.2F, 0.001F, 0.75F, 1.00F, cr, cg, cb, 0.7F);
        vertexTint(translucent, poseStack,  1.25F, -0.2F, 0.001F, 1.00F, 1.00F, cr, cg, cb, 0.7F);
        vertexTint(translucent, poseStack,  1.25F,  0.3F, 0.001F, 1.00F, 0.00F, cr, cg, cb, 0.7F);
        vertexTint(translucent, poseStack,  1.00F,  0.3F, 0.001F, 0.75F, 0.00F, cr, cg, cb, 0.7F);
        // 左下 表
        vertexTint(translucent, poseStack, -1.30F, -0.3F, 0.001F, 0.75F, 1.00F, cr, cg, cb, 0.7F);
        vertexTint(translucent, poseStack, -1.05F, -0.3F, 0.001F, 1.00F, 1.00F, cr, cg, cb, 0.7F);
        vertexTint(translucent, poseStack, -1.05F,  0.2F, 0.001F, 1.00F, 0.00F, cr, cg, cb, 0.7F);
        vertexTint(translucent, poseStack, -1.30F,  0.2F, 0.001F, 0.75F, 0.00F, cr, cg, cb, 0.7F);
        // 右上 裏
        vertexTint(translucent, poseStack,  1.00F,  0.3F,-0.001F, 0.75F, 0.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack,  1.25F,  0.3F,-0.001F, 1.00F, 0.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack,  1.25F, -0.2F,-0.001F, 1.00F, 1.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack,  1.00F, -0.2F,-0.001F, 0.75F, 1.00F, 0, 0, 0, 0.7F);
        // 左下 裏
        vertexTint(translucent, poseStack, -1.30F,  0.2F,-0.001F, 0.75F, 0.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack, -1.05F,  0.2F,-0.001F, 1.00F, 0.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack, -1.05F, -0.3F,-0.001F, 1.00F, 1.00F, 0, 0, 0, 0.7F);
        vertexTint(translucent, poseStack, -1.30F, -0.3F,-0.001F, 0.75F, 1.00F, 0, 0, 0, 0.7F);

        poseStack.popPose();
        super.render(sukima, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertexUV(VertexConsumer consumer, PoseStack poseStack,
            float x, float y, float z, float u, float v)
    {
        PoseStack.Pose pose = poseStack.last();
        consumer.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static void vertexTint(VertexConsumer consumer, PoseStack poseStack,
            float x, float y, float z, float u, float v, float r, float g, float b, float a)
    {
        PoseStack.Pose pose = poseStack.last();
        consumer.vertex(pose.pose(), x, y, z).color((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F), (int)(a * 255.0F))
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
