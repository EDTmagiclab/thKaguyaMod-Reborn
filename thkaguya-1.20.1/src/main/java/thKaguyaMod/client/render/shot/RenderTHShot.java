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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.shot.EntityTHShot;

/**
 * 弹体渲染器（1.20.1 移植版）
 *
 * 原版 RenderTHShot 用 Tessellator + 多种形状贴图（每种形状一张 sprite sheet，
 * 8 种颜色横向排布，通过 UV 的 color/8 选取子图）。
 * 本类按 shotForm 选择贴图，按 shotColor 选择 UV 子图，还原原版外观。
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
                // 原版使用 mob/Familiar.png（8 色横向 sprite sheet 模式同 smallshot）
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
                // SCALE(scaleshot 64x32)、FAMILIAR(familiar 64x64) 是单图，顶点染色
                return false;
        }
    }

    @Override
    public void render(EntityTHShot entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int form = entity.getShotForm();

        // CRYSTAL（原版 renderCrystalShot）：3D 八面体（4 个绕 Z 旋转 90° 的小金字塔外层 + 4 个更大金字塔），
        // billboard 无法还原，必须用实体朝向 yaw/pitch 旋转绘制。
        if(form == DanmakuConstants.FORM_CRYSTAL)
        {
            renderCrystalShot(entity, yaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();

        // 朝向相机的 billboard
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float size = entity.getShotSize();
        poseStack.scale(size, size, size);

        int color = entity.getShotColor() % 8;
        ResourceLocation texture = getTexture(form, color);

        // 使魔（FORM_FAMILIAR）原版会绕 Z 轴自旋 glRotatef(count*3.7F, 0,0,1)，
        // billboard 后 Z 即面向相机的轴。复现自旋。
        if(form == DanmakuConstants.FORM_FAMILIAR)
        {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(entity.getAnimationCount() * 3.7F));
        }

        // LIGHT / BIGLIGHT / KISHITU / PHANTOM 原版 renderLightShot / renderKishituShot 使用
        // 加性混合 + alpha 0.8（光晕）+ 中心白心。1.20.1 用 entityTranslucentCull 模拟加性。
        boolean isLightFamily = form == DanmakuConstants.FORM_LIGHT
                || form == DanmakuConstants.FORM_BIGLIGHT
                || form == DanmakuConstants.FORM_KISHITU
                || form == DanmakuConstants.FORM_PHANTOM;
        if(isLightFamily)
        {
            // 略放大到 2.0（与原版 width=2.0F 对齐）
            poseStack.scale(2.0F, 2.0F, 1.0F);
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // FAMILIAR 在原版是半透明 (alpha 0.5) 加性混合，1.20.1 用 entityTranslucentCull
        boolean isFamiliar = form == DanmakuConstants.FORM_FAMILIAR;
        RenderType rt = (isFamiliar || isLightFamily)
                ? RenderType.entityTranslucentCull(texture)
                : RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = buffer.getBuffer(rt);
        float half = 0.5F;

        // UV 选择：8 色横向 sprite sheet 用 color/8 选子图；单张图全图 + 顶点染色
        float u1 = 0.0F;
        float u2 = 1.0F;
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

        int alpha = isFamiliar ? 128 : (isLightFamily ? 204 : 255);
        // 四边形（v 轴：底部 v=1，顶部 v=0，与原版 addVertexWithUV 对齐）
        consumer.vertex(matrix, -half, -half, 0.0F).color(r, g, b, alpha)
                .uv(u1, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, -half, 0.0F).color(r, g, b, alpha)
                .uv(u2, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, half, 0.0F).color(r, g, b, alpha)
                .uv(u2, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half, half, 0.0F).color(r, g, b, alpha)
                .uv(u1, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * FORM_CRYSTAL 八面体渲染（1.20.1 移植，对齐原版 RenderTHShot.renderCrystalShot）。
     * 原版在 1.7.10 GL11 框架下画 4 个小金字塔外层（白）+ 4 个大金字塔外层（色），
     * 绕 Z 轴旋转 4 次，每次画一对三角形。这里用 PoseStack.mulPose(Axis.ZP) 旋转。
     * 原版的 4 个三角形 quad 在 1.20.1 中每两个一组拼成 quad（双三角形）。
     */
    private void renderCrystalShot(EntityTHShot entity, float yaw, float partialTicks,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();

        // 原版在 billboard 之后没有额外旋转（自带的 yaw/pitch 由 entity 自带）
        // 1.20.1 不用 cameraOrientation，让 entity 自身 yaw/pitch 决定朝向
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
        float width2 = width * 0.8F;     // 0.768
        float length   = 4.0F;
        float length_b = 1.0F;
        float length2   = length * 0.8F;     // 3.2
        float length2_b = length_b * 0.4F;   // 0.4

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // 内层 4 个白色小金字塔（绕 Z 转 4 次画一对对面三角形）
        for(int i = 0; i < 4; i++)
        {
            // 第 1 对三角形：菱形的「前半」（从 (0, width2, 0) 到 (-width2, 0, 0) 到 (0, 0, length2)）
            // 原版 addVertexWithUV 顺序：(0, width2, 0)→(-width2, 0, 0)→(0, 0, length2)→(0, 0, length2)
            // 实际后两点重合，是「三角形」伪装成 4 顶点 quad（2 顶点重合）。我们在 1.20.1 用三角形 3 顶点。
            tri3(consumer, matrix, normal, packedLight,
                    0.0F,  width2, 0.0F,
                    -width2, 0.0F, 0.0F,
                    0.0F,  0.0F,  length2);

            // 第 2 对三角形：(0, width2, 0) → (width2, 0, 0) → (0, 0, -length2_b)
            tri3(consumer, matrix, normal, packedLight,
                    0.0F,  width2, 0.0F,
                    width2, 0.0F, 0.0F,
                    0.0F,  0.0F,  -length2_b);

            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            pose = poseStack.last();
            matrix = pose.pose();
            normal = pose.normal();
        }

        // 外层 4 个彩色大金字塔（alpha 0.7 * alpha）
        int alpha = 178; // 0.7 * 255
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

    /** 三角形 3 顶点 helper（白） */
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
}
