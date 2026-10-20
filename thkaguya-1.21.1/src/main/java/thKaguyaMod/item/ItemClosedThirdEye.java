package thKaguyaMod.item;

import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * こいしの閉ざした第三の目（闭上的第三只眼）
 *
 * 原版 ItemClosedThirdEye：
 * - 右クリックで 1 秒間の透明化（PotionEffect 14 = invisibility, 20 tick, level 0）
 * - 周囲 30 格の生物のうち「壁で遮られている」か「8 格より遠い」ものについて、
 *   自分を攻撃対象にしていたら対象から外す（敵意をリセット）
 *
 * 1.20.1 迁移要点：
 * - EntityLiving → Mob（getAttackTarget/setAttackTarget → getTarget/setTarget）
 * - canEntityBeSeen(player) → hasLineOfSight(player)（LivingEntity、视线被方块遮挡判定）
 * - getDistanceToEntity → distanceTo
 * - world.getEntitiesWithinAABBExcludingEntity → level.getEntities(除外实体, AABB)
 */
public class ItemClosedThirdEye extends Item
{
	public ItemClosedThirdEye()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 敵に気づかれなくなる（原版 PotionEffect(14 = invisibility, 20 tick, level 0)）
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0));

		// プレイヤーを中心に 30 ブロックの立方体内の実体を取得
		List<Entity> list = level.getEntities(player,
				player.getBoundingBox().inflate(30.0D, 30.0D, 30.0D));

		for(Entity entity : list)
		{
			if(entity instanceof Mob living)
			{
				// 間に壁があるか、ある程度の距離があれば有効
				if(!living.hasLineOfSight(player) || living.distanceTo(player) > 8.0D)
				{
					if(living.getTarget() == player)
					{
						// 使用者をターゲットから外す
						living.setTarget(null);
					}
				}
			}
		}

		return InteractionResultHolder.success(itemStack);
	}
}
