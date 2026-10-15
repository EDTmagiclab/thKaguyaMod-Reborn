package thKaguyaMod.client.render.living;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelTHFairy;
import thKaguyaMod.entity.living.EntityTHFairy;

/**
 * 妖精渲染器（1.20.1 移植版）
 * 原版 RenderTHFairy（RenderLiving）-> 1.20.1 MobRenderer。
 */
public class RenderTHFairy extends MobRenderer<EntityTHFairy, ModelTHFairy<EntityTHFairy>>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/mob/fairytexture_0.png");

    public RenderTHFairy(EntityRendererProvider.Context context)
    {
        super(context, new ModelTHFairy<>(context.bakeLayer(ModelTHFairy.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHFairy entity)
    {
        return TEXTURE;
    }
}
