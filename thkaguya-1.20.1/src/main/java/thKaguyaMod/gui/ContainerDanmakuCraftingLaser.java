package thKaguyaMod.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.init.THKaguyaBlocks;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.init.THKaguyaMenuTypes;

/**
 * レーザー作業台コンテナ（1.20.1 移植版，阶段 5）
 * 原版 ContainerDanmakuCraftingLaser：3×3 形状 + 素材/弾数/速度/複製/色（无重力/跳ね返り）。
 */
public class ContainerDanmakuCraftingLaser extends AbstractContainerMenu
{
    public final TransientCraftingContainer craftMatrix;
    public final Container craftMaterial;
    public final Container craftSpeed;
    public final Container craftWay;
    public final Container craftCopy;
    public final Container craftColor;
    public final Container craftResult;

    private final Level level;
    private final BlockPos pos;

    private static final int PLAYER_INV_START = 15;
    private static final int PLAYER_INV_END = 51;

    public ContainerDanmakuCraftingLaser(int windowId, Inventory playerInventory, BlockPos pos)
    {
        super(THKaguyaMenuTypes.DANMAKU_CRAFTING_LASER.get(), windowId);
        this.level = playerInventory.player.level();
        this.pos = pos;

        this.craftMatrix = new TransientCraftingContainer(this, 3, 3);
        this.craftMaterial = new SimpleContainer(1);
        this.craftSpeed = new SimpleContainer(1);
        this.craftWay = new SimpleContainer(1);
        this.craftCopy = new SimpleContainer(1);
        this.craftColor = new SimpleContainer(1);
        this.craftResult = new SimpleContainer(1);

        addSlot(new SlotLaserResult(playerInventory.player, this.craftMaterial, this.craftMatrix,
                this.craftWay, this.craftSpeed, this.craftCopy, this.craftResult, 0, 228, 35));
        addSlot(new SlotDanmakuCraftingA(this.craftMaterial, 0, 8, 17, 5));
        addSlot(new SlotDanmakuCraftingA(this.craftWay, 0, 8, 35, 0));
        addSlot(new SlotDanmakuCraftingA(this.craftSpeed, 0, 8, 53, 0));
        addSlot(new SlotDanmakuCraftingA(this.craftCopy, 0, 192, 17, 1));
        addSlot(new SlotDanmakuCraftingA(this.craftColor, 0, 69, 17, 3));

        for (int l = 0; l < 3; ++l)
        {
            for (int i = 0; i < 3; ++i)
            {
                addSlot(new SlotDanmakuCraftingA(this.craftMatrix, i + l * 3, 134 + i * 18, 17 + l * 18, 0));
            }
        }

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

        recalcResult();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return this.level.getBlockState(this.pos).is(THKaguyaBlocks.LASER_CRAFTING_TABLE.get())
                && player.distanceToSqr(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void slotsChanged(Container inventory)
    {
        super.slotsChanged(inventory);
        recalcResult();
    }

    private void recalcResult()
    {
        ItemStack shot = craftMaterial.getItem(0);
        short way = 1;
        byte form = 0;
        byte addSpeed = 0;
        byte color = 8;

        if (!shot.isEmpty() && shot.getTag() != null)
        {
            var nbt = shot.getTag();
            way = nbt.getShort("Number");
            form = nbt.getByte("DanmakuForm");
            addSpeed = nbt.getByte("Speed");
            color = nbt.getByte("Color");
        }

        if (!craftSpeed.getItem(0).isEmpty())
        {
            addSpeed += (byte)craftSpeed.getItem(0).getCount();
            if (addSpeed > 64 || addSpeed < 0)
            {
                addSpeed = 64;
            }
        }

        if (!craftWay.getItem(0).isEmpty())
        {
            way += (short)craftWay.getItem(0).getCount();
            if (way > THKaguyaConfig.laserMaxNumber || way < 0)
            {
                way = (short)THKaguyaConfig.laserMaxNumber;
            }
        }

        boolean[] m = new boolean[9];
        for (int i = 0; i < 9; i++)
        {
            m[i] = !craftMatrix.getItem(i).isEmpty();
        }
        form = ContainerDanmakuCrafting.detectForm(m, way, form);

        if (!craftColor.getItem(0).isEmpty())
        {
            ItemStack colorStack = craftColor.getItem(0);
            if (colorStack.getItem() == Items.RED_DYE)
            {
                color = (byte)DanmakuConstants.SELECTABLE_COLOR[Math.floorMod(colorStack.getDamageValue(), 16)];
            }
            else if (colorStack.getItem() == Items.GUNPOWDER)
            {
                color = DanmakuConstants.RANDOM;
            }
            else if (colorStack.getItem() == Items.DIAMOND)
            {
                color = DanmakuConstants.RAINBOW;
            }
        }

        if (!shot.isEmpty() && !(form == 4 && way > 32) && form >= 0)
        {
            ItemStack newShot = new ItemStack(THKaguyaItems.LASER_MATERIAL.get());
            newShot.setDamageValue(craftMaterial.getItem(0).getDamageValue());

            var nbt2 = newShot.getOrCreateTag();
            nbt2.putShort("Number", way);
            nbt2.putByte("DanmakuForm", form);
            nbt2.putByte("Speed", addSpeed);
            nbt2.putByte("Color", color);

            int copyNum = 1;
            if (!craftCopy.getItem(0).isEmpty())
            {
                int oriNum = shot.getCount();
                copyNum = craftCopy.getItem(0).getCount() + 1;
                if (copyNum > oriNum)
                {
                    copyNum = oriNum;
                }
                newShot.setCount(copyNum);
            }
            craftResult.setItem(0, newShot);
        }
        else
        {
            craftResult.setItem(0, ItemStack.EMPTY);
        }
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

            if (index < PLAYER_INV_START)
            {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                if (!this.moveItemStackTo(stack, 1, PLAYER_INV_START, false))
                {
                    return ItemStack.EMPTY;
                }
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
        if (!this.level.isClientSide)
        {
            clearContainer(player, this.craftMatrix);
            clearContainer(player, this.craftMaterial);
            clearContainer(player, this.craftWay);
            clearContainer(player, this.craftSpeed);
            clearContainer(player, this.craftCopy);
            clearContainer(player, this.craftColor);
        }
    }

    protected void clearContainer(Player player, Container container)
    {
        for (int i = 0; i < container.getContainerSize(); ++i)
        {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty())
            {
                player.drop(stack, false);
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
