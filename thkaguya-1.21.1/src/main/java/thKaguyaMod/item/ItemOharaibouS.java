package thKaguyaMod.item;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.effect.EntityMiracleCircle;
import thKaguyaMod.entity.shot.EntitySanaeWind;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.THStackData;

/**
 * 風祝のお祓い棒（早苗の御祓い棒）
 *
 * 原版 ItemOharaibouS：
 * - 右クリック：五芒星（EntityMiracleCircle）を 1 つ描き始める。押し続けた tick 数で
 *   描いた五芒星の数が増えていく
 * - 離した瞬間の tick 数で分岐：
 *     30tick 未満  → 通常攻撃「風起こし」（EntitySanaeWind）。威力はレベル/5、弾数はレベル*3（15〜120）
 *     30tick 以上  → インベントリ内の符卡を探し、条件を満たす符卡に「奇跡の力」（NBT Charge=1000）を付与。
 *                    同時にお祓い棒の耐久を回復する
 *                      八坂の神風(No.18)  : 150tick / Lv40 以上 → 耐久 +30
 *                      モーゼの奇跡(No.17) : 120tick / Lv35 以上 → 耐久 +20
 *                      妖力スポイラー(No.16): 90tick / Lv30 以上 → 耐久 +15
 *                      ファフロッキーズの奇跡(No.15): 60tick / Lv25 以上 → 耐久 +10
 *                      ミラクルフルーツ(No.14)     : 20tick / Lv20 以上 → 耐久 +5
 * - 耐久 50 / 最大スタック 1
 *
 * 1.7.10 -> 1.20.1:
 *   onItemRightClick        -> use
 *   onPlayerStoppedUsing    -> releaseUsing
 *   getMaxItemUseDuration   -> getUseDuration
 *   getItemUseAction(block) -> getUseAnimation() == UseAnim.BLOCK
 *   inventory.mainInventory -> player.getInventory().items（空スタックは isEmpty() で除外）
 *   getItemDamage()         -> getDamageValue()（符卡は 1 アイテム + damage = 符卡 No 方式）
 *   damageItem(負の値)      -> setDamageValue(max(0, damage - n))
 *   NBTTagCompound          -> CompoundTag（getOrCreateTag）
 */
public class ItemOharaibouS extends Item
{
	public ItemOharaibouS()
	{
		// 原版 setMaxDamage(50) / setMaxStackSize(1) / CreativeTabs.tabCombat
		super(new Item.Properties().stacksTo(1).durability(50));
	}

	/** 右クリックを押した瞬間：五芒星を 1 つ出現させる */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		player.startUsingItem(hand);

		EntityMiracleCircle entityMiracleCircle = new EntityMiracleCircle(level, player, 0);
		if(!level.isClientSide)
		{
			level.addFreshEntity(entityMiracleCircle);
		}

		return InteractionResultHolder.consume(itemStack);
	}

	/** 右クリックを終了したときに呼び出される */
	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		// アイテムを右クリックしていた時間
		int time = getUseDuration(itemStack, player) - timeLeft;

		// 30tick 経過しているなら（五芒星を一つ以上描き終えているなら）
		if(time >= 30)
		{
			for(ItemStack card : player.getInventory().items)
			{
				if(!card.isEmpty() && card.getItem() == THKaguyaItems.SPELL_CARD.get())
				{
					int cardNo = card.getDamageValue();

					// 五芒星を５つ以上、使用者のレベルが４０以上、大奇跡「八坂の神風」なら
					if(time >= 150 && player.experienceLevel >= 40 && cardNo == 18)
					{
						setMiraclePower(card);
						repairItem(itemStack, 30);
					}
					// 五芒星を４つ以上、使用者のレベルが３５以上、開海「モーゼの奇跡」なら
					else if(time >= 120 && player.experienceLevel >= 35 && cardNo == 17)
					{
						setMiraclePower(card);
						repairItem(itemStack, 20);
					}
					// 五芒星を３つ以上、使用者のレベルが３０以上、妖怪退治「妖力スポイラー」なら
					else if(time >= 90 && player.experienceLevel >= 30 && cardNo == 16)
					{
						setMiraclePower(card);
						repairItem(itemStack, 15);
					}
					// 五芒星を２つ以上、使用者のレベルが２５以上、奇跡「ファフロッキーズの奇跡」なら
					else if(time >= 60 && player.experienceLevel >= 25 && cardNo == 15)
					{
						setMiraclePower(card);
						repairItem(itemStack, 10);
					}
					// 五芒星を１つ以上、使用者のレベルが２０以上、奇跡「ミラクルフルーツ」なら
					else if(player.experienceLevel >= 20 && cardNo == 14)
					{
						setMiraclePower(card);
						repairItem(itemStack, 5);
					}
				}
			}
		}

		// 五芒星を描き終えていないなら：通常攻撃の風起こし
		if(time < 30)
		{
			Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);

			float damage = player.experienceLevel / 5F;
			int endurance = player.experienceLevel * 3;
			if(endurance > 120)
			{
				endurance = 120;
			}
			else if(endurance < 15)
			{
				endurance = 15;
			}
			if(damage == 0)
			{
				damage = 0.1F;
			}

			EntitySanaeWind entitySanaeWind = new EntitySanaeWind(level, player, player,
					THShotLib.pos_Living(player), vec3, 0F,
					THShotLib.rotate_Default(), 0F, 9999,
					0.4D, 0.01D, 0.99D, THShotLib.gravity_Zero(),
					0, 1.0F, damage, 0, endurance, DanmakuConstants.WIND01);

			if(!level.isClientSide)
			{
				level.addFreshEntity(entitySanaeWind);
			}

			itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
		}
	}

	/** スペルカードに奇跡の力を付与する */
	private void setMiraclePower(ItemStack itemStack)
	{
		CompoundTag chargeTag = THStackData.getOrCreate(itemStack);
		chargeTag.putInt("Charge", 1000);
		THStackData.set(itemStack, chargeTag);
	}

	/** 原版 damageItem(負の値) 相当：耐久を回復する */
	private void repairItem(ItemStack itemStack, int amount)
	{
		itemStack.setDamageValue(Math.max(0, itemStack.getDamageValue() - amount));
	}

	@Override
	public int getUseDuration(ItemStack itemStack, LivingEntity entity)
	{
		return 72000;
	}

	/** 右クリック時は剣のガードアクション */
	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BLOCK;
	}

	/** 原版の既定値（Item.getItemEnchantability() == 0）に合わせる */
	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}
}
