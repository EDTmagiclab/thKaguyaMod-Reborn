package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/**
 * 天狗の団扇（天狗的团扇）
 *
 * 原版 ItemTenguFan：
 * - 右クリックで「使用開始」（弓アクション、24 tick）
 * - 離した瞬間に FORM_WIND の風弾を飛ばす。速度は引きの長さに比例
 *   （speed = (getMaxItemUseDuration - usedTime) / 7.0 = 引きの長さ / 7.0）
 * - special = WIND01：命中時に相手を弾き飛ばす（EntityTHShot 側で処理）
 *
 * 1.20.1 迁移要点：
 * - onItemRightClick + setItemInUse → use() + startUsingItem()
 * - onPlayerStoppedUsing(usedTime) → releaseUsing(timeLeft)
 *   1.7.10 の usedTime も 1.20.1 の timeLeft も「残り tick」なので、
 *   原版の (24 - usedTime) は (24 - timeLeft) = 引きの長さ と等価
 * - swingItem() → swing(InteractionHand)
 * - world.playSoundAtEntity("random.bow") → level.playSound(SoundEvents.ARROW_SHOOT)
 * - isFull3D() は 1.20.1 で廃止（モデル JSON の display が担当）
 */
public class ItemTenguFan extends Item
{
	public ItemTenguFan()
	{
		super(new Item.Properties().stacksTo(1).durability(40));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		ShotData shot = ShotData.shot(DanmakuConstants.FORM_WIND, DanmakuConstants.AQUA,
				0, 20, DanmakuConstants.WIND01);
		// 原版 speed = (getMaxItemUseDuration - usedTime) / 7.0D = 引きの長さ / 7.0D
		double speed = (double)(getUseDuration(itemStack, player) - timeLeft) / 7.0D;
		THShotLib.createShot(player, THShotLib.pos_Living(player),
				THShotLib.angle(player.getYRot(), player.getXRot()), speed, shot);
		player.swing(InteractionHand.MAIN_HAND);
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.random.nextFloat() * 4F + 0.8F));
		itemStack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
	}

	@Override
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
	{
		return 24;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BOW;
	}
}
