package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntityKinkakuzi;

/** 金閣寺の一枚天井を投げつけるアイテム */
public class ItemKinkakuzi extends Item
{
	public ItemKinkakuzi()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		double pow = 0.7D;
		if(player.isShiftKeyDown())
		{
			pow = 1.0D;
		}

		// 使用者の上方向に発射
		EntityKinkakuzi entityKinkakuzi = new EntityKinkakuzi(level, player.getX(), player.getY() + player.getBbHeight(), player.getZ(), player, pow);
		// 原版 "random.drr"
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

		if(!level.isClientSide)
		{
			level.addFreshEntity(entityKinkakuzi);
		}
		itemStack.shrink(1);

		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}
}
