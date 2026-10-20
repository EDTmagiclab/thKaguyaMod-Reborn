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
 *
 * 贴图随形态切换（原版逻辑）：
 *   getForm() >= 0 → FairyTexture_{danmakuPattern % 3}.png（3 种身体）
 *   getForm() < 0  → ZombieFairyTexture.png（僵尸妖精）
 */
public class RenderTHFairy extends MobRenderer<EntityTHFairy, ModelTHFairy<EntityTHFairy>>
{
    private static final ResourceLocation[] FAIRY_TEXTURES =
    {
        ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/mob/fairytexture_0.png"),
        ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/mob/fairytexture_1.png"),
        ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/mob/fairytexture_2.png")
    };

    private static final ResourceLocation ZOMBIE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/mob/zombiefairytexture.png");

    public RenderTHFairy(EntityRendererProvider.Context context)
    {
        super(context, new ModelTHFairy<>(context.bakeLayer(ModelTHFairy.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTHFairy entity)
    {
        if(entity.getForm() >= 0)
        {
            return FAIRY_TEXTURES[Math.floorMod(entity.getDanmakuPattern(), 3)];
        }
        return ZOMBIE_TEXTURE;
    }
}
