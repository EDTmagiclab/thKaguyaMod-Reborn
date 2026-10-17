package thKaguyaMod.client.render.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.living.EntityFamiliar;

/**
 * 使い魔の描画（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/client/render/living/RenderFamiliar.java（1.7.10、117 行）
 *   - 貼り付け先は textures/mob/Familiar.png 全面（umin 0.0 / umax 1.0 / vmin 0.0 / vmax 1.0）
 *   - 形状 ID がマイナスなら描画しない（原版 getForm() < 0 で return）
 *   - カメラ正面向きの板 1 枚、Z 軸回転は ticksExisted * 5 度（ぐるぐる回る）
 *   - サイズ = 3.0 + sin(ticksExisted * 10 / 180 * π) * 0.6（脈打つように拡縮）
 *   - 色 = (sin(形式ID % 8), cos(形式ID % 8), -sin(形式ID % 8) * 2)、alpha 0.6
 *   - ブレンドは GL_ONE, GL_ONE_MINUS_SRC_COLOR ＋深度書き込みなし（原版 glDepthMask(false)）
 *   - 常に全輝度（原版 0xf000f0）
 *
 * 1.7.10 の setColorRGBA_F は 0..1 外の値も受け付けるが、1.20.1 の
 * VertexConsumer#color(int,int,int,int) は 0..255 しか受け付けないため、
 * 原版の (sin, cos, -sin*2) を 0..1 にクランプして渡す（明るさの差は僅少）。
 */
public class RenderFamiliar extends EntityRenderer<EntityFamiliar>
{
    private static final ResourceLocation TEX =
            new ResourceLocation(THKaguyaCore.MODID, "textures/mob/familiar.png");

    /** 原版 RenderFamiliar の colorR/colorG/colorB（未使用、書式のみ残す） */
    private static final float[] COLOR_R = {255F, 255F, 255F, 0F, 0F, 0F, 255F};
    private static final float[] COLOR_G = {0F, 165F, 255F, 255F, 0F, 255F, 0F};
    private static final float[] COLOR_B = {0F, 0F, 0F, 0F, 255F, 255F, 255F};

    public RenderFamiliar(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFamiliar entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityFamiliar entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        // 形状 ID がマイナスなら描画しない（原版どおり）
        if(entity.getForm() < 0)
        {
            return;
        }

        int color = Math.floorMod((int)entity.getForm(), 8);

        // 原版: sin(color), cos(color), -sin(color) * 2（0..1 にクランプ）
        float cr = clamp01((float)Math.sin(color));
        float cg = clamp01((float)Math.cos(color));
        float cb = clamp01((float)(-Math.sin(color) * 2.0F));

        float size = 3.0F + (float)Math.sin(entity.tickCount * 10 / 180.0D * Math.PI) * 0.6F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(entity.tickCount * 5.0F));
        poseStack.scale(size, size, size);

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX, false, false));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        int r = (int)(cr * 255F);
        int g = (int)(cg * 255F);
        int b = (int)(cb * 255F);
        int a = (int)(0.6F * 255F);

        // 原版は ±0.6 の 1 枚板（Z 0.0）
        consumer.vertex(matrix, -0.6F, -0.6F, 0.0F).color(r, g, b, a)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, 0.6F, -0.6F, 0.0F).color(r, g, b, a)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, 0.6F, 0.6F, 0.0F).color(r, g, b, a)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -0.6F, 0.6F, 0.0F).color(r, g, b, a)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static float clamp01(float value)
    {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
