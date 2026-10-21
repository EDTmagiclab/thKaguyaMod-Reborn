package thKaguyaMod.client.spellcard_cutin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 符卡宣言演出の「半身像」を描く薄い適配層。
 *
 * <p><b>ここだけがバージョン依存</b>。演出の他の部分（タイムライン・黒幕・カード・文字）は
 * 1.20.1 でも 1.21.1 でもそのまま動くが、半身像は原版の内部 API を借りているので
 * シグネチャが版ごとに動く：</p>
 *
 * <ul>
 *   <li>1.20.1 Forge：{@code renderEntityInInventoryFollowsAngle(GuiGraphics, int x, int y,
 *       int scale, float angleX, float angleY, LivingEntity)}。{@code (x, y)} は<b>足元</b>で、
 *       実体はそこから上へ伸びる。切り抜きは自前で {@code enableScissor}。</li>
 *   <li>1.21.1 NeoForge：同名だが {@code (GuiGraphics, int x0, int y0, int x1, int y1,
 *       int scale, float angleX, float angleY, LivingEntity)} の 10 引数。四角形を渡すと
 *       <b>内部で {@code enableScissor} までやってくれる</b>（実体は矩形の中心に置かれる）。</li>
 * </ul>
 *
 * <p>呼び出し側（{@link SpellCardCutInRenderer}）を版ごとに分岐させないため、
 * こちらの公開シグネチャは「表示したい矩形」で統一し、足元座標や拡大率の換算は
 * このクラスの中で吸収する。移植するときはこのクラスだけ書き換えればよい。</p>
 */
public final class EntityBustRenderer
{
    /** 体の向き（度）。0 = 正面。 */
    private static final float BODY_YAW = 20.0F;

    /** 顔の上下（度）。正でやや見下ろす。 */
    private static final float HEAD_PITCH = 0.0F;

    /** 足元を矩形の下辺より少し下に置く（脚が枠外に出て胸像らしくなる）。 */
    private static final float FOOT_BELOW_BOX = 0.10F;

    /** 実体の拡大率＝矩形の高さに対する比率。原版物品欄は全身で 30 程度。 */
    private static final float SCALE_PER_BOX_HEIGHT = 0.58F;

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

        int footX = (left + right) / 2;
        int footY = bottom + Math.round(boxHeight * FOOT_BELOW_BOX);
        int scale = Math.round(boxHeight * SCALE_PER_BOX_HEIGHT);

        // 原版の関数は内部で Lighting を切り替え、最後に bufferSource を flush する。
        // GUI 描画段階で呼ぶぶんには Screen から呼ぶのと同じ条件なので、そのまま使える。
        guiGraphics.flush();
        guiGraphics.enableScissor(left, visibleTop, right, bottom);
        try
        {
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                    guiGraphics, footX, footY, scale, BODY_YAW, HEAD_PITCH, entity);
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
