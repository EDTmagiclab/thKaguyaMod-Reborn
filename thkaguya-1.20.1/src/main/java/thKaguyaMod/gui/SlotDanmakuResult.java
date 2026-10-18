package thKaguyaMod.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 弾幕作業台の結果スロット（1.20.1 移植版，阶段 5）
 * 原版 SlotDanmakuResult：取り出し時に素材/弾数/速度/重力/跳ね返りを消費。
 */
public class SlotDanmakuResult extends Slot
{
    private final Container craftMaterial;
    private final Container craftMatrix;
    private final Container craftNum;
    private final Container craftSpeed;
    private final Container craftCopy;
    private final Container craftGravity;
    private final Container craftSpecial;
    private final Player thePlayer;

    public SlotDanmakuResult(Player player, Container material, Container form, Container num,
            Container speed, Container copy, Container gravity, Container special,
            Container result, int index, int x, int y)
    {
        super(result, index, x, y);
        this.thePlayer = player;
        this.craftMaterial = material;
        this.craftMatrix = form;
        this.craftNum = num;
        this.craftSpeed = speed;
        this.craftCopy = copy;
        this.craftGravity = gravity;
        this.craftSpecial = special;
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

        // 形状素材（3×3 マトリクス）を 1 個ずつ消費
        for (int i = 0; i < craftMatrix.getContainerSize(); ++i)
        {
            ItemStack matrixStack = craftMatrix.getItem(i);
            if (!matrixStack.isEmpty())
            {
                // 原版 SlotDanmakuResult.java:114-146 は hasContainerItem の分岐を持つが、
                // 本体が「itemstack2 = itemstack1」になっていて容器アイテムを返せていなかった。
                // ここでは 1.20.1 の getCraftingRemainingItem で正しく残り物（バケツ等）を返す。
                ItemStack remaining = matrixStack.getCraftingRemainingItem();
                craftMatrix.removeItem(i, 1);
                if (!remaining.isEmpty())
                {
                    if (craftMatrix.getItem(i).isEmpty())
                    {
                        craftMatrix.setItem(i, remaining);
                    }
                    else if (!player.getInventory().add(remaining))
                    {
                        player.drop(remaining, false);
                    }
                }
            }
        }

        // 弾数
        int usePoint = THKaguyaConfig.shotMaxNumber;
        ItemStack materialStack = craftMaterial.getItem(0);
        if (!materialStack.isEmpty())
        {
            var nbt = materialStack.getTag();
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

        // 速度
        usePoint = 64;
        materialStack = craftMaterial.getItem(0);
        if (!materialStack.isEmpty())
        {
            var nbt = materialStack.getTag();
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

        // 重力
        usePoint = 16;
        materialStack = craftMaterial.getItem(0);
        if (!materialStack.isEmpty())
        {
            var nbt = materialStack.getTag();
            if (nbt != null)
            {
                int baseGravity = nbt.getByte("Gravity");
                if (!craftGravity.getItem(0).isEmpty())
                {
                    if (craftGravity.getItem(0).getCount() > 16 - baseGravity)
                    {
                        usePoint = 16 - baseGravity;
                    }
                }
            }
        }
        slotUseProcess(craftGravity, usePoint);

        // 跳ね返り
        usePoint = 4;
        materialStack = craftMaterial.getItem(0);
        if (!materialStack.isEmpty())
        {
            var nbt = materialStack.getTag();
            if (nbt != null)
            {
                int baseBound = nbt.getInt("Special") - THShotLib.BOUND01 + 1;
                if (!craftSpecial.getItem(0).isEmpty())
                {
                    if (craftSpecial.getItem(0).getCount() > 4 - baseBound)
                    {
                        usePoint = 4 - baseBound;
                    }
                }
            }
        }
        slotUseProcess(craftSpecial, usePoint);
        slotUseProcess(craftMaterial, copyNum);
        slotUseProcess(craftCopy, copyNum - 1);

        super.onTake(player, resultStack);
    }

    /** 完成したアイテムを取り出したときの、各インベントリでの処理 */
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
