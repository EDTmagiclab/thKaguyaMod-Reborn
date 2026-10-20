package thKaguyaMod;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * 実績（advancement）付与の補助。
 *
 * <p>1.20.1 では {@code ServerAdvancementManager} が JSON を読み込み、
 * {@code ServerPlayer#getAdvancements()#award(Advancement, String)} で解除する。
 * 条件は {@code minecraft:impossible} なので自動解除はされず、コードからの付与のみ。
 *
 * <p>本 mod は「原作で神隠し（コメントアウト）にされた万宝槌 GUI」を実験的機能として
 * 復活させるため、その有効化を記念する実績を用意している（{@link #MIRACLE_MALLET_GUI}）。
 */
public final class THKaguyaAdvancements
{
    /** 実験的機能：万宝槌 GUI を有効化した（icon は万宝槌そのもの） */
    public static final ResourceLocation MIRACLE_MALLET_GUI =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "experimental/miracle_mallet_gui");

    private THKaguyaAdvancements()
    {
    }

    /**
     * 指定 ID の実績をプレイヤーに付与する。既に解除済み・未登録なら何もしない。
     *
     * @return 新たに解除されたら true
     */
    public static boolean award(ServerPlayer player, ResourceLocation id, String criterion)
    {
        MinecraftServer server = player.getServer();
        if(server == null)
        {
            return false;
        }

        ServerAdvancementManager manager = server.getAdvancements();
        if(manager == null)
        {
            return false;
        }

        net.minecraft.advancements.AdvancementHolder advancement = manager.get(id);
        if(advancement == null)
        {
            return false;
        }

        return player.getAdvancements().award(advancement, criterion);
    }

    /** 万宝槌 GUI 有効化の実績を付与する */
    public static boolean awardMiracleMalletGui(ServerPlayer player)
    {
        return award(player, MIRACLE_MALLET_GUI, "enabled");
    }
}
