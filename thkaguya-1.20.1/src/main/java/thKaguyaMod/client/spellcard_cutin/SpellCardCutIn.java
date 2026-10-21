package thKaguyaMod.client.spellcard_cutin;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 符卡宣言演出（カットイン）の状態管理。クライアント専用・バージョン非依存。
 *
 * <h2>なぜ独自のネットワーク通信を作らないのか</h2>
 * <p>宣言の「合図」は既存の {@code EntitySpellCard} をそのまま使う。この実体は
 * {@code THKaguyaLib.spellCardDeclaration()} という<b>唯一の spawn 地点</b>から、
 * プレイヤーの宣言でもボス（{@code EntityDanmakuMob#useSpellCard}）の宣言でも必ず生成される。
 * つまり「クライアントが符卡実体を見た」＝「誰かが符卡を宣言した」という意味が元から備わっている。</p>
 *
 * <p>そこで {@code EntitySpellCard} に同期フィールドを一つ（{@code CASTER_ID}）足すだけで、
 * カード番号（{@code SPELL_CARD_NUMBER}、既存）と宣言者の entity id がクライアントに揃う。
 * Forge {@code SimpleChannel} / NeoForge {@code PayloadRegistrar} のような
 * バージョンごとに書き直しになるプロトコル層を持ち込まずに済む。</p>
 *
 * <p>発火は {@code EntitySpellCard#onSyncedDataUpdated}（同期データが実際に届いた瞬間）で行う。
 * {@code EntityJoinLevelEvent} では駄目：{@code AddEntity} パケットは同期データを積んでおらず、
 * 参加した瞬間に読むとカード番号が 0（＝れっきとした「夢想封印」）に見えて区別できない。</p>
 */
public final class SpellCardCutIn
{
    /** 演出全体の長さ（ミリ秒）。{@link SpellCardCutInRenderer} のタイムラインと一致させること。 */
    public static final long DURATION_MS = 2000L;

    /**
     * ボスが<b>同じ符卡</b>を宣言し直したときに演出を省く時間（ミリ秒）。
     *
     * <p>原版のボスは符卡の {@code attackCounter} が 140 tick（7 秒）で 0 に戻るため、
     * 同じカードを 7 秒ごとに「宣言し直す」（{@code useSpellCard} が毎回
     * {@code EntitySpellCard} を spawn する）。演出を付ける前は弾幕がループするだけに見えていたのが、
     * 今は 7 秒ごとに画面が暗転してしまう。そこでボスに限り、同じカードは 30 秒に 1 回だけ演じる。</p>
     *
     * <p>プレイヤーの宣言はこの制限を受けない（自分で撃った回数だけ演出が出る）。
     * 実際の宣言回数・弾幕・難易度には一切影響しない、純粋に見た目側の抑制。</p>
     */
    private static final long BOSS_REPLAY_COOLDOWN_MS = 30_000L;

    /** 卡号の収容数（原版は 0〜41）。 */
    private static final int CARD_SLOTS = 64;

    /** 追跡する宣言者の上限。超えたら丸ごと捨てる（ボスは数体しかいないので実質発動しない）。 */
    private static final int MAX_TRACKED_CASTERS = 64;

    /** 宣言者の entity id → 各カードを最後に「演じた」時刻（ミリ秒、添字＝カード番号）。 */
    private static final Map<Integer, long[]> BOSS_SHOWN_AT = new HashMap<>();

    private static boolean playing = false;
    private static long startMillis = 0L;
    private static int cardNo = 0;
    private static int casterId = -1;

    private SpellCardCutIn()
    {
    }

    /**
     * 演出を開始する。
     *
     * <p>すでに上演中なら新しい要求は捨てる（ボスが複数体いても画面が重ならないように）。
     * 符卡の再宣言間隔はふつう数秒以上あるので、取りこぼしは実質起きない。</p>
     *
     * <p>ボス（プレイヤー以外）の宣言は、同じカードを {@link #BOSS_REPLAY_COOLDOWN_MS} 以内に
     * 宣言し直しても演じない。プレイヤーは制限なし。</p>
     *
     * <p>発動音は原版の {@code thkaguyamod:spellcard} を鳴らしたいところだが、
     * この mod には {@code .ogg} が一本も入っていない（登録名だけがある状態）ので、
     * 代わりに原版の不死のトーテム音を借りる。将来 ogg を用意したらここを差し替えるだけでよい。</p>
     */
    public static void trigger(int cardNoIn, int casterIdIn)
    {
        if (!THKaguyaConfig.enableSpellCardCutIn)
        {
            return;
        }
        if (isPlaying())
        {
            return;
        }

        long now = System.currentTimeMillis();
        boolean boss = isBossCaster(casterIdIn);

        if (boss && isBossCoolingDown(casterIdIn, cardNoIn, now))
        {
            // ボスのカードループ（原版で 7 秒ごと）による再上演はここで黙って捨てる
            return;
        }

        cardNo = cardNoIn;
        casterId = casterIdIn;
        startMillis = now;
        playing = true;

        if (boss)
        {
            markBossShown(casterIdIn, cardNoIn, now);
        }

        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 1.0F, 0.8F));
    }

    /* ------------------------------------------------------------------ */
    /* ボスの再上演抑制                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * 宣言者がプレイヤーかどうか。
     *
     * <p>クライアントから見えない宣言者（追跡範囲外・同期前）は「ボス扱い」にして抑制側へ倒す。
     * 見えない相手の演出で画面が埋まる方が困るため。</p>
     */
    private static boolean isBossCaster(int casterId)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            return true;
        }
        Entity entity = minecraft.level.getEntity(casterId);
        if (entity == null)
        {
            return true;
        }
        return !(entity instanceof Player);
    }

    private static boolean isBossCoolingDown(int casterId, int cardNoIn, long now)
    {
        if (cardNoIn < 0 || cardNoIn >= CARD_SLOTS)
        {
            return false;
        }
        long[] shownAt = BOSS_SHOWN_AT.get(casterId);
        if (shownAt == null)
        {
            return false;
        }
        long last = shownAt[cardNoIn];
        return last > 0L && now - last < BOSS_REPLAY_COOLDOWN_MS;
    }

    private static void markBossShown(int casterId, int cardNoIn, long now)
    {
        if (cardNoIn < 0 || cardNoIn >= CARD_SLOTS)
        {
            return;
        }
        if (BOSS_SHOWN_AT.size() > MAX_TRACKED_CASTERS)
        {
            BOSS_SHOWN_AT.clear();
        }
        long[] shownAt = BOSS_SHOWN_AT.computeIfAbsent(casterId, key -> new long[CARD_SLOTS]);
        shownAt[cardNoIn] = now;
    }

    /** 上演中か（時間切れならその場で畳む）。 */
    public static boolean isPlaying()
    {
        if (!playing)
        {
            return false;
        }
        long elapsed = elapsedMillis();
        if (elapsed < 0L || elapsed > DURATION_MS)
        {
            playing = false;
            return false;
        }
        return true;
    }

    /** 開始からの経過ミリ秒。 */
    public static long elapsedMillis()
    {
        return System.currentTimeMillis() - startMillis;
    }

    /** 宣言された符卡番号。 */
    public static int cardNo()
    {
        return cardNo;
    }

    /** 宣言者の entity id（クライアント側で未同期なら -1）。 */
    public static int casterId()
    {
        return casterId;
    }

    /**
     * 宣言者の実体を引く。範囲外・削除済み・次元違いなら null。
     * その場合 {@link SpellCardCutInRenderer} は半身像を諦めてカードと文字だけ描く。
     */
    public static Entity caster()
    {
        if (casterId < 0)
        {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            return null;
        }
        return minecraft.level.getEntity(casterId);
    }

    /** 強制終了（設定を切ったときなど）。 */
    public static void reset()
    {
        playing = false;
        casterId = -1;
    }
}
