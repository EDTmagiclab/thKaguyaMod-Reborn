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
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;

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
    }

    public EntityTHLaser(Level level, LivingEntity user, Entity source,
            Vec3 pos, Vec3 angle, float slope,
            Vec3 rotate, float rotationSpeed, int rotationEnd,
            double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
            int shotId, float width, float length, float damage, int delay, int end, int special)
    {
        super(level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                shotId, width, damage, delay, end, special);
        setLaserLength(0.0D);
        setMaxLaserLength(length);
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
