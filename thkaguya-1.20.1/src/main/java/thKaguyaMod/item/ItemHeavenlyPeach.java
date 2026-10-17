package thKaguyaMod.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 天界の桃（Heavenly Peach）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemHeavenlyPeach.java（36 行，extends ItemFood）：
 * ・ItemFood(2, true)、maxStackSize = 10、setAlwaysEdible
 * ・onFoodEaten：PotionEffect(11 = 耐性, 20*20 tick, 2)
 *
 * 注意：原版第三引数は「effect level」ではなく <b>amplifier</b> なので、2 ＝耐性 <b>III</b>。
 * （移植計画時に「耐性 II」と書いていたのは誤り。原版コードに合わせて amplifier 2 とした）
 */
public class ItemHeavenlyPeach extends Item
{
	/** 原版 effectTime = 20 * 20 */
	public static final int EFFECT_DURATION = 400;
	/** 原版 PotionEffect(11, 400, 2) の第 3 引数（amplifier） */
	public static final int EFFECT_AMPLIFIER = 2;

	public ItemHeavenlyPeach()
	{
		super(new Item.Properties()
				.stacksTo(10)
				.food(new FoodProperties.Builder()
						.nutrition(2)
						.saturationMod(0.6F)
						.alwaysEat()
						.build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living)
	{
		ItemStack result = super.finishUsingItem(stack, level, living);

		if (!level.isClientSide)
		{
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, EFFECT_AMPLIFIER));
		}
		return result;
	}
}
