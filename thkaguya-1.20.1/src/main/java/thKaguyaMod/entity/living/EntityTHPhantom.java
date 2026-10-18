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

    /**
     * 原版 EntityTHPhantom.onDeathUpdate は setDead() のみ（死亡演出なし）。
     */
    @Override
    protected boolean hasDeathExplosion()
    {
        return false;
    }

    /**
     * 原版 EntityTHPhantom.getSoundVolume()（:265 付近）：幽霊は無音（0.0F）。
     * 基类 EntityDanmakuMob の蝙蝠音（音量 0.3 / ピッチ 1.95）を打ち消す。
     */
    @Override
    protected float getSoundVolume()
    {
        return 0.0F;
    }

    /**
     * 原版 EntityTHPhantom.onUpdate（:74-80）：昼夜光量で焼けて消滅する。
     * 1.7.10 の {@code getBrightness(1.0F)} は「block/sky の合成光量 / 15」なので、
     * 1.20.1 では {@code getMaxLocalRawBrightness}（sky - skyDarken を考慮）で等価。
     */
    @Override
    public void tick()
    {
        super.tick();
        if(this.isAlive() && !this.level().isClientSide)
        {
            float f = this.level().getMaxLocalRawBrightness(this.blockPosition()) / 15.0F;
            if(f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F
                    && this.level().canSeeSky(this.blockPosition()))
            {
                this.setHealth(0.0F);
            }
        }
    }

    /**
     * 原版 EntityTHPhantom.dropFewItems（:124 付近）。
     * 弾素材 SMALL＋威力/得点アイテム＋魂の灯（soul_torch）。
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
        this.dropShotItem(thKaguyaMod.item.ItemTHShot.SMALL,
                this.getRandom().nextInt(2) + looting, 3, 0, thKaguyaMod.THShotLib.RED, 0, 0, 2);
        this.dropPowerUpItem(this.getRandom().nextInt(5) + this.getRandom().nextInt(1 + looting));
        this.dropPointItem(this.getRandom().nextInt(5) + this.getRandom().nextInt(1 + looting));
        int soulTorch = (this.getRandom().nextInt(10) / 9) + this.getRandom().nextInt(1 + looting);
        if (soulTorch > 0)
        {
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    thKaguyaMod.init.THKaguyaItems.SOUL_TORCH.get(), soulTorch));
        }
    }
}
