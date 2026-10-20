package thKaguyaMod.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.block.BlockDivineSpirit;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.tileentity.TileEntityDivineSpirit;

/**
 * 小神霊ブロックの TESR（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/client/TileEntityDivineSpiritRenderer.java（1.7.10、98 行）
 *   - ブロック中心 (x+0.5, y+0.5 + sin(time*5/180*π)*0.16, z+0.5) に浮かぶ板を 3 枚重ねる
 *     （alpha 0.3 / 0.7 / 1.0、Z 0.000 / 0.001 / 0.002、UMin 0.0 / UMax 0.5 = MusouFuuin の左半分）
 *   - 常にカメラへ正対（原版は TESR に渡る相対座標から viewYaw/viewPitch を atan2 で算出していた。
 *     相対座標をそのまま使う当時の実装意図は「プレイヤーの方を向く」なので、
 *     1.20.1 では {@code cameraOrientation()} で同じ結果になる）
 *   - 1 枚目と 2 枚目は time_r 由来の揺らぎで四隅を動かす（3 枚目は固定 ±0.4）
 *   - 全体を 0.8 + sin(time_r*5)*0.16 倍（上下に呼吸するように伸縮）。原版はさらに Z を 0 倍に潰していたが、
 *     1.20.1 では板自体が Z 平面なのでスケールを掛けない（見た目は同じ）
 *   - ブレンド GL_ONE / GL_ONE_MINUS_SRC_COLOR（→ {@link THRenderTypes#glow}）+ 深度書き込みなし
 *   - 常に全輝度（原版 0xf000f0）、影なし
 *   - 時間は worldTime + x*y*z（原版どおりブロックごとに位相がずれる）
 *   - 色はメタデータ 8 値 → 1.20.1 は {@link BlockDivineSpirit#COLOR}
 */
public class RenderDivineSpiritBlockEntity implements BlockEntityRenderer<TileEntityDivineSpirit>
{
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/shot/musoufuuin.png");

    /* 原版 colorR/colorG/colorB（色番号 0..7 = 赤/青/緑/黄/紫/水/橙/白） */
    private static final float[] COLOR_R = {224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F, 255F / 255F};
    private static final float[] COLOR_G = {0F / 255F, 0F / 255F, 224F / 255F, 64F / 255F, 0F / 255F, 224F / 255F, 128F / 255F, 255F / 255F};
    private static final float[] COLOR_B = {0F / 255F, 224F / 255F, 0F / 255F, 0F / 255F, 224F / 255F, 224F / 255F, 0F / 255F, 255F / 255F};

    public RenderDivineSpiritBlockEntity(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(TileEntityDivineSpirit blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        Level level = blockEntity.getLevel();
        if (level == null)
        {
            return;
        }

        int color = colorIndex(blockEntity.getBlockState().getValue(BlockDivineSpirit.COLOR));
        int cr = (int)(COLOR_R[color] * 255F);
        int cg = (int)(COLOR_G[color] * 255F);
        int cb = (int)(COLOR_B[color] * 255F);

        // 原版: int time = (int)world.getWorldTime() + x*y*z
        int time = (int)level.getGameTime()
                + blockEntity.getBlockPos().getX() * blockEntity.getBlockPos().getY() * blockEntity.getBlockPos().getZ();
        double time_r = (double)time / 180.0D * Math.PI;

        // 浮遊（原版 sin(time * 5F / 180F * PI) * 0.16F）
        double bob = Math.sin((double)time * 5.0D / 180.0D * Math.PI) * 0.16D;
        // 呼吸（原版 0.8F + sin(time_r * 5.0D) * 0.16F）
        float size = 0.8F + (float)Math.sin(time_r * 5.0D) * 0.16F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D + bob, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(size, size, size);

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
    }

    /** 原版メタデータ順（赤/青/緑/黄/紫/水/橙/白）の色番号へ */
    private static int colorIndex(DyeColor dyeColor)
    {
        switch (dyeColor)
        {
            case RED:    return 0;
            case BLUE:   return 1;
            case GREEN:  return 2;
            case YELLOW: return 3;
            case PURPLE: return 4;
            case CYAN:   return 5;
            case ORANGE: return 6;
            default:     return 7;   // WHITE
        }
    }

    /** 原版 Tessellator の 1 クアッド分（RenderDivineSpirit と同じ頂点順・法線） */
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
