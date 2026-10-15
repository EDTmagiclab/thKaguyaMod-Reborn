package thKaguyaMod.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import thKaguyaMod.entity.effect.EntityAjaRedStoneEffect;

import java.util.Random;

/**
 * 艾雅赤石蓄光效果渲染器（1.20.1 移植版，阶段 4-6）
 *
 * 原版 RenderAjaRedStoneEffect：面向相机的辐射光线束（加色混合），
 * 每条光线是细长三角（底部宽 ±widthX，收敛到尖端 y=lengthY2）。
 * 颜色红色随机（闪烁），UV 取 MiracleCircle.png 右侧子图。
 */
public class RenderAjaRedStoneEffect extends EntityRenderer<EntityAjaRedStoneEffect>
{
    private static final ResourceLocation TEX =
            new ResourceLocation(THKaguyaCore.MODID, "textures/misc/miraclecircle.png");

    private final Random random = new Random();

    public RenderAjaRedStoneEffect(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityAjaRedStoneEffect entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityAjaRedStoneEffect entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 面向相机（原版 -playerViewY / playerViewX）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        int lightNum = entity.getLightPower();
        lightNum = lightNum * lightNum;
        if(lightNum <= 0)
        {
            poseStack.popPose();
            return;
        }

        float widthX = 0.01F;
        float lengthY = (float)lightNum / 120.0F;
        float angleSpan = 360F / (float)lightNum;

        float umin = 0.5F;
        float umax = 38F / 64F;
        float vmin = 0.0F;
        float vmax = 1.0F;

        float iniAngle = (float)random.nextInt(36000) / 100F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(iniAngle / 180F * (float)Math.PI) / (float)Math.PI * 180F));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEX));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        for(int i = 0; i < lightNum; i++)
        {
            float lengthY2 = lengthY * (0.2F + (float)random.nextInt(8000) / 8000.0F);
            float zPos = (float)random.nextInt(10000) / 10000.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(angleSpan / 180F * (float)Math.PI) / (float)Math.PI * 180F));

            // 重新取矩阵（旋转后）
            pose = poseStack.last();
            matrix = pose.pose();
            normal = pose.normal();

            float r = (float)random.nextInt(10000) / 10000.0F;
            int cr = (int)(r * 255.0F);

            consumer.vertex(matrix, -widthX, 0.0F, 0.0F).color(cr, 0, 0, 178)
                    .uv(umin, vmax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            consumer.vertex(matrix, widthX, 0.0F, 0.0F).color(cr, 0, 0, 178)
                    .uv(umax, vmax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            consumer.vertex(matrix, 0.0F, lengthY2, zPos).color(cr, 0, 0, 178)
                    .uv(umax, vmin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            consumer.vertex(matrix, 0.0F, lengthY2, zPos).color(cr, 0, 0, 178)
                    .uv(umin, vmin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
