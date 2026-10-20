package thKaguyaMod.client.render;

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
import thKaguyaMod.entity.EntityDivineSpirit;

/**
 * 小神霊の描画（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/client/render/RenderDivineSpirit.java（1.7.10、105 行）
 *   - 貼り付け先は textures/shot/MusouFuuin.png の左半分（umin 0.0 / umax 0.5 / vmin 0.0 / vmax 1.0）
 *     （原版の pattern は常に 0 なので umin = 0、umax = 32/64 = 0.5）
 *   - カメラ正面向きの板を 3 枚重ねる（alpha 0.3 / 0.7 / 1.0、Z 0.000 / 0.001 / 0.002）
 *   - 1 枚目と 2 枚目は ticksExisted から作った揺らぎで四隅を動かす（3 枚目は固定 ±0.4）
 *   - 全体を (300 - count) / 300 倍に縮小（時間経過で小さくなる）
 *   - ブレンドは GL_ONE, GL_ONE_MINUS_SRC_COLOR（THRenderTypes.glow）+ 深度書き込みなし
 *     （原版は GL11.glDepthMask(false)）
 *   - 影なし（原版 getShadowSize() = 0.0F → shadowRadius = 0）、常に全輝度（原版 0xf000f0）
 *   - 原版の size2（= size * 0.7F）は原版でも未使用のため移植しない
 */
public class RenderDivineSpirit extends EntityRenderer<EntityDivineSpirit>
{
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/shot/musoufuuin.png");

    /* 原版 colorR/colorG/colorB（色番号 0..7 = 通常/村人/クリーパー/動物/手懐け・妖精/水棲/虫/不死） */
    private static final float[] COLOR_R = {224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F, 255F / 255F};
    private static final float[] COLOR_G = {0F / 255F, 0F / 255F, 224F / 255F, 64F / 255F, 0F / 255F, 224F / 255F, 128F / 255F, 255F / 255F};
    private static final float[] COLOR_B = {0F / 255F, 224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F};

    public RenderDivineSpirit(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.0F;   // 原版 getShadowSize() = 0.0F
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDivineSpirit entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityDivineSpirit entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int color = Math.max(0, Math.min(7, entity.getDivineSpiritColor()));
        int cr = (int)(COLOR_R[color] * 255F);
        int cg = (int)(COLOR_G[color] * 255F);
        int cb = (int)(COLOR_B[color] * 255F);

        double time_r = (double)entity.tickCount / 180.0D * Math.PI;

        float size = (300F - (float)entity.getDivineSpiritCount()) / 300F;
        if(size < 0.0F)
        {
            size = 0.0F;   // 原版は消滅直前 1tick だけ負値になり得るが、負のスケールを避けるため 0 で止める
        }

        poseStack.pushPose();
        // プレイヤーに常に正面を向ける（原版 THKaguyaRenderLib.getGLRotatefByTherdPersonView）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);

        // 深度書き込みなし（原版 glDepthMask(false)）、カリングなし
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX, false, false));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        float umin = 0.0F;
        float umax = 0.5F;
        float vmin = 0.0F;
        float vmax = 1.0F;

        // 1 枚目（alpha 0.3）
        quad(consumer, matrix, 0.0D, umin, vmin, umax, vmax, cr, cg, cb, (int)(0.3F * 255F),
                -0.7F + (float)Math.sin(time_r * 3.0D) * 0.1F, -0.7F - (float)Math.cos(time_r * 7.0D) * 0.1F,
                 0.7F - (float)Math.cos(time_r * 4.0D) * 0.1F, -0.7F - (float)Math.sin(time_r * 5.0D) * 0.1F,
                 0.7F + (float)Math.sin(time_r * 5.0D) * 0.1F,  0.7F + (float)Math.cos(time_r * 4.0D) * 0.1F,
                -0.7F - (float)Math.cos(time_r * 7.0D) * 0.1F,  0.7F + (float)Math.sin(time_r * 3.0D) * 0.1F);

        // 2 枚目（alpha 0.7）
        quad(consumer, matrix, 0.001D, umin, vmin, umax, vmax, cr, cg, cb, (int)(0.7F * 255F),
                -0.6F - (float)Math.cos(time_r * 7.0D) * 0.1F, -0.6F + (float)Math.sin(time_r * 5.0D) * 0.1F,
                 0.6F + (float)Math.sin(time_r * 3.0D) * 0.1F, -0.6F + (float)Math.cos(time_r * 4.0D) * 0.1F,
                 0.6F - (float)Math.cos(time_r * 4.0D) * 0.1F,  0.6F - (float)Math.sin(time_r * 3.0D) * 0.1F,
                -0.6F + (float)Math.sin(time_r * 5.0D) * 0.1F,  0.6F - (float)Math.cos(time_r * 7.0D) * 0.1F);

        // 3 枚目（alpha 1.0、固定）
        quad(consumer, matrix, 0.002D, umin, vmin, umax, vmax, cr, cg, cb, 255,
                -0.4F, -0.4F,
                 0.4F, -0.4F,
                 0.4F,  0.4F,
                -0.4F,  0.4F);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * 原版 Tessellator の 1 クアッド分。
     * 頂点順は 左下(vmax) → 右下(vmax) → 右上(vmin) → 左上(vmin)、法線は原版どおり (0, 1, 0)。
     */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, double z,
                             float umin, float vmin, float umax, float vmax, int cr, int cg, int cb, int ca,
                             float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3)
    {
        consumer.addVertex(matrix, x0, y0, (float)z).setColor(cr, cg, cb, ca)
                .setUv(umin, vmax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x1, y1, (float)z).setColor(cr, cg, cb, ca)
                .setUv(umax, vmax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x2, y2, (float)z).setColor(cr, cg, cb, ca)
                .setUv(umax, vmin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x3, y3, (float)z).setColor(cr, cg, cb, ca)
                .setUv(umin, vmin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
