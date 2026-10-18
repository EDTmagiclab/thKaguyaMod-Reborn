package thKaguyaMod.entity.living;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.registry.DanmakuPatternRegistry;

/** 使い魔（1.20.1 移植版） */
public class EntityFamiliar extends EntityTHFairy
{
    public EntityFamiliar(EntityType<? extends EntityFamiliar> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 0;
        this.setForm((byte)this.random.nextInt(3));
        if(!level.isClientSide && DanmakuPatternRegistry.pattern.size() > 0)
        {
            this.setPattern(this.random.nextInt(DanmakuPatternRegistry.pattern.size()));
        }
        this.setSpeed(0.3D);
        this.setSpecies(this.SPECIES_SHIKIGAMI_FAMILIAR);
        this.isFlyingMode = true;
        this.setAttackDistance(8.0D);
        this.setDetectionDistance(0.0D);
        this.setFlyingHeight(2);
    }

    public EntityFamiliar(Level level)
    {
        this(THKaguyaEntities.FAMILIAR.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return EntityDanmakuMob.createAttributes();
    }

    /**
     * 弾の発射主（乗っている Entity）を返す。
     * 騎乗していないときは自分自身を返す——原版这里返回 null，但原版使い魔不会进入攻击模式，
     * 移植版允许击杀之后它会开火，null 会让 EntityTHFairy#danmakuPattern 里的 getShooter().getYRot() 直接 NPE
     * （客户端实测：打一下使魔就崩，latest.log 里是 EntityTHFairy.java:88 的 NullPointerException）。
     */
    @Override
    public LivingEntity getShooter()
    {
        Entity vehicle = this.getVehicle();
        if(vehicle instanceof LivingEntity)
        {
            return (LivingEntity)vehicle;
        }
        return this;
    }

    @Override
    protected boolean canFairyCall()
    {
        return false;
    }

    @Override
    protected double getFairyCallDistance()
    {
        return 0.0D;
    }

    @Override
    public int getMaxSpawnClusterSize()
    {
        return 1;
    }

    /** 原版 EntityFamiliar.onDeathUpdate は setDead() のみ（死亡演出なし）。 */
    @Override
    protected boolean hasDeathExplosion()
    {
        return false;
    }

    /**
     * 原版 EntityFamiliar.onUpdate：騎乗主がいない使い魔は自滅し、
     * 騎乗している Mob のターゲットを引き継ぐ。
     */
    @Override
    public void tick()
    {
        if(!this.level().isClientSide)
        {
            if(this.tickCount >= 1 && !(this.getVehicle() instanceof LivingEntity))
            {
                this.discard();
                return;
            }
            if(this.getVehicle() instanceof Mob vehicle)
            {
                this.setTarget(vehicle.getTarget());
            }
        }
        super.tick();
    }

    /* ===== 音：原版 EntityFamiliar は getLivingSound/getHurtSound/getDeathSound が全て null（無音） ===== */

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound()
    {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source)
    {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound()
    {
        return null;
    }
}
