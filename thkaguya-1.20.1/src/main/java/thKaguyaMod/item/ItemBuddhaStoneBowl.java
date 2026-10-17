package thKaguyaMod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 仏の御石の鉢（Buddha's Stone Bowl）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemBuddhaStoneBowl.java（89 行，extends ItemTool）：
 * ・石の複合ツール（剣/ツルハシ/スコップ/斧）、ダメージ 4.0F、耐久 = ToolMaterial.STONE = 131
 * ・getDigSpeed 恒常 4F、func_150897_b：岩/鉄/鉄鎧（anvil）だけ採掘可、
 *   金/ダイヤ/エメラルド/ラピス/レッドストーン/黒曜石は（たとえメタル素材でも）採掘不可
 * ・getItemEnchantability = 0（本でエンチャント不可）、hasEffect = true（常に発光）
 *
 * 1.20.1 対応：
 * ・ItemTool 相当が DiggerItem（Tier 必須）なので、属性と採掘判定は自前で与える
 *   （採掘可否は BlockTags.MINEABLE_WITH_PICKAXE から「原版で禁止されているブロック」を除外）
 * ・攻撃力 4.0F は Attributes.ATTACK_DAMAGE の ADDITION +3.0F（素手 1.0F と合算）
 */
public class ItemBuddhaStoneBowl extends Item
{
	/** 原版 ToolMaterial.STONE.getMaxUses() */
	public static final int MAX_DURABILITY = 131;
	/** 原版 super(4.0F, toolMaterial, null) のダメージ */
	public static final double ATTACK_DAMAGE = 4.0D;

	public ItemBuddhaStoneBowl()
	{
		super(new Item.Properties().stacksTo(1).durability(MAX_DURABILITY));
	}

	/** 原版 hasEffect = true：常にエンチャントの輝き */
	@Override
	public boolean isFoil(ItemStack stack)
	{
		return true;
	}

	/** 原版 getItemEnchantability = 0 */
	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	/** 原版 isBookEnchantable = false */
	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return false;
	}

	/** 原版 getDigSpeed：可採掘なら 4F、それ以外は 1F */
	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state)
	{
		return isHarvestable(state) ? 4.0F : 1.0F;
	}

	/** 原版 func_150897_b の採掘判定 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)
	{
		return isHarvestable(state);
	}

	/**
	 * 原版 func_150897_b をそのまま移植：
	 * 岩質（= 1.20.1 の pickaxe 採掘対象）で、かつ金/ダイヤ/エメラルド/ラピス/レッドストーン/黒曜石ではない。
	 */
	private static boolean isHarvestable(BlockState state)
	{
		if (state.is(Blocks.OBSIDIAN)
				|| state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
				|| state.is(Blocks.DIAMOND_BLOCK)
				|| state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
				|| state.is(Blocks.EMERALD_BLOCK)
				|| state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)
				|| state.is(Blocks.GOLD_BLOCK)
				|| state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
				|| state.is(Blocks.LAPIS_BLOCK)
				|| state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE))
		{
			return false;
		}
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	/** 原版 ItemTool：攻撃力 4.0F、攻撃速度は剣と同じ 1.6（-2.4 補正） */
	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot)
	{
		if (slot == EquipmentSlot.MAINHAND)
		{
			ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
			builder.put(Attributes.ATTACK_DAMAGE,
					new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
							ATTACK_DAMAGE - 1.0D, AttributeModifier.Operation.ADDITION));
			builder.put(Attributes.ATTACK_SPEED,
					new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
							-2.4D, AttributeModifier.Operation.ADDITION));
			return builder.build();
		}
		return super.getDefaultAttributeModifiers(slot);
	}

	/** 原版 hitEntity：耐久を 1 消費して true（攻撃は通る） */
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
	{
		stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}
}
