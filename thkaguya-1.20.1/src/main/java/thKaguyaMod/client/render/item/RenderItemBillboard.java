package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import thKaguyaMod.THKaguyaCore;

/**
 * 汎用アイテム Billboard レンダラー（1.20.1 移植版，阶段 3d-4 修复可见性）
 *
 * 对应原版 RenderTHItem 模式：用物品 2D 贴图画面向相机的悬浮四边形。
 * 13 个 item 实体的可见性兜底；银刀/隙间/扫帚有专属渲染器覆盖。
 * 阶段 4-6 再为各实体补真实模型。
 */
public class RenderItemBillboard<T extends Entity> extends EntityRenderer<T>
{
    /** 实体类 -> 物品贴图路径（textures/item/ 下） */
    private final ResourceLocation texture;

    /** 贴图缩放（原版尺寸 0.4 方块 wide，四边形 half=0.4） */
    private final float scale;

    public RenderItemBillboard(EntityRendererProvider.Context context, String itemTexturePath, float scale)
    {
        super(context);
        this.texture = new ResourceLocation(THKaguyaCore.MODID, itemTexturePath);
        this.scale = scale;
    }

    public RenderItemBillboard(EntityRendererProvider.Context context, String itemTexturePath)
    {
        this(context, itemTexturePath, 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return texture;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 面向相机（原版 renderTHItem 调 THKaguyaRenderLib.getGLRotatefByTherdPersonView 同效）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        // 轻微悬浮上下漂动（原版 sin(ticksExisted/20)*0.1）
        float size = scale;

        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        float half = size;

        consumer.vertex(matrix,  half,  half, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half,  half, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -half, -half, 0.0F).color(255, 255, 255, 255)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix,  half, -half, 0.0F).color(255, 255, 255, 255)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
