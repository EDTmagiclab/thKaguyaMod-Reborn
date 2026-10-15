package thKaguyaMod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntityPendulum;

/** ナズーリンペンデュラム */
public class ItemPendulum extends Item
{
	public ItemPendulum()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		ItemStack searchItem = player.getInventory().items.get(player.getInventory().selected + 1);

		// 右にアイテムがないなら失敗
		if(searchItem.isEmpty())
		{
			return InteractionResultHolder.fail(itemStack);
		}
		// ブロックでないならサーチ不能
		if(!(searchItem.getItem() instanceof BlockItem))
		{
			return InteractionResultHolder.fail(itemStack);
		}
		if(player.getInventory().selected >= 8)
		{
			return InteractionResultHolder.fail(itemStack);
		}

		int mode = 0;
		if(player.isShiftKeyDown())
		{
			mode = 1;
		}

		String name = Component.translatable(searchItem.getDescriptionId()).getString();
		EntityPendulum entityPendulum = new EntityPendulum(level, player, mode, name, searchItem);
		// 原版 "random.orb"
		level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.9F);
		if(!level.isClientSide)
		{
			level.addFreshEntity(entityPendulum);
			itemStack.shrink(1);
		}

		// 使用時のメッセージ
		String str;
		if(mode == 0)
		{
			str = Component.translatable("thKaguya.msg.pendulum1").getString() + name + Component.translatable("thKaguya.msg.pendulum2").getString();
		}
		else
		{
			str = Component.translatable("thKaguya.msg.pendulum1").getString() + name + Component.translatable("thKaguya.msg.pendulum3").getString();
		}
		if(!level.isClientSide)
		{
			player.sendSystemMessage(Component.literal(str));
		}

		return InteractionResultHolder.success(itemStack);
	}
}
