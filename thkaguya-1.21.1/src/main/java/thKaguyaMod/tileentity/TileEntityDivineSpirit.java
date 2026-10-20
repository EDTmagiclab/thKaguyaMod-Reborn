package thKaguyaMod.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thKaguyaMod.init.THKaguyaBlocks;

/**
 * 小神霊 TileEntity（1.20.1 移植版）
 * 原版 1.7.10 的 TileEntity 所有方法体均被作者注释（time 计数、NBT、packet 全部禁用），
 * 因此 1.20.1 仅保留空实现占位，功能与原版一致（无行为）。
 */
public class TileEntityDivineSpirit extends BlockEntity
{
    public TileEntityDivineSpirit(BlockPos pos, BlockState state)
    {
        super(THKaguyaBlocks.DIVINE_SPIRIT_ENTITY.get(), pos, state);
    }
}
