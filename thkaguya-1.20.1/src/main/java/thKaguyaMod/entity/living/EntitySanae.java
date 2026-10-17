package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 東風谷 早苗（1.20.1 移植版）
 * 原版实现 IMerchant（右击开村民交易界面，1 条交易：64 弹幕素材 → 1 病气平愈御守）。
 */
public class EntitySanae extends EntityTradingDanmakuMob implements IDanmakuMob
{
    /** RenderTHBoss 系のボスなので、バニラのボスバーを出す */
    @Override
    public boolean hasBossBar()
    {
        return true;
    }

    public EntitySanae(EntityType<? extends EntitySanae> type, Level level)
    {
        super(type, level);
        this.danmakuXpReward = 760;
        this.setDanmakuMobName("Sanae Kochiya");
        this.setSpecies(this.SPECIES_HUMAN, this.SPECIES_GOD);
        this.setDanmakuPattern(NOT_ATTACK);
        this.setMaxHP(56.0F);
        this.setHealth(56.0F);
        this.setSpeed(0.5D);
        this.setAttackDistance(14.0D);
        this.setDetectionDistance(0.0D);
        this.setFlyingHeight(0);
        this.isFlyingMode = false;
        this.isSpellCardMode = false;
        this.attackInterval = 0;
    }

    public EntitySanae(Level level)
    {
        this(THKaguyaEntities.SANAE.get(), level);
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
            return EntitySpellCard.SC_SANAE_MiracleFruit;
        case SPELLCARD_ATTACK02:
            return EntitySpellCard.SC_SANAE_Moses_no_Kiseki;
        case SPELLCARD_ATTACK03:
            return EntitySpellCard.SC_SANAE_Yasaka_no_Kamikaze;
        case SPELLCARD_ATTACK04:
            return EntitySpellCard.SC_SANAE_YouryokuSpoiler;
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
        case SPELLCARD_ATTACK02:
        case SPELLCARD_ATTACK03:
        case SPELLCARD_ATTACK04:
            return false;  // 最终阶段：真正死亡
        default:
            return false;
        }
    }

    @Override
    public void danmakuPattern(int level)
    {
        Vec3 look = this.look();
        switch(getDanmakuPattern())
        {
        case NORMAL_ATTACK01:
            danmaku01(look, level);
            break;
        case SPELLCARD_ATTACK01:
            if(attackCounter == 1) { this.useSpellCard(EntitySpellCard.SC_SANAE_MiracleFruit); }
            else if(attackCounter >= 140) { attackCounter = 0; }
            break;
        case SPELLCARD_ATTACK02:
            if(attackCounter == 1) { this.useSpellCard(EntitySpellCard.SC_SANAE_Moses_no_Kiseki); }
            else if(attackCounter >= 140) { attackCounter = 0; }
            break;
        case SPELLCARD_ATTACK03:
            if(attackCounter == 1) { this.useSpellCard(EntitySpellCard.SC_SANAE_Yasaka_no_Kamikaze); }
            else if(attackCounter >= 140) { attackCounter = 0; }
            break;
        case SPELLCARD_ATTACK04:
            if(attackCounter == 1) { this.useSpellCard(EntitySpellCard.SC_SANAE_YouryokuSpoiler); }
            else if(attackCounter >= 140) { attackCounter = 0; }
            break;
        default:
            break;
        }
    }

    private void danmaku01(Vec3 look, int level)
    {
        if(attackCounter < 50)
        {
            setStopStart();
            double size = 2.0D;
            int div = 10;
            double divLength = size / (double)div;

            double px = Math.cos(Math.PI / 2.0D) * size;
            double py = Math.sin(Math.PI / 2.0D) * size;
            double pz = 0.0D;

            float angle = 252F / 180F * 3.141593F;

            for(int i = 0; i < attackCounter; i++)
            {
                px += Math.cos(angle) * divLength;
                py += Math.sin(angle) * divLength;

                if((i % div) == (div - 1))
                {
                    angle += 144F / 180F * 3.141593F;
                }
            }
            Vec3 pos = THShotLib.getOuterProduct(look, new Vec3(px, py, pz));
            pos = new Vec3(pos.x + this.getX(), pos.y + THShotLib.getPosYFromEye(this), pos.z + this.getZ());

            THShotLib.createShot(this, this, pos, look, 0F, 0.0F, 0.5F, 0.03F, gravity_Zero(), ShotData.shot(FORM_TINY, BLUE, 55 - attackCounter, 120));
        }
        else if(attackCounter == 50)
        {
            setStopEnd();
        }
        else if(attackCounter >= 100)
        {
            attackCounter = 0;
        }
    }

    @Override
    protected boolean canFairyCall()
    {
        return false;
    }

    /** 原版 EntitySanae.entityInit() 的 buyingList（只有 1 条交易）。 */
    @Override
    protected MerchantOffers createOffers()
    {
        MerchantOffers offers = new MerchantOffers();
        // 原版：new MerchantRecipe(new ItemStack(THKaguyaItems.shot_material, 64, 0),
        //                         new ItemStack(THKaguyaItems.illness_recovery_charm, 1, 0))
        offers.add(new MerchantOffer(
                new ItemStack(THKaguyaItems.SHOT_MATERIAL.get(), 64),
                new ItemStack(THKaguyaItems.ILLNESS_RECOVERY_CHARM.get()),
                7, 0, 0.0F));
        return offers;
    }

    @Override
    protected Component getTradeTitle()
    {
        // 原版：StatCollector.translateToLocal("entity.Sanae.name")
        return Component.translatable("entity.thkaguya.sanae");
    }

    @Override
    public int getMaxSpawnClusterSize()
    {
        return 1;
    }
}
