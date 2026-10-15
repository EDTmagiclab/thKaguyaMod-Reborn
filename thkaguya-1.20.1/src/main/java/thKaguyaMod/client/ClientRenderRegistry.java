package thKaguyaMod.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelCirno;
import thKaguyaMod.client.model.living.ModelMiko;
import thKaguyaMod.client.model.living.ModelReimu;
import thKaguyaMod.client.model.living.ModelRumia;
import thKaguyaMod.client.model.living.ModelSakuya;
import thKaguyaMod.client.model.living.ModelSanae;
import thKaguyaMod.client.model.living.ModelTHFairy;
import thKaguyaMod.client.model.living.ModelToziko;
import thKaguyaMod.client.model.living.ModelWriggle;
import thKaguyaMod.client.render.effect.RenderSpellCardCircle;
import thKaguyaMod.client.render.item.RenderItemBillboard;
import thKaguyaMod.client.render.item.RenderSukima;
import thKaguyaMod.client.render.item.RenderSilverKnife;
import thKaguyaMod.client.render.living.RenderTHBoss;
import thKaguyaMod.client.render.living.RenderTHFairy;
import thKaguyaMod.client.render.shot.RenderTHLaser;
import thKaguyaMod.client.render.shot.RenderTHShot;
import thKaguyaMod.client.render.spellcard.RenderSpellCard;
import thKaguyaMod.entity.living.EntityCirno;
import thKaguyaMod.entity.living.EntityMiko;
import thKaguyaMod.entity.living.EntityReimu;
import thKaguyaMod.entity.living.EntityRumia;
import thKaguyaMod.entity.living.EntitySakuya;
import thKaguyaMod.entity.living.EntitySanae;
import thKaguyaMod.entity.living.EntityToziko;
import thKaguyaMod.entity.living.EntityWriggle;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * 客户端渲染注册（1.20.1 移植版，阶段 4 止血 + 3d-4 可见性修复）
 * 1.20.1 要求实体必须有 EntityRenderer，否则生成即崩溃（NPE）。
 *
 * item 实体：银刀/隙间/扫帚用专属渲染器（用户实测重点），其余用物品贴图
 * billboard 兜底（对应原版 RenderTHItem 模式）。阶段 4-6 逐个换成真实模型。
 */
@Mod.EventBusSubscriber(modid = THKaguyaCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRenderRegistry
{
    /* boss 模型 layer 位置（阶段 3d-6） */
    private static final ModelLayerLocation CIRNO_LAYER   = layer("cirno");
    private static final ModelLayerLocation MIKO_LAYER    = layer("miko");
    private static final ModelLayerLocation REIMU_LAYER   = layer("reimu");
    private static final ModelLayerLocation RUMIA_LAYER   = layer("rumia");
    private static final ModelLayerLocation SAKUYA_LAYER  = layer("sakuya");
    private static final ModelLayerLocation SANAE_LAYER   = layer("sanae");
    private static final ModelLayerLocation TOZIKO_LAYER  = layer("toziko");
    private static final ModelLayerLocation WRIGGLE_LAYER = layer("wriggle");

    private static ModelLayerLocation layer(String name)
    {
        return new ModelLayerLocation(new ResourceLocation(THKaguyaCore.MODID, name), "main");
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(ModelTHFairy.LAYER_LOCATION, ModelTHFairy::createBodyLayer);
        event.registerLayerDefinition(CIRNO_LAYER, ModelCirno::createBodyLayer);
        event.registerLayerDefinition(MIKO_LAYER, ModelMiko::createBodyLayer);
        event.registerLayerDefinition(REIMU_LAYER, ModelReimu::createBodyLayer);
        event.registerLayerDefinition(RUMIA_LAYER, ModelRumia::createBodyLayer);
        event.registerLayerDefinition(SAKUYA_LAYER, ModelSakuya::createBodyLayer);
        event.registerLayerDefinition(SANAE_LAYER, ModelSanae::createBodyLayer);
        event.registerLayerDefinition(TOZIKO_LAYER, ModelToziko::createBodyLayer);
        event.registerLayerDefinition(WRIGGLE_LAYER, ModelWriggle::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(THKaguyaEntities.TH_FAIRY.get(), RenderTHFairy::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_SHOT.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_LASER.get(), RenderTHLaser::new);
        event.registerEntityRenderer(THKaguyaEntities.SPELL_CARD_CIRCLE.get(), RenderSpellCardCircle::new);
        event.registerEntityRenderer(THKaguyaEntities.SPELL_CARD.get(), RenderSpellCard::new);

        // living boss（阶段 3d-6）：用原版模型 + 贴图，替换妖精占位
        event.registerEntityRenderer(THKaguyaEntities.CIRNO.get(),
                ctx -> new RenderTHBoss<EntityCirno>(ctx, new ModelCirno<>(ctx.bakeLayer(CIRNO_LAYER)), "textures/mob/cirnotexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.DANMAKU_CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(THKaguyaEntities.FAMILIAR.get(), RenderTHFairy::new);
        event.registerEntityRenderer(THKaguyaEntities.MIKO.get(),
                ctx -> new RenderTHBoss<EntityMiko>(ctx, new ModelMiko<>(ctx.bakeLayer(MIKO_LAYER)), "textures/mob/mikotexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.REIMU.get(),
                ctx -> new RenderTHBoss<EntityReimu>(ctx, new ModelReimu<>(ctx.bakeLayer(REIMU_LAYER)), "textures/mob/reimutexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.RUMIA.get(),
                ctx -> new RenderTHBoss<EntityRumia>(ctx, new ModelRumia<>(ctx.bakeLayer(RUMIA_LAYER)), "textures/mob/rumiatexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.SAKUYA.get(),
                ctx -> new RenderTHBoss<EntitySakuya>(ctx, new ModelSakuya<>(ctx.bakeLayer(SAKUYA_LAYER)), "textures/mob/sakuyatexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.SANAE.get(),
                ctx -> new RenderTHBoss<EntitySanae>(ctx, new ModelSanae<>(ctx.bakeLayer(SANAE_LAYER)), "textures/mob/sanaetexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.SUNFLOWER_FAIRY.get(), RenderTHFairy::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_PHANTOM.get(), RenderTHFairy::new);
        event.registerEntityRenderer(THKaguyaEntities.TOZIKO.get(),
                ctx -> new RenderTHBoss<EntityToziko>(ctx, new ModelToziko<>(ctx.bakeLayer(TOZIKO_LAYER)), "textures/mob/tozikotexture.png", 0.25F));
        event.registerEntityRenderer(THKaguyaEntities.WRIGGLE.get(),
                ctx -> new RenderTHBoss<EntityWriggle>(ctx, new ModelWriggle<>(ctx.bakeLayer(WRIGGLE_LAYER)), "textures/mob/wriggletexture.png", 0.25F));

        // 特殊弹体（阶段 3d-3）：继承 THShot 用 RenderTHShot，继承 THLaser 用 RenderTHLaser
        event.registerEntityRenderer(THKaguyaEntities.DRAGON_NECK_JEWEL.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.HOMING_AMULET.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.MASTER_SPARK.get(), RenderTHLaser::new);
        event.registerEntityRenderer(THKaguyaEntities.MUSOU_FUUIN.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.NUCLEAR_SHOT.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.ONMYOUDAMA.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.SANAE_WIND.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_HENYORI_LASER.get(), RenderTHLaser::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_SET_LASER.get(), RenderTHLaser::new);

        // item 实体（阶段 3d-4）：专属渲染器 + billboard 兜底
        // billboard 必须用 textures/item/ 的物品图标（完整单品图）；
        // textures/entity/ 的 *_texture.png 是盒子模型 UV 展开图，铺平会显示碎片（实测踩坑）
        event.registerEntityRenderer(THKaguyaEntities.SILVER_KNIFE.get(), RenderSilverKnife::new);
        event.registerEntityRenderer(THKaguyaEntities.SUKIMA.get(), RenderSukima::new);
        // 扫帚回退 billboard：magic_broom.png 是 32x32 对角布局物品图标，
        // RenderMarisaBroom 的手绘 3D quad 无法准确映射该布局（实测大量透明碎片）。
        // 完整 3D 几何留到阶段 4-6 用真实模型还原（RenderMarisaBroom 保留备用）。
        event.registerEntityRenderer(THKaguyaEntities.MARISA_BROOM.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/magic_broom.png", 0.4F));
        event.registerEntityRenderer(THKaguyaEntities.CURSED_DECOY_DOLL.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/cursed_decoy_doll.png", 0.4F));
        event.registerEntityRenderer(THKaguyaEntities.HISOU.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/hisou_sword.png", 0.5F));
        event.registerEntityRenderer(THKaguyaEntities.KINKAKUZI.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/kinkakuji.png", 0.4F));
        event.registerEntityRenderer(THKaguyaEntities.MAZINKYOUKAN.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/wall_passing_chisel.png", 0.4F));
        event.registerEntityRenderer(THKaguyaEntities.MINI_HAKKERO.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/mini_hakkero.png", 0.35F));
        event.registerEntityRenderer(THKaguyaEntities.PENDULUM.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/nazrin_pendulum.png", 0.35F));
        event.registerEntityRenderer(THKaguyaEntities.SAKUYA_STOP_WATCH.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/sakuya_stopwatch.png", 0.35F));
        event.registerEntityRenderer(THKaguyaEntities.SAKUYA_WATCH.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/sakuya_watch.png", 0.4F));
        event.registerEntityRenderer(THKaguyaEntities.SPIRITUAL_STRIKE_TALISMAN.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/spiritual_strike_talisman.png", 0.4F));
        event.registerEntityRenderer(THKaguyaEntities.YUUKA_PARASOL.get(),
                ctx -> new RenderItemBillboard<>(ctx, "textures/item/yuuka_parasol.png", 0.4F));
    }
}
