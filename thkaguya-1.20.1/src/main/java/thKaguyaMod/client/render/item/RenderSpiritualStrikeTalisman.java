package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntitySpiritualStrikeTalisman;

/**
 * 霊撃札の描画（阶段 4-6：双面旋转符札替换 billboard）。
 * 原版：双面 quad + 绕 Y 自旋 21°/tick + 绕 Z 倾 30°（时停暗幕略，阶段 6 补）。
 */
public class RenderSpiritualStrikeTalisman extends EntityRenderer<EntitySpiritualStrikeTalisman>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/spiritual_strike_talisman_texture.png");

    public RenderSpiritualStrikeTalisman(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySpiritualStrikeTalisman entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntitySpiritualStrikeTalisman talisman, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((float)talisman.tickCount * 21.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();

        float rvl = 0.0F, rul = 0.0F, rvr = 0.5F, rur = 1.0F;

        // 背面
        quad(consumer, matrix, normal, packedLight,
                 0.2F,  0.4F, 0.0F, -0.2F,  0.4F, 0.0F,
                -0.2F, -0.4F, 0.0F,  0.2F, -0.4F, 0.0F,
                rvl, rul, rvr, rul, rvr, rur, rvl, rur);
        // 表面
        quad(consumer, matrix, normal, packedLight,
                -0.2F,  0.4F, 0.0F,  0.2F,  0.4F, 0.0F,
                 0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F,
                rvl, rul, rvr, rul, rvr, rur, rvl, rur);

        poseStack.popPose();
        super.render(talisman, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer c, org.joml.Matrix4f matrix, org.joml.Matrix3f normal,
            int light,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3)
    {
        c.vertex(matrix, x0, y0, z0).color(255, 255, 255, 255).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        c.vertex(matrix, x1, y1, z1).color(255, 255, 255, 255).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        c.vertex(matrix, x2, y2, z2).color(255, 255, 255, 255).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        c.vertex(matrix, x3, y3, z3).color(255, 255, 255, 255).uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
    }
}
