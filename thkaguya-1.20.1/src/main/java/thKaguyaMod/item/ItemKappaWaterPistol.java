package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/**
 * 河童の水鉄砲（河童的水枪）
 *
 * 原版 ItemKappaWaterPistol：
 * - 潜行 + 右クリック：視線先の水源を狙い、当たっていれば耐久を 2 回復（水を補充）
 * - 右クリック：水弾を 3 連射（上下 7 度ずつずらした FORM_LIGHT 青弾）、耐久 -1
 * - 耐久が最大 -1 に達したらそれ以上は撃てない（壊れない）
 * - エンチャント不可・金床修理不可
 *
 * 1.20.1 迁移要点：
 * - getMovingObjectPositionFromPlayer(world, player, true) → level.clip(ClipContext(Fluid.SOURCE_ONLY))
 * - Blocks.water + metadata 0（水源）→ Blocks.WATER + fluidState.isSource()
 * - playSoundAtEntity("liquid.splash") → SoundEvents.GENERIC_SPLASH
 * - getItemEnchantability → getEnchantmentValue、isBookEnchantable → isEnchantable
 */
public class ItemKappaWaterPistol extends Item
{
	public ItemKappaWaterPistol()
	{
		super(new Item.Properties().stacksTo(1).durability(39));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 水を補充する処理（潜行中のみ）
		if(player.isShiftKeyDown())
		{
			BlockHitResult hit = traceSource(level, player);
			if(hit != null)
			{
				BlockState state = level.getBlockState(hit.getBlockPos());
				if(state.is(Blocks.WATER) && state.getFluidState().isSource())
				{
					// 原版は水源ブロックを消さずに耐久だけ回復する
					itemStack.setDamageValue(itemStack.getDamageValue() - 2);
					player.swing(hand);
					return InteractionResultHolder.success(itemStack);
				}
			}
		}

		// 壊れないようにする処理
		if(itemStack.getDamageValue() == itemStack.getMaxDamage() - 1)
		{
			return InteractionResultHolder.success(itemStack);
		}

		// 水弾幕を出す処理
		double speed = 0.3D;
		Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot() + 40F, player.getXRot(), 0.4D);
		double xPos = player.getX() + vec3.x;
		double yPos = THShotLib.getPosYFromEye(player, -0.1D) + vec3.y;
		double zPos = player.getZ() + vec3.z;
		Vec3 pos = THShotLib.pos(xPos, yPos, zPos);
		ShotData shot = ShotData.shot(DanmakuConstants.FORM_LIGHT, DanmakuConstants.BLUE,
				0.2F, 4.0F, 0, 120);

		THShotLib.createShot(player, player, pos,
				THShotLib.angle(player.getYRot(), player.getXRot() - 7F), 0F,
				1.0D - speed, 0.0D, 0.4D, THShotLib.gravity_Default(), shot);
		THShotLib.createShot(player, player, pos,
				THShotLib.angle(player.getYRot(), player.getXRot()), 0F,
				1.1D - speed, 0.0D, 0.4D, THShotLib.gravity_Default(), shot);
		THShotLib.createShot(player, player, pos,
				THShotLib.angle(player.getYRot(), player.getXRot() + 7F), 0F,
				1.2D - speed, 0.0D, 0.4D, THShotLib.gravity_Default(), shot);
		speed += 0.07D;

		level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS,
				0.5F, 4.0F);
		itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));

		return InteractionResultHolder.success(itemStack);
	}

	/** 視線先の水源を探す（原版 getMovingObjectPositionFromPlayer(world, player, true)） */
	private static BlockHitResult traceSource(Level level, Player player)
	{
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.x * 5.0D, look.y * 5.0D, look.z * 5.0D);
		BlockHitResult hit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
		return hit.getType() == HitResult.Type.BLOCK ? hit : null;
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
