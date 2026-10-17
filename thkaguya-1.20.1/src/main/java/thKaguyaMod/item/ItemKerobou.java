package thKaguyaMod.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thKaguyaMod.client.model.item.ModelSuwakoHat;
import thKaguyaMod.init.THKaguyaArmorMaterials;

import java.util.function.Consumer;

/**
 * 洩矢の帽子（Suwako's Hat）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemKerobou.java（115 行，extends ItemArmor）：
 * ・setMaxDamage(620)，材质 SUWAKO，type=0 即头盔
 * ・getArmorTexture → thkaguyamod:textures/armor/SuwakoHatTexture.png（不分内外层）
 * ・onUpdate / onArmorTick → 没有附魔就自动补荆棘 X（thorns 10）
 * ・getItemEnchantability = 0，isBookEnchantable = false
 * ・getArmorModel → 只有头盔槽返回 new ModelSuwakoHat(0.4F, yOffset, 64, 32)
 * ・isFull3D = true（1.20.1 无对应，手持外观由 model json 决定）
 *
 * 1.20.1 对应：
 * ・getArmorModel → IClientItemExtensions#getHumanoidArmorModel（配合 ModelSuwakoHat，注册在 ClientRenderRegistry）
 * ・onUpdate → inventoryTick；onArmorTick 仍在 IForgeItem 上
 * ・addEnchantment(thorns, 10) → stack.enchant(Enchantments.THORNS, 10)
 */
public class ItemKerobou extends ArmorItem
{
	public ItemKerobou()
	{
		super(THKaguyaArmorMaterials.SUWAKO, ArmorItem.Type.HELMET, new Properties());
	}

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type)
	{
		return "thkaguya:textures/models/armor/suwako_hat_layer_1.png";
	}

	/** 原版 onUpdate：在背包里也补荆棘 X */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if(!level.isClientSide)
		{
			grantThorns(stack);
		}
	}

	/** 原版 onArmorTick：穿在身上也补荆棘 X */
	@Override
	public void onArmorTick(ItemStack stack, Level level, Player player)
	{
		if(!level.isClientSide)
		{
			grantThorns(stack);
		}
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

	/**
	 * 原版：if(!itemStack.isItemEnchanted()) itemStack.addEnchantment(Enchantment.thorns, 10);
	 * 1.20.1 的 ThornsEnchantment 触发概率 = 15% × 等级，10 级恒真（与原版一致）。
	 */
	private static void grantThorns(ItemStack stack)
	{
		if(!stack.isEnchanted())
		{
			stack.enchant(Enchantments.THORNS, 10);
		}
	}

	/** 原版 getArmorModel：只有头盔槽用自定义模型 */
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer)
	{
		consumer.accept(new IClientItemExtensions()
		{
			private ModelSuwakoHat model;

			@Override
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
					HumanoidModel<?> original)
			{
				if(slot == EquipmentSlot.HEAD)
				{
					if(model == null)
					{
						model = new ModelSuwakoHat(Minecraft.getInstance().getEntityModels().bakeLayer(ModelSuwakoHat.LAYER));
					}
					return model;
				}
				return original;
			}
		});
	}
}
