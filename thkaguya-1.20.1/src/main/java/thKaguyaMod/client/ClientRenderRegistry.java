package thKaguyaMod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.model.living.ModelTHFairy;
import thKaguyaMod.client.render.effect.RenderSpellCardCircle;
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
    }
}
