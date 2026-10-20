package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntitySpiritualStrikeTalisman;

/** 霊撃札 */
public class ItemSpiritualStrikeTalisman extends Item
{
	public ItemSpiritualStrikeTalisman()
	{
		super(new Item.Properties().stacksTo(16));
	}

	/** 右クリックで霊撃札を発射する */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		EntitySpiritualStrikeTalisman talisman = new EntitySpiritualStrikeTalisman(level, player);

		if(!level.isClientSide)
		{
			level.addFreshEntity(talisman);
		}
		// 原版 "random.fizz"
		level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2.0F, 12.0F);

		if(!player.isCreative())
		{
			itemStack.shrink(1);
		}

		return InteractionResultHolder.success(itemStack);
	}
}
