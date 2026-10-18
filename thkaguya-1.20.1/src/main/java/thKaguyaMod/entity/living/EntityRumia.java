package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaEntities;

/** ルーミア（1.20.1 移植版） */
public class EntityRumia extends EntityDanmakuMob
{
    /** 原版 RenderTHBoss 系のボスなので、バニラのボスバーを出す */
    @Override
    public boolean hasBossBar()
    {
        return true;
    }
    public EntityRumia(EntityType<? extends EntityRumia> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 160;
        this.noCulling = true;
        this.setDanmakuPattern(NORMAL_ATTACK01);
        this.setMaxHP(36.0F);
        this.setHealth(36.0F);
        this.setDanmakuMobName("Rumia");
        this.setSpecies(this.SPECIES_YOUKAI);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(12.0D);
        this.setFlyingHeight(2);
        this.isFlyingMode = true;
        this.setNoGravity(true);
    }

    public EntityRumia(Level level)
    {
        this(THKaguyaEntities.RUMIA.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return EntityDanmakuMob.createAttributes();
    }

    @Override
    public int getUsingSpellCardNo()
    {
        switch(getDanmakuPattern())
        {
        case SPELLCARD_ATTACK01:
            return EntitySpellCard.SC_RUMIA_MoonlightRay;
        default:
            return -1;
        }
    }

    @Override
    protected boolean onDeathUpdate()
    {
        switch(getDanmakuPattern())
        {
        case NORMAL_ATTACK01:
            moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 44.0F, 60);
            this.setAttackDistance(14.0D);
            this.setDetectionDistance(30.0D);
            this.setFlyingHeight(2);
            return true;
        case SPELLCARD_ATTACK01:
            // 原版 EntityRumia.java:74：最終スペルカードを破壊されると ATTACK_END へ移行（死亡演出）
            moveDanmakuAttack(ATTACK_END, 30, 0F, 60);
            return true;
        default:
            return false;
        }
    }

    /** 原版 EntityRumia の死亡演出は 9 tick 間隔で爆発（他ボスは 6）。 */
    @Override
    protected int deathExplosionInterval()
    {
        return 9;
    }

    @Override
    public void danmakuPattern(int level)
    {
        switch(getDanmakuPattern())
        {
        case NORMAL_ATTACK01:
        {
            Vec3 look = this.look();
            if(attackCounter == 1)
            {
                THShotLib.playShotSound(this);
                for(int i = 0; i < 1 + level; i++)
                {
                    THShotLib.createCircleShot(this, pos(), THShotLib.angle(this.getYRot(), this.getXRot()), 2.0D, 0.4D, -0.3D, ShotData.shot(FORM_CIRCLE, BLUE, 10 + i), 12 + 3 * level);
                }
            }
            else if(attackCounter == 14)
            {
                THShotLib.playShotSound(this);
                for(int i = 0; i < 1 + level; i++)
                {
                    THShotLib.createCircleShot(this, pos(), THShotLib.angle(this.getYRot(), this.getXRot()), 2.0D, 0.4D, -0.3D, ShotData.shot(FORM_CIRCLE, THShotLib.RED, 10 + i), 12 + 3 * level);
                }
            }
            else if(attackCounter == 24)
            {
                moveRight(0.3D, 20);
            }
            else if(attackCounter >= 62 && attackCounter < 67)
            {
                THShotLib.playShotSound(this);
                int i = attackCounter - 62;
                THShotLib.createCircleShot(this, pos(), THShotLib.angle(this.getYRot() + i * 5F, this.getXRot()), 0.2D + level * 0.1D, ShotData.shot(FORM_TINY, attackCounter % 7), 5 + level * 2);
            }
            else if(attackCounter == 67)
            {
                moveLeft(0.3D, 20);
            }
            if(attackCounter >= 127)
            {
                attackCounter = 0;
            }
            break;
        }
        case SPELLCARD_ATTACK01:
            if(attackCounter == 1)
            {
                useSpellCard(getUsingSpellCardNo());
            }
            else if(attackCounter < 60 && attackCounter % 9 == 0)
            {
                THShotLib.createCircleShot(this, pos(), THShotLib.angle(this.getYRot(), this.getXRot()), 0.1D + level * 0.1D, ShotData.shot(FORM_TINY, WHITE), 15 + level * 3);
                THShotLib.playShotSound(this);
            }
            else if(attackCounter >= 120)
            {
                attackCounter = 0;
            }
            break;
        default:
            break;
        }
    }

    @Override
    protected boolean canFairyCall()
    {
        return false;
    }

    @Override
    public int getMaxSpawnClusterSize()
    {
        return 1;
    }

    /**
     * 原版 EntityRumia.dropFewItems（:160 付近）。
     * 弾素材 SMALL（青）＋ 威力/得点アイテムを落とす。
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit)
        {
            this.dropShotItem(thKaguyaMod.item.ItemTHShot.SMALL,
                    8 + this.getRandom().nextInt(2) + looting, 5, 32, THShotLib.BLUE, 0, 0, 2);
            this.dropPowerUpItem(8 + this.getRandom().nextInt(5) + this.getRandom().nextInt(1 + looting));
            this.dropPointItem(8 + this.getRandom().nextInt(5) + this.getRandom().nextInt(1 + looting));
        }
    }
}
