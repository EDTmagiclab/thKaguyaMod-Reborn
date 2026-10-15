package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntityMazinkyoukan;

/** アイテム　魔人経巻 */
public class ItemMazinKyoukan extends Item
{
	public ItemMazinKyoukan()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);

		EntityMazinkyoukan mazinkyoukan = new EntityMazinkyoukan(level, player);
		if(!level.isClientSide)
		{
			level.addFreshEntity(mazinkyoukan);
		}
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BLOCK;
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}
}
