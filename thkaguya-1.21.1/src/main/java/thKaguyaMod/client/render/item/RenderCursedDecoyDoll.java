package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thKaguyaMod.entity.item.EntityCursedDecoyDoll;

/**
 * 呪いのデコイ人形の描画（阶段 4-6：ModelCursedDecoyDoll 3D 模型替换 billboard）。
 * 原版 RenderCursedDecoyDoll extends RenderLiving，仅渲染模型（无附加变换）。
 */
public class RenderCursedDecoyDoll extends RenderModelItem<EntityCursedDecoyDoll>
{
    public RenderCursedDecoyDoll(EntityRendererProvider.Context context, EntityModel<EntityCursedDecoyDoll> model)
    {
        super(context, model, "textures/entity/decoy_texture.png");
    }

    @Override
    protected void applyTransforms(EntityCursedDecoyDoll doll, float yaw, float partialTick, PoseStack poseStack)
    {
        // 模型原点已含 setRotationPoint，无需额外变换
    }
}
