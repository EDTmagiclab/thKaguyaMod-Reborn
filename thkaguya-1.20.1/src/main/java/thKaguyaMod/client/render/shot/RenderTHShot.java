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
        poseStack.pushPose();

        // 朝向相机的 billboard
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float size = entity.getShotSize();
        poseStack.scale(size, size, size);

        int form = entity.getShotForm();
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
}
