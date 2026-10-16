package thKaguyaMod.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.spellcard.THSpellCard;
import thKaguyaMod.registry.SpellCardRegistry;

/**
 * スペルカード（符卡）
 *
 * 原版 ItemTHSpellCard は metadata（damage 値）をスペルカード No として使っていた。
 * 1.20.1 には metadata が無いが、非ダメージアイテムの damage 値は NBT に保存され
 * ItemStack.getDamageValue()/setDamageValue() で読み書きできるため、
 * 原版と同じ「1 アイテム + damage = 符卡 No」方式をそのまま踏襲する。
 * （真珠・弾・銀刀などはアイテム分割したが、符卡は 40 枚近くあり
 *   画像・言語キーも 1 枚ごとに用意されているため分割しない方が忠実。）
 *
 * 1.7.10 -> 1.20.1:
 *   onItemRightClick        -> use(Level, Player, InteractionHand)
 *   onUpdate（常時）        -> inventoryTick
 *   addInformation          -> appendHoverText
 *   hasEffect               -> isFoil
 *   getUnlocalizedName+damage -> getDescriptionId(ItemStack)
 *   maxStackSize=1          -> Item.Properties().stacksTo(1)
 *   newInstance()           -> getDeclaredConstructor().newInstance()
 */
public class ItemTHSpellCard extends Item
{
	/** 符卡 No -> 必要レベル のキャッシュ（needLevel は符卡ごとに固定値） */
	private static final Map<Integer, Integer> LEVEL_CACHE = new HashMap<Integer, Integer>();

	public ItemTHSpellCard()
	{
		super(new Item.Properties().stacksTo(1));
	}

	/** ダメージ値（＝符卡 No）ごとに別名にする：item.thkaguya.spell_card.&lt;no&gt; */
	@Override
	public String getDescriptionId(ItemStack itemStack)
	{
		return super.getDescriptionId(itemStack) + "." + itemStack.getDamageValue();
	}

	/** 右クリック：スペルカードを宣言する */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 原版はここで THShotLib.NORMAL（難易度 Normal）を渡す
		THKaguyaLib.checkSpellCardDeclaration(level, itemStack, player, itemStack.getDamageValue(), THShotLib.NORMAL, true);

		return InteractionResultHolder.success(itemStack);
	}

	/** インベントリにある限り常時呼ばれる：早苗の符卡のチャージを減らす */
	@Override
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slot, boolean selected)
	{
		CompoundTag nbt = itemStack.getTag();
		if(nbt != null)
		{
			int miracleTime = nbt.getInt("Charge");
			if(miracleTime > 1)
			{
				nbt.putInt("Charge", miracleTime - 1);
			}
			else
			{
				nbt.remove("Charge");
			}
		}
	}

	/** アイテムの表示情報付加 */
	@Override
	public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(itemStack, level, tooltip, flag);

		int damage = itemStack.getDamageValue();

		tooltip.add(Component.literal("Lv." + getLevel(damage) + " Spell Card"));

		if(damage >= 14 && damage <= 18)
		{
			int miracleTime = 0;
			CompoundTag nbt = itemStack.getTag();
			if(nbt != null)
			{
				miracleTime = nbt.getInt("Charge");
			}

			int sneLevel = 20 - ((14 - damage) * 5);
			tooltip.add(Component.translatable("thKaguya.msg.sneSc1")
					.append(Component.literal(String.valueOf(sneLevel)))
					.append(Component.translatable("thKaguya.msg.sneSc2"))
					.append(Component.literal(String.valueOf(damage - 13)))
					.append(Component.translatable("thKaguya.msg.sneSc3")));
			tooltip.add(Component.translatable("thKaguya.msg.sneSc4")
					.append(Component.literal(String.valueOf(miracleTime / 10)))
					.append(Component.literal("%")));
		}
	}

	/** 早苗のスペルカードがアクティブ状態なら発光させる */
	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		int damage = itemStack.getDamageValue();

		if(damage >= 14 && damage <= 18)
		{
			CompoundTag nbt = itemStack.getTag();
			if(nbt != null && nbt.getInt("Charge") > 0)
			{
				return true;
			}
		}

		return false;
	}

	/** アイテムスタックからスペルカードIDを取得する */
	public int getSpellCardID(ItemStack itemStack)
	{
		return itemStack.getDamageValue();
	}

	/**
	 * スペルカードIDから宣言コストのレベル（1〜5）を取得する
	 * 原版同様、符卡クラスを反射でインスタンス化して getNeedLevel() を読む。
	 * 符卡ごとに固定値なので結果をキャッシュする。
	 */
	public int getLevel(int spellCardID)
	{
		Integer cached = LEVEL_CACHE.get(spellCardID);
		if(cached != null)
		{
			return cached;
		}

		int needLevel = 1;
		Class<? extends THSpellCard> spellcard = SpellCardRegistry.getSpellCardClass(spellCardID);

		if(spellcard != null)
		{
			try
			{
				THSpellCard useSpellCard = spellcard.getDeclaredConstructor().newInstance();
				needLevel = useSpellCard.getNeedLevel();
			}
			catch(ReflectiveOperationException e)
			{
				e.printStackTrace();
			}
		}

		LEVEL_CACHE.put(spellCardID, needLevel);
		return needLevel;
	}
}
