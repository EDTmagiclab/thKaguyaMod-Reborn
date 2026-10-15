package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import org.joml.Quaternionf;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntityMiniHakkero;

/**
 * ミニ八卦炉の描画（阶段 4-6：ModelMiniHakkero×2 + 光轮替换 billboard）。
 * 原版：本体（2 盒）+ 侧面 8 面板（ModelMiniHakkero2）+ 19 圈旋转光轮（加色混合退化 translucent）。
 */
public class RenderMiniHakkero extends EntityRenderer<EntityMiniHakkero>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/mini_hakkero_texture.png");

    private static final float[] CONST_R = { 255F, 255F, 255F,   0F,   0F,  76F, 128F };
    private static final float[] CONST_G = {   0F, 165F, 255F, 128F,   0F,   0F,   0F };
    private static final float[] CONST_B = {   0F,   0F,   0F,   0F, 255F, 130F, 128F };

    private final EntityModel<EntityMiniHakkero> modelBody;
    private final EntityModel<EntityMiniHakkero> modelSide;

    public RenderMiniHakkero(EntityRendererProvider.Context context,
            EntityModel<EntityMiniHakkero> modelBody, EntityModel<EntityMiniHakkero> modelSide)
    {
        super(context);
        this.modelBody = modelBody;
        this.modelSide = modelSide;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMiniHakkero entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityMiniHakkero hakkero, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();

        // 朝向：原版绕 ( -sin(yaw-90), 0, cos(yaw-90) ) 轴转 pitch，再绕 Y 转 180-yaw
        float rad = (yaw - 90.0F) * (float)Math.PI / 180.0F;
        float ax = -(float)Math.sin(rad);
        float az = (float)Math.cos(rad);
        poseStack.mulPose(new Quaternionf().rotationAxis((float)Math.toRadians(hakkero.getXRot()), ax, 0.0F, az));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        VertexConsumer modelConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        modelBody.renderToBuffer(poseStack, modelConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.scale(1.002F, 1.002F, 1.002F);
        modelSide.renderToBuffer(poseStack, modelConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        // 19 圈光轮（加色混合退化，entityTranslucentCull）
        renderRings(hakkero, poseStack, buffer, packedLight);

        poseStack.popPose();
        super.render(hakkero, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderRings(EntityMiniHakkero hakkero, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        float uMin = 0.0F, vMin = 0.0F, uMax = 0.5F, vMax = 0.25F;
        float angle = hakkero.getCircleAngle();
        float pi18 = (float)Math.PI / 9.0F;
        int c = hakkero.tickCount;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();

        poseStack.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(angle) / 3.141593F * 180.0F));
        for (int i = 0; i < 19; i++)
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(pi18) / 3.141593F * 180.0F));

            float fr = CONST_R[c % 7] / 255F * 0.5F;
            float fg = CONST_G[c % 7] / 255F * 0.5F;
            float fb = CONST_B[c % 7] / 255F * 0.5F;
            quad(consumer, matrix, normal, packedLight, fr, fg, fb, 0.6F,
                     0.77F, 4.5F, -6.0F, -0.77F, 4.5F, -6.0F, -0.77F, 4.5F, -4.0F, 0.77F, 4.5F, -4.0F,
                     uMin, vMin, uMax, vMin, uMax, vMax, uMin, vMax);
            quad(consumer, matrix, normal, packedLight, fr, fg, fb, 0.6F,
                     -0.77F, 4.5F, -6.0F, 0.77F, 4.5F, -6.0F, 0.77F, 4.5F, -4.0F, -0.77F, 4.5F, -4.0F,
                     uMin, vMin, uMax, vMin, uMax, vMax, uMin, vMax);

            float br = CONST_R[(c + 3) % 7] / 255F * 0.5F;
            float bg = CONST_G[(c + 3) % 7] / 255F * 0.5F;
            float bb = CONST_B[(c + 3) % 7] / 255F * 0.5F;
            quad(consumer, matrix, normal, packedLight, br, bg, bb, 0.6F,
                     1.11F, 6.5F, -12.0F, -1.11F, 6.5F, -12.0F, -1.11F, 6.5F, -10.0F, 1.11F, 6.5F, -10.0F,
                     uMin, vMin, uMax, vMin, uMax, vMax, uMin, vMax);
            quad(consumer, matrix, normal, packedLight, br, bg, bb, 0.6F,
                     -1.11F, 6.5F, -12.0F, 1.11F, 6.5F, -12.0F, 1.11F, 6.5F, -10.0F, -1.11F, 6.5F, -10.0F,
                     uMin, vMin, uMax, vMin, uMax, vMax, uMin, vMax);

            angle += pi18;
        }
    }

    private static void quad(VertexConsumer c, org.joml.Matrix4f matrix, org.joml.Matrix3f normal,
            int light, float r, float g, float b, float a,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3)
    {
        c.vertex(matrix, x0, y0, z0).color(r, g, b, a).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        c.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        c.vertex(matrix, x2, y2, z2).color(r, g, b, a).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        c.vertex(matrix, x3, y3, z3).color(r, g, b, a).uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
    }
}
