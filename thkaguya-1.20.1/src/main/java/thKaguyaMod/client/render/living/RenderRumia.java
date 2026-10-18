package thKaguyaMod.client.render.living;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thKaguyaMod.client.model.living.ModelRumia;
import thKaguyaMod.client.render.DarkSphereRenderer;
import thKaguyaMod.entity.living.EntityRumia;

/**
 * ルーミアの描画（1.20.1 移植版）。
 *
 * <p>原版 {@code RenderRumia} はモデルを描いたあと、体の中心（+1.3）に
 * 半径 6 の「闇球」（{@code textures/DarkTexture.png}、GL_ZERO / GL_ONE_MINUS_SRC_COLOR）を
 * ビルボードで重ねていた。移植版は {@link RenderTHBoss} をそのまま使っていて
 * この闇が完全に欠落していたため、専用レンダラを用意する。
 */
public class RenderRumia extends RenderTHBoss<EntityRumia>
{
    public RenderRumia(EntityRendererProvider.Context context, net.minecraft.client.model.geom.ModelLayerLocation layer)
    {
        super(context, new ModelRumia<>(context.bakeLayer(layer)), "textures/mob/rumiatexture.png", 0.25F);
    }

    @Override
    public void render(EntityRumia entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        // 原版：GL11.glTranslatef(0, 1.3, 0) + ビルボード（-playerViewY / playerViewX）
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.3D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        DarkSphereRenderer.render(poseStack, 12.0F, 1.0F, DarkSphereRenderer.Blend.DARKEN);
        poseStack.popPose();
    }
}
