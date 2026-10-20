package thKaguyaMod.entity.living;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;

/** 豊聡耳 神子（1.20.1 移植版） */
public class EntityMiko extends EntityDanmakuMob
{
    /** 原版 RenderTHBoss 系のボスなので、バニラのボスバーを出す */
    @Override
    public boolean hasBossBar()
    {
        return true;
    }
    public EntityMiko(EntityType<? extends EntityMiko> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 250;
        this.setDanmakuMobName("Toyosatomimi no Miko");
        this.setSpecies(this.SPECIES_HUMAN_SAINT);
        this.setDanmakuPattern(NOT_ATTACK);
        this.setMaxHP(74.0F);
        this.setHealth(74.0F);
        this.setSpeed(0.4F);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(0.0D);
        this.setFlyingHeight(0);
        this.isFlyingMode = false;
        this.isSpellCardMode = false;
        this.attackInterval = 0;
    }

    public EntityMiko(Level level)
    {
        this(THKaguyaEntities.MIKO.get(), level);
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
            // 原版 EntityMiko.java:68：スペルカードを破壊されると ATTACK_END へ移行（死亡演出）
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

    /**
     * 右クリックで弾幕難易度を切り替える（原版 EntityMiko#interact L250-281 の移植）。
     * EASY(1) → NORMAL(2) → HARD(3) → LUNATIC(4) → EASY(1)。原版同様に config 値そのものを書き換える。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        // 原版: ticksExisted % 2 == 0 のときは反応しない（連打で 2 回進むのを防ぐ）
        if(this.tickCount % 2 == 0)
        {
            return InteractionResult.PASS;
        }

        int level = THKaguyaConfig.danmakuLevel;
        String str;

        if(level == DanmakuConstants.EASY)
        {
            level = DanmakuConstants.NORMAL;
            str = "NORMAL";
        }
        else if(level == DanmakuConstants.NORMAL)
        {
            level = DanmakuConstants.HARD;
            str = "HARD";
        }
        else if(level == DanmakuConstants.HARD)
        {
            level = DanmakuConstants.LUNATIC;
            str = "LUNATIC";
        }
        else
        {
            level = DanmakuConstants.EASY;
            str = "EASY";
        }

        THKaguyaConfig.danmakuLevel = level;
        player.displayClientMessage(Component.literal(str), false);
        return InteractionResult.SUCCESS;
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
     * 無敵かどうか（原版 EntityMiko.java:171-179）。
     * 非攻撃状態（NOT_ATTACK）のときは無敵。基类は attackInterval しか見ないため、
     * この覆写が無いと待機中の神子を直接殴り殺せてしまう。
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

    /** 原版 EntityMiko.java:155-166：無敵状態なら受けるダメージを 5% に軽減。 */
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
}
