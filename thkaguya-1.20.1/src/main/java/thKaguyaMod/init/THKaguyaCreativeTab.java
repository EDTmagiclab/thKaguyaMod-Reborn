package thKaguyaMod.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.registry.SpellCardRegistry;

/**
 * 创造标签（1.20.1 移植版）
 *
 * 原版有两个创造标签：
 *   1) 通常アイテム（各アイテムを new する際に setCreativeTab で割り当て）
 *   2) CreativeTabSpellCard … スペルカード専用タブ（icon = spell_card、
 *      getSubItems で SpellCardRegistry.sortSpellCard() の順に全符卡を並べる）
 *
 * 1.20.1 では CreativeModeTab を DeferredRegister で登録し、displayItems で中身を供給する。
 * 符卡は damage 値（＝符卡 No）違いの同一アイテムなので、ここで全 No 分を流し込む。
 */
public class THKaguyaCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, THKaguyaCore.MODID);

    /** 通常アイテムタブ（符卡は専用タブへ分離するため除外） */
    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thkaguya"))
                    .icon(() -> new ItemStack(THKaguyaItems.HOURAI_JEWELED_BRANCH.get()))
                    .displayItems((params, output) -> {
                        THKaguyaItems.ITEMS.getEntries().forEach(entry -> {
                            // 原版では spell_card は setCreativeTab(tabSpellCard) で専用タブのみに所属していた
                            if (entry.get() == THKaguyaItems.SPELL_CARD.get()) {
                                return;
                            }
                            output.accept(new ItemStack(entry.get()));
                        });
                    })
                    .build());

    /** スペルカード専用タブ（原版 CreativeTabSpellCard 相当） */
    public static final RegistryObject<CreativeModeTab> SPELL_CARD = CREATIVE_TABS.register("spell_card",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thkaguya.spellcard"))
                    .icon(() -> new ItemStack(THKaguyaItems.SPELL_CARD.get()))
                    .displayItems((params, output) -> {
                        for (int id : sortedSpellCardIds())
                        {
                            ItemStack stack = new ItemStack(THKaguyaItems.SPELL_CARD.get());
                            stack.setDamageValue(id);
                            output.accept(stack);
                        }
                    })
                    .build());

    /**
     * 符卡 No を並び順で返す。
     * 原版は ItemTHSpellCard.getSubItems で SpellCardRegistry.sortSpellCard()（使用者順）を使っていた。
     * 符卡登録（FMLCommonSetupEvent）より前に呼ばれた場合に備え、空なら ID 昇順へフォールバックする。
     */
    private static List<Integer> sortedSpellCardIds()
    {
        List<Integer> ids = new ArrayList<Integer>();

        List<String> sorted = SpellCardRegistry.sortSpellCard();
        if (sorted != null && !sorted.isEmpty())
        {
            for (String name : sorted)
            {
                int id = SpellCardRegistry.getSpellCardNumber(name);
                if (id >= 0 && !ids.contains(id))
                {
                    ids.add(id);
                }
            }
        }

        if (ids.isEmpty())
        {
            ids.addAll(SpellCardRegistry.spellcardID.values());
            Collections.sort(ids);
        }

        return ids;
    }
}
