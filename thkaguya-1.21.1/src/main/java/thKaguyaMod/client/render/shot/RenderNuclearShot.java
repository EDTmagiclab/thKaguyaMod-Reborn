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
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.shot.EntityNuclearShot;

/**
 * 核熱「ニュークリアフュージョン」等の核弾の描画（1.20.1 移植版）。
 *
 * <p>原版 {@code RenderNuclearShot}（222 行）は専用レンダラで、2 つの描画モードを持つ：
 * <ul>
 *   <li>{@code animationCount < 0}（発射遅延）：ビルボード 1 枚の発光（MusouFuuin.png、α0.5、
 *       U は 64px 中の 32px を 2 フレームで切替、大きさ min(delayCount*size*2, 1)）</li>
 *   <li>通常時：Z 軸回り 18 分割 × 奥行き 9 分割の球体メッシュを 2 回描く。
 *       1 回目は白（color 7）を発光混合 (ONE, ONE_MINUS_SRC_COLOR)・表面・深度あり、
 *       2 回目は shotColor を純加算 (ONE, ONE)・両面・深度なしで 1.4 倍。
 *       頂点色は {@code (time + j)/10} の sin/cos で ±0.2 揺らぐ。</li>
 * </ul>
 * 移植版は汎用 {@code RenderTHShot} で代用していたため、この 2 段球体と遅延発光が失われていた。
 */
public class RenderNuclearShot extends EntityRenderer<EntityNuclearShot>
{
    private static final ResourceLocation LASER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/shot/laser.png");
    private static final ResourceLocation LIGHT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/shot/musoufuuin.png");

    private static final float[] COLOR_R = {224F / 255F, 0F, 0F, 224F / 255F, 224F / 255F, 0F, 255F / 255F, 1.0F};
    private static final float[] COLOR_G = {0F, 0F, 224F / 255F, 64F / 255F, 0F, 224F / 255F, 128F / 255F, 1.0F};
    private static final float[] COLOR_B = {0F, 224F / 255F, 0F, 0F, 224F / 255F, 224F / 255F, 0F, 1.0F};

    public RenderNuclearShot(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityNuclearShot entity)
    {
        return entity.getAnimationCount() < 0 ? LIGHT_TEXTURE : LASER_TEXTURE;
    }

    @Override
    public void render(EntityNuclearShot entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        int anim = entity.getAnimationCount();
        int color = Math.floorMod(entity.getShotColor(), 8);
        float size = entity.getShotSize();

        poseStack.pushPose();

        if (anim < 0)
        {
            // ---- 発射遅延の発光 ----
            int delayCount = Math.min(-anim, 10);
            float size2 = delayCount * size * 2.0F;
            if (size2 > 1.0F)
            {
                size2 = 1.0F;
            }
            int frame = Math.floorMod(anim, 2);
            float uMin = (frame * 32F) / 64F;
            float uMax = (frame * 32F + 32F) / 64F;

            poseStack.scale(size2, size2, size2);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

            // 遅延演出も musoufuuin.png（透明部の RGB が赤）を発光混合で描くため、
            // alpha discard 付きパスでないと赤い四角＝「橙色の小弾」に見えてしまう。
            VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glowCutout(LIGHT_TEXTURE));
            int r = (int)(COLOR_R[color] * 255F);
            int g = (int)(COLOR_G[color] * 255F);
            int b = (int)(COLOR_B[color] * 255F);
            quad(consumer, poseStack.last(), r, g, b, 128, uMin, uMax, -0.5F, -0.25F, 1.0F, 1.0F, 0.75F);
        }
        else
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));

            // 1 回目：白い芯（発光混合・表面・深度あり）
            renderLaser(poseStack, buffer.getBuffer(THRenderTypes.glow(LASER_TEXTURE, true, true)),
                    size, size, 7, 0.8F, 0);

            // 2 回目：色付き外殻（純加算・両面・深度なし、1.4 倍）
            renderLaser(poseStack, buffer.getBuffer(THRenderTypes.blend(LASER_TEXTURE,
                            THRenderTypes.BLEND_DOUBLE_ADD, false, false)),
                    size * 1.4D, size * 1.4F, color, 0.8F, entity.tickCount);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 原版 renderLaser：Z 軸回り 18 分割 × 奥行き 9 分割の球体メッシュ */
    private static void renderLaser(PoseStack poseStack, VertexConsumer consumer,
            double length, float width, int color, float alpha, int time)
    {
        final int zAngleDivNum = 18;
        final double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;
        final int zDivNum = 9;
        final float angleSpan = (float)Math.PI / (float)zDivNum;

        float maxWidth = width / 2.0F;
        double zPos = -maxWidth;
        double zPosOld = zPos;
        float angle = -(float)Math.PI / 2.0F + angleSpan;
        float widthOld = 0.0F;

        PoseStack.Pose pose = poseStack.last();

        for (int j = 0; j < zDivNum; j++)
        {
            zPos = Math.sin(angle) * maxWidth;
            float w = (float)Math.cos(angle) * maxWidth;

            float xPos = w;
            float yPos = 0.0F;
            double angleZ = 0.0D;
            float xPosOld = (float)Math.cos(angleZ) * w;
            float yPosOld = (float)Math.sin(angleZ) * w;
            float xPos2Old = (float)Math.cos(angleZ) * widthOld;
            float yPos2Old = (float)Math.sin(angleZ) * widthOld;
            angleZ = angleSpanZ;

            for (int i = 1; i <= zAngleDivNum; i++)
            {
                xPos = (float)Math.cos(angleZ) * w;
                yPos = (float)Math.sin(angleZ) * w;
                float xPos2 = (float)Math.cos(angleZ) * widthOld;
                float yPos2 = (float)Math.sin(angleZ) * widthOld;

                double colorVar = time != 0 ? (time + j) / 10.0D : 0.0D;
                float r = clamp(COLOR_R[color] + (float)Math.sin(colorVar) * 0.2F);
                float g = clamp(COLOR_G[color] + (float)Math.cos(colorVar) * 0.2F);
                float b = clamp(COLOR_B[color] - (float)Math.sin(colorVar) * 0.2F);
                int ir = (int)(r * 255F);
                int ig = (int)(g * 255F);
                int ib = (int)(b * 255F);
                int ia = (int)(alpha * 255F);

                vertex(consumer, pose, xPos, yPos, (float)zPos, 1.0F, 0.0F, ir, ig, ib, ia);
                vertex(consumer, pose, xPosOld, yPosOld, (float)zPos, 0.0F, 0.0F, ir, ig, ib, ia);
                vertex(consumer, pose, xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F, ir, ig, ib, ia);
                vertex(consumer, pose, xPos2, yPos2, (float)zPosOld, 1.0F, 1.0F, ir, ig, ib, ia);

                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }

            zPosOld = zPos;
            angle += angleSpan;
            widthOld = w;
        }
    }

    private static float clamp(float value)
    {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /** 原版 renderLightEffect：ビルボード 1 枚（x -0.5..0.5, y -0.25..0.75） */
    private static void quad(VertexConsumer c, PoseStack.Pose pose, int r, int g, int b, int a,
            float uMin, float uMax, float x0, float y0, float x1, float y1, float y2)
    {
        vertex(c, pose, x0, y0, 0.0F, uMin, 1.0F, r, g, b, a);
        vertex(c, pose, x1, y0, 0.0F, uMax, 1.0F, r, g, b, a);
        vertex(c, pose, x1, y2, 0.0F, uMax, 0.0F, r, g, b, a);
        vertex(c, pose, x0, y2, 0.0F, uMin, 0.0F, r, g, b, a);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int r, int g, int b, int a)
    {
        c.addVertex(pose.pose(), x, y, z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
