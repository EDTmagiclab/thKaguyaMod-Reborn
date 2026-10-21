package thKaguyaMod.client.spellcard_cutin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 符卡宣言演出の描画（クライアント専用）。
 *
 * <p>タイムライン（ミリ秒、{@link SpellCardCutIn#DURATION_MS} = 2000）：</p>
 * <pre>
 *   0 ──170          画面を暗くする（α 0 → 0.62）
 * 110 ──560          カードが下から 40px 滑り込み、0.35 → 1.07 → 1.0 と跳ねる
 * 150 ──560          右下の半身像が下からせり上がる（scissor の窓を広げる）
 * 400 ──940          符卡名の大文字がフェードインしながら等倍へ寄る
 * 1560 ──1960        すべてを淡く消す（画面の明るさも戻る）
 * </pre>
 *
 * <p>いずれも {@code System.currentTimeMillis()} 基準なので、フレームレートに依存しない。</p>
 *
 * <p>「カードが跳ねる」動きは原版の不死のトーテムと同じ手口（拡大カーブ＋上下動＋フェード）で、
 * 専用のパーティクルやシェーダは要らない。</p>
 */
public final class SpellCardCutInRenderer implements LayeredDraw.Layer
{
    public static final SpellCardCutInRenderer INSTANCE = new SpellCardCutInRenderer();

    /* ---- タイムライン ---- */
    private static final long DARK_IN_START = 0L;
    private static final long DARK_IN_END = 170L;
    private static final long CARD_IN_START = 110L;
    private static final long CARD_IN_END = 560L;
    private static final long BUST_IN_START = 150L;
    private static final long BUST_IN_END = 560L;
    private static final long NAME_IN_START = 400L;
    private static final long NAME_IN_END = 940L;
    private static final long FADE_START = 1560L;
    private static final long FADE_END = 1960L;

    /** 画面を暗くする最大量（0〜1）。 */
    private static final float DARK_MAX = 0.62F;

    /**
     * 符卡番号 → カード画像のファイル名（{@code textures/item/spellcard/<name>.png}）。
     * 添字がそのまま符卡番号。原版で実装されていない番号（31/32/36〜39）は null。
     */
    private static final String[] CARD_TEXTURES = new String[]
    {
        "musoufuuin",                    // 0  霊符「夢想封印」
        "masterspark",                   // 1  恋符「マスタースパーク」
        "kasho_no_eimin",                // 2  華胥の永眠
        "meteonicshower",                // 3  魔砲「メテオニックシャワー」
        "nami_to_tubu_no_kyoukai",       // 4  境符「波と粒の境界」
        "nijuukokushichou",              // 5  結界「二重黒死蝶」
        "scarletshoot",                  // 6  紅符「スカーレットシュート」
        "zenzinrui_no_hisouten",         // 7  全人類の緋想天
        "satuzindoll",                   // 8  奇術「殺人ドール」
        "perfectfreeze",                 // 9  氷符「パーフェクトフリーズ」
        "hikouchuunest",                 // 10 罔両「八雲紫の神隠し」
        "kappaspororoca",                // 11 河符「洩矢の鉄の輪」
        "stardustreverie",               // 12 彗星「ブレイジングスター」
        "kerochan_fuuu_ni_makezu",       // 13 土著神「小小青蛙不输风雨」
        "miraclefruit",                  // 14 奇跡「ミラクルフルーツ」
        "fafrotskies_no_kiseki",         // 15 奇跡「ファフロツキーズの奇跡」
        "youryokuspoiler",               // 16 妖怪退治「妖力スポイラー」
        "moses_no_kiseki",               // 17 開海「モーゼの奇跡」
        "yasaka_no_kamikaze",            // 18 大奇跡「八坂の神風」
        "iciclefall",                    // 19 氷符「アイシクルフォール」
        "starbowbreak",                  // 20 禁忌「スターボウブレイク」
        "catadioptric",                  // 21 禁忌「カタディオプトリック」
        "mishagujisama",                 // 22 祟符「ミシャグジ様」
        "redmagic",                      // 23 紅符「レッドマジック」
        "eternalmeek",                   // 24 奇術「エターナルミーク」
        "non-directionallaser",          // 25 恋符「ノンディレクショナルレーザー」
        "kachoufuugetushoufuurougetu",   // 26 花符「花鳥風月、嘯風弄月」
        "spear_the_gungnir",             // 27 神槍「スピア・ザ・グングニル」
        "moonlightray",                  // 28 月符「ムーンライトレイ」
        "houkakenran",                   // 29 華符「芳華絢爛」
        "saikouranbu",                   // 30 彩符「彩光乱舞」
        null,                            // 31 水符「プリンセスウンディネ」（実装なし）
        null,                            // 32 （未使用）
        "fujiyamavolcano",               // 33 神技「冨士山火山」
        "littlebugstorm",                // 34 虫符「リトルバグストーム」
        "meteor_on_earth",               // 35 流星「メテオ・オン・アース」
        null,                            // 36
        null,                            // 37
        null,                            // 38
        null,                            // 39
        "gagoujicyclone",                // 40 連鎖「縮小していく世界」
        "brilliantdragonbullet",         // 41 神光「ブリリアントドラゴンバレット」
    };

    /** 番号が範囲外／未実装のときに使う「空のカード枠」。 */
    private static final ResourceLocation FALLBACK_TEXTURE =
            cardTexture("textures/item/spellcard/spellcard_frame.png");

    /** 番号 → 実際の ResourceLocation（遅延生成してキャッシュ）。 */
    private static final ResourceLocation[] TEXTURE_CACHE = new ResourceLocation[CARD_TEXTURES.length];

    private SpellCardCutInRenderer()
    {
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker)
    {
        Minecraft minecraft = Minecraft.getInstance();
        renderCutIn(graphics, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }

    /**
     * 演出本体。版ごとに違うのは呼び出し口（{@code IGuiOverlay} か {@code LayeredDraw.Layer} か）と
     * その引数だけで、この中身は 1.20.1 / 1.21.1 で同じものがそのまま動く。
     */
    private void renderCutIn(GuiGraphics graphics, int width, int height)
    {
        if (!THKaguyaConfig.enableSpellCardCutIn)
        {
            return;
        }
        if (!SpellCardCutIn.isPlaying())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.options.hideGui)
        {
            // 物品欄やポーズ画面を開いている間は邪魔しない
            return;
        }

        long elapsed = SpellCardCutIn.elapsedMillis();
        float fadeOut = 1.0F - clamp01((elapsed - FADE_START) / (float) (FADE_END - FADE_START));
        if (fadeOut <= 0.0F)
        {
            return;
        }

        int cardNo = SpellCardCutIn.cardNo();
        int centerX = width / 2;
        int cardCenterY = Math.round(height * 0.42F);

        drawDarken(graphics, width, height, elapsed, fadeOut);
        drawCard(graphics, centerX, cardCenterY, width, height, elapsed, cardNo, fadeOut);
        drawBust(graphics, width, height, elapsed);
        drawName(graphics, minecraft, centerX, height, elapsed, cardNo, fadeOut);
    }

    /* ------------------------------------------------------------------ */
    /* 各パーツ                                                            */
    /* ------------------------------------------------------------------ */

    private void drawDarken(GuiGraphics graphics, int width, int height, long elapsed, float fadeOut)
    {
        float grow = easeOutCubic(clamp01((elapsed - DARK_IN_START) / (float) (DARK_IN_END - DARK_IN_START)));
        float dark = DARK_MAX * grow * fadeOut;
        if (dark > 0.004F)
        {
            graphics.fill(0, 0, width, height, argb(dark, 0x000000));
        }
    }

    private void drawCard(GuiGraphics graphics, int centerX, int centerY, int width, int height,
                          long elapsed, int cardNo, float fadeOut)
    {
        float raw = clamp01((elapsed - CARD_IN_START) / (float) (CARD_IN_END - CARD_IN_START));

        float slide = easeOutCubic(raw);
        float pop = easeOutBack(raw);

        float scale = 0.35F + 0.65F * pop;
        float offsetY = 40.0F * (1.0F - slide);
        float rotation = -8.0F * (1.0F - slide);
        float alpha = clamp01(raw * 1.8F) * fadeOut;
        if (alpha <= 0.004F)
        {
            return;
        }

        int size = Mth.clamp(Math.round(Math.min(width * 0.22F, height * 0.40F)), 56, 180);
        ResourceLocation texture = textureOf(cardNo);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY + offsetY, 0.0F);
        pose.scale(scale, scale, 1.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(texture, -size / 2, -size / 2, size, size, 0.0F, 0.0F, 128, 128, 128, 128);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private void drawBust(GuiGraphics graphics, int width, int height, long elapsed)
    {
        Entity caster = SpellCardCutIn.caster();
        if (!(caster instanceof LivingEntity living) || !EntityBustRenderer.canRender(living))
        {
            return;
        }

        float raw = clamp01((elapsed - BUST_IN_START) / (float) (BUST_IN_END - BUST_IN_START));
        if (raw <= 0.0F)
        {
            return;
        }

        // 窓の上端を「下から上へ」広げる＝せり上がって見える。
        // 半身像そのものには透明度をかけられない（原版の実体描画が α=1 で塗るため）ので、
        // 露出量で出現を表現する。
        float rise = easeOutCubic(raw);

        int boxWidth = Math.min(96, Math.round(width * 0.22F));
        int boxHeight = Math.min(112, Math.round(height * 0.46F));
        int right = width - 12;
        int bottom = height - 10;
        int left = right - boxWidth;
        int top = bottom - boxHeight;

        // 足元座標・拡大率・切り抜きは EntityBustRenderer が吸収する（版ごとに API が違うため）
        EntityBustRenderer.render(graphics, left, top, right, bottom, rise, living);
    }

    private void drawName(GuiGraphics graphics, Minecraft minecraft, int centerX, int height,
                          long elapsed, int cardNo, float fadeOut)
    {
        float raw = clamp01((elapsed - NAME_IN_START) / (float) (NAME_IN_END - NAME_IN_START));
        float appear = easeOutCubic(raw);

        int alpha = Mth.clamp((int) (255.0F * appear * fadeOut), 0, 255);
        if (alpha < 4)
        {
            return;
        }

        Font font = minecraft.font;
        Component name = cardNameOf(cardNo);
        int color = (alpha << 24) | 0xFFFFFF;

        float scale = 1.65F + 0.35F * appear;
        float drift = 10.0F * (1.0F - appear);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, Math.round(height * 0.70F) + drift, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, name, -font.width(name) / 2, 0, color, true);
        pose.popPose();

        // 誰の符卡か（半身像が出せない状況でも手掛かりになる）
        Entity caster = SpellCardCutIn.caster();
        if (caster != null)
        {
            Component by = Component.empty()
                    .append(Component.literal("\u2014\u2014 ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(caster.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            graphics.drawCenteredString(font, by, centerX,
                    Math.round(height * 0.70F) + 20, (alpha << 24) | 0xB0B0B0);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 補助                                                                */
    /* ------------------------------------------------------------------ */

    /** リソース位置の作り方が版ごとに違うので、ここだけ差し替える（1.21.1 は fromNamespaceAndPath）。 */
    private static ResourceLocation cardTexture(String path)
    {
        return ResourceLocation.fromNamespaceAndPath("thkaguya", path);
    }

    private static ResourceLocation textureOf(int cardNo)
    {
        if (cardNo < 0 || cardNo >= CARD_TEXTURES.length || CARD_TEXTURES[cardNo] == null)
        {
            return FALLBACK_TEXTURE;
        }
        ResourceLocation cached = TEXTURE_CACHE[cardNo];
        if (cached == null)
        {
            cached = cardTexture("textures/item/spellcard/" + CARD_TEXTURES[cardNo] + ".png");
            TEXTURE_CACHE[cardNo] = cached;
        }
        return cached;
    }

    private static Component cardNameOf(int cardNo)
    {
        String key = "item.thkaguya.spell_card." + cardNo;
        if (Language.getInstance().has(key))
        {
            return Component.translatable(key);
        }
        return Component.literal("Spell Card #" + cardNo);
    }

    private static int argb(float alpha, int rgb)
    {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp01(float value)
    {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }

    private static float easeOutCubic(float x)
    {
        float inverse = 1.0F - x;
        return 1.0F - inverse * inverse * inverse;
    }

    /** 終点でわずかに行き過ぎてから戻る（「跳ねる」感じ）。 */
    private static float easeOutBack(float x)
    {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float inverse = x - 1.0F;
        return 1.0F + c3 * inverse * inverse * inverse + c1 * inverse * inverse;
    }
}
