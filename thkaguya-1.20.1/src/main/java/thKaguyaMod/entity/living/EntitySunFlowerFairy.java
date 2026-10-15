package thKaguyaMod.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import thKaguyaMod.init.THKaguyaEntities;

/** ひまわり妖精（1.20.1 移植版） */
public class EntitySunFlowerFairy extends EntityTHFairy
{
    public EntitySunFlowerFairy(EntityType<? extends EntitySunFlowerFairy> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 30;
        this.setForm((byte)10);
        this.setMaxHP(20.0F);
        this.setHealth(20.0F);
    }

    public EntitySunFlowerFairy(Level level)
    {
        this(THKaguyaEntities.SUNFLOWER_FAIRY.get(), level);
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
        return 3;
    }
}
