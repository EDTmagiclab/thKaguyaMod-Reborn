package thKaguyaMod.client.render.shot;

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
import thKaguyaMod.entity.shot.EntityTHLaser;
import thKaguyaMod.entity.shot.EntityTHSetLaser;

/**
 * 激光渲染器（1.20.1 移植版，阶段 4-2 完整化）
 *
 * 原版 RenderTHLaser（723 行）用 Tessellator 画多段圆柱体激光。这里按 shotId 分支：
 *   - shotId 0-7   ：普通激光（梭形圆柱体，白色核心 + 彩色外层双 pass）
 *   - shotId 8-15  ：冈格尼尔之枪（Gungnir，物品触发，暂兜底普通激光）
 *   - shotId 16-23 ：莱瓦汀（Laevateinn，物品触发，暂兜底普通激光）
 *   - shotId 24-31 ：魔理沙 MasterSpark（彩虹光柱）
 *   - animationCount < 0（非 SetLaser）：蓄力光效
 *
 * 激光方向 = 实体 yaw/pitch（同弹体 renderArrowShot 的朝向变换），沿 +Z 延伸。
 */
public class RenderTHLaser extends EntityRenderer<EntityTHLaser>
{
    /* 8 色顶点染色表（与 RenderTHShot 一致） */
    private static final int[] COLOR_R = {224,   0,   0, 224, 224,   0, 255, 255};
    private static final int[] COLOR_G = {  0,   0, 224, 224,   0, 224, 128, 255};
    private static final int[] COLOR_B = {  0, 224,   0,   0, 224, 224,   0, 255};

    private static final ResourceLocation TEX_LASER = tex("textures/shot/laser.png");
    private static final ResourceLocation TEX_LIGHT  = tex("textures/shot/musoufuuin.png");

    private static ResourceLocation tex(String path)
    {
        return new ResourceLocation(THKaguyaCore.MODID, path);
    }

    public RenderTHLaser(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHLaser entity)
    {
        return TEX_LASER;
    }

    @Override
    public void render(EntityTHLaser entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int shotId = entity.getShotId();
        int color = shotId % 8;
        double length = entity.getLaserLength();
        float width = entity.getShotSize();
        if(length < 0.1D)
        {
            length = 0.1D;
        }

        poseStack.pushPose();

        // 激光朝向：原版 glRotatef(yaw,0,1,0) + glRotatef(-pitch,1,0,0)，沿 +Z 延伸
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));

        if(entity.getAnimationCount() < 0 && !(entity instanceof EntityTHSetLaser))
        {
            // 蓄力阶段：光效（原版 renderLightEffect）
            renderLightEffect(entity, color, poseStack, buffer, packedLight);
        }
        else if(shotId >= 24)
        {
            // 魔理沙 MasterSpark：彩虹光柱（原版 renderMasterSparkLaser）
            renderMasterSpark(entity, length, width, poseStack, buffer, packedLight);
        }
        else
        {
            // 普通激光（shotId 0-7）；Gungnir(8-15)/Laevateinn(16-23) 物品触发，
            // 专属顶点模型留待阶段 4-6，此处兜底为普通激光
            renderNormalLaser(entity, length, width, color, poseStack, buffer, packedLight);
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    // ============================================================
    // 普通激光：梭形圆柱体（13 段、8 边形截面，cos 宽度两头尖中间粗）
    // 白色核心（原版 alpha 1.0，size=shotSize）+ 彩色外层（alpha 0.6，size*1.2）
    // ============================================================
    private void renderNormalLaser(EntityTHLaser entity, double length, float width, int color,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];

        // 白色核心（不透明）
        VertexConsumer core = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_LASER));
        renderCylinder(core, poseStack, packedLight, length, width, 0.1D, 255, 255, 255, 255);

        // 彩色外层（半透明，1.2 倍，原版加色混合退化为半透明）
        VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucent(TEX_LASER));
        renderCylinder(glow, poseStack, packedLight, length * 1.2D, width * 1.2F, 0.0D, r, g, b, 153);
    }

    /**
     * 梭形圆柱体（原版 renderLaser 锥形重载）。
     * @param zStart 激光起点沿 Z 的偏移（原版 centerZ1-centerZ2）
     */
    private void renderCylinder(VertexConsumer consumer, PoseStack poseStack, int packedLight,
                                double length, float radius, double zStart,
                                int r, int g, int b, int a)
    {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        int zAngleDivNum = 8;                       // 8 边形截面
        double angleSpanZ = Math.PI * 2.0D / zAngleDivNum;
        int zDivNum = 13;                           // 沿 Z 分 13 段
        double zDivLength = length / (double)(zDivNum - 1);
        double zPosOld = zStart;

        float angle = -(float)Math.PI / 2.0F;       // cos 从 0 → 1 → 0（梭形）
        float angleSpan = (float)Math.PI / (float)zDivNum;
        angle += angleSpan;
        float widthOld = 0.0F;

        for(int j = 0; j < zDivNum; j++)
        {
            double zPos = zPosOld + zDivLength;
            float width = (float)Math.cos(angle) * radius;
            if(width < 0.0F) width = 0.0F;

            double angleZ = 0.0D;
            float xPosOld = (float)Math.cos(angleZ) * width;
            float yPosOld = (float)Math.sin(angleZ) * width;
            float xPos2Old = (float)Math.cos(angleZ) * widthOld;
            float yPos2Old = (float)Math.sin(angleZ) * widthOld;
            angleZ = angleSpanZ;

            for(int i = 0; i <= zAngleDivNum; i++)
            {
                float xPos = (float)Math.cos(angleZ) * width;
                float yPos = (float)Math.sin(angleZ) * width;
                float xPos2 = (float)Math.cos(angleZ) * widthOld;
                float yPos2 = (float)Math.sin(angleZ) * widthOld;

                // 侧面 quad（原版顶点顺序，双面渲染故 winding 不敏感）
                consumer.vertex(matrix, xPosOld,  yPosOld,  (float)zPos   ).color(r, g, b, a)
                        .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2Old, yPos2Old, (float)zPosOld).color(r, g, b, a)
                        .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2,    yPos2,    (float)zPosOld).color(r, g, b, a)
                        .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos,     yPos,     (float)zPos   ).color(r, g, b, a)
                        .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }

            zPosOld = zPos;
            angle += angleSpan;
            widthOld = width;
        }
    }

    // ============================================================
    // MasterSpark 彩虹光柱（单边锥形：发射端尖，末端粗）
    // 白色核心（16 边形）+ 彩虹外层（64 边形，颜色随角度/时间循环）
    // ============================================================
    private void renderMasterSpark(EntityTHLaser entity, double length, float width,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        float ticks = Math.abs((float)entity.getAnimationCount() * 10.0F);

        // 白色核心（16 边形）
        VertexConsumer core = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_LASER));
        renderMasterSparkCylinder(core, poseStack, packedLight, length, width, 255, 255, 255, 255, false, ticks);

        // 彩虹外层（64 边形，1.2 倍）
        VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucent(TEX_LASER));
        renderMasterSparkCylinder(glow, poseStack, packedLight, length, width * 1.2F, 0, 0, 0, 153, true, ticks);
    }

    private void renderMasterSparkCylinder(VertexConsumer consumer, PoseStack poseStack, int packedLight,
                                           double length, float radius,
                                           int r, int g, int b, int a, boolean rainbow, float ticks)
    {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        float maxWidth = radius * 1.6F;             // 原版 maxWidth = width * 1.6
        int zAngleDivNum = rainbow ? 64 : 16;
        double angleSpanZ = Math.PI * 2.0D / zAngleDivNum;
        int zDivNum = 13;
        double zDivLength = length / (double)(zDivNum - 1);
        double zPosOld = 0.0D;

        float angle = 0F;
        float angleSpan = (float)Math.PI / (float)(zDivNum - 1);
        float width = (float)Math.sin(angle) * maxWidth;   // = 0
        float widthOld = width;

        float colorAngle = 0F;
        float colorRev = 3.141593F * 5.6125F / 180F;

        double zPos = 0.0D;
        while(zPos < length)
        {
            zPos += zDivLength;
            widthOld = width;
            angle += angleSpan;
            width = (width < maxWidth) ? (float)Math.sin(angle) * maxWidth : maxWidth;

            // 彩虹波动（原版 width2 = width * (sin(angle+ticks/40)*0.1 + 1)）
            float width2 = width * ((float)Math.sin(angle + ticks / 40F) * 0.1F + 1.0F);
            float widthOld2 = widthOld * ((float)Math.sin(angle - angleSpan + ticks / 40F) * 0.1F + 1.0F);

            double angleZ = 0.0D;
            float xPosOld = (float)Math.cos(angleZ) * width2;
            float yPosOld = (float)Math.sin(angleZ) * width2;
            float xPos2Old = (float)Math.cos(angleZ) * widthOld2;
            float yPos2Old = (float)Math.sin(angleZ) * widthOld2;
            angleZ = angleSpanZ;

            for(int i = 0; i <= zAngleDivNum; i++)
            {
                float xPos = (float)Math.cos(angleZ) * width2;
                float yPos = (float)Math.sin(angleZ) * width2;
                float xPos2 = (float)Math.cos(angleZ) * widthOld2;
                float yPos2 = (float)Math.sin(angleZ) * widthOld2;

                int cr = r, cg = g, cb = b;
                if(rainbow)
                {
                    float c = (colorAngle + ticks) * colorRev;
                    cr = (int)(Math.max(0.0F, Mth.sin(c)) * 255.0F);
                    cg = (int)(Math.max(0.0F, Mth.cos(c)) * 255.0F);
                    cb = (int)(Math.max(0.0F, -Mth.sin(c) * 2.0F) * 255.0F);
                }

                // 原版 MasterSpark 顶点顺序（不同与普通激光）
                consumer.vertex(matrix, xPosOld,  yPosOld,  (float)zPos   ).color(cr, cg, cb, a)
                        .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos,     yPos,     (float)zPos   ).color(cr, cg, cb, a)
                        .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2,    yPos2,    (float)zPosOld).color(cr, cg, cb, a)
                        .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2Old, yPos2Old, (float)zPosOld).color(cr, cg, cb, a)
                        .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

                colorAngle++;
                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }

            zPosOld = zPos;
            ticks += 1.0F;
        }
    }

    // ============================================================
    // 蓄力光效（原版 renderLightEffect）：billboard，musoufuuin 子图 + 颜色 tint
    // ============================================================
    private void renderLightEffect(EntityTHLaser entity, int color,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int delay = -entity.getAnimationCount();
        if(delay > 10) delay = 10;
        float size = delay * entity.getShotSize() * 2.0F;
        if(size > 1.0F) size = 1.0F;

        int frame = Math.floorMod(entity.getAnimationCount(), 2);
        float uMin = (frame * 32.0F) / 64.0F;
        float uMax = (frame * 32.0F + 32.0F) / 64.0F;

        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];

        poseStack.pushPose();
        // 面向相机
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEX_LIGHT));

        // 原版 quad：(-0.5,-0.25) → (0.5,0.75)，alpha 0.5
        consumer.vertex(matrix, -0.5F, -0.25F, 0.0F).color(r, g, b, 128)
                .uv(uMin, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix,  0.5F, -0.25F, 0.0F).color(r, g, b, 128)
                .uv(uMax, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix,  0.5F,  0.75F, 0.0F).color(r, g, b, 128)
                .uv(uMax, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -0.5F,  0.75F, 0.0F).color(r, g, b, 128)
                .uv(uMin, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
    }
}
