package thKaguyaMod.item;

import thKaguyaMod.THStackData;
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
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.entity.item.EntityHisou;
import thKaguyaMod.init.THKaguyaItems;

/** 緋想の剣 */
public class ItemHisou extends SwordItem
{
	/** 原版 ToolMaterial "hisou" の maxUses = 200 */
	public static final int MAX_DAMAGE = 200;

	public ItemHisou()
	{
		super(Tiers.IRON, (new Properties().stacksTo(1).durability(MAX_DAMAGE)).attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)));
	}

	@Override
	public int getMaxDamage(ItemStack stack)
	{
		return MAX_DAMAGE;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity living_hit, LivingEntity living_user)
	{
		CompoundTag nbt = THStackData.get(itemStack);

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
			THStackData.set(itemStack, nbt);
		}

		nbt.putString("EntityName", living_hit.getType().getDescriptionId());

		itemStack.hurtAndBreak(1, living_user, net.minecraft.world.entity.EquipmentSlot.MAINHAND);

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
			// 原版：先宣言符卡「全人類の緋想天」（符卡 No 7 = Zenzinrui_no_Hisouten，难度 Normal），
			// 宣言失败（非创造模式下 bomb_item + 经验等级不足 / 早苗卡未充能 / 视线内无目标 /
			// 已有同使用者的 EntitySpellCard）则不发射。
			if(!THKaguyaLib.checkSpellCardDeclaration(level, itemStack, player, 7, 2, true))
			{
				return InteractionResultHolder.success(itemStack);
			}

			// 緋想の剣本体を出現させる（原版：damage に現在の損傷値を渡す）
			EntityHisou hisou_0 = new EntityHisou(level, player, null, 7 + 1, itemStack.getDamageValue());
			level.addFreshEntity(hisou_0);
			// 1.21.1: 剣は EntityHisou 側の itemEffectFinish で返却されるが、クリエイティブでは
			// その実装が「そのまま消滅」なので手元から消えたままになる。
			// 1.20.1 では ServerPlayerGameMode がスロットを再同期しなかったため
			// 「手に残っている」ように見えていた（実体は既に消費済み）ので、
			// 両バージョンで「クリエイティブでは消費しない」に統一する。
			if(!player.isCreative())
			{
				itemStack.shrink(1);
			}

			// 残像を 7 つ出現させる
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
	public void appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext context, List<Component> list, TooltipFlag flag)
	{
		super.appendHoverText(itemStack, context, list, flag);

		CompoundTag nbt = THStackData.get(itemStack);
		if(nbt == null)
		{
			return;
		}
		String entityName = nbt.getString("EntityName");
		list.add(Component.translatable("thKaguya.item.kishitu").append(" : ").append(Component.translatable(entityName)));
	}

	/** 旧存档：载入时把 1.20.1 的 tag 从 {@code minecraft:custom_data} 搬进 {@code thkaguya:stack_tag}。 */
	@Override
	public void verifyComponentsAfterLoad(ItemStack stack)
	{
		THStackData.migrate(stack);
	}
}
