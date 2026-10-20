package thKaguyaMod.client.render.item;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.item.EntityTHItem;

/**
 * パワーアップ／得点／ボム／エクステンドアイテムの描画
 * （原版 {@code RenderTHItem} の 1.20.1 版）。
 *
 * <p>原版の貼り分け:
 * {@code SmallPowerUpItem.png} / {@code BigPowerUpItem.png} / {@code THPointItem.png} /
 * {@code BombItem.png} / {@code ExtendItem.png}。移植版は同名の item アイコンを使う。
 */
public class RenderTHItem extends RenderItemBillboard<EntityTHItem>
{
	private static final ResourceLocation SMALL_POWER = ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/item/power_up_item_small.png");
	private static final ResourceLocation BIG_POWER = ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/item/power_up_item_big.png");
	private static final ResourceLocation POINT = ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/item/point_item.png");
	private static final ResourceLocation BOMB = ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/item/bomb_item.png");
	private static final ResourceLocation EXTEND = ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/item/extend_item.png");

	public RenderTHItem(EntityRendererProvider.Context context)
	{
		super(context, "textures/item/power_up_item_small.png", 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTHItem entity)
	{
		switch (entity.getItemType())
		{
			case EntityTHItem.ITEM_ID_BIGPOWER:
				return BIG_POWER;
			case EntityTHItem.ITEM_ID_POINT:
				return POINT;
			case EntityTHItem.ITEM_ID_BOMB:
				return BOMB;
			case EntityTHItem.ITEM_ID_EXTEND:
				return EXTEND;
			case EntityTHItem.ITEM_ID_SMALLPOWER:
			default:
				return SMALL_POWER;
		}
	}
}
