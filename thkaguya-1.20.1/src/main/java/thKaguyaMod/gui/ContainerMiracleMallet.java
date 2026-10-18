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
     * 原版との意図的な差異（実験的機能として復活させた際の判断。報告書 13.10 参照）
     *
     *   1. 原版 ContainerMiracleMallet.java:251-254 は {@code if(player.experienceLevel >= 30)} を
     *      満たすときだけ産物を返す（レベル 30 制限）。ただし素材が入った時点で
     *      CraftingInventory のリスナーが既に産物を組んでおり、この分岐はクリック後の再計算に
     *      しか効かないため、移植版は制限を課さない（原版でも実質無制限）。
     *   2. 原版 :100-108 は {@code alicedoll.bare} を {@code cursed_decoy_doll} に写像する分岐を持つが、
     *      参照先は外部 mod「Alice's Dolls」のアイテムで、本 mod 単体では存在しない。
     *      移植版はこの分岐を持たず {@code originID} は常に -1 のため、
     *      ItemCursedDecoyDoll 側の OriginalItemID/OriginalDamage 復元処理は休眠状態になる。
     *   3. 原版の slotClick は 30 レベル時に選択スロットを再計算して返すが、1 と同理由で移植しない。
     */

    /** 素材槽改动 → 重算产物（结果槽不挂监听器，故不会递归）。 */
    private final ContainerListener recalcListener = this::slotsChanged;

    public ContainerMiracleMallet(int windowId, Inventory playerInventory)
    {
        super(THKaguyaMenuTypes.MIRACLE_MALLET.get(), windowId);
        this.craftMaterial = new SimpleContainer(1);
        this.craftResult = new SimpleContainer(1);

        // 坐标必须与 client/gui/GuiMiracleMallet 的 MATERIAL_X/Y・RESULT_X/Y 一致
        // （原版是 (228,35) 与 (8,17)，这里为了「妖器化」专用外观改成居中的 素材→結果 布局）
        addSlot(new SlotMiracleMalletResult(playerInventory.player, this.craftMaterial,
                this.craftResult, 0, 139, 35));
        addSlot(new Slot(this.craftMaterial, 0, 99, 35));

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
                slot.setByPlayer(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }

            // 什么都没移动なら终了（原版 AbstractContainerMenu 同判定）
            if (stack.getCount() == result.getCount())
            {
                return ItemStack.EMPTY;
            }

            // 原版 1.7.10 的 transferStackInSlot 末尾有 slot.onPickupFromSlot(...)，
            // 结果槽靠这次调用消耗素材；1.20.1 的等价物是 onTake。
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);

        // 客户端也会调用 removed()，素材只在服务端掉落（原版即如此，另 2 个容器同样判定）
        if (player.level().isClientSide)
        {
            return;
        }

        ItemStack stack = craftMaterial.getItem(0);
        if (!stack.isEmpty())
        {
            player.drop(stack, false);
            craftMaterial.setItem(0, ItemStack.EMPTY);
        }
    }
}
