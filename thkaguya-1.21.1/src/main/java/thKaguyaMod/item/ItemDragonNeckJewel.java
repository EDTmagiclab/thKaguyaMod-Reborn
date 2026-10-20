package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityDragonNeckJewel;

/**
 * 龍の頸の玉（龙颈之玉）
 *
 * 原版 ItemDragonNeckJewel：
 * - 通常の右クリック：5 色のうちランダム 1 色の龍の頸の玉を前方へ投げる。
 *   ただし「残りチャージ」が 70/300 より大きい（damage &lt; 229）ときのみ。耐久 -70
 * - スニーク + 右クリック：5 色同時に扇状（-40 度から 20 度刻み）に投げる。
 *   満タン（damage &lt; 1）のときのみ。耐久を最大まで消費（-299）
 * - インベントリにある限り 20 tick ごとにチャージが 20 ずつ自然回復する
 * - 常に発光（hasEffect）
 *
 * 1.20.1 迁移要点：
 * - onUpdate → inventoryTick、ticksExisted → tickCount
 * - damageItem(-20, entity)（負数で回復）→ setDamageValue(max(0, damage - 20))
 * - world.playSoundAtEntity("random.orb") → SoundEvents.EXPERIENCE_ORB_PICKUP
 */
public class ItemDragonNeckJewel extends Item
{
	/** 5 色の弾色（原版 r/g/b テーブル） */
	private static final float[] COLOR_R = {1.0F, 0.1F, 0.1F, 1.0F, 1.0F};
	private static final float[] COLOR_G = {0.1F, 1.0F, 0.1F, 1.0F, 0.1F};
	private static final float[] COLOR_B = {0.1F, 0.1F, 1.0F, 0.1F, 1.0F};

	public ItemDragonNeckJewel()
	{
		super(new Item.Properties().stacksTo(1).durability(300));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		if(!player.isShiftKeyDown() && itemStack.getDamageValue() < 229)
		{
			// チャージが 70/300 より大きいなら 1 発投げる
			Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);
			int rand = level.random.nextInt(5);
			EntityDragonNeckJewel jewel = new EntityDragonNeckJewel(level, player, player,
					THShotLib.pos(player.getX(), THShotLib.getPosYFromEye(player, -0.2D), player.getZ()),
					vec3, COLOR_R[rand], COLOR_G[rand], COLOR_B[rand]);

			level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
					SoundSource.PLAYERS, 0.5F, 0.4F / (level.random.nextFloat() * 4F + 0.8F));
			if(!level.isClientSide)
			{
				level.addFreshEntity(jewel);
			}

			// 耐久を 70 減らす
			itemStack.hurtAndBreak(70, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
		}
		else if(player.isShiftKeyDown() && itemStack.getDamageValue() < 1)
		{
			// 満タンなら 5 色同時発射
			float angle = -40F;
			for(int i = 0; i < 5; i++)
			{
				Vec3 vec3 = THShotLib.getRotationVectorFromAngle(player.getYRot(), player.getXRot(),
						angle, 1.0D);
				EntityDragonNeckJewel jewel = new EntityDragonNeckJewel(level, player, player,
						THShotLib.pos(player.getX(), THShotLib.getPosYFromEye(player, -0.2D), player.getZ()),
						vec3, COLOR_R[i], COLOR_G[i], COLOR_B[i]);
				if(!level.isClientSide)
				{
					level.addFreshEntity(jewel);
				}
				angle += 20F;
			}
			level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
					SoundSource.PLAYERS, 0.5F, 0.4F / (level.random.nextFloat() * 4F + 0.8F));

			// 耐久を最大まで減らす
			itemStack.hurtAndBreak(299, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
		}

		return InteractionResultHolder.success(itemStack);
	}

	/** インベントリにある限り 20 tick ごとにチャージが 20 回復する */
	@Override
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slot, boolean selected)
	{
		if(entity.tickCount % 20 == 0 && itemStack.getDamageValue() > 0)
		{
			itemStack.setDamageValue(Math.max(0, itemStack.getDamageValue() - 20));
		}
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}
}
