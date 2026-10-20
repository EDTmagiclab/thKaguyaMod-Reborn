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
 * 幽々子の扇（幽々子的扇子）
 *
 * 原版 ItemYuyukoFan：
 * - 右クリックで「使用開始」（ガードアクション、最大 72000 tick の長押し）
 * - 離した瞬間、押していた長さに応じて蝶弾を撃つ（最低 32 発 / 最大 72 発、3 の倍数に丸め）
 *     通常時：扇 90 度を 2 方向（baseAngle ±120 度）に広げて発射
 *     スニーク時：弾速違いの 3 セットを全方位（±12 度ずらしの全方位円）に発射
 * - 使用ごとに満腹度を 1.5 消費（クリエイティブを除く）
 * - 命中時：体力 20 未満かつ被弾直後（hurtTime > 0）の相手に対し 5% で
 *   その相手の残り体力を吸収しつつ 4444 ダメージ（即死級）
 *
 * 1.20.1 迁移要点：
 * - onPlayerStoppedUsing(usedTime) → releaseUsing(timeLeft)：shotNum の式は
 *   32 + (duration - usedTime) = 32 + timeLeft
 * - addExhaustion → causeFoodExhaustion、hasEffect → isFoil
 * - world.playSoundAtEntity("random.orb") → SoundEvents.EXPERIENCE_ORB_PICKUP
 * - "mob.zombie.unfect" → SoundEvents.ZOMBIE_VILLAGER_CURE
 */
public class ItemYuyukoFan extends Item
{
	public ItemYuyukoFan()
	{
		super(new Item.Properties().stacksTo(1).durability(444));
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

		// 原版 shotNum = 32 + (getMaxItemUseDuration - usedTime)（最低 32 / 最大 72 / 3 の倍数）
		// 1.7.10 の onPlayerStoppedUsing の第 4 引数は 1.20.1 の timeLeft と同じ「残り tick」なので
		// 「押していた時間」= getUseDuration - timeLeft。押すほど弾数が増える。
		int shotNum = 32 + (getUseDuration(itemStack, player) - timeLeft);
		if(shotNum > 72)
		{
			shotNum = 72;
		}
		else if(shotNum % 3 != 0)
		{
			shotNum = (shotNum / 3) * 3;
		}
		int shotNum3 = shotNum / 3;

		float size = DanmakuConstants.SIZE[DanmakuConstants.FORM_BUTTERFLY];

		if(player.isShiftKeyDown())
		{
			// 全方位：弾速違いの 3 セットを ±12 度ずらして放つ
			int color = DanmakuConstants.BLUE + level.random.nextInt(4);
			int color2 = color + 1;
			if(color2 >= DanmakuConstants.BLUE + 4)
			{
				color2 = DanmakuConstants.BLUE;
			}
			for(int i = 0; i < 3; i++)
			{
				int color3 = (i == 1) ? color2 : color;
				double speed = 0.1D + (i * 0.1D);
				ShotData shot = ShotData.shot(DanmakuConstants.FORM_BUTTERFLY, color3, size, 4.0F, 1, 80);
				THShotLib.createCircleShot(player, player, THShotLib.pos_Living(player),
						player.getLookAngle(), 12F, 10, speed, speed, 0.0D,
						THShotLib.gravity_Zero(), shot, shotNum3, 0.5D, 0F);
				THShotLib.createCircleShot(player, player, THShotLib.pos_Living(player),
						player.getLookAngle(), -12F, 10, speed, speed, 0.0D,
						THShotLib.gravity_Zero(), shot, shotNum3, 0.5D, 0F);
			}
		}
		else
		{
			int color = DanmakuConstants.BLUE + level.random.nextInt(4);
			int color2 = color + 1;
			if(color2 >= DanmakuConstants.BLUE + 4)
			{
				color2 = DanmakuConstants.BLUE;
			}
			Vec3 look = THShotLib.angle(player.getYRot(), player.getXRot());
			Vec3 pos = THShotLib.pos(
					player.getX() + look.x * 0.5D,
					THShotLib.getPosYFromEye(player) + look.y * 0.5D,
					player.getZ() + look.z * 0.5D);

			THShotLib.createWideShot(player, player, pos, look, 14.4F, 8,
					0.0D, 0.45D, 0.02D, THShotLib.gravity_Zero(),
					ShotData.shot(DanmakuConstants.FORM_BUTTERFLY, color, size, 4.0F, 1, 80),
					shotNum3 + 1, 90F, 0.2D, -120F);
			THShotLib.createWideShot(player, player, pos, look, -14.4F, 8,
					0.0D, 0.45D, 0.02D, THShotLib.gravity_Zero(),
					ShotData.shot(DanmakuConstants.FORM_BUTTERFLY, color2, size, 4.0F, 1, 80),
					shotNum3 + 1, 90F, 0.2D, 120F);
		}

		if(!player.isCreative())
		{
			// 使うたびに少し腹が減る
			player.causeFoodExhaustion(1.5F);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS, 0.5F, 1.0F);
		itemStack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
	}

	/** 命中時：5% で相手の残り体力を吸収し 4444 ダメージを与える */
	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker)
	{
		if(target.getHealth() > 0.0F && target.getHealth() < 20.0F && target.hurtTime > 0)
		{
			if(attacker.level().random.nextInt(100) < 5)
			{
				attacker.heal(target.getHealth());
				target.hurt(attacker.level().damageSources().mobAttack(attacker), 4444.0F);
				attacker.level().playSound(null, attacker.blockPosition(),
						SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.0F, 40.0F);
			}
		}
		itemStack.hurtAndBreak(1, attacker, net.minecraft.world.entity.LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
		return true;
	}

	@Override
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
	{
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BLOCK;
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
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
