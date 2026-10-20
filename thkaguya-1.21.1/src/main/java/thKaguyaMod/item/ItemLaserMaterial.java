package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 激光の素（激光素材）
 *
 * 原版 1.7.10 にはこの物品が存在しない（素材は {@code light_shot} 等の弾アイテムだけ）。
 * 移植版で「激光工作台の素材」として追加した物品なので、见た目を {@code shot_material}
 * と揃えるため右クリック試射だけ 1.20.1 側で補った。
 *
 * 種類（middle / short / long）は damage 値で表す（{@code THKaguyaItems.laserTypeOf} と同ルール）。
 */
public class ItemLaserMaterial extends ItemTHLaser
{
	public ItemLaserMaterial()
	{
		super(ItemTHLaser.MIDDLE);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		if(!level.isClientSide)
		{
			int type = Math.floorMod(itemStack.getDamageValue(), ItemTHLaser.SPEED.length);
			shootLaser(itemStack, player, type);
			thKaguyaMod.THShotLib.playShotSound(player);

			if(!player.isCreative())
			{
				itemStack.shrink(1);
			}
		}

		return InteractionResultHolder.success(itemStack);
	}
}
