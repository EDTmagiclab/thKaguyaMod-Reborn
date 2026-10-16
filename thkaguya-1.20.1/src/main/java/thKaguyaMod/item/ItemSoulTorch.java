package thKaguyaMod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

/**
 * 人魂灯（人魂灯）
 *
 * 原版 ItemSoulTorch implements ISpecialShot：
 * - 右クリックで「使用開始」（弓アクション、12 tick）
 * - 使用完了時に真上向きのリング状に FORM_PHANTOM 弾を 3 発放つ
 *   （special = SPECIAL_SOULTORCH は「このアイテム自身」が特殊弾処理を提供する）
 * - 特殊弾は毎 tick homing(16.0F) で強い追尾、ブロックに当たっても消えない
 * - クリエイティブでは右クリックした瞬間に発動
 *
 * 1.20.1 迁移要点：
 * - 原版は Item 自身が ISpecialShot を実装し、コンストラクタで
 *   SpecialShotRegistry に登録する → 同じ構造をそのまま維持できる
 * - world.isRemote 判定の有無も原版どおり（createRingShot 内部で両側生成される）
 */
public class ItemSoulTorch extends Item implements ISpecialShot
{
	public static final int SPECIAL_SOULTORCH = 352;

	public ItemSoulTorch()
	{
		super(new Item.Properties().stacksTo(16));
		SpecialShotRegistry.registerSpecialShot(ItemSoulTorch.class, SPECIAL_SOULTORCH);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// 原版：クリエイティブなら右クリックした瞬間に発動
		if(player.isCreative())
		{
			emitSouls(itemStack, level, player);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity living)
	{
		if(living instanceof Player player)
		{
			emitSouls(itemStack, level, player);
		}
		return itemStack;
	}

	private void emitSouls(ItemStack itemStack, Level level, Player player)
	{
		// 【一時診断】原因特定後削除
		com.mojang.logging.LogUtils.getLogger().info("[TH] soulTorch emitSouls (client={})", level.isClientSide);

		ShotData shot = ShotData.shot(DanmakuConstants.FORM_PHANTOM, DanmakuConstants.PURPLE,
				0.4F, 7.0F, 0, 480, SPECIAL_SOULTORCH);
		Vec3 angle = THShotLib.angle(player.getYRot(), -90F);
		THShotLib.createRingShot(player, player, THShotLib.pos_Living(player), angle, 0F, 9999,
				0.01D, 0.4D, 0.03D, THShotLib.gravity_Zero(), shot, 3, 0.3D, 30F, 0F);

		THShotLib.playShotSound(player);

		if(!player.isCreative())
		{
			itemStack.shrink(1);
		}
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		return 12;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BOW;
	}

	// ===== ISpecialShot（このアイテム自身が特殊弾の挙動を提供する） =====

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot)
	{
		shot.homing(16.0F);
	}

	@Override
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot, HitResult result)
	{
		// ブロック衝突では消えない
		return true;
	}

	@Override
	public boolean specialShot_hitEntity(Level world, int id, EntityTHShot shot, Entity entityHit)
	{
		return false;
	}
}
