package thKaguyaMod.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaEntities;

/** 博麗 霊夢（1.20.1 移植版） */
public class EntityReimu extends EntityDanmakuMob
{
    public EntityReimu(EntityType<? extends EntityReimu> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 250;
        this.setDanmakuMobName("Reimu Hakurei");
        this.setSpecies(this.SPECIES_HUMAN);
        this.setDanmakuPattern(SPELLCARD_ATTACK01);
        this.setMaxHP(74.0F);
        this.setHealth(74.0F);
        this.setSpeed(0.4F);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(0.0D);
        this.setFlyingHeight(4);
        this.isFlyingMode = true;
        this.isSpellCardMode = false;
        this.attackInterval = 0;
        this.setNoGravity(true);
    }

    public EntityReimu(Level level)
    {
        this(THKaguyaEntities.REIMU.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return EntityDanmakuMob.createAttributes();
    }

    @Override
    public int getUsingSpellCardNo()
    {
        switch(this.getDanmakuPattern())
        {
        case SPELLCARD_ATTACK01:
            return EntitySpellCard.SC_REIMU_MusouFuuin;
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
            setFlyingHeight(2);
            moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 60.0F, 160);
            return true;
        case SPELLCARD_ATTACK01:
            moveDanmakuAttack(ATTACK_END, 90, 40.0F, 160);
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
            break;
        case SPELLCARD_ATTACK01:
            if(attackCounter == 1)
            {
                this.useSpellCard(EntitySpellCard.SC_REIMU_MusouFuuin);
            }
            else if(attackCounter >= 140)
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
