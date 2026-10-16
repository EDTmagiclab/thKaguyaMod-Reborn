package thKaguyaMod.entity.shot;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.LaserData;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * レーザー系の共通処理（1.20.1 移植版）
 *
 * 继承 EntityTHShot，增加激光长度（laserLength / maxLaserLength）与射线碰撞。
 * 阶段 3d-1：裁剪 EntityShotMaterial（弹消材质，shotFinishBonus 用）、EntitySakuyaWatch、
 * EntityTHSetLaser、GUNGNIR 特殊激光——这些依赖未迁移实体，留待后续。
 */
public class EntityTHLaser extends EntityTHShot
{
    /** 激光长度（×10000 精度存储） */
    private static final EntityDataAccessor<Integer> LASER_LENGTH =
            SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);
    /** 激光最大长度（×10000 精度存储） */
    private static final EntityDataAccessor<Integer> MAX_LASER_LENGTH =
            SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);

    public EntityTHLaser(EntityType<?> type, Level level)
    {
        super(type, level);
        // 原版 ignoreFrustumCheck = true：激光中心出视野也渲染（1.20.1 等价物 noCulling 字段）
        this.noCulling = true;
    }

    public EntityTHLaser(EntityType<?> type, Level level, LivingEntity user, Entity source,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            int shotId, float width, float length, float damage, int delay, int end, int special)
    {
        super(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                shotId, width, damage, delay, end, special);
        this.noCulling = true;
        setLaserLength(0.0D);
        setMaxLaserLength(length);
    }

    public EntityTHLaser(Level level, LivingEntity user, Entity source,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            int shotId, float width, float length, float damage, int delay, int end, int special)
    {
        this(THKaguyaEntities.TH_LASER.get(), level, user, source, pos, angle, slope,
                rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                shotId, width, length, damage, delay, end, special);
    }

    public EntityTHLaser(Level level, LivingEntity user, Entity source,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            LaserData laser)
    {
        this(level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                laser.form * 8 + laser.color, laser.width, laser.length, laser.damage,
                laser.delay, laser.end, laser.special);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(LASER_LENGTH, 0);
        this.entityData.define(MAX_LASER_LENGTH, 0);
    }

    /** 毎 tick 処理：激光长度增长 */
    @Override
    public void tick()
    {
        super.tick();

        if(!level().isClientSide)
        {
            double laserLength = getLaserLength() + getSpeed() * 2.0D;
            if(laserLength > getMaxLaserLength())
            {
                setLaserLength(getMaxLaserLength());
            }
            else
            {
                setLaserLength(laserLength);
            }
        }
    }

    /**
     * 特殊激光每 tick 处理（原版 EntityTHLaser.specialMotion 的 GUNGNIR case）
     *
     * 冈格尼尔之枪（special=GUNGNIR）的枪形激光初速度为 0，长度不靠 speed 增长，
     * 而是靠这里每 tick +1.0 增长到 maxLaserLength；前 10 tick 同时旋转 18°/tick。
     * 第 10 tick 玩家反冲并开始加速，之后发射环形弹（神枪落地爆炸）。
     * 原版缺此分支会导致 laserLength 恒 0、渲染时 z 缩放为 0、枪形压扁不可见（"丢不出去"）。
     */
    @Override
    protected void specialMotion()
    {
        super.specialMotion();

        if(level().isClientSide || shotSpecial != DanmakuConstants.GUNGNIR)
        {
            return;
        }

        if(tickCount <= 10)
        {
            // 枪形激光长度增长 + 旋转 18°/tick（原版 getVectorFromRotation(rotate, angle, 18F)）
            Vec3 move = THShotLib.getVectorFromRotation(rotate, angle, 18F);
            if(getLaserLength() < getMaxLaserLength())
            {
                setLaserLength(getLaserLength() + 1.0D);
            }
            else
            {
                setLaserLength(getMaxLaserLength());
            }
            angle = move;
            updateAngle();
        }
        if(tickCount == 10)
        {
            // 玩家反冲 + 开始加速（原版 shotLimitSpeed=2.0D, shotAcceleration=0.6D）
            if(user != null)
            {
                user.setDeltaMovement(user.getDeltaMovement().subtract(angle.x, 0.0D, angle.z));
            }
            shotLimitSpeed = 2.0D;
            shotAcceleration = 0.6D;
        }
        if(tickCount > 10)
        {
            // 神枪落地爆炸：红色结晶环形弹
            ShotData shot = ShotData.shot(DanmakuConstants.FORM_CRYSTAL, DanmakuConstants.RED,
                    0.45F, 6.0F, 0, 40);
            THShotLib.createRingShot(user, this, THShotLib.pos_Entity(this), angle, 0F, 9999,
                    1.4D, 0.1D, -0.1D, THShotLib.gravity_Zero(), shot, 4, 0.3D, 240F,
                    tickCount * 10F);
        }
    }

    /**
     * レーザーの当たり判定は「発射点から本体長まで」伸びるため、探索範囲もその長さぶん必要。
     * 既定（1.0）のままだと発射点付近しか探索されず、**レーザーがまったくダメージを与えない**
     * （実測：レーヴァテインの横薙ぎ・縦斬りが無ダメージ、魔炮も同様）。
     */
    @Override
    protected double getHitCheckReach()
    {
        return Math.max(1.0D, getLaserLength() + getShotSize());
    }

    /** 碰撞检测：激光射线（从发射点沿 angle 延伸到 laserLength） */
    @Override
    protected void hitCheck()
    {
        double width = getShotSize();
        double length = getLaserLength() - getShotSize();
        if(length < 0.1D)
        {
            length = 0.1D;
        }
        Vec3 motion = this.getDeltaMovement();
        Vec3 startPos = new Vec3(getX() + angle.x * width, getY() + angle.y * width, getZ() + angle.z * width);
        Vec3 endPos = new Vec3(getX() + motion.x + angle.x * length, getY() + motion.y + angle.y * length, getZ() + motion.z + angle.z * length);

        // 方块碰撞：命中则缩短激光长度
        HitResult blockHit = level().clip(new net.minecraft.world.level.ClipContext(
                startPos, endPos, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if(blockHit != null && blockHit.getType() == HitResult.Type.BLOCK)
        {
            // 原版 onImpact 358 行：方块命中先做特殊处理（EXPLOSION01 爆炸等）
            blockHitSpecialProcess(blockHit);
            double hitDistance = this.position().distanceTo(blockHit.getLocation());
            if(hitDistance > 0.3D)
            {
                setLaserLength(hitDistance);
                setMaxLaserLength(hitDistance);
            }
            else
            {
                discard();
            }
            return;
        }

        // 实体碰撞（复用父类 hitEntityCheck）
        HitResult entityHit = hitEntityCheck(blockHit, startPos, endPos);
        if(entityHit != null && entityHit.getType() == HitResult.Type.ENTITY)
        {
            onImpact(entityHit);
        }
    }

    /** 命中处理：激光持续伤害 + 缩短长度（不立即删除） */
    @Override
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
                // 弹体相杀：激光抵消弹体（不同 user）
                EntityTHShot other = (EntityTHShot)hit;
                if(user != other.user)
                {
                    other.discard();
                }
            }
            else
            {
                // 原版 onImpact 278 行：实体命中先做特殊处理（EXPLOSION01 爆炸等）
                entityHitSpecialProcess(hit);
                // 魔法伤害（持续，受无敌帧限制）
                damageRate = 1.0F;
                float dmg = getShotDamageWithDifficulty() * damageRate;
                DamageSource dmgSource = user != null
                        ? level().damageSources().indirectMagic(this, user)
                        : level().damageSources().magic();
                hit.hurt(dmgSource, dmg);

                // 命中后缩短激光长度
                double hitDistance = this.distanceTo(hit);
                if(hitDistance > 0.3D)
                {
                    setLaserLength(hitDistance);
                    setMaxLaserLength(hitDistance);
                }
            }
        }
    }

    /** 激光长度 getter/setter（×10000 精度） */
    public void setLaserLength(double length)
    {
        this.entityData.set(LASER_LENGTH, (int)(length * 10000.0D));
    }

    public double getLaserLength()
    {
        return (double)this.entityData.get(LASER_LENGTH) / 10000.0D;
    }

    public void setMaxLaserLength(double maxLength)
    {
        this.entityData.set(MAX_LASER_LENGTH, (int)(maxLength * 10000.0D));
    }

    public double getMaxLaserLength()
    {
        return (double)this.entityData.get(MAX_LASER_LENGTH) / 10000.0D;
    }
}
