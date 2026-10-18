package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 銀のナイフ */
public class EntitySilverKnife extends Entity
{
	private final byte KNIFE_COLOR_BLUE	= 0x00;
	private final byte KNIFE_COLOR_RED	= 0x01;
	private final byte KNIFE_COLOR_GREEN	= 0x02;
	private final byte KNIFE_COLOR_WHITE	= 0x03;

	// 同步数据（1.7 dataWatcher 18/19/20）
	private static final EntityDataAccessor<Integer> KNIFE_COUNT =
			SynchedEntityData.defineId(EntitySilverKnife.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> KNIFE_COLOR =
			SynchedEntityData.defineId(EntitySilverKnife.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> KNIFE_Z_ANGLE =
			SynchedEntityData.defineId(EntitySilverKnife.class, EntityDataSerializers.INT);

	private byte color;//ナイフの色
	private int xTile;//Xの整数座標
    private int yTile;//Yの整数座標
    private int zTile;//Zの整数座標
    // 1.7: int inTile（今いる場所のブロックID）/ int inData（メタデータ）
    // 1.20.1 に数字ブロックIDは存在しないため、刺さっているブロックの BlockState を直接保持する
    private BlockState inState;
    private boolean inGround;//地中にいるか
    private int ticksInAir;

    /** 使用者 */
    public LivingEntity shootingEntity;
	public LivingEntity playerEntity;

	private int ticksInGround;
    public double accelerationX;
    public double accelerationY;
    public double accelerationZ;

	private boolean hit;//衝突済み判定
	private boolean rockhit;//刺さらないブロックの上に乗っているかの判定

	public float angleZ;

	private int lastTime;
	private double lastShotMotionX;
	private double lastShotMotionY;
	private double lastShotMotionZ;
	private int delay;

	public EntitySilverKnife(EntityType<?> type, Level level)
	{
		super(type, level);
        xTile = -1;
        yTile = -1;
        zTile = -1;
        inState = null;
        inGround = false;
    	// setSize(0.1F, 0.1F); // 1.20.1: サイズは EntityType 登録時に決定
    	hit = false;
	}

	/** ワールド読み込み時に呼び出されるコンストラクト */
    public EntitySilverKnife(Level level)
    {
        this(THKaguyaEntities.SILVER_KNIFE.get(), level);
    }

    public EntitySilverKnife(Level level, Vec3 pos, Vec3 angle, double speed, int t)
    {
    	this(level, null, pos, angle, speed, t);
    }

    public EntitySilverKnife(Level level, LivingEntity entityLivingBase, Vec3 pos, Vec3 angle, double speed, int t)
    {
        this(level);
    	color = (byte)t;//ナイフの色を保存
    	setKnifeColor(color);
        shootingEntity = entityLivingBase;
    	playerEntity = entityLivingBase;

    	double motionX = angle.x * speed;
    	double motionY = angle.y * speed;
    	double motionZ = angle.z * speed;
    	setDeltaMovement(motionX, motionY, motionZ);
    	lastShotMotionX = motionX;
    	lastShotMotionY = motionY;
    	lastShotMotionZ = motionZ;
    	accelerationX = (motionX/2.0);
        accelerationY = (motionY/2.0);
        accelerationZ = (motionZ/2.0);
    	setKnifeZAngle(90.0F);
		float yaw = (float)Math.atan2(motionX, -motionZ) / 3.141593F * 180F;
    	float pitch = (float)Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)) / 3.141593F * 180F;
    	setYRot(yaw);
    	setXRot(pitch);
    	// 1.7: setLocationAndAngles(x, y, z, yaw, pitch)
    	moveTo(	pos.x + angle.x * speed, pos.y + angle.y * speed, pos.z + angle.z * speed,
    			yaw, pitch);
    	// 1.7: prevPosX = posX 等
    	this.xo = getX();
    	this.yo = getY();
    	this.zo = getZ();
    	lastTime = 0;

    	delay = 0;
    	if(t == 3)
    	{
    		delay = -30;
    		//setKnifeCount(-30);
    	}
    }

	// 1.7: protected boolean canTriggerWalking() -> 1.20.1 には存在しないため削除

	/**
	 * 押すことができるかどうか
	 * @return true : 押せる
	 */
    @Override
    public boolean isPushable()
    {
    	//押せない
    	return false;
    }

	// 1.7: getCollisionBox(Entity) / getBoundingBox() を null 返しでオーバーライドしていた
	// （当たり判定を持たない）。1.20.1 では getBoundingBox() を null にするとクラッシュするため削除し、
	// 当たり判定の細さは EntityType 登録時のサイズで表現する。

    @Override
    public boolean shouldRenderAtSqrDistance(double d)
    {
        double d1 = getBoundingBox().getSize() * 4D;
        d1 *= 64D;
        return d < d1 * d1;
    }

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
    protected void defineSynchedData()
    {
		this.entityData.define(KNIFE_COUNT, 0);
    	this.entityData.define(KNIFE_COLOR, (byte)0);
    	this.entityData.define(KNIFE_Z_ANGLE, 0);
    }


	/**
	 * 移動量から速度を取得する
	 * @return 速度を返す
	 */
	public double getEntitySpeed()
	{
		Vec3 m = getDeltaMovement();
		return Math.sqrt(m.x * m.x + m.y * m.y + m.z * m.z);
	}

	/** 消滅させる */
	public void delete()
	{
		if(!level().isClientSide)
		{
			discard();//消滅させる
		}
	}

	/** 毎tick呼ばれる処理 */
	@Override
    public void tick()
    {
        super.tick();

        if(tickCount > 1)
        {
        	// 時間停止空間（EntitySakuyaWatch#inPrivateSquare）の中では
        	// hitEntity.tickCount-- で年齢が巻き戻される。その結果 tickCount が
        	// 前 tick の値（lastTime）以下になるので「止まっている」と判定できる。
        	// 原版 EntitySilverKnife.java:187-191 と同じ仕組み。
        	if(tickCount <= lastTime)
        	{
        		setKnifeCount(getKnifeCount());
        		return;
        	}
	        //時間が通常通り流れているなら
			Vec3 motion = new Vec3(lastShotMotionX, lastShotMotionY, lastShotMotionZ);
			setDeltaMovement(motion);
			if(!level().isClientSide)
		    {
		    	delay ++;
		    	if(delay >= 0)
		    	{
		    		setKnifeCount(getKnifeCount() + 1);
		    	}
		    }
        }

        if(getKnifeCount() < 0)
        {
        	setKnifeCount(getKnifeCount() + 1);
        	return;
        }

        //どこともぶつからずに長時間経過した場合、消滅させる
    	if(!hit && tickCount >= 300)
    	{
    		delete();
    	}
    	//ぶつかっている場合は、かなり長い間停滞する
    	else if(hit && tickCount >= 6000)
    	{
    		delete();
    	}

    	//白色のナイフで、壁に衝突していたら消滅させる
    	if(getKnifeColor() == 3 && hit && tickCount >= 1)
    	{
    		delete();
    	}

    	rockhit = false;//硬いブロックと衝突しているかの判定変数

    	//地面の中なら
        if (inGround)
        {
        	//衝突しているブロックを取得
            BlockState hitState = level().getBlockState(new BlockPos(xTile, yTile, zTile));

            if (inState == null || hitState != inState)
            {
                inGround = false;
                Vec3 m = getDeltaMovement();
                setDeltaMovement(
                		m.x * random.nextFloat() * 0.2F,
                		m.y * random.nextFloat() * 0.2F,
                		m.z * random.nextFloat() * 0.2F);
            	ticksInGround = 0;
                setKnifeCount(3);
            }
			else
        	{
        		ticksInGround++;//地中カウンタを増やす

            	if (ticksInGround == 3000)//地中カウンタが3000なら消滅
            	{
            		delete();
            	}
        	}

            return;//これ以上の処理は行わない
        }
        else
        {
        	ticksInAir = 5;
        }

        Vec3 motion = getDeltaMovement();

    	//始点を登録
    	Vec3 posStart = THShotLib.pos_Entity(this);
    	//終点を登録
    	Vec3 posEnd = THShotLib.pos( getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        //始点と終点からブロックとの衝突を取得
    	// 1.7: worldObj.func_147447_a(posStart, posEnd, false, true, false)
    	HitResult hitResult = level().clip(new ClipContext(posStart, posEnd,
    			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

    	posStart = THShotLib.pos_Entity(this);
    	posEnd = THShotLib.pos( getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        //ブロックと衝突していたなら
    	if (hitResult.getType() != HitResult.Type.MISS)
        {
        	//終点を衝突した点に変更
        	posEnd = hitResult.getLocation();
        }

        Entity hitEntity = null;
    	//このEntityから移動後までの線分に、指定分の範囲を追加した直方体と衝突するEntityのリストを取得
        List<Entity> list = level().getEntities(this,
        		getBoundingBox().expandTowards(motion.x, motion.y, motion.z).inflate(0.01D, 0.01D, 0.01D));
        //現在最も近い距離を保存
        double nearDistance = 999.0D;

        for (int j = 0; j < list.size(); j++)
        {
        	// 衝突リストから、i番目のEntityを取得
            Entity entityCheck = list.get(j);

    		if(	entityCheck.isPickable() && // 当たり判定を持ったEntityであるか？
    			entityCheck != shootingEntity   && // 使用者ではないか？
    			ticksInAir >= 5                 &&
    			!(entityCheck instanceof EntityTHShot))//弾幕には当たらない
    		{
    			if(entityCheck instanceof EntitySilverKnife)
    			{
    				EntitySilverKnife entitySilverKnife = (EntitySilverKnife)entityCheck;
    				if(this.getKnifeColor() != 3  && entitySilverKnife.getKnifeColor() != 3)
    				{
    	        		float boxExtend = 0.01F;
    	            	AABB axisalignedbb = entityCheck.getBoundingBox().inflate(boxExtend, boxExtend, boxExtend);
    	            	Vec3 interceptVec = axisalignedbb.clip(posStart, posEnd).orElse(null);

    	        		//Entityとの衝突がないなら、このEntityはパスする
    	            	if (interceptVec != null)
    	            	{
    	        			//始点からEntityに衝突した点までの距離を取得
    	            		double distance = posStart.distanceTo(interceptVec);
    	        			//今までで一番近いEntityなら、Entityと距離を記憶する
    	            		if (distance < nearDistance || nearDistance == 999.0D)
    	            		{
    	                		hitEntity = entityCheck;
    	                		nearDistance = distance;
    	            		}
    	            	}
    				}
    			}
    			else
    			{
	        		float boxExtend = 0.1F;
	            	AABB axisalignedbb = entityCheck.getBoundingBox().inflate(boxExtend, boxExtend, boxExtend);
	            	Vec3 interceptVec = axisalignedbb.clip(posStart, posEnd).orElse(null);
	        		//Entityとの衝突がないなら、このEntityはパスする
	            	if (interceptVec != null)
	            	{
	        			//始点からEntityに衝突した点までの距離を取得
	            		double d1 = posStart.distanceTo(interceptVec);
	        			//今までで一番近いEntityなら、Entityと距離を記憶する
	            		if (d1 < nearDistance || nearDistance == 999.0D)
	            		{
	                		hitEntity = entityCheck;
	                		nearDistance = d1;
	            		}
	            	}
            	}
            }
        }

    	//Entityに当たっていたなら
        if (hitEntity != null)
        {
            hitResult = new EntityHitResult(hitEntity);
        }

    	if (hitResult instanceof EntityHitResult entityHit
    			&& entityHit.getEntity() instanceof Player entityPlayer)
        {
            //if (entityPlayer.capabilities.disableDamage || this.shootingEntity instanceof EntityPlayer && !((EntityPlayer)this.shootingEntity).func_96122_a(entityPlayer))
            if (entityPlayer.getAbilities().invulnerable || this.shootingEntity instanceof Player /*&& !((EntityPlayer)this.shootingEntity).func_96122_a(entityPlayer)*/)
            {
            	hitResult = null;
            }
        }

    	if (hitResult != null)
        {
        	Entity entityHit2 = hitResult instanceof EntityHitResult ehr ? ehr.getEntity() : null;
        	//Entityに当たっていたなら
            if (entityHit2 != null)
            {
            	//当たったEntityがEntitySilverKnifeに属していなければ
            	if(!(entityHit2 instanceof EntitySilverKnife)  )
            	{
            		//衝突処理
            		onImpact(hitResult);
            		//
            		Vec3 m = getDeltaMovement();
            		//ナイフの動きを鈍らせ、逆方向に働かせる
            		setDeltaMovement(
            				m.x * -0.210000000149011612D,
            				m.y * -0.210000000149011612D,
            				m.z * -0.210000000149011612D);
            		accelerationX *= -0.01;
            		accelerationY *= -0.01;
            		accelerationZ *= -0.01;
            		setYRot(getYRot() + 180F);
            		yRotO += 180F;
            		//生存カウンタを3まで戻す
            		setKnifeCount(3);
            		//衝突チェックをtrueにする
                	hit = true;
            		setKnifeZAngle(0.0F);
            	}
            }

            // 1.7: MovingObjectPosition.blockX/Y/Z（Entity命中時は未初期化=0,0,0 だった）
            BlockPos hitBlockPos = hitResult instanceof BlockHitResult bhr ? bhr.getBlockPos() : BlockPos.ZERO;
            xTile = hitBlockPos.getX();
            yTile = hitBlockPos.getY();
            zTile = hitBlockPos.getZ();
            inState = level().getBlockState(hitBlockPos);
            // 1.7: sideHit（int。Entity命中時は 0=下面扱いだった）
            Direction sideHit = hitResult instanceof BlockHitResult bhr2 ? bhr2.getDirection() : Direction.DOWN;
            Vec3 hitVec = hitResult.getLocation();

            //当たったブロックの材質を取得
            // 1.7: Material material = worldObj.getBlock(xTile, yTile, zTile).getMaterial();
            // 1.20.1 に Material は存在しないため SoundType + 氷ブロックで近似（isHardMaterial 参照）

            //当たったブロックの材質が、rock,iron,glass,iceなら
            if( isHardMaterial(inState) )
            {
            	//速度が遅すぎでなければ
            	if(Math.abs(getEntitySpeed()) > 0.08F)
            	{
            		//銀のナイフに衝突しているなら
            		if(entityHit2 instanceof EntitySilverKnife entitySilverKnife)
            		{
            			//どちらかが白のナイフでなければ
            			if(entitySilverKnife.getKnifeColor() != 3 && this.getKnifeColor() != 3)
            			{
            				//衝突時の金属音のような音を出す
                			level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 5.0F);// 原: "random.orb"

	            			//衝突処理をし、少しランダムに向きを変える
	            			float randangle = (float)random.nextInt(120);
	            			setYRot(getYRot() + 165.0F - 15.0F + randangle);
	            			Vec3 m2 = getDeltaMovement();
	            			setDeltaMovement(
	            					(m2.x + Math.sin(randangle)) * -0.150000000149011612D * Math.sin(randangle),//移動方向を逆にする
	            					m2.y * -0.150000000149011612D,
	            					(m2.z + Math.cos(randangle)) * -0.150000000149011612D * Math.cos(randangle));
	            			accelerationX *= -0.01;
		            		accelerationZ *= -0.01;
	            			//このナイフと当たったナイフの上下関係で速度に変化が出る
	            			if(getY() > entityHit2.getY())
	            			{
	            				accelerationY *=  0.01;
	            			}
	            			else
	            			{
	            				accelerationY *= -1.2;
	            			}
            			}
            		}
            		//硬いブロックに当たっているなら
            		if(entityHit2 == null)
            		{
            			//衝突時の金属音のような音を出す
            			level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 5.0F);// 原: "random.orb"

            			//跳ね返り係数
            			double returnRate = 0.25D * getEntitySpeed();

            			if(getKnifeColor() == 0)
            			{
            				returnRate = 0.01D;
            			}

            			Vec3 m3 = getDeltaMovement();
            			double supPosX1 = getX();
            			double supPosX2 = getX() + m3.x;
            			double supPosY1 = getY();
            			double supPosY2 = getY() + m3.y;
            			double supPosZ1 = getZ();
            			double supPosZ2 = getZ() + m3.z;
            			//始点を登録
            			Vec3 supVec3d1 = new Vec3(supPosX1, supPosY1, supPosZ1);
    					//終点を登録
    					Vec3 supVec3d2 = new Vec3(supPosX2, supPosY2, supPosZ2);
        				//始点と終点からブロックとの衝突を取得
    					//MovingObjectPosition supMovingObjectPosition = worldObj.rayTraceBlocks_do_do(supVec3d1, supVec3d2, false, true);
    					BlockHitResult supHit = level().clip(new ClipContext(supVec3d1, supVec3d2,
    							ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

    					//何かにあたっている間
            			while(supHit != null && supHit.getType() != HitResult.Type.MISS)
            			{
            				returnRate = 0.25D * getEntitySpeed();
            				if(getKnifeColor() != 1)
            				{
            					returnRate = 0.00001D;
            				}
            				//衝突したブロックの面ごとに判定
            				// 1.7: sideHit 0=下面 1=上面 2/3=Z面 4/5=X面
            				Vec3 m4 = getDeltaMovement();
            				switch(supHit.getDirection())
	            			{
	            				case DOWN://下面
	            					setDeltaMovement(m4.x, m4.y * -returnRate, m4.z);
	            					accelerationY *= -returnRate;
	            					break;
	            				case UP://上面
	            					setDeltaMovement(m4.x, m4.y * -returnRate, m4.z);
	            					accelerationY *= -returnRate;
	            					setKnifeCount(0);
	            					if(m4.y < 0.2D)
	            					{
	            						setDeltaMovement(m4.x * 0.3D, m4.y * -returnRate, m4.z * 0.3D);
	            					}
	            					if(getKnifeZAngle() > 0.0F)
	            					{
	            						setKnifeZAngle(getKnifeZAngle() - 21.0F);
	            					}
	            					else
	            					{
	            						setKnifeZAngle(0.0F);
	            					}
	            					break;
	            				case NORTH:
	            				case SOUTH:
	            					setDeltaMovement(m4.x, m4.y, m4.z * -returnRate);
	            					accelerationZ *= -returnRate;
	            					break;
	            				case WEST:
	            				case EAST:
	            				default:
	            					setDeltaMovement(m4.x * -returnRate, m4.y, m4.z);
	            					accelerationX *= -returnRate;
	            					break;
	            			}
            				if(getKnifeColor() != 1)
            				{
            					Vec3 m5 = getDeltaMovement();
            					setDeltaMovement(m5.x * 0.3D, m5.y * 0.3D, m5.z * 0.3D);
            					accelerationX *= 0.3D;
            					accelerationY *= 0.3D;
            					accelerationZ *= 0.3D;
            				}
            				//rotationYaw += 180F;//向きを180度変える
            				supPosX1 = supPosX1 + (supHit.getLocation().x - supPosX1) * 0.999D;
            				supPosY1 = supPosY1 + (supHit.getLocation().y - supPosY1) * 0.999D;
            				//supPosY1 = supMovingObjectPosition.hitVec.yCoord;
            				supPosZ1 = supPosZ1 + (supHit.getLocation().z - supPosZ1) * 0.999D;
            				Vec3 m6 = getDeltaMovement();
            				supPosX2 = supPosX1 + m6.x;
            				supPosY2 = supPosY1 + m6.y;
            				supPosZ2 = supPosZ1 + m6.z;
            				//始点を登録
            				supVec3d1 = new Vec3(supPosX1, supPosY1, supPosZ1);
    						//終点を登録
    						supVec3d2 = new Vec3(supPosX2, supPosY2, supPosZ2);
        					//始点と終点からブロックとの衝突を取得
    						//supMovingObjectPosition = worldObj.rayTraceBlocks_do_do(supVec3d1, supVec3d2, false, true);
    						supHit = level().clip(new ClipContext(supVec3d1, supVec3d2,
    								ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    						//count++;
            			}
            		}
            		Vec3 m7 = getDeltaMovement();
            		double newMotionX = Math.abs(m7.x) < 0.1D ? 0.0D : m7.x;
            		double newMotionZ = Math.abs(m7.z) < 0.1D ? 0.0D : m7.z;
            		setDeltaMovement(newMotionX, m7.y, newMotionZ);
	            	setKnifeCount(3);
            		hit = false;
            	}
            	//速度が遅すぎて、かつ、衝突先のブロックが空気でなければ
            	else if(!inState.isAir())
            	{
            		//遅すぎる場合はほぼ地面についていると考える　　・・・これ、空中停止バグの原因じゃ・・・
            		//角度を水平にする
            		if(sideHit == Direction.UP)
            		{
            			setXRot(0F);
            			Vec3 m8 = new Vec3(hitVec.x - getX(), hitVec.y - getY(), hitVec.z - getZ());
	            		accelerationX = 0.0D;
	            		accelerationY = 0.0D;
	            		accelerationZ = 0.0D;
	            		//当たったブロックとナイフの距離
		            	double f2 = Math.sqrt(m8.x * m8.x + m8.y * m8.y + m8.z * m8.z);
	            		//めり込んだ分だけ引き戻し
		            	setPos(getX() - (m8.x / f2) * 0.05000000074505806D,
		            		   getY() - (m8.y / f2) * 0.05000000074505806D,
		            		   getZ() - (m8.z / f2) * 0.05000000074505806D);
	            		//地中チェックをtrue
		            	inGround = true;
	            		//衝突チェックをtrue
		            	hit = true;
	            		//石衝突チェックをtrue
	            		rockhit = true;
            			setKnifeCount(0);
            			setKnifeZAngle(0.0F);
            		}
            		else
            		{
            			//rotationPitch = 0F;
            			Vec3 m8 = new Vec3(hitVec.x - getX(), hitVec.y - getY(), hitVec.z - getZ());
	            		accelerationX *= -0.01;
	            		accelerationY *= -0.01;
	            		accelerationZ *= -0.01;
	            		//当たったブロックとナイフの距離
		            	double f2 = Math.sqrt(m8.x * m8.x + m8.y * m8.y + m8.z * m8.z);
	            		//めり込んだ分だけ引き戻し
		            	setPos(getX() - (m8.x / f2) * 0.05000000074505806D,
		            		   getY() - (m8.y / f2) * 0.05000000074505806D,
		            		   getZ() - (m8.z / f2) * 0.05000000074505806D);
	            		//地中チェックをtrue
		            	inGround = true;
	            		//衝突チェックをtrue
		            	hit = true;
	            		//石衝突チェックをtrue
	            		rockhit = true;
            		}
            	}
            	else
            	{
            		hit = false;
            	}
            }
        	//衝突先のブロックの材質が、硬いものでなく、空気でもなければ
            else if(!inState.isAir())
            {
            	//その場で刺さって停止
            	Vec3 m9 = new Vec3(hitVec.x - getX(), hitVec.y - getY(), hitVec.z - getZ());
            	accelerationX *= -0.01;
            	accelerationY *= -0.01;
            	accelerationZ *= -0.01;
	            float f2 = Mth.sqrt((float)(m9.x * m9.x + m9.y * m9.y + m9.z * m9.z));//当たったブロックとナイフの距離
	            setPos(getX() - (m9.x / (double)f2) * 0.05000000074505806D,//めり込んだ分だけ引き戻し
	            	   getY() - (m9.y / (double)f2) * 0.05000000074505806D,
	            	   getZ() - (m9.z / (double)f2) * 0.05000000074505806D);
	            //worldObj.playSoundAtEntity(this, "mob.blaze.hit1", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
	            inGround = true;
            	setKnifeCount(3);
	            hit = true;
	          //衝突時の金属音のような音を出す
    			//worldObj.playSoundAtEntity(this, worldObj.getBlock(xTile, yTile, zTile), 0.5F, 5.0F);
            }
        }

    	if(delay < 0)
    	{
    		return;
    	}

    	if(!level().isClientSide && getKnifeCount() > 2 && delay >= 0)
    	{
    		Vec3 m10 = getDeltaMovement();
    		setPos(getX() + m10.x, getY() + m10.y, getZ() + m10.z);
    	}

    	//緑のナイフの白ナイフ出現処理
    	if(getKnifeColor() == 2 && Math.abs(getEntitySpeed()) > 0.08F && tickCount < 60 && tickCount > 2 && tickCount % 2 == 1)
    	{
    		Vec3 m11 = getDeltaMovement();
        	Vec3 angle = THShotLib.angle(m11.x, m11.y, m11.z);
        	// 1.7 は getVectorNomalize が破壊的変更だったが、1.20.1 の Vec3 は不変なので戻り値を受ける
        	angle = THShotLib.getVectorNomalize(angle);

        	if(!level().isClientSide)
        	{
        		EntitySilverKnife knife;
        		if(this.shootingEntity == null || this.playerEntity == null)
        		{
        			knife = new EntitySilverKnife(level(), THShotLib.pos_Entity(this), angle, getEntitySpeed(), 3);
        			level().addFreshEntity(knife);
        		}
        		else
        		{
        			// 1.7: THKaguyaLib.createSilverKnife(shootingEntity, pos, angle, speed, 3, 0)
        			// （内部で new + setKnifeCount + spawn している）を展開
        			knife = new EntitySilverKnife(level(), shootingEntity, THShotLib.pos_Entity(this), angle, getEntitySpeed(), 3);
        			knife.setKnifeCount(0);
        			level().addFreshEntity(knife);
        		}
        		knife.accelerationX = this.accelerationX;
        		knife.accelerationY = this.accelerationY;
        		knife.accelerationZ = this.accelerationZ;

        	}
    	}

    	Vec3 m12 = getDeltaMovement();
        float f = Mth.sqrt((float)(m12.x * m12.x + m12.z * m12.z));
        float newYaw = (float)((Math.atan2(m12.x, -m12.z) * 180D) / 3.1415927410125732D);
        setYRot(newYaw);
        //for (rotationPitch = (float)((Math.atan2(motionY, f) * 180D) / 3.1415927410125732D); rotationPitch - prevRotationPitch < -180F; prevRotationPitch -= 360F) { }
        float newPitch = getXRot();
        if(!level().isClientSide)
        {
        	for (newPitch = (float)((Math.atan2(m12.y, f) * 180D) / 3.1415927410125732D); newPitch - xRotO < -180F; xRotO -= 360F) { }
        }
        for (; newPitch - xRotO >= 180F; xRotO += 360F) { }
        for (; newYaw - yRotO < -180F; yRotO -= 360F) { }
        for (; newYaw - yRotO >= 180F; yRotO += 360F) { }
        setXRot(xRotO + (newPitch - xRotO) * 1.0F);

    	if(rockhit == true)
    	{
    		setXRot(0F);
    	}

    	setYRot(yRotO + (newYaw - yRotO) * 1.0F);
        float slowDownRate = 0.95F;//減速率
        if (isInWater())//水に入っている場合
        {
            slowDownRate = 0.8F;//さらに減速率を上げる
        }

        if(delay >= 0/*getKnifeCount() >= 0*/)
        {
	        Vec3 m13 = getDeltaMovement().add(accelerationX, accelerationY, accelerationZ);

	    	//重力処理
	        //if(lastTime < getKnifeCount())
	        {
	        	m13 = m13.add(0.0D, -0.008 * getKnifeCount(), 0.0D);
	        }

	        setDeltaMovement(m13.x * slowDownRate, m13.y * slowDownRate, m13.z * slowDownRate);
        }

        setPos(getX(), getY(), getZ());

		if(tickCount > lastTime)
    	{
    		lastTime = tickCount;
    		Vec3 m14 = getDeltaMovement();
    		lastShotMotionX = m14.x;
    		lastShotMotionY = m14.y;
    		lastShotMotionZ = m14.z;
    	}
    }

	//衝突したときの処理
	protected void onImpact(HitResult hitResult)
    {
		Entity entityHit = hitResult instanceof EntityHitResult ehr ? ehr.getEntity() : null;
    	//当たったEntityがnullでなければ
    	if ( !level().isClientSide && entityHit != null )
        {
        	//当たったEntityがナイフを投げたEntityでなければ
            if ( entityHit != shootingEntity )
            {
            	float damage = 4.0F;
            	//当たったEntityがEntityLivingBaseに属しているなら
            	if(entityHit instanceof LivingEntity entityLivingBase)
            	{
            		//当たったEntityLivingBaseが無敵時間なら
            		if(entityLivingBase.hurtTime > 0)
            		{
            			boolean isLive = false;
            			if(entityLivingBase.getHealth() > 0.0F)
            			{
            				isLive = true;
            			}
            			//強引にダメージを入れ込む（おいw
            			entityLivingBase.heal(-damage);
            			if(entityLivingBase.getHealth() <= 0.0F && isLive)
            			{
            				// 1.7: onDeath(DamageSource.causeMobDamage(shootingEntity))
            				DamageSource deathSource = shootingEntity != null
            						? level().damageSources().mobAttack(shootingEntity)
            						: level().damageSources().generic();
            				entityLivingBase.die(deathSource);
            			}
            			//衝突チェックをtrueにする
            			hit = true;
            		}
            		//当たったEntityLivingBaseが無敵時間でないなら
            		else
            		{
            			//ハート2.0の通常ダメージ
            			DamageSource damageSource = shootingEntity != null
            					? level().damageSources().mobAttack(shootingEntity)
            					: level().damageSources().generic();
            			entityHit.hurt(damageSource, damage);
            		}
            	}
            }
        }
    }

	/**
	 * 保存するデータの書き込み
	 * @param tag : NTBタグ
	 */
	@Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putShort("xTile", (short)xTile);
        tag.putShort("yTile", (short)yTile);
        tag.putShort("zTile", (short)zTile);
    	// 1.7: "inTile"(ブロックID)/"inData"(メタデータ)。1.20.1 には数字IDがないため保存しない。
    	// 刺さっているブロックは inState フィールドで保持し、読み込み後は現在位置の BlockState で再初期化する。
    	//nbtTagCompount.setByte("shootingEntity;", (byte)inData);

        tag.putByte("inGround", (byte)(inGround ? 1 : 0));

    	tag.putShort("color", (byte)getKnifeColor());
    	tag.putShort("zAngle",(short)(getKnifeZAngle() * 10000.0F));
    }

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        xTile = tag.getShort("xTile");
        yTile = tag.getShort("yTile");
        zTile = tag.getShort("zTile");
    	// 1.7: inTile/inData（上記 addAdditionalSaveData のコメント参照）
        inGround = tag.getByte("inGround") == 1;
        inState = null;// tick 内で現在位置の BlockState と再比較される

    	setKnifeColor(tag.getByte("color"));
    	setKnifeZAngle((float)tag.getShort("zAngle") / 10000.0F);

    }

    /**
     * 刺さっているものなら回収可能
     * @param player : 落ちているナイフに触れたプレイヤー
     */
	// 1.7: onCollideWithPlayer(EntityPlayer)
	@Override
    public void playerTouch(Player player)
    {
        if (!level().isClientSide && inGround && player.getInventory().add(new ItemStack(getKnifeItemByColor(getKnifeColor()), 1)))
        {
            level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 4.0F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);// 原: "random.pop"
            // 1.7: player.onItemPickup(this, 1)。1.20.1 では ItemEntity 専用のため take で代用
            player.take(this, 1);
            discard();
        }
    }

	/** ナイフの色から対応するアイテムを返す（0青 1紅 2緑 3白） */
	private static Item getKnifeItemByColor(int knifeColor)
	{
		switch(knifeColor)
		{
			case 1:  return THKaguyaItems.SILVER_KNIFE_RED.get();
			case 2:  return THKaguyaItems.SILVER_KNIFE_GREEN.get();
			case 3:  return THKaguyaItems.SILVER_KNIFE_WHITE.get();
			case 0:
			default: return THKaguyaItems.SILVER_KNIFE_BLUE.get();
		}
	}

	/** 1.7 の Material.rock / iron / glass / ice の近似判定 */
	private static boolean isHardMaterial(BlockState state)
	{
		SoundType soundType = state.getSoundType();
		return soundType == SoundType.STONE
				|| soundType == SoundType.METAL
				|| soundType == SoundType.GLASS
				|| state.is(Blocks.ICE)
				|| state.is(Blocks.PACKED_ICE)
				|| state.is(Blocks.BLUE_ICE)
				|| state.is(Blocks.FROSTED_ICE);
	}

	@Override
    public boolean isPickable()
    {
        return true;
    }

	// 1.7: getCollisionBorderSize() -> 1.20.1 には存在しないため削除

	// 1.7: attackEntityFrom(DamageSource, int)
	// ※ 1.7 では第2引数が int のため Entity.attackEntityFrom(DamageSource, float) を
	//    オーバーライドできていなかった（実質呼ばれないバグ）。1.20.1 では正しく hurt をオーバーライドする。
	@Override
    public boolean hurt(DamageSource damagesource, float amount)
    {
        markHurt();
        if (damagesource.getEntity() != null)
        {
            Vec3 vec3d = damagesource.getEntity().getLookAngle();
            if (vec3d != null)
            {
                setDeltaMovement(vec3d.x, vec3d.y, vec3d.z);
                accelerationX = vec3d.x * 0.10000000000000001D;
                accelerationY = vec3d.y * 0.10000000000000001D;
                accelerationZ = vec3d.z * 0.10000000000000001D;
            }
            if (damagesource.getEntity() instanceof LivingEntity)
            {
                shootingEntity = (LivingEntity)damagesource.getEntity();
            }
            return true;
        }
        else
        {
            return false;
        }
    }

	/**
	 * ナイフのカウントを設定
	 * @param count ナイフのカウント
	 */
	public void setKnifeCount(int count)
	{
		this.entityData.set(KNIFE_COUNT, count);
	}

	/**
	 * ナイフのカウントを取得
	 * @return ナイフのカウントを返す
	 */
	public int getKnifeCount()
	{
		return this.entityData.get(KNIFE_COUNT);
	}

	/**
	 * ナイフの色を設定
	 * @param color ナイフの色
	 */
	public void setKnifeColor(int color)
	{
		this.entityData.set(KNIFE_COLOR, (byte)color);
	}

	/**
	 * ナイフの色を取得
	 * @return ナイフの色を返す
	 */
	public byte getKnifeColor()
	{
		return this.entityData.get(KNIFE_COLOR);
	}

	//ナイフの奥行き方向への角度設定する
	public void setKnifeZAngle(float angle)
	{
		this.entityData.set(KNIFE_Z_ANGLE, (int)(angle * 10000.0F));
	}

	//ナイフの奥行き方向への角度を返す
	public float getKnifeZAngle()
	{
		return (float)this.entityData.get(KNIFE_Z_ANGLE) / 10000.0F;
	}

	// 1.7: getShadowSize() -> レンダラで処理。削除
	// TODO(阶段4): 影サイズ 0.0F の処理
	// 1.7: getEntityBrightness(float) -> レンダラで処理。削除
	// TODO(阶段4): 亮度処理
	// 1.7: getEntityBrightnessForRender(float) -> 0xf000f0。削除
	// TODO(阶段4): 亮度処理
}
