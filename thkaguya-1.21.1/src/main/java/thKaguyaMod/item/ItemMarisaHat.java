package thKaguyaMod.item;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import thKaguyaMod.client.model.item.ModelMarisaHat;
import thKaguyaMod.init.THKaguyaArmorMaterials;

/**
 * 魔理沙の帽子（Marisa's Hat）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemMarisaHat.java（119 行，extends ItemArmor）：
 * ・armorMaterial_Marisa（防御 1/1/1/1）、type 0 = 頭、setMaxDamage(40)
 * ・getArmorTexture → textures/armor/MarisaHatTexture.png、エンチャント不可
 * ・onArmorTick：周囲 5 格の EntityItem 全部に対して onCollideWithPlayer(player) を呼ぶ
 *   ＝かぶるとアイテムの拾得判定が 5 格まで広がる（原版の目玉機能）
 *
 * 1.20.1 対応：
 * ・getArmorModel（ModelMarisaHat）は今回の範囲外 → ユーザー了承済みの「アーマーレイヤー方式」。
 *   テクスチャは MarisaHatTexture.png を models/armor/marisa_hat_layer_1.png として同梱。
 * ・onArmorTick は IForgeItem のまま存在するのでそのまま使える（EntityItem → ItemEntity）。
 * ・耐久 40 は素材ではなく Item#getMaxDamage の上書きで与える（素材 MARISA は耐久 0 = 他部位用）。
 */
public class ItemMarisaHat extends ArmorItem
{
	/** 原版 setMaxDamage(40) */
	public static final int MAX_DURABILITY = 40;
	/** 原版 expand(5.0D, 5.0D, 5.0D) */
	public static final double PICKUP_RANGE = 5.0D;

	public ItemMarisaHat()
	{
		super(THKaguyaArmorMaterials.MARISA, ArmorItem.Type.HELMET, new net.minecraft.world.item.Item.Properties().durability(MAX_DURABILITY));
	}

	@Override
	public int getMaxDamage(ItemStack stack)
	{
		return MAX_DURABILITY;
	}

	/** 原版 getItemEnchantability = 0 */
	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	/** 原版 isBookEnchantable = false */
	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return false;
	}

	/** 原版 onArmorTick：5 格内のドロップアイテムを拾得判定に載せる（1.21.1 は inventoryTick で代替） */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if (level.isClientSide || !(entity instanceof Player player) || !isWorn(player, stack))
		{
			return;
		}
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
				player.getBoundingBox().inflate(PICKUP_RANGE, PICKUP_RANGE, PICKUP_RANGE)))
		{
			item.playerTouch(player);
		}
	}


	/** 原版 getArmorModel：只有头盔槽用自定义模型 ModelMarisaHat（scale 0.4F） */
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer)
	{
		consumer.accept(new IClientItemExtensions()
		{
			private ModelMarisaHat model;

			@Override
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
					HumanoidModel<?> original)
			{
				if(slot == EquipmentSlot.HEAD)
				{
					if(model == null)
					{
						model = new ModelMarisaHat(Minecraft.getInstance().getEntityModels().bakeLayer(ModelMarisaHat.LAYER));
					}
					return model;
				}
				return original;
			}
		});
	}
	private static boolean isWorn(Player player, ItemStack stack)
	{
		for (ItemStack armor : player.getInventory().armor)
		{
			if (armor == stack)
			{
				return true;
			}
		}
		return false;
	}
}
