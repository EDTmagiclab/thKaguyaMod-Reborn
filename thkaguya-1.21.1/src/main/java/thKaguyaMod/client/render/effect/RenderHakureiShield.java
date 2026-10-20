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
import thKaguyaMod.entity.effect.EntityHakureiShield;

/**
 * 灵梦结界渲染器（1.20.1 移植版，阶段 4-6）
 *
 * 原版 RenderHakureiShield：面向相机 + 绕 Y 转 yaw 的 1.5×1.5 橙黄色符札方片。
 * 原版贴图 Amulet.png 在原版 jar 即缺失（上游遗留），回退用 homing_amulet.png 左半。
 */
public class RenderHakureiShield extends EntityRenderer<EntityHakureiShield>
{
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/item/homing_amulet.png");

    public RenderHakureiShield(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityHakureiShield entity)
    {
        return TEX;
    }

    @Override
    public void render(EntityHakureiShield entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        float minU = 0.0F;
        float maxU = 0.5F;
        float width = 1.5F;
        float height = 1.5F;

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX, false, true));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        // 原版橙黄 (0.9, 0.6, 0.1, 0.8)
        int r = 229, g = 153, b = 25, a = 204;

        consumer.addVertex(matrix, -width, -height, 0.0F).setColor(r, g, b, a)
                .setUv(minU, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, width, -height, 0.0F).setColor(r, g, b, a)
                .setUv(maxU, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, width, height, 0.0F).setColor(r, g, b, a)
                .setUv(maxU, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, -width, height, 0.0F).setColor(r, g, b, a)
                .setUv(minU, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
