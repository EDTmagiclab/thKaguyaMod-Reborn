package thKaguyaMod.client.spellcard_cutin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * 由利宣言演出の「半身像」を描く薄い適配層。
 *
 * <p><b>ここだけがバージョン依存</b>。演出の他の部分（タイムライン・黒幕・カード・文字）は
 * 1.20.1 でも 1.21.1 でもそのまま動くが、半身像は原版の内部 API を借りているので
 * シグネチャが版ごとに動く：</p>
 *
 * <ul>
 *   <li>1.20.1 Forge：{@code renderEntityInInventoryFollowsAngle(GuiGraphics, int x, int y, int scale,
 *       float angleX, float angleY, LivingEntity)}</li>
 *   <li>1.21.1 NeoForge：同名だが 10 引数（{@code int x0, int y0, int x1, int y1} の矩形指定に変わった）</li>
 * </ul>
 *
 * <p>移植するときはこのメソッドの中身だけ書き換えればよい。</p>
 *
 * <p>モデルの向き：{@code angleX} は「体の向き（度）」で、0 がカメラ正面。少しだけ横向きにすると
 * 棒立ちに見えにくいので既定で 20 度だけ振ってある。</p>
 */
public final class EntityBustRenderer
{
    /** 体の向き（度）。0 = 正面。 */
    private static final float BODY_YAW = 20.0F;

    /** 顔の上下（度）。正でやや見下ろす。 */
    private static final float HEAD_PITCH = 0.0F;

    private EntityBustRenderer()
    {
    }

    /**
     * 指定位置（足元の中心）に半身像を描く。
     *
     * @param guiGraphics 描画先
     * @param x           足元の中心 X（GUI 座標）
     * @param y           足元の Y（GUI 座標）
     * @param scale       拡大率（原版の物品欄は全身で 30 程度。半身なら 60 前後が扱いやすい）
     * @param entity      描く実体
     */
    public static void render(GuiGraphics guiGraphics, int x, int y, int scale, LivingEntity entity)
    {
        // 原版の関数は内部で Lighting を切り替え、最後に bufferSource を flush する。
        // GUI の描画段階で呼ぶぶんには Screen から呼ぶのと同じ条件なので、そのまま使える。
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                guiGraphics, x, y, scale, BODY_YAW, HEAD_PITCH, entity);
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
