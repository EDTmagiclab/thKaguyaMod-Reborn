package thKaguyaMod.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.gui.ContainerDanmakuCrafting;
import thKaguyaMod.gui.ContainerDanmakuCraftingLaser;
import thKaguyaMod.gui.ContainerMiracleMallet;

/**
 * GUI メニュー種別の登録クラス（1.20.1 移植版，阶段 5）
 * 原版 GuiHandler（IGuiHandler）→ 1.20.1 MenuType + IMenuTypeExtension。
 * 弹幕工作台/激光工作台经 BlockPos 传坐标；小槌仅传玩家背包。
 * 早苗/咲夜的商店不再走自定义菜单：原版的 ContainerMerchantSanae/GuiMerchantSanae
 * 本身就是打不开的死代码，交易直接用原版 MerchantMenu/MerchantScreen（见 EntityTradingDanmakuMob）。
 */
public class THKaguyaMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, THKaguyaCore.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerDanmakuCrafting>> DANMAKU_CRAFTING =
            MENU_TYPES.register("danmaku_crafting",
                    () -> IMenuTypeExtension.create((windowId, inv, data) ->
                            new ContainerDanmakuCrafting(windowId, inv, data.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerDanmakuCraftingLaser>> DANMAKU_CRAFTING_LASER =
            MENU_TYPES.register("danmaku_crafting_laser",
                    () -> IMenuTypeExtension.create((windowId, inv, data) ->
                            new ContainerDanmakuCraftingLaser(windowId, inv, data.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerMiracleMallet>> MIRACLE_MALLET =
            MENU_TYPES.register("miracle_mallet",
                    () -> IMenuTypeExtension.create((windowId, inv, data) ->
                            new ContainerMiracleMallet(windowId, inv)));
}
