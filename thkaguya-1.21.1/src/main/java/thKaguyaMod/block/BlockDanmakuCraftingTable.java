package thKaguyaMod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thKaguyaMod.gui.ContainerDanmakuCrafting;
import thKaguyaMod.gui.ContainerDanmakuCraftingLaser;

/**
 * 弾幕作業台ブロック（1.20.1 移植版，阶段 5）
 * 原版 1.7.10 用 metadata 区分弹/激光两种工作台；1.20.1 拆成两个方块，
 * 分别经 use() 打开对应的 MenuType。
 */
public class BlockDanmakuCraftingTable extends Block
{
    private final boolean laser;

    public BlockDanmakuCraftingTable(Properties properties, boolean laser)
    {
        super(properties);
        this.laser = laser;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit)
    {
        if (level.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        MenuProvider provider;
        if (laser)
        {
            provider = new SimpleMenuProvider(
                    (windowId, inv, p) -> new ContainerDanmakuCraftingLaser(windowId, inv, pos),
                    Component.translatable("danmakuCraftingLaser.name"));
        }
        else
        {
            provider = new SimpleMenuProvider(
                    (windowId, inv, p) -> new ContainerDanmakuCrafting(windowId, inv, pos),
                    Component.translatable("danmakuCrafting.name"));
        }

        ((ServerPlayer)player).openMenu(provider, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
