package thKaguyaMod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 楼観旋風刃（楼观旋风刃）
 *
 * 原版 ItemRoukanSenpuuzin extends ItemRoukanken：
 * - 楼観剣と同じアンデッド特攻・耐久・破壊速度を持つ
 * - 常時発動：移動量に関係なく、移動ベクトル+1.5 格以内の Entity を自動で斬り続ける
 *     （原版の弾斬りブロックと耐久減少はコメントアウトされているため、
 *       旋風刃は弾を斬らず、斬撃で耐久も減らない）
 * - 斬った相手が hurtTime > 0 か既に瀕死なら、その場で判定を打ち切る
 * - 常に発光（hasEffect → isFoil）
 *
 * 1.7.10 -> 1.20.1 は ItemRoukanken と同じ（onUpdate → inventoryTick のみの差分）。
 */
public class ItemRoukanSenpuuzin extends ItemRoukanken
{
	public ItemRoukanSenpuuzin()
	{
		super();
	}

	/** 所持している限り常に呼ばれる：近くの相手を勝手に切り裂く */
	@Override
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slot, boolean selected)
	{
		if(!(entity instanceof Player player))
		{
			return;
		}

		// 手に持っているものがこの旋風刃でなければ何もしない
		if(player.getMainHandItem() != itemStack
				|| player.getMainHandItem().getItem() != THKaguyaItems.ROUKAN_SENPUUZIN.get())
		{
			return;
		}

		Vec3 motion = player.getDeltaMovement();

		// 指定範囲（1.5 格）内の Entity のリストを取得
		List<Entity> list = level.getEntities(player,
				player.getBoundingBox().expandTowards(motion).inflate(1.5D), e -> true);

		for(Entity target : list)
		{
			if(!target.isPickable())
			{
				continue;
			}

			if(target instanceof LivingEntity living)
			{
				// 既に斬られた直後、あるいは瀕死なら判定を打ち切る
				if(living.hurtTime > 0 || living.getHealth() <= 0.0F)
				{
					break;
				}

				if(player.hasLineOfSight(living))
				{
					if(living.getType().is(EntityTypeTags.UNDEAD))
					{
						living.hurt(level.damageSources().mobAttack(player), 16.0F);
						player.magicCrit(living);
					}
					else
					{
						living.hurt(level.damageSources().mobAttack(player), 8F);
					}

					level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
							SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F,
							0.4F / (level.random.nextFloat() * 4F + 0.8F));
				}
			}

			// 投げる動作をさせる（原版同様、耐久は減らない）
			player.swing(InteractionHand.MAIN_HAND);

			if(itemStack.isEmpty())
			{
				break;
			}
		}
	}

	/** 常に発光させる */
	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}
}
