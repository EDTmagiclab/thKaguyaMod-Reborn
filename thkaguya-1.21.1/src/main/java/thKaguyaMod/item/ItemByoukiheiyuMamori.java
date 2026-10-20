package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 病気平癒守（病气平愈守）
 *
 * 原版 ItemByoukiheiyuMamori：
 * - 右クリックで「使用開始」（弓アクション、24 tick）
 * - 使用完了時に：被ダメージ中（hurtTime > 0）なら失敗、そうでなければ
 *   すべてのポーション効果を消し、リジェネレーション Lv4 を 3 秒付与
 * - クリエイティブでは右クリックした瞬間に即発動
 *
 * 1.20.1 迁移要点：
 * - onItemRightClick + setItemInUse → use() + player.startUsingItem()
 * - onEaten（使用完了）→ finishUsingItem(ItemStack, Level, LivingEntity)
 * - getMaxItemUseDuration → getUseDuration、EnumAction.bow → UseAnim.BOW
 * - clearActivePotions() → removeAllEffects()
 * - PotionEffect(10, 60, 3) → MobEffectInstance(MobEffects.REGENERATION, 60, 3)
 */
public class ItemByoukiheiyuMamori extends Item
{
	public ItemByoukiheiyuMamori()
	{
		super(new Item.Properties().stacksTo(16));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 原版：クリエイティブモードなら右クリックした瞬間に発動
		if(player.isCreative())
		{
			applyHeal(itemStack, level, player);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity living)
	{
		if(living instanceof Player player)
		{
			applyHeal(itemStack, level, player);
		}
		return itemStack;
	}

	/** 原版 onEaten の本体 */
	private void applyHeal(ItemStack itemStack, Level level, Player player)
	{
		// ダメージを受けている状態だと失敗する
		if(player.hurtTime > 0)
		{
			return;
		}

		// 全てのポーション効果を消す
		player.removeAllEffects();

		if(!level.isClientSide)
		{
			// リジェネ効果付与（原版 PotionEffect(10=regeneration, 3*20 tick, level 3)）
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 3 * 20, 3));
		}

		if(!player.isCreative())
		{
			itemStack.shrink(1);
		}
	}

	@Override
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
	{
		return 24;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BOW;
	}
}
