package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 子安貝（Swallow's Cowrie Shell）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemSwallowCowrieShell.java（80 行，extends ItemFood）：
 * ・ItemFood(0, false)、maxStackSize = 1、setAlwaysEdible、hasEffect = true
 * ・onEaten：経験レベルを「性能」として消費する
 *   - 潜行していない：performance = (int)((20 - 空腹度) / 2.0 + 0.5)、0〜自分のレベルにクランプ
 *   - 潜行している：performance = 自分の経験レベル
 *   - どちらも最大 10
 * ・addExperienceLevel(-performance)、空腹を performance*2 回復、隠し満腹度 5.0F、
 *   更に PotionEffect(10 = 再生, performance*3*20 tick, amplifier 1) ＝再生 II
 *
 * 1.20.1 対応：
 * ・ItemFood → Item.Properties().food(FoodProperties)
 * ・onEaten → finishUsingItem（空腹回復と XP 消費は自前で行い、最後に super で個数消費と統計を任せる）
 */
public class ItemSwallowCowrieShell extends Item
{
	/** 消費レベル上限（原版 if(performance > 10) performance = 10） */
	public static final int MAX_LEVEL_COST = 10;
	/** 隠し満腹度（原版 addStats の第 2 引数 5F） */
	public static final float SATURATION = 5.0F;

	public ItemSwallowCowrieShell()
	{
		super(new Item.Properties()
				.stacksTo(1)
				.food(new FoodProperties.Builder()
						.nutrition(0)
						.saturationMod(0.0F)
						.alwaysEat()
						.build()));
	}

	/** 原版 hasEffect = true */
	@Override
	public boolean isFoil(ItemStack stack)
	{
		return true;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living)
	{
		if (living instanceof Player player)
		{
			int performance = player.experienceLevel;

			if (!player.isShiftKeyDown())
			{
				// 原版：回復しきれる分だけレベルを消費させる
				performance = (int) ((20 - player.getFoodData().getFoodLevel()) / 2.0D + 0.5D);
				if (performance < 0)
				{
					performance = 0;
				}
				else if (performance > player.experienceLevel)
				{
					performance = player.experienceLevel;
				}
			}
			if (performance > MAX_LEVEL_COST)
			{
				performance = MAX_LEVEL_COST;
			}

			player.giveExperienceLevels(-performance);
			player.getFoodData().eat(performance * 2, SATURATION);

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_BURP, SoundSource.PLAYERS,
					0.5F, level.random.nextFloat() * 0.1F + 0.9F);

			if (!level.isClientSide)
			{
				// 原版 PotionEffect(10, performance*3*20, 1) = 再生 II を performance 秒×3
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, performance * 3 * 20, 1));
			}
		}

		// 個数消費・統計・FoodProperties の効果処理は既定実装に任せる
		return super.finishUsingItem(stack, level, living);
	}
}
