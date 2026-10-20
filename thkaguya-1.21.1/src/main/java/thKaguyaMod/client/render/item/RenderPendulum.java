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
import thKaguyaMod.entity.item.EntityPendulum;

/**
 * ナズーリンペンデュラムの描画（阶段 4-6：程序化摆锤替换 billboard）。
 * 原版：4 面锥形锤体（90° 旋转）+ 4 面中间带（45° 旋转，双面），加色混合退化 translucent。
 */
public class RenderPendulum extends EntityRenderer<EntityPendulum>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/entity/pendulum_texture.png");

    public RenderPendulum(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPendulum entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityPendulum pendulum, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();

        float rotationLength = (float)pendulum.getSearchCount() / 48.0F;
        float xDrawPos = -(float)Math.sin(pendulum.getSearchAngle() / 180.0F * Math.PI) * rotationLength;
        float zDrawPos =  (float)Math.cos(pendulum.getSearchAngle() / 180.0F * Math.PI) * rotationLength;
        poseStack.translate(xDrawPos, 0.0F, zDrawPos);

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(0.3F, 0.3F, 0.3F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();

        float uMin = 16.0F / 64.0F, vMin = 20.0F / 32.0F;
        float uMax = 28.0F / 64.0F, vMax = 32.0F / 32.0F;
        float topP1u = 12.0F / 64.0F, topP1v = 4.0F / 32.0F;
        float topP2u = 1.0F / 64.0F,  topP2v = 19.0F / 32.0F;
        float topP3u = 22.0F / 64.0F, topP3v = 19.0F / 32.0F;
        float downP1u = 33.0F / 64.0F, downP1v = 0.0F;
        float downP2u = 55.0F / 64.0F, downP2v = 0.0F;
        float downP3u = 44.0F / 64.0F, downP3v = 31.0F / 32.0F;

        for (int i = 0; i < 4; i++)
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            quad(consumer, matrix, normal, packedLight,
                     0.0F, 1.75F, 0.0F, 0.0F, 1.75F, 0.0F,
                     0.3F, 1.25F, -0.3F, -0.3F, 1.25F, -0.3F,
                     topP1u, topP1v, topP1u, topP1v, topP3u, topP3v, topP2u, topP2v);
            quad(consumer, matrix, normal, packedLight,
                     -0.3F, 1.25F, -0.3F, 0.3F, 1.25F, -0.3F,
                     0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F,
                     downP1u, downP1v, downP2u, downP2v, downP3u, downP3v, downP3u, downP3v);
        }

        for (int i = 0; i < 4; i++)
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            quad(consumer, matrix, normal, packedLight,
                     -0.2F, 1.90F, 0.0F, 0.2F, 1.90F, 0.0F,
                     0.2F, 1.60F, 0.0F, -0.2F, 1.60F, 0.0F,
                     uMin, vMax, uMax, vMax, uMax, vMin, uMin, vMin);
        }

        poseStack.popPose();
        super.render(pendulum, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer c, org.joml.Matrix4f matrix, org.joml.Matrix3f normal,
            int light,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3)
    {
        c.addVertex(matrix, x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(matrix, x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        c.addVertex(matrix, x3, y3, z3).setColor(255, 255, 255, 255).setUv(u3, v3)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }
}
