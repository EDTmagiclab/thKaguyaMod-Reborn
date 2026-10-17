package thKaguyaMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 伊吹瓢（ibuki_gourd，伊吹萃香的酒葫芦）—— 原版 {@code thKaguyaMod.item.ItemIbukihyou}（129 行）
 *
 * 原版行为：
 * - 构造：营养 0、`setAlwaysEdible()`（随时可喝）、`setMaxDamage(11)`、`maxStackSize=1`、`setNoRepair()`
 * - `getMaxItemUseDuration` = 16 tick，`getItemUseAction` = DRINK（耐久 ≥ 6 时改 NONE，即空葫芦喝不了）
 * - `onEaten`（喝干）：潜行时不生效；耐久 {@code < 6} 时 `damageItem(5)`、`addStats(0,0)`、
 *   播 `random.burp`，再 `onFoodEaten` → **力量 15 秒**（1.7.10 Potion 5 = damageBoost）+ **反胃 20 秒 III**（Potion 9 = confusion）
 * - `onItemRightClick`：对着**水源方块**右键 → 耐久 +1（往葫芦里灌水），不喝水；
 *   否则耐久 < 6 才进入饮用状态
 *
 * 注意 1.7.10 的 potion id 与 1.20.1 完全不同：5 是力量（不是治疗）、9 是反胃（不是再生），
 * 原版注释「攻撃力アップ」「ゆらゆら」是对的。
 */
public class ItemIbukihyou extends Item
{
	/** 原版 setMaxDamage(11) */
	public static final int MAX_DAMAGE = 11;
	/** 耐久（damage 值）低于此值才能喝；原版判定是 getItemDamageForDisplay() < 6 */
	public static final int DRINKABLE_DAMAGE = 6;
	/** 每次喝干消耗 5 点耐久 */
	public static final int DRINK_COST = 5;

	public ItemIbukihyou()
	{
		super(new Item.Properties()
				.stacksTo(1)
				.durability(MAX_DAMAGE)
				.food(new FoodProperties.Builder()
						.nutrition(0)
						.saturationMod(0.0F)
						.alwaysEat()
						.build()));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 原版 getMovingObjectPositionFromPlayer(world, player, true)：含流体的方块射线
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

		if(hit.getType() == HitResult.Type.BLOCK)
		{
			BlockPos pos = hit.getBlockPos();
			if(level.getBlockState(pos).is(Blocks.WATER))
			{
				// 水源で給水：耐久を 1 回復する（原版 itemStack.setItemDamage(getItemDamage() - 1)）
				if(itemStack.getDamageValue() > 0)
				{
					player.swing(hand);
					if(!level.isClientSide)
					{
						itemStack.setDamageValue(itemStack.getDamageValue() - 1);
					}
				}
				return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
			}
		}

		// 空（耐久 >= 6）なら飲めない
		if(itemStack.getDamageValue() >= DRINKABLE_DAMAGE)
		{
			return InteractionResultHolder.fail(itemStack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity living)
	{
		if(itemStack.getDamageValue() >= DRINKABLE_DAMAGE)
		{
			return itemStack;
		}

		if(living instanceof Player player)
		{
			// 原版：潜行中は何も起きない
			if(player.isShiftKeyDown())
			{
				return itemStack;
			}

			if(!level.isClientSide)
			{
				itemStack.setDamageValue(Math.min(itemStack.getDamageValue() + DRINK_COST, MAX_DAMAGE));

				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.PLAYER_BURP, SoundSource.PLAYERS,
						0.5F, level.random.nextFloat() * 0.1F + 0.9F);

				// 原版 onFoodEaten：PotionEffect(5, 15*20, 0) = 力量、PotionEffect(9, 20*20, 2) = 反胃 III
				player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 15 * 20, 0));
				player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 20, 2));
			}
		}

		return itemStack;
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 16;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		// 原版：耐久 >= 6 は EnumAction.none（空の葫芦）
		if(itemStack.getDamageValue() >= DRINKABLE_DAMAGE)
		{
			return UseAnim.NONE;
		}
		return UseAnim.DRINK;
	}

	@Override
	public boolean isBarVisible(ItemStack itemStack)
	{
		return itemStack.getDamageValue() > 0;
	}
}
