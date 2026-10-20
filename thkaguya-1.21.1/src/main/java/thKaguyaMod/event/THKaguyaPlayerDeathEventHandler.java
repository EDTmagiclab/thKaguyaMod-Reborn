package thKaguyaMod.event;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;
import thKaguyaMod.entity.living.EntityDanmakuMob;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.init.THKaguyaSounds;

/**
 * EntityLiving が倒れたときの割り込み処理（原版 thKaguyaMod.event.THKaguyaPlayerDeathEventHandler の移植）
 *
 * <p>プレイヤーが倒れたとき、所持していれば「エクステンドアイテム（extend_item）」を 1 つ消費して
 * その場で復活する。原版の実装は 1.7.10 の {@code LivingDeathEvent} 内で
 * {@code player.isDead = false} を直接書き換える荒技だったが、1.20.1 では
 * {@code LivingEntity.die()} の先頭で {@code ForgeHooks.onLivingDeath()} が呼ばれ、
 * イベントがキャンセルされていれば {@code die()} が即 return する（javap で確認済み）ため、
 * {@code event.setCanceled(true)} + 体力復元で同じ結果になる。
 *
 * <p>1.7.10 → 1.20.1 の対応：
 * <ul>
 *   <li>{@code player.hurtResistantTime = 100} → {@code player.invulnerableTime = 100}</li>
 *   <li>{@code player.isDead = false} + {@code setHealth(max)} → {@code event.setCanceled(true)} + {@code setHealth(max)}</li>
 *   <li>{@code inventory.mainInventory[i]} → {@code player.getInventory().items.get(i)}</li>
 *   <li>{@code "thkaguyamod:down"} → {@code THKaguyaSounds.DOWN}（sounds.json の "down"）</li>
 *   <li>{@code THShotLib.PURPLE}(0x0004) → {@code EntityDanmakuMob.CIRCLE_COLOR_PURPLE}(4)（同値）</li>
 * </ul>
 */
@EventBusSubscriber(modid = THKaguyaCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class THKaguyaPlayerDeathEventHandler
{
    private THKaguyaPlayerDeathEventHandler()
    {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if(!(event.getEntity() instanceof Player player))
        {
            return;
        }
        if(player.level().isClientSide)
        {
            return;
        }

        // 倒れた瞬間の効果音（原版：倒れた場所で thkaguyamod:down を鳴らす）
        player.level().playSound(null, player.blockPosition(), THKaguyaSounds.DOWN.get(),
                SoundSource.PLAYERS, THKaguyaConfig.DownVol, 1.0F);

        // エクステンドアイテムを持っていれば 1 つ消費してその場で復活
        for(int i = 0; i < player.getInventory().items.size(); i++)
        {
            ItemStack stack = player.getInventory().items.get(i);
            if(!stack.isEmpty() && stack.is(THKaguyaItems.EXTEND_ITEM.get()))
            {
                stack.shrink(1);

                // 原版：hurtResistantTime = 100 → 復活直後の連続ダメージを防ぐ
                event.setCanceled(true);
                player.invulnerableTime = 100;
                player.setHealth(player.getMaxHealth());

                // 復活演出：無敵時間と同じ長さの符卡円（原版は hurtResistantTime をそのまま渡していた）
                EntitySpellCardCircle circle = new EntitySpellCardCircle(
                        player.level(), player, EntityDanmakuMob.CIRCLE_COLOR_PURPLE, player.invulnerableTime);
                player.level().addFreshEntity(circle);
                break;
            }
        }
    }
}
