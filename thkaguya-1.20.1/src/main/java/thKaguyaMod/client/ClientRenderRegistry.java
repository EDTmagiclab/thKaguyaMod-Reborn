package thKaguyaMod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelTHFairy;
import thKaguyaMod.client.render.effect.RenderSpellCardCircle;
import thKaguyaMod.client.render.item.RenderEmptyItem;
import thKaguyaMod.client.render.living.RenderTHFairy;
import thKaguyaMod.client.render.shot.RenderTHLaser;
import thKaguyaMod.client.render.shot.RenderTHShot;
import thKaguyaMod.client.render.spellcard.RenderSpellCard;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * 客户端渲染注册（1.20.1 移植版，阶段 4 止血）
 * 1.20.1 要求实体必须有 EntityRenderer，否则生成即崩溃（NPE）。
 * 这里注册妖精 + 弹体的模型层和渲染器。
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

        // item 实体（阶段 3d-4）：占位空渲染器，阶段 4-6 补真实模型
        event.registerEntityRenderer(THKaguyaEntities.CURSED_DECOY_DOLL.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.HISOU.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.KINKAKUZI.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.MARISA_BROOM.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.MAZINKYOUKAN.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.MINI_HAKKERO.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.PENDULUM.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.SAKUYA_STOP_WATCH.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.SAKUYA_WATCH.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.SILVER_KNIFE.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.SPIRITUAL_STRIKE_TALISMAN.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.SUKIMA.get(), RenderEmptyItem::new);
        event.registerEntityRenderer(THKaguyaEntities.YUUKA_PARASOL.get(), RenderEmptyItem::new);
    }
}
