package thKaguyaMod.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.THStackData;

/** 咲夜の懐中時計 */
public class ItemSakuyaWatch extends Item
{
	public ItemSakuyaWatch()
	{
		super(new Item.Properties().stacksTo(1));
	}

	// NBT に mode を保存（0=通常, 1=高感度）
	private static int getMode(ItemStack stack)
	{
		CompoundTag tag = THStackData.get(stack);
		return tag == null ? 0 : tag.getInt("mode");
	}

	private static void setMode(ItemStack stack, int mode)
	{
		CompoundTag tag = THStackData.getOrCreate(stack);
		tag.putInt("mode", mode);
		THStackData.set(stack, tag);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// スニークでモード切り替え
		if(player.isShiftKeyDown() && player.tickCount % 2 == 0)
		{
			setMode(itemStack, getMode(itemStack) == 0 ? 1 : 0);
			return InteractionResultHolder.success(itemStack);
		}

		// クリエイティブの場合は即時発動
		if(player.isCreative())
		{
			finishUsingItem(itemStack, level, player);
			return InteractionResultHolder.success(itemStack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity)
	{
		if(!(entity instanceof Player player))
		{
			return itemStack;
		}

		int usedTime = getUseDuration(itemStack, player) - player.getUseItemRemainingTicks();
		int mode = EntitySakuyaWatch.TIME_HALF_WITH_LIMIT;

		// 空腹でないか、クリエイティブなら使用可能
		if(player.getFoodData().getFoodLevel() > 0 || player.isCreative())
		{
			if(player.isCreative())
			{
				// 原版代码与自身注释矛盾（注释「通常は停止モード」vs 代码新表=减速）。
				// 按注释意图取正：新表=时间停止（TIME_STOP），潜行切换后=时间减速（TIME_HALF）。
				// 这也是本迁移版的实测体验修复：减速 1/2 几乎不可感知，默认应给戏剧性的时停。
				mode = getMode(itemStack) == 0 ? EntitySakuyaWatch.TIME_STOP : EntitySakuyaWatch.TIME_HALF;
			}
			else if(usedTime >= getUseDuration(itemStack, player))
			{
				// 与创造模式同步：mode 0=时停（默认），mode 1=减速
				mode = getMode(itemStack) == 0 ? EntitySakuyaWatch.TIME_STOP_WITH_LIMIT : EntitySakuyaWatch.TIME_HALF_WITH_LIMIT;
			}
			else
			{
				if(player.isShiftKeyDown())
				{
					setMode(itemStack, getMode(itemStack) == 0 ? 1 : 0);
				}
				return itemStack;
			}

			// 周囲にすでに咲夜の懐中時計が出現している場合
			List<Entity> list = level.getEntities(player, player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
			for(Entity e : list)
			{
				if(e instanceof EntitySakuyaWatch)
				{
					return itemStack;
				}
			}

			// 原版 "random.click"
			level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

			EntitySakuyaWatch entityPrivateSquare = new EntitySakuyaWatch(level, player, mode);
			if(!level.isClientSide)
			{
				level.addFreshEntity(entityPrivateSquare);
			}
			if(!player.isCreative())
			{
				itemStack.shrink(1);
			}
		}
		return itemStack;
	}

	@Override
	public int getUseDuration(ItemStack itemStack, LivingEntity entity)
	{
		return getMode(itemStack) == 0 ? 20 : 48;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BOW;
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return getMode(itemStack) != 0;
	}
}
