package thKaguyaMod.event;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thKaguyaMod.THKaguyaAdvancements;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 実験的機能（原作で神隠しにされた要素）の起動時処理。
 *
 * <p>万宝槌 GUI が有効なセーブでは、ログイン時に「在倒悬的天守阁中」の実績を付与する。
 * 実績の条件は {@code minecraft:impossible} なので、この処理が唯一の解除経路になる。
 * 設定はサーバー側（COMMON config）で判定するため、マルチでも全員に同じ挙動になる。
 */
@EventBusSubscriber(modid = THKaguyaCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class THKaguyaExperimentalEventHandler
{
    private THKaguyaExperimentalEventHandler()
    {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if(!THKaguyaConfig.enableMalletGui)
        {
            return;
        }

        // 何度ログインしても award() は既解除なら false を返すだけなので二重解除にはならない
        THKaguyaAdvancements.awardMiracleMalletGui(player);
    }
}
