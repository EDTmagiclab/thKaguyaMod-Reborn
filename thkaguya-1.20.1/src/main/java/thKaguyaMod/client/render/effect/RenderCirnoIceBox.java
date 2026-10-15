package thKaguyaMod.client.render.effect;

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
import thKaguyaMod.entity.effect.EntityCirnoIceBox;

/**
 * 琪露诺冰柜渲染器（1.20.1 移植版，阶段 4-6）
 *
 * 原版 RenderCirnoIceBox：6 面半透明冰长方体（顶点表 + 6 面拓扑），
 * 尺寸由 getIceBoxWidth/getIceBoxHeight 决定，贴图 MC 原版 ice.png。
 */
public class RenderCirnoIceBox extends EntityRenderer<EntityCirnoIceBox>
{
    private static final ResourceLocation ICE =
            new ResourceLocation("minecraft", "textures/block/ice.png");

    public RenderCirnoIceBox(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCirnoIceBox entity)
    {
        return ICE;
    }

    @Override
    public void render(EntityCirnoIceBox iceBox, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();

        float width = iceBox.getIceBoxWidth() / 2.0F + 0.4F;
        float height = iceBox.getIceBoxHeight() + 0.8F;

        // 原版顶点表（8 顶点长方体）
        float[] px = {-width, width, width, -width, -width, width, width, -width};
        float[] py = {height, height, height, height, 0.0F, 0.0F, 0.0F, 0.0F};
        float[] pz = {width, width, -width, -width, -width, -width, width, width};

        // 原版 6 面拓扑
        int[][] p = {
                {0, 1, 2, 3},
                {3, 2, 5, 4},
                {2, 1, 6, 5},
                {1, 0, 7, 6},
                {0, 3, 4, 7},
                {4, 5, 6, 7}
        };

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(ICE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        for(int i = 0; i < 6; i++)
        {
            float x0 = px[p[i][0]], y0 = py[p[i][0]], z0 = pz[p[i][0]];
            float x1 = px[p[i][1]], y1 = py[p[i][1]], z1 = pz[p[i][1]];
            float x2 = px[p[i][2]], y2 = py[p[i][2]], z2 = pz[p[i][2]];
            float x3 = px[p[i][3]], y3 = py[p[i][3]], z3 = pz[p[i][3]];

            consumer.vertex(matrix, x0, y0, z0).color(255, 255, 255, 255)
                    .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            consumer.vertex(matrix, x1, y1, z1).color(255, 255, 255, 255)
                    .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            consumer.vertex(matrix, x2, y2, z2).color(255, 255, 255, 255)
                    .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            consumer.vertex(matrix, x3, y3, z3).color(255, 255, 255, 255)
                    .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                    .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        }

        poseStack.popPose();
        super.render(iceBox, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
