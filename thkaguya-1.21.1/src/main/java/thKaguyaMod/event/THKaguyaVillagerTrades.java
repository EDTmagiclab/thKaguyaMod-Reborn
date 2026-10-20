package thKaguyaMod.event;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.init.THKaguyaVillagers;

/**
 * 霖之助的交易表（原版 1.7.10 {@code VillagerRinnosuke#manipulateTradesForVillager}）。
 *
 * <p>原版用 {@code MerchantRecipe(buying, selling)} 构造，默认 maxUses = 7、不提供经验、无动态涨价，
 * 这里逐条对齐（maxUses 7、villagerXp 0、priceMultiplier 0）。原版 8 条全部加在 1 级：
 * <pre>
 * 22 × power(小)      → 1  spiritual_strike_talisman（博丽御币）
 * 20 × 铁锭           → 1  mini_hakkero（迷你八卦炉）
 * 1  × 下界之星       → 1  magic_broom（原版 magic_bloom = ItemMarisaBroom，魔理沙的扫帚）
 * 10 × power(小)      → 9  point_item
 * 10 × point_item     → 9  power(小)
 * 1  × 红石           → 64 power(小)
 * 1  × 青金石         → 64 point_item
 * 1  × 金锭           → 1  末影珍珠
 * </pre>
 *
 * <p>原版 1.7.10 的 {@code power_item} 是带 metadata 的单物品（0 = 小、1 = 大），
 * 1.20.1 移植版拆成了 {@code power_up_item_small} / {@code power_up_item_big} 两个物品，
 * 这里对应原版 metadata 0 → {@code POWER_UP_ITEM_SMALL}。
 */
@EventBusSubscriber(modid = THKaguyaCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class THKaguyaVillagerTrades
{
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event)
    {
        if (event.getType() != THKaguyaVillagers.RINNOSUKE.get())
        {
            return;
        }

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        List<VillagerTrades.ItemListing> level1 = trades.computeIfAbsent(1, key -> new java.util.ArrayList<>());

        // 22 × power(小) → 博丽御币
        level1.add(offer(new ItemStack(THKaguyaItems.POWER_UP_ITEM_SMALL.get(), 22),
                new ItemStack(THKaguyaItems.SPIRITUAL_STRIKE_TALISMAN.get())));
        // 20 × 铁锭 → 迷你八卦炉
        level1.add(offer(new ItemStack(Items.IRON_INGOT, 20),
                new ItemStack(THKaguyaItems.MINI_HAKKERO.get())));
        // 1 × 下界之星 → 魔理沙的扫帚（原版字段名 magic_bloom，实际物品是 ItemMarisaBroom）
        level1.add(offer(new ItemStack(Items.NETHER_STAR, 1),
                new ItemStack(THKaguyaItems.MAGIC_BROOM.get())));
        // 10 × power(小) → 9 × point
        level1.add(offer(new ItemStack(THKaguyaItems.POWER_UP_ITEM_SMALL.get(), 10),
                new ItemStack(THKaguyaItems.POINT_ITEM.get(), 9)));
        // 10 × point → 9 × power(小)
        level1.add(offer(new ItemStack(THKaguyaItems.POINT_ITEM.get(), 10),
                new ItemStack(THKaguyaItems.POWER_UP_ITEM_SMALL.get(), 9)));
        // 1 × 红石 → 64 × power(小)
        level1.add(offer(new ItemStack(Items.REDSTONE, 1),
                new ItemStack(THKaguyaItems.POWER_UP_ITEM_SMALL.get(), 64)));
        // 1 × 青金石 → 64 × point（原版 Items.dye metadata 4 = 青金石）
        level1.add(offer(new ItemStack(Items.LAPIS_LAZULI, 1),
                new ItemStack(THKaguyaItems.POINT_ITEM.get(), 64)));
        // 1 × 金锭 → 1 × 末影珍珠
        level1.add(offer(new ItemStack(Items.GOLD_INGOT, 1),
                new ItemStack(Items.ENDER_PEARL, 1)));
    }

    /** 原版 MerchantRecipe(buying, selling) 的默认值：maxUses 7 / xp 0 / 不涨价 */
    private static VillagerTrades.ItemListing offer(ItemStack buying, ItemStack selling)
    {
        return (trader, random) -> new MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(buying.getItem(), buying.getCount()),
                selling, 7, 0, 0.0F);
    }

    private THKaguyaVillagerTrades()
    {
    }
}
