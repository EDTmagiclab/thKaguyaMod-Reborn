package thKaguyaMod.item;

import java.util.Optional;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 死神の大鎌（死神的大镰）
 *
 * 原版 ItemDeathScythe extends ItemSword：
 * - 右クリックで視線方向 16 格の線分上で最も近い生物を探し、
 *   その生物の motion を操作する（通常＝引き寄せ / スニーク＝押し出し、power 0.6）
 * - 操作した生物 1 体につき耐久 -1
 * - エンチャント適性 7
 *
 * 1.20.1 迁移要点：
 * - ItemSword → SwordItem（原版 ToolMaterial.IRON → Tiers.IRON）
 * - world.func_147447_a(from, to, false, true, true) → level.clip(ClipContext.Block.COLLIDER)
 * - AxisAlignedBB.calculateIntercept(from,to) → AABB.clip(from,to)（Optional&lt;Vec3&gt;）
 * - world.getEntitiesWithinAABBExcludingEntity(player, box) → level.getEntities(player, box)
 * - entity.motionX += → getDeltaMovement()/setDeltaMovement()
 * - 原版 setMaxDamage(444) は 1.20.1 では Tier の耐久（IRON=250）に上書きされる（既知の偏差）
 */
public class ItemDeathScythe extends SwordItem
{
	public ItemDeathScythe()
	{
		super(Tiers.IRON, 3, -2.4F, new Item.Properties().stacksTo(1).durability(444));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 有効距離
		double range = 16.0D;
		Vec3 look = player.getLookAngle();
		Vec3 from = player.getEyePosition(1.0F);
		Vec3 to = from.add(look.x * range, look.y * range, look.z * range);

		// 始点と終点からブロックとの衝突を取得（当たったなら終点をその点に変更）
		BlockHitResult blockHit = level.clip(new ClipContext(from, to,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if(blockHit.getType() != HitResult.Type.MISS)
		{
			to = blockHit.getLocation();
		}

		// 線分上で最も近い生物を探す
		Entity target = null;
		double closest = 0.0D;
		AABB searchBox = player.getBoundingBox()
				.expandTowards(look.x * range, look.y * range, look.z * range)
				.inflate(1.0D, 1.0D, 1.0D);

		for(Entity entity : level.getEntities(player, searchBox))
		{
			if(!(entity instanceof LivingEntity))
			{
				continue;
			}
			float f2 = 0.3F;
			Optional<Vec3> intercept = entity.getBoundingBox()
					.inflate(f2, f2, f2).clip(from, to);
			if(intercept.isPresent())
			{
				double d1 = from.distanceTo(intercept.get());
				if(d1 < closest || closest == 0.0D)
				{
					target = entity;
					closest = d1;
				}
			}
		}

		// Entity に当たっていたなら motion を操作する
		if(target != null && !level.isClientSide)
		{
			double power = 0.6D;
			Vec3 motion = target.getDeltaMovement();
			if(player.isShiftKeyDown())
			{
				// スニークなら斥力
				target.setDeltaMovement(motion.x + look.x * power,
						motion.y + look.y * power,
						motion.z + look.z * power);
			}
			else
			{
				// スニークじゃないなら引力
				target.setDeltaMovement(motion.x - look.x * power,
						motion.y - look.y * power,
						motion.z - look.z * power);
			}
			// 距離を操った生物の数だけ耐久が減る
			itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
		}

		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public int getEnchantmentValue()
	{
		return 7;
	}
}
