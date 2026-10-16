package thKaguyaMod.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.entity.effect.EntityAjaRedStoneEffect;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

/**
 * エイジャの赤石（艾哲红石）
 *
 * 原版 ItemAjaRedStone implements ISpecialShot：
 * - 右クリックで使用開始（ガードアクション）。同時に「光を溜める」
 *   EntityAjaRedStoneEffect を出現させる
 * - 溜めている間（isUsingItem）は光量が増え続け、離した瞬間にその光量に応じた
 *   アンデッド特効レーザーを発射する
 * - 特殊弾（SPECIAL_AJA_REDSTONE）は命中相手がアンデッドなら
 *   延焼させ、damageRate を 2 倍にする（＝通常の 2 倍ダメージ）
 * - 常に発光（hasEffect）
 *
 * 1.20.1 迁移要点：
 * - setItemInUse + getMaxItemUseDuration(72000) → startUsingItem + getUseDuration
 * - EnumCreatureAttribute.UNDEAD → MobType.UNDEAD（LivingEntity.getMobType）
 * - setFire(t) → setSecondsOnFire(t)、world.playSoundEffect → level.playSound
 * - spawnParticle("largesmoke") → addParticle(ParticleTypes.LARGE_SMOKE)（クライアントのみ）
 * - 特殊弾 ID は DanmakuConstants.SPECIAL_AJA_REDSTONE（351）と同値
 */
public class ItemAjaRedStone extends Item implements ISpecialShot
{
	public ItemAjaRedStone()
	{
		super(new Item.Properties().stacksTo(1));
		SpecialShotRegistry.registerSpecialShot(ItemAjaRedStone.class, DanmakuConstants.SPECIAL_AJA_REDSTONE);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// アイテムの使用継続時間を記憶させる
		player.startUsingItem(hand);

		// 光吸収エフェクトを出現させる
		EntityAjaRedStoneEffect ajaRedStoneEffect = new EntityAjaRedStoneEffect(level, player);
		if(!level.isClientSide)
		{
			level.addFreshEntity(ajaRedStoneEffect);
		}

		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
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

	// ===== ISpecialShot =====

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot)
	{
	}

	@Override
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot, HitResult result)
	{
		return false;
	}

	@Override
	public boolean specialShot_hitEntity(Level world, int id, EntityTHShot shot, Entity entityHit)
	{
		// 指定したダメージ分の魔法ダメージを与える
		if(entityHit instanceof LivingEntity living)
		{
			if(living.getMobType() == MobType.UNDEAD)
			{
				// 通常の 2 倍のダメージを与える
				living.setSecondsOnFire((int)shot.shotDamage);
				shot.damageRate *= 2.0F;
				world.playSound(null, shot.getX(), shot.getY(), shot.getZ(),
						SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F,
						2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
				if(world.isClientSide)
				{
					for(int l = 0; l < 8; l++)
					{
						world.addParticle(ParticleTypes.LARGE_SMOKE,
								shot.getX() + world.random.nextDouble(),
								shot.getY() + world.random.nextDouble(),
								shot.getZ() + world.random.nextDouble(),
								0.0D, 0.0D, 0.0D);
					}
				}
			}
		}
		return false;
	}
}
