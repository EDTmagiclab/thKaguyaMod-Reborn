package thKaguyaMod.item;

import thKaguyaMod.THStackData;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 血に餓えた陰陽玉（bloodthirsty_yin_yang_orb，嗜血阴阳玉）—— 原版 {@code thKaguyaMod.item.ItemBloodthirstyOnmyoudama}（190 行）
 *
 * 原版行为：
 * - 构造：`maxStackSize=1`、`setMaxDamage(150)`、常时发光（`hasEffect` 恒 true）
 * - 右键按住（弓动作，72000 tick 上限），松手时沿视线 64 格找「最近的生物」，
 *   把玩家瞬移到目标身前 2 格（Y 取整 +1 再 +0.1，原版如此），没有伤害
 * - 每次使用消耗 1 点耐久；耐久耗尽时变回普通 `yin_yang_orb`，
 *   并还原 NBT `OriginalDamage`（由万宝槌 GUI 的 `ContainerMiracleMallet` 写入，`ContainerMiracleMallet.java:90`）
 *
 * 原版缺陷与移植处理：
 * - 原版用 `new MovingObjectPosition(entity)` 后读 `hitVec.xCoord`，而该构造器不设 `hitVec`，
 *   服务端实际会 NPE（所以这个物品在原版几乎不能用）。移植版改用目标实体位置计算方向。
 * - 原版对实体做 `AxisAlignedBB.calculateIntercept`，对应 1.20.1 `AABB#clip(Vec3, Vec3)`。
 */
public class ItemBloodthirstyOnmyoudama extends Item
{
	/** 原版 setMaxDamage(150) */
	public static final int MAX_DAMAGE = 150;
	/** 视线长度（格） */
	public static final double REACH = 64.0D;
	/** 落点与目标保持的距离（格） */
	public static final double STAND_OFF = 2.0D;

	public ItemBloodthirstyOnmyoudama()
	{
		super(new Item.Properties().stacksTo(1).durability(MAX_DAMAGE));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		// 原版 player.setItemInUse(itemStack, getMaxItemUseDuration(itemStack))
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		// 原版：物体検索は両側で走るがテレポートは EntityPlayerMP 限定。移植版はテレポート/耐久をサーバー限定に統一
		if(!level.isClientSide)
		{
			Vec3 start = player.getEyePosition();
			Vec3 look = player.getLookAngle();
			Vec3 end = start.add(look.scale(REACH));

			// 原版 world.func_147447_a(vec3d, vec3d1, false, true, true)：方块（含流体）碰撞
			BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if(blockHit.getType() == HitResult.Type.BLOCK)
			{
				end = blockHit.getLocation();
			}

			// 原版 getEntitiesWithinAABBExcludingEntity(player, boundingBox.addCoord(look*64).expand(1,1,1))
			AABB searchBox = player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0D);
			List<Entity> candidates = level.getEntities(player, searchBox,
					e -> e instanceof LivingEntity && e.isAlive());

			Entity target = null;
			double nearest = 0.0D;

			for(Entity entity : candidates)
			{
				if(!(entity instanceof LivingEntity))
				{
					continue;
				}

				// 原版 boundingBox.expand(0.3) の intercept 判定
				AABB box = entity.getBoundingBox().inflate(0.3D);
				var intercept = box.clip(start, end);
				if(intercept.isEmpty())
				{
					continue;
				}

				double distance = start.distanceTo(intercept.get());
				if(distance < nearest || nearest == 0.0D)
				{
					target = entity;
					nearest = distance;
				}
			}

			// 命中したら、その手前 2 格へプレイヤーを移動させる（原版の式そのまま）
			if(target != null)
			{
				Vec3 fromPlayer = THShotLib.pos_Living(player);
				Vec3 toVec = new Vec3(target.getX() - fromPlayer.x, target.getY() - fromPlayer.y, target.getZ() - fromPlayer.z);
				Vec3 direction = toVec.normalize();

				double newX = player.getX() + toVec.x - direction.x * STAND_OFF;
				double newY = (int)(player.getY() + toVec.y - direction.y * STAND_OFF + 1.0D) + 0.1D;
				double newZ = player.getZ() + toVec.z - direction.z * STAND_OFF;

				if(player instanceof ServerPlayer serverPlayer)
				{
					// 1.20.1 必须先改服务端实体坐标再发包，否则客户端会被旧位置拉回
					player.setPos(newX, newY, newZ);
					serverPlayer.connection.teleport(newX, newY, newZ, player.getYRot(), player.getXRot());
				}
			}

			// 原版 itemStack.damageItem(1, player)
			int damage = itemStack.getDamageValue() + 1;
			itemStack.setDamageValue(damage);

			// 耐久切れ：普通の陰陽玉へ戻し、万宝槌で保存された元の耐久値を復元
			if(damage >= MAX_DAMAGE)
			{
				int originalDamage = 0;
				if(THStackData.get(itemStack) != null)
				{
					originalDamage = THStackData.get(itemStack).getInt("OriginalDamage");
					THStackData.get(itemStack).remove("OriginalDamage");
				}

				ItemStack orb = new ItemStack(THKaguyaItems.YIN_YANG_ORB.get());
				orb.setDamageValue(originalDamage);

				InteractionHand hand = player.getMainHandItem() == itemStack
						? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
				player.setItemInHand(hand, orb);
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
	{
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		// 原版 EnumAction.bow
		return UseAnim.BOW;
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		// 原版 hasEffect 恒 true（常に発光）
		return true;
	}
}
