package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.shot.ModelSilverKnife;
import thKaguyaMod.entity.item.EntitySilverKnife;

/**
 * 銀のナイフ（投擲物）の描画。1.7.10 原版 {@code RenderSilverKnife} の 1:1 移植。
 *
 * <p>原版は {@link ModelSilverKnife}（鍔 7×1×3 / 刀身 3×14×1 / 刀尖 1×1×1）を
 * {@code glRotatef(180-yaw, Y) → glRotatef(-rotationPitch, X) → glRotatef(knifeZAngle, Z)}
 * → {@code glScalef(0.5)} → {@code model.render(..., 0.0625F)} で描いていた。
 * 移植初期は「物品アイコン（16×16）を貼った細長い quad」で代用していたが、
 * モデル・テクスチャ（textures/shot/silverknife_*.png、64×32）とも揃っているため
 * 本来の 3D 描画へ戻した（2026-10-21）。
 *
 * <p>テクスチャは必ず {@code textures/shot/} 側（64×32 の UV 展開図）。
 * {@code textures/item/silver_knife_*.png} は 16×16 のアイコンで、UV が合わない。
 *
 * <p>回転は「180 - yaw」：この実体の yaw は 1.20.1 のバニラ規約（+Z 基準・反時計回り）で
 * 入っているため、1.7.10 と同じ式をそのまま使う（弾幕の atan2 系とは別規約）。
 */
public class RenderSilverKnife extends EntityRenderer<EntitySilverKnife>
{
    private static final ResourceLocation TEX_BLUE  = tex("textures/shot/silverknife_blue.png");
    private static final ResourceLocation TEX_RED   = tex("textures/shot/silverknife_red.png");
    private static final ResourceLocation TEX_GREEN = tex("textures/shot/silverknife_green.png");
    private static final ResourceLocation TEX_WHITE = tex("textures/shot/silverknife_white.png");

    private static ResourceLocation tex(String path)
    {
        return new ResourceLocation(THKaguyaCore.MODID, path);
    }

    private final ModelPart model;

    public RenderSilverKnife(EntityRendererProvider.Context context)
    {
        super(context);
        this.model = context.bakeLayer(ModelSilverKnife.LAYER_LOCATION);
        this.shadowRadius = 0.3F; // 原版 shadowSize
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySilverKnife entity)
    {
        return getKnifeTexture(entity.getKnifeColor());
    }

    private static ResourceLocation getKnifeTexture(int color)
    {
        switch(color)
        {
            case 0:  return TEX_BLUE;
            case 1:  return TEX_RED;
            case 2:  return TEX_GREEN;
            default: return TEX_WHITE;
        }
    }

    @Override
    public void render(EntitySilverKnife knife, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 原版：glRotatef(180F - yaw, 0,1,0); glRotatef(-rotationPitch, 1,0,0); glRotatef(knifeZAngle, 0,0,1)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-knife.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(knife.getKnifeZAngle()));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        ResourceLocation texture = getKnifeTexture(knife.getKnifeColor());
        // 両面描画：1.7.10 は直前の GL 状態任せだったが、刃が 1/16 厚の板なので
        // 面の向きで消える余地を残さない（刀弾 renderKnifeShot と同じ判断）。
        this.model.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)),
                packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(knife, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
