package thKaguyaMod.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 御柱
 *
 * 原版 ItemOnbashira extends ItemSword：
 * - とてつもなく強力な武器（命中時に 5 + rand(18) の追加ダメージ → 計 9〜26）
 * - しかし重く、所持しているだけで移動速度が 0.9 倍になる（上昇中は落下も 0.9 倍）
 * - ブロック破壊速度は 3.0 固定
 *
 * 1.20.1 迁移要点：
 * - ItemSword → SwordItem；ToolMaterial.WOOD → Tiers.WOOD（耐久 59・攻撃力ボーナス 0 が原版一致）
 *   ※ 原版 onbashira は maxUses=59 / damageVsEntity=0 の WOOD 素材なので Tiers.WOOD が等価
 * - getDigSpeed → getDestroySpeed
 * - hitEntity → hurtEnemy（base 互換のため super を呼び、その上で追加ダメージ）
 * - onUpdate → inventoryTick
 * - motionX/Y/Z → getDeltaMovement() / setDeltaMovement()
 */
public class ItemOnbashira extends SwordItem
{
	public ItemOnbashira()
	{
		super(Tiers.WOOD, 3, -2.4F, new Item.Properties().stacksTo(1));
	}

	/** ブロックに対する強さ（原版 getDigSpeed = 3F 固定） */
	@Override
	public float getDestroySpeed(ItemStack itemStack, BlockState state)
	{
		return 3.0F;
	}

	/** Entity に当たったときの処理：追加ダメージ 5 + rand(18)（原版 DamageSource.causeMobDamage） */
	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker)
	{
		boolean result = super.hurtEnemy(itemStack, target, attacker);
		target.hurt(target.level().damageSources().mobAttack(attacker),
				5 + target.level().random.nextInt(18));
		return result;
	}

	/** 所持している限り常時呼び出される：移動速度が 0.9 倍になる */
	@Override
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slot, boolean selected)
	{
		Vec3 motion = entity.getDeltaMovement();
		double motionY = motion.y;
		if(motionY > 0.0D)
		{
			motionY *= 0.9D;
		}
		entity.setDeltaMovement(motion.x * 0.9D, motionY, motion.z * 0.9D);
	}
}
