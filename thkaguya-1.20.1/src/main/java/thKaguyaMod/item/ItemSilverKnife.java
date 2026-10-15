package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.item.EntitySilverKnife;

/** 咲夜の銀のナイフ（4色は独立アイテム、color: 0青 1赤 2緑 3白） */
public class ItemSilverKnife extends Item
{
	private final int color;

	public ItemSilverKnife(int color)
	{
		super(new Item.Properties().stacksTo(64));
		this.color = color;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		double speed = 0.28D;

		Vec3 angle = THShotLib.angle(player.getYRot(), player.getXRot());
		Vec3 pos = THShotLib.pos_Distance(THShotLib.pos_Living(player), angle, speed);

		EntitySilverKnife silverKnife = new EntitySilverKnife(level, player, pos, angle, speed, color);

		// 原版 "random.bow"
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

		if(!level.isClientSide)
		{
			level.addFreshEntity(silverKnife);
			itemStack.shrink(1);
		}

		player.swing(hand);

		return InteractionResultHolder.success(itemStack);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity living_hit, LivingEntity living_used)
	{
		living_hit.hurt(living_hit.level().damageSources().mobAttack(living_used), 3.0F);
		// 白ナイフなら消える
		if(color == 3)
		{
			itemStack.shrink(1);
		}
		return true;
	}
}
