package thKaguyaMod.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.item.EntitySukima;
import thKaguyaMod.init.THKaguyaConfig;

/** スキマを設置する */
public class ItemSukima extends Item
{
	public ItemSukima()
	{
		// 原版 ItemSukima は maxStackSize を設定していない＝既定の 64。
		// 移植版は 1 に絞っていたが、原版どおりスタック可能に戻す。
		super(new Item.Properties());
	}

	private static float getSetAngle(Player player, double setX, double setZ)
	{
		float rotationYaw = (float)Math.atan2(setX - player.getX(), setZ - player.getZ()) / 3.141593F * 180F;
		if(rotationYaw < 0F)
		{
			rotationYaw += 360F;
		}
		if(rotationYaw % 90F < 45F)
		{
			rotationYaw = rotationYaw - (rotationYaw % 45F);
		}
		else
		{
			rotationYaw = rotationYaw - (rotationYaw % 45F) + 45F;
		}
		return -rotationYaw;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		double setX = player.getX() - Math.sin(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 2.0D;
		double setY = player.getY() - Math.sin(player.getXRot() / 180F * (float)Math.PI) * 2.0D + player.getEyeHeight() / 2.0D;
		double setZ = player.getZ() + Math.cos(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 2.0D;
		boolean lost = true;

		if(!level.getBlockState(new BlockPos((int)setX, (int)setY, (int)setZ)).isRedstoneConductor(level, new BlockPos((int)setX, (int)setY, (int)setZ)))
		{
			EntitySukima entitySukima;
			EntitySukima entitySukima2;
			double hitX = player.getX();
			double hitY = player.getY();
			double hitZ = player.getZ();
			float rotationYaw = 0F;

			if(!player.isShiftKeyDown())
			{
				double i = (int)setX;
				double j = (int)setY;
				double k = (int)setZ;

				HitResult hit = player.pick(5.0D, 0.0F, false);

				if(hit.getType() == HitResult.Type.MISS)
				{
					Vec3 look = player.getLookAngle();
					i = (player.getX() + look.x * 2.0D - 0.5D);
					j = (player.getY() + look.y * 2.0D + 1.0D);
					k = (player.getZ() + look.z * 2.0D - 0.5D);
					rotationYaw = player.getYRot();
				}
				else if(hit.getType() == HitResult.Type.BLOCK)
				{
					BlockHitResult blockHit = (BlockHitResult)hit;
					i = blockHit.getBlockPos().getX();
					j = blockHit.getBlockPos().getY();
					k = blockHit.getBlockPos().getZ();

					switch(blockHit.getDirection())
					{
						case DOWN:   j--; rotationYaw = getSetAngle(player, i, k); break;
						case UP:     j++; rotationYaw = getSetAngle(player, i, k); break;
						case NORTH:  k--; rotationYaw = 0F;   break;
						case SOUTH:  k++; rotationYaw = 180F; break;
						case WEST:   i--; rotationYaw = 270F; break;
						case EAST:   i++; rotationYaw = 90F;  break;
					}
				}

				entitySukima = new EntitySukima(level, player, i + 0.5D, j, k + 0.5D, rotationYaw);
				entitySukima2 = null;
			}
			else
			{
				double xVecMax = -Math.sin(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 128.0D;
				double yVecMax = -Math.sin(player.getXRot() / 180F * (float)Math.PI) * 128.0D;
				double zVecMax = Math.cos(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 128.0D;

				Vec3 vec3d = THShotLib.pos(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
				Vec3 vec3d1 = THShotLib.pos(player.getX() + xVecMax, player.getY() + yVecMax, player.getZ() + zVecMax);

				BlockHitResult blockHit = level.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

				if(blockHit.getType() == HitResult.Type.BLOCK)
				{
					vec3d1 = blockHit.getLocation();
					hitX = blockHit.getLocation().x;
					hitY = blockHit.getLocation().y;
					hitZ = blockHit.getLocation().z;
					double xz_sq = (hitX - setX) * (hitX - setX) + (hitZ - setZ) * (hitZ - setZ);
					if(Math.sqrt(xz_sq + (hitY - setY) * (hitY - setY)) <= 5.0D)
					{
						return InteractionResultHolder.fail(itemStack);
					}
				}
				else
				{
					return InteractionResultHolder.fail(itemStack);
				}

				Entity entity = null;
				List<Entity> list = level.getEntities(player, player.getBoundingBox().move(xVecMax, yVecMax, zVecMax).inflate(1.0D, 1.0D, 1.0D));
				double d = 0.0D;
				for(Entity entity1 : list)
				{
					if(entity1 instanceof LivingEntity)
					{
						AABB axisalignedbb = entity1.getBoundingBox().inflate(0.3D, 0.3D, 0.3D);
						Optional<Vec3> mop1 = axisalignedbb.clip(vec3d, vec3d1);
						if(mop1.isPresent())
						{
							double d1 = vec3d.distanceTo(mop1.get());
							if((d1 < d || d == 0.0D) && d1 > 5.0D)
							{
								entity = entity1;
								d = d1;
							}
						}
					}
				}

				if(entity != null)
				{
					if(entity instanceof LivingEntity hitEntityLivingBase)
					{
						hitX = hitEntityLivingBase.getX();
						hitY = hitEntityLivingBase.getY() + hitEntityLivingBase.getBbHeight() / 2.0D;
						hitZ = hitEntityLivingBase.getZ();
						setY += 1.0D;
					}
					else
					{
						hitX = entity.getX();
						hitY = entity.getY() + 0.75D;
						hitZ = entity.getZ();
					}
				}

				entitySukima = new EntitySukima(level, player, setX, setY, setZ, 16);
				entitySukima2 = new EntitySukima(level, player, hitX, hitY, hitZ, 16);
				lost = player.getRandom().nextInt(100) < 15;

				if(!level.isClientSide)
				{
					level.addFreshEntity(entitySukima2);
				}
			}

			if(THKaguyaConfig.useDefaultGapSE)
			{
				// 原版 "mob.endermen.portal"
				level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
			}
			else
			{
				// 自定义音效：原版 jar 无 sukima.ogg（上游遗留），回退同款原版音效
				level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
			}

			if(!level.isClientSide)
			{
				level.addFreshEntity(entitySukima);
				if(lost)
				{
					itemStack.shrink(1);
				}
			}
		}
		return InteractionResultHolder.success(itemStack);
	}
}
