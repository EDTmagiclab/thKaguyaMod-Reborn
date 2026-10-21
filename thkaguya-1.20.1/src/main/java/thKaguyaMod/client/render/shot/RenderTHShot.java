package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.shot.ModelSilverKnife;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.shot.EntityTHShot;

import java.util.Random;

/**
 * 弹体渲染器（1.20.1，全面对齐 1.7.10 原版 RenderTHShot）。
 *
 * <p><b>本轮对齐的核心</b>：原版全程直接操作固定管线 GL 状态，其中三项对观感影响最大，
 * 移植时最容易漏：
 * <ol>
 *   <li><b>混合式</b>：原版用 {@code glBlendFunc} 明确区分「加性 (SRC_ALPHA, ONE)」「発光
 *       (ONE, ONE_MINUS_SRC_COLOR)」「纯加算 (ONE, ONE)」。1.20.1 的 entityTranslucent 是标准
 *       半透明 (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)，观感完全不同（叠加变暗而非发光）→ 改用
 *       {@link THRenderTypes} 自建的三种混合 RenderType。</li>
 *   <li><b>深度掩码</b>：凡启用混合的层，原版都 {@code glDepthMask(false)}（只写颜色不写深度），
 *       否则同屏大量弹幕互相遮挡、后面的弹幕被前面的切成硬边。</li>
 *   <li><b>全亮</b>：原版开头 {@code glDisable(GL_LIGHTING)}，弹幕不受世界光照影响 →
 *       顶点 uv2 一律传 {@link LightTexture#FULL_BRIGHT}。</li>
 * </ol>
 *
 * <p>另一类是<b>几何/尺寸</b>的漏项（本轮补齐）：原版每个 form 的顶点是「以 half=1.0(或 2.0) 的
 * 局部坐标 + glScalef(size)」，而移植版用固定 half=0.5 的 quad，因此凡未乘 2 的形状都只有原版
 * 一半大；FORM_SCALE / FORM_RICE / FORM_OVAL 是程序化椭球体（原版用 laser.png），FORM_AMULET
 * 是御札双面卡片，FORM_KNIFE 用 ModelSilverKnife 立体模型 —— 这四类此前是 billboard 兜底。
 *
 * <p>贴图映射、UV 取法（8 色横向 sheet 用 color/8 选子图）、光弾的 2 帧动画（count%2 取 U 半幅）
 * 均与原版逐项核对过。
 */
public class RenderTHShot extends EntityRenderer<EntityTHShot>
{
    /* 贴图资源（1.7.10 的 textures/shot/*.png，文件名小写化） */
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
    private static final ResourceLocation TEX_FAMILIAR   = tex("textures/mob/familiar.png");

    private static final ResourceLocation TEX_KNIFE_RED    = tex("textures/shot/silverknife_red.png");
    private static final ResourceLocation TEX_KNIFE_BLUE   = tex("textures/shot/silverknife_blue.png");
    private static final ResourceLocation TEX_KNIFE_GREEN  = tex("textures/shot/silverknife_green.png");
    private static final ResourceLocation TEX_KNIFE_YELLOW = tex("textures/shot/silverknife_yellow.png");
    private static final ResourceLocation TEX_KNIFE_PURPLE = tex("textures/shot/silverknife_purple.png");
    private static final ResourceLocation TEX_KNIFE_AQUA   = tex("textures/shot/silverknife_aqua.png");
    private static final ResourceLocation TEX_KNIFE_ORANGE = tex("textures/shot/silverknife_orange.png");
    private static final ResourceLocation TEX_KNIFE_WHITE  = tex("textures/shot/silverknife_white.png");

    /** 原版 colorR/colorG/colorB（0..255）——8 种颜色 */
    private static final int[] COLOR_R = {224,   0,   0, 224, 224,   0, 255, 255};
    private static final int[] COLOR_G = {  0,   0, 224, 224,   0, 224, 128, 255};
    private static final int[] COLOR_B = {  0, 224,   0,   0, 224, 224,   0, 255};

    /** 原版 renderPhantomPart 的 damage 参数恒为 0（alpha = (40 - damage)/40 = 1.0） */
    private static final float PHANTOM_DAMAGE = 0.0F;

    private final ModelPart knifeModel;

    private static ResourceLocation tex(String path)
    {
        return new ResourceLocation(THKaguyaCore.MODID, path);
    }

    public RenderTHShot(EntityRendererProvider.Context context)
    {
        super(context);
        this.knifeModel = context.bakeLayer(ModelSilverKnife.LAYER_LOCATION);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHShot entity)
    {
        if (entity.getAnimationCount() < 0)
        {
            return TEX_LIGHT;
        }
        return getTexture(entity.getShotForm(), entity.getShotColor());
    }

    /** 按形状 + 颜色选择贴图（还原原版 getEntityTexture） */
    private static ResourceLocation getTexture(int form, int color)
    {
        switch (form)
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
            // 鱗弾 / 米弾 / 楕円弾 は原版いずれも Laser.png をプログラム楕円体に貼る
            case DanmakuConstants.FORM_SCALE:
            case DanmakuConstants.FORM_RICE:
            case DanmakuConstants.FORM_OVAL:
                return TEX_LASER;
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
        switch (color)
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

    /** 原版 renderNormalShot 的 8 色横向 sheet 形状（UV 用 color/8 选子图） */
    private static boolean isEightColorSheet(int form)
    {
        switch (form)
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

    // ==================================================================
    // 分派
    // ==================================================================
    @Override
    public void render(EntityTHShot entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        int form = entity.getShotForm();
        int anim = entity.getAnimationCount();

        // 原版：delayTime 期間（animationCount < 0）は弾本体を描かず光だけ見える
        if (anim < 0)
        {
            renderDelayEffect(entity, poseStack, buffer);
            super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        float pitch = entity.getXRot();
        float slope = entity.getAngleZ();

        switch (form)
        {
            case DanmakuConstants.FORM_SCALE:
                renderScaleShot(entity, yaw, pitch, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_BUTTERFLY:
                renderButterflyShot(entity, anim, yaw, pitch, slope, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_STAR:
            case DanmakuConstants.FORM_SMALLSTAR:
                renderStarShot(entity, anim, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_RICE:
                renderRiceShot(entity, yaw, pitch, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_CRYSTAL:
                renderCrystalShot(entity, yaw, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_LIGHT:
            case DanmakuConstants.FORM_BIGLIGHT:
                renderLightShot(entity, anim, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_KUNAI:
                renderKunaiShot(entity, yaw, pitch, slope, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_TALISMAN:
                renderTalismanShot(entity, yaw, pitch, slope, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_OVAL:
                renderOvalShot(entity, yaw, pitch, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_FAMILIAR:
                renderFamiliar(entity, anim, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_ARROW:
                renderArrowShot(entity, yaw, pitch, slope, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_PHANTOM:
                renderPhantomShot(entity, anim, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_AMULET:
                renderAmuletShot(entity, anim, yaw, pitch, slope, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_KNIFE:
                renderKnifeShot(entity, yaw, pitch, slope, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_WIND:
                renderWindShot(entity, anim, poseStack, buffer);
                break;
            case DanmakuConstants.FORM_KISHITU:
                renderKishituShot(entity, anim, poseStack, buffer);
                break;
            default:
                // SMALL / TINY / MEDIUM / PEARL / CIRCLE / HEART / BIG
                renderNormalShot(entity, poseStack, buffer);
                break;
        }

        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    // ==================================================================
    // 遅延光效（原版 renderLightEffect）—— delayTime 期間だけ見える発光
    //   混合：glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)（RenderTHShot 系）
    //   尺寸：min(10, -count) * 0.3F * size、上限 1.0F
    // ==================================================================
    private void renderDelayEffect(EntityTHShot entity, PoseStack poseStack, MultiBufferSource buffer)
    {
        int anim = entity.getAnimationCount();
        int delayCount = -anim;
        if (delayCount > 10)
        {
            delayCount = 10;
        }
        float size = delayCount * 0.3F * entity.getShotSize();
        if (size > 1.0F)
        {
            size = 1.0F;
        }

        int color = entity.getShotColor() % 8;
        // 原版 count % 2 在负数下会取到负 UV；此处用 floorMod 取 0/1（musoufuuin 两帧）
        int frame = Math.floorMod(anim, 2);
        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size * 2.0F, size * 2.0F, size * 2.0F); // 原版 width = 1.0F

        // musoufuuin.png は透明部分の RGB が赤のまま（1.7.10 の alpha test 前提アセット）なので、
        // 発光混合（alpha を見ない）で描く際は alpha discard 付きパスを使う。
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glowCutout(TEX_LIGHT));
        billboard(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 128, uMin, uMax);
        poseStack.popPose();
    }

    // ==================================================================
    // 普通弾 / 大弾（原版 renderNormalShot）
    //   blend=false（大部分）：无混合 → alpha test + 深度写入，白顶点色（靠贴图本身的颜色）
    //   blend=true （FORM_BIG）：加性混合 + 深度掩码关闭
    //   尺寸：顶点 ±1.0F → half=0.5 的 quad 需 ×2
    // ==================================================================
    private void renderNormalShot(EntityTHShot entity, PoseStack poseStack, MultiBufferSource buffer)
    {
        int form = entity.getShotForm();
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        ResourceLocation texture = getTexture(form, color);
        boolean blend = form == DanmakuConstants.FORM_BIG;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size * 2.0F, size * 2.0F, size * 2.0F);

        VertexConsumer consumer = buffer.getBuffer(blend
                ? THRenderTypes.additive(texture)
                : THRenderTypes.cutout(texture, false));

        float uMin = color / 8.0F;
        float uMax = (color + 1) / 8.0F;
        billboard(consumer, poseStack.last(), 255, 255, 255, 255, uMin, uMax);
        poseStack.popPose();
    }

    // ==================================================================
    // FORM_LIGHT / FORM_BIGLIGHT（原版 renderLightShot）
    //   加性混合 + 深度掩码关闭 + 2 帧动画（musoufuuin 左半/右半）
    //   外層 width=2.0F α0.8（彩色）→ 内層 width=1.0F α1.0（白芯，z+0.01）
    // ==================================================================
    private void renderLightShot(EntityTHShot entity, int anim, PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        int frame = Math.floorMod(anim, 2);
        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size * 4.0F, size * 4.0F, size * 4.0F); // 原版 width = 2.0F

        // 面向相机的 billboard 必须用 NoCull 系 RenderType（详见 billboard() 说明）
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additive(TEX_LIGHT));
        billboard(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 204, uMin, uMax);

        // 内層白芯（width=1.0F → 再 ×0.5）
        poseStack.translate(0.0F, 0.0F, 0.01F);
        poseStack.scale(0.5F, 0.5F, 1.0F);
        billboard(consumer, poseStack.last(), 255, 255, 255, 255, uMin, uMax);
        poseStack.popPose();
    }

    // ==================================================================
    // FORM_KISHITU（気質弾，原版 renderKishituShot）
    //   加性混合 + 深度掩码关闭；外層 width=2.0F α0.5 → 内層 width=1.6F α0.8
    //   ※ 緋想剣「全人類の緋想天」の主弾がこれ
    // ==================================================================
    private void renderKishituShot(EntityTHShot entity, int anim, PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        int frame = Math.floorMod(anim, 2);
        float uMin = (frame * 32F) / 64F;
        float uMax = (frame * 32F + 32F) / 64F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size * 4.0F, size * 4.0F, size * 4.0F); // 原版外層 width = 2.0F

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additive(TEX_LIGHT));
        billboard(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 128, uMin, uMax);

        // 内層白芯（width=1.6F → 1.6/2.0 = ×0.8）
        poseStack.translate(0.0F, 0.0F, 0.01F);
        poseStack.scale(0.8F, 0.8F, 1.0F);
        billboard(consumer, poseStack.last(), 255, 255, 255, 204, uMin, uMax);
        poseStack.popPose();
    }

    // ==================================================================
    // 星弾（原版 renderStarShot）：加性 + 深度掩码关闭 + 自旋 count*3.7°
    //   10 点程序化五角星（外 2.0 / 内 1.2，36°），5 个扇面 ×（彩色外圈 α0.7 + 白色内圈 α0.7）
    // ==================================================================
    private void renderStarShot(EntityTHShot entity, int anim, PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(anim * 3.7F));

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additive(TEX_STAR));

        float[] xPos = new float[11];
        float[] yPos = new float[11];
        float angle = 0F;
        float span = (float) Math.PI * 0.2F;   // 36°
        for (int i = 0; i < 10; i += 2)
        {
            xPos[i] = (float) Math.cos(angle) * 2.0F;
            yPos[i] = (float) Math.sin(angle) * 2.0F;
            angle += span;
            // 原版ここは 1.2F（外径の 60%）だが、凹角が浅すぎて画面上は五角形に見えていた
            // （白色内層 0.95 倍も凹角をほぼ覆う）。0.8F で五角星の輪郭が出る。意図的な差異。
            xPos[i + 1] = (float) Math.cos(angle) * 0.8F;
            yPos[i + 1] = (float) Math.sin(angle) * 0.8F;
            angle += span;
        }
        xPos[10] = xPos[0];
        yPos[10] = yPos[0];

        for (int i = 0; i < 9; i += 2)
        {
            //星の枠部（彩色）—— 原版は扇形を「3 点 + 中心」の四角形として描く
            //（中心頂点を省いて三角形 3 点で描くと、1 個あたり 30 頂点＝4 の倍数でなくなり、
            //  QUADS の共有バッファで隣の弾の頂点と組になって弾同士を結ぶ「棒」が出る）
            quad(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 178,
                    xPos[i], yPos[i], 0F, 0F, 0F,
                    xPos[i + 1], yPos[i + 1], 0F, 1F, 0F,
                    xPos[i + 2], yPos[i + 2], 0F, 1F, 1F,
                    0.0F, 0.0F, 0F, 0F, 1F);
            //星の中心部（白、0.7/0.95 縮小、z=0.01 で手前）
            quad(consumer, poseStack.last(), 255, 255, 255, 178,
                    xPos[i] * 0.7F, yPos[i] * 0.7F, 0.01F, 0F, 0F,
                    xPos[i + 1] * 0.95F, yPos[i + 1] * 0.95F, 0.01F, 1F, 0F,
                    xPos[i + 2] * 0.7F, yPos[i + 2] * 0.7F, 0.01F, 1F, 1F,
                    0.0F, 0.0F, 0.01F, 0F, 1F);
        }
        poseStack.popPose();
    }

    // ==================================================================
    // 使い魔（原版 renderFamiliar）：発光混合 (ONE, ONE_MINUS_SRC_COLOR)
    //   + billboard + 自旋 count*3.7° + depthMask(false) + α0.5、width=height=1.0F（×2）
    // ==================================================================
    private void renderFamiliar(EntityTHShot entity, int anim, PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size * 2.0F, size * 2.0F, size * 2.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(anim * 3.7F));

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX_FAMILIAR));
        billboard(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 128, 0.0F, 1.0F);
        poseStack.popPose();
    }

    // ==================================================================
    // 蝶弾（原版 renderButterflyShot）：加性 + 両面 + 深度掩码关闭
    //   wingAngle = sin(count/3)*45°，翅宽 width=2.0F，白色高光 α0.4
    // ==================================================================
    private void renderButterflyShot(EntityTHShot entity, int anim, float yaw, float pitch, float slope,
                                     PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        float wingAngle = Mth.sin(anim / 3.0F) * 45.0F;
        if (pitch > 90F)
        {
            pitch = 90F - pitch % 90F;
        }
        else if (pitch < -90F)
        {
            pitch = -90F - pitch % 90F;
        }

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(slope));
        poseStack.mulPose(Axis.ZP.rotationDegrees(wingAngle));

        // butterflyshot.png 128×64、UV は左 1/4 幅のみ使用
        float uMin = 0F;
        float uMax = 32F / 128F;
        float w = 2.0F;
        float w2 = 1.8F;

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additive(TEX_BUTTERFLY));

        // 右の翅 + 白い高光
        quad(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 255,
                0.0F, 0.0F, w, uMax, 0F,
                w, 0.0F, w, uMin, 0F,
                w, 0.0F, -w, uMin, 1F,
                0.0F, 0.0F, -w, uMax, 1F);
        quad(consumer, poseStack.last(), 255, 255, 255, 102,
                0.0F, 0.0F, w2, uMax, 0F,
                w2, 0.0F, w2, uMin, 0F,
                w2, 0.0F, -w2, uMin, 1F,
                0.0F, 0.0F, -w2, uMax, 1F);

        // 左の翅（-wingAngle*2 反転）
        poseStack.mulPose(Axis.ZP.rotationDegrees(-wingAngle * 2.0F));
        quad(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 255,
                0.0F, 0.0F, w, uMax, 0F,
                -w, 0.0F, w, uMin, 0F,
                -w, 0.0F, -w, uMin, 1F,
                0.0F, 0.0F, -w, uMax, 1F);
        quad(consumer, poseStack.last(), 255, 255, 255, 102,
                0.0F, 0.0F, w2, uMax, 0F,
                -w2, 0.0F, w2, uMin, 0F,
                -w2, 0.0F, -w2, uMin, 1F,
                0.0F, 0.0F, -w2, uMax, 1F);

        poseStack.popPose();
    }

    // ==================================================================
    // 札弾（原版 renderTalismanShot）：無混合（alpha test）+ 両面
    //   width=1.6F、length=2.0D、UV は 8 色 sheet の color 番目
    // ==================================================================
    private void renderTalismanShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                    PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        float u1 = color / 8.0F;
        float u2 = (color + 1) / 8.0F;
        float width = 1.6F;
        float len = 2.0F;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(slope));

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.cutout(TEX_TALISMAN, false));
        quad(consumer, poseStack.last(), 255, 255, 255, 255,
                width, 0.0F, len, u2, 0F,
                -width, 0.0F, len, u1, 0F,
                -width, 0.0F, -len, u1, 1F,
                width, 0.0F, -len, u2, 1F);
        poseStack.popPose();
    }

    // ==================================================================
    // 御札弾（原版 renderAmuletShot）：発光混合 + 両面 + 深度マスク关闭
    //   180 - count*23° で回転、UV 前半＝札の下地、後半＝色付きの縁（1.2 倍）
    // ==================================================================
    private void renderAmuletShot(EntityTHShot entity, int anim, float yaw, float pitch, float slope,
                                  PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        ResourceLocation texture = getTexture(DanmakuConstants.FORM_AMULET, color);

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(slope));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - (float) anim * 23.0F));

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(texture));
        // 札の下地（原版は色指定なし＝白）
        quad(consumer, poseStack.last(), 255, 255, 255, 255,
                -1.0F, 0.0F, 1.0F, 0.0F, 0.0F,
                1.0F, 0.0F, 1.0F, 0.5F, 0.0F,
                1.0F, 0.0F, -1.0F, 0.5F, 1.0F,
                -1.0F, 0.0F, -1.0F, 0.0F, 1.0F);
        // 色付きの縁（1.2 倍、α0.6）
        quad(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 153,
                -1.2F, 0.0F, 1.2F, 0.5F, 0.0F,
                1.2F, 0.0F, 1.2F, 1.0F, 0.0F,
                1.2F, 0.0F, -1.2F, 1.0F, 1.0F,
                -1.2F, 0.0F, -1.2F, 0.5F, 1.0F);
        poseStack.popPose();
    }

    // ==================================================================
    // 風弾（原版 renderWindShot）：発光混合 + 両面 + 深度書き込みあり
    //   size*0.5、45° ずつ 8 枚の板、ランダム揺らぎ（原版どおり毎フレーム再抽選）
    // ==================================================================
    private void renderWindShot(EntityTHShot entity, int anim, PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize() * 0.5F;

        Random random = new Random();
        float rand1 = random.nextInt(50) / 100F;
        float rand2 = random.nextInt(100) / 100F + 4F;
        int pattern = anim % 4;
        float u1 = ((pattern % 2) * 32 + 0) / 64F;
        float u2 = ((pattern % 2) * 32 + 32) / 64F;
        float v1 = ((int) (pattern / 2) * 16 + 0) / 32F;
        float v2 = ((int) (pattern / 2) * 16 + 16) / 32F;
        float f9 = 0.5F;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        // 原版は depthMask を触らない＝深度書き込みあり
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(TEX_WIND, false, true));

        for (int i = 0; i < 8; i++)
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(45F));
            quad(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 178,
                    0.0F - rand1, -1.0F - f9, 0.0F, u1, v2,
                    0.0F + rand1, -1.0F - f9, 0.0F, u2, v2,
                    0.0F + rand2, 2.0F - f9, 2.0F, u2, v1,
                    0.0F - rand2, 2.0F - f9, 2.0F, u1, v1);
        }
        poseStack.popPose();
    }

    // ==================================================================
    // 幽霊弾（原版 FORM_PHANTOM 分岐 + renderPhantomPart）
    //   純加算 (ONE, ONE) + 深度マスク关闭 + billboard、3 層（拡大縮小＋揺らぎ）
    //   ※ 原版は shotSize を使わず size=1.0F 固定
    // ==================================================================
    private void renderPhantomShot(EntityTHShot entity, int anim, PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        double timeR = (double) anim / 180.0D * Math.PI;
        // 純加算 (ONE, ONE) は alpha も無視するため、透明部の RGB が残る musoufuuin.png では
        // alpha discard 付きパスが必要（さもないと弾全体が赤い四角になる）。
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.doubleAddCutout(TEX_LIGHT));

        poseStack.pushPose();
        poseStack.scale(1.0F, 1.0F, 1.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        phantomPart(consumer, poseStack.last(), color, timeR);

        poseStack.translate((float) Math.sin(timeR * 5) * 0.2F, 0.1F + (anim % 20) * 0.06F, 0.0F);
        float s2 = 0.9F * ((20F - (float) (anim % 20)) / 20F);
        poseStack.scale(s2, s2, s2);
        phantomPart(consumer, poseStack.last(), color, timeR);

        poseStack.translate((float) Math.cos(timeR * 5) * 0.2F, 0.1F + (anim % 20) * 0.07F, 0.0F);
        float s3 = 0.8F * ((20F - (float) (anim % 20)) / 20F);
        poseStack.scale(s3, s3, s3);
        phantomPart(consumer, poseStack.last(), color, timeR);
        poseStack.popPose();
    }

    /** 原版 renderPhantomPart：揺らぐ二枚 + 中央三枚（damage>0 で赤、通常は 0） */
    private static void phantomPart(VertexConsumer consumer, PoseStack.Pose pose, int color, double timeR)
    {
        float umin = 0.0F;
        float umax = 32F / 64F;
        float alpha = (40F - PHANTOM_DAMAGE) / 40F;
        float r = COLOR_R[color] * 0.3F;
        float g = COLOR_G[color] * 0.3F;
        float b = COLOR_B[color] * 0.3F;

        quad(consumer, pose, (int) r, (int) g, (int) b, (int) (0.3F * alpha * 255F),
                -0.7F + (float) Math.sin(timeR * 3.0D) * 0.1F, -0.2F - (float) Math.cos(timeR * 7.0D) * 0.1F, 0.0F, umin, 1F,
                0.7F - (float) Math.cos(timeR * 4.0D) * 0.1F, -0.2F - (float) Math.sin(timeR * 5.0D) * 0.1F, 0.0F, umax, 1F,
                0.7F + (float) Math.sin(timeR * 5.0D) * 0.1F, 1.2F + (float) Math.cos(timeR * 4.0D) * 0.1F, 0.0F, umax, 0F,
                -0.7F - (float) Math.cos(timeR * 7.0D) * 0.1F, 1.2F + (float) Math.sin(timeR * 3.0D) * 0.1F, 0.0F, umin, 0F);

        quad(consumer, pose, (int) r, (int) g, (int) b, (int) (0.7F * alpha * 255F),
                -0.6F - (float) Math.cos(timeR * 7.0D) * 0.1F, -0.1F + (float) Math.sin(timeR * 5.0D) * 0.1F, 0.001F, umin, 1F,
                0.6F + (float) Math.sin(timeR * 3.0D) * 0.1F, -0.1F + (float) Math.cos(timeR * 4.0D) * 0.1F, 0.001F, umax, 1F,
                0.6F - (float) Math.cos(timeR * 4.0D) * 0.1F, 1.1F - (float) Math.sin(timeR * 3.0D) * 0.1F, 0.001F, umax, 0F,
                -0.6F + (float) Math.sin(timeR * 5.0D) * 0.1F, 1.1F - (float) Math.cos(timeR * 7.0D) * 0.1F, 0.001F, umin, 0F);

        for (int i = 0; i < 3; i++)
        {
            int cr = 255;
            int cg = 0;
            int cb = 0;
            if (PHANTOM_DAMAGE <= 0.0F)
            {
                cr = 255;
                cg = 255;
                cb = 255;
            }
            quad(consumer, pose, cr, cg, cb, (int) (alpha * 255F),
                    -0.5F, 0.0F, 0.002F, umin, 1F,
                    0.5F, 0.0F, 0.002F, umax, 1F,
                    0.5F, 1.0F, 0.002F, umax, 0F,
                    -0.5F, 1.0F, 0.002F, umin, 0F);
        }
    }

    // ==================================================================
    // 結晶弾（原版 renderCrystalShot）：加性 + 深度マスク关闭
    //   白い内側 4 面（α1.0）+ 色付き外側 4 面（α0.7）
    // ==================================================================
    private void renderCrystalShot(EntityTHShot entity, float yaw, PoseStack poseStack, MultiBufferSource buffer)
    {
        float size = entity.getShotSize();
        int color = entity.getShotColor() % 8;
        float pitch = entity.getXRot();

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additiveCull(TEX_STAR));

        float width = 0.96F;
        float width2 = width * 0.8F;
        float length = 4.0F;
        float lengthB = 1.0F;
        float length2 = length * 0.8F;
        float length2B = lengthB * 0.4F;

        for (int i = 0; i < 4; i++)
        {
            tri3(consumer, poseStack.last(), 255, 255, 255, 255,
                    0.0F, width2, 0.0F,
                    -width2, 0.0F, 0.0F,
                    0.0F, 0.0F, length2);
            tri3(consumer, poseStack.last(), 255, 255, 255, 255,
                    0.0F, width2, 0.0F,
                    width2, 0.0F, 0.0F,
                    0.0F, 0.0F, -length2B);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        for (int i = 0; i < 4; i++)
        {
            tri3(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 178,
                    0.0F, width, 0.0F,
                    -width, 0.0F, 0.0F,
                    0.0F, 0.0F, length);
            tri3(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 178,
                    0.0F, width, 0.0F,
                    width, 0.0F, 0.0F,
                    0.0F, 0.0F, -lengthB);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
        poseStack.popPose();
    }

    // ==================================================================
    // 鱗弾（原版 renderScaleShot）── 幽香の日傘「花弾」がこれ
    //   回転：yaw + 180F / pitch（他形状と符号が違う点に注意）
    //   白芯＝楕円体（無混合・表面描画）／ 色付き＝半楕円体（加性・両面・depthMask false）
    // ==================================================================
    private void renderScaleShot(EntityTHShot entity, float yaw, float pitch,
                                 PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        double length = 2.0D;
        float width = 1.0F;
        int zAngleDivNum = 7;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // 白芯（原版未開混合 = alpha test、-0.85 は z オフセット、color=7 は白）
        ovalPart(buffer.getBuffer(THRenderTypes.cutout(TEX_LASER, true)), poseStack.last(),
                length * 0.4D, width * 0.5F, -0.85D, zAngleDivNum, 5, 7, 1.0F);
        // 色付き半楕円体（加性・両面・depthMask false）
        halfOvalPart(buffer.getBuffer(THRenderTypes.additive(TEX_LASER)), poseStack.last(),
                length, width * 1.2F, -1.0D, zAngleDivNum, 8, color, 0.31F);

        poseStack.popPose();
    }

    // ==================================================================
    // 米弾（原版 renderRiceShot）：白芯（無混合）+ 色付き（加性）
    // ==================================================================
    private void renderRiceShot(EntityTHShot entity, float yaw, float pitch,
                                PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        double length = 4.0D;
        float width = 1.20F;
        double zPos = -2.0D;
        int zAngleDivNum = 7;
        int zDivNum = 5;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.scale(size, size, size);

        ricePart(buffer.getBuffer(THRenderTypes.cutout(TEX_LASER, true)), poseStack.last(),
                length * 0.8D, width * 0.8F, zPos + length * 0.1D, zAngleDivNum, zDivNum, 7, 1.0F);
        ricePart(buffer.getBuffer(THRenderTypes.additive(TEX_LASER)), poseStack.last(),
                length, width, zPos, zAngleDivNum, zDivNum, color, 0.7F);

        poseStack.popPose();
    }

    // ==================================================================
    // 楕円弾（原版 renderOvalShot）：白芯（無混合）+ 色付き（加性）
    // ==================================================================
    private void renderOvalShot(EntityTHShot entity, float yaw, float pitch,
                                PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();
        double length = 2.0D;
        float width = 1.0F;
        double zPos = -1.0D;
        int zAngleDivNum = 7;
        int zDivNum = 5;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        ovalPart(buffer.getBuffer(THRenderTypes.cutout(TEX_LASER, true)), poseStack.last(),
                length * 0.8D, width * 0.8F, zPos + length * 0.1D, zAngleDivNum, zDivNum, 7, 1.0F);
        ovalPart(buffer.getBuffer(THRenderTypes.additive(TEX_LASER)), poseStack.last(),
                length, width, zPos, zAngleDivNum, zDivNum, color, 0.7F);

        poseStack.popPose();
    }

    /** 原版 renderOvalShotPart：zPos = zOffset + sin(angle)*length の紡錘体 */
    private static void ovalPart(VertexConsumer consumer, PoseStack.Pose pose,
                                 double length, float maxWidth, double zOffset,
                                 int zAngleDivNum, int zDivNum, int color, float alpha)
    {
        double angleSpanZ = Math.PI * 2.0D / (double) zAngleDivNum;
        double zPosOld = zOffset - length;
        float xPosOld = 0F;
        float yPosOld = 0F;
        float xPos2Old = 0F;
        float yPos2Old = 0F;
        float angle = -(float) Math.PI / 2.0F;
        float angleSpan = (float) Math.PI / (float) zDivNum;
        angle += angleSpan;
        float widthOld = 0.0F;

        for (int j = 0; j < zDivNum; j++)
        {
            double zPos = zOffset + Math.sin(angle) * length;
            float w = (float) Math.cos(angle) * maxWidth;
            float xPos = w;
            float yPos = 0F;
            xPosOld = (float) Math.cos(0.0D) * w;
            yPosOld = (float) Math.sin(0.0D) * w;
            xPos2Old = (float) Math.cos(0.0D) * widthOld;
            yPos2Old = (float) Math.sin(0.0D) * widthOld;
            double angleZ = angleSpanZ;

            for (int i = 0; i <= zAngleDivNum; i++)
            {
                xPos = (float) Math.cos(angleZ) * w;
                yPos = (float) Math.sin(angleZ) * w;
                float xPos2 = (float) Math.cos(angleZ) * widthOld;
                float yPos2 = (float) Math.sin(angleZ) * widthOld;

                stripQuad(consumer, pose, color, alpha,
                        xPosOld, yPosOld, (float) zPos,
                        xPos2Old, yPos2Old, (float) zPosOld,
                        xPos2, yPos2, (float) zPosOld,
                        xPos, yPos, (float) zPos);

                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }
            zPosOld = zPos;
            angle += angleSpan;
            widthOld = w;
        }
    }

    /** 原版 renderRiceShotPart：zPos += zDivLength の円柱状（米弾＝細長い米形） */
    private static void ricePart(VertexConsumer consumer, PoseStack.Pose pose,
                                 double length, float maxWidth, double zPos,
                                 int zAngleDivNum, int zDivNum, int color, float alpha)
    {
        double angleSpanZ = Math.PI * 2.0D / (double) zAngleDivNum;
        double zDivLength = length / (double) (zDivNum - 1);
        double zPosOld = zPos;
        float xPosOld = 0F;
        float yPosOld = 0F;
        float xPos2Old = 0F;
        float yPos2Old = 0F;
        float angle = -(float) Math.PI / 2.0F;
        float angleSpan = (float) Math.PI / (float) zDivNum;
        angle += angleSpan;
        float widthOld = 0.0F;

        for (int j = 0; j < zDivNum; j++)
        {
            zPos += zDivLength;
            float w = (float) Math.cos(angle) * maxWidth;
            xPosOld = (float) Math.cos(0.0D) * w;
            yPosOld = (float) Math.sin(0.0D) * w;
            xPos2Old = (float) Math.cos(0.0D) * widthOld;
            yPos2Old = (float) Math.sin(0.0D) * widthOld;
            double angleZ = angleSpanZ;

            for (int i = 0; i <= zAngleDivNum; i++)
            {
                float xPos = (float) Math.cos(angleZ) * w;
                float yPos = (float) Math.sin(angleZ) * w;
                float xPos2 = (float) Math.cos(angleZ) * widthOld;
                float yPos2 = (float) Math.sin(angleZ) * widthOld;

                stripQuad(consumer, pose, color, alpha,
                        xPosOld, yPosOld, (float) zPos,
                        xPos2Old, yPos2Old, (float) zPosOld,
                        xPos2, yPos2, (float) zPosOld,
                        xPos, yPos, (float) zPos);

                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }
            zPosOld = zPos;
            angle += angleSpan;
            widthOld = w;
        }
    }

    /** 原版 renderHarfOvalShotPart：半円（zDivNum/2+1 段）で α が 0.05 ずつ減衰、両面 */
    private static void halfOvalPart(VertexConsumer consumer, PoseStack.Pose pose,
                                     double length, float maxWidth, double zOffset,
                                     int zAngleDivNum, int zDivNum, int color, float alpha)
    {
        double angleSpanZ = Math.PI * 2.0D / (double) zAngleDivNum;
        double zPosOld = -length;
        float xPosOld = 0F;
        float yPosOld = 0F;
        float xPos2Old = 0F;
        float yPos2Old = 0F;
        float angle = -(float) Math.PI / 2.0F;
        float angleSpan = (float) Math.PI / (float) zDivNum;
        angle += angleSpan;
        float widthOld = 0.0F;
        int steps = (zDivNum / 2) + 1;

        for (int j = 0; j < steps; j++)
        {
            double zPos = Math.sin(angle) * length;
            float w = (float) Math.cos(angle) * maxWidth;
            xPosOld = (float) Math.cos(0.0D) * w;
            yPosOld = (float) Math.sin(0.0D) * w;
            xPos2Old = (float) Math.cos(0.0D) * widthOld;
            yPos2Old = (float) Math.sin(0.0D) * widthOld;
            double angleZ = angleSpanZ;

            for (int i = 0; i <= zAngleDivNum; i++)
            {
                float xPos = (float) Math.cos(angleZ) * w;
                float yPos = (float) Math.sin(angleZ) * w;
                float xPos2 = (float) Math.cos(angleZ) * widthOld;
                float yPos2 = (float) Math.sin(angleZ) * widthOld;

                stripQuad(consumer, pose, color, alpha,
                        xPosOld, yPosOld, (float) zPos,
                        xPos2Old, yPos2Old, (float) zPosOld,
                        xPos2, yPos2, (float) zPosOld,
                        xPos, yPos, (float) zPos);

                xPosOld = xPos;
                yPosOld = yPos;
                xPos2Old = xPos2;
                yPos2Old = yPos2;
                angleZ += angleSpanZ;
            }
            alpha -= j * 0.05F;
            zPosOld = zPos;
            angle += angleSpan;
            widthOld = w;
        }
    }

    // ==================================================================
    // ナイフ弾（原版 FORM_KNIFE：ModelSilverKnife 立体モデル）
    //   glScalef(size * 3) してモデルを 1/16 スケールで描画
    // ==================================================================
    private void renderKnifeShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                 PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize() * 3.0F;

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(slope));

        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.cutout(getKnifeTexture(color), true));
        this.knifeModel.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    // ==================================================================
    // クナイ弾（原版 renderKunaiShot）：白い刀身（無混合・表面）+ 色付き外層（発光）
    //   把手のみ両面描画
    // ==================================================================
    private void renderKunaiShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                 PoseStack poseStack, MultiBufferSource buffer)
    {
        // 原版：柄の先端 root = -2.6F、柄 UV v 開始 0.375
        renderBladeAndHandle(entity, TEX_KUNAI, -2.6F, 0.375F, yaw, pitch, slope, poseStack, buffer);
    }

    // ==================================================================
    // 矢弾（原版 renderArrowShot）：構造はクナイと同じ、柄が長い（root = -5.8F）
    // ==================================================================
    private void renderArrowShot(EntityTHShot entity, float yaw, float pitch, float slope,
                                 PoseStack poseStack, MultiBufferSource buffer)
    {
        // 原版：柄の先端 root = -5.8F、柄 UV v 開始 0.0
        renderBladeAndHandle(entity, TEX_ARROW, -5.8F, 0.0F, yaw, pitch, slope, poseStack, buffer);
    }

    /**
     * クナイ / 矢 の共通形状（原版 renderKunaiShot / renderArrowShot は柄の長さと柄 UV だけが違う）。
     *
     * @param root      柄の先端 Z
     * @param handleV0  柄 UV の v 開始（クナイ 0.375 / 矢 0.0）
     */
    private void renderBladeAndHandle(EntityTHShot entity, ResourceLocation texture, float root,
                                      float handleV0, float yaw, float pitch, float slope,
                                      PoseStack poseStack, MultiBufferSource buffer)
    {
        int color = entity.getShotColor() % 8;
        float size = entity.getShotSize();

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        // 姿勢は「従来版で正南（yaw = 0, pitch = 0）へ撃ったとき」で固定する。
        // 発射方向に応じて yaw / pitch を回すと、見る角度によって環が真横になって見えなくなるため、
        // どの方向へ撃っても正南発射と同じ見た目になるよう yaw / pitch の回転は行わない。
        // （自転成分 slope だけは残す）
        poseStack.mulPose(Axis.ZP.rotationDegrees(slope));

        // ---- 白い刀身（原版は色指定なし＝白・無混合・表面描画）----
        emitBlade(buffer.getBuffer(THRenderTypes.cutout(texture, true)), poseStack.last(),
                0.8F, 1.12F, -0.8F, -1.0F, 0.24F, 255, 255, 255, 255);
        // 柄（原版が明示的に両面描画へ切り替える部分）
        emitHandle(buffer.getBuffer(THRenderTypes.cutout(texture, false)), poseStack.last(),
                0.6F, -1.0F, root, 0.50F, handleV0, 0.75F, 255, 255, 255, 255);

        // ---- 色付き外層（原版は 発光混合 (ONE, ONE_MINUS_SRC_COLOR)・頂点は一回り大きい）----
        emitBlade(buffer.getBuffer(THRenderTypes.glow(texture, true, true)), poseStack.last(),
                1.0F, 1.4F, -0.8F, -1.0F, 0.3F,
                COLOR_R[color], COLOR_G[color], COLOR_B[color], 255);
        emitHandle(buffer.getBuffer(THRenderTypes.glow(texture, false, true)), poseStack.last(),
                0.6F, -1.0F, root, 0.50F, handleV0, 0.75F,
                COLOR_R[color], COLOR_G[color], COLOR_B[color], 255);

        poseStack.popPose();
    }

    /** 原版の刀身 6 面（上 / 下左 / 下右 / 上左 / 下左 / 上右 / 下右 の 7 枚） */
    private static void emitBlade(VertexConsumer c, PoseStack.Pose pose,
                                  float width, float flont, float back, float back2, float height,
                                  int r, int g, int b, int a)
    {
        quad(c, pose, r, g, b, a, -width, 0.0F, back, 0.50F, 1F, 0.0F, 0.0F, flont, 0.25F, 0F,
                width, 0.0F, back, 0.00F, 1F, 0.0F, height, back, 0.25F, 1F);
        quad(c, pose, r, g, b, a, 0.0F, 0.0F, flont, 0.25F, 0F, 0.0F, 0.0F, flont, 0.25F, 0F,
                0.0F, -height, back, 0.25F, 1F, width, 0.0F, back, 0.00F, 1F);
        quad(c, pose, r, g, b, a, 0.0F, 0.0F, flont, 0.25F, 0F, 0.0F, 0.0F, flont, 0.25F, 0F,
                -width, 0.0F, back, 0.50F, 1F, 0.0F, -height, back, 0.25F, 1F);
        quad(c, pose, r, g, b, a, 0.0F, 0.0F, back2, 0.25F, 0F, 0.0F, 0.0F, back2, 0.25F, 0F,
                0.0F, height, back, 0.25F, 1F, width, 0.0F, back, 0.00F, 1F);
        quad(c, pose, r, g, b, a, 0.0F, 0.0F, back2, 0.25F, 0F, 0.0F, 0.0F, back2, 0.25F, 0F,
                width, 0.0F, back, 0.00F, 1F, 0.0F, -height, back, 0.25F, 1F);
        quad(c, pose, r, g, b, a, 0.0F, 0.0F, back2, 0.25F, 0F, 0.0F, 0.0F, back2, 0.25F, 0F,
                -width, 0.0F, back, 0.50F, 1F, 0.0F, height, back, 0.25F, 1F);
        quad(c, pose, r, g, b, a, 0.0F, 0.0F, back2, 0.25F, 0F, 0.0F, 0.0F, back2, 0.25F, 0F,
                0.0F, -height, back, 0.25F, 1F, -width, 0.0F, back, 0.50F, 1F);
    }

    /** 原版の柄（両面） */
    private static void emitHandle(VertexConsumer c, PoseStack.Pose pose,
                                   float width2, float back2, float root,
                                   float u1, float v0, float u2,
                                   int r, int g, int b, int a)
    {
        quad(c, pose, r, g, b, a,
                width2, 0.0F, back2, u1, v0,
                -width2, 0.0F, back2, u2, v0,
                -width2, 0.0F, root, u2, 1F,
                width2, 0.0F, root, u1, 1F);
    }

    // ==================================================================
    // 低層 helper
    // ==================================================================

    /**
     * カメラ正面向き billboard 用の 4 頂点 quad（左下→右下→右上→左上、Z=0 平面）。
     *
     * <p>【不変条件】呼び出し側は <b>CULL なしの RenderType</b> を使うこと。
     * {@code mulPose(cameraOrientation())} の座標系では本頂点順が「裏面」と判定され、
     * 背面剔除つき RenderType を渡すと quad 全体が消える（実測：FORM_LIGHT / FORM_KISHITU）。
     *
     * <p>顶点は全亮（FULL_BRIGHT）。原版 {@code glDisable(GL_LIGHTING)} 相当。
     */
    private static void billboard(VertexConsumer consumer, PoseStack.Pose pose,
                                  int r, int g, int b, int a, float u1, float u2)
    {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float half = 0.5F;
        consumer.vertex(matrix, -half, -half, 0.0F).color(r, g, b, a)
                .uv(u1, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, -half, 0.0F).color(r, g, b, a)
                .uv(u2, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, half, half, 0.0F).color(r, g, b, a)
                .uv(u2, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half, half, 0.0F).color(r, g, b, a)
                .uv(u1, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 通用 4 顶点 quad（原版 addVertexWithUV 顺序、顶点要素齐全） */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             int r, int g, int b, int a,
                             float ax, float ay, float az, float uA, float vA,
                             float bx, float by, float bz, float uB, float vB,
                             float cx, float cy, float cz, float uC, float vC,
                             float dx, float dy, float dz, float uD, float vD)
    {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        consumer.vertex(matrix, ax, ay, az).color(r, g, b, a)
                .uv(uA, vA).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(r, g, b, a)
                .uv(uB, vB).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, cz).color(r, g, b, a)
                .uv(uC, vC).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, dx, dy, dz).color(r, g, b, a)
                .uv(uD, vD).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 楕円体面片（UV は原版どおり (0,0)(0,1)(1,1)(1,0)） */
    private static void stripQuad(VertexConsumer consumer, PoseStack.Pose pose, int color, float alpha,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3)
    {
        quad(consumer, pose, COLOR_R[color], COLOR_G[color], COLOR_B[color], (int) (alpha * 255F),
                x0, y0, z0, 0.0F, 0.0F,
                x1, y1, z1, 0.0F, 1.0F,
                x2, y2, z2, 1.0F, 1.0F,
                x3, y3, z3, 1.0F, 0.0F);
    }

    /** 三角形を「退化四角形（最終頂点を複製）」で描く —— 結晶弾の面。
     *  原版も 1.7.10 の startDrawingQuads に同じ頂点を 2 回入れて三角形を表現していた。
     *  3 頂点だけ流すと QUADS の共有バッファで 4 頂点境界がずれ、隣の弾と混ざる。 */
    private static void tri3(VertexConsumer consumer, PoseStack.Pose pose, int r, int g, int b, int a,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz)
    {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        consumer.vertex(matrix, ax, ay, az).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, cz).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, cz).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
