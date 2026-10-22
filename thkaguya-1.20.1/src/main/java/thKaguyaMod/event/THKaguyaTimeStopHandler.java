package thKaguyaMod.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.TimeStopManager;

/**
 * 時間停止の tick ゲート（1.20.1 移植版で新規追加）。
 *
 * <p>原版 1.7.10 の {@code EntitySakuyaWatch#onUpdate} が毎 tick 全 Entity の座標を巻き戻していた処理を、
 * Carpet MOD と同じ「tick 入口で gate する」方式に置き換えたもの。判定そのものは
 * {@link TimeStopManager#isFrozen(Entity)} に集約してある。</p>
 *
 * <h2>LivingTickEvent だけでは足りない理由</h2>
 * Forge は {@code LivingEntity#tick()} の先頭でこのイベントを発火する。ところが
 * {@code EntityDanmakuMob#tick()} は tick を丸ごと上書きしていて、
 *
 * <pre>
 * public void tick() {
 *     ...                 // ← ここはイベントで止まらない
 *     super.tick();       // ← やっと LivingEntity#tick()（＝イベント）
 *     tickBossBar();      // ← イベントをキャンセルしても走る
 *     danmakuPattern();   // ← 弾幕を撃ち続ける
 * }
 * </pre>
 *
 * <p>となっている。よって自前 Entity は各自の {@code tick()} 先頭で早期 return し、
 * ここではバニラ生物（ゾンビ・スケルトン・村人など）だけを担当する。</p>
 */
@Mod.EventBusSubscriber(modid = THKaguyaCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class THKaguyaTimeStopHandler
{
    private THKaguyaTimeStopHandler()
    {
    }

    /** バニラ／他 MOD の生物を丸ごと止める（tickCount・AI・受傷・移動のすべてが止まる） */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity entity = event.getEntity();
        if(entity == null)
        {
            return;
        }
        if(TimeStopManager.blockTick(entity))
        {
            // 座標も向きも変わらないので補間用の旧値はそのままでよい（復帰時の瞬間移動が起きない）
            event.setCanceled(true);
        }
    }

    /** 看門狗・失効判定。サーバ tick の終わりに 1 回だけ走る */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if(event.phase == TickEvent.Phase.END)
        {
            TimeStopManager.serverTick();
        }
    }

    /** 次元が降りたら即解除（宙ぶらりんの状態を残さない） */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if(!(event.getLevel() instanceof ServerLevel))
        {
            return;
        }
        TimeStopManager.stop();
    }

    /** 術者がログアウトしたら解除（時計だけが残って誰も止められなくなるのを防ぐ） */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if(TimeStopManager.isActive() && TimeStopManager.getOwner() == event.getEntity())
        {
            // 持ち主はもう居ないので、時計はその場に落とす（インベントリに入れても消える）
            TimeStopManager.stopAndDiscardWatch(false);
        }
    }

    /** サーバ停止時に randomTickSpeed を必ず戻す（0 のまま level.dat に残るのを防ぐ） */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        TimeStopManager.onServerStopping();
    }
}
