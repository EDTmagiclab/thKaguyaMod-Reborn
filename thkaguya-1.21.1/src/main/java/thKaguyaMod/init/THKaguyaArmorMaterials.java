package thKaguyaMod.init;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import thKaguyaMod.THKaguyaCore;

/**
 * 本模组的盔甲材质（原版 1.7.10 用 {@code EnumHelper.addArmorMaterial} 注册了 3 种）。
 *
 * 原版定义（extract/sources/java/thKaguyaMod/init/THKaguyaItems.java:375-379）：
 * <pre>
 * armorMaterial_Hinezumi = EnumHelper.addArmorMaterial("HINEZUMI", 1, new int[]{1, 2, 2, 2}, 0);
 * armorMaterial_Marisa   = EnumHelper.addArmorMaterial("MARISA",   1, new int[]{1, 1, 1, 1}, 0);
 * armorMaterial_Suwako   = EnumHelper.addArmorMaterial("SUWAKO",   1, new int[]{1, 1, 1, 1}, 0);
 * </pre>
 * 数组顺序是 {靴, 脚, 胴, 兜}。
 *
 * <p>1.21.1 的 {@link ArmorMaterial} 是 <b>record</b>，不再承载耐久（耐久改由
 * {@code Item.Properties#durability} 给出），防御按 {@link ArmorItem.Type} 存 Map。
 * 这里用 {@link Holder#direct} 把 3 个实例包成 {@code Holder<ArmorMaterial>}，
 * 因为 {@link ArmorItem} 的构造器要求 {@code Holder<ArmorMaterial>}。</p>
 */
public final class THKaguyaArmorMaterials
{
	private THKaguyaArmorMaterials()
	{
	}

	/** 火鼠の皮衣用（无限耐久，防御 兜2/胴2/脚2/靴1） */
	public static final Holder<ArmorMaterial> HINEZUMI = create("hinezumi",
			Map.of(
					ArmorItem.Type.HELMET, 2,
					ArmorItem.Type.CHESTPLATE, 2,
					ArmorItem.Type.LEGGINGS, 2,
					ArmorItem.Type.BOOTS, 1));

	/** 洩矢の帽子用（头盔耐久 620，防御 1/1/1/1） */
	public static final Holder<ArmorMaterial> SUWAKO = create("suwako_hat",
			Map.of(
					ArmorItem.Type.HELMET, 1,
					ArmorItem.Type.CHESTPLATE, 1,
					ArmorItem.Type.LEGGINGS, 1,
					ArmorItem.Type.BOOTS, 1));

	/** 魔理沙の帽子用（原版未移植，先备好） */
	public static final Holder<ArmorMaterial> MARISA = create("marisa_hat",
			Map.of(
					ArmorItem.Type.HELMET, 1,
					ArmorItem.Type.CHESTPLATE, 1,
					ArmorItem.Type.LEGGINGS, 1,
					ArmorItem.Type.BOOTS, 1));

	private static Holder<ArmorMaterial> create(String name, Map<ArmorItem.Type, Integer> defense)
	{
		Supplier<Ingredient> repair = () -> Ingredient.EMPTY;
		List<ArmorMaterial.Layer> layers = List.of(
				new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, name)));
		return Holder.direct(new ArmorMaterial(defense, 0, SoundEvents.ARMOR_EQUIP_LEATHER, repair, layers, 0.0F, 0.0F));
	}
}
