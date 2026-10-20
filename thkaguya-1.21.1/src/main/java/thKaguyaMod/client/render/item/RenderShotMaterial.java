package thKaguyaMod.client.render.item;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import thKaguyaMod.entity.item.EntityShotMaterial;

/**
 * 弾の素の描画（原版 {@code RenderShotMaterial} の 1.20.1 版）。
 * 原版は {@code textures/items/material/ShotMaterial.png} の板ポリゴン。
 * 移植版は item アイコン {@code textures/item/shot_material.png}（同源の 16x16）を板に貼る。
 */
public class RenderShotMaterial extends RenderItemBillboard<EntityShotMaterial>
{
	public RenderShotMaterial(EntityRendererProvider.Context context)
	{
		super(context, "textures/item/shot_material.png", 0.5F);
	}
}
