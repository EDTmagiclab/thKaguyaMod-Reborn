package thKaguyaMod.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 打ち出の小槌の結果スロット（1.20.1 移植版，阶段 5）
 * 原版 SlotMiracleMalletResult：取り出し時に素材を 1 個消費。
 */
public class SlotMiracleMalletResult extends Slot
{
    private final Container craftMaterial;
    private final Player thePlayer;

    public SlotMiracleMalletResult(Player player, Container material, Container result, int index, int x, int y)
    {
        super(result, index, x, y);
        this.thePlayer = player;
        this.craftMaterial = material;
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack resultStack)
    {
        this.checkTakeAchievements(resultStack);
        slotUseProcess(craftMaterial, 1);
        super.onTake(player, resultStack);
    }

    private void slotUseProcess(Container inventory, int useNum)
    {
        ItemStack stack = inventory.getItem(0);
        if (!stack.isEmpty() && useNum > 0)
        {
            ItemStack remaining = stack.getItem().hasCraftingRemainingItem()
                    ? stack.getItem().getCraftingRemainingItem(stack) : ItemStack.EMPTY;
            inventory.removeItem(0, useNum);

            if (!remaining.isEmpty())
            {
                if (inventory.getItem(0).isEmpty())
                {
                    inventory.setItem(0, remaining);
                }
                else if (!thePlayer.getInventory().add(remaining))
                {
                    thePlayer.drop(remaining, false);
                }
            }
        }
    }

    @Override
    public int getMaxStackSize()
    {
        return 64;
    }
}
