package thKaguyaMod.client.render.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.entity.living.EntityDanmakuMob;

/**
 * 弹幕 boss 通用渲染器（1.20.1 移植版，阶段 3d-6）
 * 每个 boss 用对应模型 + 贴图，替换妖精模型占位。
 *
 * <p>并含原版 {@code RenderTHBoss.renderTHBossStatus()} 的移植：攻击中（
 * {@code getDanmakuPattern() != NOT_ATTACK}）且在视野 ±20° 内时，在头顶绘制
 * 血条（{@code textures/mob/status.png}）、boss 名（绿）与符卡名（白）。
 *
 * <p>1.7.10 → 1.20.1 的对应：
 * <ul>
 *   <li>{@code renderManager.playerViewY/X} → {@code entityRenderDispatcher.camera}（夹角用视线与实体方向的点积求）</li>
 *   <li>{@code danmakuMob.height} → {@code getBbHeight()}</li>
 *   <li>{@code getDistanceToCamera()} → 相机到实体的距离</li>
 *   <li>{@code tessellator.addVertexWithUV} → {@link VertexConsumer} + {@link RenderType#entityTranslucent}</li>
 *   <li>{@code font.drawStringWithShadow} → {@link Font#drawInBatch(String, float, float, int, boolean, Matrix4f, MultiBufferSource, Font.DisplayMode, int, int)}</li>
 * </ul>
 * 原版的 alpha（20°〜50° 渐隐）在 1.7.10 里因顶点色被注释掉而无效，这里同样按硬显/硬隐处理。
 */
public class RenderTHBoss<T extends EntityDanmakuMob> extends MobRenderer<T, EntityModel<T>>
{
    /** 原版 textures/mob/Status.png（已拷到 assets/thkaguya/textures/mob/status.png） */
    private static final ResourceLocation STATUS_TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/mob/status.png");

    /** 视野角：原版在 ±20° 内才显示 */
    private static final float VISIBLE_ANGLE = 20.0F;

    /** 名牌绿 / 符卡名白 */
    private static final int NAME_COLOR = 0x00FF88;
    private static final int SPELL_CARD_COLOR = 0xFFFFFF;

    private final ResourceLocation texture;

    public RenderTHBoss(EntityRendererProvider.Context context, EntityModel<T> model, String texturePath, float shadowRadius)
    {
        super(context, model, shadowRadius);
        this.texture = new ResourceLocation(THKaguyaCore.MODID, texturePath);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return texture;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight)
    {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        renderTHBossStatus(entity, poseStack, buffer, packedLight);
    }

    /** 原版 {@code RenderTHBoss.renderTHBossStatus}（1.7.10 版 41-145 行）。 */
    protected void renderTHBossStatus(T mob, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        // 非攻撃中（NOT_ATTACK）は出さない
        if (mob.getDanmakuPattern() == EntityDanmakuMob.NOT_ATTACK)
        {
            return;
        }

        Camera camera = this.entityRenderDispatcher.camera;
        Vec3 toEntity = mob.position().subtract(camera.getPosition());
        double distance = toEntity.length();
        if (distance < 1.0E-4D)
        {
            return;
        }

        Vector3f look = camera.getLookVector();
        double cos = (look.x() * toEntity.x + look.y() * toEntity.y + look.z() * toEntity.z) / distance;
        float span = (float)Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0D, 1.0D)));
        if (span > VISIBLE_ANGLE)
        {
            return;
        }

        // 遠いほど大きく（原版 size = 1 + distance/64、上限 5）
        float size = Math.min(1.0F + (float)distance / 64.0F, 5.0F);

        int cardNo = mob.getUsingSpellCardNo();
        float hp = mob.getHealth() / mob.getMaxHealth();
        float hpLeft = hp * 2.0F - 1.0F; // 原版 hp2：満タンで -1（＝左端）

        // ---- 血条（1 単位 = 1 ブロック、原版と同じ座標系）----
        poseStack.pushPose();
        poseStack.translate(0.0D, mob.getBbHeight() + 1.5D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-size, -size, size);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(STATUS_TEXTURE));

        // 底板：UV v = 3/16 〜 4/16
        quad(vertices, matrix, -1.02F, 1.02F, -0.02F, 0.07F, 0.0F, 1.0F, 3.0F / 16.0F, 4.0F / 16.0F, packedLight);

        // 体力ゲージ：符卡使用中は v = 8/16 〜 11/16、通常は 0/16 〜 3/16
        float v0 = cardNo >= 0 ? 8.0F / 16.0F : 0.0F;
        float v1 = cardNo >= 0 ? 11.0F / 16.0F : 3.0F / 16.0F;
        quad(vertices, matrix, -hpLeft, 1.0F, 0.0F, 0.05F, hp, 0.0F, v0, v1, packedLight);

        // ---- 名前と符卡名（原版 0.02 倍スケール、x = -50 px から）----
        poseStack.translate(0.0D, 0.0D, 0.002D);
        poseStack.scale(0.02F, 0.02F, 0.02F);
        Matrix4f textMatrix = poseStack.last().pose();
        Font font = this.getFont();

        font.drawInBatch(mob.getDanmakuMobName(), -50.0F, 0.0F, NAME_COLOR, true,
                textMatrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        if (cardNo >= 0)
        {
            font.drawInBatch(THKaguyaLib.spellCardDecarationName(cardNo), -50.0F, -12.0F, SPELL_CARD_COLOR, true,
                    textMatrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }

    /** 上端 (x0,y1) から反時計回りに 1 枚の quad を書く。 */
    private static void quad(VertexConsumer vertices, Matrix4f matrix, float x0, float x1, float y0, float y1,
            float u0, float u1, float v0, float v1, int packedLight)
    {
        vertex(vertices, matrix, x0, y1, u0, v0, packedLight);
        vertex(vertices, matrix, x1, y1, u1, v0, packedLight);
        vertex(vertices, matrix, x1, y0, u1, v1, packedLight);
        vertex(vertices, matrix, x0, y0, u0, v1, packedLight);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float u, float v, int packedLight)
    {
        vertices.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
