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
import thKaguyaMod.entity.effect.EntitySpellCardCircle;

/**
 * 魔法阵渲染器（1.20.1 移植版，阶段 4-4 完整化）
 *
 * 原版 RenderSpellCardCircle：billboard 用 Familiar.png，按 circleType 决定颜色与朝向。
 *   - circleType >= 16 ：面向玩家 + 摆动 + 自旋（boss 符卡，type=16+颜色）
 *   - 否则            ：平躺地面（绕 X 90°）+ 自旋（符卡 EntitySpellCard，type=颜色）
 *   - circleType == 9  ：彩虹循环（DanmakuConstants.RAINBOW=9）
 *   - 否则 color %= 8  ：7 色渐变表染色（红橙黄绿蓝青品红 + 白兜底）
 */
public class RenderSpellCardCircle extends EntityRenderer<EntitySpellCardCircle>
{
    /* 魔法阵 7 色渐变（原版 colorR/G/B，红橙黄绿蓝青品红）+ 白（兜底，避免 WHITE=7 越界） */
    private static final int[] COLOR_R = {255, 255, 255,   0,   0,   0, 255, 255};
    private static final int[] COLOR_G = {  0, 165, 255, 255,   0, 255,   0, 255};
    private static final int[] COLOR_B = {  0,   0,   0,   0, 255, 255, 255, 255};

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/mob/familiar.png");

    public RenderSpellCardCircle(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySpellCardCircle entity)
    {
        return TEX;
    }

    @Override
    public void render(EntitySpellCardCircle entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        float size = entity.getCircleSize() * 6.0F;
        int color = entity.getCircleType();

        poseStack.pushPose();
        poseStack.scale(size, size, size);

        boolean rainbow = false;
        if(color >= 16)
        {
            // boss 符卡：面向玩家 + 摆动 + 自旋（原版 rotate 180-playerViewY+angle / -playerViewX-angle / anim*5）
            float angle = Mth.sin(2F * entity.getAnimationCount() / 180F * (float)Math.PI) * 40F;
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getAnimationCount() * 5F));
            color %= 16;
        }
        else
        {
            // 普通符卡：平躺地面（绕 X 90°）+ 自旋
            poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getAnimationCount() * 5F));
        }

        if(color == 9)
        {
            rainbow = true;
        }
        else
        {
            color %= 8;
        }

        int r, g, b;
        if(rainbow)
        {
            float t = entity.tickCount / 3.0F;
            r = (int)(Math.max(0.0F, Mth.sin(t)) * 255.0F);
            g = (int)(Math.max(0.0F, Mth.cos(t)) * 255.0F);
            b = (int)(Math.max(0.0F, -Mth.sin(t) * 2.0F) * 255.0F);
        }
        else
        {
            r = COLOR_R[color];
            g = COLOR_G[color];
            b = COLOR_B[color];
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        // 原版加色混合（GL_ONE, GL_ONE_MINUS_SRC_COLOR）→ entityTranslucent 半透明
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX, false, true));

        // billboard quad：(-0.6,-0.6) → (0.6,0.6)，全图 UV，颜色 tint，alpha 1.0
        consumer.addVertex(matrix, -0.6F, -0.6F, 0.0F).setColor(r, g, b, 255)
                .setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix,  0.6F, -0.6F, 0.0F).setColor(r, g, b, 255)
                .setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix,  0.6F,  0.6F, 0.0F).setColor(r, g, b, 255)
                .setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, -0.6F,  0.6F, 0.0F).setColor(r, g, b, 255)
                .setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
