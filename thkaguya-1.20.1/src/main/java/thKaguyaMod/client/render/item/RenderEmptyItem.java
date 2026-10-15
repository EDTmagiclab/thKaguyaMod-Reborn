package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * item 实体的占位渲染器（1.20.1 移植版，阶段 3d-4）
 *
 * 阶段 4-6 将为各 item 实体补真实模型/贴图（RenderSilverKnife / RenderMiniHakkero 等）。
 * 此处仅注册空渲染器，确保实体生成不因缺渲染器而崩溃（客户端）。
 */
public class RenderEmptyItem<T extends Entity> extends EntityRenderer<T>
{
    public RenderEmptyItem(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light)
    {
        // 占位：阶段 4-6 实现真实渲染
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return null;
    }
}
