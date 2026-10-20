package thKaguyaMod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntitySakuyaStopWatch;
import thKaguyaMod.entity.item.EntitySakuyaWatch;

/** ストップウォッチ（咲夜の時止め空間を作る） */
public class ItemSakuyaStopWatch extends Item
{
	public ItemSakuyaStopWatch()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 周囲にすでに時計系のEntityがいるなら使用失敗
		List<Entity> list = level.getEntities(player, player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
		for(Entity entity : list)
		{
			if(entity instanceof EntitySakuyaWatch || entity instanceof EntitySakuyaStopWatch)
			{
				return InteractionResultHolder.fail(itemStack);
			}
		}

		// 原版 "random.click"
		level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

		EntitySakuyaStopWatch stopWatch = new EntitySakuyaStopWatch(level, player);
		if(!level.isClientSide)
		{
			level.addFreshEntity(stopWatch);// 時間停止空間を生み出す
		}

		if(!player.isCreative())
		{
			itemStack.shrink(1);
		}

		return InteractionResultHolder.success(itemStack);
	}
}
