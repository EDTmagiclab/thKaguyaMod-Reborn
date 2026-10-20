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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.effect.EntityHakurouReflecter;

/**
 * 白楼反射盾渲染器（1.20.1 移植版，阶段 4-6）
 *
 * 原版 RenderHakurouReflecter：3 圈 × 16 段的扇形光刃（绕中心旋转），
 * 每段是从弧边两点收敛到原点的三角形，颜色白色 alpha 随段递增。
 * 贴图 hakuroureflect.png 左半。
 */
public class RenderHakurouReflecter extends EntityRenderer<EntityHakurouReflecter>
{
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/misc/hakuroureflect.png");

    public RenderHakurouReflecter(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityHakurouReflecter entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityHakurouReflecter entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 原版绕 Y 转 yaw + 绕 X 转 rotationPitch
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        float minU = 0.0F;
        float maxU = 0.5F;
        float angle = entity.tickCount / 5F;
        float span = (float)Math.PI / 8F;
        double length = 1.8D;

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX, false, false));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        for(int i = 0; i < 3; i++)
        {
            double xPos1 = Math.cos(angle) * length;
            double xPos2 = Math.cos(angle + span) * length;
            double yPos1 = Math.sin(angle) * length;
            double yPos2 = Math.sin(angle + span) * length;
            for(int j = 0; j < 16; j++)
            {
                int a = (int)((0.2F + j * 0.3F) * 255.0F);

                consumer.addVertex(matrix, (float)xPos1, (float)yPos1, 0.0F).setColor(255, 255, 255, a)
                        .setUv(minU, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(0.0F, 0.0F, 1.0F);
                consumer.addVertex(matrix, (float)xPos2, (float)yPos2, 0.0F).setColor(255, 255, 255, a)
                        .setUv(maxU, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(0.0F, 0.0F, 1.0F);
                consumer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, a)
                        .setUv(maxU, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(0.0F, 0.0F, 1.0F);
                consumer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, a)
                        .setUv(minU, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(0.0F, 0.0F, 1.0F);

                angle += span;
                xPos1 = Math.cos(angle) * length;
                xPos2 = Math.cos(angle + span) * length;
                yPos1 = Math.sin(angle) * length;
                yPos2 = Math.sin(angle + span) * length;
            }
            length -= 0.3D;
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
