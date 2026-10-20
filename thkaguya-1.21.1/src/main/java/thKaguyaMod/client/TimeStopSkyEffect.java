package thKaguyaMod.client;
import net.neoforged.fml.common.EventBusSubscriber;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4fStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntitySakuyaStopWatch;
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 懐中時計の時間停止エフェクト（空を覆う幕）。
 *
 * 原版 1.7.10 の {@code RenderSakuyaStopWatch} / {@code RenderPrivateSquare} は、半径
 * {@code min(ticksExisted*12, 240)} の球殻を {@code glBlendFunc(ONE_MINUS_DST_COLOR, ZERO)} で描いていた
 * （合成式 {@code dst' = src*(1-dst)}、src は純白 → 負片）。球殻は地形より先に描かれるので、
 * 実際に見えるのは「空だけ」のエフェクトで、地形・エンティティは上から描かれて上書きされる。
 *
 * 本実装はこの「空だけ」を {@code AFTER_SKY} の全画面quadで再現する。この時点のフレームバッファには
 * 空（太陽・月・星・雲の手前まで）しか無いので、地形やエンティティは後から上書きされる。
 *
 * ただし原版の「純白で負片」は、青空の負が橙褐色（{@code 1-青 = 橙}）になって汚い茶色に見える
 * （ユーザー指摘）。そこで負片のまま色相だけ冷たい側へ寄せる：
 *  - 1枚目：{@code blendFunc(ONE_MINUS_DST_COLOR, ZERO)}、頂点色 {@code TINT} →
 *    {@code dst' = TINT * (1-dst)}（負片のコントラストを保ったまま青紫へ）
 *  - 2枚目：{@code blendFunc(ONE, ONE)}、頂点色 {@code GLOW} →
 *    {@code dst' += GLOW}（負片が暗く沈みすぎるので冷たい底上げを足す）
 * 昼はくすんだ藤色、夜は明るい薄紫、夕焼けは青に転ぶ。色は下の定数で調整できる。
 * 硬切（淡出なし）は原版どおり：原版の alpha は混合因子に SRC_ALPHA が無く実際には効いていない。
 *
 * 発動条件は原版に合わせる：
 *  - クライアント側に懐中時計（{@link EntitySakuyaWatch} / {@link EntitySakuyaStopWatch}）が存在すること。
 *  - カメラがその停止空間の内側（距離 &lt;= {@code ticksExisted*12}、上限 240 格）にいること。
 *  - コンフィグ {@code useTimeStopEffect} が true のとき。
 */
@EventBusSubscriber(modid = THKaguyaCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class TimeStopSkyEffect
{
    /** 原版の球殻半径の上限 */
    private static final double MAX_RADIUS = 240.0D;

    /** 負片にかける色。青空の負（橙褐色）を青紫へ寄せるための乗算色 */
    private static final float TINT_R = 0.38F;
    private static final float TINT_G = 0.30F;
    private static final float TINT_B = 0.95F;

    /** 負片の底上げ（加算）。負片だけだと暗く沈むので冷たい光を足す */
    private static final float GLOW_R = 0.22F;
    private static final float GLOW_G = 0.14F;
    private static final float GLOW_B = 0.38F;

    /** 直近のtickで「カメラが停止空間の内側にいた」か。毎tickクライアント側で更新する */
    private static boolean active = false;

    private TimeStopSkyEffect()
    {
    }

    /**
     * クライアント側のエンティティ一覧から懐中時計を探し、カメラが停止空間の内側にいれば発動状態にする。
     * 原版はエンティティの描画時に球殻を出すので、範囲判定は「カメラが球殻の内側か」と同じ意味になる。
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        // 毎tick必ず false から始める（懐中時計が消えたら次のtickで即座に切れる＝硬切）
        active = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused())
        {
            return;
        }
        if (!THKaguyaConfig.useTimeStopEffect)
        {
            return;
        }

        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (entity instanceof EntitySakuyaWatch || entity instanceof EntitySakuyaStopWatch)
            {
                // 原版：爆発的に広がる停止空間。半径は ticksExisted*12（上限 240）。tick 0 では半径 0
                double radius = Math.min(entity.tickCount * 12.0D, MAX_RADIUS);
                if (mc.player.distanceToSqr(entity) <= radius * radius)
                {
                    active = true;
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
        {
            return;
        }
        if (!active || !THKaguyaConfig.useTimeStopEffect)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        renderCurtain(mc.getWindow());
    }

    /**
     * 画面全体を覆う幕を描く。ここは「空しか描かれていない」タイミングなので、
     * 地形やエンティティは後段で上書きされ、結果として空だけが色を変える。
     *
     * 座標は GUI と同じ正射影 + 単位モデルビューで画面ピクセル直指定にする。
     * レベル描画中の PoseStack（カメラ回転・平行移動が乗っている）を使うと
     * quad が傾いて「空の半分だけ変わる壁」になるため、射影行列ごと差し替える。
     */
    private static void renderCurtain(Window window)
    {
        int width = window.getWidth();
        int height = window.getHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // GUI と同じ「正射影 + 単位モデルビュー」で画面ピクセル直指定にする
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, (float) width, (float) height, 0.0F, -1000.0F, 1000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        modelViewStack.identity();
        RenderSystem.applyModelViewMatrix();

        // 1枚目：負片（dst' = TINT * (1-dst)）
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO);
        drawFullscreenQuad(width, height, TINT_R, TINT_G, TINT_B);
        // 2枚目：底上げ（dst' += GLOW）
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        drawFullscreenQuad(width, height, GLOW_R, GLOW_G, GLOW_B);

        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.DISTANCE_TO_ORIGIN);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawFullscreenQuad(int width, int height, float red, float green, float blue)
    {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addVertex(builder, 0.0F, 0.0F, red, green, blue);
        addVertex(builder, 0.0F, (float) height, red, green, blue);
        addVertex(builder, (float) width, (float) height, red, green, blue);
        addVertex(builder, (float) width, 0.0F, red, green, blue);
        BufferUploader.drawWithShader(builder.build());
    }

    private static void addVertex(BufferBuilder builder, float x, float y, float red, float green, float blue)
    {
        builder.addVertex(x, y, 0.0F).setColor(red, green, blue, 1.0F);
    }
}
