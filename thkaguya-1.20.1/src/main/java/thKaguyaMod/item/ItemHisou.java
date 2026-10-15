package thKaguyaMod.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thKaguyaMod.entity.item.EntityHisou;
import thKaguyaMod.init.THKaguyaItems;

/** 緋想の剣 */
public class ItemHisou extends SwordItem
{
	public ItemHisou()
	{
		super(Tiers.IRON, 3, -2.4F, new Properties().stacksTo(1));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity living_hit, LivingEntity living_user)
	{
		CompoundTag nbt = itemStack.getTag();

		if(nbt != null)
		{
			String living_hit_name = living_hit.getType().getDescriptionId();
			if(living_hit_name.equals(nbt.getString("EntityName")))
			{
				// 通常の２倍のダメージ
				living_hit.hurt(living_hit.level().damageSources().mobAttack(living_user), 10F);
				if(living_user instanceof Player player)
				{
					player.magicCrit(living_hit);
				}
			}
		}
		else
		{
			nbt = new CompoundTag();
			itemStack.setTag(nbt);
		}

		nbt.putString("EntityName", living_hit.getType().getDescriptionId());

		itemStack.hurtAndBreak(1, living_user, e -> {});

		return true;
	}

	@Override
	public boolean mineBlock(ItemStack itemStack, Level level, BlockState state, BlockPos pos, LivingEntity living)
	{
		if(state.is(BlockTags.LEAVES))
		{
			if(living.getRandom().nextInt(25) == 1 && pos.getY() > 128)
			{
				// 天界の桃を出現させる
				ItemEntity entityItem = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(THKaguyaItems.HEAVENLY_PEACH.get()));
				if(!level.isClientSide)
				{
					level.addFreshEntity(entityItem);
				}
			}
			return true;
		}
		return super.mineBlock(itemStack, level, state, pos, living);
	}

	@Override
	public float getDestroySpeed(ItemStack itemStack, BlockState state)
	{
		if(state.is(BlockTags.LEAVES))
		{
			return 15F;
		}
		else if(state.is(BlockTags.WOOL))
		{
			return 10F;
		}
		return super.getDestroySpeed(itemStack, state);
	}

	@Override
	public boolean isFoil(ItemStack itemstack)
	{
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		if(!level.isClientSide)
		{
			// TODO(阶段3d-5): 原版先 THKaguyaLib.checkSpellCardDeclaration 宣言「全人類の緋想天」（依赖符卡系统），此处直接生成
			EntityHisou hisou_0 = new EntityHisou(level, player, null, 8, 0);
			level.addFreshEntity(hisou_0);
			itemStack.shrink(1);

			for(int i = 0; i < 7; i++)
			{
				EntityHisou hisou_1 = new EntityHisou(level, player, hisou_0, i + 1, 0);
				level.addFreshEntity(hisou_1);
			}
		}
		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	@Override
	public void appendHoverText(ItemStack itemStack, Level level, List<Component> list, TooltipFlag flag)
	{
		super.appendHoverText(itemStack, level, list, flag);

		CompoundTag nbt = itemStack.getTag();
		if(nbt == null)
		{
			return;
		}
		String entityName = nbt.getString("EntityName");
		list.add(Component.translatable("thKaguya.item.kishitu").append(" : ").append(Component.translatable(entityName)));
	}
}
