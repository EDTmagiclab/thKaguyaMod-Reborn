package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntityYuukaParasol;

/**
 * 幽香の日傘の描画（阶段 4-6：ModelYuukaParasol + 18 伞面替换 billboard）。
 * 原版：18 圈伞面 quad（绕 Y 转 20°）+ scale(0.3,0.4,0.3) 的伞柄模型（取手/骨部）。
 */
public class RenderYuukaParasol extends EntityRenderer<EntityYuukaParasol>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/yuuka_parasol_texture.png");

    private final EntityModel<EntityYuukaParasol> model;

    public RenderYuukaParasol(EntityRendererProvider.Context context, EntityModel<EntityYuukaParasol> model)
    {
        super(context);
        this.model = model;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityYuukaParasol entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityYuukaParasol parasol, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.translate(0.0F, -2.4F, 0.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(parasol.getXRot()));

        float parasolUMin = 0.5F, parasolUMax = 0.75F;
        float parasolVMin = 0.0F, parasolVMax = 0.5F;
        float parasolLength = 2.2F, parasolWidth = 0.5F;
        float stickHeight = 2.7F;
        float angleSpan = 360.0F / 18.0F;

        poseStack.scale(0.7F, 0.7F, 0.7F);
        VertexConsumer panelConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();

        for (int i = 0; i < 18; i++)
        {
            quad(panelConsumer, matrix, normal, packedLight,
                     0.0F, stickHeight, 0.0F, 0.0F, stickHeight, 0.0F,
                     parasolWidth, parasolLength, -1.0F, -parasolWidth, parasolLength, -1.0F,
                     parasolUMin, parasolVMax, parasolUMax, parasolVMax, parasolUMax, parasolVMin, parasolUMin, parasolVMin);
            quad(panelConsumer, matrix, normal, packedLight,
                     -parasolWidth, parasolLength, -1.0F, parasolWidth, parasolLength, -1.0F,
                     parasolWidth * 2.0F, parasolLength - 0.5F, -1.7F, -parasolWidth * 2.0F, parasolLength - 0.5F, -1.7F,
                     parasolUMin, parasolVMax, parasolUMax, parasolVMax, parasolUMax, parasolVMin, parasolUMin, parasolVMin);
            poseStack.mulPose(Axis.YP.rotationDegrees(angleSpan));
        }

        // 伞柄模型（取手部、骨部）
        poseStack.scale(0.3F, 0.4F, 0.3F);
        VertexConsumer modelConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, modelConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
        super.render(parasol, yaw, partialTick, poseStack, buffer, packedLight);
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
