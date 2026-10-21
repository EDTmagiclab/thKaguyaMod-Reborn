package thKaguyaMod.item;

import thKaguyaMod.THStackData;
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
			// 原版 ItemCursedDecoyDoll.java:62-77：耐久が尽きたら「呪われる前のアイテム」に戻す。
			// OriginalItemID / OriginalDamage は万宝槌の作業台（ContainerMiracleMallet）が書き込む。
			int originalDamage = 0;
			int originID = -1;
			if(THStackData.has(itemStack))
			{
				originalDamage = THStackData.get(itemStack).getInt("OriginalDamage");
				originID = THStackData.get(itemStack).getInt("OriginalItemID");
			}

			Item originalItem = originID >= 0
					? net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(originID)
					: null;
			if(originalItem != null && originalItem != net.minecraft.world.item.Items.AIR)
			{
				ItemStack restored = new ItemStack(originalItem);
				restored.setDamageValue(originalDamage);
				return InteractionResultHolder.success(restored);
			}

			// NBT が無い（万宝槌を経由していない）場合は原版同様に消滅させる
			itemStack.shrink(1);
		}

		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}

	/** 旧存档：载入时把 1.20.1 的 tag 从 {@code minecraft:custom_data} 搬进 {@code thkaguya:stack_tag}。 */
	@Override
	public void verifyComponentsAfterLoad(ItemStack stack)
	{
		THStackData.migrate(stack);
	}
}
