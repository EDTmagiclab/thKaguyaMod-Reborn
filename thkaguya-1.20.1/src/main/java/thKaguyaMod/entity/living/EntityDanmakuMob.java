package thKaguyaMod.entity.living;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.entity.item.EntityTHItem;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 弾幕を使うMOBの定義クラス（1.20.1 移植版，抽象基类）
 *
 * 阶段 3a：裁剪版——保留飞行 / 索敌 / 弹幕框架 / 属性（3b），
 * 裁剪符卡魔法阵（EntitySpellCardCircle）、掉落物品（EntityTHItem）、符卡宣言（THKaguyaLib），
 * 这些依赖实体层 3d 迁移完成后补回。
 *
 * 1.7.10 → 1.20.1 关键映射：
 *   EntityCreature            -> Mob
 *   SharedMonsterAttributes   -> Attributes
 *   getEntityAttribute(...)   -> getAttribute(...)
 *   applyEntityAttributes()   -> createAttributes() 静态方法返回 AttributeSupplier.Builder
 *   worldObj                  -> level()
 *   posX/posY/posZ            -> getX()/getY()/getZ()
 *   motionX/Y/Z               -> setDeltaMovement()
 *   Vec3.createVectorHelper   -> new Vec3
 *   AxisAlignedBB             -> AABB
 */
public abstract class EntityDanmakuMob extends Mob
{
    /** 最后移动时间 */
    protected int lastTime;
    public int attackCounter = 0;
    public int attackInterval;
    public int courseChangeCooldown = 0;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    public int lostTarget;

    /** 指定移动方向 */
    protected Vec3 moveVec;
    /** 指定移动时间 */
    protected int moveTimer;
    protected int moveTimerMax;
    /** 停止中か */
    protected boolean stop;

    /** ターゲットとの間合い */
    protected double attackDistance;
    /** 近づくと敵対化する距離 */
    protected double detectionDistance;
    /** 飛行モードか */
    public boolean isFlyingMode;

    /** 妖精などの弾幕パターン */
    public int danmakuPattern;
    /** 発射頻度 */
    public int danmakuSpan;
    /** 弾の形状 */
    public byte shotForm;
    /** 弾の色 */
    public byte shotColor;
    /** 弾の速度 */
    public float speedRate;
    /** 特殊弾 */
    public short special;

    /** スペルカードモードか */
    public boolean isSpellCardMode;

    // 種族 ID
    protected int species_1;
    protected int species_2;

    // 人間に属す種族
    public static final int SPECIES_HUMAN = 0;
    public static final int SPECIES_HUMAN_HALF = 1;
    public static final int SPECIES_HUMAN_LUNARIAN = 2;
    public static final int SPECIES_HUMAN_HERMIT = 3;
    public static final int SPECIES_HUMAN_SHIKAISEN = 4;
    public static final int SPECIES_HUMAN_CELESTIALS = 5;
    public static final int SPECIES_HUMAN_SAINT = 6;
    public static final int SPECIES_HUMAN_INCHLINGS = 7;
    public static final int SPECIES_HUMAN_HOURAI = 8;

    // 神に属す種族
    public static final int SPECIES_GOD = 32;
    public static final int SPECIES_GOD_ARAHITOGAMI = 33;
    public static final int SPECIES_GOD_YATAGARASU = 34;

    // 閻魔
    public static final int SPECIES_ENMA = 48;

    // 妖精に属す種族
    public static final int SPECIES_FAIRY = 64;
    public static final int SPECIES_FAIRY_ICE = 65;

    // 幽霊に属す種族
    public static final int SPECIES_PHANTOM = 92;
    public static final int SPECIES_PHANTOM_HALF = 93;
    public static final int SPECIES_PHANTOM_GHOST = 94;
    public static final int SPECIES_PHANTOM_DIVINESPIRIT = 95;
    public static final int SPECIES_PHANTOM_SHIP = 96;
    public static final int SPECIES_PHANTOM_JIANGSHI = 97;

    // 式神に属す種族
    public static final int SPECIES_SHIKIGAMI = 108;
    public static final int SPECIES_SHIKIGAMI_DOLL = 109;
    public static final int SPECIES_SHIKIGAMI_FAMILIAR = 110;

    // 妖怪に属す種族
    public static final int SPECIES_YOUKAI = 128;
    public static final int SPECIES_YOUKAI_MAGICIAN = 129;
    public static final int SPECIES_YOUKAI_BEAST = 130;
    public static final int SPECIES_YOUKAI_VAMPIRE = 131;
    public static final int SPECIES_YOUKAI_ONI = 132;
    public static final int SPECIES_YOUKAI_KAPPA = 133;
    public static final int SPECIES_YOUKAI_TENGU_CROW = 134;
    public static final int SPECIES_YOUKAI_TENGU_WHITEWOLF = 135;
    public static final int SPECIES_YOUKAI_SHINIGAMI = 136;
    public static final int SPECIES_YOUKAI_YUKIONNA = 137;
    public static final int SPECIES_YOUKAI_BUG = 139;
    public static final int SPECIES_YOUKAI_YOSUZUME = 140;
    public static final int SPECIES_YOUKAI_RABBIT = 142;
    public static final int SPECIES_YOUKAI_TUKUMOGAMI = 143;
    public static final int SPECIES_YOUKAI_TURUBEOTOSHI = 144;
    public static final int SPECIES_YOUKAI_HASHIHIME = 145;
    public static final int SPECIES_YOUKAI_SATORI = 146;
    public static final int SPECIES_YOUKAI_NYUUDOU = 147;
    public static final int SPECIES_YOUKAI_NUE = 148;
    public static final int SPECIES_YOUKAI_MERMAID = 149;
    public static final int SPECIES_YOUKAI_AMANOJAKU = 150;

    // 龍神
    public static final int SPECIES_DRAGON = 192;

    // その他の種族
    public static final int SPECIES_OTHERS = 255;

    // 攻撃の段階
    /** 攻撃の意思がない状態を表す */
    public static final int NOT_ATTACK      = 0;
    public static final int NORMAL_ATTACK01 = 1;
    public static final int NORMAL_ATTACK02 = 2;
    public static final int NORMAL_ATTACK03 = 3;
    public static final int NORMAL_ATTACK04 = 4;
    public static final int NORMAL_ATTACK05 = 5;
    public static final int NORMAL_ATTACK06 = 6;
    public static final int NORMAL_ATTACK07 = 7;
    public static final int NORMAL_ATTACK08 = 8;
    public static final int NORMAL_ATTACK09 = 9;
    public static final int NORMAL_ATTACK10 = 10;
    public static final int NORMAL_ATTACK11 = 11;
    public static final int NORMAL_ATTACK12 = 12;
    public static final int NORMAL_ATTACK13 = 13;
    public static final int NORMAL_ATTACK14 = 14;
    public static final int NORMAL_ATTACK15 = 15;
    public static final int NORMAL_ATTACK16 = 16;
    public static final int NORMAL_ATTACK17 = 17;
    public static final int NORMAL_ATTACK18 = 18;
    public static final int NORMAL_ATTACK19 = 19;
    public static final int NORMAL_ATTACK20 = 20;
    public static final int SPELLCARD_ATTACK01 = 101;
    public static final int SPELLCARD_ATTACK02 = 102;
    public static final int SPELLCARD_ATTACK03 = 103;
    public static final int SPELLCARD_ATTACK04 = 104;
    public static final int SPELLCARD_ATTACK05 = 105;
    public static final int SPELLCARD_ATTACK06 = 106;
    public static final int SPELLCARD_ATTACK07 = 107;
    public static final int SPELLCARD_ATTACK08 = 108;
    public static final int SPELLCARD_ATTACK09 = 109;
    public static final int SPELLCARD_ATTACK10 = 110;
    public static final int SPELLCARD_ATTACK11 = 111;
    public static final int SPELLCARD_ATTACK12 = 112;
    public static final int SPELLCARD_ATTACK13 = 113;
    public static final int SPELLCARD_ATTACK14 = 114;
    public static final int SPELLCARD_ATTACK15 = 115;
    public static final int SPELLCARD_ATTACK16 = 116;
    public static final int SPELLCARD_ATTACK17 = 117;
    public static final int SPELLCARD_ATTACK18 = 118;
    public static final int SPELLCARD_ATTACK19 = 119;
    public static final int SPELLCARD_ATTACK20 = 120;
    public static final int SPELLCARD_ATTACK21 = 121;
    public static final int SPELLCARD_ATTACK22 = 122;
    public static final int SPELLCARD_ATTACK23 = 123;
    public static final int SPELLCARD_ATTACK24 = 124;
    public static final int SPELLCARD_ATTACK25 = 125;
    public static final int SPELLCARD_ATTACK26 = 126;
    /** 攻撃の終了を表す(倒れた) */
    public static final int ATTACK_END = 127;

    // 魔法陣の色
    public static final int CIRCLE_COLOR_RED = 0;
    public static final int CIRCLE_COLOR_BLUE = 1;
    public static final int CIRCLE_COLOR_GREEN = 2;
    public static final int CIRCLE_COLOR_YELLOW = 3;
    public static final int CIRCLE_COLOR_PURPLE = 4;
    public static final int CIRCLE_COLOR_AQUA = 5;
    public static final int CIRCLE_COLOR_ORANGE = 6;
    public static final int CIRCLE_COLOR_WHITE = 7;
    public static final int CIRCLE_COLOR_RAINBOW = 8;

    /** 魔法陣 */
    public EntitySpellCardCircle circle;

    /** 見た目（妖精の形態 ID，渲染阶段 4 用） */
    protected byte form;

    /** 英名 */
    protected String danmakuMobName = "";

    protected EntityDanmakuMob(EntityType<? extends EntityDanmakuMob> type, Level level)
    {
        super(type, level);
        isFlyingMode = true;
        this.stop = false;
        this.setSpecies(this.SPECIES_OTHERS);
    }

    /** 属性注册（3b：setHealth -> Attribute 的载体） */
    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    /** 最大HP を設定 */
    public void setMaxHP(float hp)
    {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
    }

    /** 移動速度を設定 */
    public void setSpeed(double speed)
    {
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }

    public double getDanmakuSpeed()
    {
        return this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
    }

    public void setAttackDistance(double distance)
    {
        attackDistance = distance;
    }

    public double getAttackDistance()
    {
        return attackDistance;
    }

    public void setDetectionDistance(double distance)
    {
        detectionDistance = distance;
    }

    protected double getDetectionDistance()
    {
        return detectionDistance;
    }

    /** 周りの妖精を敵対化させるか */
    /** 批次 C1：自然生成服从配置开关（Spawn Fairy / Phantom / Boss / Danmaku Mob 总开关） */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason)
    {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION)
        {
            // 用户实测反馈：原生生成会落到地下洞穴里很难找 → 只允许「头顶露天」的位置生成
            if (!level.canSeeSky(this.blockPosition()))
            {
                return false;
            }
            return thKaguyaMod.init.THKaguyaConfig.allowNaturalSpawn(this.getType());
        }
        return super.checkSpawnRules(level, reason);
    }

    /**
     * 用户需求：自然生成的妖怪自带永久发光，方便在野外找到。
     * （只对 NATURAL/CHUNK_GENERATION 生效——刷怪蛋、命令、繁殖出来的不带，避免污染手动测试）
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag)
    {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION)
                && this.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
        {
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false));
        }
        return result;
    }
    protected boolean canFairyCall()
    {
        return false;
    }

    protected double getFairyCallDistance()
    {
        return 10.0D;
    }

    public LivingEntity getTarget()
    {
        if(THKaguyaConfig.danmakuLevel <= 0)
        {
            return null;
        }
        return super.getTarget();
    }

    /** 位置ベクトル */
    public Vec3 pos(double x, double y, double z)
    {
        return THShotLib.pos(x, y, z);
    }

    public Vec3 pos()
    {
        return THShotLib.pos_Living(this);
    }

    public Vec3 pos(LivingEntity living)
    {
        return THShotLib.pos_Living(living);
    }

    public Vec3 angle(float yaw, float pitch)
    {
        return THShotLib.angle(yaw, pitch);
    }

    public Vec3 gravity_Zero()
    {
        return THShotLib.gravity_Zero();
    }

    public byte getFlyingHeight()
    {
        return (byte)(this.isFlyingMode ? 3 : 0);
    }

    public void setFlyingHeight(int height)
    {
        this.isFlyingMode = height > 0;
    }

    protected void setSpecies(int species1, int species2)
    {
        species_1 = species1;
        species_2 = species2;
    }

    protected void setSpecies(int species)
    {
        setSpecies(species, -1);
    }

    public int getSpecies_1()
    {
        return this.SPECIES_OTHERS;
    }

    public int getSpecies_2()
    {
        return this.SPECIES_OTHERS;
    }

    public boolean isHuman()
    {
        return getSpecies_1() < this.SPECIES_GOD || getSpecies_2() < this.SPECIES_GOD;
    }

    public boolean isPhantom()
    {
        return (getSpecies_1() >= this.SPECIES_PHANTOM && getSpecies_1() < this.SPECIES_SHIKIGAMI) ||
                (getSpecies_2() >= this.SPECIES_PHANTOM && getSpecies_2() < this.SPECIES_SHIKIGAMI);
    }

    public boolean isFairy()
    {
        return getSpecies_1() == this.SPECIES_FAIRY || getSpecies_2() == this.SPECIES_FAIRY;
    }

    public boolean isYoukai()
    {
        return (getSpecies_1() >= this.SPECIES_YOUKAI && getSpecies_1() < this.SPECIES_DRAGON) ||
                (getSpecies_2() >= this.SPECIES_YOUKAI && getSpecies_2() < this.SPECIES_DRAGON);
    }

    public boolean isInSpecies(int species)
    {
        return getSpecies_1() == species || getSpecies_2() == species;
    }

    /** 名前（英名）を返す */
    public String getDanmakuMobName()
    {
        return danmakuMobName;
    }

    /** 名前（英名）を設定する */
    public void setDanmakuMobName(String name)
    {
        danmakuMobName = name;
    }

    /** 経験値（1.20.1 Mob.xpReward 为 private，用自有字段 + getExperienceReward 覆盖） */
    protected int danmakuXpReward = 0;

    @Override
    public int getExperienceReward()
    {
        return danmakuXpReward;
    }

    /** 見た目（妖精の形態 ID）を返す */
    public byte getForm()
    {
        return form;
    }

    /** 見た目（妖精の形態 ID）を設定する */
    public void setForm(byte type)
    {
        form = type;
    }

    /** 使用中のスペルカードNoを返す（子类实现） */
    public int getUsingSpellCardNo()
    {
        return -1;
    }

    /** スペルカードを使用中かどうか */
    public boolean isSpellCardAttack()
    {
        return getUsingSpellCardNo() >= 0;
    }

    /** 魔法陣の色 */
    public int getSpellCardCircleColor()
    {
        return CIRCLE_COLOR_RED;
    }

    /** スペルカードを宣言する */
    protected void useSpellCard(int spellCardNo)
    {
        LivingEntity target = this.getTarget();
        if(target != null)
        {
            THKaguyaLib.spellCardDeclaration(this.level(), this, target, spellCardNo, 0, THKaguyaConfig.danmakuLevel, !isSpellCardMode);
        }
        if(!isSpellCardMode)
        {
            isSpellCardMode = true;
            // スペルカードの魔法陣を出現させる
            circle = new EntitySpellCardCircle(this.level(), this, 16 + getSpellCardCircleColor(), -100);
            if(!this.level().isClientSide)
            {
                this.level().addFreshEntity(circle);
            }
        }
    }

    /**
     * 死に際して呼ばれる処理（阶段转换拦截）。
     * @return true = 进入下一攻击阶段并复活（不真正死亡）；false = 真正死亡
     */
    protected boolean onDeathUpdate()
    {
        return false;
    }

    /**
     * 死亡拦截：boss 在 HP 归零时先调用 onDeathUpdate() 判断是否进入下一阶段，
     * 若复活则跳过 super.die()（1.7.10 的 onDeathUpdate 每 tick 阶段转换等价物）。
     */
    @Override
    public void die(DamageSource source)
    {
        if(!this.level().isClientSide && this.onDeathUpdate())
        {
            return;
        }
        super.die(source);
    }

    // ------------------------------------------------------------------
    // ボスバー（1.20.1 ではバニラのボスバーを使う）
    //
    // 原版 1.7.10 は RenderTHBoss が頭上に status.png の体力バーを描画していたが、
    // 1.20.1 ではクライアント描画の都合（視野角 20°制限・カメラ向き・拡大率）で
    // 見た目が崩れやすいため、エンダードラゴンと同じ ServerBossEvent に置き換えた。
    // バーを出すのは hasBossBar() が true のボス系のみ（妖精・使い魔は出さない）。
    // ------------------------------------------------------------------

    /** ボスバー（サーバー側のみ実体を持つ。クライアントでは null のまま） */
    @Nullable
    private ServerBossEvent bossBar;

    /** このモブがボスバーを出すか。ボス系の子クラスだけ true を返す */
    public boolean hasBossBar()
    {
        return false;
    }

    /** ボスバーを遅延生成して返す */
    protected ServerBossEvent bossBar()
    {
        if(bossBar == null)
        {
            bossBar = new ServerBossEvent(getDisplayName(),
                    BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
        }
        return bossBar;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player)
    {
        super.startSeenByPlayer(player);
        if(hasBossBar())
        {
            bossBar().addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player)
    {
        super.stopSeenByPlayer(player);
        if(bossBar != null)
        {
            bossBar.removePlayer(player);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason)
    {
        if(!level().isClientSide && bossBar != null)
        {
            bossBar.removeAllPlayers();
        }
        super.remove(reason);
    }

    /** ボスバーの体力と表示状態を毎 tick 同期する */
    protected void tickBossBar()
    {
        if(level().isClientSide || !hasBossBar())
        {
            return;
        }

        ServerBossEvent bar = bossBar();
        // 攻撃中（弾幕パターンが NOT_ATTACK 以外）だけ出す。原版の頭上バーと同じ条件
        boolean show = this.isAlive() && getDanmakuPattern() != NOT_ATTACK;
        if(bar.isVisible() != show)
        {
            bar.setVisible(show);
        }
        bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
    }

    protected void setStopStart()
    {
        stop = true;
        moveTimer = 0;
        moveTimerMax = 0;
    }

    protected void setStopEnd()
    {
        stop = false;
    }

    public boolean isStop()
    {
        return stop;
    }

    /** ベクトル指定移動 */
    protected void move(Vec3 move, double speed, int time)
    {
        moveVec = move.scale(speed);
        moveTimer = time;
        moveTimerMax = time;
    }

    protected void moveRight(double speed, int time)
    {
        moveVec = THShotLib.getVecFromAngle(getYRot() + 90F, getXRot());
        move(moveVec, speed, time);
    }

    protected void moveLeft(double speed, int time)
    {
        moveVec = THShotLib.getVecFromAngle(getYRot() - 90F, getXRot());
        move(moveVec, speed, time);
    }

    protected void moveForward(double speed, int time)
    {
        moveVec = THShotLib.getVecFromAngle(getYRot(), getXRot());
        move(moveVec, speed, time);
    }

    protected void moveBack(double speed, int time)
    {
        moveVec = THShotLib.getVecFromAngle(getYRot() + 180, getXRot());
        move(moveVec, speed, time);
    }

    /** 攻撃を次の段階に移す */
    protected void moveDanmakuAttack(int nextPattern, int interval, float maxHP, int invincibleTime)
    {
        setDanmakuPattern((byte)nextPattern);
        attackCounter = -interval;
        this.deathTime = 0;
        if(!level().isClientSide)
        {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);
            this.setHealth(maxHP);
        }
        attackInterval = invincibleTime;
        // 原版：阶段切换时消掉周围 40 格的「敌方以外」弹幕（自己的旧弹幕清空，避免叠屏）
        THShotLib.danmakuRemove(this, 40.0D, "Enemy", true);
        isSpellCardMode = false;
    }

    public Vec3 look()
    {
        return THShotLib.angle(this.getYRot(), this.getXRot());
    }

    /** 毎 tick 処理 */
    @Override
    public void tick()
    {
        // ピースフルなら消滅させる
        if(!this.level().isClientSide && this.level().getDifficulty().getId() == 0)
        {
            this.discard();
            return;
        }

        // 弾幕レベルが０なら、攻撃の対象を持たない
        if(THKaguyaConfig.danmakuLevel <= 0)
        {
            this.setTarget(null);
        }

        // 时停检查：原版 1.7.10 依赖「外部框架先 ticksExisted++，再进 onUpdate」，
        // 而 1.20.1 的 tickCount 在 super.tick() 内递增，若沿用 <= 判断会第一次 tick 就永久冻结。
        // 时停系统（Sakuya 技能）尚未移植，lastTime 恒为 0，故此处直接正常 tick。
        super.tick();

        tickBossBar();

        if(this.getHealth() <= 0.0F)
        {
            return;
        }

        // 妖精系のみ発射間隔で攻撃カウンターをリセット（boss は danmakuSpan=0 で自前制御）
        if(this.danmakuSpan > 0 && this.attackCounter > this.danmakuSpan)
        {
            this.attackCounter = 0;
        }

        if(attackInterval > 0)
        {
            attackInterval--;
        }

        if(moveTimer > 0)
        {
            double moveRate = Math.cos((double)moveTimer / (double)moveTimerMax * Math.PI / 2.0D);
            if(!level().isClientSide)
            {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.add(moveVec.x * moveRate, moveVec.y * moveRate, moveVec.z * moveRate));
            }
            moveTimer--;
        }

        // ターゲットがいないなら索敵
        if(this.getTarget() == null)
        {
            Player player = this.level().getNearestPlayer(this, getDetectionDistance());
            if(player != null)
            {
                this.setTarget(player);
            }
        }

        LivingEntity target = this.getTarget();
        if(target != null && !target.isAlive())
        {
            this.setTarget(null);
        }

        if(target != null)
        {
            // ターゲットの方を向く
            double xDistance = target.getX() - getX();
            double yDistance = (target.getY() + target.getEyeHeight()) - (getY() + this.getEyeHeight());
            double zDistance = target.getZ() - getZ();
            float angleXZ = -((float)Math.atan2(xDistance, zDistance)) / 3.141593F * 180F;
            float angleY = -(float)Math.atan2(yDistance, Math.sqrt(xDistance * xDistance + zDistance * zDistance)) / 3.141593F * 180F;

            if(!stop)
            {
                this.setYRot(angleXZ);
                this.setXRot(angleY);
            }

            if(this.hasLineOfSight(target))
            {
                lostTarget = 0;
                int level = THKaguyaConfig.danmakuLevel;
                if(level != 0 && attackCounter >= 0)
                {
                    danmakuPattern(level);
                }
                attackCounter++;
            }
            else
            {
                lostTarget++;
                if(lostTarget > 200)
                {
                    this.setTarget(null);
                }
            }
        }

        // 移动逻辑（原版：迷失目标时随机移动，有目标时保持攻击距离）
        if(moveTimer <= 0 && lostTarget > 0)
        {
            move(THShotLib.getVecFromAngle(this.random.nextFloat() * 360F, this.random.nextFloat() * 360F),
                    getDanmakuSpeed(), 30);
        }

        if(moveTimer <= 0 && target != null && getAttackDistance() > 0)
        {
            double toTargetDistance = this.distanceTo(target);
            double rate;
            if(toTargetDistance < getAttackDistance())
            {
                rate = 1.0D - (toTargetDistance / getAttackDistance());
                moveBack(getDanmakuSpeed() * 0.3D * rate, 15);
            }
            else
            {
                rate = toTargetDistance / getAttackDistance();
                moveForward(getDanmakuSpeed() * 0.5D * rate, 15);
            }
        }

        if(tickCount > lastTime)
        {
            lastTime = tickCount;
        }
    }

    /** 無敵状態（攻击阶段切换的间隔时间 + NOT_ATTACK 状态均无敌） */
    @Override
    public boolean isInvulnerableTo(DamageSource source)
    {
        if(attackInterval > 0)
        {
            return true;
        }
        if(getDanmakuPattern() == NOT_ATTACK)
        {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if(super.hurt(source, amount))
        {
            this.setDeltaMovement(Vec3.ZERO);
            if(source.getEntity() instanceof Player)
            {
                this.setTarget((LivingEntity)source.getEntity());
            }
            return true;
        }
        return false;
    }

    /** 弾幕パターン（子类实现） */
    protected void danmakuPattern(int level)
    {
    }

    public int getDanmakuPattern()
    {
        return this.danmakuPattern;
    }

    public void setDanmakuPattern(int pattern)
    {
        this.danmakuPattern = pattern;
    }

    /** 飞行怪不掉落伤害（1.20.1 无 checkFallDamage，重写 causeFallDamage） */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source)
    {
        return false;
    }

    /* ==================== 掉落（阶段 3d-6 简化版） ==================== */
    /**
     * 掉落弹幕/威力/得点道具的底层实现。
     * 原版依赖 EntityTHItem（弹幕拾取实体），该实体尚未迁移（属 item 相关实体），
     * 阶段 4/7 迁移后回填；当前为 no-op 占位。
     */
    protected void dropItem(int type, Vec3 pos, Vec3 angle)
    {
        if (this.level().isClientSide)
        {
            return;
        }
        EntityTHItem item = new EntityTHItem(THKaguyaEntities.TH_ITEM.get(), this.level(),
                pos.x, pos.y, pos.z, angle, (byte)type);
        this.level().addFreshEntity(item);
    }

    public void dropPowerUpItem(Vec3 pos, Vec3 angle)
    {
        dropItem(1, pos, angle);
    }

    public void dropPowerUpItem(int num)
    {
        for (int i = 1; i <= num; i++)
        {
            dropPowerUpItem(this.position(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
        }
    }

    public void dropPointItem(Vec3 pos, Vec3 angle)
    {
        dropItem(4, pos, angle);
    }

    public void dropPointItem(int num)
    {
        for (int i = 1; i <= num; i++)
        {
            dropPointItem(this.position(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
        }
    }

    public void dropSpellCardItem(Vec3 pos, Vec3 angle)
    {
        dropItem(10, pos, angle);
    }

    public void dropSpellCardItem(int num)
    {
        for (int i = 1; i <= num; i++)
        {
            dropSpellCardItem(this.position(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
        }
    }

    public void dropExtendItem(Vec3 pos, Vec3 angle)
    {
        dropItem(11, pos, angle);
    }

    public void dropExtendItem(int num)
    {
        for (int i = 1; i <= num; i++)
        {
            dropExtendItem(this.position(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
        }
    }

    /**
     * 倒れたときに落とすアイテム（1.7.10 dropFewItems 等价物）。
     * 原版 EntityDanmakuMob.java:1390-1397:
     *   if(getUsingSpellCardNo() >= 0 && hasBeenAttackedByPlayer)
     *       entityDropItem(new ItemStack(THKaguyaItems.spell_card, 1, getUsingSpellCardNo()), 0.0F);
     * 即「正在放的这张符卡」，符卡编号就写在 damage 值里（ItemTHSpellCard 用 damage 决定名字）。
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int spellCardNo = this.getUsingSpellCardNo();
        if (spellCardNo >= 0 && recentlyHit)
        {
            // damage = 符卡 No。漏掉这一步会让所有 boss 一律掉 damage 0（灵符「梦想封印」）
            ItemStack card = new ItemStack(THKaguyaItems.SPELL_CARD.get(), 1);
            card.setDamageValue(spellCardNo);
            this.spawnAtLocation(card);
        }
    }
}
