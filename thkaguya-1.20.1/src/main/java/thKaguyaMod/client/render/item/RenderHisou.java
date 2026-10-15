package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import thKaguyaMod.entity.item.EntityHisou;

/**
 * 緋想の剣の描画（阶段 4-6：ModelHisou 3D 模型替换 billboard）。
 * 原版：rotate(-yaw,Y) + rotate(+pitch,X) + rotate(getAngle(),Z) + scale(0.3,0.7,0.3)，
 * 半透明 alpha = getNum()*0.125（加色混合退化，用 entityTranslucentCull 近似）。
 */
public class RenderHisou extends RenderModelItem<EntityHisou>
{
    public RenderHisou(EntityRendererProvider.Context context, EntityModel<EntityHisou> model)
    {
        super(context, model, "textures/entity/hisou_texture.png");
    }

    @Override
    protected void applyTransforms(EntityHisou hisou, float yaw, float partialTick, PoseStack poseStack)
    {
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(hisou.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(hisou.getAngle()));
        poseStack.scale(0.3F, 0.7F, 0.3F);
    }

    @Override
    public void render(EntityHisou hisou, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        applyTransforms(hisou, yaw, partialTick, poseStack);
        float alpha = Math.max(0.0F, Math.min(1.0F, (float)hisou.getNum() * 0.125F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, alpha);
        poseStack.popPose();
        super.render(hisou, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
