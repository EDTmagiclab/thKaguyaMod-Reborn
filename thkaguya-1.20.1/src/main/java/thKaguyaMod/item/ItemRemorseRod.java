package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/**
 * 悔悟の棒（悔悟棒）
 *
 * 原版 ItemKaigo extends ItemSword：
 * - damage 値が状態を表す（0＝文字あり＝有効 / 1＝文字なし＝使用済み）
 * - 命中時：相手の体力が最大値の 75% より多い AND damage == 0 なら、
 *   相手の体力を最大値の 75% まで落とし、damage を 1 にする（文字が消える）
 * - 右クリック：黒染料（原版 Items.dye damage 0）を 1 個消費して damage を 0 に戻す
 * - damage == 0 のとき発光（hasEffect）
 *
 * 1.20.1 迁移要点：
 * - metadata 状態は 1.20.1 でも ItemStack の damage 値（NBT）で表現できる
 *   （setDamageValue は 1.20.1 では下限制限のみで、maxDamage 0 の物品でも値を持てる）
 * - Items.dye damage 0（黒）→ Items.BLACK_DYE
 * - hasEffect → isFoil
 * - setItemInUse + getMaxItemUseDuration → startUsingItem（ガードアクション継続）
 */
public class ItemRemorseRod extends SwordItem
{
	public ItemRemorseRod()
	{
		super(Tiers.IRON, 3, -2.4F, new Item.Properties().stacksTo(1));
	}

	/** 命中時：体力 75% 超なら 75% まで落とす（damage 0 のときのみ、1 回限り） */
	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker)
	{
		if((double)target.getMaxHealth() * 0.75D < (double)target.getHealth()
				&& itemStack.getDamageValue() == 0)
		{
			// 当てた相手の体力を 75% にする
			target.setHealth((float)((double)target.getMaxHealth() * 0.75D));
			// 文字が消える
			itemStack.setDamageValue(1);
			return true;
		}
		return false;
	}

	/** 右クリック：黒染料を 1 個消費して文字を復活させる */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		int inkSlot = -1;
		for(int i = 0; i < player.getInventory().items.size(); i++)
		{
			ItemStack slotStack = player.getInventory().items.get(i);
			if(!slotStack.isEmpty() && slotStack.is(Items.BLACK_DYE))
			{
				inkSlot = i;
				break;
			}
		}

		if(inkSlot >= 0 && itemStack.getDamageValue() == 1)
		{
			player.getInventory().items.get(inkSlot).shrink(1);
			itemStack.setDamageValue(0);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return itemStack.getDamageValue() == 0;
	}

	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	@Override
	public boolean isEnchantable(ItemStack stack)
	{
		return false;
	}
}
