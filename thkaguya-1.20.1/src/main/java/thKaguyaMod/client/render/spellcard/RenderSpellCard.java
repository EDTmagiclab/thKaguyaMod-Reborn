package thKaguyaMod.client.render.spellcard;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.spellcard.EntitySpellCard;

/**
 * 符卡实体渲染器（阶段 3d-2 占位，阶段 4 补完整 RenderSpellCard）
 */
public class RenderSpellCard extends EntityRenderer<EntitySpellCard>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/shot.png");

    public RenderSpellCard(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySpellCard entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntitySpellCard entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        // 占位：阶段 4 补完整符卡渲染
    }
}
