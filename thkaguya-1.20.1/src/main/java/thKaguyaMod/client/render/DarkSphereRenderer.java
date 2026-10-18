package thKaguyaMod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;

/**
 * 原版の「暗闇（DarkTexture）球」描画の移植。
 *
 * <p>1.7.10 の各 Renderer は {@code renderDark(Tessellator, length, width, zPos, alpha, time)} で
 * 半径 {@code width/2} の球面（Z 軸回り 18 分割 × 奥行き 9 分割）を描いていた。
 * 移植版ではこの球体描画が丸ごと欠落しており（{@code textures/entity/dark_texture.png} が
 * どのコードからも参照されない死リソースになっていた）、ルーミアの闇・霊撃札の暗幕が出ていなかった。
 *
 * <p>1.20.1 には任意のブレンド関数を使う RenderType が無いため、
 * {@link Tesselator} + {@link RenderSystem#blendFunc} で直接描画する。
 * 呼び出し側は「ワールド座標での原点」に {@link PoseStack} を合わせてから呼ぶこと。
 */
public final class DarkSphereRenderer
{
    /** 原版 textures/DarkTexture.png（移植版では entity 配下に配置） */
    public static final ResourceLocation DARK_TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/dark_texture.png");

    /** ブレンド方法（原版の GL11.glBlendFunc に対応） */
    public enum Blend
    {
        /** ZERO / ONE_MINUS_SRC_COLOR：ルーミアの闇（周囲を暗くする） */
        DARKEN,
        /** SRC_ALPHA / ONE：霊撃札の暗幕（加算） */
        ADDITIVE
    }

    private DarkSphereRenderer()
    {
    }

    /**
     * 原版 renderDark と同一の球体を描く。
     *
     * @param poseStack ワールド座標に合わせた PoseStack
     * @param width     球の直径（原版の width 引数）
     * @param alpha     頂点色のアルファ（0..1）
     * @param blend     ブレンド方法
     */
    public static void render(PoseStack poseStack, float width, float alpha, Blend blend)
    {
        final int zAngleDivNum = 18;                       // Z 軸回転の分割数
        final double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;
        final int zDivNum = 9;                             // 奥行き方向の分割数（奇数）
        final float angleSpan = (float)Math.PI / (float)zDivNum;

        float maxWidth = width / 2.0F;
        float angle = -(float)Math.PI / 2.0F + angleSpan;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, DARK_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (blend == Blend.ADDITIVE)
        {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
        else
        {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        }

        int alphaByte = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
        var matrix = poseStack.last().pose();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // 奥行き初期値（原版 zPos = sin(-π/2) * maxWidth）
        double zPos = -maxWidth;
        double zPosOld = -maxWidth;
        float widthOld = 0.0F;

        for (int j = 0; j < zDivNum; j++)
        {
            zPos = Math.sin(angle) * maxWidth;
            width = (float)Math.cos(angle) * maxWidth;

            float xPos = width;
            float yPos = 0.0F;
            double angleZ = 0.0D;
            float xPosOld = (float)Math.cos(angleZ) * width;
            float yPosOld = (float)Math.sin(angleZ) * width;
            float xPos2Old = (float)Math.cos(angleZ) * widthOld;
            float yPos2Old = (float)Math.sin(angleZ) * widthOld;
            angleZ = angleSpanZ;

            for (int i = 1; i <= zAngleDivNum; i++)
            {
                xPos = (float)Math.cos(angleZ) * width;
                yPos = (float)Math.sin(angleZ) * width;
                float xPos2 = (float)Math.cos(angleZ) * widthOld;
                float yPos2 = (float)Math.sin(angleZ) * widthOld;

                vertex(buffer, matrix, xPos, yPos, zPos, 1.0F, 0.0F, alphaByte);
                vertex(buffer, matrix, xPosOld, yPosOld, zPos, 0.0F, 0.0F, alphaByte);
                vertex(buffer, matrix, xPos2Old, yPos2Old, zPosOld, 0.0F, 1.0F, alphaByte);
                vertex(buffer, matrix, xPos2, yPos2, zPosOld, 1.0F, 1.0F, alphaByte);

                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }

            zPosOld = zPos;
            angle += angleSpan;
            widthOld = width;
        }

        tessellator.end();

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void vertex(BufferBuilder buffer, org.joml.Matrix4f matrix,
            float x, float y, double z, float u, float v, int alpha)
    {
        buffer.vertex(matrix, x, y, (float)z).uv(u, v).color(255, 255, 255, alpha).endVertex();
    }
}
