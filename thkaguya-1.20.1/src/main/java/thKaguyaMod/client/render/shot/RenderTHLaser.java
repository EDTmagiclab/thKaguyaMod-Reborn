package thKaguyaMod.client.render.shot;

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
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
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

        // ============================================================
        // 激光朝向（原版 renderTHLaser）：
        //     GL11.glRotatef(yaw,   0F, 1F, 0F);
        //     GL11.glRotatef(-pitch, 1F, 0F, 0F);
        // 即 Axis.YP(yaw) + Axis.XP(-pitch)，**没有任何 180 偏移**。
        //
        // 【为什么不能写 180 - yaw】弹体/激光实体的 yaw 是由
        //   THShotLib.getYawFromVector(angle.x, angle.z) = atan2(vx, vz)
        // 得到的（自定义 (sinθ, cosθ) 参数化；同一方向向量在此约定下的 yaw 恰为 MC 原版
        // yaw 的相反数）。于是 Axis.YP(180 - yaw) 不是「绕 Y 再转 180°」而是**把方向以 X 轴
        // 镜像**：光束方向变成 (sin yaw, 0, -cos yaw)，而真实方向是 (sin yaw, 0, cos yaw)。
        //   → 正东/正西射击时两者恰好相同（yaw=±90），正南/正北时相差 180°（朝身后射）。
        //   实测症状：魔炮光柱不穿过迷你八卦炉模型上那两枚同心圆环（= 炮口轴）的中心。
        // 这个 180 是 commit a3c5366 依据「GameRenderer 施加 cameraYaw+180」的假设加的，而
        // javap 实测 EntityRenderDispatcher.render 只做 pushPose + translate，并无相机旋转
        // → 假设不成立，该偏移是纯粹的错误。
        //
        // 【为什么用 yaw/xRot 而不是 entity.angle】angle 是普通字段（未经 entityData 同步，
        // 客户端上恒为 (0,0,0)），只有 yaw/xRot 会在生成包与随后的旋转同步中到达客户端。
        // 原版渲染器同样只用 dispatcher 传入的 yaw/pitch。
        // ============================================================
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));

        // 原版：SetLaser 系が遅延中（animationCount < 0）の間は光束を 1/10 の太さで描く
        //   float width = 1.0F;
        //   if(thLaser.getAnimationCount() < 0 && thLaser instanceof EntityTHSetLaser) width = 0.1F;
        //   …各分岐で GL11.glScalef(width, width, 1.0F);
        // これが「溜め」の見た目（細い光条 → 本発射で一気に太くなる）を作っている。
        // 移植版はこの倍率が抜けていたため、溜めの段階から最大級の太さで描かれていた。
        float widthFactor = 1.0F;
        if(entity.getAnimationCount() < 0 && entity instanceof EntityTHSetLaser)
        {
            widthFactor = 0.1F;
        }

        if(entity.getAnimationCount() < 0 && !(entity instanceof EntityTHSetLaser))
        {
            // 蓄力阶段：光效（原版 renderLightEffect）
            renderLightEffect(entity, color, poseStack, buffer, packedLight);
        }
        else if(shotId >= 24)
        {
            // 魔理沙 MasterSpark：彩虹光柱（原版 renderMasterSparkLaser）
            poseStack.pushPose();
            poseStack.scale(widthFactor, widthFactor, 1.0F);
            renderMasterSpark(entity, length, width, poseStack, buffer, packedLight);
            poseStack.popPose();
        }
        else if(shotId >= 16)
        {
            // 莱瓦汀（Laevateinn，shotId 16-23）：原版 renderLaevateinn 程序化 3D 剑形几何
            // 原版 dispatch：绕 Z 轴转 angleZ（纵劈时物品侧 setAngleZ(90F)，缺了横劈/纵劈渲染无差别）
            //   + Z 向缩放 length/20.8
            //   + 白心 pass（color=7, alpha=1.0, zPos=1.6）
            //   + 彩色光晕 pass（再叠 scale 1.2³, alpha=0.6, zPos=1.0）
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getAngleZ()));
            poseStack.pushPose();
            poseStack.scale(1.0F, 1.0F, (float)(length / 20.8D));
            VertexConsumer core = buffer.getBuffer(THRenderTypes.blend(TEX_LASER, THRenderTypes.BLEND_ADDITIVE, false, true));
            renderLaevateinn(core, poseStack, packedLight, 7, 1.6F, 255, entity.tickCount + partialTicks);
            poseStack.scale(1.2F, 1.2F, 1.2F);
            VertexConsumer glow = buffer.getBuffer(THRenderTypes.glow(TEX_LASER, false, false));
            renderLaevateinn(glow, poseStack, packedLight, color, 1.0F, 153, entity.tickCount + partialTicks);
            poseStack.popPose();
        }
        else if(shotId >= 8)
        {
            // 冈格尼尔之枪（Gungnir，shotId 8-15）：原版 renderSpearTheGungnir 程序化 3D 枪形几何
            // 原版 dispatch：绕 Z 轴自旋 30°/tick + 缩放(0.3, 0.3, 0.5*length/10)
            //   + 白心 pass（color=7, alpha=1.0, zPos=centerZ1-centerZ2=length*0.1）
            //   + 彩色光晕 pass（再叠 scale 1.2³, alpha=0.6, zPos=-1.0）
            poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTicks) * 30F));
            poseStack.pushPose();
            poseStack.scale(0.3F, 0.3F, (float)(0.5D * length / 10D));
            VertexConsumer core = buffer.getBuffer(THRenderTypes.blend(TEX_LASER, THRenderTypes.BLEND_ADDITIVE, false, true));
            renderSpearTheGungnir(core, poseStack, packedLight, 7, (float)(length * 0.1D), 255);
            poseStack.scale(1.2F, 1.2F, 1.2F);
            VertexConsumer glow = buffer.getBuffer(THRenderTypes.glow(TEX_LASER, false, false));
            renderSpearTheGungnir(glow, poseStack, packedLight, color, -1.0F, 153);
            poseStack.popPose();
        }
        else
        {
            // 普通激光（shotId 0-7）
            poseStack.pushPose();
            poseStack.scale(widthFactor, widthFactor, 1.0F);
            renderNormalLaser(entity, length, width, color, poseStack, buffer, packedLight);
            poseStack.popPose();
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
        VertexConsumer core = buffer.getBuffer(THRenderTypes.blend(TEX_LASER, THRenderTypes.BLEND_ADDITIVE, true, true));
        renderCylinder(core, poseStack, packedLight, length, width, 0.1D, 255, 255, 255, 255);

        // 彩色外层（半透明，1.2 倍，原版加色混合退化为半透明）
        VertexConsumer glow = buffer.getBuffer(THRenderTypes.glow(TEX_LASER, true, false));
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
                        .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2Old, yPos2Old, (float)zPosOld).color(r, g, b, a)
                        .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2,    yPos2,    (float)zPosOld).color(r, g, b, a)
                        .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos,     yPos,     (float)zPos   ).color(r, g, b, a)
                        .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
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
        VertexConsumer core = buffer.getBuffer(THRenderTypes.blend(TEX_LASER, THRenderTypes.BLEND_ADDITIVE, false, true));
        renderMasterSparkCylinder(core, poseStack, packedLight, length, width, 255, 255, 255, 255, false, ticks);

        // 彩虹外层（64 边形，1.2 倍）
        VertexConsumer glow = buffer.getBuffer(THRenderTypes.glow(TEX_LASER, false, true));
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
                        .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos,     yPos,     (float)zPos   ).color(cr, cg, cb, a)
                        .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2,    yPos2,    (float)zPosOld).color(cr, cg, cb, a)
                        .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, xPos2Old, yPos2Old, (float)zPosOld).color(cr, cg, cb, a)
                        .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
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

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additive(TEX_LIGHT));

        // 原版 quad：(-0.5,-0.25) → (0.5,0.75)，alpha 0.5
        consumer.vertex(matrix, -0.5F, -0.25F, 0.0F).color(r, g, b, 128)
                .uv(uMin, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix,  0.5F, -0.25F, 0.0F).color(r, g, b, 128)
                .uv(uMax, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix,  0.5F,  0.75F, 0.0F).color(r, g, b, 128)
                .uv(uMax, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -0.5F,  0.75F, 0.0F).color(r, g, b, 128)
                .uv(uMin, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
    }

    // ============================================================
    // 冈格尼尔之枪（Gungnir）：原版 renderSpearTheGungnir
    // 29 顶点 + 24 quad（4 重镜像：原/+x/-y/-x-y 共 96 quad）
    // 注：原版 length/width 参数未对顶点表缩放（缩放由 dispatch 侧 poseStack.scale 完成），
    //     zPos 在顶点 z 上平移后再经受 scale（与原版 GL 矩阵语义一致）
    // ============================================================
    private void renderSpearTheGungnir(VertexConsumer consumer, PoseStack poseStack, int packedLight,
                                        int color, float zPos, int alpha)
    {
        color %= 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];

        // 原版顶点表（直接照搬）
        final double[][] V = {
            { 0.000000,  0.681531, -5.956767},
            { 0.000000,  0.999996,  6.000000},
            {-0.681527,  0.000004, -5.956767},
            {-1.000000, -0.000004,  6.000000},
            {-0.481912,  0.481916, -5.956767},
            {-0.707107,  0.707103,  6.000000},
            {-0.000000,  1.957804,  8.160793},
            {-1.733818, -0.000005,  4.933474},
            {-1.461115,  0.707101,  6.843752},
            {-2.247491, -0.000000,  6.180964},
            {-3.164763, -0.000000,  6.988164},
            {-4.155417, -0.000000,  6.107583},
            {-5.292834, -0.000000,  3.575912},
            {-5.476289, -0.000000,  7.743996},
            {-4.321013, -0.000000, 11.094296},
            {-0.787193, -0.000000, 13.423745},
            { 0.005330, -0.000000, 15.669228},
            {-3.180439,  1.438709,  7.695314},
            {-4.787621,  0.707101,  6.067568},
            {-4.588031,  0.707101,  7.597761},
            { 0.000000,  1.996092, 10.168588},
            { 0.000000,  1.370200, 12.822122},
            {-3.372246,  1.295732, 10.534312},
            {-1.406365,  1.812266, 10.156220},
            {-1.433740,  1.775428,  8.389470},
            {-0.000000,  0.000006, -9.680047},
            {-0.000000,  1.076022,  0.000000},
            {-1.076022,  0.000000,  0.000000},
            {-0.760863,  0.760862,  0.000000}
        };

        // 原版 quad 顺序表（1-based 索引；顶点表用 0-based 索引时 -1）
        final int[][] Q = {
            {29, 6, 2,27}, {28, 4, 6,29}, {23,15,14,20}, {13,19,12,12},
            {14,20,19,13}, { 8, 9, 4, 4}, { 9, 8,10,10}, { 9,10,11,18},
            {18,11,12,19}, {20,18,23,23}, {19,20,18,18}, {17,22,16,16},
            {22,16,15,23}, { 4, 9, 6, 6}, { 9,18,25,25}, {23,18,25,24},
            {24,25, 7,21}, {22,23,24,21}, { 6,25, 7, 2}, { 9,25, 6, 6},
            { 5, 1,26,26}, { 3, 5,26,26}, { 5,29,27, 1}, { 3,28,29, 5}
        };

        // zPos 由调用侧传入（原版白心 pass = centerZ1-centerZ2，光晕 pass = -1.0），
        // 顶点 z 先加 zPos 再经受 poseStack 的 scale（与原版 GL 矩阵语义一致）

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 4 重镜像：原 / +x / -y / -x-y
        for(int mirror = 0; mirror < 4; mirror++)
        {
            float sx = (mirror == 0 || mirror == 2) ? 1.0F : -1.0F;
            float sy = (mirror == 0 || mirror == 1) ? 1.0F : -1.0F;

            for(int i = 0; i < Q.length; i++)
            {
                float x0 = (float)V[Q[i][0] - 1][0] * sx;
                float y0 = (float)V[Q[i][0] - 1][1] * sy;
                float z0 = (float)V[Q[i][0] - 1][2] + zPos;
                float x1 = (float)V[Q[i][1] - 1][0] * sx;
                float y1 = (float)V[Q[i][1] - 1][1] * sy;
                float z1 = (float)V[Q[i][1] - 1][2] + zPos;
                float x2 = (float)V[Q[i][2] - 1][0] * sx;
                float y2 = (float)V[Q[i][2] - 1][1] * sy;
                float z2 = (float)V[Q[i][2] - 1][2] + zPos;
                float x3 = (float)V[Q[i][3] - 1][0] * sx;
                float y3 = (float)V[Q[i][3] - 1][1] * sy;
                float z3 = (float)V[Q[i][3] - 1][2] + zPos;

                consumer.vertex(matrix, x0, y0, z0).color(r, g, b, alpha)
                        .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha)
                        .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha)
                        .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, x3, y3, z3).color(r, g, b, alpha)
                        .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            }
        }
    }

    // ============================================================
    // 莱瓦汀（Laevateinn）：原版 renderLaevateinn
    // 52 顶点 + 43 quad（2 重镜像：原 / -y 共 86 quad）
    // 顶点含时间 sin 抖动（原版 thLaser.ticksExisted，1.20.1 用 tickCount+partialTicks 更平滑）
    // ============================================================
    private void renderLaevateinn(VertexConsumer consumer, PoseStack poseStack, int packedLight,
                                   int color, float zPos, int alpha, float time)
    {
        color %= 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];

        // 原版顶点表（带 time 抖动）
        double sin60 = Math.sin(time * 60.0 / 180.0 * Math.PI) * 0.5;
        double sin86 = Math.sin(time * 86.0 / 180.0 * Math.PI) * 0.5;
        double sin50 = Math.sin(time * 50.0 / 180.0 * Math.PI) * 0.5;
        double sin43 = Math.sin(time * 43.0 / 180.0 * Math.PI) * 0.5;
        double sin80 = Math.sin(time * 80.0 / 180.0 * Math.PI) * 0.5;

        final double[][] V = {
            {-0.012712, 0.860909, 19.683685}, { 0.010621, 0.799994,  3.958289},
            {-0.411288, 0.576921, 19.683094}, {-0.555063, 0.565679,  3.957450},
            {-0.576384, 0.000006, 19.682850}, {-0.789378,-0.000006,  3.957103},
            { 0.944878+sin60, 0.000006, 19.685106}, { 0.810621,-0.000006,  3.959476},
            { 0.385864+sin86, 0.576921, 19.684277}, { 0.576306, 0.565679,  3.959129},
            { 0.000000, 1.097230, 11.116394}, {-0.565685, 0.803469, 11.115555},
            {-0.978337,-0.000000, 11.115208}, { 0.799999, 0.000000, 11.117582},
            { 0.565685, 0.922361, 11.117234}, { 0.010666, 0.838774,  3.928527},
            {-2.699365, 0.740431,  3.924505}, {-3.821897,-0.000008,  3.922842},
            { 3.843230,-0.000008,  3.934216}, { 2.720698, 0.819214,  3.932549},
            { 0.011292, 0.878166,  3.507193}, {-2.698740, 0.819214,  3.503171},
            {-3.821272,-0.000009,  3.501507}, { 3.843855,-0.000009,  3.512882},
            { 2.721323, 0.819214,  3.511215}, { 0.011649, 0.801885,  3.266243},
            {-0.603876, 0.559341,  3.293626}, {-0.844117,-0.001264,  3.318336},
            { 0.867178,-0.000875,  3.320815}, { 0.627090, 0.559341,  3.295454},
            { 0.015762, 0.620065,  0.494560}, {-0.422696, 0.438449,  0.493910},
            {-0.604311,-0.000009,  0.493641}, { 0.635834,-0.000009,  0.495482},
            { 0.454219, 0.438449,  0.495212}, { 0.017635,-0.000009, -0.767668},
            {-0.017040, 0.277390, 22.599312}, {-0.213180, 0.196148, 22.599022},
            {-0.294422, 0.000008, 22.598900}, { 0.260342+sin50, 0.000008, 22.599724},
            { 0.179100, 0.196148, 22.599602}, {-0.019185, 0.000008, 24.045269},
            {-0.488487, 0.482134, 15.399324}, {-1.282651, 0.000003, 15.399029},
            { 1.620884+sin43, 0.000003, 15.402454}, { 0.869693, 0.482134, 15.401340},
            {-0.006356, 0.681839, 15.400040}, { 0.005311, 0.799997,  7.537342},
            {-0.560374, 0.565682,  7.536503}, {-1.151364,-0.000003,  7.536155},
            { 2.026458+sin80,-0.000003,  7.540341}, { 1.161873, 0.565682,  7.539058}
        };

        final int[][] Q = {
            {48, 2, 4,49}, {49, 4, 6,50}, { 2,10,20,16}, {52,10, 2,48},
            {51, 8,10,52}, { 9, 1,37,41}, {47,11,12,43}, {43,12,13,44},
            {46,15,11,47}, {45,14,15,46}, {16,20,25,21}, {10, 8,19,20},
            { 6, 4,17,18}, { 4, 2,16,17}, {20,19,24,25}, {18,17,22,23},
            {17,16,21,22}, {21,25,30,26}, {25,24,29,30}, {23,22,27,28},
            {22,21,26,27}, {26,30,35,31}, {30,29,34,35}, {28,27,32,33},
            {27,26,31,32}, {31,35,36,36}, {35,34,36,36}, {33,32,36,36},
            {32,31,36,36}, { 7, 9,41,40}, { 3, 5,39,38}, { 1, 3,38,37},
            {41,37,42,42}, {40,41,42,42}, {38,39,42,42}, {37,38,42,42},
            { 1,47,43, 3}, { 3,43,44, 5}, { 9,46,47, 1}, { 7,45,46, 9},
            {11,48,49,12}, {12,49,50,13}, {15,52,48,11}, {14,51,52,15}
        };

        // zPos 由调用侧传入（原版白心 pass = 1.6，光晕 pass = 1.0）

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 2 重镜像：原 / -y
        for(int mirror = 0; mirror < 2; mirror++)
        {
            float sy = (mirror == 0) ? 1.0F : -1.0F;

            for(int i = 0; i < Q.length; i++)
            {
                float x0 = (float)V[Q[i][0] - 1][0];
                float y0 = (float)V[Q[i][0] - 1][1] * sy;
                float z0 = (float)V[Q[i][0] - 1][2] + zPos;
                float x1 = (float)V[Q[i][1] - 1][0];
                float y1 = (float)V[Q[i][1] - 1][1] * sy;
                float z1 = (float)V[Q[i][1] - 1][2] + zPos;
                float x2 = (float)V[Q[i][2] - 1][0];
                float y2 = (float)V[Q[i][2] - 1][1] * sy;
                float z2 = (float)V[Q[i][2] - 1][2] + zPos;
                float x3 = (float)V[Q[i][3] - 1][0];
                float y3 = (float)V[Q[i][3] - 1][1] * sy;
                float z3 = (float)V[Q[i][3] - 1][2] + zPos;

                consumer.vertex(matrix, x0, y0, z0).color(r, g, b, alpha)
                        .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha)
                        .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha)
                        .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                consumer.vertex(matrix, x3, y3, z3).color(r, g, b, alpha)
                        .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            }
        }
    }
}
