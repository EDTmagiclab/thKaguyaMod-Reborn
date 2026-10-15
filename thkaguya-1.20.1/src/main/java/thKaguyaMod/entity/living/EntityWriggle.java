package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.spellcard.THSC_LittleBugStorm;
import thKaguyaMod.init.THKaguyaEntities;

/** リグル・ナイトバグ（1.20.1 移植版） */
public class EntityWriggle extends EntityDanmakuMob
{
    public EntityWriggle(EntityType<? extends EntityWriggle> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 140;
        this.setDanmakuPattern(NORMAL_ATTACK01);
        this.setMaxHP(30.0F);
        this.setHealth(30.0F);
        this.setDanmakuMobName("Wriggle");
        this.setSpecies(this.SPECIES_YOUKAI_BUG);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(12.0D);
        this.setFlyingHeight(3);
        this.isFlyingMode = true;
        this.setNoGravity(true);
    }

    public EntityWriggle(Level level)
    {
        this(THKaguyaEntities.WRIGGLE.get(), level);
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
            return 35; // 原版 THSC_MeteorOnEarth
        case SPELLCARD_ATTACK02:
            return 34; // 原版 THSC_LittleBugStorm
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
            moveDanmakuAttack(SPELLCARD_ATTACK01, 20, 36.0F, 40);
            this.setAttackDistance(16.0D);
            this.setDetectionDistance(30.0D);
            this.setFlyingHeight(2);
            return true;
        case SPELLCARD_ATTACK01:
            moveDanmakuAttack(SPELLCARD_ATTACK02, 60, 42.0F, 80);
            this.setAttackDistance(15.0D);
            this.setDetectionDistance(30.0D);
            this.setFlyingHeight(4);
            return true;
        case SPELLCARD_ATTACK02:
            return false;  // 最终阶段：真正死亡
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
        {
            Vec3 look = this.look();
            Vec3 angle;
            if(attackCounter < 30)
            {
                if(attackCounter % 15 >= 1 && attackCounter % 15 < 8)
                {
                    angle = THShotLib.getRotationVectorFromAngle(this.getYRot(), this.getXRot(), (attackCounter % 8) * 10F, 1.0);
                    THShotLib.playShotSound(this);
                    THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.2, ShotData.shot(FORM_RICE, YELLOW, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_01));
                    if(level >= HARD)
                    {
                        THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.4, ShotData.shot(FORM_RICE, YELLOW, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_01));
                    }
                }
                else if(attackCounter % 15 >= 8 && attackCounter % 15 < 15)
                {
                    angle = THShotLib.getRotationVectorFromAngle(this.getYRot(), this.getXRot(), -(attackCounter % 8) * 10F, 1.0);
                    THShotLib.playShotSound(this);
                    THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.2, ShotData.shot(FORM_RICE, AQUA, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_02));
                    if(level >= HARD)
                    {
                        THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.4, ShotData.shot(FORM_RICE, AQUA, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_02));
                    }
                }
            }
            else if(attackCounter == 24)
            {
                moveRight(0.3D, 20);
            }
            else if(attackCounter >= 62 && attackCounter < 72)
            {
                THShotLib.playShotSound(this);
                double firstSpeed = 0.3 + 0.1 * level;
                double limit = 0.3;
                double acceleration = -(attackCounter - 61) * 0.005;
                THShotLib.createWideShot(this, pos(), look, firstSpeed, limit, acceleration, ShotData.shot(FORM_SMALL, YELLOW, 10), 5, 90F);
            }
            else if(attackCounter == 67)
            {
                moveLeft(0.3D, 20);
            }
            if(attackCounter >= 147)
            {
                attackCounter = 0;
            }
            break;
        }
        case SPELLCARD_ATTACK01:
            if(attackCounter % 15 == 1 && attackCounter < 120)
            {
                if(level == NORMAL)
                {
                    THShotLib.createSphereShot(this, pos(), look(), 0.2D, ShotData.shot(FORM_MEDIUM, BLUE), 16, this.tickCount * 13F);
                }
                else if(level == HARD)
                {
                    THShotLib.createSphereShot(this, pos(), look(), 0.3D, ShotData.shot(FORM_MEDIUM, BLUE), 24, this.tickCount * 13F);
                }
                else if(level == LUNATIC)
                {
                    THShotLib.createSphereShot(this, pos(), look(), 0.4D, ShotData.shot(FORM_MEDIUM, BLUE), 32, this.tickCount * 13F);
                }
            }
            if(attackCounter == 1)
            {
                useSpellCard(getUsingSpellCardNo());
            }
            else if(attackCounter >= 180)
            {
                attackCounter = 0;
            }
            break;
        case SPELLCARD_ATTACK02:
            if(attackCounter == 1)
            {
                useSpellCard(getUsingSpellCardNo());
            }
            else if(attackCounter >= 260)
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
