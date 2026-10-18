package thKaguyaMod.gui;

import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.init.THKaguyaMenuTypes;

/**
 * 打ち出の小槌コンテナ（1.20.1 移植版，阶段 5）
 * 原版 ContainerMiracleMallet：素材 → 结果（阴阳玉→血饿阴阳玉、隙间→折叠伞、楼観剣→楼観旋風刃）。
 */
public class ContainerMiracleMallet extends AbstractContainerMenu
{
    private final Container craftMaterial;
    private final Container craftResult;

    /*
     * TODO(D1・未実装・保留)：万宝槌コンテナの潜在的な原版乖離 4 点。
     * （現在この GUI は openScreen されないため到達不能。実装可否は後日判断）
     *
     *   1. 原版 ContainerMiracleMallet.java:251-254 は
     *      {@code if(player.experienceLevel >= 30)} を満たすときだけ産物を返す（レベル 30 制限）。
     *      移植版は無条件に産物を組み立てる。
     *   2. 原版 :100-108 は {@code alicedoll.bare} を {@code cursed_decoy_doll} に写像する分岐を持つ。
     *      移植版はこの分岐が無く {@code originID} が常に -1 のため、
     *      ItemCursedDecoyDoll 側の OriginalItemID/OriginalDamage 復元処理が休眠状態。
     *   3. 移植版 {@code removed()} は {@code level().isClientSide} を判定せずに素材をドロップする
     *      （他の 2 コンテナは判定済み）。
     *   4. 移植版 GuiMiracleMallet は原版の {@code miracleMallet.material} ラベル（原版 134,6）を描かない。
     */

    /** 素材槽改动 → 重算产物（结果槽不挂监听器，故不会递归）。 */
    private final ContainerListener recalcListener = this::slotsChanged;

    public ContainerMiracleMallet(int windowId, Inventory playerInventory)
    {
        super(THKaguyaMenuTypes.MIRACLE_MALLET.get(), windowId);
        this.craftMaterial = new SimpleContainer(1);
        this.craftResult = new SimpleContainer(1);

        addSlot(new SlotMiracleMalletResult(playerInventory.player, this.craftMaterial,
                this.craftResult, 0, 228, 35));
        addSlot(new Slot(this.craftMaterial, 0, 8, 17));

        // SimpleContainer 不会自己通知 menu（只有 TransientCraftingContainer 会），必须手动挂，
        // 否则往素材槽放东西后产物不会刷新。
        ((SimpleContainer) this.craftMaterial).addListener(this.recalcListener);

        for (int l = 0; l < 3; ++l)
        {
            for (int i = 0; i < 9; ++i)
            {
                addSlot(new Slot(playerInventory, i + l * 9 + 9, 48 + i * 18, 84 + l * 18));
            }
        }
        for (int l = 0; l < 9; ++l)
        {
            addSlot(new Slot(playerInventory, l, 48 + l * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public void slotsChanged(Container inventory)
    {
        super.slotsChanged(inventory);

        ItemStack originItem = craftMaterial.getItem(0);
        ItemStack resultItem = ItemStack.EMPTY;

        if (!originItem.isEmpty())
        {
            boolean isCommonDamage = false;
            int originID = -1;

            if (originItem.getItem() == THKaguyaItems.YIN_YANG_ORB.get())
            {
                resultItem = new ItemStack(THKaguyaItems.BLOODTHIRSTY_YIN_YANG_ORB.get());
            }
            else if (originItem.getItem() == THKaguyaItems.GAP.get())
            {
                resultItem = new ItemStack(THKaguyaItems.GAP_FOLDING_UMBRELLA.get());
            }
            else if (originItem.getItem() == THKaguyaItems.ROUKANKEN.get())
            {
                resultItem = new ItemStack(THKaguyaItems.ROUKAN_SENPUUZIN.get());
                resultItem.setDamageValue(originItem.getDamageValue());
                isCommonDamage = true;
            }

            if (!resultItem.isEmpty() && !isCommonDamage)
            {
                var nbt = resultItem.getOrCreateTag();
                nbt.putInt("OriginalDamage", originItem.getDamageValue());
                if (originID >= 0)
                {
                    nbt.putInt("OriginalItemID", originID);
                }
            }
        }

        craftResult.setItem(0, resultItem);
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

            if (index < 2)
            {
                if (!this.moveItemStackTo(stack, 2, 38, true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(stack, 1, 2, false))
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
        ItemStack stack = craftMaterial.getItem(0);
        if (!stack.isEmpty())
        {
            player.drop(stack, false);
            craftMaterial.setItem(0, ItemStack.EMPTY);
        }
    }
}
