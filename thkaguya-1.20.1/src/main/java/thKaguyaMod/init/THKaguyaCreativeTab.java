package thKaguyaMod.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;

/**
 * 创造标签（1.20.1 移植版）
 * 原版 CreativeTabSpellCard -> 1.20.1 CreativeModeTab。
 * 阶段 1 提供一个主标签容纳全部物品；符卡专用标签后续阶段细分。
 */
public class THKaguyaCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, THKaguyaCore.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thkaguya"))
                    .icon(() -> new ItemStack(THKaguyaItems.HOURAI_JEWELED_BRANCH.get()))
                    .displayItems((params, output) -> {
                        THKaguyaItems.ITEMS.getEntries().forEach(entry ->
                                output.accept(new ItemStack(entry.get())));
                    })
                    .build());
}
