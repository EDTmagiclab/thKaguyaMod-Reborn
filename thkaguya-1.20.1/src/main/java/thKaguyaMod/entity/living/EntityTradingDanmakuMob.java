package thKaguyaMod.entity.living;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * 会开原版商人界面的弹幕 BOSS 基类（1.20.1 移植版）。
 *
 * <p>原版 1.7.10 的 {@code EntitySanae} / {@code EntitySakuya} 都实现 {@code IMerchant}：
 * 右击 → {@code displayGUIMerchant(this, 名字)} → 原版村民交易界面（{@code GuiMerchant}），
 * 取出商品时回调 {@code useRecipe()} → {@code MerchantRecipe.incrementToolUses()}。</p>
 *
 * <p>1.20.1 对应关系：{@code IMerchant} → {@link Merchant}，
 * {@code MerchantRecipeList} → {@link MerchantOffers}，
 * {@code displayGUIMerchant} → {@link Merchant#openTradingScreen}（内部开原版
 * {@code MerchantMenu} 并把报价单发给客户端）。</p>
 *
 * <p>交易表每个商人固定不变（原版 {@code buyingList} 在 {@code entityInit()} 里一次性建好），
 * 且没有补货逻辑（原版 {@code useRecipe()} 里的重置计时器整段被注释）→ 默认 7 次卖完为止。</p>
 */
public abstract class EntityTradingDanmakuMob extends EntityDanmakuMob implements Merchant
{
    /** 原版 {@code IMerchant} 的 buyingPlayer（getCustomer / setCustomer） */
    @Nullable
    private Player tradingPlayer;
    /** 原版 {@code IMerchant} 的 buyingList（MerchantRecipeList） */
    @Nullable
    private MerchantOffers offers;
    /** 折扣剩余 tick（>0 时报价按五折；战斗胜利后由 reviveAsMerchant 设置） */
    protected int discountTicks;

    /** 胜利折扣时长：5 分钟 */
    public static final int DISCOUNT_DURATION = 6000;

    protected EntityTradingDanmakuMob(EntityType<? extends EntityTradingDanmakuMob> type, Level level)
    {
        super(type, level);
    }

    /** 原版 {@code entityInit()} 里往 buyingList 加的那些 MerchantRecipe。 */
    protected abstract MerchantOffers createOffers();

    /** 交易界面的标题（原版 {@code StatCollector.translateToLocal("entity.Sanae.name")}）。 */
    protected abstract Component getTradeTitle();

    protected MerchantOffers offers()
    {
        if (this.offers == null)
        {
            this.offers = this.createOffers();
        }

        return this.offers;
    }

    /** 现在是否处于胜利折扣期 */
    public boolean hasDiscount()
    {
        return this.discountTicks > 0;
    }

    /** 进货数量按折扣倍率换算（五折，至少 1 个） */
    protected int discounted(int base)
    {
        if(this.discountTicks <= 0)
        {
            return base;
        }
        return Math.max(1, (int)Math.ceil(base * 0.5D));
    }

    /** 报价表按当前折扣状态重建（已打开的交易界面需重新打开才会刷新） */
    protected void refreshOffers()
    {
        this.offers = this.createOffers();
    }

    /** 胜利折扣：重建报价表并开始倒计时 */
    protected void startDiscount(int ticks)
    {
        this.discountTicks = ticks;
        this.refreshOffers();
    }

    /** 戦闘に負けたときに戻す最大 HP（各商人ボスの初期値で上書きする） */
    protected float merchantMaxHealth()
    {
        return 20.0F;
    }

    /**
     * 実験的機能：弾幕戦に負けた商人ボスをその場で復活させる。
     *
     * <p>{@code NOT_ATTACK} に戻す＝無敵・非交戦状態に戻るので、右クリックで取引できる。
     * HP は {@link #merchantMaxHealth()}（フェーズ移行で 40 に落ちている）へ戻し、
     * 周囲の残留弾と常駐魔法陣を片付けてから 5 分の五折を開始する。</p>
     */
    protected void reviveAsMerchant(int discountDuration)
    {
        this.setTarget(null);
        this.setStopEnd();
        this.attackCounter = 0;
        this.deathTime = 0;
        this.attackInterval = 0;
        this.isSpellCardMode = false;
        this.clearSpellCardCircle();
        this.setFlyingHeight(0);
        this.setDanmakuPattern(NOT_ATTACK);

        float maxHealth = this.merchantMaxHealth();
        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        this.setHealth(maxHealth);

        thKaguyaMod.THShotLib.danmakuRemove(this, 40.0D, "Enemy", true);
        this.clearBattleGlow();
        this.startDiscount(discountDuration);
    }

    /** 折扣倒计时（服务端）；到 0 时把报价表恢复原价 */
    @Override
    public void tick()
    {
        super.tick();
        if(!this.level().isClientSide && this.discountTicks > 0)
        {
            this.discountTicks--;
            if(this.discountTicks == 0)
            {
                this.refreshOffers();
            }
        }
    }

    @Override
    public void setTradingPlayer(@Nullable Player player)
    {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer()
    {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers()
    {
        return this.offers();
    }

    @Override
    public void overrideOffers(MerchantOffers offers)
    {
        this.offers = offers;
    }

    /** 原版 {@code useRecipe()}：{@code incrementToolUses()} + 重置 livingSoundTime。 */
    @Override
    public void notifyTrade(MerchantOffer offer)
    {
        offer.increaseUses();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack)
    {
        // 原版 func_110297_a_() 里的 "mob.villager.yes/no" 播放在原版就是注释掉的，这里保持静音
    }

    @Override
    public int getVillagerXp()
    {
        return 0;
    }

    @Override
    public void overrideXp(int xp)
    {
    }

    @Override
    public boolean showProgressBar()
    {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound()
    {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide()
    {
        return this.level().isClientSide;
    }

    /**
     * 原版 {@code interact(EntityPlayer)}：手上不是刷怪蛋、自己活着、玩家没潜行 → 开交易界面。
     * 只在服务端开（客户端由 MerchantMenu 的报价同步包接管）。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        ItemStack held = player.getItemInHand(hand);
        boolean spawnEgg = held.getItem() instanceof SpawnEggItem;

        // 実験的機能：弾幕戦の最中は商人メニューを出さない（交戦中に取引できてしまうと不自然）
        if (!spawnEgg && this.isAlive() && !player.isShiftKeyDown() && this.getDanmakuPattern() == NOT_ATTACK)
        {
            if (!this.level().isClientSide)
            {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getTradeTitle(), 1);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        // 原版 writeEntityToNBT 存 buyingList（"buyingList"）
        tag.put("Offers", this.offers().createTag());
        tag.putInt("DiscountTicks", this.discountTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);

        // 原版 readEntityFromNBT：有 "buyingList" 才恢复，否则用 entityInit 的默认表
        if (tag.contains("Offers"))
        {
            this.offers = new MerchantOffers(tag.getCompound("Offers"));
        }

        this.discountTicks = tag.getInt("DiscountTicks");
        // 読み込み時に折扣中なら、保存されていた报价を半額で作り直す
        if (this.discountTicks > 0)
        {
            this.refreshOffers();
        }
    }
}
