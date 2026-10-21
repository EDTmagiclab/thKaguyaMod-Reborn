package thKaguyaMod.client.spellcard_cutin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
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
        cardNo = cardNoIn;
        casterId = casterIdIn;
        startMillis = System.currentTimeMillis();
        playing = true;

        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 1.0F, 0.8F));
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
