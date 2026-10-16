package thKaguyaMod.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * 五つの難題MOD+ 専用 RenderType（1.20.1）。
 *
 * <p>1.7.10 原版弹幕渲染直接操作固定管线 GL 状态，移植时必须把这些状态重新表达成
 * 1.20.1 的 RenderType（= 一整套「shader + 纹理 + 混合 + 深度 + 剔除」状态包）。
 * 原版用到的混合式有三种，本案逐一还原：
 *
 * <pre>
 *   BLEND_ADDITIVE    glBlendFunc(GL_SRC_ALPHA, GL_ONE)                 加算（最多数弹幕）
 *   BLEND_GLOW        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)       「発光」系
 *   BLEND_DOUBLE_ADD  glBlendFunc(GL_ONE, GL_ONE)                       纯加算（幽霊弾）
 * </pre>
 *
 * 另外两个原版状态也要还原：
 * <ul>
 *   <li>{@code glDepthMask(false)} → {@code writeMaskState(COLOR_WRITE)}（只写颜色不写深度），
 *       否则叠加弹幕之间会互相遮挡；</li>
 *   <li>{@code glDisable(GL_LIGHTING)} → 顶点 {@code uv2} 传 {@code LightTexture.FULL_BRIGHT}
 *       （全亮）。原版 Tessellator 未设置 brightness，1.7.10 会退化为全亮，因此弹幕不受光照影响。</li>
 * </ul>
 *
 * <p>关于能否自定义：{@code RenderStateShard} 的<b>静态字段</b>（TRANSLUCENT_TRANSPARENCY 等）
 * 是 protected，无法直接引用；但它的<b>内部 shard 类</b>（TransparencyStateShard / ShaderStateShard /
 * TextureStateShard / WriteMaskStateShard / CullStateShard ...）以及它们的构造函数<b>全部是 public</b>
 * （javap 实测确认），因此可以自行 new 出所需 shard 并用
 * {@code RenderType.create(...)}（public static）拼装。无需 AccessTransformer 或 Mixin。
 */
@OnlyIn(Dist.CLIENT)
public final class THRenderTypes
{
    private THRenderTypes()
    {
    }

    /** GL_SRC_ALPHA / GL_ONE ：加算混合（LIGHT・BIGLIGHT・KISHITU・STAR・BIG・CRYSTAL・BUTTERFLY・RICE・OVAL・SCALE・電射白芯 pass） */
    public static final int BLEND_ADDITIVE = 0;
    /** GL_ONE / GL_ONE_MINUS_SRC_COLOR ：「発光」混合（FAMILIAR・WIND・遅延光效・苦無/矢外層・御札・電射彩色外層） */
    public static final int BLEND_GLOW = 1;
    /** GL_ONE / GL_ONE ：纯加算（PHANTOM 幽霊弾） */
    public static final int BLEND_DOUBLE_ADD = 2;
    private static final int BLEND_COUNT = 3;

    // ------------------------------------------------------------------
    // shard（全部可 public 构造）
    // ------------------------------------------------------------------
    private static final RenderStateShard.ShaderStateShard SHADER_ENTITY_TRANSLUCENT =
            new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentShader);
    private static final RenderStateShard.ShaderStateShard SHADER_ENTITY_CUTOUT =
            new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutShader);
    private static final RenderStateShard.ShaderStateShard SHADER_ENTITY_CUTOUT_NO_CULL =
            new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutNoCullShader);

    private static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
    private static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);
    /** glDepthMask(false)
     *  只写颜色 */
    private static final RenderStateShard.WriteMaskStateShard COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);
    /** glDepthMask(true) 颜色 + 深度 */
    private static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(true, true);
    private static final RenderStateShard.CullStateShard CULL = new RenderStateShard.CullStateShard(true);
    private static final RenderStateShard.CullStateShard NO_CULL = new RenderStateShard.CullStateShard(false);
    private static final RenderStateShard.DepthTestStateShard LEQUAL = new RenderStateShard.DepthTestStateShard("lequal", 515);

    private static final RenderStateShard.TransparencyStateShard[] BLENDS = new RenderStateShard.TransparencyStateShard[BLEND_COUNT];

    static
    {
        BLENDS[BLEND_ADDITIVE] = transparency("thkaguya_additive",
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BLENDS[BLEND_GLOW] = transparency("thkaguya_glow",
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        BLENDS[BLEND_DOUBLE_ADD] = transparency("thkaguya_double_add",
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    }

    private static RenderStateShard.TransparencyStateShard transparency(String name,
            GlStateManager.SourceFactor src, GlStateManager.DestFactor dst)
    {
        return new RenderStateShard.TransparencyStateShard(name,
                () ->
                {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(src, dst);
                },
                RenderSystem::disableBlend);
    }

    // ------------------------------------------------------------------
    // 工厂
    // ------------------------------------------------------------------
    private static final Map<String, RenderType> CACHE = new HashMap<>();

    /**
     * 混合パス（原版 glEnable(GL_BLEND) 那一层）。
     *
     * @param blend       {@link #BLEND_ADDITIVE} / {@link #BLEND_GLOW} / {@link #BLEND_DOUBLE_ADD}
     * @param cull        true = 表面描画（3D 形状），false = 両面描画（面向相机的 billboard 必须 false）
     * @param depthWrite  true = glDepthMask(true)
     */
    public static RenderType blend(ResourceLocation texture, int blend, boolean cull, boolean depthWrite)
    {
        return get(texture, blend, cull, depthWrite, false);
    }

    /** 加算 + 両面 + 不写深度：面向相机的 billboard / 椭球外层 等最常用组合 */
    public static RenderType additive(ResourceLocation texture)
    {
        return blend(texture, BLEND_ADDITIVE, false, false);
    }

    /** 加算 + 表面描画 + 不写深度 */
    public static RenderType additiveCull(ResourceLocation texture)
    {
        return blend(texture, BLEND_ADDITIVE, true, false);
    }

    /** 「発光」混合（GL_ONE, GL_ONE_MINUS_SRC_COLOR）+ 両面 + 不写深度 */
    public static RenderType glow(ResourceLocation texture)
    {
        return blend(texture, BLEND_GLOW, false, false);
    }

    public static RenderType glow(ResourceLocation texture, boolean cull, boolean depthWrite)
    {
        return blend(texture, BLEND_GLOW, cull, depthWrite);
    }

    /** 纯加算（GL_ONE, GL_ONE）+ 両面 + 不写深度 */
    public static RenderType doubleAdd(ResourceLocation texture)
    {
        return blend(texture, BLEND_DOUBLE_ADD, false, false);
    }

    /**
     * 不透明（alpha test）パス —— 原版 glDisable(GL_BLEND) 那一层（白芯 / 苦無刀身 / 符札 等）。
     * 深度写入打开，由 shader 的 alpha discard 取代 1.7.10 的 GL_ALPHA_TEST。
     */
    public static RenderType cutout(ResourceLocation texture, boolean cull)
    {
        return get(texture, BLEND_ADDITIVE, cull, true, true);
    }

    private static RenderType get(ResourceLocation texture, int blend, boolean cull, boolean depthWrite, boolean alphaTest)
    {
        String key = texture + "|" + blend + "|" + cull + "|" + depthWrite + "|" + alphaTest;
        RenderType cached = CACHE.get(key);
        if (cached != null)
        {
            return cached;
        }
        RenderType created = create(texture, blend, cull, depthWrite, alphaTest);
        CACHE.put(key, created);
        return created;
    }

    private static RenderType create(ResourceLocation texture, int blend, boolean cull, boolean depthWrite, boolean alphaTest)
    {
        var builder = RenderType.CompositeState.builder()
                .setShaderState(alphaTest
                        ? (cull ? SHADER_ENTITY_CUTOUT : SHADER_ENTITY_CUTOUT_NO_CULL)
                        : SHADER_ENTITY_TRANSLUCENT)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false));

        if (!alphaTest)
        {
            builder.setTransparencyState(BLENDS[blend]);
        }

        RenderType.CompositeState state = builder
                .setWriteMaskState(depthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                .setCullState(cull ? CULL : NO_CULL)
                .setDepthTestState(LEQUAL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false);

        String name = "thkaguya_" + (alphaTest ? "cutout" : "blend" + blend) + (cull ? "_cull" : "_nocull")
                + (depthWrite ? "_dwrite" : "_dnowrite") + "_" + texture.getPath().replace('/', '_');
        // sortOnUpload：vanilla の entity_translucent と同じく混合パスのみ distance sort する
        return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, !alphaTest, state);
    }
}
