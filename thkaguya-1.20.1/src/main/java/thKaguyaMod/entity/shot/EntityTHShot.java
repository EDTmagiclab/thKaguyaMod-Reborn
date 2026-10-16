package thKaguyaMod.entity.shot;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.registry.SpecialShotRegistry;

/**
 * ショット系のEntityの共通処理（1.20.1 移植版）
 *
 * 阶段 3a：裁剪版弹体。保留飞行 / 碰撞 / 伤害核心，裁剪特殊弹注册表
 * （SpecialShotRegistry）、弹消材质（EntityShotMaterial）、符卡特殊弹（THSC_YouryokuSpoiler）。
 *
 * 1.7.10 → 1.20.1 关键映射：
 *   Entity（直接继承）        -> Entity（保持）
 *   dataWatcher              -> SynchedEntityData + EntityDataAccessor
 *   MovingObjectPosition     -> HitResult
 *   AxisAlignedBB            -> AABB
 *   attackEntityFrom         -> hurt
 *   worldObj                 -> level()
 *   setDead()                -> discard()
 */
public class EntityTHShot extends Entity
{
    // 同步数据
    private static final EntityDataAccessor<Integer> ANGLE_Z =
            SynchedEntityData.defineId(EntityTHShot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_COUNT =
            SynchedEntityData.defineId(EntityTHShot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOT_END_TIME =
            SynchedEntityData.defineId(EntityTHShot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOT_ID =
            SynchedEntityData.defineId(EntityTHShot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOT_SIZE =
            SynchedEntityData.defineId(EntityTHShot.class, EntityDataSerializers.INT);

    /** 弾の持ち主 */
    public LivingEntity user;
    /** 弾の発射主 */
    public Entity source;

    /** 弾の速度 */
    public double shotSpeed;
    public double shotLimitSpeed;
    public double shotAcceleration;

    /** 重力ベクトル */
    public Vec3 gravity = THShotLib.gravity_Zero();
    /** 弾のダメージ */
    public float shotDamage;
    /** 弾の色 */
    protected int shotColor;
    /** 弾の形状 */
    protected int shotForm;
    /** ショットID */
    protected int shotId;
    /** 方向ベクトル */
    public Vec3 angle = THShotLib.angle(0.0D, 0.0D, 0.0D);
    /** 特殊动作 */
    public int shotSpecial;

    protected int lastTime;
    public double lastShotMotionX;
    public double lastShotMotionY;
    public double lastShotMotionZ;

    /** 遅延時間 */
    public int delayTime;
    public float damageRate;

    // ===== 追尾の診断用（1 個体につき 1 度だけ出す使い捨てログ）=====
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private boolean homingCalledLogged;
    private boolean homingPickedLogged;
    private boolean specialDiagLogged;
    private boolean tickDiagLogged;

    /** 回転軸ベクトル */
    public Vec3 rotate = THShotLib.rotate_Default();
    public float rotationYawSpeed;
    public int rotationEnd;

    public EntityTHShot(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntityTHShot(Level level)
    {
        this(THKaguyaEntities.TH_SHOT.get(), level);
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(ANGLE_Z, 0);
        this.entityData.define(ANIMATION_COUNT, 0);
        this.entityData.define(SHOT_END_TIME, 0);
        this.entityData.define(SHOT_ID, 0);
        this.entityData.define(SHOT_SIZE, 0);
    }

    /** 完整参数构造（指定 EntityType，供激光等子类传入自己的类型） */
    public EntityTHShot(EntityType<?> type, Level level, LivingEntity entityUser, Entity entity,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            int form, float size, float damage, int delay, int end, int special)
    {
        this(type, level);
        user = entityUser;
        source = entity;

        setPos(pos.x, pos.y, pos.z);
        this.angle = angle;
        setAngleZ(slope);

        this.rotate = rotate;
        setRotationYawSpeed(rotationSpeed);
        this.rotationEnd = rotationEnd;

        this.shotSpeed = firstSpeed;
        this.shotLimitSpeed = limitSpeed;
        this.shotAcceleration = acceleration;
        this.gravity = gravity;

        setShotId(form);
        setShotSize(size);
        shotDamage = damage;
        delayTime = delay;
        setShotEndTime(end);
        shotSpecial = special;

        Vec3 motion = angle.scale(firstSpeed);
        this.setDeltaMovement(motion);
        lastShotMotionX = motion.x;
        lastShotMotionY = motion.y;
        lastShotMotionZ = motion.z;

        updateYawAndPitch();
        lastTime = 0;
        setAnimationCount(-delay);
    }

    /** 完整参数构造（默认 EntityType = TH_SHOT，普通弹体用） */
    public EntityTHShot(Level level, LivingEntity entityUser, Entity entity,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            int form, float size, float damage, int delay, int end, int special)
    {
        this(THKaguyaEntities.TH_SHOT.get(), level, entityUser, entity, pos, angle, slope,
                rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                form, size, damage, delay, end, special);
    }

    public EntityTHShot(Level level, LivingEntity entityUser, Entity entity,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            ShotData shot)
    {
        this(level, entityUser, entity, pos, angle, slope,
                rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                shot.form * 8 + shot.color, shot.size, shot.damage, shot.delay, shot.end, shot.special);
    }

    /** 位置ベクトル */
    public Vec3 pos()
    {
        return THShotLib.pos(getX(), getY(), getZ());
    }

    public Vec3 pos_User()
    {
        if(user != null)
        {
            return THShotLib.pos_Living(user);
        }
        return pos();
    }

    public double getSpeed()
    {
        Vec3 m = getDeltaMovement();
        return Math.sqrt(m.x * m.x + m.y * m.y + m.z * m.z);
    }

    /** 向いている方向ベクトルを返す */
    public Vec3 getShotVector()
    {
        return THShotLib.getVectorNomalize(angle);
    }

    /** 角度更新 */
    public void updateAngle()
    {
        Vec3 m = getDeltaMovement();
        float f = (float)Math.sqrt(m.x * m.x + m.z * m.z);
        this.setYRot((float)((Math.atan2(m.x, m.z) * 180D) / Math.PI));
        this.setXRot((float)((Math.atan2(m.y, f) * 180D) / Math.PI));
    }

    /** 弾を消滅させる */
    public void delete()
    {
        if(!level().isClientSide)
        {
            discard();
        }
    }

    @Override
    public void tick()
    {
        // 先递增弹体年龄（tickCount），并执行 Entity 基础逻辑（原版依赖 ticksExisted 递增）
        super.tick();

        // ===== 一時診断（原因特定後削除）：tick が動いているか / special が載っているか =====
        if(!tickDiagLogged)
        {
            tickDiagLogged = true;
            LOGGER.info("[TH] tick alive: special={} anim={} dmg={} client={}",
                    shotSpecial, getAnimationCount(), shotDamage, level().isClientSide);
        }

        // 弹主不存在则消失
        if(!level().isClientSide && user == null)
        {
            discard();
            return;
        }

        if(shotDamage <= 0.0F)
        {
            if(!level().isClientSide)
            {
                discard();
                return;
            }
        }

        // 消滅時間を過ぎたら消滅
        if(!level().isClientSide && getAnimationCount() > getShotEndTime())
        {
            discard();
            return;
        }

        // 动画计数更新（原版：>=0 时跟踪时间，<0 时为延迟倒计时，延迟阶段不移动）
        if(!level().isClientSide)
        {
            if(getAnimationCount() >= 0)
            {
                setAnimationCount(tickCount);
            }
            else
            {
                setAnimationCount(getAnimationCount() + 1);
                return;
            }
        }

        // 时间停止逻辑（1.7 时停功能阶段 7 接入，此处简化为正常推进）
        Vec3 motion = new Vec3(lastShotMotionX, lastShotMotionY, lastShotMotionZ);
        this.setDeltaMovement(motion);

        if(rotationYawSpeed != 0F)
        {
            angle = THShotLib.getVectorFromRotation(rotate, angle, rotationYawSpeed);
        }

        updateAngle();
        shotAcceleration();
        specialMotion();
        gravity = THShotLib.gravity(gravity.x, gravity.y, gravity.z);
        motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.add(gravity.x, gravity.y, gravity.z));

        hitCheck();

        shotSpeed = getSpeed();
        if(rotationEnd < lastTime)
        {
            rotationYawSpeed = 0F;
        }

        if(getAnimationCount() >= 0)
        {
            motion = this.getDeltaMovement();
            this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        }

        if(tickCount > lastTime)
        {
            lastTime = tickCount;
            lastShotMotionX = motion.x;
            lastShotMotionY = motion.y;
            lastShotMotionZ = motion.z;
        }
    }

    public void shotAcceleration()
    {
        Vec3 m = this.getDeltaMovement();
        if(shotAcceleration > 0.0D && getSpeed() < shotLimitSpeed)
        {
            Vec3 accel = angle.scale(shotAcceleration);
            this.setDeltaMovement(m.add(accel));
            if(getSpeed() > shotLimitSpeed)
            {
                this.setDeltaMovement(angle.scale(shotLimitSpeed));
            }
        }
        else if(shotAcceleration < 0.0D && getSpeed() > shotLimitSpeed)
        {
            if(Math.abs(shotAcceleration) > getSpeed() - shotLimitSpeed)
            {
                this.setDeltaMovement(angle.scale(shotLimitSpeed));
            }
            else
            {
                Vec3 accel = angle.scale(shotAcceleration);
                this.setDeltaMovement(m.add(accel));
            }
        }
    }

    /** 使用者に当たるかどうか */
    public boolean userHitCheck(Entity entity)
    {
        return !entity.is(user);
    }

    /**
     * 当たり判定の探索範囲を angle 方向へ伸ばす距離。
     *
     * <p>通常弾は 1 tick の移動量（≒1 ブロック）で足りるので既定は 1.0。
     * レーザー（EntityTHLaser）は当たり判定が本体長まで伸びるため必ず上書きすること。
     */
    protected double getHitCheckReach()
    {
        return 1.0D;
    }

    /** 特殊弹的每 tick 处理（子类重写） */
    protected void specialMotion()
    {
        // ===== 一時診断（原因特定後削除）=====
        if(!specialDiagLogged)
        {
            specialDiagLogged = true;
            Class<? extends ISpecialShot> c = SpecialShotRegistry.getSpecialShotClass(shotSpecial);
            LOGGER.info("[TH] special={} -> {} (client={})", shotSpecial,
                    c == null ? "NOT REGISTERED" : c.getSimpleName(), level().isClientSide);
        }

        // 原版 977 行：shotSpecial 查 SpecialShotRegistry 分发到 ISpecialShot.specialShot_move
        if(shotSpecial != 0)
        {
            Class<? extends ISpecialShot> specialShotClass = SpecialShotRegistry.getSpecialShotClass(shotSpecial);
            if(specialShotClass != null)
            {
                try {
                    ISpecialShot iSpecialShot = specialShotClass.getDeclaredConstructor().newInstance();
                    iSpecialShot.specialShot_move(level(), shotSpecial, this);
                } catch (ReflectiveOperationException e) {
                }
                return;
            }

            // 原版 990-1004 行：注册表查不到时的硬编码基础特殊弹（阶段 7 哑物品批次1 补回）
            //   注意原版在注册表命中时直接 return，所以这段只在未注册时执行
            switch(shotSpecial)
            {
                case DanmakuConstants.HOMING01:
                    // ホーミング弾（ホーミングアミュレット / 人魂灯）
                    homing(4.0F);
                    break;
                case DanmakuConstants.DIFFUSION01:
                    // 拡散アミュレット：消滅時に扇状弾へ分裂
                    diffusion(ShotData.shot(DanmakuConstants.FORM_AMULET, DanmakuConstants.BLUE,
                            0.4F, 5.0F, 0, 60), 0.09D);
                    break;
                case DanmakuConstants.FALL01:
                    // 落下弾
                    setDeltaMovement(getDeltaMovement().add(0.0D, -0.02D, 0.0D));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * ホーミング（原版 1472 行）。
     *
     * <p>24 格内最近的「有效目标」（LivingEntity，排除动物/村民/使用者/发射源，且视线无方块遮挡）
     * 与本弹飞行方向的夹角若在 homingLevel 度以内，则每 tick 最多转向 homingLevel 度。
     * 目标评估用的是「距离 × sin(夹角)」—— 即目标偏离当前飞行轴线的横向距离，取最小者。
     *
     * @param homingLevel 每 tick 最大转向角（度）
     */
    public void homing(float homingLevel)
    {
        if(!level().isClientSide && !homingCalledLogged)
        {
            homingCalledLogged = true;
            LOGGER.info("[thkaguya] homing() called (special={}, entity={})", shotSpecial, getId());
            if(user instanceof Player p)
            {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "[TH] homing() called (special=" + shotSpecial + ")"), false);
            }
        }

        AABB searchBox = getBoundingBox()
                .expandTowards(getDeltaMovement())
                .inflate(24.0D, 24.0D, 24.0D);
        List<Entity> list = level().getEntities(this, searchBox);

        LivingEntity nearEntity = null;
        double nearDistance = 999.9D;
        float nearAngle = 180F;
        double nearValue = nearDistance * THShotLib.halfAbsSin(nearAngle / 180F * (float)Math.PI);
        Vec3 shotVec = this.getShotVector();

        for(Entity entitys : list)
        {
            // 生物でない、または動物・村人・使用者・発射源は対象外
            if(!(entitys instanceof LivingEntity entity1)
                    || entitys instanceof Animal
                    || entitys instanceof Villager
                    || entitys == source
                    || entitys == user)
            {
                continue;
            }
            if(entity1.isDeadOrDying())
            {
                continue;
            }

            Vec3 from = position();
            Vec3 to = new Vec3(entity1.getX(), entity1.getEyeY(), entity1.getZ());
            // ブロックに遮られているなら対象外（原版 worldObj.func_147447_a(..., false, true, false)）
            BlockHitResult blockHit = level().clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if(blockHit.getType() != HitResult.Type.MISS)
            {
                continue;
            }

            Vec3 targetVec = THShotLib.getVectorNomalize(
                    new Vec3(entity1.getX() - getX(), entity1.getEyeY() - getY(), entity1.getZ() - getZ()));
            float angleSpan = Math.abs(THShotLib.getVectorAndVectorAngle(shotVec, targetVec));
            double toEntity1Distance = Math.sqrt(distanceToSqr(entity1.getX(), entity1.getEyeY(), entity1.getZ()));
            double value = toEntity1Distance * THShotLib.halfAbsSin(angleSpan / 180F * (float)Math.PI);
            if(nearValue > value)
            {
                nearEntity = entity1;
                nearAngle = angleSpan;
                nearValue = value;
                nearDistance = toEntity1Distance;
            }
        }

        // 追尾対象がいるならば向きを最大 homingLevel 度だけ回す
        if(nearEntity != null)
        {
            if(!level().isClientSide && !homingPickedLogged)
            {
                homingPickedLogged = true;
                LOGGER.info("[thkaguya] homing() locked on target={} distance={}", nearEntity, nearDistance);
                if(user instanceof Player p)
                {
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "[TH] homing locked on " + nearEntity.getType().toShortString()), false);
                }
            }
            Vec3 targetVec = THShotLib.getVectorNomalize(new Vec3(
                    nearEntity.getX() - getX(),
                    nearEntity.getEyeY() - getY(),
                    nearEntity.getZ() - getZ()));
            Vec3 rotate = THShotLib.getOuterProduct(this.getShotVector(), targetVec);
            float rotateAngle = THShotLib.getVectorAndVectorAngle(shotVec, targetVec);
            if(rotateAngle > homingLevel)
            {
                rotateAngle = homingLevel;
            }
            else if(rotateAngle < -homingLevel)
            {
                rotateAngle = -homingLevel;
            }
            Vec3 newVec = THShotLib.getVectorFromRotation(rotate, angle, rotateAngle);
            Vec3 dir = THShotLib.getVectorNomalize(newVec);
            if(dir.lengthSqr() < 0.5D)
            {
                // 回転結果が退化（ゼロ／NaN）なら元の向きを維持
                dir = this.getShotVector();
            }
            angle = dir;
            if(!level().isClientSide)
            {
                // 速さはそのままに、速度ベクトルの向きも新しい angle へ張り替える。
                //
                // 原版はここで shotAcceleration() のみを呼び、setMotion() は
                // コメントアウトされている。そのため速度が shotLimitSpeed に達すると
                // shotAcceleration() の条件「getSpeed() < shotLimitSpeed」が偽になり、
                // angle をどれだけ変えても進行方向が変わらない = 追尾しない。
                // （実測：人魂灯の弾が真上に飛んだまま目標を追わなかった）
                double speed = getSpeed();
                if(speed <= 0.0D)
                {
                    speed = shotSpeed;
                }
                Vec3 redirected = dir.scale(speed);
                this.setDeltaMovement(redirected);
                // tick 冒頭で lastShotMotion から速度を復元するため、こちらも同時に更新する
                lastShotMotionX = redirected.x;
                lastShotMotionY = redirected.y;
                lastShotMotionZ = redirected.z;
                shotAcceleration();
            }
        }
    }

    /**
     * 拡散アミュレット（原版 1659 行）：自身の消滅 tick に、
     * 使用者の視線方向へ 20/16/8 発の扇状弾を撒いて消える。
     *
     * @param shot         撒く弾のデータ
     * @param gravityLevel 使用者の視線方向の重力（弾が広がる向き）
     */
    public void diffusion(ShotData shot, double gravityLevel)
    {
        if(!isShotEndTime())
        {
            return;
        }

        if(user != null)
        {
            double vectorXG = -Math.sin(user.getYRot() / 180F * 3.141593F)
                    * Math.cos(user.getXRot() / 180F * 3.141593F) * gravityLevel;
            double vectorYG = -Math.sin(user.getXRot() / 180F * 3.141593F) * gravityLevel;
            double vectorZG = Math.cos(user.getYRot() / 180F * 3.141593F)
                    * Math.cos(user.getXRot() / 180F * 3.141593F) * gravityLevel;
            Vec3 gravity = THShotLib.gravity(vectorXG, vectorYG, vectorZG);

            THShotLib.createSphereShot(user, user, THShotLib.pos_Entity(this), angle, 0F,
                    THShotLib.rotate_Default(), 0F, 9999, 0.6D, 0.6D, 0.00D, gravity,
                    shot, 20, 0.0D, this.random.nextFloat() * 360F);
            THShotLib.createSphereShot(user, user, THShotLib.pos_Entity(this), angle, 0F,
                    THShotLib.rotate_Default(), 0F, 9999, 0.4D, 0.6D, 0.01D, gravity,
                    shot, 16, 0.0D, this.random.nextFloat() * 360F);
            THShotLib.createSphereShot(user, user, THShotLib.pos_Entity(this), angle, 0F,
                    THShotLib.rotate_Default(), 0F, 9999, 0.2D, 0.6D, 0.01D, gravity,
                    shot, 8, 0.0D, this.random.nextFloat() * 360F);
        }

        if(!level().isClientSide)
        {
            discard();
        }
    }

    /** 使用者に当たるかどうか（子类重写） */
    protected boolean isUserHit()
    {
        return false;
    }

    /** Entity 命中时的特殊处理，返回 true 表示弹体继续存在。
     *  原版 1192 行：shotSpecial 查 SpecialShotRegistry 分发到 ISpecialShot.specialShot_hitEntity */
    public boolean entityHitSpecialProcess(Entity hitEntity)
    {
        if(shotSpecial != 0)
        {
            Class<? extends ISpecialShot> specialShotClass = SpecialShotRegistry.getSpecialShotClass(shotSpecial);
            if(specialShotClass != null)
            {
                try {
                    ISpecialShot iSpecialShot = specialShotClass.getDeclaredConstructor().newInstance();
                    return iSpecialShot.specialShot_hitEntity(level(), shotSpecial, this, hitEntity);
                } catch (ReflectiveOperationException e) {
                }
            }
            // 原版 EntityTHShot.entityHitSpecialProcess 的硬编码基础特效 switch（阶段 4 补回）
            switch(shotSpecial)
            {
                case DanmakuConstants.FIRE:
                    hitEntity.setSecondsOnFire((int)shotDamage);
                    return false;
                case DanmakuConstants.EXPLOSION01:
                    if(!level().isClientSide)
                    {
                        Level.ExplosionInteraction interaction = THKaguyaConfig.MasterSparkDestroysBlocks
                                ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
                        level().explode(user, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), 3.0F, false, interaction);
                    }
                    return false;
                case DanmakuConstants.WIND01:
                {
                    Vec3 m = hitEntity.getDeltaMovement();
                    Vec3 d = this.getDeltaMovement();
                    hitEntity.setDeltaMovement(m.x + d.x * 1.7D, m.y + d.y * 1.7D + 2.0D, m.z + d.z * 1.7D);
                    if(!hitEntity.onGround())
                    {
                        damageRate *= 2.0F;
                    }
                    return false;
                }
                default:
                    return false;
            }
        }
        return false;
    }

    /** 从 yRot/xRot 更新 angle 向量 + motion（原版 setVector） */
    public void setVector()
    {
        float yaw = getYRot();
        float pitch = getXRot();
        angle = new Vec3(
            Math.sin(yaw / 180F * Math.PI) * Math.cos(pitch / 180F * Math.PI),
            Math.sin(pitch / 180F * Math.PI),
            Math.cos(yaw / 180F * Math.PI) * Math.cos(pitch / 180F * Math.PI));
        updateMotion();
    }

    /** 从 angle 更新 motion + lastShotMotion（原版 updateMotion） */
    public void updateMotion()
    {
        double speed = getSpeed();
        Vec3 motion = angle.scale(speed);
        this.setDeltaMovement(motion);
        lastShotMotionX = motion.x;
        lastShotMotionY = motion.y;
        lastShotMotionZ = motion.z;
    }

    /** 命中判定的追加过滤（子类重写，返回 true 表示跳过该实体） */
    protected boolean hitCheckEx(Entity entity)
    {
        return false;
    }

    /** 方块碰撞的特殊处理，返回 true 表示弹体继续存在。
     *  原版 1240 行：shotSpecial 查 SpecialShotRegistry 分发到 ISpecialShot.specialShot_hitBlock */
    public boolean blockHitSpecialProcess(HitResult result)
    {
        if(shotSpecial != 0)
        {
            Class<? extends ISpecialShot> specialShotClass = SpecialShotRegistry.getSpecialShotClass(shotSpecial);
            if(specialShotClass != null)
            {
                try {
                    ISpecialShot iSpecialShot = specialShotClass.getDeclaredConstructor().newInstance();
                    return iSpecialShot.specialShot_hitBlock(level(), shotSpecial, this, result);
                } catch (ReflectiveOperationException e) {
                }
            }
            // 原版 EntityTHShot.blockHitSpecialProcess 的硬编码基础特效 switch（阶段 4 补回）
            // BOUND01~04 的反弹方法（bound）尚未迁移，暂保持消弹（已知延后项）
            switch(shotSpecial)
            {
                case DanmakuConstants.EXPLOSION01:
                    if(!level().isClientSide && result.getLocation() != null)
                    {
                        Vec3 hit = result.getLocation();
                        Level.ExplosionInteraction interaction = THKaguyaConfig.MasterSparkDestroysBlocks
                                ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
                        level().explode(user, hit.x, hit.y, hit.z, 5.0F, false, interaction);
                    }
                    return false;
                default:
                    return false;
            }
        }
        return false;
    }

    /** 特殊弾から普通の弾に戻す（原版 specialEnd） */
    public void specialEnd()
    {
        shotSpecial = 0;
    }

    /** ショットの左右の方向の角度を変更する（原版 setShotRotationYaw） */
    public void setShotRotationYaw(float rotateAngle)
    {
        Vec3 newAngle = THShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z, angle.x, angle.y, angle.z, rotateAngle);

        angle = newAngle;
        updateMotion();
    }

    /** 弾の消滅時間を設定（原版 setDeadTime 的别名，符卡代码大量使用） */
    public void setDeadTime(int time)
    {
        setShotEndTime(time);
    }

    /** 弾の消滅時間を返す（原版 getDeadTime 的别名，符卡代码大量使用） */
    public int getDeadTime()
    {
        return getShotEndTime();
    }

    /** 消滅時間に達したかどうか（原版 isShotEndTime） */
    public boolean isShotEndTime()
    {
        return tickCount == getShotEndTime();
    }

    /** 現在の弾の情報を引き継ぎ再生成する（原版 reCreate）。この弾は消えずに残り、コピーを作る */
    public EntityTHShot reCreate()
    {
        if(!level().isClientSide)
        {
            return THShotLib.createShot(
                    this.user,
                    this.source,
                    THShotLib.pos_Entity(this),
                    this.angle,
                    this.getAngleZ(),
                    this.rotate,
                    this.rotationYawSpeed,
                    this.rotationEnd,
                    this.shotSpeed,
                    this.shotLimitSpeed,
                    this.shotAcceleration,
                    this.gravity,
                    ShotData.shot(this.getShotForm(),
                                  this.getShotColor(),
                                  this.getShotSize(),
                                  this.shotDamage,
                                  this.delayTime,
                                  this.getShotEndTime(),
                                  this.shotSpecial
                                  )
                    );
        }
        return null;
    }

    /** 速度を設定（原版 setSpeed）。限界速度は越えない */
    public void setShotSpeed(double speed)
    {
        shotSpeed = speed;
        updateMotion();
    }

    /** Entity との当たり判定 */
    public HitResult hitEntityCheck(HitResult result, Vec3 start, Vec3 end)
    {
        Entity hit = null;
        double closest = Double.MAX_VALUE;
        float hitSize = getShotSize() * 0.5F;

        // 【範囲は「弾の到達距離」ぶん伸ばす必要がある】
        // 探索用 AABB を「自身の bbox + angle * 1.0」だけで作ると 1 ブロック程度しか覆わない。
        // 通常弾は 1 tick の移動量 ≒ 1 ブロックなのでこれで足りるが、**レーザーは当たり判定が
        // 最大 20～40 ブロック先まで伸びる**ため、この範囲では発射点の近くしか拾えず、
        // 実質「レーザーはまったくダメージを与えない」状態になる。
        // （実測：レーヴァテイン横薙ぎ/縦斬りともに無ダメージ、魔炮も同様に無ダメージ。
        //   原版 1.7.10 は boundingBox.addCoord(dir * length) で length ぶん伸ばしていた。）
        // そのため到達距離をサブクラスが供給できるようにし、既定は従来どおり 1.0 とする。
        double reach = getHitCheckReach();
        AABB area = this.getBoundingBox()
                .expandTowards(angle.x * reach, angle.y * reach, angle.z * reach)
                .inflate(hitSize);
        List<Entity> list = level().getEntities(this, area);

        for(Entity e : list)
        {
            // 1.7.10 的 canBeCollidedWith() = 1.20.1 的 isPickable()。
            // 1.20.1 的 canBeCollidedWith() 语义已变（默认 false，玩家也 false），不能用它判断命中。
            if(!e.isPickable() || !userHitCheck(e))
            {
                continue;
            }
            if(e instanceof LivingEntity || e instanceof EntityTHShot)
            {
                AABB aabb = e.getBoundingBox().inflate(hitSize);
                Vec3 interceptVec = aabb.clip(start, end).orElse(null);
                if(interceptVec != null)
                {
                    double d = start.distanceTo(interceptVec);
                    if(d < closest)
                    {
                        hit = e;
                        closest = d;
                    }
                }
            }
        }

        if(hit != null)
        {
            return new EntityHitResultHelper(hit);
        }
        return result;
    }

    /** 碰撞检测 */
    protected void hitCheck()
    {
        double halfSize = this.getShotSize() / 2.0D;
        double shotSizeX = angle.x * halfSize;
        double shotSizeY = angle.y * halfSize;
        double shotSizeZ = angle.z * halfSize;
        Vec3 motion = this.getDeltaMovement();

        Vec3 startPos = new Vec3(getX() - shotSizeX, getY() - shotSizeY, getZ() - shotSizeZ);
        Vec3 endPos = new Vec3(getX() + motion.x + shotSizeX, getY() + motion.y + shotSizeY, getZ() + motion.z + shotSizeZ);

        HitResult blockHit = level().clip(new net.minecraft.world.level.ClipContext(
                startPos, endPos, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));

        if(blockHit != null && blockHit.getType() == HitResult.Type.BLOCK)
        {
            if(!blockHitSpecialProcess(blockHit))
            {
                delete();
                return;
            }
        }

        HitResult entityHit = hitEntityCheck(blockHit, startPos, endPos);
        if(entityHit != null && entityHit.getType() == HitResult.Type.ENTITY)
        {
            onImpact(entityHit);
        }
    }

    /** 命中处理 */
    protected void onImpact(HitResult result)
    {
        if(level().isClientSide)
        {
            return;
        }
        if(result instanceof EntityHitResultHelper helper)
        {
            Entity hit = helper.getEntity();
            if(hit instanceof EntityTHShot)
            {
                // 弾同士の相殺
                EntityTHShot other = (EntityTHShot)hit;
                if(user != other.user)
                {
                    float a = this.shotDamage;
                    float b = other.shotDamage;
                    if(user instanceof Player)
                    {
                        shotDamage -= b * 0.1F;
                    }
                    else
                    {
                        shotDamage -= b * 0.3F;
                    }
                    if(other.user instanceof Player)
                    {
                        other.shotDamage -= a * 0.1F;
                    }
                    else
                    {
                        other.shotDamage -= a * 0.3F;
                    }
                    if(shotDamage < 0.0F)
                    {
                        delete();
                    }
                }
            }
            else
            {
                // 原版 1192 行：特殊弹的实体命中处理，返回 false 表示普通命中流程
                if(entityHitSpecialProcess(hit))
                {
                    return;
                }
                // 伤害倍率默认 1（原版在命中时设为 1.0F；字段默认 0，不设则无伤害）
                damageRate = 1.0F;
                float dmg = getShotDamageWithDifficulty() * damageRate;
                // 原版 DamageSource.causeIndirectMagicDamage -> 1.20.1 DamageSources.indirectMagic
                DamageSource dmgSource = user != null
                        ? level().damageSources().indirectMagic(this, user)
                        : level().damageSources().magic();
                hit.hurt(dmgSource, dmg);
                delete();
            }
        }
    }

    /** 難易度によるダメージ補正 */
    protected float getShotDamageWithDifficulty()
    {
        if(this.user instanceof Player)
        {
            return shotDamage;
        }
        if(THKaguyaConfig.danmakuOneKillMode)
        {
            return 999999F;
        }
        switch(this.level().getDifficulty())
        {
            case PEACEFUL: return shotDamage;
            case EASY:     return shotDamage * 0.7F;
            case NORMAL:   return shotDamage;
            case HARD:     return shotDamage * 1.5F;
            default:       return shotDamage;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if(source.getEntity() instanceof Player)
        {
            if(source.getEntity() != this.user)
            {
                if(!level().isClientSide)
                {
                    delete();
                }
                return true;
            }
        }
        return false;
    }

    // ===== 同步数据 getter/setter =====
    public void setAngleZ(float angleZ)
    {
        this.entityData.set(ANGLE_Z, (int)(angleZ * 10000F));
    }

    public float getAngleZ()
    {
        return (float)this.entityData.get(ANGLE_Z) / 10000F;
    }

    public void setAnimationCount(int count)
    {
        this.entityData.set(ANIMATION_COUNT, count + 1000);
    }

    public int getAnimationCount()
    {
        return this.entityData.get(ANIMATION_COUNT) - 1000;
    }

    public void setShotEndTime(int time)
    {
        this.entityData.set(SHOT_END_TIME, time);
    }

    public int getShotEndTime()
    {
        return this.entityData.get(SHOT_END_TIME);
    }

    public void setShotId(int id)
    {
        this.entityData.set(SHOT_ID, id);
    }

    public int getShotId()
    {
        return this.entityData.get(SHOT_ID);
    }

    public void setShotForm(int form)
    {
        setShotId(form * 8 + getShotColor());
    }

    public void setShotColor(int color)
    {
        setShotId(getShotForm() * 8 + color);
    }

    public int getShotForm()
    {
        return (getShotId() - getShotColor()) / 8;
    }

    public int getShotColor()
    {
        return getShotId() % 8;
    }

    public void setShotSize(float size)
    {
        this.entityData.set(SHOT_SIZE, (int)(size * 100F));
    }

    public float getShotSize()
    {
        return (float)this.entityData.get(SHOT_SIZE) / 100F;
    }

    public void setRotationYawSpeed(float rotation)
    {
        rotationYawSpeed = rotation;
    }

    public float getRotationYawSpeed()
    {
        return rotationYawSpeed;
    }

    protected void updateYawAndPitch()
    {
        this.setYRot(THShotLib.getYawFromVector(angle.x, angle.z));
        this.setXRot(THShotLib.getPitchFromVector(angle.x, angle.y, angle.z));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }

    /** HitResult 的 Entity 包装（替代 1.7 的 new MovingObjectPosition(entity)） */
    static final class EntityHitResultHelper extends HitResult
    {
        private final Entity entity;

        EntityHitResultHelper(Entity entity)
        {
            super(entity.position());
            this.entity = entity;
        }

        @Override
        public Type getType()
        {
            return Type.ENTITY;
        }

        Entity getEntity()
        {
            return entity;
        }
    }
}
