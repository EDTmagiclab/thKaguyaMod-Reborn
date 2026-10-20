package thKaguyaMod.client.render.living;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelSunFlowerFairy;
import thKaguyaMod.entity.living.EntitySunFlowerFairy;

/**
 * 向日葵妖精渲染器（1.20.1 移植版）
 * 原版 RenderSunFlowerFairy：固定 FairyTexture_10.png（128×64），配 ModelSunFlowerFairy。
 */
public class RenderSunFlowerFairy extends MobRenderer<EntitySunFlowerFairy, ModelSunFlowerFairy<EntitySunFlowerFairy>>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/mob/fairytexture_10.png");

    public RenderSunFlowerFairy(EntityRendererProvider.Context context, ModelSunFlowerFairy<EntitySunFlowerFairy> model)
    {
        super(context, model, 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySunFlowerFairy entity)
    {
        return TEXTURE;
    }
}
