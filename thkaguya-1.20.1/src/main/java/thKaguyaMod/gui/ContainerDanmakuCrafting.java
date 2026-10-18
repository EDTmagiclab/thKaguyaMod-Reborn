package thKaguyaMod.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaBlocks;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.init.THKaguyaMenuTypes;

/**
 * 弾幕作業台コンテナ（1.20.1 移植版，阶段 5）
 * 原版 ContainerDanmakuCrafting：3×3 形状マトリクス + 素材/弾数/速度/複製/色/重力/跳ね返りスロット。
 * onCraftMatrixChanged 的逻辑原样迁移（NBT Number/DanmakuForm/Speed/Color/Special/Gravity）。
 */
public class ContainerDanmakuCrafting extends AbstractContainerMenu
{
    public final TransientCraftingContainer craftMatrix;
    public final Container craftMaterial;
    public final Container craftSpeed;
    public final Container craftWay;
    public final Container craftCopy;
    public final Container craftColor;
    public final Container craftBound;
    public final Container craftGravity;
    public final Container craftResult;

    private final Level level;
    private final BlockPos pos;
    private final Player player;

    /**
     * 参数槽（素材/弹数/速度/复制/颜色/重力/反弹）任一改动 → 重算产物。
     * 结果槽 {@code craftResult} 不挂监听器，所以 {@link #recalcResult()} 里写结果不会递归。
     */
    private final ContainerListener recalcListener = this::slotsChanged;

    private static final int RESULT_SLOT = 0;
    private static final int PLAYER_INV_START = 17;
    private static final int PLAYER_INV_END = 53;

    public ContainerDanmakuCrafting(int windowId, Inventory playerInventory, BlockPos pos)
    {
        super(THKaguyaMenuTypes.DANMAKU_CRAFTING.get(), windowId);
        this.level = playerInventory.player.level();
        this.pos = pos;
        this.player = playerInventory.player;

        this.craftMatrix = new TransientCraftingContainer(this, 3, 3);
        this.craftMaterial = new SimpleContainer(1);
        this.craftSpeed = new SimpleContainer(1);
        this.craftWay = new SimpleContainer(1);
        this.craftCopy = new SimpleContainer(1);
        this.craftColor = new SimpleContainer(1);
        this.craftBound = new SimpleContainer(1);
        this.craftGravity = new SimpleContainer(1);
        this.craftResult = new SimpleContainer(1);

        // SimpleContainer 不会自己通知 menu（只有 TransientCraftingContainer 会），
        // 不手动挂监听器就会出现「放了染料，产物还停在上一次算出来的颜色」。
        for (Container input : new Container[] { this.craftMaterial, this.craftWay, this.craftSpeed,
                this.craftCopy, this.craftColor, this.craftGravity, this.craftBound })
        {
            ((SimpleContainer) input).addListener(this.recalcListener);
        }

        addSlot(new SlotDanmakuResult(player, this.craftMaterial, this.craftMatrix, this.craftWay,
                this.craftSpeed, this.craftCopy, this.craftGravity, this.craftBound,
                this.craftResult, 0, 228, 35));
        addSlot(new SlotDanmakuCraftingA(this.craftMaterial, 0, 8, 17, 2));
        addSlot(new SlotDanmakuCraftingA(this.craftWay, 0, 8, 35, 0));
        addSlot(new SlotDanmakuCraftingA(this.craftSpeed, 0, 8, 53, 0));
        addSlot(new SlotDanmakuCraftingA(this.craftCopy, 0, 192, 17, 1));
        addSlot(new SlotDanmakuCraftingA(this.craftColor, 0, 69, 17, 3));
        addSlot(new SlotDanmakuCraftingA(this.craftGravity, 0, 69, 35, 0));
        addSlot(new SlotDanmakuCraftingA(this.craftBound, 0, 69, 53, 4));

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
        return this.level.getBlockState(this.pos).is(THKaguyaBlocks.DANMAKU_CRAFTING_TABLE.get())
                && player.distanceToSqr(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    /** 中身に変更があったときに呼ばれる */
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
        byte gravity = 0;
        int special = 0;

        if (!shot.isEmpty() && shot.getTag() != null)
        {
            var nbt = shot.getTag();
            way = nbt.getShort("Number");
            form = nbt.getByte("DanmakuForm");
            addSpeed = nbt.getByte("Speed");
            special = nbt.getInt("Special");
            gravity = nbt.getByte("Gravity");
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
            if (way > THKaguyaConfig.shotMaxNumber || way < 0)
            {
                way = (short)THKaguyaConfig.shotMaxNumber;
            }
        }

        if (!craftGravity.getItem(0).isEmpty())
        {
            gravity += (byte)craftGravity.getItem(0).getCount();
            if (gravity > 16 || gravity < 0)
            {
                gravity = 16;
            }
        }

        if (!craftBound.getItem(0).isEmpty())
        {
            int num = craftBound.getItem(0).getCount();
            if (special >= THShotLib.BOUND01 && special <= THShotLib.BOUND01 + 3)
            {
                special = special + num;
            }
            else
            {
                special = THShotLib.BOUND01;
                num--;
                special += num;
            }
            if (special > THShotLib.BOUND01 + 3)
            {
                special = THShotLib.BOUND01 + 3;
            }
        }

        // 形状判定（3×3 マトリクス）
        boolean[] m = new boolean[9];
        for (int i = 0; i < 9; i++)
        {
            m[i] = !craftMatrix.getItem(i).isEmpty();
        }
        form = detectForm(m, way, form);
        if (!craftColor.getItem(0).isEmpty())
        {
            ItemStack colorStack = craftColor.getItem(0);
            int dyeColor = dyeColorIndex(colorStack);
            if (dyeColor >= 0)
            {
                color = (byte)dyeColor;
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

        ItemStack newShot;
        if (!shot.isEmpty() && !(form == 4 && way > 32) && form >= 0)
        {
            newShot = new ItemStack(THKaguyaItems.shotItem(THKaguyaItems.shotTypeOf(craftMaterial.getItem(0))));

            var nbt2 = newShot.getOrCreateTag();
            nbt2.putShort("Number", way);
            nbt2.putByte("DanmakuForm", form);
            nbt2.putByte("Speed", addSpeed);
            nbt2.putByte("Color", color);
            nbt2.putInt("Special", special);
            nbt2.putByte("Gravity", gravity);

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

    /**
     * 1.20.1 的 16 种染料物品 → 1.7.10 `Items.dye` 的 metadata（0=墨囊 … 15=骨粉）。
     * 原版用 `getItemDamage() % 16` 查 {@link DanmakuConstants#SELECTABLE_COLOR}，1.20.1 染料拆成 16 个物品、
     * damage 恒 0，若继续读 damage 会让「红染料」也变成白色（索引 0 → 7=白）。
     */
    private static final Item[] DYE_BY_LEGACY_META =
    {
            Items.BLACK_DYE, Items.RED_DYE, Items.GREEN_DYE, Items.BROWN_DYE,
            Items.BLUE_DYE, Items.PURPLE_DYE, Items.CYAN_DYE, Items.LIGHT_GRAY_DYE,
            Items.GRAY_DYE, Items.PINK_DYE, Items.LIME_DYE, Items.YELLOW_DYE,
            Items.LIGHT_BLUE_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.WHITE_DYE
    };

    /** 染料物品 → 弹幕调色板索引（0-7，见 {@link DanmakuConstants#SELECTABLE_COLOR}）；不是染料返回 -1。 */
    public static int dyeColorIndex(ItemStack stack)
    {
        for (int i = 0; i < DYE_BY_LEGACY_META.length; i++)
        {
            if (stack.getItem() == DYE_BY_LEGACY_META[i])
            {
                return DanmakuConstants.SELECTABLE_COLOR[i];
            }
        }
        return -1;
    }

    /** 形状判定：返回 form（0=一点,1=前方随机,2=扇状,3=全方位,4=球状,5=环形,-1=无效）。激光工作台复用。 */
    static byte detectForm(boolean[] m, short way, byte originalForm)
    {
        if (!m[0] && !m[1] && !m[2] && !m[3] && m[4] && !m[5] && !m[6] && !m[7] && !m[8]) return 0;
        if (!m[0] && m[1] && !m[2] && !m[3] && m[4] && !m[5] && !m[6] && !m[7] && !m[8]) return 1;
        if (m[0] && m[1] && m[2] && !m[3] && !m[4] && !m[5] && !m[6] && !m[7] && !m[8]) return 2;
        if (m[0] && m[1] && m[2] && m[3] && !m[4] && m[5] && m[6] && m[7] && m[8]) return 3;
        if (m[0] && m[1] && m[2] && m[3] && m[4] && m[5] && m[6] && m[7] && m[8])
        {
            if (way > 2) return 4;
        }
        if (!m[0] && m[1] && !m[2] && m[3] && !m[4] && m[5] && !m[6] && m[7] && !m[8])
        {
            if (way > 2) return 5;
        }
        if (!m[0] && !m[1] && !m[2] && !m[3] && !m[4] && !m[5] && !m[6] && !m[7] && !m[8])
        {
            return originalForm; // 変化なし
        }
        return -1;
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
                // コンテナスロット → プレイヤーインベントリ
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true))
                {
                    return ItemStack.EMPTY;
                }
                // 原版 1.7.10 の transferStackInSlot は最後に必ず
                // slot.onPickupFromSlot(player, itemstack1) を呼び、そこで材料を消費していた
                // （原版 ContainerDanmakuCrafting.java:485）。
                // 1.20.1 の AbstractContainerMenu#moveItemStackTo は onTake を呼ばないため、
                // ここで明示的に呼ばないと **Shift+クリックで材料を消費せず増殖できる**。
                if (slot instanceof SlotDanmakuResult)
                {
                    slot.onTake(player, stack);
                }
            }
            else
            {
                // プレイヤーインベントリ → コンテナスロット（結果スロットは mayPlace=false）
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
            clearContainer(player, this.craftGravity);
            clearContainer(player, this.craftBound);
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
