package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.shot.EntityTHShot;

/**
 * 弹体渲染器（1.20.1 移植版，阶段 4 完整化）
 *
 * 1.7.10 原版 RenderTHShot 用 Tessellator + 多种形状贴图（每种形状一张 sprite sheet，
 * 8 种颜色横向排布，通过 UV 的 color/8 选取子图）。1.20.1 这里按 form 分支：
 *   - 8 色 sheet 简单 billboard：SMALL/TINY/MEDIUM/PEARL/CIRCLE/HEART
 *   - 3D 程序化几何：CRYSTAL（八面体）、ARROW（矢弹）、KUNAI（苦无）、
 *                     LIGHT/BIGLIGHT（光弹双 pass）、KISHITU（气弹双 pass）、
 *                     PHANTOM（幽灵多层）、STAR/SMALLSTAR（程序化五角星）、
 *                     BUTTERFLY（蝴蝶）、TALISMAN（旋转符札）、BIG（加性大弹）
 *   - billboard 兜底退化：SCALE/RICE/OVAL（Wriggle/Toziko 米弹，椭球几何形态退化）
 *   - KNIFE：原版用 ModelSilverKnife，1.20.1 暂走 sprite 兜底（阶段 4 再补 LayerDefinition）
 *   - AMULET/WIND：阶段 4 补 3D 几何，当前 sprite 兜底
 */
public class RenderTHShot extends EntityRenderer<EntityTHShot>
{
    /* 贴图资源（原版 textures/shot/ 下的小写文件名） */
    private static final ResourceLocation TEX_SMALL      = tex("textures/shot/smallshot.png");
    private static final ResourceLocation TEX_PEARL      = tex("textures/shot/pearlshot.png");
    private static final ResourceLocation TEX_CIRCLE     = tex("textures/shot/circleshot.png");
    private static final ResourceLocation TEX_STAR       = tex("textures/shot/starshot.png");
    private static final ResourceLocation TEX_HEART      = tex("textures/shot/heartshot.png");
    private static final ResourceLocation TEX_KUNAI      = tex("textures/shot/kunaishot.png");
    private static final ResourceLocation TEX_TALISMAN   = tex("textures/shot/talismanshot.png");
    private static final ResourceLocation TEX_LASER      = tex("textures/shot/laser.png");
    private static final ResourceLocation TEX_LIGHT      = tex("textures/shot/musoufuuin.png");
    private static final ResourceLocation TEX_BUTTERFLY  = tex("textures/shot/butterflyshot.png");
    private static final ResourceLocation TEX_WIND       = tex("textures/shot/windshot.png");
    private static final ResourceLocation TEX_BIG        = tex("textures/shot/bigshot.png");
    private static final ResourceLocation TEX_ARROW      = tex("textures/shot/arrowshot.png");
    private static final ResourceLocation TEX_AMULET     = tex("textures/shot/homingamulet.png");
    private static final ResourceLocation TEX_DIFFUSION  = tex("textures/shot/diffusionamulet.png");
    private static final ResourceLocation TEX_SCALE      = tex("textures/shot/scaleshot.png");
    private static final ResourceLocation TEX_FAMILIAR   = tex("textures/mob/familiar.png");

    private static final ResourceLocation TEX_KNIFE_RED    = tex("textures/shot/silverknife_red.png");
    private static final ResourceLocation TEX_KNIFE_BLUE   = tex("textures/shot/silverknife_blue.png");
    private static final ResourceLocation TEX_KNIFE_GREEN  = tex("textures/shot/silverknife_green.png");
    private static final ResourceLocation TEX_KNIFE_YELLOW = tex("textures/shot/silverknife_yellow.png");
    private static final ResourceLocation TEX_KNIFE_PURPLE = tex("textures/shot/silverknife_purple.png");
    private static final ResourceLocation TEX_KNIFE_AQUA   = tex("textures/shot/silverknife_aqua.png");
    private static final ResourceLocation TEX_KNIFE_ORANGE = tex("textures/shot/silverknife_orange.png");
    private static final ResourceLocation TEX_KNIFE_WHITE  = tex("textures/shot/silverknife_white.png");

    /** 单张图（非 8 色横向 sprite sheet）时的顶点染色表，对应 8 种颜色 */
    private static final int[] COLOR_R = {224,   0,   0, 224, 224,   0, 255, 255};
    private static final int[] COLOR_G = {  0,   0, 224, 224,   0, 224, 128, 255};
    private static final int[] COLOR_B = {  0, 224,   0,   0, 224, 224,   0, 255};

    private static ResourceLocation tex(String path)
    {
        return new ResourceLocation(THKaguyaCore.MODID, path);
    }

    public RenderTHShot(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHShot entity)
    {
        return getTexture(entity.getShotForm(), entity.getShotColor());
    }

    /** 按形状 + 颜色选择贴图（还原原版 getEntityTexture） */
    private static ResourceLocation getTexture(int form, int color)
    {
        switch(form)
        {
            case DanmakuConstants.FORM_SMALL:
            case DanmakuConstants.FORM_TINY:
            case DanmakuConstants.FORM_MEDIUM:
                return TEX_SMALL;
            case DanmakuConstants.FORM_PEARL:
                return TEX_PEARL;
            case DanmakuConstants.FORM_CIRCLE:
                return TEX_CIRCLE;
            case DanmakuConstants.FORM_STAR:
            case DanmakuConstants.FORM_SMALLSTAR:
            case DanmakuConstants.FORM_CRYSTAL:
                return TEX_STAR;
            case DanmakuConstants.FORM_HEART:
                return TEX_HEART;
            case DanmakuConstants.FORM_KUNAI:
                return TEX_KUNAI;
            case DanmakuConstants.FORM_TALISMAN:
                return TEX_TALISMAN;
            case DanmakuConstants.FORM_LIGHT:
            case DanmakuConstants.FORM_BIGLIGHT:
            case DanmakuConstants.FORM_PHANTOM:
            case DanmakuConstants.FORM_KISHITU:
                return TEX_LIGHT;
            case DanmakuConstants.FORM_SCALE:
                return TEX_SCALE;
            // 米弹 / 椭圆弹原版是程序化椭球几何（renderRiceShot / renderOvalShot），
            // 1.20.1 billboard 实现无法还原椭球形态。回退到 light 弹贴图（musoufuuin.png），
            // 避免使用 laser.png（细长光带）造成视觉完全错误。
            case DanmakuConstants.FORM_RICE:
            case DanmakuConstants.FORM_OVAL:
                return TEX_LIGHT;
            case DanmakuConstants.FORM_BUTTERFLY:
                return TEX_BUTTERFLY;
            case DanmakuConstants.FORM_FAMILIAR:
                return TEX_FAMILIAR;
            case DanmakuConstants.FORM_WIND:
                return TEX_WIND;
            case DanmakuConstants.FORM_BIG:
                return TEX_BIG;
            case DanmakuConstants.FORM_ARROW:
                return TEX_ARROW;
            case DanmakuConstants.FORM_AMULET:
                return color == 1 ? TEX_DIFFUSION : TEX_AMULET;
            case DanmakuConstants.FORM_KNIFE:
                return getKnifeTexture(color);
            default:
                return TEX_LIGHT;
        }
    }

    private static ResourceLocation getKnifeTexture(int color)
    {
        switch(color)
        {
            case 0: return TEX_KNIFE_RED;
            case 1: return TEX_KNIFE_BLUE;
            case 2: return TEX_KNIFE_GREEN;
            case 3: return TEX_KNIFE_YELLOW;
            case 4: return TEX_KNIFE_PURPLE;
            case 5: return TEX_KNIFE_AQUA;
            case 6: return TEX_KNIFE_ORANGE;
            default: return TEX_KNIFE_WHITE;
        }
    }

    /** 是否为「8 色横向 sprite sheet」（原版 renderNormalShot 用 color/8 选 UV 的形状） */
    private static boolean isEightColorSheet(int form)
    {
        switch(form)
        {
            case DanmakuConstants.FORM_SMALL:
            case DanmakuConstants.FORM_TINY:
            case DanmakuConstants.FORM_MEDIUM:
            case DanmakuConstants.FORM_PEARL:
            case DanmakuConstants.FORM_CIRCLE:
            case DanmakuConstants.FORM_HEART:
            case DanmakuConstants.FORM_TALISMAN:
            case DanmakuConstants.FORM_BIG:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void render(EntityTHShot entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int form = entity.getShotForm();
        int anim = entity.getAnimationCount();
        float pitch = entity.getXRot();
        float slope = entity.getAngleZ();
        float size = entity.getShotSize();

        // === 3D 几何 / 程序化形态（按 form 分发） ===
        switch(form)
        {
            case DanmakuConstants.FORM_CRYSTAL:
                renderCrystalShot(entity, yaw, partialTicks, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_KUNAI:
                renderKunaiShot(entity, yaw, pitch, slope, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_ARROW:
                renderArrowShot(entity, yaw, pitch, slope, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_LIGHT:
            case DanmakuConstants.FORM_BIGLIGHT:
                renderLightShot(entity, anim, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_KISHITU:
                renderKishituShot(entity, anim, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_PHANTOM:
                renderPhantomShot(entity, anim, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_STAR:
            case DanmakuConstants.FORM_SMALLSTAR:
                renderStarShot(entity, anim, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_BUTTERFLY:
                renderButterflyShot(entity, anim, yaw, pitch, slope, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_TALISMAN:
                renderTalismanShot(entity, yaw, pitch, slope, poseStack, buffer, packedLight);
                return;
            case DanmakuConstants.FORM_FAMILIAR:
                renderFamiliar(entity, anim, poseStack, buffer, packedLight);
                return;
            default:
                // 普通 8 色 sheet billboard（SMALL/TINY/MEDIUM/PEARL/CIRCLE/HEART/BIG）
                // + 兜底（SCALE/RICE/OVAL/WIND/AMULET/KNIFE）
                renderBillboard(entity, poseStack, buffer, packedLight);
                return;
        }
    }

    // ============================================================
    // billboard（普通 8 色 sheet 弹 + 兜底）
    // ============================================================
    private void renderBillboard(EntityTHShot entity, PoseStack poseStack,
                                  MultiBufferSource buffer, int packedLight)
    {
        int form = entity.getShotForm();
        int color = entity.getShotColor() % 8;
        ResourceLocation texture = getTexture(form, color);
        float size = entity.getShotSize();

        poseStack.pushPose();
        // 朝向相机的 billboard
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // FORM_BIG 原版是 renderNormalShot(size, color, blend=true) → 加性混合
        boolean bigBlend = form == DanmakuConstants.FORM_BIG;
        RenderType rt = bigBlend
                ? RenderType.entityTranslucentCull(texture)
                : RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = buffer.getBuffer(rt);

        float half = 0.5F;
        float u1 = 0.0F, u2 = 1.0F;
        int r = 255, g = 255, b = 255;
        if(isEightColorSheet(form))
        {
            u1 = color / 8.0F;
            u2 = (color + 1) / 8.0F;
        }
        else
        {
            r = COLOR_R[color];
            g = COLOR_G[color];
            b = COLOR_B[color];
        }
        int a = bigBlend ? 204 : 255;

        quadBillboard(consumer, matrix, normal, packedLight,
                r, g, b, a, u1, u2);
        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_LIGHT / FORM_BIGLIGHT（双 pass：彩色光晕 + 白色中心，2 帧动画）
    // 对应原版 renderLightShot。原贴图 musoufuuin.png 是 64×32，左半 0..32 = 帧0，
    // 右半 32..64 = 帧1（沿 U 切换）。MUSOU_FUIUUI 加性混合双层。
    // ============================================================
    private void renderLightShot(EntityTHShot entity, int anim, PoseStack poseStack,
                                  MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];
        int frame = anim & 1; // 0 or 1
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        // 原版光弹 width=2.0F，billboard 大小放大到 2x
        poseStack.scale(2.0F, 2.0F, 1.0F);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 光晕层（彩色 + alpha 0.8）
        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_LIGHT));
        quadBillboard(consumer, matrix, normal, packedLight, r, g, b, 204, uMin, uMax);

        // 白色中心层（z 偏移 0.01 模拟原版顶点次序，原 1.7 不直接画颜色）
        poseStack.translate(0, 0, 0.01F);
        poseStack.scale(0.5F, 0.5F, 1.0F);
        pose = poseStack.last();
        Matrix4f matrix2 = pose.pose();
        Matrix3f normal2 = pose.normal();
        quadBillboard(consumer, matrix2, normal2, packedLight, 255, 255, 255, 255, uMin, uMax);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_KISHITU（类似 LIGHT 但尺寸略大：width=2.0 + 中心白心 1.6）
    // 对应原版 renderKishituShot
    // ============================================================
    private void renderKishituShot(EntityTHShot entity, int anim, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];
        int frame = anim & 1;
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        poseStack.scale(2.0F, 2.0F, 1.0F);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;
        // 原版 renderKishituShot 用 glBlendFunc(GL_SRC_ALPHA, GL_ONE) 加性混合
        // 1.20.1 RenderType.entityTranslucent 是标准半透明混合（GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA），
        // 不是加性，但配合双层（彩色外层 + 白心内层）已可见"白芯红晕"效果。
        // 若要完全还原原版加性效果需要自定义 RenderType（RenderStateShard.LIGHTNING_TRANSPARENCY
        // 在 1.20.1 是 protected），此处用现有 RenderType 兼顾可见性。
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEX_LIGHT));

        // 外层（彩色 alpha 0.5）
        quadBillboard(consumer, matrix, normal, packedLight, r, g, b, 128, uMin, uMax);

        // 内层白心（width=1.6，alpha 0.8，z 偏移避免 z-fighting）
        poseStack.translate(0, 0, 0.01F);
        poseStack.scale(0.8F, 0.8F, 1.0F);
        pose = poseStack.last();
        quadBillboard(consumer, pose.pose(), pose.normal(), packedLight, 255, 255, 255, 204, uMin, uMax);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_PHANTOM（多重叠加 + 缩放动画 + 加性混合 + 抖动位移）
    // 对应原版 renderPhantomShot
    // ============================================================
    private void renderPhantomShot(EntityTHShot entity, int anim, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int frame = anim & 1;
        float size = entity.getShotSize();
        double timeR = (anim / 180.0) * Math.PI;
        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // 第一层（最大，固定 size）
        poseStack.scale(size, size, size);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_LIGHT));
        quadBillboard(consumer, pose.pose(), pose.normal(), packedLight,
                COLOR_R[color], COLOR_G[color], COLOR_B[color], 178, uMin, uMax);

        // 第二层（0.7 倍 + sin 抖动 X）
        poseStack.translate((float)Math.sin(timeR * 5) * 0.2F,
                            0.2F + (anim % 20) * 0.03F, 0.01F);
        poseStack.scale(0.7F, 0.7F, 1.0F);
        pose = poseStack.last();
        quadBillboard(consumer, pose.pose(), pose.normal(), packedLight,
                COLOR_R[color], COLOR_G[color], COLOR_B[color], 153, uMin, uMax);

        // 第三层（0.6 倍 + cos 抖动 X）
        poseStack.translate((float)Math.cos(timeR * 5) * 0.2F,
                            0.2F + (anim % 20) * 0.035F, 0.01F);
        poseStack.scale(0.85F, 0.85F, 1.0F);
        pose = poseStack.last();
        quadBillboard(consumer, pose.pose(), pose.normal(), packedLight,
                COLOR_R[color], COLOR_G[color], COLOR_B[color], 128, uMin, uMax);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_STAR / FORM_SMALLSTAR（程序化 5 角星，10 顶点 polygon，加性混合，自旋）
    // 对应原版 renderStarShot。原 starshot.png 是 64x64 单图，billboard 显示效果差。
    // ============================================================
    private void renderStarShot(EntityTHShot entity, int anim, PoseStack poseStack,
                                 MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        // 自旋（原版 glRotatef(count*3.7F, 0,0,1)）
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(anim * 3.7F));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_STAR));

        // 生成 5 角星 10 点坐标：outer=2.0, inner=1.2，角度 36°
        float[] xPos = new float[11];
        float[] yPos = new float[11];
        float angle = 0F;
        float span = (float)(Math.PI * 0.2F); // 36°
        for(int i = 0; i < 10; i += 2)
        {
            xPos[i] = (float)Math.cos(angle) * 2.0F;
            yPos[i] = (float)Math.sin(angle) * 2.0F;
            angle += span;
            xPos[i+1] = (float)Math.cos(angle) * 1.2F;
            yPos[i+1] = (float)Math.sin(angle) * 1.2F;
            angle += span;
        }
        xPos[10] = xPos[0];
        yPos[10] = yPos[0];

        // 5 个三角形扇面（外圈 + 内圈白色缩小版叠加）
        for(int i = 0; i < 9; i += 2)
        {
            // 外圈（彩色 alpha 0.7）
            triangleFan3(consumer, matrix, normal, packedLight, r, g, b, 178,
                    xPos[i], yPos[i], 0, 0,
                    xPos[i+1], yPos[i+1], 1, 0,
                    xPos[i+2], yPos[i+2], 1, 1);
            // 内圈（白色缩小）
            float sx = 0.7F;
            float sw = 0.95F;
            triangleFan3(consumer, matrix, normal, packedLight, 255, 255, 255, 178,
                    xPos[i] * sx, yPos[i] * sx, 0, 0,
                    xPos[i+1] * sw, yPos[i+1] * sw, 1, 0,
                    xPos[i+2] * sx, yPos[i+2] * sx, 1, 1);
        }
        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_BUTTERFLY（3D 翅膀：两个对称翅膀绕中轴旋转）
    // 对应原版 renderButterflyShot
    // ============================================================
    private void renderButterflyShot(EntityTHShot entity, int anim, float yaw, float pitch, float slope,
                                      PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];
        float size = entity.getShotSize();
        // 原版翅膀挥动：wingAngle = sin(count/3) * 45°
        float wingAngle = Mth.sin(anim / 3.0F) * 45.0F;
        // 原版对 pitch 做 |pitch|<=90 的折叠
        if(pitch > 90F) pitch = 90F - pitch % 90F;
        else if(pitch < -90F) pitch = -90F - pitch % 90F;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        // 实体朝向
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slope));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(wingAngle));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // butterflyshot.png 128×64，原版 UV：0/128..32/128 = 1/4 宽、整高
        float uMin = 0F;
        float uMax = 32F / 128F;
        float vMin = 0F;
        float vMax = 1F;
        float w = 2.0F;  // 翅膀宽度
        float w2 = 1.8F; // 白色高光缩小版

        // 原版禁 GL_CULL_FACE 画双面；1.20.1 entityCutoutNoCull 即 cull off。
        // 注：白色高光 alpha=178（≥0.5）以通过 entityCutoutNoCull 的 alphaTest（不真正半透明）。
        VertexConsumer c2 = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_BUTTERFLY));

        // 右翅膀：4 顶点 quad（不计较 CCW，反正双面绘制）
        quadQuad(c2, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, w, uMax, vMin,
                w, 0.0F, w, uMin, vMin,
                w, 0.0F, -w, uMin, vMax,
                0.0F, 0.0F, -w, uMax, vMax);
        // 右翅膀白色高光（alpha 178 通过 alphaTest，视觉效果：浅色高亮而非真正半透明）
        quadQuad(c2, matrix, normal, packedLight, 255, 255, 255, 178,
                0.0F, 0.0F, w2, uMax, vMin,
                w2, 0.0F, w2, uMin, vMin,
                w2, 0.0F, -w2, uMin, vMax,
                0.0F, 0.0F, -w2, uMax, vMax);

        // 左翅膀：旋转 -wingAngle*2（反射）
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-wingAngle * 2.0F));
        pose = poseStack.last();
        matrix = pose.pose();
        normal = pose.normal();
        quadQuad(c2, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, w, uMax, vMin,
                -w, 0.0F, w, uMin, vMin,
                -w, 0.0F, -w, uMin, vMax,
                0.0F, 0.0F, -w, uMax, vMax);
        quadQuad(c2, matrix, normal, packedLight, 255, 255, 255, 178,
                0.0F, 0.0F, w2, uMax, vMin,
                -w2, 0.0F, w2, uMin, vMin,
                -w2, 0.0F, -w2, uMin, vMax,
                0.0F, 0.0F, -w2, uMax, vMax);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_TALISMAN（旋转符札：双面 quad，yaw/pitch/slope 三轴旋转）
    // 对应原版 renderTalismanShot
    // ============================================================
    private void renderTalismanShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                     PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        float u1 = color / 8.0F;
        float u2 = (color + 1) / 8.0F;
        float width = 1.6F;
        double len = 2.0D;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slope));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 原版禁 GL_CULL_FACE 画双面
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_TALISMAN));
        quadQuad(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                width, 0.0F, (float)len, u2, 0.0F,
                -width, 0.0F, (float)len, u1, 0.0F,
                -width, 0.0F, (float)-len, u1, 1.0F,
                width, 0.0F, (float)-len, u2, 1.0F);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_FAMILIAR（billboard + 自旋 + 半透明）
    // 对应原版 renderFamiliar
    // ============================================================
    private void renderFamiliar(EntityTHShot entity, int anim, PoseStack poseStack,
                                 MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        // 原版自旋 glRotatef(count*3.7F, 0,0,1)
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(anim * 3.7F));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 半透明 + 加性混合 → entityTranslucentCull（无加性但保留半透明）
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_FAMILIAR));
        quadBillboard(consumer, matrix, normal, packedLight,
                COLOR_R[color], COLOR_G[color], COLOR_B[color], 128, 0.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_KUNAI（3D 苦无：刀身（双层 quad）+ 把手长方形）
    // 对应原版 renderKunaiShot（kunaishot.png 64×64，UV: 刀身 0..0.5, 把手 0.5..0.75）
    // ============================================================
    private void renderKunaiShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slope));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_KUNAI));

        // 刀身尺寸
        float width  = 0.8F;
        float flont  = 1.12F;
        float back   = -0.8F;
        float back2  = -1.0F;
        float height = 0.24F;

        // 上（Y+ 朝向）
        // 原版顶点：(-w,0,back)(0,0,flont)(w,0,back)(0,height,back)
        // 这是从刀背中点到刀尖的"三角形"伪装成 4 顶点 quad（最后顶点高度+ height）
        // 1.20.1 中用 4 顶点 quad 替代：3D 楔形
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);

        // 下左（刀尖到刀背中点的 -Y/-X 一侧）
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);

        // 下右
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);

        // 上左（刀背 -height 一侧）
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);

        // 下左
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);

        // 上右
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);

        // 下右
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                -width, 0.0F, back, 0.50F, 1.0F);

        // ===== 彩色外层（原版第二段，启用加性混合放大版）=====
        poseStack.translate(0.0F, 0.001F, 0.0F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
        // 切到 entityTranslucentCull 做加性半透明
        VertexConsumer rEnc = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_KUNAI));
        width = 1.0F;
        flont = 1.4F;
        back  = -0.8F;
        back2 = -1.0F;
        height = 0.3F;
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                -width, 0.0F, back, 0.50F, 1.0F);

        // 把手（双面，原 UV 0.5..0.75 / 0.375..1.0）
        float width2 = 0.6F;
        float root = -2.6F;
        // 取消 cull 双面
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                width2, 0.0F, back2, 0.50F, 0.375F,
                -width2, 0.0F, back2, 0.75F, 0.375F,
                -width2, 0.0F, root, 0.75F, 1.0F,
                width2, 0.0F, root, 0.50F, 1.0F);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_ARROW（3D 矢弹：与 KUNAI 类似但把手更长 root=-5.8）
    // 对应原版 renderArrowShot
    // ============================================================
    private void renderArrowShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slope));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_ARROW));

        // 刀身尺寸（与 KUNAI 同）
        float width  = 0.8F;
        float flont  = 1.12F;
        float back   = -0.8F;
        float back2  = -1.0F;
        float height = 0.24F;
        // 上
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);
        quadQuadCull(consumer, matrix, normal, packedLight, 255, 255, 255, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                -width, 0.0F, back, 0.50F, 1.0F);

        // 彩色外层
        VertexConsumer rEnc = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_ARROW));
        width = 1.0F;
        flont = 1.4F;
        back  = -0.8F;
        back2 = -1.0F;
        height = 0.3F;
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                0.0F, 0.0F, flont, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, height, back, 0.25F, 1.0F,
                width, 0.0F, back, 0.00F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                width, 0.0F, back, 0.00F, 1.0F,
                0.0F, -height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                -width, 0.0F, back, 0.50F, 1.0F,
                0.0F, height, back, 0.25F, 1.0F);
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, 0.0F, back2, 0.25F, 0.0F,
                0.0F, -height, back, 0.25F, 1.0F,
                -width, 0.0F, back, 0.50F, 1.0F);

        // 长杆（原版 root=-5.8，UV 0.5..0.75 / 0..1）
        float width2 = 0.6F;
        float root = -5.8F;
        quadQuadCull(rEnc, matrix, normal, packedLight, r, g, b, 255,
                width2, 0.0F, back2, 0.50F, 0.0F,
                -width2, 0.0F, back2, 0.75F, 0.0F,
                -width2, 0.0F, root, 0.75F, 1.0F,
                width2, 0.0F, root, 0.50F, 1.0F);

        poseStack.popPose();
        super.render(entity, 0, 0, poseStack, buffer, packedLight);
    }

    // ============================================================
    // FORM_CRYSTAL 八面体渲染（1.20.1 移植，对齐原版 RenderTHShot.renderCrystalShot）。
    // ============================================================
    private void renderCrystalShot(EntityTHShot entity, float yaw, float partialTicks,
                                    PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 让 entity 自身 yaw/pitch 决定朝向（与原版 1.7 一致）
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-entity.getXRot()));

        float size = entity.getShotSize();
        poseStack.scale(size, size, size);

        int color = entity.getShotColor() % 8;
        int r = COLOR_R[color];
        int g = COLOR_G[color];
        int b = COLOR_B[color];

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEX_STAR));

        float width  = 0.96F;
        float width2 = width * 0.8F;
        float length   = 4.0F;
        float length_b = 1.0F;
        float length2   = length * 0.8F;
        float length2_b = length_b * 0.4F;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 内层 4 个白色小金字塔
        for(int i = 0; i < 4; i++)
        {
            tri3(consumer, matrix, normal, packedLight,
                    0.0F,  width2, 0.0F,
                    -width2, 0.0F, 0.0F,
                    0.0F,  0.0F,  length2);
            tri3(consumer, matrix, normal, packedLight,
                    0.0F,  width2, 0.0F,
                    width2, 0.0F, 0.0F,
                    0.0F,  0.0F,  -length2_b);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            pose = poseStack.last();
            matrix = pose.pose();
            normal = pose.normal();
        }

        // 外层 4 个彩色大金字塔（alpha 0.7）
        int alpha = 178;
        for(int i = 0; i < 4; i++)
        {
            tri3c(consumer, matrix, normal, packedLight, r, g, b, alpha,
                    0.0F,  width, 0.0F,
                    -width, 0.0F, 0.0F,
                    0.0F,  0.0F,  length);
            tri3c(consumer, matrix, normal, packedLight, r, g, b, alpha,
                    0.0F,  width, 0.0F,
                    width, 0.0F, 0.0F,
                    0.0F,  0.0F,  -length_b);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            pose = poseStack.last();
            matrix = pose.pose();
            normal = pose.normal();
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    // ============================================================
    // 低层 helper
    // ============================================================

    /** billboard 四边形（CCW 顺序），贴图覆盖整个 0..1，u1/u2 选定 sheet 子图 */
    private static void quadBillboard(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                       int light, int r, int g, int b, int a, float u1, float u2)
    {
        float half = 0.5F;
        // 顶点顺序（CCW，从 +Z 看）：左下 → 右下 → 右上 → 左上
        consumer.vertex(matrix, -half, -half, 0.0F).color(r, g, b, a)
                .uv(u1, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, -half, 0.0F).color(r, g, b, a)
                .uv(u2, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, half, 0.0F).color(r, g, b, a)
                .uv(u2, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half, half, 0.0F).color(r, g, b, a)
                .uv(u1, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 通用 4 顶点 quad（CCW），给定 4 个 (位置, UV) 对 */
    private static void quadQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                  int light, int r, int g, int b, int a,
                                  float ax, float ay, float az, float uA, float vA,
                                  float bx, float by, float bz, float uB, float vB,
                                  float cx, float cy, float cz, float uC, float vC,
                                  float dx, float dy, float dz, float uD, float vD)
    {
        consumer.vertex(matrix, ax, ay, az).color(r, g, b, a)
                .uv(uA, vA).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(r, g, b, a)
                .uv(uB, vB).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, cz).color(r, g, b, a)
                .uv(uC, vC).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, dx, dy, dz).color(r, g, b, a)
                .uv(uD, vD).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 三角形 3 顶点 helper（白色，无 UV 限制） */
    private static void tri3(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz)
    {
        tri3c(consumer, matrix, normal, light, 255, 255, 255, 255,
                ax, ay, az, bx, by, bz, cx, cy, cz);
    }

    /** 三角形 3 顶点 helper（带颜色） */
    private static void tri3c(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light,
            int r, int g, int b, int a,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz)
    {
        consumer.vertex(matrix, ax, ay, az).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, cz).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 三角形扇面 3 顶点 helper（带 UV）—— 用于五角星 */
    private static void triangleFan3(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light,
            int r, int g, int b, int a,
            float ax, float ay, float au, float av,
            float bx, float by, float bu, float bv,
            float cx, float cy, float cu, float cv)
    {
        consumer.vertex(matrix, ax, ay, 0.0F).color(r, g, b, a)
                .uv(au, av).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx, by, 0.0F).color(r, g, b, a)
                .uv(bu, bv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, 0.0F).color(r, g, b, a)
                .uv(cu, cv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** Kunai/Arrow 用：保留原版顶点顺序（部分顶点重合），用 entityCutoutNoCull（双面）避免 CCW 错误 */
    private static void quadQuadCull(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                      int light, int r, int g, int b, int a,
                                      float ax, float ay, float az, float uA, float vA,
                                      float bx, float by, float bz, float uB, float vB,
                                      float cx, float cy, float cz, float uC, float vC,
                                      float dx, float dy, float dz, float uD, float vD)
    {
        quadQuad(consumer, matrix, normal, light, r, g, b, a,
                ax, ay, az, uA, vA,
                bx, by, bz, uB, vB,
                cx, cy, cz, uC, vC,
                dx, dy, dz, uD, vD);
    }
}