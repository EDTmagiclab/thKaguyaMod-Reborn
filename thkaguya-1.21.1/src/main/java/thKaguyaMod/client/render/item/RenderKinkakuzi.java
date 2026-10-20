package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thKaguyaMod.entity.item.EntityKinkakuzi;

/**
 * 金閣寺の一枚天井の描画（阶段 4-6：ModelKinkakuzi 3D 模型替换 billboard）。
 * 原版：scale(3.0,0.75,3.0) + rotate(180-yaw,Y)（受击摇摆略）。
 */
public class RenderKinkakuzi extends RenderModelItem<EntityKinkakuzi>
{
    public RenderKinkakuzi(EntityRendererProvider.Context context, EntityModel<EntityKinkakuzi> model)
    {
        super(context, model, "textures/entity/kinkakuzi_texture.png");
    }

    @Override
    protected void applyTransforms(EntityKinkakuzi kinkakuzi, float yaw, float partialTick, PoseStack poseStack)
    {
        poseStack.scale(3.0F, 0.75F, 3.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }
}
