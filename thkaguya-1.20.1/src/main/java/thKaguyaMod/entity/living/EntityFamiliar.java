package thKaguyaMod.entity.living;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

    /** 弾の発射主（乗っている Entity）を返す */
    @Override
    public LivingEntity getShooter()
    {
        Entity vehicle = this.getVehicle();
        if(vehicle instanceof LivingEntity)
        {
            return (LivingEntity)vehicle;
        }
        return null;
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
}
