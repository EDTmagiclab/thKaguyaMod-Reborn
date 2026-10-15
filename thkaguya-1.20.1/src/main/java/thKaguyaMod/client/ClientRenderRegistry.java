package thKaguyaMod.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelTHFairy;
import thKaguyaMod.client.render.effect.RenderSpellCardCircle;
import thKaguyaMod.client.render.item.RenderItemBillboard;
import thKaguyaMod.client.render.item.RenderMarisaBroom;
import thKaguyaMod.client.render.item.RenderSukima;
import thKaguyaMod.client.render.item.RenderSilverKnife;
import thKaguyaMod.client.render.living.RenderTHFairy;
import thKaguyaMod.client.render.shot.RenderTHLaser;
import thKaguyaMod.client.render.shot.RenderTHShot;
import thKaguyaMod.client.render.spellcard.RenderSpellCard;
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
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(ModelTHFairy.LAYER_LOCATION, ModelTHFairy::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(THKaguyaEntities.TH_FAIRY.get(), RenderTHFairy::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_SHOT.get(), RenderTHShot::new);
        event.registerEntityRenderer(THKaguyaEntities.TH_LASER.get(), RenderTHLaser::new);
        event.registerEntityRenderer(THKaguyaEntities.SPELL_CARD_CIRCLE.get(), RenderSpellCardCircle::new);
        event.registerEntityRenderer(THKaguyaEntities.SPELL_CARD.get(), RenderSpellCard::new);

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
        event.registerEntityRenderer(THKaguyaEntities.MARISA_BROOM.get(), RenderMarisaBroom::new);
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
