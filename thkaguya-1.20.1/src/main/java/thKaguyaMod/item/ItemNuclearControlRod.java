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
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/**
 * お空の制御棒（阿空的制御棒）
 *
 * 原版 ItemNuclearControlRod：
 * - 右クリックした瞬間に核弾を 1 発生成（チャージ開始）
 * - 長押しを離したとき、押していた長さ（最大 100 tick）に応じた反動で
 *   自分が後ろへノックバックし、爆発音を鳴らす
 * - 離すたびに耐久 -1
 *
 * 1.20.1 迁移要点：
 * - setItemInUse + getMaxItemUseDuration(72000) → startUsingItem + getUseDuration
 * - onPlayerStoppedUsing(usedTime) → releaseUsing(timeLeft)：
 *   原版 time = 72000 - usedTime は timeLeft と等価
 * - motionX -= → getDeltaMovement()/setDeltaMovement()
 * - playSoundEffect("random.explode") → SoundEvents.GENERIC_EXPLODE
 * - THShotLib.createNuclearShot を本バッチで移植（下記）
 */
public class ItemNuclearControlRod extends Item
{
	public ItemNuclearControlRod()
	{
		super(new Item.Properties().stacksTo(1).durability(36));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);

		Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);
		Vec3 vec3_2 = THShotLib.getVecFromAngle(player.getYRot() + 20F, player.getXRot(),
				0.3D + 0.3D / 2.0D);

		ShotData shot = ShotData.shot(0, DanmakuConstants.ORANGE, 0.3F, 10.0F, 0, 70);
		THShotLib.createNuclearShot(player, player,
				THShotLib.pos(player.getX() + vec3_2.x,
						THShotLib.getPosYFromEye(player, -0.1D) + vec3_2.y,
						player.getZ() + vec3_2.z),
				vec3, 0F, THShotLib.rotate_Default(), 0F, 9999,
				0.0D, 0.8D, 0.0D, THShotLib.gravity_Zero(), shot);

		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		// 原版 time = getMaxItemUseDuration - usedTime。
		// 1.20.1 の timeLeft は「残り」なので、逆に引いて使った tick 数を求める
		// （移植版は timeLeft をそのまま使っていたため、押した瞬間に最大反動が出ていた）。
		int time = this.getUseDuration(itemStack) - timeLeft;
		if(time > 100)
		{
			time = 100;
		}

		// 爆発音
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F,
				(1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

		// 反動処理
		Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), time / 25.0D);
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x - vec3.x, motion.y - vec3.y, motion.z - vec3.z);

		// 耐久を 1 減らす
		itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BOW;
	}

	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	@Override
	public boolean isEnchantable(ItemStack stack)
	{
		return false;
	}
}
