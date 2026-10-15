package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.shot.EntityTHLaser;

/**
 * 激光渲染器（1.20.1 移植版，阶段 3d-1 最小实现）
 *
 * 面向相机的 billboard 长条，沿激光方向拉伸 laserLength。
 * 阶段 4 再完善为原版的多段激光（RenderTHLaser 723 行，含渐变/尾焰）。
 */
public class RenderTHLaser extends EntityRenderer<EntityTHLaser>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/laser.png");

    public RenderTHLaser(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHLaser entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityTHLaser entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();

        // 面向相机
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float width = entity.getShotSize();
        float length = (float)entity.getLaserLength();
        if(length < 0.1F)
        {
            length = 0.1F;
        }
        // 沿相机 Z 轴拉伸成长条
        poseStack.scale(width, width, length);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float half = 0.5F;

        consumer.vertex(matrix, -half, -half, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, -half, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, half, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half, half, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
