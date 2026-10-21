package thKaguyaMod.entity.spellcard;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;
import thKaguyaMod.entity.living.EntityDanmakuMob;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.registry.SpellCardRegistry;

/**
 * スペルカードのEntity（1.20.1 移植版）
 *
 * 阶段 3d-2：符卡实体。管理符卡生命周期，每 tick 调用 THSpellCard.onUpdate()。
 * 阶段 7-P1：补回 `playerSpellCardDanmakuRemove`（玩家符卡宣言后 removeTime 窗口内持续弹消）。
 */
public class EntitySpellCard extends Entity
{
    // スペルカード番号定数（原版 EntitySpellCard，boss 类 getUsingSpellCardNo 引用）
    public static final int SC_REIMU_MusouFuuin = 0;
    public static final int SC_MARISA_MasterSpark = 1;
    public static final int SC_YUYUKO_Kasho_no_Eimin = 2;
    public static final int SC_MARISA_MeteonicShower = 3;
    public static final int SC_YUKARI_Nami_to_Tubu_no_Kyoukai = 4;
    public static final int SC_YUKARI_NijuuKokushichou = 5;
    public static final int SC_REMILIA_ScarletShoot = 6;
    public static final int SC_TENSHI_Zenzinrui_no_Hisouten = 7;
    public static final int SC_SAKUYA_SatuzinDoll = 8;
    public static final int SC_CIRNO_PerfectFreeze = 9;
    public static final int SC_YUKARI_HikouchuuNest = 10;
    public static final int SC_NITORI_Kappa_no_Pororoca = 11;
    public static final int SC_MARISA_StardustReverie = 12;
    public static final int SC_SUWAKO_Kerochan_Fuu_ni_Makezu = 13;
    public static final int SC_SANAE_MiracleFruit = 14;
    public static final int SC_SANAE_Fafrotskies_no_Kiseki = 15;
    public static final int SC_SANAE_YouryokuSpoiler = 16;
    public static final int SC_SANAE_Moses_no_Kiseki = 17;
    public static final int SC_SANAE_Yasaka_no_Kamikaze = 18;
    public static final int SC_CIRNO_IcicleFall = 19;
    public static final int SC_FLANDRE_StarbowBreak = 20;
    public static final int SC_FLANDRE_Catadioptric = 21;
    public static final int SC_SUWAKO_MishagujiSama = 22;
    public static final int SC_REMILIA_RedMagic = 23;
    public static final int SC_SAKUYA_EternalMeek = 24;
    public static final int SC_MARISA_NonDirectionalLaser = 25;
    public static final int SC_YUUKA_Kachoufuugetu_Shoufuurougetu = 26;
    public static final int SC_REMILIA_Spear_the_Gungnir = 27;
    public static final int SC_RUMIA_MoonlightRay = 28;
    public static final int SC_MEIRIN_Houkakenran = 29;
    public static final int SC_MEIRIN_Saikouranbu = 30;
    public static final int SC_PATCHOULI_PrincessUndine = 31;

    private static final EntityDataAccessor<Integer> SPELL_CARD_NUMBER =
            SynchedEntityData.defineId(EntitySpellCard.class, EntityDataSerializers.INT);

    /**
     * 宣言者の entity id（クライアント専用の「誰が撃ったか」情報）。
     *
     * <p>{@code user} は普通のフィールドで同期されないため、クライアント側では常に null。
     * 符卡宣言演出（{@code client/spellcard_cutin}）が半身像を出すには宣言者が要るので、
     * この 1 個だけ同期データに載せる。既定値 {@code -1} は「まだ届いていない」の意。</p>
     */
    private static final EntityDataAccessor<Integer> CASTER_ID =
            SynchedEntityData.defineId(EntitySpellCard.class, EntityDataSerializers.INT);

    /** クライアント側：この実体で演出を出したか（二重発火よけ）。 */
    private boolean cutInFired;

    public LivingEntity user;
    public LivingEntity tgEntity;
    public int count;
    private int spellCardNumber;
    protected Vec3 tgVec;
    private int spellCardUsedTime;
    private int lastTime;
    public int level;

    public EntitySpellCardCircle circle;

    public boolean canMoveInTimeStop;

    Class<?> spellcard;
    THSpellCard useSpellCard;

    public EntitySpellCard(EntityType<?> type, Level level)
    {
        super(type, level);
        lastTime = 0;
    }

    public EntitySpellCard(Level world, LivingEntity spUser, LivingEntity target, int num, int spLevel)
    {
        this(THKaguyaEntities.SPELL_CARD.get(), world);

        float yawRad = (spUser.getYRot() + 30F) / 180F * (float)Math.PI;
        float pitchRad = spUser.getXRot() / 180F * (float)Math.PI;
        double x = spUser.getX() - (double)Mth.sin(yawRad) * (double)Mth.cos(pitchRad);
        double y = spUser.getY() - (double)Mth.sin(pitchRad) + (double)spUser.getEyeHeight() - 0.1D;
        double z = spUser.getZ() + (double)Mth.cos(yawRad) * (double)Mth.cos(pitchRad);
        this.moveTo(x, y, z, spUser.getYRot(), spUser.getXRot());

        tgVec = spUser.getLookAngle();
        user = spUser;
        tgEntity = target;
        spellCardNumber = num;
        setSpellCardNumber(num);
        setCasterId(spUser.getId());
        spellCardUsedTime = 0;
        count = 0;

        level = spLevel;

        if((spellcard = SpellCardRegistry.getSpellCardClass(num)) != null)
        {
            try {
                useSpellCard = (THSpellCard)spellcard.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }

            if(useSpellCard != null)
            {
                useSpellCard.init(world, this, user, tgEntity, level);
                canMoveInTimeStop = useSpellCard.canMoveInTimeStop();
                if(spUser instanceof Player)
                {
                    circle = new EntitySpellCardCircle(world, spUser, useSpellCard.getSpellCardCircleColor(), useSpellCard.getEndTime());
                    if(!world.isClientSide)
                    {
                        world.addFreshEntity(circle);
                    }
                }
            }
        }
        else
        {
            if(!level().isClientSide)
            {
                discard();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // 既定値 -1 は「まだ同期されていない」。0 を既定にすると、0 番（夢想封印）と
        // 未同期が区別できなくなる（SynchedEntityData は値が変わらないと dirty にならず、
        // 0 番のカードは実際に一度も送られてこないため）。
        builder.define(SPELL_CARD_NUMBER, -1);
        builder.define(CASTER_ID, -1);
    }

    /**
     * 同期データが届いた瞬間のフック（クライアント）。
     *
     * <p>符卡宣言演出はここで起動する。{@code EntityJoinLevelEvent} では駄目な理由：
     * {@code ClientboundAddEntityPacket} は同期データを積んでおらず、参加した瞬間に読むと
     * カード番号が既定値に見えて「未同期」と区別できない。ここなら値が入った時点で分かる。</p>
     *
     * <p>カード番号と宣言者の両方が揃うまで待つ（この 2 つは同じ束で来るが、
     * 束の中の順序は保証されないので、片方が先に来た回では発火しない）。</p>
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);

        if (!this.level().isClientSide || cutInFired)
        {
            return;
        }
        if (getCasterId() < 0 || !isCardNumberSynced())
        {
            return;
        }

        cutInFired = true;
        thKaguyaMod.client.spellcard_cutin.SpellCardCutIn.trigger(getSpellCardNumber(), getCasterId());
    }

    /** 卡片番号がクライアントに届いているか（内部表現は -1 が未同期）。 */
    private boolean isCardNumberSynced()
    {
        return this.entityData.get(SPELL_CARD_NUMBER) >= 0;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    private void finish()
    {
        if(!this.level().isClientSide)
        {
            this.discard();
            if(circle != null)
            {
                circle.discard();
            }
        }
    }

    /**
     * プレイヤーが符卡を宣言した直後の弾消し（原版 EntitySpellCard.playerSpellCardDanmakuRemove）。
     * `ticksExisted < time` の間だけ、使用者を中心に半径 40 格の「他人の弾」を消す。
     * 原版は両側で呼んでいたが、ここはサーバのみで消してクライアントへ削除パケットで伝える
     * （見た目は同じで、クライアント側の每 tick 走査を省ける）。
     */
    private void playerSpellCardDanmakuRemove(int time)
    {
        if(!level().isClientSide && user instanceof Player && tickCount < time)
        {
            THShotLib.danmakuRemove(user, 40.0F, "Other", true);
        }
    }

    public void specialProcessInTimeStop()
    {
        if(useSpellCard != null)
        {
            useSpellCard.specialProcessInTimeStop();
        }
    }

    @Override
    public void tick()
    {
        super.tick();

        // 使用者がいないか、死んでいるなら消す
        if(!level().isClientSide && (user == null || !user.isAlive()))
        {
            finish();
            return;
        }

        // 使用者が弾幕MOBなら、スペルカードモードでなければ消す
        if(user != null && user instanceof EntityDanmakuMob)
        {
            EntityDanmakuMob danmakuMob = (EntityDanmakuMob)user;
            if(!danmakuMob.isSpellCardMode)
            {
                finish();
                return;
            }
        }

        if(tickCount > spellCardUsedTime)
        {
            if(spellcard == null || useSpellCard == null)
            {
                return;
            }

            useSpellCard.onUpdate();

            // 原版：宣言后的 removeTime 窗口内，每 tick 消掉使用者周围 40 格的他人弹幕
            playerSpellCardDanmakuRemove(useSpellCard.getRemoveTime());

            if(useSpellCard.time >= useSpellCard.getEndTime())
            {
                finish();
                return;
            }
        }

        if(spellCardUsedTime < tickCount)
        {
            spellCardUsedTime = tickCount;
        }

        if(tickCount >= 9999)
        {
            if(!level().isClientSide)
            {
                finish();
            }
        }

        lastTime = tickCount;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }

    public void setSpellCardNumber(int number)
    {
        this.entityData.set(SPELL_CARD_NUMBER, number);
    }

    /**
     * 符卡番号。
     *
     * <p>内部の「未同期」を表す -1 は 0 番として返すので、呼び出し側は常に 0 以上を受け取る
     * （レンダラや名前解決が負の添字で落ちないようにするため）。
     * 未同期かどうかを見たいときは {@link #isCardNumberSynced()} を使う。</p>
     */
    public int getSpellCardNumber()
    {
        int number = this.entityData.get(SPELL_CARD_NUMBER);
        return number < 0 ? 0 : number;
    }

    /** 宣言者の entity id。クライアントで未同期なら -1。 */
    public void setCasterId(int id)
    {
        this.entityData.set(CASTER_ID, id);
    }

    public int getCasterId()
    {
        return this.entityData.get(CASTER_ID);
    }
}
