package thKaguyaMod.gui;

import thKaguyaMod.THStackData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * レーザー作業台の結果スロット（1.20.1 移植版，阶段 5）
 * 原版 SlotLaserResult：取り出し時に素材/弾数/速度を消費。
 */
public class SlotLaserResult extends Slot
{
    private final Container craftMaterial;
    private final Container craftMatrix;
    private final Container craftNum;
    private final Container craftSpeed;
    private final Container craftCopy;
    private final Player thePlayer;

    public SlotLaserResult(Player player, Container material, Container form, Container num,
            Container speed, Container copy, Container result, int index, int x, int y)
    {
        super(result, index, x, y);
        this.thePlayer = player;
        this.craftMaterial = material;
        this.craftMatrix = form;
        this.craftNum = num;
        this.craftSpeed = speed;
        this.craftCopy = copy;
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

        int copyNum = 1;
        int oriNum = 1;

        if (!craftCopy.getItem(0).isEmpty())
        {
            copyNum += craftCopy.getItem(0).getCount();
        }
        if (!craftMaterial.getItem(0).isEmpty())
        {
            oriNum = craftMaterial.getItem(0).getCount();
        }
        if (copyNum > oriNum)
        {
            copyNum = oriNum;
        }

        for (int i = 0; i < craftMatrix.getContainerSize(); ++i)
        {
            ItemStack matrixStack = craftMatrix.getItem(i);
            if (!matrixStack.isEmpty())
            {
                // 原版 SlotLaserResult も容器アイテムを返せていなかった。1.20.1 の
                // getCraftingRemainingItem で残り物（バケツ等）を正しく返す。
                ItemStack remaining = matrixStack.getCraftingRemainingItem();
                craftMatrix.removeItem(i, 1);
                if (!remaining.isEmpty())
                {
                    if (craftMatrix.getItem(i).isEmpty())
                    {
                        craftMatrix.setItem(i, remaining);
                    }
                    else if (!this.thePlayer.getInventory().add(remaining))
                    {
                        this.thePlayer.drop(remaining, false);
                    }
                }
            }
        }

        int usePoint = THKaguyaConfig.laserMaxNumber;
        ItemStack materialStack = craftMaterial.getItem(0);
        if (!materialStack.isEmpty())
        {
            var nbt = THStackData.get(materialStack);
            if (nbt != null)
            {
                int baseNum = nbt.getShort("Number");
                if (!craftNum.getItem(0).isEmpty())
                {
                    if (craftNum.getItem(0).getCount() > usePoint - baseNum)
                    {
                        usePoint = usePoint - baseNum;
                    }
                }
            }
            else
            {
                if (!craftNum.getItem(0).isEmpty())
                {
                    int numNum = craftNum.getItem(0).getCount();
                    if (numNum > usePoint)
                    {
                        usePoint--;
                    }
                    else
                    {
                        usePoint = numNum;
                    }
                }
            }
        }
        slotUseProcess(craftNum, usePoint);

        usePoint = 64;
        materialStack = craftMaterial.getItem(0);
        if (!materialStack.isEmpty())
        {
            var nbt = THStackData.get(materialStack);
            if (nbt != null)
            {
                int baseSpeed = nbt.getByte("Speed");
                if (!craftSpeed.getItem(0).isEmpty())
                {
                    if (craftSpeed.getItem(0).getCount() > 64 - baseSpeed)
                    {
                        usePoint = 64 - baseSpeed;
                    }
                }
            }
        }
        slotUseProcess(craftSpeed, usePoint);

        slotUseProcess(craftMaterial, copyNum);
        slotUseProcess(craftCopy, copyNum - 1);

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
