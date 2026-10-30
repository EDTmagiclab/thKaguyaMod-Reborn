package thKaguyaMod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.TimeStopManager;

/**
 * 時間停止中に凍らされたプレイヤーを「木桩」にする層（1.20.1 移植版で新規追加）。
 *
 * <h2>役割分担</h2>
 * <ul>
 *   <li><b>位置</b>は {@link TimeStopManager} が毎 tick アンカーへ teleport して固定する
 *       （移動はクライアントの移動パケット由来なので、tick を止めても歩けてしまう）。</li>
 *   <li><b>行動</b>はこのクラスが Forge イベントのキャンセルで塞ぐ。</li>
 * </ul>
 *
 * <h2>塞げるもの／塞げないもの</h2>
 * 攻撃・挖掘・設置・開容器・使う・捨てる・エンティティとの相互作用はイベントで塞げる。
 * <b>塞げないのは 2 つだけ</b>：ホットバー切り替え（{@code ServerboundSetCarriedItemPacket}）と、
 * 既に開いている画面の中でのアイテム移動（{@code ServerboundContainerClickPacket}）。
 * どちらも Forge イベントが存在せず Mixin が必要なので、本 MOD では<b>意図的に受け入れている</b>。
 * 容器は {@link TimeStopManager} 側が凍結時に閉じないので、開いたまま凍った場合だけ中身を動かせる。
 *
 * <p>クライアント側では何もしない（{@link TimeStopManager#isPlayerLocked} が
 * レベル同一性で false を返す）。振り・挖掘の見た目だけはローカルで続くが、サーバが受け付けない。</p>
 */
@Mod.EventBusSubscriber(modid = THKaguyaCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class THKaguyaTimeStopPlayerLock
{
    private THKaguyaTimeStopPlayerLock()
    {
    }

    private static boolean locked(Player player)
    {
        return player != null && !player.level().isClientSide && TimeStopManager.isPlayerLocked(player);
    }

    /** 近接攻撃 */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** ジャンプ攻撃のクリティカル判定（ダメージ自体は AttackEntityEvent で止まるが、演出も止める） */
    @SubscribeEvent
    public static void onCritical(CriticalHitEvent event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** ブロック破壊の開始（左クリック押しっぱなしの開始点） */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** 破壊速度を 0 に（開始をすり抜けても絶対に壊れないように二重で止める） */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if(locked(event.getEntity()))
        {
            event.setNewSpeed(0.0F);
        }
    }

    /** ブロックへの右クリック（設置・ドア・ボタン・チェストを開く等） */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** アイテムの使用（食料・ポーション・弓・盾・符卡のチャージ開始） */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** エンティティへの右クリック（村人取引・乗る・手綱・ハサミ等） */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** エンティティへの右クリック（精密版。両方止めないと種別によってすり抜ける） */
    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event)
    {
        if(locked(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /** アイテムを捨てる */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event)
    {
        if(locked(event.getPlayer()))
        {
            event.setCanceled(true);
        }
    }
}
