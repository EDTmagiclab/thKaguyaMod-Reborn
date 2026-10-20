package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/**
 * 弾幕の素（弹幕素材）
 *
 * 原版 ItemShotMaterial の移植。右クリックで極小の白弾を 1 発試射し、1 個消費する
 * （原版はノーダメージの確認用弾。THShotLib.createShot(user, pos, angle, speed, shotData) を使用）。
 */
public class ItemShotMaterial extends Item
{
	public ItemShotMaterial()
	{
		super(new Item.Properties().stacksTo(64));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		if(!level.isClientSide)
		{
			THShotLib.createShot(player, THShotLib.pos_Living(player), player.getLookAngle(), 1.0D,
					ShotData.shot(DanmakuConstants.FORM_TINY, DanmakuConstants.WHITE));
			THShotLib.playShotSound(player);

			if(!player.isCreative())
			{
				itemStack.shrink(1);
			}
		}

		return InteractionResultHolder.success(itemStack);
	}
}
