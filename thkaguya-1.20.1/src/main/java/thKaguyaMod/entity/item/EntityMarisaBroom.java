package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 魔法の箒 */
public class EntityMarisaBroom extends Entity
{
    private boolean isEmpty;
    private double speedMultiplier;
    private int boatPosRotationIncrements;
    private double broomX;
    private double broomY;
    private double broomZ;
    private double broomYaw;
    private double broomPitch;
    @OnlyIn(Dist.CLIENT)
    private double velocityX;
    @OnlyIn(Dist.CLIENT)
    private double velocityY;
    @OnlyIn(Dist.CLIENT)
    private double velocityZ;

    // 同步数据（1.7 dataWatcher 17/18/19）
    private static final EntityDataAccessor<Integer> TIME_SINCE_HIT =
            SynchedEntityData.defineId(EntityMarisaBroom.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORWARD_DIRECTION =
            SynchedEntityData.defineId(EntityMarisaBroom.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE_TAKEN =
            SynchedEntityData.defineId(EntityMarisaBroom.class, EntityDataSerializers.FLOAT);

    public EntityMarisaBroom(EntityType<?> type, Level level)
    {
        super(type, level);
        this.isEmpty = true;
        this.speedMultiplier = 0.07D;
        // 1.7: this.preventEntitySpawning = true; -> 1.20.1 には存在しないため削除
        // 1.7: this.setSize(0.9F, 0.5F); -> サイズは EntityType 登録時に決定
        // 1.7: this.yOffset = 0.8F; -> 1.20.1 には yOffset が存在しない。初期位置補正は定数 0.8D で代替
    }

    public EntityMarisaBroom(Level level)
    {
        this(THKaguyaEntities.MARISA_BROOM.get(), level);
    }

    public EntityMarisaBroom(Level level, double x, double y, double z)
    {
        this(level);
        this.setPos(x, y + 0.8D, z);// 1.7: setPosition(x, y + (double)this.yOffset, z)
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        // 1.7: prevPosX = x 等
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    // 1.7: protected boolean canTriggerWalking() -> 1.20.1 には存在しないため削除

    /** 生成時に一度だけ呼ばれる処理 */
    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(TIME_SINCE_HIT, 0);
        this.entityData.define(FORWARD_DIRECTION, 1);
        this.entityData.define(DAMAGE_TAKEN, 0.0F);
    }

    // 1.7: getCollisionBox(Entity) / getBoundingBox() のオーバーライド（entity.boundingBox を返す）->
    // 1.20.1 では当たり判定は bb フィールドで管理されるため削除

    /**
     * Returns true if this entity should push and be pushed by other entities when colliding.
     */
    @Override
    public boolean isPushable()
    {
        return true;
    }



	//魔理沙の箒に乗っているEntityが乗る高さを返す
	// 1.7: getMountedYOffset()
    @Override
    public double getPassengersRidingOffset()
    {
        return  -0.15D;
    }

	/**
	 * 操縦者を返す（1.20.1 追加実装）
	 *
	 * 1.7.10 では riddenByEntity が EntityPlayer なら「そのプレイヤーが操縦している」と
	 * 暗黙に扱われていた（EntityTrackerEntry が操縦者への位置同期を抑制し、
	 * クライアント側で入力から算出した motion がそのまま反映される）。
	 * 1.20.1 ではその等価物が getControllingPassenger() であり、
	 * Entity.isControlledByLocalInstance() と LocalPlayer.sendPosition() の
	 * ServerboundMoveVehiclePacket 送信判定がこれに依存する。
	 * 未実装のままだと、クライアントで計算した移動が
	 * サーバからの位置同期で上書きされ、キー入力が反映されない。
	 * AbstractMinecart / Boat と同じ実装方針。
	 */
    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger()
    {
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player)
        {
            return player;
        }
        return null;
    }

	//Entityに攻撃されたときに呼ばれる
    @Override
    public boolean hurt(DamageSource damageSource, float par2)
    {
    	//無敵ならfalseを返す
        if (this.isInvulnerable())
        {
            return false;
        }
        //無敵でないなら
        else if (!this.level().isClientSide && !this.isRemoved())
        {
            this.setForwardDirection(-this.getForwardDirection());
            this.setTimeSinceHit(10);
            this.setDamageTaken(this.getDamageTaken() + par2 * 10.0F);
            this.markHurt();
            boolean flag = damageSource.getEntity() instanceof Player && ((Player)damageSource.getEntity()).isCreative();

            if (flag || this.getDamageTaken() > 40.0F)//相手がプレイヤーでクリエイティブ、または、４以上のダメージを受けているなら
            {
                Entity passenger = this.getFirstPassenger();
                if (passenger != null)
                {
                    // 1.7: this.riddenByEntity.mountEntity(this);
                    passenger.stopRiding();
                }

                if (!flag)
                {
                    this.spawnAtLocation(new ItemStack(THKaguyaItems.MAGIC_BROOM.get(), 1));
                }

                this.discard();
            }

            return true;
        }
        else
        {
            return true;
        }
    }

	//攻撃された時のアニメーション関連
	// 1.7: performHurtAnimation()。1.20.1 ではクライアント側ダメージ処理のフックとして handleDamageEvent を使う
    @Override
    public void handleDamageEvent(DamageSource source)
    {
        this.setForwardDirection(-this.getForwardDirection());
        this.setTimeSinceHit(10);
        this.setDamageTaken(this.getDamageTaken() * 11.0F);
    }

	//触れることができるかどうか
	// 1.7: canBeCollidedWith()
    @Override
    public boolean isPickable()
    {
        return !this.isRemoved();
    }

    /**
     * Sets the position and rotation. Only difference from the other one is no bounding on the rotation. Args: posX,
     * posY, posZ, yaw, pitch
     */
    // 1.7: setPositionAndRotation2(double, double, double, float, float, int)
    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport)
    {
        if (this.isEmpty)
        {
            this.boatPosRotationIncrements = steps + 5;
        }
        else
        {
            double xDistance = x - this.getX();
            double yDistance = y - this.getY();
            double zDistance = z - this.getZ();
            double distance = xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;

            if (distance <= 1.0D)
            {
                return;
            }

            this.boatPosRotationIncrements = 3;
        }

        this.broomX = x;
        this.broomY = y;
        this.broomZ = z;
        this.broomYaw = (double)yaw;
        this.broomPitch = (double)pitch;
        this.setDeltaMovement(this.velocityX, this.velocityY, this.velocityZ);
    }

	//ベクトルを設定
	// 1.7: setVelocity(double, double, double)（クライアント専用）
    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpMotion(double x, double y, double z)
    {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
        this.setDeltaMovement(x, y, z);
    }

	/**
	 * キー入力の取得（1.20.1 移植版）
	 *
	 * 1.7.10 の実装は本体クラスから直接 Minecraft.getMinecraft() を参照していた（クライアント漏洩）。
	 * 1.20.1 では実体を client パッケージの
	 * {@link thKaguyaMod.client.input.ClientBroomInput} に隔離し、
	 * DistExecutor.unsafeCallWhenOn 経由でのみ呼ぶ。
	 * Dist が一致しない場合 DistExecutor は供給元を呼ばないので、
	 * 専用サーバでは client クラスが一切ロードされない（NoClassDefFoundError 回避）。
	 */
	private static boolean clientKey(java.util.function.Supplier<java.util.concurrent.Callable<Boolean>> call)
	{
		// 誤った Dist では null が返るため Boolean.TRUE.equals で false に落とす
		return Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, call));
	}

	/** 前進（原版 keyBindForward） */
	public boolean isPushKeyUp()
	{
		return clientKey(() -> thKaguyaMod.client.input.ClientBroomInput::isPushKeyUp);
	}

	/** 後退（原版 keyBindBack） */
	public boolean isPushKeyBack()
	{
		return clientKey(() -> thKaguyaMod.client.input.ClientBroomInput::isPushKeyBack);
	}

	/** 右（原版 keyBindRight） */
	public boolean isPushKeyRight()
	{
		return clientKey(() -> thKaguyaMod.client.input.ClientBroomInput::isPushKeyRight);
	}

	/** 左（原版 keyBindLeft） */
	public boolean isPushKeyLeft()
	{
		return clientKey(() -> thKaguyaMod.client.input.ClientBroomInput::isPushKeyLeft);
	}

	/** ジャンプ（原版 keyBindJump） */
	public boolean isPushKeyJump()
	{
		return clientKey(() -> thKaguyaMod.client.input.ClientBroomInput::isPushKeyJump);
	}

	/** 毎tick呼ばれる処理 */
    @Override
    public void tick()
    {
        super.tick();

    	//ダメージアニメーション関連
        if (this.getTimeSinceHit() > 0)
        {
            this.setTimeSinceHit(this.getTimeSinceHit() - 1);
        }

        if (this.getDamageTaken() > 0.0F)
        {
            this.setDamageTaken(this.getDamageTaken() - 1.0F);
        }

		//前のポジションを今のポジションにする
        // 1.7: prevPosX = posX 等
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        byte b0 = 5;
        double d0 = 0.0D;

        for (int i = 0; i < b0; ++i)
        {
            double d1 = this.getBoundingBox().minY + (this.getBoundingBox().maxY - this.getBoundingBox().minY) * (double)(i + 0) / (double)b0 - 0.125D;
            double d2 = this.getBoundingBox().minY + (this.getBoundingBox().maxY - this.getBoundingBox().minY) * (double)(i + 1) / (double)b0 - 0.125D;
            AABB axisalignedbb = new AABB(this.getBoundingBox().minX, d1, this.getBoundingBox().minZ, this.getBoundingBox().maxX, d2, this.getBoundingBox().maxZ);

            // 1.7: worldObj.isAABBInMaterial(axisalignedbb, Material.water)
            if (this.level().containsAnyLiquid(axisalignedbb))
            {
                d0 += 1.0D / (double)b0;
            }
        }

        double motionXZ = Math.sqrt(this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z);//平面方向の移動量
        double cos_yaw_rad;//d4
        double sin_yaw_rad;//d5

        double d10;
        double d11;

        if (this.level().isClientSide && this.isEmpty)
        {
        	//箒の回転増加量が０より大きいなら
            if (this.boatPosRotationIncrements > 0)
            {

                double x_increments = this.getX() + (this.broomX - this.getX()) / (double)this.boatPosRotationIncrements;
                double y_increments = this.getY() + (this.broomY - this.getY()) / (double)this.boatPosRotationIncrements;
                double z_increments = this.getZ() + (this.broomZ - this.getZ()) / (double)this.boatPosRotationIncrements;
                d10 = Mth.wrapDegrees(this.broomYaw - (double)this.getYRot());
                this.setYRot((float)((double)this.getYRot() + d10 / (double)this.boatPosRotationIncrements));
                this.setXRot((float)((double)this.getXRot() + (this.broomPitch - (double)this.getXRot()) / (double)this.boatPosRotationIncrements));
                --this.boatPosRotationIncrements;
                this.setPos(x_increments, y_increments, z_increments);
                this.setYRot(this.getYRot());
                this.setXRot(this.getXRot());
            }
        	//０より小さいなら
            else
            {
                Vec3 motion = this.getDeltaMovement();
                double x_increments = this.getX() + motion.x;
                double y_increments = this.getY() + motion.y;
                double z_increments = this.getZ() + motion.z;
                this.setPos(x_increments, y_increments, z_increments);

                if (this.onGround())//地面の上なら
                {
                	//移動量を半減する
                    this.setDeltaMovement(motion.x * 0.5D, motion.y * 0.5D, motion.z * 0.5D);
                }

                Vec3 motion2 = this.getDeltaMovement();
                this.setDeltaMovement(
                		motion2.x * 0.9900000095367432D,
                		motion2.y * 0.949999988079071D,
                		motion2.z * 0.9900000095367432D);
            }
        }
        else
        {
            if (d0 < 1.0D)
            {
                double d4 = d0 * 2.0D - 1.0D;
            }
            else
            {
                /*if (this.motionY < 0.0D)//下降気味なら
                {
                    this.motionY /= 2.0D;//下降速度を半減
                }

                this.motionY += 0.007000000216066837D;//若干浮遊させる
            */}

            //riddenByEntity = new EntityZombie(worldObj);
        	//Entityが乗っているなら
            Entity passenger = this.getFirstPassenger();
            if (passenger != null && passenger instanceof Player)
            {

            	Player living = (Player)passenger;

        		boolean move_up = true;
        		boolean move_right = false;
        		boolean move_left = false;
        		boolean move_back = false;
        		boolean move_jump = false;


            	if(level().isClientSide)
            	{
            		move_up = isPushKeyUp();
            		move_right = isPushKeyRight();
            		move_left = isPushKeyLeft();
            		move_back = isPushKeyBack();
            		move_jump = isPushKeyJump();
            	}

            	/*@SideOnly(Side.CLIENT)
            	//if(worldObj.isRemote)
            	{
	            	Minecraft mc = Minecraft.getMinecraft();
	            	GameSettings gameSettings = mc.gameSettings;
	            	//gameSettings.sendSettingsToServer();
	            	move_up = gameSettings.keyBindForward.getIsKeyPressed();
	            	move_right = gameSettings.keyBindRight.getIsKeyPressed();
	            	move_left = gameSettings.keyBindLeft.getIsKeyPressed();
	            	move_back = gameSettings.keyBindBack.getIsKeyPressed();
	            	move_jump = gameSettings.keyBindJump.getIsKeyPressed();
            	}*/
	            	//KeyBinding key;

	            	Vec3 motion = this.getDeltaMovement();
	            	double mx = motion.x;
	            	double my = motion.y;
	            	double mz = motion.z;

	            	double acceleration = 0.04D;
	            	double acceleration2 = 0.08D;
	            	if(move_jump)
	            	{
	            		acceleration = 0.5D;
	            		//motionY = -Math.sin(riddenByEntity.rotationPitch / 180F * 3.141593F) * acceleration;
		            	//if(gameSettings.keyBindForward.getIsKeyPressed() && gameSettings.keyBindBack.getIsKeyPressed())
		            	{
		            		mx *= 0.0D;
		            		mz *= 0.0D;
		            		my *= 0.0D;
		            	}
		            	//else
		            	{

			            	if(move_up)
			            	{
			            		my = -Math.sin(-90F / 180F * 3.141593F) * acceleration;
			            		//motionX = 0.0D;
			            		//motionZ = 0.0D;
			            		//motionX = -Math.sin(riddenByEntity.rotationYaw / 180F * (float)Math.PI) * acceleration;
			            		//motionZ =  Math.cos(riddenByEntity.rotationYaw / 180F * (float)Math.PI) * acceleration;
			            	}
			            	if(move_back)
			            	{
			            		my = -Math.sin(90F / 180F * 3.141593F) * acceleration;
			            		//motionX = 0.0D;
			            		//motionZ = 0.0D;
			            		//motionX =  Math.sin(riddenByEntity.rotationYaw / 180F * (float)Math.PI) * acceleration;
			            		//motionZ = -Math.cos(riddenByEntity.rotationYaw / 180F * (float)Math.PI) * acceleration;
			            	}
		            	}
		            	if(move_right)
		            	{
		            		mx =  Math.sin((getYRot() + 0F) / 180F * (float)Math.PI) * acceleration;
		            		mz = -Math.cos((getYRot() + 0F) / 180F * (float)Math.PI) * acceleration;
		            	}
		            	if(move_left)
		            	{
		            		mx =  Math.sin((getYRot() - 180F) / 180F * (float)Math.PI) * acceleration;
		            		mz = -Math.cos((getYRot() - 180F) / 180F * (float)Math.PI) * acceleration;
		            	}
		            	double moveSpeed =  Math.sqrt(mx * mx + my * my + mz * mz);
		            	if(moveSpeed > 0.5D)
		            	{
		            		mx *= moveSpeed;
		            		my *= moveSpeed;
		            		mz *= moveSpeed;
		            	}
	            	}
	            	else
	            	{
	            		my += -Math.sin(getXRot() / 180F * 3.141593F) * acceleration;
		            	if(!move_up && !move_back &&
		            			!move_right && !move_left)
		            	{
		            		mx *= 0.97D;
		            		mz *= 0.97D;
		            		my *= 0.97D;
		            	}
		            	else
		            	{
			            	if(move_up)
			            	{
			            		mx +=  Math.sin((getYRot() - 90F) / 180F * (float)Math.PI) * acceleration;
			            		mz += -Math.cos((getYRot() - 90F) / 180F * (float)Math.PI) * acceleration;
			            	}
			            	if(move_back)
			            	{
			            		mx += -Math.sin((getYRot() - 90F) / 180F * (float)Math.PI) * acceleration2;
			            		mz +=  Math.cos((getYRot() - 90F) / 180F * (float)Math.PI) * acceleration2;
			            	}
		            	}
		            	if(move_right)
		            	{
		            		mx +=  Math.sin((getYRot() + 0F) / 180F * (float)Math.PI) * acceleration;
		            		mz += -Math.cos((getYRot() + 0F) / 180F * (float)Math.PI) * acceleration;
		            	}
		            	if(move_left)
		            	{
		            		mx +=  Math.sin((getYRot() - 180F) / 180F * (float)Math.PI) * acceleration;
		            		mz += -Math.cos((getYRot() - 180F) / 180F * (float)Math.PI) * acceleration;
		            	}

            	}

            	float f = passenger.getYRot();// + -living.moveStrafing * 90.0F;
            	//f *= 20;
            	speedMultiplier = 0.6D;


            	float pitch = passenger.getXRot();// * 3F;
            	setXRot(pitch);

            	if(pitch > 90F)
            	{
            		pitch = 90F;
            	}
            	else if(pitch < -90F)
            	{
            		pitch = -90F;
            	}
            	//Entityの移動量に箒の移動量を合わす
            	//motionX -=  Math.sin(riddenByEntity.rotationYaw / 180F * 3.141593F) * speedMultiplier * 0.6D;
            	//motionZ +=  Math.cos(riddenByEntity.rotationYaw / 180F * 3.141593F) * speedMultiplier * 0.6D;
            	// = -Math.sin(pitch / 180F * 3.141593F) * speedMultiplier * 1.0D;
            	if(pitch < 0F)
            	{
            		pitch *= 0.1F;
            	}

            	//rotationYaw = living.moveStrafing;
            	setYRot(passenger.getYRot() - 90F);


            	double moveSpeed =  Math.sqrt(mx * mx + my * my + mz * mz);
            	if(moveSpeed > 0.8D)
            	{
            		mx *= moveSpeed;
            		my *= moveSpeed;
            		mz *= moveSpeed;
            	}

            	this.setDeltaMovement(mx, my, mz);
            }


            /*double thisMotionXZ = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

            if (motionXZ > 0.7D)//一定の水平移動量があるなら
            {
                double motionXZ_rate = 0.7D / motionXZ;
                this.motionX *= motionXZ_rate;//一定の速度を超えないように移動量を制限
                this.motionZ *= motionXZ_rate;
                motionXZ = 0.7D;
            }

            if (thisMotionXZ > motionXZ && this.speedMultiplier < 0.7D)
            {
                this.speedMultiplier += (0.70D - this.speedMultiplier) / 70.0D;

                if (this.speedMultiplier > 0.7D)
                {
                    this.speedMultiplier = 0.7D;
                }
            }
            else
            {
                this.speedMultiplier -= (this.speedMultiplier - 0.07D) / 70.0D;

                if (this.speedMultiplier < 0.07D)
                {
                    this.speedMultiplier = 0.07D;
                }
            }*/

            /*if (this.onGround)
            {
                this.motionX *= 0.5D;
                this.motionY *= 0.5D;
                this.motionZ *= 0.5D;
            }*/
            if(this.isInWater())
            {
            	Vec3 m = this.getDeltaMovement();
            	this.setDeltaMovement(m.x * 0.5D, m.y * 0.5D, m.z * 0.5D);
            }

            Vec3 motion3 = this.getDeltaMovement();
            this.move(MoverType.SELF, new Vec3(motion3.x, motion3.y, motion3.z));

            /*if (this.isCollidedHorizontally && motionXZ > 0.2D)
            {
                if (!this.worldObj.isRemote && !this.isDead)
                {
                    this.setDead();
                    int k;

                    for (k = 0; k < 3; ++k)
                    {
                        this.dropItemWithOffset(Block.planks.blockID, 1, 0.0F);
                    }

                    for (k = 0; k < 2; ++k)
                    {
                        this.dropItemWithOffset(Item.stick.itemID, 1, 0.0F);
                    }
                }
            }
            else*/
            {
                Vec3 m = this.getDeltaMovement();
                this.setDeltaMovement(
                		m.x * 0.9900000095367432D,
                		m.y * 0.949999988079071D,
                		m.z * 0.9900000095367432D);
            }

        	//角度の修正********
            //this.rotationPitch = 0.0F;//垂直角度は常に一定で動かない
            /*double thisYaw = (double)this.rotationYaw;
            double prevXtoX = this.prevPosX - this.posX;
            double prevZtoZ = this.prevPosZ - this.posZ;

            if (prevXtoX * prevXtoX + prevZtoZ * prevZtoZ > 0.001D)//少しでも動いているなら
            {
                thisYaw = (double)((float)(Math.atan2(prevZtoZ, prevXtoX) * 180.0D / Math.PI));
            }

            double d12 = MathHelper.wrapAngleTo180_double(thisYaw - (double)this.rotationYaw);

            if (d12 > 20.0D)
            {
                d12 = 20.0D;
            }

            if (d12 < -20.0D)
            {
                d12 = -20.0D;
            }

            this.rotationYaw = (float)((double)this.rotationYaw + d12);*/

            this.setYRot(this.getYRot());
            this.setXRot(this.getXRot());
        	//*******************

            if (!this.level().isClientSide)
            {
                List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.20000000298023224D, 0.0D, 0.20000000298023224D));
                int l;

                if (list != null && !list.isEmpty())
                {
                    for (l = 0; l < list.size(); ++l)
                    {
                        Entity entity = list.get(l);

                        if (entity != this.getFirstPassenger() && entity.isPushable() && entity instanceof EntityMarisaBroom)
                        {
                            entity.push(this);
                        }
                    }
                }

                /*for (l = 0; l < 4; ++l)
                {
                    int i1 = MathHelper.floor_double(this.posX + ((double)(l % 2) - 0.5D) * 0.8D);
                    int j1 = MathHelper.floor_double(this.posZ + ((double)(l / 2) - 0.5D) * 0.8D);

                    for (int k1 = 0; k1 < 2; ++k1)
                    {
                        int l1 = MathHelper.floor_double(this.posY) + k1;
                        int i2 = this.worldObj.getBlockId(i1, l1, j1);//箒のいる場所のブロックIDを取得

                        if (i2 == Block.snow.blockID)//雪に衝突したなら
                        {
                            this.worldObj.setBlockToAir(i1, l1, j1);//空気ブロックに入れ替える
                        }
                        else if (i2 == Block.waterlily.blockID)//蓮に衝突したなら
                        {
                            this.worldObj.destroyBlock(i1, l1, j1, true);//蓮を破壊し、蓮をアイテム化する
                        }
                    }
                }*/

            	//乗っているEntityがおり、それが死んでいるなら
                Entity rider = this.getFirstPassenger();
                if (rider != null && rider.isRemoved())
                {
                    rider.stopRiding();// 1.7: this.riddenByEntity = null;
                }
                else
                {
                	//riddenByEntity.fallDistance = 0.0F;
                }
                if(rider != null)
                {
                	rider.fallDistance = 0.0F;
                	fallDistance = 0.0F;
                }
                else
                {
                	Vec3 m = this.getDeltaMovement();
                	double mx2 = m.x * 0.9D;
                	double my2 = m.y - 0.03D;
                	double mz2 = m.z * 0.9D;
                	if(getXRot() > 1.0F)
                	{
                		setXRot(getXRot() - 1.0F);
                	}
                	else if(getXRot() < -1.0F)
                	{
                		setXRot(getXRot() + 1.0F);
                	}
                	if(onGround())
                	{
                		my2 += 0.06D;
                	}
                	this.setDeltaMovement(mx2, my2, mz2);
                }
            }
        }


    }


	//乗っているEntityの位置を更新
	// 1.7: updateRiderPosition2()（Entity.updateRiderPosition の書き換え）
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction)
    {
        if (this.hasPassenger(passenger))
        {
        	//
            double d0 = 0D;//Math.cos((double)this.rotationYaw * Math.PI / 180.0D) * 0.4D;
            double d1 = 0D;//Math.sin((double)this.rotationYaw * Math.PI / 180.0D) * 0.4D;
            // 1.7: riddenByEntity.setPosition(posX + d0, posY + getMountedYOffset() + riddenByEntity.getYOffset(), posZ + d1)
            moveFunction.accept(passenger, this.getX() + d0, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + d1);
        }
    }

    /**
     * Handles updating while being ridden by an entity
     */
    // 1.7: updateRidden()（箒が何かに乗っている場合。通常は発生しない）
    @Override
    public void rideTick()
    {
    	super.rideTick();
        if (this.getVehicle() != null && this.getVehicle().isRemoved())
        {
            this.stopRiding();
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

	// 1.7: getShadowSize()（クライアント）-> レンダラで処理。削除
	// TODO(阶段4): 影サイズ 0.0F の処理

	//プレイヤーに右クリックされたときに呼ばれる
    //public boolean interact(EntityPlayer entityPlayer)
	//public boolean func_130002_c(EntityPlayer entityPlayer)
	// 1.7: interactFirst(EntityPlayer)
    @Override
    public InteractionResult interact(Player entityPlayer, InteractionHand hand)
    {
        Entity rider = this.getFirstPassenger();
        if (rider != null && rider instanceof Player && rider != entityPlayer)
        {
            return InteractionResult.SUCCESS;
        }
		else
    	{
        	if (!level().isClientSide)
        	{
            	entityPlayer.startRiding(this);
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
        //return this.dataWatcher.func_111145_d(19);
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

    // 1.7: func_70270_d(boolean)（クライアント専用。isEmpty の設定）
    @OnlyIn(Dist.CLIENT)
    public void setIsEmpty(boolean par1)
    {
        this.isEmpty = par1;
    }
}