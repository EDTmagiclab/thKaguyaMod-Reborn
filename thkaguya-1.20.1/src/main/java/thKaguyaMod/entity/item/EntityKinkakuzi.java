package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 金閣寺の一枚天井 */
public class EntityKinkakuzi extends Entity
{
	private static final EntityDataAccessor<Integer> TIME_SINCE_HIT =
			SynchedEntityData.defineId(EntityKinkakuzi.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FORWARD_DIRECTION =
			SynchedEntityData.defineId(EntityKinkakuzi.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DAMAGE_TAKEN =
			SynchedEntityData.defineId(EntityKinkakuzi.class, EntityDataSerializers.FLOAT);

	private boolean field_70279_a;
	private double speedMultiplier;
	private int boatPosRotationIncrements;
	private double boatX;
	private double boatY;
	private double boatZ;
	private double boatYaw;
	private double boatPitch;
	private LivingEntity shootingEntity;

	private int lastTime;
	public double lastMotionX;
	public double lastMotionY;
	public double lastMotionZ;

	/** ワールド読み込み時に呼び出されるコンストラクト */
	public EntityKinkakuzi(EntityType<?> type, Level level)
	{
		super(type, level);
		field_70279_a = true;
		this.speedMultiplier = 0.07D;
	}

	public EntityKinkakuzi(Level world)
	{
		this(THKaguyaEntities.KINKAKUZI.get(), world);
	}

	/** 出現時に呼び出されるコンストラクト */
	public EntityKinkakuzi(Level world, double x, double y, double z, LivingEntity entityLivingBase, double pow)
	{
		this(world);
		Vec3 motion = new Vec3(
				-Math.sin((float)Math.toRadians(entityLivingBase.getYRot())) * 0.2D,
				pow,
				Math.cos((float)Math.toRadians(entityLivingBase.getYRot())) * 0.2D);
		setDeltaMovement(motion);
		lastMotionX = motion.x;
		lastMotionY = motion.y;
		lastMotionZ = motion.z;
		lastTime = 0;
		xo = x;
		yo = y;
		zo = z;
		setYRot(entityLivingBase.getYRot());
		setXRot(0.0F);
		moveTo(x, y, z, getYRot(), getXRot());
		shootingEntity = entityLivingBase;//使用者をshootingEntityに保存
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData()
	{
		this.entityData.define(TIME_SINCE_HIT, 0);
		this.entityData.define(FORWARD_DIRECTION, 1);
		this.entityData.define(DAMAGE_TAKEN, 0.0F);
	}

	//押すことができるかどうか
	@Override
	public boolean isPushable()
	{
		return true;
	}

	//Entityが乗ったときのそのEntityを置く高さ
	public double getMountedYOffset()
	{
		return 0.3D;
	}

	//Entityに攻撃されたときに呼び出されるメソッド　破壊とか関係なしに攻撃されれば呼び出される
	@Override
	public boolean hurt(DamageSource source, float amount)
	{
		if (this.isInvulnerable())
		{
			return false;
		}
		else if (!level().isClientSide && !isRemoved())
		{
			setForwardDirection(-getForwardDirection());
			setTimeSinceHit(10);
			setDamageTaken(getDamageTaken() + amount * 10.0F);
			markHurt();
			boolean flag = source.getEntity() instanceof Player && ((Player)source.getEntity()).isCreative();

			if (flag || getDamageTaken() > 40.0F)//相手がプレイヤーでクリエイティブ、または、４以上のダメージを受けているなら
			{
				if (getFirstPassenger() != null)
				{
					getFirstPassenger().startRiding(this);
				}
				if(!flag)
				{
					this.spawnAtLocation(new ItemStack(THKaguyaItems.KINKAKUJI.get(), 1));
				}
				discard();
			}

			return true;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Returns true if other Entities should be prevented from moving through this Entity.
	 */
	@Override
	public boolean isPickable()
	{
		return !isRemoved();
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		if (getTimeSinceHit() > 0)
		{
			setTimeSinceHit(getTimeSinceHit() - 1);
		}

		if (getDamageTaken() > 0)
		{
			setDamageTaken(getDamageTaken() - 1);
		}

		Vec3 motion = getDeltaMovement();
		double motionX = motion.x;
		double motionY = motion.y;
		double motionZ = motion.z;

		AABB box = getBoundingBox();
		double d = getWaterSubmersion(box);
		double lengthXZ = Math.sqrt(motionX * motionX + motionZ * motionZ);
		double newPosX;
		double newPosY;
		double newPosZ;

		if (d < 1.0D)
		{
			newPosX = d * 2D - 1.0D;
			motionY += 0.039999999105930328D * newPosX;
		}
		else
		{
			//上昇していないなら
			if (motionY < 0.0D)
			{
				motionY /= 2.0D;//落下速度を半分にする
			}

			//わずかに浮上させる
			motionY += 0.0070000002160668373D;
		}

		if (getFirstPassenger() != null && getFirstPassenger() instanceof LivingEntity)//誰かが乗っているなら
		{
			LivingEntity rider = (LivingEntity)getFirstPassenger();
			newPosX = (double)rider.zza;

			if (newPosX > 0.0D)
			{
				newPosY = -Math.sin((double)(rider.getYRot() * (float)Math.PI / 180.0F));
				newPosZ = Math.cos((double)(rider.getYRot() * (float)Math.PI / 180.0F));
				motionX += newPosY * this.speedMultiplier * 0.05000000074505806D;
				motionZ += newPosZ * this.speedMultiplier * 0.05000000074505806D;
			}
		}

		newPosX = Math.sqrt(motionX * motionX + motionZ * motionZ);

		if(newPosX > 0.35D)
		{
			newPosY = 0.35D / newPosX;
			motionX *= newPosY;
			motionZ *= newPosY;
			newPosX = 0.35D;
		}

		if(newPosX > lengthXZ && speedMultiplier < 0.35D)
		{
			this.speedMultiplier += (0.35D - this.speedMultiplier) / 35.0D;

			if (this.speedMultiplier > 0.35D)
			{
				this.speedMultiplier = 0.35D;
			}
		}
		else
		{
			this.speedMultiplier -= (this.speedMultiplier - 0.07D) / 35.0D;

			if (this.speedMultiplier < 0.07D)
			{
				this.speedMultiplier = 0.07D;
			}
		}

		//地面に付いているなら
		if (onGround())
		{
			motionX *= 0.5D;
			motionY *= 0.5D;
			motionZ *= 0.5D;
		}

		//移動量分だけEntityを動かす
		move(MoverType.SELF, new Vec3(motionX, motionY, motionZ));

		if (horizontalCollision && lengthXZ > 0.2D)
		{
			//ボートが勢い良くぶつかったときの処理。壊れないから無視
		}
		else
		{
			motionX *= 0.99000000953674316D;
			motionY *= 0.94999998807907104D;
			motionZ *= 0.99000000953674316D;
		}

		setXRot(0.0F);
		newPosY = getYRot();
		newPosZ = xo - getX();
		double d10 = zo - getZ();

		if (newPosZ * newPosZ + d10 * d10 > 0.001D)
		{
			newPosY = (float)((Math.atan2(d10, newPosZ) * 180D) / Math.PI);
		}

		double angleShift = Mth.wrapDegrees(newPosY - (double)this.getYRot());

		//変化量を上限下限に収める
		if (angleShift > 20D)
		{
			angleShift = 20D;
		}
		if (angleShift < -20D)
		{
			angleShift = -20D;
		}
		setYRot((float)((double)this.getYRot() + angleShift));

		if(!level().isClientSide)
		{
			List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.20000000298023224D, 0.0D, 0.20000000298023224D));
			int l;

			if (list != null && !list.isEmpty())
			{
				for (l = 0; l < list.size(); ++l)
				{
					Entity entity = list.get(l);

					if (entity != this.getFirstPassenger() && entity.isPushable() && entity instanceof Boat)
					{
						entity.push(this);
					}
				}
			}

			//***********　金閣寺の一枚天井とEntityの当たり判定を取る****************//
			Entity entity = null;
			list = level().getEntities(this, getBoundingBox().inflate(
					Mth.sin((float)boatYaw / 180.0F * (float)Math.PI) * 0.1D,
					0.5D,
					Mth.cos((float)boatYaw / 180.0F * (float)Math.PI) * 0.1D));

			if (list != null && !list.isEmpty())
			{
				for (int j1 = 0; j1 < list.size(); j1++)
				{
					entity = list.get(j1);
					if (entity != getFirstPassenger() && entity.isPushable() && (entity instanceof EntityKinkakuzi))
					{
						entity.push(this);
					}
					if (entity != null )
					{
						if (entity != getFirstPassenger() && !(entity instanceof EntityKinkakuzi))
						{
							hitOnEntity(entity);//ダメージを与える処理とかしてる
						}
					}
				}
			}

			//************************************************************************************//

			//誰かが乗っていて、それが死んでいるなら
			if (getFirstPassenger() != null && getFirstPassenger().isRemoved())
			{
				//乗っているEntityをいないことにする
				ejectPassengers();
			}
		}

		setDeltaMovement(motionX, motionY, motionZ);

		//通常通り時間が進んでいるなら
		if(tickCount > lastTime)
		{
			//最後に時間が動いていたときの時間と移動量を保存する
			lastTime = tickCount;
			lastMotionX = motionX;
			lastMotionY = motionY;
			lastMotionZ = motionZ;
		}
	}

	//金閣寺の一枚天井が水にどれだけ浸かっているか
	private double getWaterSubmersion(AABB box)
	{
		double d = 0.0D;
		int i = 5;
		for (int j = 0; j < i; j++)
		{
			double d2 = box.minY + ((box.maxY - box.minY) * (double)(j + 0)) / (double)i - 0.125D;
			double d8 = box.minY + ((box.maxY - box.minY) * (double)(j + 1)) / (double)i - 0.125D;
			BlockPos pos = new BlockPos((int)(box.minX + box.maxX) / 2, (int)((d2 + d8) / 2), (int)(box.minZ + box.maxZ) / 2);
			if (level().getBlockState(pos).getFluidState().is(FluidTags.WATER))
			{
				d += 1.0D / (double)i;
			}
		}
		return d;
	}

	//Entityと衝突したときの処理
	protected void hitOnEntity(Entity hitEntity)
	{
		//金閣寺の一枚天井が当たったEntityより上にあり、当たったEntityが、金閣寺の一枚天井、アイテム、使用者、プレイヤー、経験値オーブでないなら
		if(!level().isClientSide && hitEntity != null)
		{
			if ( hitEntity.getY() < this.getY() &&
				!(hitEntity instanceof ItemEntity)
				&& !(hitEntity instanceof ExperienceOrb)
				&& !(hitEntity instanceof EntityTHShot)
				&& hitEntity != shootingEntity)
			{
				double yMove = yo - getY();
				if(yMove >= 0.0)
				{
					int damage = (int)(yMove * 40.0) + 2;//ダメージは落下速度依存　最低でもハート１個分はダメージを与える
					hitEntity.hurt(level().damageSources().indirectMagic(this, shootingEntity), damage);
				}
			}
		}
	}

	//金閣寺の一枚天井に乗っているEntityの位置の更新
	@Override
	public void positionRider(Entity passenger, Entity.MoveFunction moveFunc)
	{
		super.positionRider(passenger, moveFunc);
		if (getFirstPassenger() != null && getFirstPassenger() == passenger)
		{
			double d = Math.cos(((double)getYRot() * Math.PI) / 180D) * 0.5D;
			double d1 = Math.sin(((double)getYRot() * Math.PI) / 180D) * 0.5D;
			passenger.setPos(getX() + d, getY() + getMountedYOffset() + passenger.getMyRidingOffset(), getZ() + d1);
		}
	}

	/**
	 * 保存するデータの書き込み
	 * @param tag : NTBタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
	}

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
	}


	//プレイヤーに右クリックされたときに呼ばれる
	@Override
	public InteractionResult interact(Player player, InteractionHand hand)
	{
		if (getFirstPassenger() != null && getFirstPassenger() instanceof Player && getFirstPassenger() != player)
		{
			return InteractionResult.SUCCESS;
		}
		else
		{
			if (!level().isClientSide)
			{
				player.startRiding(this);
			}

			return InteractionResult.SUCCESS;
		}
	}

	/**
	 * Sets the damage taken from the last hit.
	 */
	public void setDamageTaken(float par1)
	{
		this.entityData.set(DAMAGE_TAKEN, par1);
	}

	/**
	 * Gets the damage taken from the last hit.
	 */
	public float getDamageTaken()
	{
		return this.entityData.get(DAMAGE_TAKEN);
	}

	/**
	 * Sets the time to count down from since the last time entity was hit.
	 */
	public void setTimeSinceHit(int par1)
	{
		this.entityData.set(TIME_SINCE_HIT, par1);
	}

	/**
	 * Gets the time since the last hit.
	 */
	public int getTimeSinceHit()
	{
		return this.entityData.get(TIME_SINCE_HIT);
	}

	/**
	 * Sets the forward direction of the entity.
	 */
	public void setForwardDirection(int par1)
	{
		this.entityData.set(FORWARD_DIRECTION, par1);
	}

	/**
	 * Gets the forward direction of the entity.
	 */
	public int getForwardDirection()
	{
		return this.entityData.get(FORWARD_DIRECTION);
	}

}
