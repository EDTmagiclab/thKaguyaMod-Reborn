package thKaguyaMod.init;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 本模组的盔甲材质（原版 1.7.10 用 {@code EnumHelper.addArmorMaterial} 注册了 3 种）。
 *
 * 原版定义（extract/sources/java/thKaguyaMod/init/THKaguyaItems.java:375-379）：
 * <pre>
 * armorMaterial_Hinezumi = EnumHelper.addArmorMaterial("HINEZUMI", 1, new int[]{1, 2, 2, 2}, 0);
 * armorMaterial_Marisa   = EnumHelper.addArmorMaterial("MARISA",   1, new int[]{1, 1, 1, 1}, 0);
 * armorMaterial_Suwako   = EnumHelper.addArmorMaterial("SUWAKO",   1, new int[]{1, 1, 1, 1}, 0);
 * </pre>
 * 数组顺序是 {靴, 脚, 胴, 兜}。1.7.10 的耐久 = 基值 × 系数，只有「火鼠の皮衣」(ItemFireRatRobe)
 * 自己 {@code setMaxDamage(0)} 改成无限耐久；「洩矢の帽子」(ItemKerobou) 自己 {@code setMaxDamage(620)}。
 *
 * 1.20.1 的 {@link ArmorMaterial} 是接口，耐久/防御都按 {@link ArmorItem.Type} 单独给，所以这里按类型存 4 份。
 * 耐久给 0 表示「不可损坏」（原版 setMaxDamage(0)）。
 */
public final class THKaguyaArmorMaterials
{
	private THKaguyaArmorMaterials()
	{
	}

	/** 火鼠の皮衣用（无限耐久，防御 1/2/2/2） */
	public static final ArmorMaterial HINEZUMI = new Simple("hinezumi",
			0, 0, 0, 0,
			1, 2, 2, 2,
			0, SoundEvents.ARMOR_EQUIP_LEATHER);

	/** 洩矢の帽子用（只有头盔有耐久 620，防御 1/1/1/1） */
	public static final ArmorMaterial SUWAKO = new Simple("suwako",
			620, 0, 0, 0,
			1, 1, 1, 1,
			0, SoundEvents.ARMOR_EQUIP_LEATHER);

	/** 魔理沙の帽子用（原版未移植，先备好） */
	public static final ArmorMaterial MARISA = new Simple("marisa",
			0, 0, 0, 0,
			1, 1, 1, 1,
			0, SoundEvents.ARMOR_EQUIP_LEATHER);

	/** 轻量实现：按 type 返回对应的耐久与防御 */
	private record Simple(String name,
			int helmetDurability, int chestDurability, int legsDurability, int bootsDurability,
			int helmetDefense, int chestDefense, int legsDefense, int bootsDefense,
			int enchantmentValue, SoundEvent equipSound) implements ArmorMaterial
	{
		@Override
		public int getDurabilityForType(ArmorItem.Type type)
		{
			return switch(type)
			{
				case HELMET -> helmetDurability;
				case CHESTPLATE -> chestDurability;
				case LEGGINGS -> legsDurability;
				case BOOTS -> bootsDurability;
			};
		}

		@Override
		public int getDefenseForType(ArmorItem.Type type)
		{
			return switch(type)
			{
				case HELMET -> helmetDefense;
				case CHESTPLATE -> chestDefense;
				case LEGGINGS -> legsDefense;
				case BOOTS -> bootsDefense;
			};
		}

		@Override
		public int getEnchantmentValue()
		{
			return enchantmentValue;
		}

		@Override
		public SoundEvent getEquipSound()
		{
			return equipSound;
		}

		@Override
		public Ingredient getRepairIngredient()
		{
			return Ingredient.EMPTY;
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public float getToughness()
		{
			return 0.0F;
		}

		@Override
		public float getKnockbackResistance()
		{
			return 0.0F;
		}
	}
}
