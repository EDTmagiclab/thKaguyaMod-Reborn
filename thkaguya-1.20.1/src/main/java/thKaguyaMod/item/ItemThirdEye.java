package thKaguyaMod.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 第三の眼（Third Eye）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemThirdEye.java（282 行，extends Item）：
 * ・右键：从眼睛沿视线看 64 格（先算方块碰撞截断），再扫这条线附近 1 格的实体，
 *   用每个实体包围盒 expand(0.3) 与线段求交，取最近的那个记下来
 * ・下一 tick 的 onUpdate 里：把「名字」和「 HP ： 现在/最大（都除以 2 显示）」两条聊天发给持有者；
 *   如果是驯服动物，再补一条「  Owner : 主人名」
 * ・onUpdate 里每 tick 自愈 1 点耐久（原版 damageItem(-1)）
 * ・耐久 150、不可堆叠
 *
 * 原版把目标存在 Item 单例字段上（多人游戏里会串号），这里改成按玩家 UUID 存、只在服务端读写。
 * 原版 getOwner() 没有判空，这里补了判空。
 */
public class ItemThirdEye extends Item
{
	/** 原版 check / hitEntity 两个字段的替代：每个玩家各自记一个「刚被看到的实体」 */
	private final Map<UUID, LivingEntity> watched = new HashMap<>();

	public ItemThirdEye()
	{
		super(new Properties().stacksTo(1).durability(150));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);

		Vec3 look = player.getViewVector(1.0F);
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(look.scale(64.0D));

		// 原版 world.func_147447_a(vec3d, vec3d1, false, true, true)：命中方块则把终点截到命中点
		BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if(blockHit.getType() == HitResult.Type.BLOCK)
		{
			end = blockHit.getLocation();
		}

		// 原版 getEntitiesWithinAABBExcludingEntity(player, boundingBox.addCoord(look*64).expand(1,1,1))
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(64.0D)).inflate(1.0D);
		LivingEntity found = null;
		double best = 0.0D;
		for(Entity entity : level.getEntities(player, searchBox))
		{
			if(!(entity instanceof LivingEntity living))
			{
				continue;
			}
			// 原版 boundingBox.expand(0.3).calculateIntercept(vec3d, vec3d1)
			Optional<Vec3> intercept = living.getBoundingBox().inflate(0.3D).clip(start, end);
			if(intercept.isEmpty())
			{
				continue;
			}
			double distance = start.distanceTo(intercept.get());
			if(distance < best || best == 0.0D)
			{
				found = living;
				best = distance;
			}
		}

		if(found != null && !level.isClientSide)
		{
			watched.put(player.getUUID(), found);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if(level.isClientSide)
		{
			return;
		}

		// 原版：if(itemstack.isItemDamaged()) itemstack.damageItem(-1, entity) —— 每 tick 回 1 点耐久
		if(stack.getDamageValue() > 0)
		{
			stack.setDamageValue(stack.getDamageValue() - 1);
		}

		// 原版：check && hitEntity != null 时给持有者播报
		if(entity instanceof Player player)
		{
			LivingEntity target = watched.remove(player.getUUID());
			if(target == null || !target.isAlive())
			{
				return;
			}

			float hp = (float)target.getHealth() / 2.0F;
			hp = (float)Math.round(hp * 100.0F) / 100.0F;

			String name = target instanceof Player targetPlayer
					? targetPlayer.getDisplayName().getString()
					: target.getName().getString();
			String strHP = " HP ： " + hp + "/" + (float)target.getMaxHealth() / 2.0F;

			player.displayClientMessage(Component.literal(name).withStyle(ChatFormatting.WHITE), false);
			player.displayClientMessage(Component.literal(strHP).withStyle(ChatFormatting.WHITE), false);

			// 驯服动物的主人（原版直接在 getOwner() 上取名字，owner 为空会 NPE，这里判空）
			if(target instanceof TamableAnimal tameable && tameable.isTame())
			{
				Entity owner = tameable.getOwner();
				if(owner != null)
				{
					String strOwner = "  Owner : " + owner.getName().getString();
					player.displayClientMessage(Component.literal(strOwner).withStyle(ChatFormatting.WHITE), false);
				}
			}
		}
	}
}
