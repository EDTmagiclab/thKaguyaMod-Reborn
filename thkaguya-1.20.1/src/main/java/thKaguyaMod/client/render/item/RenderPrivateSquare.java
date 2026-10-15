package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thKaguyaMod.entity.item.EntitySakuyaWatch;

/**
 * 咲夜の懐中時計の描画（阶段 4-6：ModelPrivateSquare 3D 模型替换 billboard）。
 * 原版：scale(0.3) + rotate(180-yaw+ticksExisted*7,Y) + model（时停暗幕略，阶段 6 配置接线时补）。
 */
public class RenderPrivateSquare extends RenderModelItem<EntitySakuyaWatch>
{
    public RenderPrivateSquare(EntityRendererProvider.Context context, EntityModel<EntitySakuyaWatch> model)
    {
        super(context, model, "textures/entity/sakuya_watch_texture.png");
    }

    @Override
    protected void applyTransforms(EntitySakuyaWatch watch, float yaw, float partialTick, PoseStack poseStack)
    {
        poseStack.scale(0.3F, 0.3F, 0.3F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw + watch.tickCount * 7.0F));
    }
}
