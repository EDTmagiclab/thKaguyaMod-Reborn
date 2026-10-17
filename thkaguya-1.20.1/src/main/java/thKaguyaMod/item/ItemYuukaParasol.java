package thKaguyaMod.item;

import static thKaguyaMod.DanmakuConstants.*;
import static thKaguyaMod.THShotLib.*;

import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.item.EntityYuukaParasol;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

/** 幽香の傘 */
public class ItemYuukaParasol extends Item implements ISpecialShot
{
	public static final int PARASOL_MAX_DAMAGE = 60;
	public static final int SPECIAL_FLOWER_LAND = 350;

	public ItemYuukaParasol()
	{
		super(new Item.Properties().stacksTo(1).durability(PARASOL_MAX_DAMAGE));
		// 原版 ItemYuukaParasol 构造器末尾：registerSpecialShot(ItemYuukaParasol.class, SPECIAL_FLOWER_LAND)
		// ① 登记「类」：EntityTHShot 的分发先查 getSpecialShotClass(id)，为 null 就整个跳过、
		//    连下面的硬编码 switch 都轮不到，表现为「开花完全没反应」；
		// ② 再登记「实例」：1.20.1 不能 new Item，实体必须直接拿到这个物品实例。
		SpecialShotRegistry.registerSpecialShot(ItemYuukaParasol.class, SPECIAL_FLOWER_LAND);
		SpecialShotRegistry.registerSpecialShotInstance(this, SPECIAL_FLOWER_LAND);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);

		if(player.isShiftKeyDown())
		{
			// 周囲に自分の日傘があるなら出せない
			List<Entity> list = level.getEntities(player, player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
			for(Entity entity : list)
			{
				if(entity instanceof EntityYuukaParasol entityYuukaParasol && entityYuukaParasol.user == player)
				{
					return InteractionResultHolder.fail(itemStack);
				}
			}

			EntityYuukaParasol entityYuukaParasol = new EntityYuukaParasol(level, player, 0);
			if(!level.isClientSide)
			{
				level.addFreshEntity(entityYuukaParasol);
				itemStack.shrink(1);
			}
		}
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int timeLeft)
	{
		if(!(entity instanceof Player player))
		{
			return;
		}

		int shotPower = 10 + getUseDuration(itemStack) - timeLeft;
		double shotPower2 = (float)shotPower / 10.0D;
		if(shotPower2 > 4.0D)
		{
			shotPower2 = 5.0D;
		}
		float transDegree = 3.141593F / 180F;
		double ax, ay, az;
		Random random = new Random();
		float angle = (float)random.nextInt(36000) / 100F;
		float angle2 = 0.0F;
		int shotNum = 45;
		float angleSpan = (360F / (float)shotNum) * transDegree;
		float angle2Span = angleSpan * 20F;
		ShotData shot = ShotData.shot(FORM_SCALE, RED, 0.3F, 7.0F, 3, 80, SPECIAL_FLOWER_LAND);
		int color = THShotLib.SCALE[RED] + random.nextInt(2);
		if(player.getRandom().nextInt(2) == 0)
		{
			shot.color = YELLOW;
		}

		for(int i = 0; i < shotNum; i++)
		{
			ax = -(double)Mth.sin(angle);
			az = (double)Mth.cos(angle);
			double speed = 0.23D - (double)Mth.sin(angle2) * 0.02D;

			createShot(player, player,
				pos(player.getX(), THShotLib.getPosYFromEye(player, 0.2D), player.getZ()),
				angle(ax * shotPower2, 1.4F, az * shotPower2), 0F,
				speed, speed, 0.0D, gravity(-0.02), shot);
			angle += angleSpan;
			angle2 += angle2Span;
		}

		shotNum = (int)((float)shotNum * 0.25F);
		angleSpan = (360F / (float)shotNum) * transDegree;
		shotPower2 *= 0.66D;
		for(int i = 0; i < shotNum; i++)
		{
			ax = -(double)Mth.sin(angle);
			az = (double)Mth.cos(angle);
			createShot(player, player,
				pos(player.getX(), THShotLib.getPosYFromEye(player, 0.2D), player.getZ()),
				angle(ax * shotPower2, 1.1F, az * shotPower2), 0F,
				0.23D, 0.23D, 0.0D, gravity(-0.02), shot);
			angle += angleSpan;
		}
		// 原版 "random.bow"
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
		itemStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity living_hit, LivingEntity living_used)
	{
		double disX = living_hit.getX() - living_used.getX();
		double disY = living_hit.getY() - living_used.getY();
		double disZ = living_hit.getZ() - living_used.getZ();
		float toEntityYaw = (float)Math.atan2(disX, disZ);
		float toEntityPitch = (float)Math.atan2(disY, Math.sqrt(disX * disX + disZ * disZ));
		double power = 3.0D;

		living_hit.setDeltaMovement(
				Mth.sin(toEntityYaw) * Mth.cos(toEntityPitch) * power,
				Mth.sin(toEntityPitch) * power + 1.0D,
				Mth.cos(toEntityYaw) * Mth.cos(toEntityPitch) * power);

		living_hit.hurt(living_hit.level().damageSources().mobAttack(living_used), 7);
		itemStack.hurtAndBreak(1, living_used, e -> {});

		return true;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 72000;
	}

	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot)
	{
	}

	@Override
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot, HitResult result)
	{
		// 原版取的是 (int)posX, (int)(posY - 0.3D), (int)posZ —— 相对弹体中心往下 0.3 格的那个方块。
		// 1.20.1 里直接用命中结果给的方块坐标更准（弹体贴到方块表面时 blockPosition() 常常还落在空气里）。
		BlockPos hitPoint;
		if(result instanceof BlockHitResult blockHit)
		{
			hitPoint = blockHit.getBlockPos();
		}
		else
		{
			hitPoint = BlockPos.containing(shot.getX(), shot.getY() - 0.3D, shot.getZ());
		}
		if(world.getBlockState(hitPoint).getBlock() == Blocks.GRASS_BLOCK)
		{
			BlockPos setPos = hitPoint.above();
			if(world.getBlockState(setPos).isAir())
			{
				if(shot.getShotColor() == YELLOW)
				{
					if(!world.isClientSide)
					{
						world.setBlock(setPos, Blocks.DANDELION.defaultBlockState(), 3);
					}
				}
				else if(shot.getShotColor() == RED)
				{
					if(!world.isClientSide)
					{
						world.setBlock(setPos, Blocks.POPPY.defaultBlockState(), 3);
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean specialShot_hitEntity(Level world, int id, EntityTHShot shot, Entity entity_Hit)
	{
		return false;
	}
}
