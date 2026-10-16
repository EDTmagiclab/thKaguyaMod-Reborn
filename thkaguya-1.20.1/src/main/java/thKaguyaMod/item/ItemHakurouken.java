package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.effect.EntityHakurouReflecter;

/**
 * 白楼剣（白楼剑）
 *
 * 原版 ItemHakurouken extends ItemSword：
 * - 基本は鉄の剣と同じ
 * - 右クリックを離したとき：
 *     スニーク中 → 弾幕を跳ね返す「白楼反射盾」（EntityHakurouReflecter）を展開
 *     通常      → 地面上で水平移動中なら、その進行方向と逆に 1.5 格ぶん吹き飛ぶ
 * - どちらかが成立したときだけ耐久 -1
 *
 * 1.20.1 迁移要点：
 * - onPlayerStoppedUsing → releaseUsing（原版は getMaxItemUseDuration を未指定＝0 のため
 *   実質「押した瞬間」に発動する。1.20.1 では長押し可能な 72000 を返し、
 *   離した瞬間に発動する形にしている。size は通常分岐では使われないため挙動は等価）
 * - player.onGround → onGround()、motionX/Z → getDeltaMovement()/setDeltaMovement()
 * - world.spawnEntityInWorld → level.addFreshEntity
 */
public class ItemHakurouken extends SwordItem
{
	/** 原版 toolMaterial_Hakurouken（maxUses 100 / efficiency 0 / damage 0）に相当 */
	public ItemHakurouken()
	{
		super(Tiers.IRON, 3, -2.4F, new Item.Properties().stacksTo(1).durability(100));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		boolean used = false;

		if(player.isShiftKeyDown())
		{
			// 白楼反射盾を展開
			EntityHakurouReflecter reflecter = new EntityHakurouReflecter(level, player);
			if(!level.isClientSide)
			{
				level.addFreshEntity(reflecter);
			}
			used = true;
		}
		else
		{
			Vec3 motion = player.getDeltaMovement();
			if(player.onGround() && (motion.x != 0.0D || motion.z != 0.0D))
			{
				// 進行方向と逆へ吹き飛ぶ
				double angle = Math.atan2(motion.x, motion.z);
				player.setDeltaMovement(motion.x - Math.sin(angle) * 1.5D,
						motion.y,
						motion.z - Math.cos(angle) * 1.5D);
				used = true;
			}
		}

		if(used)
		{
			itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
		}
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 72000;
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
