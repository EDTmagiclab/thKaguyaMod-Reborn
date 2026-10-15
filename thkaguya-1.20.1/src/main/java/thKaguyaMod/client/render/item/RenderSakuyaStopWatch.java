package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thKaguyaMod.entity.item.EntitySakuyaStopWatch;

/**
 * ストップウォッチの描画（阶段 4-6：ModelPrivateSquare 3D 模型替换 billboard）。
 * 原版：scale(0.3) + rotate(180-yaw+ticksExisted*7,Y) + model。
 */
public class RenderSakuyaStopWatch extends RenderModelItem<EntitySakuyaStopWatch>
{
    public RenderSakuyaStopWatch(EntityRendererProvider.Context context, EntityModel<EntitySakuyaStopWatch> model)
    {
        super(context, model, "textures/entity/sakuya_stop_watch_texture.png");
    }

    @Override
    protected void applyTransforms(EntitySakuyaStopWatch watch, float yaw, float partialTick, PoseStack poseStack)
    {
        poseStack.scale(0.3F, 0.3F, 0.3F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw + watch.tickCount * 7.0F));
    }
}
