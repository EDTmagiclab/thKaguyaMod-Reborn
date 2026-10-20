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
import thKaguyaMod.entity.living.EntityTHPhantom;

/**
 * 幽霊の描画（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/client/render/living/RenderTHPhantom.java（1.7.10、137 行）
 *   - 貼り付け先は textures/shot/MusouFuuin.png の左半分（umin 0.0 / umax 32/64 = 0.5 / vmin 0.0 / vmax 1.0）
 *   - カメラ正面向きの板を「揺らぎ 2 枚 + 固定 3 枚」の計 5 枚重ねる（Z 0.000 / 0.001 / 0.002）
 *   - 色は 8 色（getForm() 0..7）、colorR/G/B の 0.3 倍を加算合成
 *   - 被弾中は hurtTime で alpha = (40 - hurtTime) / 40 に薄め、固定 3 枚は赤（1,0,0）に染める
 *   - 全体サイズ 1.0 のあと、位置をずらしながら 0.9 倍・0.8 倍の「残像」を 2 枚描く
 *   - ブレンドは GL_ONE, GL_ONE（加算）＋深度書き込みなし（原版 glDepthMask(false)）
 *   - 常に全輝度（原版 0xf000f0）
 *
 * 1.20.1 では 1.7.10 の Tessellator 直叩きを顶点ストリームに置き換えている
 * （座標・UV・色・合成式は原版の数値をそのまま維持）。
 */
public class RenderTHPhantom extends EntityRenderer<EntityTHPhantom>
{
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/shot/musoufuuin.png");

    /* 原版 RenderTHPhantom の colorR/colorG/colorB（8 色、0..255 を 1.0 基準に正規化） */
    private static final float[] COLOR_R = {224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F, 255F / 255F};
    private static final float[] COLOR_G = {0F / 255F, 0F / 255F, 224F / 255F, 64F / 255F, 0F / 255F, 224F / 255F, 128F / 255F, 255F / 255F};
    private static final float[] COLOR_B = {0F / 255F, 224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F};

    public RenderTHPhantom(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.25F;   // 原版 RenderLiving(..., 0.25F)
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHPhantom entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityTHPhantom entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int color = Math.floorMod((int)entity.getForm(), 8);
        double time_r = (double)entity.tickCount / 180.0D * Math.PI;

        float alpha = Math.max(0.0F, Math.min(1.0F, (40F - (float)entity.hurtTime) / 40F));

        poseStack.pushPose();
        // プレイヤーに常に正面を向ける（原版 glRotatef(180F - playerViewY) / glRotatef(-playerViewX)）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // 原版と同じく行列を積み上げていく（translate / scale は累積）
        poseStack.scale(1.0F, 1.0F, 1.0F);
        drawPart(poseStack, buffer, color, time_r, alpha, entity.hurtTime > 0);

        poseStack.translate(Math.sin(time_r * 5.0D) * 0.2F, 0.1F + (float)(entity.tickCount % 20) * 0.06F, 0.0F);
        float size = 0.9F * ((20F - (float)(entity.tickCount % 20)) / 20F);
        poseStack.scale(size, size, size);
        drawPart(poseStack, buffer, color, time_r, alpha, entity.hurtTime > 0);

        poseStack.translate(Math.cos(time_r * 5.0D) * 0.2F, 0.1F + (float)(entity.tickCount % 20) * 0.07F, 0.0F);
        size = 0.8F * ((20F - (float)(entity.tickCount % 20)) / 20F);
        poseStack.scale(size, size, size);
        drawPart(poseStack, buffer, color, time_r, alpha, entity.hurtTime > 0);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** 原版 renderPhantomPart：揺らぎ 2 枚（alpha 0.3 / 0.7）＋ 固定 3 枚 */
    private static void drawPart(PoseStack poseStack, MultiBufferSource buffer, int color, double time_r, float alpha, boolean hurt)
    {
        float cr = COLOR_R[color] * 0.3F;
        float cg = COLOR_G[color] * 0.3F;
        float cb = COLOR_B[color] * 0.3F;

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.doubleAdd(TEX));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        float umin = 0.0F;
        float umax = 32F / 64F;
        float vmin = 0.0F;
        float vmax = 1.0F;

        // 1 枚目（alpha 0.3）
        quad(consumer, matrix, 0.0D, umin, vmin, umax, vmax, cr, cg, cb, 0.3F * alpha,
                -0.7F + (float)Math.sin(time_r * 3.0D) * 0.1F, -0.2F - (float)Math.cos(time_r * 7.0D) * 0.1F,
                 0.7F - (float)Math.cos(time_r * 4.0D) * 0.1F, -0.2F - (float)Math.sin(time_r * 5.0D) * 0.1F,
                 0.7F + (float)Math.sin(time_r * 5.0D) * 0.1F,  1.2F + (float)Math.cos(time_r * 4.0D) * 0.1F,
                -0.7F - (float)Math.cos(time_r * 7.0D) * 0.1F,  1.2F + (float)Math.sin(time_r * 3.0D) * 0.1F);

        // 2 枚目（alpha 0.7）
        quad(consumer, matrix, 0.001D, umin, vmin, umax, vmax, cr, cg, cb, 0.7F * alpha,
                -0.6F - (float)Math.cos(time_r * 7.0D) * 0.1F, -0.1F + (float)Math.sin(time_r * 5.0D) * 0.1F,
                 0.6F + (float)Math.sin(time_r * 3.0D) * 0.1F, -0.1F + (float)Math.cos(time_r * 4.0D) * 0.1F,
                 0.6F - (float)Math.cos(time_r * 4.0D) * 0.1F,  1.1F - (float)Math.sin(time_r * 3.0D) * 0.1F,
                -0.6F + (float)Math.sin(time_r * 5.0D) * 0.1F,  1.1F - (float)Math.cos(time_r * 7.0D) * 0.1F);

        // 固定 3 枚（被弾中は赤）
        for(int i = 0; i < 3; i++)
        {
            if(hurt)
            {
                quad(consumer, matrix, 0.002D, umin, vmin, umax, vmax, 1.0F, 0.0F, 0.0F, alpha,
                        -0.5F, 0.0F, 0.5F, 0.0F, 0.5F, 1.0F, -0.5F, 1.0F);
            }
            else
            {
                quad(consumer, matrix, 0.002D, umin, vmin, umax, vmax, cr, cg, cb, 0.7F * alpha,
                        -0.5F, 0.0F, 0.5F, 0.0F, 0.5F, 1.0F, -0.5F, 1.0F);
            }
        }
    }

    /** 原版 Tessellator の 1 クアッド分（左下 vmax → 右下 vmax → 右上 vmin → 左上 vmin、法線 (0,1,0)） */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, double z,
                             float umin, float vmin, float umax, float vmax,
                             float cr, float cg, float cb, float ca,
                             float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3)
    {
        int r = (int)(cr * 255F);
        int g = (int)(cg * 255F);
        int b = (int)(cb * 255F);
        int a = (int)(ca * 255F);
        consumer.addVertex(matrix, x0, y0, (float)z).setColor(r, g, b, a)
                .setUv(umin, vmax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x1, y1, (float)z).setColor(r, g, b, a)
                .setUv(umax, vmax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x2, y2, (float)z).setColor(r, g, b, a)
                .setUv(umax, vmin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x3, y3, (float)z).setColor(r, g, b, a)
                .setUv(umin, vmin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
