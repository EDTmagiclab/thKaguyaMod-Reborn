package thKaguyaMod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntityMarisaBroom;

/**
 * 魔理沙の箒の描画（1.20.1 移植版，阶段 3d-4 可见性修复）
 *
 * 原版 RenderMarisaBroom 用 Tessellator 手绘 4 面旋转长杆 + 刷毛（贴图 MagicBloom.png）。
 * 这里还原同样几何：杆身 4 面长条 + 刷毛 3 段锥形扩散 ×4 方向，保留受击摇摆动画。
 * 贴图改用 textures/item/magic_broom.png（64x32 原版 MagicBloom 是模型展开图，
 * UV 坐标仍按其 0..1 布局取样，杆/刷毛区域近似对应）。
 * 实测坑：quad() 若缺 .color() 会崩（Not filled all elements of the vertex）。
 */
public class RenderMarisaBroom extends EntityRenderer<EntityMarisaBroom>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/item/magic_broom.png");

    public RenderMarisaBroom(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMarisaBroom entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EntityMarisaBroom broom, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        // 原版受击摇摆动画：sin(timeSinceHit) * timeSinceHit * damage / 10 * forwardDirection
        float timeSinceHit = (float)broom.getTimeSinceHit() - partialTick;
        float damageTaken = broom.getDamageTaken() - partialTick;
        if(damageTaken < 0.0F)
        {
            damageTaken = 0.0F;
        }
        if(timeSinceHit > 0.0F)
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    Mth.sin(timeSinceHit) * timeSinceHit * damageTaken / 10.0F
                            * (float)broom.getForwardDirection()));
        }

        // 原版：scale(0.75) 后 scale(1/0.75) 相互抵消，只有翻转 (-1,-1,1) 生效
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-broom.getXRot()));

        float rodWidth  = 0.05F;
        float rodWidth2 = 0.3F;
        float rodWidth3 = 0.4F;
        float rodWidth4 = 0.15F;
        double rodLength = 2.8D;
        double rodLengthHalf = rodLength / 2.0D;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // 杆身：绕 Z 轴旋转 4 次画 4 面
        for(int i = 0; i < 4; i++)
        {
            quad(consumer, poseStack,
                    rodWidth, -rodWidth,  (float)rodLengthHalf, 0.0F, 2.0F / 64.0F,
                    rodWidth, -rodWidth, -(float)rodLengthHalf, 0.5F, 2.0F / 64.0F,
                    rodWidth,  rodWidth, -(float)rodLengthHalf, 0.5F, 0.0F,
                    rodWidth,  rodWidth,  (float)rodLengthHalf, 0.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        // 杆身前端封口
        quad(consumer, poseStack,
                -rodWidth, -rodWidth,  (float)rodLengthHalf, 0.0F, 2.0F / 64.0F,
                 rodWidth, -rodWidth,  (float)rodLengthHalf, 0.5F, 2.0F / 64.0F,
                 rodWidth,  rodWidth,  (float)rodLengthHalf, 0.5F, 4.0F / 64.0F,
                -rodWidth,  rodWidth,  (float)rodLengthHalf, 0.0F, 4.0F / 64.0F);

        // 刷毛颈部
        quad(consumer, poseStack,
                -rodWidth4, -rodWidth4, -(float)rodLengthHalf + 0.3F, 0.25F, 0.5F,
                 rodWidth4, -rodWidth4, -(float)rodLengthHalf + 0.3F, 0.50F, 0.5F,
                 rodWidth4,  rodWidth4, -(float)rodLengthHalf + 0.3F, 0.50F, 1.0F,
                -rodWidth4,  rodWidth4, -(float)rodLengthHalf + 0.3F, 0.25F, 1.0F);

        // 刷毛中部封口
        quad(consumer, poseStack,
                -rodWidth3, -rodWidth3, -(float)rodLengthHalf - 1.3F, 0.25F, 0.5F,
                -rodWidth3,  rodWidth3, -(float)rodLengthHalf - 1.3F, 0.25F, 1.0F,
                 rodWidth3,  rodWidth3, -(float)rodLengthHalf - 1.3F, 0.50F, 1.0F,
                 rodWidth3, -rodWidth3, -(float)rodLengthHalf - 1.3F, 0.50F, 0.5F);

        // 刷毛：3 段锥形扩散，绕 Z 轴旋转 4 次
        for(int i = 0; i < 4; i++)
        {
            quad(consumer, poseStack,
                    rodWidth4, -rodWidth4, -(float)rodLengthHalf + 0.3F, 0.5F, 0.5F,
                    rodWidth,  -rodWidth,  -(float)rodLengthHalf,        0.5F, 3.0F / 64.0F,
                    rodWidth,   rodWidth,  -(float)rodLengthHalf,        1.0F, 3.0F / 64.0F,
                    rodWidth4,  rodWidth4, -(float)rodLengthHalf + 0.3F, 1.0F, 0.5F);

            quad(consumer, poseStack,
                    rodWidth,   -rodWidth,  -(float)rodLengthHalf,        0.5F, 0.5F,
                    rodWidth2,  -rodWidth2, -(float)rodLengthHalf - 0.6F, 0.5F, 3.0F / 64.0F,
                    rodWidth2,   rodWidth2, -(float)rodLengthHalf - 0.6F, 1.0F, 3.0F / 64.0F,
                    rodWidth,    rodWidth,  -(float)rodLengthHalf,        1.0F, 0.5F);

            quad(consumer, poseStack,
                    rodWidth2,  -rodWidth2, -(float)rodLengthHalf - 0.6F, 0.5F, 1.0F,
                    rodWidth3,  -rodWidth3, -(float)rodLengthHalf - 1.3F, 0.5F, 16.0F / 64.0F,
                    rodWidth3,   rodWidth3, -(float)rodLengthHalf - 1.3F, 1.0F, 16.0F / 64.0F,
                    rodWidth2,   rodWidth2, -(float)rodLengthHalf - 0.6F, 1.0F, 1.0F);

            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        poseStack.popPose();
        super.render(broom, yaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 原版 Tessellator.addVertexWithUV 四顶点四边形的等价实现。
     *  1.20.1 NEW_ENTITY 格式必须填满 POSITION/COLOR/UV/OVERLAY/UV2/NORMAL 六要素，
     *  缺 color 会抛「Not filled all elements of the vertex」崩溃（实测踩坑）。 */
    private static void quad(VertexConsumer consumer, PoseStack poseStack,
            float ax, float ay, float az, float uA, float vA,
            float bx, float by, float bz, float uB, float vB,
            float cx, float cy, float cz, float uC, float vC,
            float dx, float dy, float dz, float uD, float vD)
    {
        PoseStack.Pose pose = poseStack.last();
        var matrix = pose.pose();
        var normal = pose.normal();
        consumer.vertex(matrix, ax, ay, az).color(255, 255, 255, 255).uv(uA, vA)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(255, 255, 255, 255).uv(uB, vB)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, cx, cy, cz).color(255, 255, 255, 255).uv(uC, vC)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, dx, dy, dz).color(255, 255, 255, 255).uv(uD, vD)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
