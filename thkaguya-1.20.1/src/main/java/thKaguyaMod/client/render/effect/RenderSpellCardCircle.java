package thKaguyaMod.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;

/**
 * 魔法阵渲染器（阶段 3d-2 占位，阶段 4 补完整 RenderSpellCardCircle）
 */
public class RenderSpellCardCircle extends EntityRenderer<EntitySpellCardCircle>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/shot/shot.png");

    public RenderSpellCardCircle(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySpellCardCircle entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntitySpellCardCircle entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        // 占位：阶段 4 补完整魔法阵渲染
    }
}
