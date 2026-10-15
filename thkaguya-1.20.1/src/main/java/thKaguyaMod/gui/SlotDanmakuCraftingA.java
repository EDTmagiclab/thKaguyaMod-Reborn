package thKaguyaMod.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 弾幕作業台の材料スロット（1.20.1 移植版，阶段 5）
 * 原版 SlotDanmakuCraftingA：按 slotType 限定可放入的物品。
 *   type 0 = 小 power up（弾数/速度/重力/形状）、type 1 = 点符（複製）、
 *   type 2 = 弾幕の素、type 3 = 染料/火药/钻石（色）、type 4 = 大 power up（跳ね返り）、
 *   type 5 = レーザーの素。
 */
public class SlotDanmakuCraftingA extends Slot
{
    private final int slotType;

    public SlotDanmakuCraftingA(Container container, int index, int x, int y, int type)
    {
        super(container, index, x, y);
        this.slotType = type;
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        switch (slotType)
        {
            case 0:
                return stack.getItem() == THKaguyaItems.POWER_UP_ITEM_SMALL.get();
            case 1:
                return stack.getItem() == THKaguyaItems.POINT_ITEM.get();
            case 2:
                return stack.getItem() == THKaguyaItems.SHOT_MATERIAL.get();
            case 3:
                return stack.getItem() == Items.RED_DYE ||
                       stack.getItem() == Items.GUNPOWDER ||
                       stack.getItem() == Items.DIAMOND;
            case 4:
                return stack.getItem() == THKaguyaItems.POWER_UP_ITEM_BIG.get();
            case 5:
                return stack.getItem() == THKaguyaItems.LASER_MATERIAL.get();
            default:
                return false;
        }
    }

    @Override
    public int getMaxStackSize()
    {
        return 64;
    }
}
