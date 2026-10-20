package thKaguyaMod.event;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 箱子战利品：万宝槌（原版 1.7.10 的 registerChestItem / ItemUchide#getChestGenBase）。
 *
 * <p><b>有意新增</b>：原版 {@code CommonProxy#registerChestItem()} 是空方法，
 * {@code ItemUchide#getChestGenBase(...)} 也从未被注册（1.7.10 里根本没有这个接口回调），
 * 所以「万宝槌进箱子」在原版是死代码。移植版把它真正接上：四个原版就会自然生成的箱子表，
 * 低概率塞一把万宝槌（原版也没有配方可以让玩家自己做，只能靠这个渠道获得）。
 *
 * <p>用 {@link LootTableLoadEvent} 动态加池，而不是数据包 JSON：
 * 好处是不依赖 global_loot_modifiers 的路径/格式，且池名可判重（避免重复加载时叠池）。
 */
@EventBusSubscriber(modid = THKaguyaCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class THKaguyaChestLoot
{
    /** 池名（判重 / 出问题好定位） */
    private static final String POOL_NAME = "thkaguya_miracle_mallet";

    /** 目标箱子表 → 出现概率（地牢/要塞书库 5%，村庄工具匠/武器匠 10%） */
    private static final Map<ResourceLocation, Float> CHEST_CHANCES = Map.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"), 0.05F,
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_library"), 0.05F,
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_toolsmith"), 0.10F,
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_weaponsmith"), 0.10F);

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event)
    {
        Float chance = CHEST_CHANCES.get(event.getName());
        if (chance == null)
        {
            return;
        }
        // 重复加载（回退存档 / 数据包重载）时不叠池
        if (event.getTable().getPool(POOL_NAME) != null)
        {
            return;
        }

        LootPool pool = LootPool.lootPool()
                .name(POOL_NAME)
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(THKaguyaItems.MIRACLE_MALLET.get())
                        .when(LootItemRandomChanceCondition.randomChance(chance))
                        .setWeight(1))
                .build();
        event.getTable().addPool(pool);
    }

    private THKaguyaChestLoot()
    {
    }
}
