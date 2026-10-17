package thKaguyaMod.client.render.living;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.living.EntityDanmakuMob;

/**
 * 弾幕 boss 通用渲染器（1.20.1 移植版，阶段 3d-6）。
 *
 * <p>1.7.10 の {@code RenderTHBoss} はこのクラスの中で頭上に
 * {@code textures/mob/status.png} の体力バー・ボス名（緑）・符卡名（白）を
 * 描画していた（{@code renderTHBossStatus}）。1.20.1 では実際に動かしたところ
 * 体力バーの見た目が崩れた（視野角 ±20°制限・カメラ向き・距離による拡大率が
 * 組み合わさるため）ので、体力バーはバニラの {@code ServerBossEvent}
 * （エンダードラゴンと同じ画面頂点のバー）へ置き換えた。
 *
 * <p>バーの実体は {@link EntityDanmakuMob#hasBossBar()} が true のボス
 * （円、巫女、霊夢、ルーミア、咲夜、早苗、屠自古、リグル）が持つ。
 * このクラスはモデルとテクスチャの描画だけを担当する。
 */
public class RenderTHBoss<T extends EntityDanmakuMob> extends MobRenderer<T, EntityModel<T>>
{
    private final ResourceLocation texture;

    public RenderTHBoss(EntityRendererProvider.Context context, EntityModel<T> model, String texturePath, float shadowRadius)
    {
        super(context, model, shadowRadius);
        this.texture = new ResourceLocation(THKaguyaCore.MODID, texturePath);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return texture;
    }
}
