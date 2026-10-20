package thKaguyaMod.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thKaguyaMod.block.BlockDivineSpirit;

import javax.annotation.Nullable;

/**
 * 小神霊ブロックのアイテム（1.20.1 移植版）
 *
 * 原版 1.7.10 は {@code ItemBlockDivineSpirit} が 1 アイテム + 8 metadata で、
 * metadata がそのまま BlockState（当時は metadata）に写っていた。
 * 1.20.1 は metadata が無いので「色ごとに独立アイテム」にし（銀ナイフ 4 色と同じ方針）、
 * 設置時に {@link BlockDivineSpirit#COLOR} を自分の色で上書きする。
 */
public class ItemBlockDivineSpirit extends BlockItem
{
    private final DyeColor color;

    public ItemBlockDivineSpirit(Block block, DyeColor color, Item.Properties properties)
    {
        super(block, properties);
        this.color = color;
    }

    /** 原版 metadata 相当（色番号 0..7 は {@link BlockDivineSpirit#colorIndex}） */
    public DyeColor getDivineSpiritColor()
    {
        return color;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context)
    {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(BlockDivineSpirit.COLOR, color);
    }
}
