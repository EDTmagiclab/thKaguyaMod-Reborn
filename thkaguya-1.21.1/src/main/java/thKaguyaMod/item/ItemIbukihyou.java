package thKaguyaMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
						.saturationModifier(0.0F)
						.alwaysEdible()
						.build()));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 原版 getMovingObjectPositionFromPlayer(world, player, true)：含流体的方块射线
		BlockPos waterPos = findWaterSource(level, player);

		if(waterPos != null)
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

		// 空（耐久 >= 6）なら飲めない
		if(itemStack.getDamageValue() >= DRINKABLE_DAMAGE)
		{
			return InteractionResultHolder.fail(itemStack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	/**
	 * 对着方块右键时走的是「使用方块」这条路径，1.20.1 的服务端 {@code ServerPlayerGameMode#useItemOn}
	 * 在 {@code ItemStack.useOn} 返回 PASS 后**不会**再回落到 {@code Item#use}（已用 javap 核对字节码：
	 * 方法末尾直接 `areturn` 返回 useOn 的结果）。客户端在浅水处瞄准水面时，射线因为水不可拾取而
	 * 穿过去打在湖底方块上，于是只走 useOn、根本进不到 {@link #use(Level, Player, InteractionHand)}，
	 * 灌水分支永远不执行——这就是「对着水右键没反应」的真正原因。
	 * 因此这里必须再补一条 useOn 路径：点到水/含水方块，或者点到水下放块的顶面，都算在灌水。
	 */
	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if(player == null || !isWaterTarget(level, player, context))
		{
			return InteractionResult.PASS;
		}

		ItemStack itemStack = context.getItemInHand();
		if(itemStack.getDamageValue() > 0)
		{
			player.swing(context.getHand());
			if(!level.isClientSide)
			{
				itemStack.setDamageValue(itemStack.getDamageValue() - 1);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static boolean isWaterTarget(Level level, Player player, UseOnContext context)
	{
		BlockPos pos = context.getClickedPos();
		if(isWaterSource(level, pos))
		{
			return true;
		}
		// 浅水处：射线穿过水打在湖底方块上时，点到的那个面正上方就是水
		if(isWaterSource(level, pos.relative(context.getClickedFace())))
		{
			return true;
		}
		// 自己正泡在水里
		return isWaterSource(level, BlockPos.containing(player.getEyePosition()));
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
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
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

	/**
	 * 视线内寻找可灌水的水源。
	 *
	 * 原版是 {@code getMovingObjectPositionFromPlayer(world, player, true)} + {@code Blocks.water && metadata == 0}。
	 * 1.20.1 用 {@link #getPlayerPOVHitResult} 走 {@code ClipContext.Block.OUTLINE}：水没有 outline 形状，
	 * 部分情况下（和水草/含水方块/贴在池边的方块混在一起时）射线落点并不是 {@code Blocks.WATER}，
	 * 只看 {@code hit.getBlockPos()} 会漏判 → 表现为「右键水源没反应」。因此：
	 * 1) 先用与桶一致的 clip 结果做流体判定（{@link FluidTags#WATER} + {@code isSource()}）；
	 * 2) 再沿视线每 0.1 格采样兜底，碰到不透光实体方块就停下（避免隔墙灌水）。
	 */
	private static BlockPos findWaterSource(Level level, Player player)
	{
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if(hit.getType() == HitResult.Type.BLOCK && isWaterSource(level, hit.getBlockPos()))
		{
			return hit.getBlockPos();
		}

		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1.0F).normalize();
		double reach = 4.5D;
		for(double d = 0.0D; d <= reach; d += 0.1D)
		{
			BlockPos pos = BlockPos.containing(eye.add(dir.scale(d)));
			if(isWaterSource(level, pos))
			{
				return pos;
			}
			BlockState state = level.getBlockState(pos);
			if(!state.isAir() && !state.getFluidState().is(FluidTags.WATER) && state.isSolidRender(level, pos))
			{
				return null;
			}
		}
		return null;
	}

	/** 1.7.10 的 {@code Blocks.water && metadata == 0} = 水源（含水方块里的水源也算） */
	private static boolean isWaterSource(Level level, BlockPos pos)
	{
		FluidState fluid = level.getFluidState(pos);
		if(fluid.is(FluidTags.WATER))
		{
			return fluid.isSource();
		}
		return level.getBlockState(pos).is(Blocks.WATER);
	}
}
