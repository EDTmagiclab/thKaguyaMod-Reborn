package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.shot.EntityHomingAmulet;

/**
 * ホーミングアミュレット／拡散アミュレットの描画（1.20.1 移植版）。
 *
 * <p>原版 {@code RenderHomingAmulet}（109 行）は専用レンダラで、発光混合
 * (GL_ONE, GL_ONE_MINUS_SRC_COLOR)・両面・水平な 2 枚（白 α1.0 → 色付き α0.6、累積 1.1 倍）を
 * X 軸 -pitch、Y 軸 180-anim*23 で回す。size は 0.5（shotColor が奇数なら 1.2）、
 * 貼図は shotColor/2==0 なら HomingAmulet.png、それ以外は DiffusionAmulet.png。
 * 移植版は汎用 {@code RenderTHShot} の FORM_AMULET 分岐で代用していたため、
 * この専用形状（水平 2 枚・拡散アミュレットの色分け）が失われていた。
 */
public class RenderHomingAmulet extends EntityRenderer<EntityHomingAmulet>
{
    private static final ResourceLocation HOMING_TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/homingamulet.png");
    private static final ResourceLocation DIFFUSION_TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/diffusionamulet.png");

    private static final int[] COLOR_R = {255,  25,   0, 255};
    private static final int[] COLOR_G = { 25,  25, 255, 255};
    private static final int[] COLOR_B = { 25, 255,   0,   0};

    public RenderHomingAmulet(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityHomingAmulet entity)
    {
        return entity.getShotColor() / 2 == 0 ? HOMING_TEXTURE : DIFFUSION_TEXTURE;
    }

    @Override
    public void render(EntityHomingAmulet entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        int color = Math.floorMod(entity.getShotColor() / 2, COLOR_R.length);
        float size = (entity.getShotColor() % 2 == 1) ? 1.2F : 0.5F;
        int anim = entity.getAnimationCount();

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        // 原版は X 軸 -pitch、Y 軸 180 - anim*23
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - (float)anim * 23.0F));

        // 両面（原版は GL_CULL_FACE を切っている）
        VertexConsumer consumer = buffer.getBuffer(THRenderTypes.glow(getTextureLocation(entity), false, true));

        // 1 枚目：白 α1.0
        quad(consumer, poseStack.last(), 255, 255, 255, 255);

        // 2 枚目：size *= 1.1 をさらに掛けて色付き α0.6
        poseStack.scale(size * 1.1F, size * 1.1F, size * 1.1F);
        quad(consumer, poseStack.last(), COLOR_R[color], COLOR_G[color], COLOR_B[color], 153);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 原版の水平 quad（y=0、x ±0.5、z ±0.5、U 0..0.5、V 0..1） */
    private static void quad(VertexConsumer c, PoseStack.Pose pose, int r, int g, int b, int a)
    {
        float xLength = 0.5F;
        float zLength = 0.5F;
        vertex(c, pose, -xLength, 0.0F,  zLength, 0.0F, 0.0F, r, g, b, a);
        vertex(c, pose,  xLength, 0.0F,  zLength, 0.5F, 0.0F, r, g, b, a);
        vertex(c, pose,  xLength, 0.0F, -zLength, 0.5F, 1.0F, r, g, b, a);
        vertex(c, pose, -xLength, 0.0F, -zLength, 0.0F, 1.0F, r, g, b, a);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int r, int g, int b, int a)
    {
        c.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
