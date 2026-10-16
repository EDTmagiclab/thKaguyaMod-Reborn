package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import thKaguyaMod.entity.shot.EntityOnmyoudama;

/**
 * 博麗のお祓い棒（博丽驱魔棒）
 *
 * 原版 ItemOharaibouR：
 * - 右クリック：陰陽玉（EntityOnmyoudama）を目の前に出現させ、使用中は
 *   陰陽玉がプレイヤーの前方 2 格に追従しつつ巨大化する（伸ばし切りで射出される）
 * - 使用を離した瞬間：random.bow を鳴らして耐久 -1
 * - 耐久 89 / 最大スタック 1 / エンチャント不可
 *
 * 1.7.10 -> 1.20.1:
 *   onItemRightClick        -> use(Level, Player, InteractionHand)
 *   setItemInUse            -> player.startUsingItem(hand)
 *   onPlayerStoppedUsing    -> releaseUsing(ItemStack, Level, LivingEntity, int)
 *   getMaxItemUseDuration   -> getUseDuration
 *   getItemUseAction(block) -> getUseAnimation() == UseAnim.BLOCK
 *   world.spawnEntityInWorld-> level.addFreshEntity
 *   playSoundEffect(random.bow) -> playSound(null, ..., SoundEvents.ARROW_SHOOT, ...)
 *
 * なお EntityOnmyoudama 側は「使用中のアイテムが HAKUREI_MIKO_STICK かどうか」で
 * 追従/射出を判定している（EntityOnmyoudama.java:56,68）ため、本アイテムが
 * そのまま登録されることが前提になっている。
 */
public class ItemOharaibouR extends Item
{
	public ItemOharaibouR()
	{
		// 原版 setMaxDamage(89) / maxStackSize = 1 / CreativeTabs.tabCombat
		super(new Item.Properties().stacksTo(1).durability(89));
	}

	/** 右クリックを押した瞬間：陰陽玉を出現させ、使用状態に入る */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		player.startUsingItem(hand);

		Vec3 vec3 = THShotLib.getVecFromAngle(-player.getYRot(), -player.getXRot(), 0.6D);

		EntityOnmyoudama onmyoudama = new EntityOnmyoudama(level, player, player,
				THShotLib.pos(player.getX() + vec3.x,
						THShotLib.getPosYFromEye(player, -0.1D) + vec3.y,
						player.getZ() + vec3.z),
				vec3, 0F, THShotLib.rotate_Default(), 0F, 9999,
				0.0D, 1.8D, 0.00D, THShotLib.gravity_Default(),
				DanmakuConstants.ORANGE, 0.3F, 10.0F, 0, 180, DanmakuConstants.BOUND04);

		if(!level.isClientSide)
		{
			level.addFreshEntity(onmyoudama);
		}

		return InteractionResultHolder.consume(itemStack);
	}

	/** 右クリックを離したとき：音を鳴らして耐久を 1 減らす */
	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		int time = getUseDuration(itemStack) - timeLeft;
		if(time > 100)
		{
			time = 100;
		}

		// 原版 random.bow（弓を放つ音）を 2.0F / 0.3F で再生
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 2.0F, 0.3F);

		// 耐久を１減らす
		itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 72000;
	}

	/** 原版 EnumAction.block（盾のように構える） */
	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BLOCK;
	}

	/** エンチャント不可 */
	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	@Override
	public boolean isEnchantable(ItemStack itemStack)
	{
		return false;
	}
}
