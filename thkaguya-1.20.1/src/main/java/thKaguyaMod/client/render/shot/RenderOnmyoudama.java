package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import thKaguyaMod.entity.shot.EntityOnmyoudama;

/**
 * 陰陽玉レンダラー（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/client/render/shot/RenderOnmyoudama.java（223 行）
 *   - 貼图 onmyoudama2.png（128x64）の左半分（u 0..0.5, v 0..1）= 陰陽玉本体。
 *     本体クワッドには色指定がない（＝白）ので、テクスチャの赤/白がそのまま出る。
 *   - 右半分の上部 32px に 2 コマの光エフェクト（u 0.5..0.75 / 0.75..1.0, v 0..0.5、
 *     animationCount % 2 でコマ送り）。色は原版どおり colorR/G/B[0]（赤）、alpha 0.5。
 *   - 常にカメラ正面（原版 THKaguyaRenderLib.getGLRotatefByTherdPersonView）＋ Z 軸回転 time * 5F、
 *     スケールは getShotSize()（溜め中は 3.0 まで拡大する）。
 *   - ブレンドは GL_ONE, GL_ONE_MINUS_SRC_COLOR、深度書き込みなし（THRenderTypes.glow）。
 */
public class RenderOnmyoudama extends EntityRenderer<EntityOnmyoudama>
{
    private static final ResourceLocation ORB_TEX =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/onmyoudama2.png");
    private static final ResourceLocation LIGHT_TEX =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/musoufuuin.png");

    /* 原版 RenderOnmyoudama の 8 色テーブル */
    private static final float[] COLOR_R = {224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F, 255F / 255F};
    private static final float[] COLOR_G = {0F / 255F, 0F / 255F, 224F / 255F, 64F / 255F, 0F / 255F, 224F / 255F, 128F / 255F, 255F / 255F};
    private static final float[] COLOR_B = {0F / 255F, 224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F};

    public RenderOnmyoudama(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOnmyoudama entity)
    {
        // 原版 getEntityTexture: animationCount < 0 のときだけ光テクスチャ
        return entity.getAnimationCount() < 0 ? LIGHT_TEX : ORB_TEX;
    }

    @Override
    public void render(EntityOnmyoudama entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int time = entity.tickCount;
        int color = entity.getShotColor() % 8;
        if(color < 0)
        {
            color += 8;
        }

        poseStack.pushPose();
        // 常にカメラ正面を向かせ、Z 軸で回転（原版と同じ順序）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 5F));
        poseStack.scale(entity.getShotSize(), entity.getShotSize(), entity.getShotSize());

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(ORB_TEX, false, false));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 本体（原版は色指定なし = 白、alpha 1.0）
        quad(consumer, matrix, normal, -0.5F, -0.5F, 0.5F, 0.5F, 0.01F,
                0.0F, 0.5F, 0.0F, 1.0F, 255, 255, 255, 255);

        // 光エフェクト（原版 renderLightEffect(tessellator, 0, size + 0.2F, entity)）
        int count = entity.getAnimationCount() % 2;
        float umin = (64F + count * 32F) / 128F;
        float umax = (64F + count * 32F + 32F) / 128F;
        int cr = (int)(COLOR_R[0] * 255.0F);
        int cg = (int)(COLOR_G[0] * 255.0F);
        int cb = (int)(COLOR_B[0] * 255.0F);

        quad(consumer, matrix, normal, -0.7F, -0.7F, 0.7F, 0.7F, 0.0F,
                umin, umax, 0.0F, 0.5F, cr, cg, cb, (int)(0.5F * 255.0F));

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** 原版 Tessellator の 4 頂点クワッド（左下 → 右下 → 右上 → 左上、UV は v 反転） */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                             float x0, float y0, float x1, float y1, float z,
                             float umin, float umax, float vmin, float vmax,
                             int red, int green, int blue, int alpha)
    {
        consumer.vertex(matrix, x0, y0, z).color(red, green, blue, alpha).uv(umin, vmax)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, x1, y0, z).color(red, green, blue, alpha).uv(umax, vmax)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, x1, y1, z).color(red, green, blue, alpha).uv(umax, vmin)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, x0, y1, z).color(red, green, blue, alpha).uv(umin, vmin)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
