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

    /**
     * 原版 EntitySunFlowerFairy.dropFewItems（:110 付近）。
     * 弾素材 MEDIUM（緑）＋ 威力/得点アイテム。さらに 5% で extend、25% でスペルカード。
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.getForm() < 0)
        {
            return;
        }
        if (!recentlyHit)
        {
            return;
        }
        this.dropShotItem(thKaguyaMod.item.ItemTHShot.MEDIUM,
                this.getRandom().nextInt(12) + looting * 2, 7, 8, thKaguyaMod.THShotLib.GREEN, 0, 0, 2);
        this.dropPowerUpItem(this.getRandom().nextInt(20) + this.getRandom().nextInt(3 + looting * 2));
        this.dropPointItem(this.getRandom().nextInt(20) + this.getRandom().nextInt(3 + looting * 2));
        if (this.getRandom().nextInt(100) < 5)
        {
            this.dropExtendItem(1);
        }
        else if (this.getRandom().nextInt(100) < 25)
        {
            this.dropSpellCardItem(1);
        }
    }
}
