package thKaguyaMod.item;

import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntityCursedDecoyDoll;

/** 呪いのデコイ人形 */
public class ItemCursedDecoyDoll extends Item
{
	public ItemCursedDecoyDoll()
	{
		super(new Item.Properties().stacksTo(1).durability(30));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 周囲にすでに呪いのデコイ人形が出現している場合
		List<Entity> list = level.getEntities(player, player.getBoundingBox().inflate(40.0D, 40.0D, 40.0D));
		for(Entity entity : list)
		{
			if(entity instanceof EntityCursedDecoyDoll)
			{
				return InteractionResultHolder.fail(itemStack);
			}
		}

		EntityCursedDecoyDoll decoy = new EntityCursedDecoyDoll(level, player);
		if(!level.isClientSide)
		{
			level.addFreshEntity(decoy);
		}

		itemStack.setDamageValue(itemStack.getDamageValue() + 1);
		if(itemStack.getDamageValue() >= itemStack.getMaxDamage())
		{
			// TODO(阶段X): 原版在这里把诅咒人偶恢复成被诅咒前的原始物品（依赖 1.7 的 Item ID 系统）
			itemStack.shrink(1);
		}

		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}
}
