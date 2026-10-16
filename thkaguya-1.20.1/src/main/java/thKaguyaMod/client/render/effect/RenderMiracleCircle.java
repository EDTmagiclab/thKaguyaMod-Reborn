package thKaguyaMod.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.effect.EntityMiracleCircle;

/**
 * 奇迹五芒星渲染器（1.20.1 移植版，阶段 4-6）
 *
 * 原版 RenderMiracleCircle：面向相机的五角星线条（5 条线，最后一条按时间生长），
 * 颜色按 getColor() 从紫/黄/赤/青/绿 5 色取，贴图 miraclecircle.png 右侧星形子图。
 */
public class RenderMiracleCircle extends EntityRenderer<EntityMiracleCircle>
{
    private static final ResourceLocation TEX =
            new ResourceLocation(THKaguyaCore.MODID, "textures/misc/miraclecircle.png");

    /* 原版 5 色：紫、黄、赤、青、绿 */
    private static final float[] COLOR_R = {0.7F, 0.7F, 0.9F, 0.3F, 0.2F};
    private static final float[] COLOR_G = {0.1F, 0.7F, 0.1F, 0.3F, 0.9F};
    private static final float[] COLOR_B = {0.9F, 0.1F, 0.2F, 0.9F, 0.2F};

    public RenderMiracleCircle(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMiracleCircle entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityMiracleCircle entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getColor();
        int j = Math.max(0, Math.min(4, color));
        int cr = (int)(COLOR_R[j] * 255.0F);
        int cg = (int)(COLOR_G[j] * 255.0F);
        int cb = (int)(COLOR_B[j] * 255.0F);
        int ca = (int)(0.5F * 255.0F);

        poseStack.pushPose();
        // 面向相机（原版 THKaguyaRenderLib.getGLRotatefByTherdPersonView）+ 绕 Z 转 turnAngle
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.turnAngle));

        float umin = 0.5F;
        float umax = 0.625F;
        float vmin = 0.0F;
        float vmax = 1.0F;
        float angle = -108F;
        float length = 3.0F;
        float width = 0.10F;
        float py = length / (2.0F * Mth.cos(0.3141593F));
        float px = 0F;
        float nextpx = px + Mth.cos(angle / 180F * (float)Math.PI) * length;
        float nextpy = py + Mth.sin(angle / 180F * (float)Math.PI) * length;
        int lastLine = entity.getNumberOfDrewLine() - 1;

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX, false, true));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        int i;
        for(i = 0; i < lastLine; i++)
        {
            float zbuf = 0.001F * (float)i;
            float widthX = Mth.cos((angle + 90F) / 180F * (float)Math.PI) * width;
            float widthY = Mth.sin((angle + 90F) / 180F * (float)Math.PI) * width;

            consumer.vertex(matrix, px - widthX, py - widthY, zbuf).color(cr, cg, cb, ca)
                    .uv(umin, vmax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            consumer.vertex(matrix, px + widthX, py + widthY, zbuf).color(cr, cg, cb, ca)
                    .uv(umax, vmax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            consumer.vertex(matrix, nextpx + widthX, nextpy + widthY, zbuf).color(cr, cg, cb, ca)
                    .uv(umax, vmin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            consumer.vertex(matrix, nextpx - widthX, nextpy - widthY, zbuf).color(cr, cg, cb, ca)
                    .uv(umin, vmin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();

            angle += 144F;
            px = nextpx;
            py = nextpy;
            nextpx = px + Mth.cos(angle / 180F * (float)Math.PI) * length;
            nextpy = py + Mth.sin(angle / 180F * (float)Math.PI) * length;
        }

        // 最后一条线（按时间生长）
        float lastLineLength = length * (float)entity.getLastLineTime() / 10F;
        nextpx = px + Mth.cos(angle / 180F * (float)Math.PI) * lastLineLength;
        nextpy = py + Mth.sin(angle / 180F * (float)Math.PI) * lastLineLength;
        float zbufLast = 0.001F * (float)i;
        float widthX = Mth.cos((angle + 90F) / 180F * (float)Math.PI) * width;
        float widthY = Mth.sin((angle + 90F) / 180F * (float)Math.PI) * width;

        consumer.vertex(matrix, px - widthX, py - widthY, zbufLast).color(cr, cg, cb, ca)
                .uv(umin, vmax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, px + widthX, py + widthY, zbufLast).color(cr, cg, cb, ca)
                .uv(umax, vmax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, nextpx + widthX, nextpy + widthY, zbufLast).color(cr, cg, cb, ca)
                .uv(umax, vmin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, nextpx - widthX, nextpy - widthY, zbufLast).color(cr, cg, cb, ca)
                .uv(umin, vmin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
