package thKaguyaMod.client.render.shot;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.shot.EntityDragonNeckJewel;

/**
 * 龍の首の玉の描画（原版 RenderRyuuLightningBolt の移植）。
 *
 * <p>原版は thKaguyaTerrain.png の 16px セルを billboard で貼る：
 * 既定セルは {@code 1 + 16 = 17}（黄）、1/3 の確率で {@code 16..20} のどれか。
 * quad は (0-0.5, 0-0.25) から 1x1、常にプレイヤーの方を向く。</p>
 */
public class RenderDragonNeckJewel extends EntityRenderer<EntityDragonNeckJewel>
{
    /** 原版 RenderRyuuLightningBolt#dragonBulletTexture */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/entity/thkaguya_terrain.png");

    /** 原版 private Random random = new Random()：3 回に 1 回だけ色が変わる */
    private final Random random = new Random();

    public RenderDragonNeckJewel(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDragonNeckJewel entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityDragonNeckJewel entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // 常にプレイヤーの方を向ける（原版 THKaguyaRenderLib#getGLRotatefByTherdPersonView）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        int color = 1 + 16;
        if(this.random.nextInt(3) == 0)
        {
            color = this.random.nextInt(5) + 16;
        }

        float u0 = (float)((color % 16) * 16) / 256F;
        float u1 = (float)((color % 16) * 16 + 16) / 256F;
        float v0 = (float)((color / 16) * 16) / 256F;
        float v1 = (float)((color / 16) * 16 + 16) / 256F;

        float x0 = 0.0F - 0.5F;
        float x1 = 1.0F - 0.5F;
        float y0 = 0.0F - 0.25F;
        float y1 = 1.0F - 0.25F;

        Matrix4f matrix = poseStack.last().pose();        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(TEXTURE));

        consumer.addVertex(matrix, x0, y0, 0.0F).setColor(255, 255, 255, 255).setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x1, y0, 0.0F).setColor(255, 255, 255, 255).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x1, y1, 0.0F).setColor(255, 255, 255, 255).setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, x0, y1, 0.0F).setColor(255, 255, 255, 255).setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
