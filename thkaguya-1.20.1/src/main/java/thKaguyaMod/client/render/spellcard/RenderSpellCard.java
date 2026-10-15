package thKaguyaMod.client.render.spellcard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.registry.SpellCardRegistry;

/**
 * 符卡实体渲染器（1.20.1 移植版，阶段 4-5 完整化）
 *
 * 原版 RenderSpellCard：渲染一张竖持的符卡卡面 + 等级边框两层 billboard。
 *   - 卡面贴图 = textures/items/spellcard/{name}.png（name 来自 SpellCardRegistry）
 *   - 边框贴图 = textures/items/spellcard/spellcard_lv{level}.png
 *   - 绕 Y 轴自旋 21°/tick + 绕 Z 轴倾斜 30°（固定朝向，非面向相机）
 *   - UV 子图 14/128 ~ 114/128（128 宽贴图，左右各留 14px 边距）
 *
 * 1.20.1 迁移要点：
 *   - 原版 bindEntityTexture + 双层 Tessellator quad → entityCutoutNoCull 双面渲染
 *   - 原版 getLevel() 反射实例化 THSpellCard 取 getNeedLevel() → 直接用 entity.level
 *     （EntitySpellCard 构造已存 spLevel，玩家使用时固定 level=2）
 *   - 贴图文件名全小写化 + 去单引号（原版 Kappa'sPororoca.png 含非法字符）
 */
public class RenderSpellCard extends EntityRenderer<EntitySpellCard>
{
    public RenderSpellCard(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySpellCard entity)
    {
        return cardTexture(entity);
    }

    @Override
    public void render(EntitySpellCard entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        ResourceLocation cardTex = cardTexture(entity);
        if(cardTex == null)
        {
            return;
        }

        // 等级边框（原版 getLevel() 反射实例化取 needLevel，这里直接用实体 level 字段）
        int level = Math.max(1, Math.min(5, entity.level));
        ResourceLocation frameTex = new ResourceLocation(THKaguyaCore.MODID,
                "textures/items/spellcard/spellcard_lv" + level + ".png");

        poseStack.pushPose();
        // 原版：绕 Y 自旋 21°/tick + 绕 Z 倾斜 30°
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 21F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(30F));

        // 原版 UV：rvl=14/128, rul=0, rvr=114/128, rur=1.0
        float u0 = 14.0F / 128.0F;
        float u1 = 114.0F / 128.0F;
        float v0 = 0.0F;
        float v1 = 1.0F;
        float hw = 0.1F;   // 原版 ±0.2
        float hh = 0.15F;  // 原版 ±0.3

        // 卡面（z=0）+ 边框（z=0.002，避免 z-fighting）
        renderBillboard(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(cardTex)),
                packedLight, hw, hh, u0, u1, v0, v1, 0.0F);
        renderBillboard(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(frameTex)),
                packedLight, hw, hh, u0, u1, v0, v1, 0.002F);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** 符卡卡面贴图；未注册的符卡返回 null（跳过渲染） */
    private ResourceLocation cardTexture(EntitySpellCard entity)
    {
        String name = SpellCardRegistry.getSpellCardName(entity.getSpellCardNumber());
        if(name == null)
        {
            return null;
        }
        // 贴图文件名 = 注册名全小写 + 去单引号（Kappa'sPororoca -> kappaspororoca）
        return new ResourceLocation(THKaguyaCore.MODID,
                "textures/items/spellcard/" + name.toLowerCase().replace("'", "") + ".png");
    }

    private void renderBillboard(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                 float hw, float hh, float u0, float u1, float v0, float v1, float z)
    {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        consumer.vertex(matrix,  hw,  hh, z).color(255, 255, 255, 255)
                .uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -hw,  hh, z).color(255, 255, 255, 255)
                .uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -hw, -hh, z).color(255, 255, 255, 255)
                .uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix,  hw, -hh, z).color(255, 255, 255, 255)
                .uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
