package thKaguyaMod.entity.living;

import java.util.Random;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.LaserData;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.registry.DanmakuPatternRegistry;

/**
 * 妖精（1.20.1 移植版）
 *
 * 阶段 3a：最小链路实体。继承 EntityDanmakuMob，实现弹幕图案。
 * 弹体生成（THShotLib.createSphereShot 等）依赖 EntityTHShot，在 3c 补回；
 * 本类 danmakuPattern 暂以占位实现，保持编译通过。
 */
public class EntityTHFairy extends EntityDanmakuMob
{
    public EntityTHFairy(EntityType<? extends EntityTHFairy> type, Level level)
    {
        super(type, level);
        this.lastTime = 0;
        this.setSpeed(0.3D);
        this.setSpecies(this.SPECIES_FAIRY);
        // 妖精形态：原版 setForm(rand.nextInt(3))，随机 0/1/2（僵尸形态 -1 由 onDeathUpdate 在万圣节/地狱设定）
        this.setForm((byte)this.random.nextInt(3));
        this.isFlyingMode = true;
        this.setAttackDistance(8.0D);
        this.setDetectionDistance(8.0D);
        this.setFlyingHeight(2);

        // 飞行怪：不受重力（原版 isFlyingMode 通过 motionY 调整保持飞行）
        this.setNoGravity(true);

        // 弹幕图案初始化（原版 setPattern(rand.nextInt(pattern.size()))）
        // 若注册表尚未填充（DanmakuPatternRegistry.setDanmakuPattern 未调用）则跳过，避免 nextInt(0) 崩溃
        if(!level.isClientSide && DanmakuPatternRegistry.pattern.size() > 0)
        {
            this.setPattern(this.random.nextInt(DanmakuPatternRegistry.pattern.size()));
        }
    }

    protected void setPattern(int patternId)
    {
        if(DanmakuPatternRegistry.pattern.size() == 0)
        {
            DanmakuPatternRegistry.setDanmakuPattern();
        }
        if(patternId >= DanmakuPatternRegistry.pattern.size())
        {
            patternId = 0;
        }
        String patternKey = DanmakuPatternRegistry.pattern.get(patternId);
        int shot = DanmakuPatternRegistry.form.get(patternKey);
        setDanmakuPattern(DanmakuPatternRegistry.danmaku.get(patternKey));
        danmakuSpan = DanmakuPatternRegistry.span.get(patternKey);
        speedRate = DanmakuPatternRegistry.speed.get(patternKey);
        shotForm = (byte)((shot - (shot % 8)) / 8);
        shotColor = (byte)(shot % 8);
    }

    public EntityTHFairy(Level level)
    {
        this(THKaguyaEntities.TH_FAIRY.get(), level);
    }

    /** 弾の発射主 */
    public LivingEntity getShooter()
    {
        return this;
    }

    /** 弾幕の処理 */
    @Override
    protected void danmakuPattern(int level)
    {
        if(attackCounter > 0)
        {
            return;
        }

        Vec3 look = THShotLib.angle(getShooter().getYRot(), getShooter().getXRot());
        look = THShotLib.angle_LimitRandom(look, THKaguyaConfig.danmakuAccuracy);

        int d = getDanmakuPattern();
        boolean isShot = shotForm < 32;
        boolean isLaser = shotForm == 64;
        int shot = shotForm * 8 + shotColor;
        ShotData shotData = ShotData.shot(shotForm, shotColor, 0, 80, special);
        LaserData laserData = LaserData.laser(THShotLib.LASER_NORMAL, shotColor, 0.1F, 2.0F, 3.0F, 0, 80, special);

        THShotLib.playShotSound(this);

        // 速度差のある弾を一点に飛ばす
        if(d >= 0 && d <= 999)
        {
            int way = d % 100 + 1;
            int num = d / 100 + 1;
            double speed = speedRate;
            double slowDownRate = speed / num;
            if(isShot)
            {
                for(int i = 0; i < num; i++)
                {
                    THShotLib.createSphereShot(getShooter(), pos(), look, speed, shotData, way, 0F);
                    speed -= slowDownRate;
                }
            }
            else if(isLaser)
            {
                for(int i = 0; i < num; i++)
                {
                    THShotLib.createSphereLaserA(getShooter(), getShooter(), pos(), look, speed, speed, 0.0D, gravity_Zero(), laserData, way, 0F, 0F);
                    speed -= slowDownRate;
                }
            }
        }
        // 扇状弾幕
        else if(d >= 1000 && d <= 1999)
        {
            int num = d % 100 + 1;
            float span = (d - 1000) / 50F + 1F;
            if(isShot)
            {
                THShotLib.createWideShot(getShooter(), pos(), look, (double)speedRate, (double)speedRate, 0.0D, shotData, num, num * span);
            }
            else if(isLaser)
            {
                THShotLib.createWideLaserA(getShooter(), pos(), look, (double)speedRate, (double)speedRate, 0.0D, laserData, num, num * span);
            }
        }
        // 2～36way全方位弾
        else if(d >= 2000 && d <= 2999)
        {
            int way = d % 100 + 1;
            int num = (d - 2000) / 100 + 1;
            double speed = speedRate;
            double slowDownRate = speed / num;
            if(isShot)
            {
                for(int i = 0; i < num; i++)
                {
                    THShotLib.createCircleShot(getShooter(), pos(), look, speed, shotData, way);
                    speed -= slowDownRate;
                }
            }
            else if(isLaser)
            {
                for(int i = 0; i < num; i++)
                {
                    THShotLib.createCircleLaserA(getShooter(), pos(), look, speed, laserData, way);
                    speed -= slowDownRate;
                }
            }
        }
        // １発の弾をランダム角度に発射
        else if(d >= 3000 && d <= 3999)
        {
            float shotSpan = d % 100 + 1F;
            int num = (d - 3000) / 100 + 1;
            if(isShot)
            {
                THShotLib.createRandomRingShot(getShooter(), pos(), look, speedRate, shotData, num, shotSpan);
            }
        }
        else if(d >= 4000 && d <= 4999)
        {
            float shotSpan = ((d - 4000) % 100 + 1F) * 1.8F;
            int way = (d - 4000) / 100 * ((d % 100) / 10) + 3;
            THShotLib.createRingShot(getShooter(), pos(), look, speedRate, shotData, way, 0, shotSpan, level().random.nextFloat() * 360F);
        }
        else if(d >= 5000 && d <= 5999)
        {
            int num = d % 100 + 1;
            THShotLib.createRandomRingShot(getShooter(), pos(), look, speedRate, shotData, num, 360F);
        }
    }

    @Override
    protected boolean canFairyCall()
    {
        return true;
    }

    @Override
    protected double getFairyCallDistance()
    {
        return 30.0D;
    }

    @Override
    public int getMaxSpawnClusterSize()
    {
        return 10;
    }
}
