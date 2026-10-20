package thKaguyaMod.init;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thKaguyaMod.THKaguyaCore;

/**
 * 注册骨架。当前只有一个 smoke 物品，用来验证
 * {@code DeferredRegister.Items} / {@code DeferredItem} / mod bus 这条链路在 1.21.1 上可用；
 * 正式物品迁入时整体替换本类。
 */
public final class THKaguyaItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(THKaguyaCore.MODID);

    public static final DeferredItem<Item> SMOKE_ITEM =
            ITEMS.registerSimpleItem("smoke_item", new Item.Properties());

    private THKaguyaItems() {
    }
}
