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
    /** 原版 RenderTHBoss 系のボスなので、バニラのボスバーを出す */
    @Override
    public boolean hasBossBar()
    {
        return true;
    }
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
        // 原版 1.7.10 は isFlyingMode = false でも、毎 tick の「足元 8 格の地面を探して
        // motionY を押し戻す」処理（原版 EntityDanmakuMob の飛行高度維持）でホバーしていた。
        // 1.20.1 移植版にはその高度維持処理が無いため、noGravity を切るとそのまま落下する。
        // 灵梦（以及同批的飞天 boss）在原版中的实际观感就是「悬浮」，故这里保持悬浮。
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
            // 原版 EntityReimu.java:66：スペルカードを破壊されると ATTACK_END へ移行（死亡演出）
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

    /**
     * 無敵かどうか（原版 EntityReimu.java:171-179）。
     * 非攻撃状態（NOT_ATTACK）では無敵。基类只判 attackInterval。
     */
    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source)
    {
        if(getDanmakuPattern() == NOT_ATTACK)
        {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    /** 原版 EntityReimu.java:155-166：無敵状態ならダメージを 5% に軽減。 */
    @Override
    protected float getDamageAfterMagicAbsorb(net.minecraft.world.damagesource.DamageSource source, float damage)
    {
        damage = super.getDamageAfterMagicAbsorb(source, damage);
        if(isInvulnerableTo(source))
        {
            damage = (float)((double)damage * 0.05D);
        }
        return damage;
    }

    /**
     * 原版 dropFewItems：符卡攻击状态倒下时喷 12 威力 + 12 得点（实现见基类 dropSpellCardAttackLoot）。
     * 移植版此前整链缺失，三位 Boss 只掉符卡本体、不掉素材。
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.dropSpellCardAttackLoot();
    }
}
