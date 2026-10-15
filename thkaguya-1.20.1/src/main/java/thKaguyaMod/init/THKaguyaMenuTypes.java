package thKaguyaMod.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.gui.ContainerDanmakuCrafting;
import thKaguyaMod.gui.ContainerDanmakuCraftingLaser;
import thKaguyaMod.gui.ContainerMerchantSanae;
import thKaguyaMod.gui.ContainerMiracleMallet;

/**
 * GUI メニュー種別の登録クラス（1.20.1 移植版，阶段 5）
 * 原版 GuiHandler（IGuiHandler）→ 1.20.1 MenuType + IForgeMenuType。
 * 弹幕工作台/激光工作台经 BlockPos 传坐标；商店/小槌仅传玩家背包。
 */
public class THKaguyaMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, THKaguyaCore.MODID);

    public static final RegistryObject<MenuType<ContainerDanmakuCrafting>> DANMAKU_CRAFTING =
            MENU_TYPES.register("danmaku_crafting",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new ContainerDanmakuCrafting(windowId, inv, data.readBlockPos())));

    public static final RegistryObject<MenuType<ContainerDanmakuCraftingLaser>> DANMAKU_CRAFTING_LASER =
            MENU_TYPES.register("danmaku_crafting_laser",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new ContainerDanmakuCraftingLaser(windowId, inv, data.readBlockPos())));

    public static final RegistryObject<MenuType<ContainerMerchantSanae>> MERCHANT_SANAE =
            MENU_TYPES.register("merchant_sanae",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new ContainerMerchantSanae(windowId, inv)));

    public static final RegistryObject<MenuType<ContainerMiracleMallet>> MIRACLE_MALLET =
            MENU_TYPES.register("miracle_mallet",
                    () -> IForgeMenuType.create((windowId, inv, data) ->
                            new ContainerMiracleMallet(windowId, inv)));
}
