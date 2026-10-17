package thKaguyaMod.client.render.living;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelRinnosuke;
import thKaguyaMod.entity.living.EntityRinnosuke;

/**
 * 森近霖之助の描画（1.20.1 移植版）
 * 原版 extract/sources/java/thKaguyaMod/client/render/living/RenderRinnosuke.java（RenderLiving）→ 1.20.1 MobRenderer。
 * ・モデル ModelRinnosuke（64x64）、影の半径 0.25F（原版 super(new ModelRinnosuke(), 0.25F)）
 * ・貼り付け先 thkaguya:textures/entity/mob/rinnosuke_texture.png
 *   （原版は thkaguyamod:textures/mob/RinnosukeTexture.png）
 */
public class RenderRinnosuke extends MobRenderer<EntityRinnosuke, ModelRinnosuke>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/entity/mob/rinnosuke_texture.png");

    public RenderRinnosuke(EntityRendererProvider.Context context)
    {
        super(context, new ModelRinnosuke(context.bakeLayer(ModelRinnosuke.LAYER)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRinnosuke entity)
    {
        return TEXTURE;
    }
}
