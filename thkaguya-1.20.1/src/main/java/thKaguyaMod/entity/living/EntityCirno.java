package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.LaserData;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** チルノ（1.20.1 移植版） */
public class EntityCirno extends EntityDanmakuMob
{
    private int randomValue;

    public EntityCirno(EntityType<? extends EntityCirno> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 250;
        this.setDanmakuMobName("Cirno");
        this.setSpecies(this.SPECIES_FAIRY, this.SPECIES_FAIRY_ICE);
        this.setDanmakuPattern(NORMAL_ATTACK01);
        this.setMaxHP(38.0F);
        this.setHealth(38.0F);
        this.setSpeed(0.5F);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(30.0D);
        this.setFlyingHeight(2);
        this.isFlyingMode = true;
        this.isSpellCardMode = false;
        this.attackInterval = 0;
        this.randomValue = 0;
        this.setNoGravity(true);
    }

    public EntityCirno(Level level)
    {
        this(THKaguyaEntities.CIRNO.get(), level);
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
            return EntitySpellCard.SC_CIRNO_IcicleFall;
        case SPELLCARD_ATTACK02:
            return EntitySpellCard.SC_CIRNO_PerfectFreeze;
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
            moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 46.0F, 60);
            return true;
        case SPELLCARD_ATTACK01:
            moveDanmakuAttack(NORMAL_ATTACK02, 60, 32.0F, 20);
            return true;
        case NORMAL_ATTACK02:
            moveDanmakuAttack(SPELLCARD_ATTACK02, 40, 58.0F, 60);
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
        Vec3 angle = THShotLib.angle(this.getYRot(), this.getXRot());
        switch(getDanmakuPattern())
        {
        case NORMAL_ATTACK01:
            danmaku01(angle, level);
            break;
        case SPELLCARD_ATTACK01:
            spellcard01(angle, level);
            break;
        case NORMAL_ATTACK02:
            danmaku02(angle, level);
            break;
        case SPELLCARD_ATTACK02:
            spellcard02(angle, level);
            break;
        default:
            break;
        }
    }

    private void danmaku01(Vec3 angle, int level)
    {
        if(attackCounter == 40)
        {
            angle = this.look();
            for(int i = 0; i < 7; i++)
            {
                THShotLib.createRingShot(this, this, pos(), angle, 0F, 9999, 0.6D + level * 0.15D, 0.6D + level * 0.15D, 0.1D, gravity_Zero(), ShotData.shot(FORM_CRYSTAL, AQUA, 0.2F, 4.0F, i, 120, 0), i + 2, 0.0D, i * level, this.random.nextFloat() * 360F);
            }
            THShotLib.playShotSound(this);
        }
        if(attackCounter >= 50)
        {
            attackCounter = 0;
        }
    }

    private void spellcard01(Vec3 angle, int level)
    {
        if(attackCounter == 1)
        {
            useSpellCard(getUsingSpellCardNo());
        }
        else if(attackCounter > 30 && attackCounter < 170 && attackCounter % 17 == 0 && level >= 2)
        {
            float size = THShotLib.SIZE[THShotLib.SMALL[0] / 8];
            if(level >= 3)
            {
                size = THShotLib.SIZE[THShotLib.MEDIUM[0] / 8];
            }
            THShotLib.createWideShot(this, this, pos(), angle, 0.3D + level * 0.1D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_SMALL, YELLOW, size, 4.0F, 3, 80), 3, 70F, 0.2D);
            THShotLib.playShotSound(this);
        }
        else if(attackCounter >= 240)
        {
            attackCounter = 0;
        }
    }

    private void danmaku02(Vec3 angle, int level)
    {
        if(attackCounter < 50 && attackCounter % 6 == 0)
        {
            THShotLib.createCircleShot(this, pos(), angle, 0.2D, ShotData.shot(FORM_TINY, WHITE), 5 + level * 2);
            THShotLib.createCircleShot(this, pos(), angle, 0.3D + level * 0.1D, ShotData.shot(FORM_CIRCLE, BLUE), 5 + level * 2);
            THShotLib.playShotSound(this);
        }
        else if(attackCounter == 55 || attackCounter == 65 || attackCounter == 75)
        {
            THShotLib.createWideLaserA(this, this.pos(), this.look(), 0.3D + level * 0.2D, LaserData.laser(WHITE, 0.1F, 4.0F, 4.0F), 3, 30F);
        }
        if(attackCounter > 128)
        {
            attackCounter = 0;
        }
    }

    private void spellcard02(Vec3 angle, int level)
    {
        if(attackCounter == 1)
        {
            randomValue = this.random.nextInt(2);
            useSpellCard(getUsingSpellCardNo());
        }
        else if(attackCounter >= 70 && attackCounter <= 90 && attackCounter % 3 == 0)
        {
            int way = 4 + randomValue;
            for(int i = 0; i < 3 + level; i++)
            {
                THShotLib.createWideShot(this, this, pos(), angle, 0.3D + level * 0.1D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_CIRCLE, BLUE, 0.3F, 4.0F, i, 80), way, 70F, 0.2D);
            }
            THShotLib.playShotSound(this);
        }
        else if(attackCounter >= 170)
        {
            attackCounter = 0;
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

    /** 死亡掉落：SPELLCARD_ATTACK02（完美冻结符卡阶段）被玩家击杀时掉落冰剑 */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        // 原版 dropFewItems: スペルカード攻撃中に倒されたら得点／威力アイテムを 40 個リング状に落とす
        if (recentlyHit && this.isSpellCardAttack())
        {
            int j = 40;
            for (int k = 0; k < j; k += 2)
            {
                float yaw = 360F / (float)j * (float)k;
                float pitch = (float)Math.sin(yaw / 180F * Math.PI * 4F) * 20F - 60F;
                this.dropPointItem(this.position(), THShotLib.getVecFromAngle(yaw, pitch, 1.0F));

                yaw = 360F / (float)j * (float)(k + 1);
                pitch = (float)Math.cos(yaw / 180F * Math.PI * 4F) * 20F - 60F;
                this.dropPowerUpItem(this.position(), THShotLib.getVecFromAngle(yaw, pitch, 1.0F));
            }
        }

        if(recentlyHit && getDanmakuPattern() == SPELLCARD_ATTACK02)
        {
            this.spawnAtLocation(new ItemStack(THKaguyaItems.ICICLE_SWORD.get(), 1));
            this.dropExtendItem(this.position(), THShotLib.angle(this.getYRot(), -90F));
        }
    }
}
