package thKaguyaMod.client.render.living;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelTHFairy;
import thKaguyaMod.entity.living.EntityDanmakuMob;

/**
 * 弹幕 boss 占位渲染器（1.20.1 移植版，阶段 3d-6）
 * boss（Cirno/Reimu/Miko/Sakuya/Sanae/Rumia/Toziko/Wriggle）暂用妖精模型占位，
 * 阶段 4-6 逐个换成真实模型。
 */
public class RenderDanmakuMob extends MobRenderer<EntityDanmakuMob, ModelTHFairy<EntityDanmakuMob>>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/mob/fairytexture_0.png");

    public RenderDanmakuMob(EntityRendererProvider.Context context)
    {
        super(context, new ModelTHFairy<>(context.bakeLayer(ModelTHFairy.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDanmakuMob entity)
    {
        return TEXTURE;
    }
}
