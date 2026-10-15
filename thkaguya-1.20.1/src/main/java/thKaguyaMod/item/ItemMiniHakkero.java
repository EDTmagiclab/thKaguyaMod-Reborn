package thKaguyaMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntityMiniHakkero;
import thKaguyaMod.init.THKaguyaConfig;

/** ミニ八卦炉（マスタースパーク） */
public class ItemMiniHakkero extends Item
{
	public ItemMiniHakkero()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// マスタースパークの使用を許可しない設定なら何も起きない
		if(!THKaguyaConfig.UsingMasterSpark)
		{
			return InteractionResultHolder.pass(itemStack);
		}

		// ミニ八卦炉の設置地点にブロックがないことを確認
		BlockPos pos = new BlockPos(
				(int)(player.getX() - Math.sin(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 1.3D),
				(int)(player.getY() - Math.sin(player.getXRot() / 180F * (float)Math.PI) * 1.3D + player.getEyeHeight()),
				(int)(player.getZ() + Math.cos(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 1.3D));

		if(!level.getBlockState(pos).isRedstoneConductor(level, pos))
		{
			// ファイアーチャージを所持していないなら使用できない
			if(!player.getInventory().contains(new ItemStack(Items.FIRE_CHARGE)))
			{
				return InteractionResultHolder.pass(itemStack);
			}

			int count = 0;
			for(ItemStack invStack : player.getInventory().items)
			{
				if(invStack.getItem() == Items.FIRE_CHARGE)
				{
					count += invStack.getCount();
				}
			}

			// ファイアーチャージを３２個以上持っているなら
			if(count >= 32)
			{
				int remaining = 32;
				for(ItemStack invStack : player.getInventory().items)
				{
					if(invStack.getItem() == Items.FIRE_CHARGE && remaining > 0)
					{
						int take = Math.min(remaining, invStack.getCount());
						invStack.shrink(take);
						remaining -= take;
					}
				}
			}
			else
			{
				return InteractionResultHolder.pass(itemStack);
			}

			// ミニ八卦炉を目の前に出現させる
			EntityMiniHakkero miniHakkero = new EntityMiniHakkero(level, player, 0);
			if(!level.isClientSide)
			{
				level.addFreshEntity(miniHakkero);
				itemStack.shrink(1);
			}
		}

		return InteractionResultHolder.success(itemStack);
	}
}
