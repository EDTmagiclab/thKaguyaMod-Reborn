package thKaguyaMod.client.render.living;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.living.EntityDanmakuMob;

/**
 * 弹幕 boss 通用渲染器（1.20.1 移植版，阶段 3d-6）
 * 每个 boss 用对应模型 + 贴图，替换妖精模型占位。
 * 血条/符卡名渲染（原版 RenderTHBoss.renderTHBossStatus）留阶段 4-6 补。
 */
public class RenderTHBoss<T extends EntityDanmakuMob> extends MobRenderer<T, EntityModel<T>>
{
    private final ResourceLocation texture;

    public RenderTHBoss(EntityRendererProvider.Context context, EntityModel<T> model, String texturePath, float shadowRadius)
    {
        super(context, model, shadowRadius);
        this.texture = new ResourceLocation(THKaguyaCore.MODID, texturePath);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return texture;
    }
}
