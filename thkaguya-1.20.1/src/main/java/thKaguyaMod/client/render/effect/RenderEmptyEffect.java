package thKaguyaMod.client.render.effect;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import thKaguyaMod.THKaguyaCore;

/**
 * effect 实体占位渲染器（阶段 3d-7）
 * 赤石/冰柜/结界/反射盾/五芒星的完整渲染留阶段 4-6（RenderAjaRedStoneEffect 等）。
 * 本类只保证实体生成不崩（1.20.1 实体必须有渲染器）。
 */
public class RenderEmptyEffect<T extends Entity> extends EntityRenderer<T>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/misc/empty.png");

    public RenderEmptyEffect(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return TEXTURE;
    }
}
