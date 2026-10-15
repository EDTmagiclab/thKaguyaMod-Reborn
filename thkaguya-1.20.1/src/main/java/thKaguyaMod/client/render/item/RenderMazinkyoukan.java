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
import thKaguyaMod.entity.item.EntityMazinkyoukan;

/**
 * 魔人経巻の描画（阶段 4-6：程序化经文带替换 billboard）。
 * 原版：绕圈的文字经卷 + 左右取手，加色混合退化 entityTranslucent。
 */
public class RenderMazinkyoukan extends EntityRenderer<EntityMazinkyoukan>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/mazinkyoukan_texture.png");

    public RenderMazinkyoukan(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMazinkyoukan entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityMazinkyoukan mazinkyoukan, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();

        int count = mazinkyoukan.count;
        float angle = mazinkyoukan.getYRot() - 57.0F;
        float py = -0.3F;
        float widthY = 0.3F;
        float widthY2 = widthY + 0.1F;

        // 取手 左侧
        float px2min = -sinDeg(angle - 2.0F) * 1.55F;
        float px2max = -sinDeg(angle + 2.0F) * 1.65F;
        float pz2min =  cosDeg(angle - 2.0F) * 1.55F;
        float pz2max =  cosDeg(angle + 2.0F) * 1.65F;
        quad(consumer, matrix, normal, packedLight, 1, 1, 1, 1,
                px2min, py - widthY2, pz2min, px2max, py - widthY2, pz2max,
                px2max, py + widthY2, pz2max, px2min, py + widthY2, pz2min,
                0.875F, 1.0F, 1.0F, 1.0F, 1.0F, 0.5F, 0.875F, 0.5F);

        // 经文带
        float px = -sinDeg(angle) * 1.6F;
        double pz = cosDeg(angle) * 1.6D;
        float nextpx = -sinDeg(angle + 1.0F) * 1.6F;
        double nextpz = cosDeg(angle + 1.0F) * 1.6D;
        float colorRev = (float)Math.PI * 3.0F / 180.0F;
        float ticks = (float)mazinkyoukan.tickCount;

        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        for (int i = 0; i < count; i++)
        {
            float color = (angle + ticks) * colorRev;
            float r = Math.max(0.0F, (float)Math.sin(color));
            float g = Math.max(0.0F, (float)Math.cos(color));
            float b = Math.max(0.0F, -(float)Math.sin(color));
            float umin = (float)(i % 63) / 64.0F;
            float umax = (float)((i + 1) % 63) / 64.0F;
            float vmin = (float)(i / 63) * 16.0F / 32.0F;
            float vmax = vmin + 0.5F;
            quad(glowConsumer, matrix, normal, packedLight, r, g, b, 0.5F,
                    px, py - widthY, (float)pz, nextpx, py - widthY, (float)nextpz,
                    nextpx, py + widthY, (float)nextpz, px, py + widthY, (float)pz,
                    umin, vmax, umax, vmax, umax, vmin, umin, vmin);
            angle += 1.0F;
            px = nextpx;
            pz = nextpz;
            nextpx = -sinDeg(angle + 1.0F) * 1.6F;
            nextpz = cosDeg(angle + 1.0F) * 1.6D;
        }

        // 取手 右侧
        px2min = -sinDeg(angle - 2.0F) * 1.55F;
        px2max = -sinDeg(angle + 2.0F) * 1.65F;
        pz2min =  cosDeg(angle - 2.0F) * 1.55F;
        pz2max =  cosDeg(angle + 2.0F) * 1.65F;
        quad(consumer, matrix, normal, packedLight, 1, 1, 1, 1,
                px2min, py - widthY2, pz2min, px2max, py - widthY2, pz2max,
                px2max, py + widthY2, pz2max, px2min, py + widthY2, pz2min,
                1.0F, 1.0F, 0.875F, 1.0F, 0.875F, 0.5F, 1.0F, 0.5F);

        poseStack.popPose();
        super.render(mazinkyoukan, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static float sinDeg(float deg)
    {
        return (float)Math.sin(deg / 180.0F * Math.PI);
    }

    private static float cosDeg(float deg)
    {
        return (float)Math.cos(deg / 180.0F * Math.PI);
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
