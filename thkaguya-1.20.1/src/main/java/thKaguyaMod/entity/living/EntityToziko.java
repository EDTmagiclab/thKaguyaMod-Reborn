package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.spellcard.THSC_GagoujiCyclone;
import thKaguyaMod.init.THKaguyaEntities;

/** 蘇我 屠自古（1.20.1 移植版） */
public class EntityToziko extends EntityDanmakuMob
{
    public EntityToziko(EntityType<? extends EntityToziko> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 160;
        this.setDanmakuPattern(NORMAL_ATTACK01);
        this.setMaxHP(40.0F);
        this.setHealth(40.0F);
        this.setDanmakuMobName("Soga no Toziko");
        this.setSpecies(this.SPECIES_PHANTOM_GHOST);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(12.0D);
        this.setFlyingHeight(2);
        this.isFlyingMode = true;
        this.setNoGravity(true);
    }

    public EntityToziko(Level level)
    {
        this(THKaguyaEntities.TOZIKO.get(), level);
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
            return 40; // 原版 THSC_GagoujiCyclone
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
            moveDanmakuAttack(ATTACK_END, 30, 0F, 60);
            return true;
        default:
            return false;
        }
    }

    @Override
    public void danmakuPattern(int level)
    {
        switch(getDanmakuPattern())
        {
        case NORMAL_ATTACK01:
            if(attackCounter % 80 < 20)
            {
                THShotLib.playShotSound(this);
                for(int i = 0; i < 1 + level; i++)
                {
                    THShotLib.createShot(this, this, THShotLib.pos_Random(pos(), attackCounter * 0.2D), THShotLib.angle_Random(), 0F, 0.3D, 0.0D, -0.03D, gravity_Zero(), ShotData.shot(FORM_ARROW, BLUE, 1, 20, THSC_GagoujiCyclone.SPECIAL_TOZIKO01));
                }
            }
            else if(attackCounter == 24)
            {
                if(this.random.nextInt(2) == 0)
                {
                    this.moveRight(this.random.nextDouble() * 0.3D + 0.2D, 16);
                }
                else
                {
                    this.moveLeft(this.random.nextDouble() * 0.3D + 0.2D, 16);
                }
            }
            if(attackCounter >= 80)
            {
                attackCounter = 0;
            }
            break;
        case SPELLCARD_ATTACK01:
            if(attackCounter == 1)
            {
                useSpellCard(getUsingSpellCardNo());
            }
            else if(attackCounter >= 200)
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
}
