package thKaguyaMod.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.registry.DanmakuPatternRegistry;

/** 幽霊（1.20.1 移植版） */
public class EntityTHPhantom extends EntityTHFairy
{
    public EntityTHPhantom(EntityType<? extends EntityTHPhantom> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 5;
        this.setForm((byte)this.random.nextInt(8));
        if(!level.isClientSide && DanmakuPatternRegistry.pattern.size() > 0)
        {
            this.setPattern(this.random.nextInt(DanmakuPatternRegistry.pattern.size()));
        }
        this.setSpeed(0.18D);
        this.setSpecies(this.SPECIES_PHANTOM);
        this.isFlyingMode = true;
        this.setAttackDistance(6.0D);
        this.setDetectionDistance(30.0D);
        this.setFlyingHeight(3);
    }

    public EntityTHPhantom(Level level)
    {
        this(THKaguyaEntities.TH_PHANTOM.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return EntityDanmakuMob.createAttributes();
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
        return 10;
    }
}
