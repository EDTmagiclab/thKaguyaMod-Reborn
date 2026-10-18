package thKaguyaMod.entity.living;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 十六夜 咲夜（1.20.1 移植版）
 * 原版实现 IMerchant（右击开村民交易界面，2 条交易：银刀 / 怀表）。
 */
public class EntitySakuya extends EntityTradingDanmakuMob implements IDanmakuMob
{
    /** RenderTHBoss 系のボスなので、バニラのボスバーを出す */
    @Override
    public boolean hasBossBar()
    {
        return true;
    }

    public EntitySakuya(EntityType<? extends EntitySakuya> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 250;
        this.setDanmakuMobName("Sakuya Izayoi");
        this.setSpecies(this.SPECIES_HUMAN);
        this.setDanmakuPattern(NOT_ATTACK);
        this.setMaxHP(44.0F);
        this.setHealth(44.0F);
        this.setSpeed(0.5F);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(0.0D);
        this.setFlyingHeight(0);
        this.isFlyingMode = false;
        this.isSpellCardMode = false;
        this.attackInterval = 0;
    }

    public EntitySakuya(Level level)
    {
        this(THKaguyaEntities.SAKUYA.get(), level);
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
            return EntitySpellCard.SC_SAKUYA_SatuzinDoll;
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
            // 原版 EntitySakuya.java:100：スペルカードを破壊されると ATTACK_END へ移行（死亡演出）
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
                this.useSpellCard(getUsingSpellCardNo());
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

    /** 原版 EntitySakuya.entityInit() 的 buyingList（2 条交易）。 */
    @Override
    protected MerchantOffers createOffers()
    {
        MerchantOffers offers = new MerchantOffers();
        // 原版：new MerchantRecipe(new ItemStack(THKaguyaItems.shot_material, 16, 0),
        //                         new ItemStack(THKaguyaItems.silver_knife, 1, 0))
        offers.add(new MerchantOffer(
                new ItemStack(THKaguyaItems.SHOT_MATERIAL.get(), 16),
                new ItemStack(THKaguyaItems.SILVER_KNIFE_BLUE.get()),
                7, 0, 0.0F));
        // 原版：new MerchantRecipe(new ItemStack(THKaguyaItems.shot_material, 64, 0),
        //                         new ItemStack(THKaguyaItems.sakuya_stopwatch, 1, 0))
        offers.add(new MerchantOffer(
                new ItemStack(THKaguyaItems.SHOT_MATERIAL.get(), 64),
                new ItemStack(THKaguyaItems.SAKUYA_STOPWATCH.get()),
                7, 0, 0.0F));
        return offers;
    }

    @Override
    protected Component getTradeTitle()
    {
        // 原版：StatCollector.translateToLocal("entity.Sakuya.name")
        return Component.translatable("entity.thkaguya.sakuya");
    }

    @Override
    public int getMaxSpawnClusterSize()
    {
        return 1;
    }

    /**
     * 無敵かどうか（原版 EntitySakuya.java:203-211）。
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

    /** 原版 EntitySakuya.java:187-198：無敵状態ならダメージを 5% に軽減。 */
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
