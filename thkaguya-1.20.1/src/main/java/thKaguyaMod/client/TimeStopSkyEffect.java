package thKaguyaMod.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntitySakuyaStopWatch;
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 懐中時計の時間停止エフェクト（空の反色）。
 *
 * 原版 1.7.10 の {@code RenderSakuyaStopWatch.renderPrivateSquare} / {@code RenderPrivateSquare} は
 * {@code glBlendFunc(ONE_MINUS_DST_COLOR, ZERO)} で純白テクスチャ（DarkTexture.png、実体は白一色）の
 * 球殻を描いていた。合成式は {@code dst' = src*(1-dst)} なので、描画済みの内容がそのまま負片になる。
 * 球殻は半径 {@code min(ticksExisted*12, 240)} で毎tick拡大し、地形より先に描かれるため、
 * 実際に見えるのは「空だけ」の反色（地形・エンティティは上から描かれて上書きされる）。
 * 原版は時間経過による淡出をしない（alpha を掛けているが混合因子に SRC_ALPHA が無いため無効）＝ハードカット。
 *
 * ここでは同じ合成を {@code AFTER_SKY} の全画面quadで行う。この時点のフレームバッファには
 * 空（太陽・月・星を含む）しか描かれていないので、結果は原版と同じ「空だけ反色」になる。
 *
 * 発動条件は原版に合わせる：
 *  - クライアント側に懐中時計（{@link EntitySakuyaWatch} / {@link EntitySakuyaStopWatch}）が読み込まれていること。
 *  - カメラがその停止空間の内側（距離 &lt;= 半径）にいること。原版の球殻は内部からのみ画面を覆う。
 *  - コンフィグ {@code useTimeStopEffect} が true のとき。
 */
@Mod.EventBusSubscriber(modid = THKaguyaCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeStopSkyEffect
{
    /** 原版の球殻半径の上限 */
    private static final double MAX_RADIUS = 240.0D;

    /** 直近に発動対象だったゲームtick。1tickの遅れまで許容して硬切れの境界を安定させる */
    private static long lastSeenTick = Long.MIN_VALUE;

    private TimeStopSkyEffect()
    {
    }

    /**
     * クライアント側のエンティティ一覧から懐中時計を探し、カメラが停止空間の内側にいれば発動対象にする。
     * 原版はエンティティの描画時に球殻を出すので、範囲判定は「カメラが球殻の内側か」と同じ意味になる。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
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
                    lastSeenTick = mc.level.getGameTime();
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
        if (!THKaguyaConfig.useTimeStopEffect)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return;
        }
        // 硬切：懐中時計が消えたら次のtickにはもう出さない
        if (mc.level.getGameTime() - lastSeenTick > 1L)
        {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        // 原版と同じ混合：dst' = src*(1-dst) + dst*0。src を純白にすると負片（1-dst）になる
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // カメラ空間（near plane 0.05 より奥）で画面全体を覆う大きさのquad。
        // 半径1・距離0.1なら画角178°までカバーできるので、FOVに依存しない
        addVertex(builder, matrix, -1.0F, -1.0F, -0.1F);
        addVertex(builder, matrix, 1.0F, -1.0F, -0.1F);
        addVertex(builder, matrix, 1.0F, 1.0F, -0.1F);
        addVertex(builder, matrix, -1.0F, 1.0F, -0.1F);
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void addVertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z)
    {
        builder.vertex(matrix, x, y, z).color(255, 255, 255, 255).endVertex();
    }
}
