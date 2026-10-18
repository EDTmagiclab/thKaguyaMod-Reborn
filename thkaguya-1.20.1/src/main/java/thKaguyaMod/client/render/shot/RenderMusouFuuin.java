package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.shot.EntityMusouFuuin;

/**
 * 夢想封印の光弾（1.20.1 移植版）。
 *
 * <p>原版 {@code RenderMusouFuuin}（104 行）は {@code RenderTHShot} とは別の専用レンダラで、
 * 加算ではなく <b>発光混合 (GL_ONE, GL_ONE_MINUS_SRC_COLOR)</b> の 3 枚重ね
 * （±0.6 を sin/cos で虹色 α0.60、±0.5 を同じ色 α0.60、±0.4 を白 α0.9）を
 * ビルボードで描き、U 座標は 64px 中の 32px を {@code animationCount % 2} で切り替える。
 * 移植版は汎用 {@code RenderTHShot} で代用していたため、この発光・虹色・2 フレームが失われていた。
 */
public class RenderMusouFuuin extends net.minecraft.client.renderer.entity.EntityRenderer<EntityMusouFuuin>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/musoufuuin.png");

    public RenderMusouFuuin(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMusouFuuin entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityMusouFuuin entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        int anim = entity.getAnimationCount();
        int frame = Math.floorMod(anim, 2);
        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;

        float size = entity.getShotSize();

        // 原版 setColorRGBA_F(sin(color), cos(color), -sin(color)*2, alpha)
        float r = Mth.sin((float)anim);
        float g = Mth.cos((float)anim);
        float b = -Mth.sin((float)anim) * 2.0F;
        int cr = clampColor(r);
        int cg = clampColor(g);
        int cb = clampColor(b);

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEXTURE));

        // 外殻（±0.6、α0.60）
        quad(consumer, poseStack.last(), cr, cg, cb, 153, 0.6F, uMin, uMax);
        // 中間（±0.5、α0.60、z+0.001）
        poseStack.translate(0.0F, 0.0F, 0.001F);
        quad(consumer, poseStack.last(), cr, cg, cb, 153, 0.5F, uMin, uMax);
        // 芯（±0.4、白 α0.9、z+0.001）
        poseStack.translate(0.0F, 0.0F, 0.001F);
        quad(consumer, poseStack.last(), 255, 255, 255, 230, 0.4F, uMin, uMax);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static int clampColor(float value)
    {
        return Math.max(0, Math.min(255, (int)(value * 255.0F)));
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose, int r, int g, int b, int a,
            float half, float uMin, float uMax)
    {
        c.vertex(pose.pose(), -half, -half, 0.0F).color(r, g, b, a).uv(uMin, 1.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        c.vertex(pose.pose(), half, -half, 0.0F).color(r, g, b, a).uv(uMax, 1.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        c.vertex(pose.pose(), half, half, 0.0F).color(r, g, b, a).uv(uMax, 0.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        c.vertex(pose.pose(), -half, half, 0.0F).color(r, g, b, a).uv(uMin, 0.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
