package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 東風谷 早苗（1.20.1 移植版）
 * 原版实现 IMerchant（右击开村民交易界面，1 条交易：64 弹幕素材 → 1 病气平愈御守）。
 */
public class EntitySanae extends EntityTradingDanmakuMob implements IDanmakuMob
{
    /** 符卡戦の進行位置（0=01 枚目 … 3=04 枚目）。非符を挟むたびに +1 する。 */
    private int spellCardIndex;

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

    /**
     * 実験的機能：諏訪子の帽子を被り、御柱を手に持って右クリックすると弾幕戦が始まる。
     *
     * <p>原版の早苗は商人のままで、戦闘遷移コードは書かれていたが入口が無かった。
     * 商人機能はそのまま残る（条件を満たさない右クリックは従来どおり取引画面）。</p>
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if(THKaguyaConfig.enableSanaeBossFight
                && getDanmakuPattern() == NOT_ATTACK
                && player.getItemInHand(hand).is(THKaguyaItems.ONBASHIRA.get())
                && player.getItemBySlot(EquipmentSlot.HEAD).is(THKaguyaItems.SUWAKO_HAT.get()))
        {
            if(!this.level().isClientSide)
            {
                this.spellCardIndex = 0;
                if(this.beginDanmakuBattle(player))
                {
                    this.giveBattleReward(player);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /**
     * 開戦祝い：諏訪子系の符卡を 1 枚ランダムで渡す。
     * （大奇跡「八坂の神風」／崇符「洩矢大人」／土着神「ケロちゃん風雨に負けず」）
     */
    private void giveBattleReward(Player player)
    {
        int[] pool = {
                EntitySpellCard.SC_SANAE_Yasaka_no_Kamikaze,
                EntitySpellCard.SC_SUWAKO_MishagujiSama,
                EntitySpellCard.SC_SUWAKO_Kerochan_Fuu_ni_Makezu
        };
        int no = pool[this.getRandom().nextInt(pool.length)];
        ItemStack card = new ItemStack(THKaguyaItems.SPELL_CARD.get());
        card.setDamageValue(no);
        if(!player.getInventory().add(card))
        {
            player.drop(card, false);
        }
        player.displayClientMessage(Component.translatable("message.thkaguya.boss_battle_reward",
                Component.translatable("item.thkaguya.spell_card." + no)), false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("SanaeSpellCardIndex", this.spellCardIndex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        int index = tag.getInt("SanaeSpellCardIndex");
        this.spellCardIndex = index < 0 ? 0 : (index > 3 ? 3 : index);
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
            // 実験的機能：非符 → 符卡01 → 非符 → 02 → 非符 → 03 → 非符 → 04 → 終了
            moveDanmakuAttack(SPELLCARD_ATTACK01 + this.spellCardIndex, 40, 60.0F, 160);
            return true;
        case SPELLCARD_ATTACK01:
        case SPELLCARD_ATTACK02:
        case SPELLCARD_ATTACK03:
        case SPELLCARD_ATTACK04:
            // 一枚破られるごとに、次の符卡が残っていれば非符（NORMAL）へ戻して繋ぐ
            if(this.spellCardIndex < 3)
            {
                this.spellCardIndex++;
                moveDanmakuAttack(NORMAL_ATTACK01, 40, 60.0F, 160);
            }
            else
            {
                // 原版 EntitySanae.java:113-122：最後の符卡を破壊されたら ATTACK_END へ（死亡演出）
                moveDanmakuAttack(ATTACK_END, 90, 40.0F, 160);
            }
            return true;
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
    /** 戦闘に負けたときに戻す最大 HP（原版コンストラクタの 56） */
    @Override
    protected float merchantMaxHealth()
    {
        return 56.0F;
    }

    /**
     * 実験的機能：最後の段階（ATTACK_END）で倒されたら死なずに商人へ戻る。
     *
     * <p>元の死亡処理（{@code super.die}）は呼ばない＝レベルや通常ドロップは出さないが、
     * 各段階を割ったときの符卡/素材は既に {@code dropLootForPattern} で出ている。
     * 代わりに 5 分間の五折を解放し、倒したプレイヤーにだけ通知する。</p>
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source)
    {
        if(!this.level().isClientSide && getDanmakuPattern() == ATTACK_END)
        {
            this.reviveAsMerchant(DISCOUNT_DURATION);
            if(source.getEntity() instanceof Player player)
            {
                player.displayClientMessage(Component.translatable("message.thkaguya.merchant_discount",
                        this.getTradeTitle(), DISCOUNT_DURATION / 1200), false);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            return;
        }
        super.die(source);
    }

    @Override
    protected MerchantOffers createOffers()
    {
        MerchantOffers offers = new MerchantOffers();
        // 原版：new MerchantRecipe(new ItemStack(THKaguyaItems.shot_material, 64, 0),
        //                         new ItemStack(THKaguyaItems.illness_recovery_charm, 1, 0))
        offers.add(new MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(THKaguyaItems.SHOT_MATERIAL.get(), discounted(64)),
                new ItemStack(THKaguyaItems.ILLNESS_RECOVERY_CHARM.get()),
                7, 0, 0.0F));
        // 実験的機能：弾幕之素 32 個で「空中落物の奇跡」1 枚（＝抽選券）
        ItemStack fafrotskies = new ItemStack(THKaguyaItems.SPELL_CARD.get());
        fafrotskies.setDamageValue(EntitySpellCard.SC_SANAE_Fafrotskies_no_Kiseki);
        offers.add(new MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(THKaguyaItems.SHOT_MATERIAL.get(), discounted(32)),
                fafrotskies,
                999, 0, 0.0F));
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

    /**
     * 原版 EntitySanae.dropFewItems（:248 付近）。
     * 弾素材 STAR（緑・BOUND02 反射 3 回）＋ 威力/得点アイテム。
     * 原版は「直前にスペルカードを使っていた場合のみ」落とす。
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(level, source, recentlyHit);        int looting = this.getLootingLevel(source);
        if (recentlyHit && this.getUsingSpellCardNo() != -1)
        {
            this.dropShotItem(thKaguyaMod.item.ItemTHShot.STAR,
                    16 + this.getRandom().nextInt(2) + looting, 5, 32, THShotLib.GREEN, 0, THShotLib.BOUND02, 2);
            this.dropPowerUpItem(25 + this.getRandom().nextInt(5) + this.getRandom().nextInt(1 + looting));
            this.dropPointItem(25 + this.getRandom().nextInt(5) + this.getRandom().nextInt(1 + looting));
        }
    }

    /**
     * 無敵かどうか（原版 EntitySanae.java:312-320）。
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

    /** 原版 EntitySanae.java:296-307：無敵状態ならダメージを 5% に軽減。 */
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
