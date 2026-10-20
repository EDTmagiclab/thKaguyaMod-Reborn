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
import thKaguyaMod.entity.item.EntityMarisaBroom;

/** 魔理沙の魔法の箒 */
public class ItemMarisaBroom extends Item
{
	public ItemMarisaBroom()
	{
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		EntityMarisaBroom broom = new EntityMarisaBroom(level, player.getX(), player.getY(), player.getZ());
		// 原版 "random.drr"
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

		if(!level.isClientSide)
		{
			level.addFreshEntity(broom);
			itemStack.shrink(1);
		}
		if(!player.isPassenger() && !player.isShiftKeyDown())
		{
			player.startRiding(broom);
			broom.setYRot(player.getYRot());
			broom.setXRot(player.getXRot());
		}

		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity living_hit, LivingEntity living_used)
	{
		living_hit.hurt(living_hit.level().damageSources().mobAttack(living_used), 2.0F);
		return true;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
	{
		return 72000;
	}
}
