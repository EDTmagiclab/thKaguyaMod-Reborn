package thKaguyaMod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.entity.item.EntitySakuyaStopWatch;
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.entity.living.EntityDanmakuMob;
import thKaguyaMod.entity.shot.EntityTHLaser;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 時間停止／時間減速の状態機械（1.20.1 移植版で新規追加）。
 *
 * <h2>なぜこのクラスが要るのか</h2>
 * 原版 1.7.10 は {@code EntitySakuyaWatch#onUpdate} の中で、毎 tick 周囲の Entity の座標を
 * 1 tick 前へ巻き戻す（{@code setPos(xo,yo,zo)}）ことで「止まって見える」状態を作っていた。
 * 1.20.1 にはそれに相当する安全な手段が無く（{@code TickRateManager} は 1.20.3 以降）、
 * また座標巻き戻しはプレイヤーの移動まで奪うためマルチでは使えない。
 *
 * <p>そこで 1.20.3 の {@code /tick freeze} と Carpet MOD と同じ思路、すなわち
 * <b>tick の入口で gate する</b>方式に置き換える。判定は本クラスの {@link #isFrozen(Entity)} 1 箇所に集約し、</p>
 * <ul>
 *   <li>バニラ生物 → Forge の {@code LivingEvent.LivingTickEvent} をキャンセル</li>
 *   <li>自前 Entity（弾／レーザー／符卡／弾幕Mob）→ 各 {@code tick()} の先頭で早期 return</li>
 * </ul>
 * <p>の二段構えで止める。</p>
 *
 * <h2>サーバとクライアントで状態を分ける理由</h2>
 * 統合サーバ（シングルプレイ）ではクライアントとサーバが同一 JVM で動くため、
 * 静的フィールドを 1 組だけ持つとクライアント側の更新がサーバ側の状態を壊す。
 * よって {@link #SERVER} と {@link #CLIENT} の 2 組に分け、
 * {@code entity.level().isClientSide} で引き当てる。
 * 両者はそれぞれ自分のスレッドからしか触られない。
 *
 * <h2>六条の安全弁</h2>
 * <ol>
 *   <li>看門狗：{@link #HARD_LIMIT_DEFAULT} tick を超えたら無条件に解除</li>
 *   <li>全サーバ単例：{@link #start} は既に active なら false を返し、上書きも延長もしない</li>
 *   <li>既定半径はコンフィグで絞る（既定 32 格。原版は 240 格 = 実質全サーバ）</li>
 *   <li>次元／チャンク境界：開始時の {@link Level} 以外では一切効かせない。時計 Entity が消えたら即解除</li>
 *   <li>両端判定同源：クライアントは同期値（モード／術者 id／半径／フラグ）だけを見る</li>
 *   <li>既定オフ：{@code enableTimeStop} が false なら一切凍らせない</li>
 * </ol>
 */
public final class TimeStopManager
{
    /** 看門狗の既定上限（30 秒） */
    public static final int HARD_LIMIT_DEFAULT = 600;

    /**
     * {@link EntitySakuyaWatch#TIME_HALF} と同値。
     * 静的初期化の循環（Manager → Watch → Manager）を避けるため数値で持つ。
     */
    private static final int DEFAULT_MODE = 2;

    /** 停止空間が 1 tick に広がる半径（原版 {@code ticksExisted * 12}） */
    private static final double RADIUS_GROWTH = 12.0D;

    /** {@link State#flags} bit0：この発生源が実際に時間操作を駆動している */
    public static final int FLAG_ACTIVE = 1;
    /** {@link State#flags} bit1：半径内を敵味方問わず凍らせる */
    public static final int FLAG_FREEZE_ALL = 1 << 1;

    private static final State SERVER = new State();
    private static final State CLIENT = new State();

    /**
     * 凍結したプレイヤーのアンカー（サーバ専用）。
     *
     * <p>プレイヤーの移動はパケット由来なので、{@code tick} を止めても歩けてしまう。
     * そこで「凍結が始まった瞬間の座標」を覚えておき、毎 tick {@code connection.teleport} で
     * そこへ引き戻す（Bukkit 系プラグインの "freeze" と同じ手法）。</p>
     */
    private static final java.util.Map<java.util.UUID, Vec3> PLAYER_ANCHORS = new java.util.HashMap<>();

    private TimeStopManager()
    {
    }

    /**
     * 片側（サーバ or クライアント）ぶんの状態。
     * フィールドを 1 クラスにまとめておくと、判定ロジックを 1 本で書ける。
     */
    private static final class State
    {
        boolean active;
        Level level;
        /** 停止空間の中心（時計 Entity の現在位置。毎 tick 更新） */
        Vec3 center = Vec3.ZERO;
        double radius;
        int mode = DEFAULT_MODE;
        int ownerId = -1;
        int maxRadius = 32;
        boolean freezeAll;
        /** true = 術者以外のプレイヤーの位置も固定する（符卡駆動のときだけ） */
        boolean freezePlayers;

        /* --- サーバ専用 --- */
        ServerLevel serverLevel;
        LivingEntity owner;
        Entity watch;
        long startGameTime;
        int hardLimit = HARD_LIMIT_DEFAULT;
        /** 停止前に保存した {@code randomTickSpeed}。-1 = 未保存 */
        int savedRandomTick = -1;
    }

    /* ==================================================================== */
    /* サーバ側 API                                                          */
    /* ==================================================================== */

    /**
     * 時間操作を開始する（サーバ専用）。
     *
     * @return true = 開始した。false = すでに別の時間操作が動いている／引数不正
     */
    public static boolean start(ServerLevel level, LivingEntity caster, Entity watch, int mode)
    {
        return start(level, caster, watch, mode, false);
    }

    /**
     * @param freezePlayers true = 術者以外のプレイヤーの位置も固定する（符卡駆動専用）
     */
    public static boolean start(ServerLevel level, LivingEntity caster, Entity watch, int mode, boolean freezePlayers)
    {
        if(level == null || watch == null || watch.isRemoved())
        {
            return false;
        }
        // 全サーバ単例：重ね掛けも上書きもしない（時間の長さが無限に伸びるのを防ぐ）
        if(SERVER.active)
        {
            return false;
        }

        SERVER.active = true;
        SERVER.serverLevel = level;
        SERVER.level = level;
        SERVER.owner = caster;
        SERVER.ownerId = caster == null ? -1 : caster.getId();
        SERVER.watch = watch;
        SERVER.mode = mode;
        SERVER.center = watch.position();
        SERVER.maxRadius = clampRadius(THKaguyaConfig.timeStopRadius);
        SERVER.radius = radiusForAge(0, SERVER.maxRadius);
        SERVER.freezeAll = !THKaguyaConfig.timeStopHostileOnly;
        SERVER.freezePlayers = freezePlayers;
        SERVER.startGameTime = level.getGameTime();
        SERVER.hardLimit = Math.max(20, THKaguyaConfig.timeStopHardLimitTicks);
        PLAYER_ANCHORS.clear();

        // 時間停止のあいだは作物・炎・葉の自然崩壊も止める（1.20.1 にはイベントが無いので GameRule を退避して 0 にする）
        if(isTimeStopMode(mode))
        {
            freezeRandomTicks(level);
        }
        return true;
    }

    /** サーバ tick の終わりに呼ぶ：看門狗・失効判定・中心と半径の更新 */
    public static void serverTick()
    {
        if(!SERVER.active)
        {
            return;
        }
        ServerLevel level = SERVER.serverLevel;
        Entity watch = SERVER.watch;
        if(level == null || watch == null || watch.isRemoved() || !watch.isAlive())
        {
            // 時計が /kill された・チャンクが降りた・自前の寿命で消えた → 即解除
            stop();
            return;
        }
        if(watch.level() != level)
        {
            stop();
            return;
        }
        LivingEntity owner = SERVER.owner;
        if(owner != null && (!owner.isAlive() || owner.level() != level))
        {
            stop();
            return;
        }

        long age = level.getGameTime() - SERVER.startGameTime;
        if(age > SERVER.hardLimit)
        {
            // 看門狗：時計がどうなっていようと必ず解除する（ここが無いと凍ったまま再起不能になる）
            ServerLevel stoppedLevel = level;
            LivingEntity stoppedOwner = owner;
            stopAndDiscardWatch(true);
            notifyForcedEnd(stoppedLevel, stoppedOwner);
            return;
        }

        SERVER.center = watch.position();
        SERVER.radius = radiusForAge((int)age, SERVER.maxRadius);

        if(SERVER.freezePlayers)
        {
            lockPlayers(level);
        }
    }

    /**
     * 術者以外のプレイヤーをアンカーへ引き戻す（サーバ専用）。
     *
     * <p>プレイヤーの位置はクライアントの移動パケットで決まるので、{@code tick()} を止めても
     * 歩けてしまう。よって「最初に凍った瞬間の座標」を覚えておき、毎 tick teleport で戻す。
     * {@code awaitingPositionFromClient} はクライアントの位置確認で自然に解除されるため、
     * 連続 teleport でも「moved too quickly」判定には掛からない。</p>
     */
    private static void lockPlayers(ServerLevel level)
    {
        for(ServerPlayer player : level.players())
        {
            // FakePlayer（ディスペンサー等が作る偽プレイヤー）は connection を持たないことがある
            if(player.connection == null)
            {
                continue;
            }
            java.util.UUID id = player.getUUID();
            if(!isAffected(SERVER, player))
            {
                // 範囲外へ出た／乗り物に乗った等：アンカーを捨てて自由にする
                PLAYER_ANCHORS.remove(id);
                continue;
            }
            Vec3 anchor = PLAYER_ANCHORS.get(id);
            if(anchor == null)
            {
                anchor = player.position();
                PLAYER_ANCHORS.put(id, anchor);
            }
            if(player.position().distanceToSqr(anchor) > 1.0E-6D)
            {
                player.setDeltaMovement(Vec3.ZERO);
                player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
                player.hurtMarked = true;
            }
        }
    }

    /** 時間操作を終了する。サーバ・クライアントどちらの状態でも安全に呼べる */
    public static void stop()
    {
        clear(SERVER, true);
    }

    /**
     * 時間操作を終了し、発生源の時計 Entity も片付ける。
     *
     * <p>看門狗による強制解除では絶対に必要。{@code TIME_STOP}（創造モードの無制限モード）は
     * 時計自身が寿命を持たないので、ここで消さないと「効果は切れたのに時計だけ漂い続ける」ことになる。</p>
     *
     * @param returnToOwner true = 持ち主のインベントリへ戻す／false = その場に落とす
     */
    public static void stopAndDiscardWatch(boolean returnToOwner)
    {
        Entity watch = SERVER.watch;
        LivingEntity owner = SERVER.owner;
        stop();
        if(watch == null || watch.isRemoved())
        {
            return;
        }
        if(watch instanceof EntitySakuyaWatch sakuyaWatch)
        {
            THKaguyaLib.itemEffectFinish(sakuyaWatch, returnToOwner ? owner : null, THKaguyaItems.SAKUYA_WATCH.get());
        }
        else
        {
            watch.discard();
        }
    }

    /** サーバ側が時間操作中か */
    public static boolean isActive()
    {
        return SERVER.active;
    }

    /** サーバ側の現在モード（{@link EntitySakuyaWatch} の定数） */
    public static int getMode()
    {
        return SERVER.mode;
    }

    /** サーバ側の術者。開始していなければ null */
    public static LivingEntity getOwner()
    {
        return SERVER.owner;
    }

    /** サーバ側の現在半径 */
    public static double getRadius()
    {
        return SERVER.radius;
    }

    /* ==================================================================== */
    /* クライアント側 API                                                    */
    /* ==================================================================== */

    /**
     * クライアントのミラーを同期値で更新する（クライアント tick から毎 tick 呼ぶ）。
     * サーバが送ってきた値しか使わないので、両端の判定が食い違わない。
     */
    public static void clientUpdate(Level level, int mode, int ownerId, Vec3 center, int ageTicks,
                                    int maxRadius, boolean freezeAll)
    {
        CLIENT.active = true;
        CLIENT.level = level;
        CLIENT.mode = mode;
        CLIENT.ownerId = ownerId;
        CLIENT.center = center;
        CLIENT.maxRadius = clampRadius(maxRadius);
        CLIENT.radius = radiusForAge(ageTicks, CLIENT.maxRadius);
        CLIENT.freezeAll = freezeAll;
    }

    /** クライアント側に時間操作の発生源が無くなった */
    public static void clientClear()
    {
        if(CLIENT.active)
        {
            clear(CLIENT, false);
        }
    }

    /** クライアント側が時間操作中か */
    public static boolean isClientActive()
    {
        return CLIENT.active;
    }

    /**
     * カメラ位置が停止空間の内側にあるか（クライアント）。
     * 空の反転演出は「中にいる人にだけ」見せる必要があるので、この判定を演出側から使う。
     */
    public static boolean isClientInside(net.minecraft.world.phys.Vec3 pos)
    {
        if(!CLIENT.active)
        {
            return false;
        }
        double radius = CLIENT.radius;
        return pos.distanceToSqr(CLIENT.center.x, CLIENT.center.y, CLIENT.center.z) <= radius * radius;
    }

    /* ==================================================================== */
    /* 判定                                                                  */
    /* ==================================================================== */

    /**
     * この Entity はこの tick 動いてはいけないか。
     *
     * <p>両端で同じ結果になるよう、同期済みの情報だけで判定する。
     * 未起動時は最初の 1 行で抜けるので、常時の負荷はほぼゼロ。</p>
     */
    public static boolean isFrozen(Entity entity)
    {
        State state = entity.level().isClientSide ? CLIENT : SERVER;
        if(!state.active)
        {
            return false;
        }
        if(!isAffected(state, entity))
        {
            return false;
        }
        // 時間停止系：完全に止める
        if(isTimeStopMode(state.mode))
        {
            return true;
        }
        // 時間減速：2 tick に 1 回だけ動かす。
        // 位相に entity.tickCount を使うと「凍っているあいだ増えない」ので位相が固定されてしまう。
        // 世界時計 + 固有 id で散らす（両端で同じ値になる）。
        return ((entity.level().getGameTime() + entity.getId()) & 1L) != 0L;
    }

    /**
     * tick の入口で呼ぶ。true = この tick を丸ごと止める。
     *
     * <p>{@link #isFrozen} と違い<b>副作用がある</b>：凍結中でも受傷関係のタイマーだけは 1 進める。</p>
     *
     * <p>理由：{@code hurtTime} と {@code invulnerableTime} の減算は {@code LivingEntity#aiStep()}
     * （＝{@code tick()} の中）にある。tick を丸ごと止めるとこれらの値が固定され、
     * 一度殴った個体は {@code invulnerableTime = 20} のまま固まって
     * <b>二発目以降が全部「まだ無敵」で弾かれる</b>（時停中に敵が実質無敵になる）。
     * 原版 1.7.10 は位置だけ巻き戻していたのでこの問題は無かった。
     * ここで手動で 1 ずつ減らすことで「止まっているのに殴れば減る」という原作の手触りを保つ。</p>
     */
    public static boolean blockTick(Entity entity)
    {
        if(!isFrozen(entity))
        {
            return false;
        }
        if(entity instanceof LivingEntity living)
        {
            if(living.hurtTime > 0)
            {
                --living.hurtTime;
            }
            if(living.invulnerableTime > 0)
            {
                --living.invulnerableTime;
            }
        }
        return true;
    }

    /** 半径・除外・敵味方の判定（モード非依存） */
    private static boolean isAffected(State state, Entity entity)
    {
        if(entity.isRemoved())
        {
            return false;
        }
        // 開始した次元でしか効かせない（ネザー／エンドに飛んだら無関係）
        if(entity.level() != state.level)
        {
            return false;
        }
        // 術者本人（プレイヤー判定より先に置く。術者は他のプレイヤー凍結モードでも動ける）
        if(entity.getId() == state.ownerId)
        {
            return false;
        }
        if(state.owner != null && entity == state.owner)
        {
            return false;
        }
        // 時計自身。凍らせると tickCount が伸びず、制限時間が永遠に来なくなる
        if(entity instanceof EntitySakuyaWatch || entity instanceof EntitySakuyaStopWatch)
        {
            return false;
        }

        /* ---- 距離（球殻。原版・演出と同じ式）---- */
        double radius = state.radius;
        if(entity.distanceToSqr(state.center.x, state.center.y, state.center.z) > radius * radius)
        {
            return false;
        }

        /* ---- プレイヤー ----
         * 既定（アイテム駆動）では凍らせない。原版 1.7.10 は他人の座標を毎 tick
         * 巻き戻していたが、実際には xo が既に更新済みで効いておらず、効かせようとすると
         * teleport 連打でしか実現できない。よって「符卡駆動のときだけ、明示的に有効化する」。
         * サーバ側だけ true（クライアントでプレイヤーの tick を止めると入力とカメラが壊れる）。
         * 位置の固定は {@link #lockPlayers} が teleport で行う。
         */
        if(entity instanceof Player)
        {
            if(!state.freezePlayers || entity.level().isClientSide)
            {
                return false;
            }
            // 載り物の中にいる人は固定できない（乗り物側ごと固めることになり、降りられなくなる）
            return !entity.isPassenger() && entity.getPassengers().isEmpty();
        }

        // 騎乗チェーン（載っている側・載せている側）は丸ごと除外。馬や船ごと固めると降りられなくなる
        if(entity.isPassenger() || !entity.getPassengers().isEmpty())
        {
            return false;
        }

        /* ---- 符卡の例外（原版が残したフィールドをそのまま使う）---- */
        if(entity instanceof EntitySpellCard card && card.isTimeStopExempt())
        {
            return false;
        }

        /* ---- ここから先は「凍らせてよい対象か」---- */
        if(state.freezeAll)
        {
            return true;
        }

        // プレイヤーの資産は凍らせない（他人の農場・取引・ペットを止めないための線引き）
        if(entity instanceof TamableAnimal tame && tame.isTame())
        {
            return false;
        }
        if(entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null)
        {
            return false;
        }
        if(entity.hasCustomName())
        {
            return false;
        }
        if(entity instanceof AbstractVillager)
        {
            return false;
        }
        if(entity instanceof IronGolem || entity instanceof SnowGolem)
        {
            return false;
        }

        // 弾幕・レーザーは全凍結（これが時停の主目的）
        if(entity instanceof EntityTHShot || entity instanceof EntityTHLaser)
        {
            return true;
        }
        // 弾幕Mob は Monster を継承していないので個別に判定する
        if(entity instanceof EntityDanmakuMob)
        {
            return true;
        }
        // 敵対 Mob：術者を狙っているか、無主の敵対職
        if(entity instanceof Mob mob)
        {
            if(mob.getTarget() != null && mob.getTarget().getId() == state.ownerId)
            {
                return true;
            }
            return mob instanceof Monster;
        }
        // 敵の符卡（上の例外に当たらなかったもの）は止める
        if(entity instanceof EntitySpellCard)
        {
            return true;
        }
        // それ以外（落ちている物・矢・TNT・トロッコ等）は触らない：誤爆より取りこぼしを選ぶ
        return false;
    }

    /** 時間停止系モードか（減速系は false） */
    public static boolean isTimeStopMode(int mode)
    {
        return mode == EntitySakuyaWatch.TIME_STOP
                || mode == EntitySakuyaWatch.TIME_STOP_IN_SPELLCARD
                || mode == EntitySakuyaWatch.TIME_STOP_WITH_LIMIT;
    }

    /** 原版と同じ「1 tick に 12 格広がり、上限で止まる」半径 */
    public static double radiusForAge(int ageTicks, int maxRadius)
    {
        if(ageTicks <= 0)
        {
            return 0.0D;
        }
        return Math.min(ageTicks * RADIUS_GROWTH, (double)maxRadius);
    }

    private static int clampRadius(int radius)
    {
        return Math.max(8, Math.min(240, radius));
    }

    /* ==================================================================== */
    /* 内部                                                                  */
    /* ==================================================================== */

    private static void clear(State state, boolean isServer)
    {
        if(isServer && state.savedRandomTick >= 0 && state.serverLevel != null)
        {
            restoreRandomTicks(state.serverLevel, state.savedRandomTick);
        }
        state.active = false;
        state.level = null;
        state.center = Vec3.ZERO;
        state.radius = 0.0D;
        state.mode = DEFAULT_MODE;
        state.ownerId = -1;
        state.freezeAll = false;
        state.freezePlayers = false;
        state.serverLevel = null;
        state.owner = null;
        state.watch = null;
        state.startGameTime = 0L;
        state.savedRandomTick = -1;
        if(isServer)
        {
            PLAYER_ANCHORS.clear();
        }
    }

    private static void freezeRandomTicks(ServerLevel level)
    {
        if(SERVER.savedRandomTick >= 0)
        {
            return;
        }
        GameRules rules = level.getGameRules();
        int original = rules.getInt(GameRules.RULE_RANDOMTICKING);
        SERVER.savedRandomTick = original;
        if(original != 0)
        {
            rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        }
    }

    private static void restoreRandomTicks(ServerLevel level, int original)
    {
        GameRules rules = level.getGameRules();
        if(rules.getInt(GameRules.RULE_RANDOMTICKING) != original)
        {
            rules.getRule(GameRules.RULE_RANDOMTICKING).set(original, level.getServer());
        }
    }

    /** サーバ停止時に GameRule を必ず元へ戻す（0 のまま level.dat に保存されるのを防ぐ） */
    public static void onServerStopping()
    {
        if(SERVER.active)
        {
            stop();
        }
    }

    private static void notifyForcedEnd(ServerLevel level, LivingEntity owner)
    {
        if(owner instanceof net.minecraft.server.level.ServerPlayer player && player.level() == level)
        {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("thkaguya.timestop.force_end"), true);
        }
    }
}
