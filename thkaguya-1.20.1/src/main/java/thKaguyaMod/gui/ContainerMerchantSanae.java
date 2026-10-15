package thKaguyaMod.gui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thKaguyaMod.init.THKaguyaMenuTypes;

/**
 * 早苗のアイテム交換コンテナ（1.20.1 移植版，阶段 5，简化）
 * 原版 ContainerMerchantSanae：商人交易界面（IMerchant 在原版即为 null 半成品，
 * 完整的村民交易延后到阶段 7）。此处保留双输入 + 结果 + 玩家背包的骨架。
 */
public class ContainerMerchantSanae extends AbstractContainerMenu
{
    private final Container merchantInventory = new SimpleContainer(3);

    public ContainerMerchantSanae(int windowId, Inventory playerInventory)
    {
        super(THKaguyaMenuTypes.MERCHANT_SANAE.get(), windowId);

        addSlot(new Slot(merchantInventory, 0, 36, 53));
        addSlot(new Slot(merchantInventory, 1, 62, 53));
        addSlot(new Slot(merchantInventory, 2, 120, 53) {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }
        });

        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i)
        {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 3)
            {
                if (!this.moveItemStackTo(stack, 3, 39, true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(stack, 0, 2, false))
            {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        for (int i = 0; i < 2; ++i)
        {
            ItemStack stack = merchantInventory.getItem(i);
            if (!stack.isEmpty())
            {
                player.drop(stack, false);
                merchantInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
