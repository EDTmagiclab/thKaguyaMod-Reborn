package thKaguyaMod.entity.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 幽香の日傘（1.20.1 移植版）
 *
 * 1.7.10 -> 1.20.1 关键映射：
 *   dataWatcher(19) -> SynchedEntityData PARASOL_MODE
 *   ridingEntity / updateRidden() -> getVehicle() / startRiding() / rideTick()
 *   isInRangeToRenderDist -> shouldRenderAtSqrDistance
 *   getCollisionBox(entity)（user 衝突なし） -> canCollideWith(entity)
 *   isDead -> !isAlive()
 *   ItemYuukaParasol.PARASOL_MAX_DAMAGE -> DanmakuConstants.PARASOL_MAX_DAMAGE
 */
public class EntityYuukaParasol extends Entity
{
	private static final EntityDataAccessor<Integer> PARASOL_MODE =
			SynchedEntityData.defineId(EntityYuukaParasol.class, EntityDataSerializers.INT);

	/** 使用者 */
	public Player user;
	public int damage;
	public int mode;

	/** ワールド読み込み時に呼び出されるコンストラクト */
public EntityYuukaParasol(EntityType<?> type, Level level)
    {
        super(type, level);
        // 原版：setSize(0.1F, 0.1F)（注册设定）、ignoreFrustumCheck = true（渲染阶段处理）
        // 1.20.1 noCulling = true 等价于 ignoreFrustumCheck：阳伞紧贴玩家头顶，bbox 中心可能在视锥外，
        // 不开 noCulling 会在某些视角（玩家看脚下时）导致"有时候看不到"
        this.noCulling = true;
        mode = getParasolMode();
    }

    public EntityYuukaParasol(Level level)
    {
        this(THKaguyaEntities.YUUKA_PARASOL.get(), level);
    }

	/** 出現時に呼び出されるコンストラクト */
    public EntityYuukaParasol(Level level, Player entityPlayer, int da)
    {
        this(level);
    	setParasolMode(0);
    	// force = true：プレイヤーを載せ替える際、canAddPassenger の既定判定に弾かれないようにする
    	this.startRiding(entityPlayer, true);
    	user = entityPlayer;
    	double newX = user.getX() - Math.sin((user.getYRot() - 23F) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * 0.5D;
    	double newY = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * Math.cos(-23F / 180F * 3.141593F) * 0.5D + user.getEyeHeight() + 1.6D;
    	double newZ = user.getZ() + Math.cos((user.getYRot() - 23F) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * 0.5D;
        setPos(newX, newY, newZ);//初期位置を設定(x,y,z)
    	setRot(user.getYRot(), 0.0F);
    	damage = da;
    }

	/** 生成時に一度だけ呼ばれる処理 */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
    	builder.define(PARASOL_MODE, 0);
    }

	/*
	 * 描画判定のチェック（原版 isInRangeToRenderDist）
	 */
	@Override
	public boolean shouldRenderAtSqrDistance(double distance)
    {
        double d1 = this.getBoundingBox().getSize() * 4.0D;
        d1 *= 64.0D;
        return distance < d1 * d1;
    }

	/**
	 * 他の物体と衝突したときのその物体の当たり判定？
	 * （原版 getCollisionBox：使用者とは衝突しない）
	 */
    @Override
    public boolean canCollideWith(Entity entity)
    {
        return entity != user;
    }

	/**
	 * 当たり判定を設定
	 *
	 * <p>原版は {@code setSize(0.1F, 0.1F)} したうえで {@code getBoundingBox()} を
	 * 「posX±1.0 / posY±1.0 / posZ±1.0」の 2×2×2 に上書きしていた。
	 * 1.20.1 では {@code getBoundingBox()} の上書きが衝突判定に効かず（各所が {@code this.bb} を直接参照）、
	 * さらに {@code getBoundingBoxForPose} は {@code refreshDimensions()} の時しか評価されないため、
	 * 実際の寸法を {@link #getDimensions(Pose)} で与える。
	 * これにより当たり判定 2×2×2 と描画距離（{@code getSize()*4*64} → 512 格）も原版どおりになる。
	 */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(Pose pose)
    {
    	return net.minecraft.world.entity.EntityDimensions.scalable(2.0F, 2.0F);
    }

	/**
	 * 押すことができるかどうか
	 */
    @Override
    public boolean isPushable()
    {
        return false;
    }

	/**
	 * 当たり判定の有無　falseだと右クリックの選択ですらできない。trueなら当たり判定が出現する
	 */
	@Override
    public boolean isPickable()
    {
        return true;
    }

	/** 毎tick呼ばれる処理 */
	@Override
    public void tick()
    {
    	super.tick();

    	//使用者がいないなら終了処理
    	if(!level().isClientSide && (user == null || !user.isAlive()) )
    	{
    		THKaguyaLib.itemEffectFinish(this, user, THKaguyaItems.YUUKA_PARASOL.get(), damage);
    		return;
    	}

    	float posAngle = -23F;
		double distance = 1.0D;

    	//使用者がいるなら
    	if(user != null)
    	{
    		// 何らかの理由でプレイヤーから降ろされてしまった場合は載せ直す。
    		// これがないと、乗客として同期されない（＝その場に取り残される）ことがある。
    		if(!level().isClientSide && !this.isPassenger())
    		{
    			this.startRiding(user, true);
    		}

			if(getParasolMode() == 0)
			{
				distance = 0.5F;
				setXRot(-user.getXRot());
			}
			else if(getParasolMode() == 1)
			{
				posAngle = -23F;
				setXRot(-user.getXRot() + 40F);
			}
			else
			{
				posAngle = -23F;
				setXRot(-user.getXRot() - 90F);
				distance = 0.7D;
			}

    		if(getParasolMode() == 0)
    		{
    			//使用者が地面に立っているなら
    			if(user.isShiftKeyDown() && tickCount > 10)
    			{
    				setParasolMode(1);
    				tickCount = 0;
    			}
    			user.fallDistance *= 0.7F;
    		}
    		else if(getParasolMode() == 1)
    		{
    			if(user.isShiftKeyDown() && tickCount > 10)
    			{
    				setParasolMode(2);
    				tickCount = 0;
    			}
    		}
    		else
    		{
    			if(user.isShiftKeyDown() && tickCount > 10)
    			{
    				THKaguyaLib.itemEffectFinish(this, user, THKaguyaItems.YUUKA_PARASOL.get(), damage);
    			}
    		}
    		setYRot(user.getYRot());

    		//今フレームの移動量を保存
    		Vec3 motion = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
    		setDeltaMovement(motion);

    		double newX = user.getX() - Math.sin((user.getYRot() + posAngle) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * distance;
    		double newY = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * Math.cos(posAngle / 180F * 3.141593F) * distance + user.getEyeHeight() + 1.6D;
    		double newZ = user.getZ() + Math.cos((user.getYRot() + posAngle) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * distance;

    		newX += motion.x;
    		newY += motion.y;
    		newZ += motion.z;

    		setPos(newX, newY, newZ);
    	}

    	if(!level().isClientSide && damage >= DanmakuConstants.PARASOL_MAX_DAMAGE)//ダメージが武器の耐久を越したら
    	{
    		discard();//消滅
    	}

    	setRot(getYRot(), getXRot());
    }

    //乗っているEntityの処理（原版 updateRidden -> 1.20.1 rideTick）
    //ここでは使用者の処理。使用者のmotionなどは恐らくここでしかまともに操作できない
    @Override
    public void rideTick()
	{
    	Entity vehicle = getVehicle();
		if(vehicle != null)
		{
			if(getParasolMode() == 0)
			{
				easyFalling();
			}
			Vec3 vehicleMotion = vehicle.getDeltaMovement();
			vehicle.setDeltaMovement(vehicleMotion.x * 0.95D, vehicleMotion.y, vehicleMotion.z * 0.95D);
			setDeltaMovement(vehicleMotion);
		}
		else
		{
			if(!level().isClientSide)
			{
				THKaguyaLib.itemEffectFinish(this, user, THKaguyaItems.YUUKA_PARASOL.get(), damage);
			}
		}

		// EntityHisou と同じ理由（1.20.1 では乗客は rideTick() 経由でしか tick() が呼ばれない）：
		// ここで tick() を駆動しないと、tick() 内の終了処理（discard）と
		// 追従処理（itemEffectFollowUser）が一切走らず、傘が消えずに残留する。
		// Entity.rideTick() が行う positionRider() は、tick() 内の追従配置を
		// 上書きしないよう意図的に通していない。
		if(this.isAlive())
		{
			this.tick();
		}
	}

	protected void easyFalling()
	{
		Entity vehicle = getVehicle();
		if(vehicle != null && vehicle.getDeltaMovement().y < 0.0D)
		{
			Vec3 vehicleMotion = vehicle.getDeltaMovement();
			vehicle.setDeltaMovement(vehicleMotion.x, vehicleMotion.y * 0.7D, vehicleMotion.z);
		}
	}

	/** 保存するデータの書き込み */
	@Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    	tag.putInt("damage", damage);
    }

	/** 保存したデータの読み込み */
	@Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    	damage = tag.getInt("damage");
    }

	/**
	 * 日傘のモードチェンジ
	 * @param mode チェンジするモード
	 */
    public void setParasolMode(int mode)
	{
		this.entityData.set(PARASOL_MODE, mode);
	}

    /**
     * パラソルのモードを返す
     * @return パラソルのモード
     */
	public int getParasolMode()
	{
		return this.entityData.get(PARASOL_MODE);
	}
}
