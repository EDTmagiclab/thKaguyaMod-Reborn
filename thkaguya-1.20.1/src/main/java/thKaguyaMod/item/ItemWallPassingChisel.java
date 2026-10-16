package thKaguyaMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 壁抜けの鑿（穿墙凿）
 *
 * 原版 ItemKabenuke：
 * - 对墙面右クリック（onItemUseFirst）で、その方向 16 ブロックまで「2 格の空気」を探し、
 *   見つかればそこへ瞬間移動する（岩盤に当たったら失敗）
 * - 移動ごとに耐久 -1。THKaguyaConfig.canUseWallPassingChisel で禁止可能
 *
 * 1.20.1 迁移要点：
 * - onItemUseFirst(ItemStack, EntityPlayer, World, x,y,z, side, hx,hy,hz)
 *   → Forge の onItemUseFirst(ItemStack, UseOnContext)
 * - 原版 sideHit の 0-5 → Direction（DOWN/UP/NORTH/SOUTH/WEST/EAST）
 * - world.isAirBlock → level.isEmptyBlock、Blocks.bedrock → Blocks.BEDROCK
 * - 原版は posY = blockY + yOffset（1.7.10 の yOffset は眼球高）→ 1.20.1 は
 *   teleportTo の Y がそのまま「足元の Y」なので blockY を渡す
 * - playerMP.playerNetServerHandler.setPlayerLocation → player.teleportTo（同期は自動）
 */
public class ItemWallPassingChisel extends Item
{
	public ItemWallPassingChisel()
	{
		super(new Item.Properties().stacksTo(1).durability(122));
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context)
	{
		// コンフィグで使用制限をかけているなら使えない
		if(!THKaguyaConfig.canUseWallPassingChisel)
		{
			return InteractionResult.PASS;
		}

		Player player = context.getPlayer();
		if(player == null)
		{
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();
		Direction face = context.getClickedFace();

		int px = 0;
		int py = 0;
		int pz = 0;
		switch(face)
		{
			case DOWN:
				py = 1;
				break;
			case UP:
				py = -1;
				break;
			case NORTH:
				pz = 1;
				break;
			case SOUTH:
				pz = -1;
				break;
			case WEST:
				px = 1;
				break;
			default:
				px = -1;
				break;
		}

		if(!level.isClientSide)
		{
			BlockPos pos = context.getClickedPos();
			// 最大 16 ブロックまで抜けられる
			for(int i = 0; i < 16; i++)
			{
				if(level.isEmptyBlock(pos))
				{
					if(level.isEmptyBlock(pos.above()))
					{
						level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
								SoundSource.PLAYERS, 0.5F, 1.0F);
						player.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
						player.setDeltaMovement(0.0D, 0.0D, 0.0D);
						itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(context.getHand()));
						return InteractionResult.PASS;
					}
					else if(level.isEmptyBlock(pos.below()))
					{
						level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
								SoundSource.PLAYERS, 0.5F, 1.0F);
						player.teleportTo(pos.getX() + 0.5D, pos.getY() - 1.0D, pos.getZ() + 0.5D);
						player.setDeltaMovement(0.0D, 0.0D, 0.0D);
						itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(context.getHand()));
						return InteractionResult.PASS;
					}
				}

				// 岩盤があったら失敗する（岩盤は壁抜け不可）
				if(level.getBlockState(pos).is(Blocks.BEDROCK))
				{
					return InteractionResult.PASS;
				}

				pos = pos.offset(px, py, pz);
			}
		}

		return InteractionResult.PASS;
	}
}
