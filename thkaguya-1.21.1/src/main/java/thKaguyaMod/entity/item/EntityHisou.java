package thKaguyaMod.entity.item;

import net.minecraft.core.HolderLookup;
import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thKaguyaMod.ShotData;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 緋想の剣 */
public class EntityHisou extends Entity
{
	private static final EntityDataAccessor<Integer> NUM =
			SynchedEntityData.defineId(EntityHisou.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANGLE =
			SynchedEntityData.defineId(EntityHisou.class, EntityDataSerializers.INT);

	/** 使用者 */
	public LivingEntity userEntity;
	private Entity motherEntity;
	private int count;
	public int num;
	private int damage;
	private int spellCardUsedTime;

	public EntityHisou(EntityType<?> type, Level level)
	{
		super(type, level);
	}

	public EntityHisou(Level world)
	{
		this(THKaguyaEntities.HISOU.get(), world);
	}

	public EntityHisou(Level world, LivingEntity entityLivingBase, Entity mother, int n, int da)
	{
		this(world);

		userEntity = entityLivingBase;//使用者をshootingEntityに保存
		xo = userEntity.getX();
		yo = userEntity.getY();
		zo = userEntity.getZ();
		setPos(userEntity.getX() - Math.sin(userEntity.getYRot() / 180F * (float)Math.PI) * Math.cos(userEntity.getXRot() / 180F * (float)Math.PI) * 1.2D,
				userEntity.getY() - Math.sin(userEntity.getXRot() / 180F * (float)Math.PI) * 1.2D +  userEntity.getEyeHeight() - 0.7D,
				userEntity.getZ() + Math.cos(userEntity.getYRot() / 180F * (float)Math.PI) * Math.cos(userEntity.getXRot() / 180F * (float)Math.PI) * 1.2D);//初期位置を設定(x,y,z)
		setYRot(userEntity.getYRot());
		startRiding(userEntity);
		motherEntity = mother; //残像に対して本物
		count = 0;
		num = n;
		setNum(n);
		damage = da;
		spellCardUsedTime = 0;
		// 原版 EntityHisou.java:52：生成時にマスタースパーク音を鳴らす（HisoutenVol）
		if(!level().isClientSide)
		{
			level().playSound(null, blockPosition(), thKaguyaMod.init.THKaguyaSounds.MASTER_SPARK.get(),
					net.minecraft.sounds.SoundSource.PLAYERS, THKaguyaConfig.HisoutenVol, 1.0F);
		}
	}

	/** 生成時に一度だけ呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(NUM, 0);
		builder.define(ANGLE, 0);
	}

	/**
	 * Returns true if this entity should push and be pushed by other entities when colliding.
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * Returns true if other Entities should be prevented from moving through this Entity.
	 */
	@Override
	public boolean isPickable()
	{
		return false;
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		if(!level().isClientSide && userEntity == null)
		{
			if(num == 8)
			{
				THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.HISOU_SWORD.get(), damage);
			}
			else
			{
				discard();
			}
			return;
		}

		setAngle(getAngle() + ((float)count+(float)getNum()/3.0F) * 39.0F);

		THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, 0F);
		if(userEntity != null)//shootingEntityが存在するなら、それをぬるぬる追尾する
		{
			setYRot(userEntity.getYRot());
			setXRot(userEntity.getXRot());
		}
		//else//使用者がいないならその場でアイテム化
		else{
			if(getNum() == 8)
			{
				THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.HISOU_SWORD.get(), damage);
			}
			if(!level().isClientSide)
			{
				discard();
			}
		}

	//****気質弾の発射****//
	if(tickCount > spellCardUsedTime && userEntity != null && getNum() == 8)
	{
		ShotData shot = ShotData.shot(FORM_KISHITU, RED, 0.5F, 6.0F, 3, 60, SPECIAL_HISOUTEN01);
		THShotLib.createRandomRingShot(userEntity, this, THShotLib.pos_Entity(this), userEntity.getLookAngle(), 0F, 9999, 0.1D, 0.8D, 0.05D, THShotLib.gravity_Zero(), shot, num, 0.1D, 30F);
	}
	//********//

	if(tickCount > spellCardUsedTime)
	{
		spellCardUsedTime = tickCount;
	}

		//時間で消滅
	if(tickCount >= 100)
	{
		if(getNum() == 8)
		{
			THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.HISOU_SWORD.get(), damage);
		}
		if(!level().isClientSide)
		{
			discard();
		}
	}
	}

	//乗っているEntityの処理
	//ここでは使用者の処理。使用者のmotionXなどは恐らくここでしかまともに操作できない
	@Override
	public void rideTick()
	{
		// 原版 1.7.10 の流れ：Entity.onUpdate() の内部で this.updateRidden()（＝この減速処理）が
		// 呼ばれ、その後に EntityHisou.onUpdate() 本体が走る。つまり
		//   「減速処理 → 本体（消滅判定・気質弾の発射）」
		// の順で、騎乗中でも本体は毎 tick 実行されていた。
		//
		// 1.20.1 では乗客は tick() ではなく rideTick() 経由でしか駆動されない
		// （Entity.rideTick() が tick() を呼ぶ）。ここで覆写したうえで tick() を
		// 駆動しないと本体が一切走らず、次の 2 つが同時に壊れる（実測で発生）：
		//   1) tickCount >= 100 の消滅判定が発火せず主剣（num==8）が残留する
		//   2) その結果、下の使用者減速（motion *= 0.1）が永久に掛かり続け、
		//      プレイヤーの移動速度と落下速度が戻らなくなる
		//   3) tickCount > spellCardUsedTime の気質弾も発射されない
		//
		// なお Entity.rideTick() が行う positionRider() は意図的に通していない：
		// tick() 内の itemEffectFollowUser(this, userEntity, 1.2D, 0F) が
		// 使用者の 1.2 ブロック前方へ配置するため、既定の騎乗位置で上書きさせない。
		if(getVehicle() != null && getNum() == 8)
		{
			Entity ridden = getVehicle();
			ridden.setYRot(ridden.yRotO + (ridden.getYRot() - ridden.yRotO) * 0.3F);
			ridden.setXRot(ridden.xRotO + (ridden.getXRot() - ridden.xRotO) * 0.3F);
			ridden.setDeltaMovement(ridden.getDeltaMovement().scale(0.1D));
		}

		// 本体（消滅判定・気質弾・追尾）を走らせる。これが 1.7.10 の「onUpdate が毎 tick 走る」相当。
		if(this.isAlive())
		{
			this.tick();
		}
	}

	/**
	 * 保存するデータの書き込み
	 * @param tag : NTBタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putShort("count", (short)count);
		tag.putShort("damage", (short)damage);
		tag.putByte("number", (byte)num);
	}

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		count = tag.getShort("count");
		damage = tag.getShort("damage");
		num = (int)tag.getByte("number");
	}


	public void setNum(int num)
	{
		this.entityData.set(NUM, num);
	}

	public int getNum()
	{
		return this.entityData.get(NUM);
	}

	public void setAngle(float angle)
	{
		this.entityData.set(ANGLE, (int)(angle * 1000F));
	}

	public float getAngle()
	{
		return (float)this.entityData.get(ANGLE) / 1000F;
	}

}
