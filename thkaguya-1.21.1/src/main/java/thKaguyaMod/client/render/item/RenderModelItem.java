package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import thKaguyaMod.THKaguyaCore;

/**
 * item 实体通用 3D 模型渲染器（阶段 4-6：替换 billboard 兜底）。
 *
 * 对应原版 extends Render + modelXxx.render(...) 的模式：持有 LayerDefinition 烘焙出的
 * EntityModel + 贴图，渲染时先由子类 applyTransforms 施加平移/旋转/缩放，再画模型。
 * 默认走 entityCutoutNoCull（不透明双面，适配原版 disable(GL_CULL_FACE) 的薄板模型）。
 */
public class RenderModelItem<T extends Entity> extends EntityRenderer<T>
{
    protected final EntityModel<T> model;
    protected final ResourceLocation texture;

    public RenderModelItem(EntityRendererProvider.Context context, EntityModel<T> model, String texturePath)
    {
        super(context);
        this.model = model;
        this.texture = ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, texturePath);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return texture;
    }

    /** 子类覆写：在渲染模型前施加平移/旋转/缩放（对应原版 doRender 里的 GL 变换）。 */
    protected void applyTransforms(T entity, float yaw, float partialTick, PoseStack poseStack)
    {
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        applyTransforms(entity, yaw, partialTick, poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, net.minecraft.util.FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F));
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
