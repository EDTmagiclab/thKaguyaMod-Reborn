package thKaguyaMod.client.spellcard_cutin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 符卡宣言演出の「半身像」を描く薄い適配層（1.21.1 NeoForge 版）。
 *
 * <p><b>ここだけがバージョン依存</b>。演出の他の部分（タイムライン・黒幕・カード・文字）は
 * 1.20.1 とまったく同じものが動く。1.21.1 の原版 API は 1.20.1 と次の 2 点で違う：</p>
 *
 * <ul>
 *   <li>引数が 10 個 {@code (GuiGraphics, int x0, int y0, int x1, int y1, int scale,
 *       float angleX, float angleY, LivingEntity)}。{@code (x0,y0)-(x1,y1)} の矩形を渡すと
 *       <b>内部で {@code enableScissor} までやってくれる</b>（成対で disable されることも確認済み）。</li>
 *   <li>実体は「足元」ではなく<b>矩形の中心</b>を基準に置かれる。</li>
 * </ul>
 *
 * <p>呼び出し側（{@link SpellCardCutInRenderer}）を版ごとに分岐させないため、
 * 公開シグネチャは 1.20.1 版と完全に同じ「矩形＋出現率」にしてある。
 * 矩形は<b>固定</b>で渡し（＝実体の位置と大きさが動かない）、せり上がりの表現だけ自前の
 * scissor で上辺を削る。原版が内側でも scissor を張るが、{@code ScissorStack} は入れ子を
 * 取れる（内側は成対で閉じる）ので二重になっても問題ない。</p>
 */
public final class EntityBustRenderer
{
    /** 体の向き（度）。0 = 正面。1.20.1 版と揃える。 */
    private static final float BODY_YAW = 20.0F;

    /** 顔の上下（度）。正でやや見下ろす。 */
    private static final float HEAD_PITCH = 0.0F;

    /**
     * 実体の拡大率＝矩形の高さに対する比率。
     * 1.20.1 は「足元」基準、こちらは「矩形の中心」基準で原版の配置が違うため、
     * 同じ見た目になる値は版ごとに変わる（ここは実機で詰めた値）。
     */
    private static final float SCALE_PER_BOX_HEIGHT = 0.50F;

    /** 姿勢の補間値。静止表示なので「現在 tick の最終姿勢」を意味する 1.0 を渡す。 */
    private static final float PARTIAL_TICK = 1.0F;

    private EntityBustRenderer()
    {
    }

    /**
     * 指定した矩形の中に半身像を描く。矩形の外は切り抜かれる。
     *
     * @param guiGraphics 描画先
     * @param left        矩形の左（GUI 座標）
     * @param top         矩形の上（＝完全に出きったときの上端）
     * @param right       矩形の右
     * @param bottom      矩形の下
     * @param reveal      出現率 0〜1。0 で完全に隠れ、1 で矩形全体が出る
     *                    （下端から上へ窓が開く＝せり上がって見える）。
     *                    半身像には透明度をかけられない（原版の実体描画が α=1 で塗る）ため、
     *                    フェードの代わりに露出量で出現を表現している。
     * @param entity      描く実体
     */
    public static void render(GuiGraphics guiGraphics, int left, int top, int right, int bottom,
                              float reveal, LivingEntity entity)
    {
        int boxWidth = right - left;
        int boxHeight = bottom - top;
        if (boxWidth <= 0 || boxHeight <= 0)
        {
            return;
        }

        int visibleTop = Mth.clamp(Math.round(Mth.lerp(reveal, bottom, top)), top, bottom);
        int scale = Math.round(boxHeight * SCALE_PER_BOX_HEIGHT);

        // 原版の関数は内部で Lighting を切り替え、最後に bufferSource を flush する。
        // GUI 描画段階で呼ぶぶんには Screen から呼ぶのと同じ条件なので、そのまま使える。
        //
        // 引数は (g, x0, y0, x1, y1, scale, partialTick, angleX, angleY, entity)。
        // 1.20.1 には無い partialTick が 6 番目に挟まっている（原版の
        // renderEntityInInventoryFollowsMouse がその位置に渡しているのを確認済み）。
        // 静止画なので 1.0F（＝現在 tick の最終姿勢）を渡す。
        guiGraphics.flush();
        guiGraphics.enableScissor(left, visibleTop, right, bottom);
        try
        {
            // 矩形は固定（top〜bottom）で渡す：せり上がりは外側の scissor が担当するため、
            // ここで top を動かすと実体そのものが動いてしまう。
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                    guiGraphics, left, top, right, bottom, scale,
                    PARTIAL_TICK, BODY_YAW, HEAD_PITCH, entity);
        }
        finally
        {
            // 実体描画が万一失敗しても scissor を開いたままにしない
            // （開いたままだと以降の HUD が全部切り取られる）
            guiGraphics.disableScissor();
        }
    }

    /** 呼び出し側で「今この実体を描けるか」を判定するための補助。 */
    public static boolean canRender(LivingEntity entity)
    {
        return entity != null
                && entity.isAlive()
                && Minecraft.getInstance().level != null
                && entity.level() == Minecraft.getInstance().level;
    }
}
