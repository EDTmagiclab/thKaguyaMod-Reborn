package thKaguyaMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 舟幽霊の柄杓（船幽灵的长勺）
 *
 * 原版 ItemHisyaku（一个类，subID 区分两态）：
 * - 0 = FILL（水入り）：水源を有限回設置できる。設置ごとに耐久 -2
 * - 1 = EMPTY（空）：水源を無限に取り除ける
 * - 潜行 + 右クリック：モード切替（もう片方のアイテムに入れ替わる、耐久は引き継ぐ）
 * - 何もブロックがないところで使うと足元が水源になる
 * - ネザーでは水源が置けず、代わりに消える音と煙が出る
 *
 * 1.20.1 迁移要点：
 * - metadata 変体は 1.20.1 で廃止されたため、2 登録項（ship_ghost_dipper / _empty）に分割し
 *   このクラスを subID 付きコンストラクタで共用する
 * - getMovingObjectPositionFromPlayer → level.clip(ClipContext)
 * - Blocks.water + metadata 0 → Blocks.WATER + fluidState.isSource()
 * - world.setBlockToAir → level.setBlock(AIR, UPDATE_ALL_IMMEDIATE)
 *   （Level.removeBlock は水源に対して no-op。理由は該当箇所のコメント参照）
 * - world.setBlock(..., Blocks.water) → level.setBlock(WATER, UPDATE_CLIENTS) + 流体 tick の取消
 *   （原版の「静止した水源」を再現。理由は該当箇所のコメント参照）
 * - world.provider.isHellWorld → level.dimension() == Level.NETHER
 */
public class ItemShipGhostDipper extends Item
{
	/** 水入り（設置モード） */
	public static final int FILL = 0;
	/** 空（除去モード） */
	public static final int EMPTY = 1;

	private final int mode;

	public ItemShipGhostDipper(int mode)
	{
		super(new Item.Properties().stacksTo(1).durability(60));
		this.mode = mode;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		// モード切り替え：もう片方の柄杓に入れ替える（耐久は引き継ぐ）
		//
		// ※ 創造モードで飛行中は Shift が「降下」なので、そこで切り替わってしまうと
		//   「放水したつもりが空の柄杓に変わった」という誤操作になる（実測で報告あり）。
		//   飛行中は切り替えを無効化し、放水（または取水）を優先する。
		if(player.isShiftKeyDown() && !player.getAbilities().flying)
		{
			ItemStack switched = new ItemStack(mode == FILL
					? THKaguyaItems.SHIP_GHOST_DIPPER_EMPTY.get()
					: THKaguyaItems.SHIP_GHOST_DIPPER.get(), 1);
			switched.setDamageValue(itemStack.getDamageValue());
			return InteractionResultHolder.success(switched);
		}

		// 原版 flag = (mode == EMPTY)：除去モードのみ液体をレイに含める
		boolean useLiquids = (mode == EMPTY);
		BlockHitResult hit = rayTrace(level, player, useLiquids);

		if(mode == EMPTY)
		{
			// 水源を除去する
			if(hit != null)
			{
				BlockPos pos = hit.getBlockPos();
				BlockState state = level.getBlockState(pos);
				if(state.is(Blocks.WATER) && state.getFluidState().isSource())
				{
					// ※ Level.removeBlock(pos, false) は「その場の流体の legacy block を書き戻す」実装
					//   （setBlock(pos, fluidState.createLegacyBlock(), 3)）なので、水源に対しては
					//   まったく同じ Blocks.WATER[level=0] を置き直すだけ ＝ **除去されない**。
					//   1.20.1 で水を正しく消すのはバニラの LiquidBlock.pickupBlock と同じ
					//   「AIR を UPDATE_ALL_IMMEDIATE(11) で設置」。
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
				}
			}
		}
		else
		{
			// 水源を設置する
			BlockPos place;
			if(hit == null)
			{
				// 原版「何もブロックがないところで使うと足元が水源になる」：
				// 落とすのは「足元のブロック（プレイヤーが立っているマス）」。
				// ※ 以前は getY() - 0.5 を使っていたため、足元ではなく
				//   「立っている地面そのもの」を指してしまい、固体ブロックなので
				//   置換判定に弾かれて水が出なかった（実測で発覚）。
				place = player.blockPosition();
			}
			else
			{
				Direction side = hit.getDirection();
				place = hit.getBlockPos().relative(side);
			}

			BlockState state = level.getBlockState(place);
			if(state.isAir() || state.canBeReplaced() || state.getFluidState().is(Fluids.WATER))
			{
				if(level.dimension() == Level.NETHER)
				{
					// ネザーでは水が蒸発する
					level.playSound(null, player.getX(), player.getEyeY() + 0.5D, player.getZ(),
							SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F,
							2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
					if(level.isClientSide)
					{
						for(int l = 0; l < 8; l++)
						{
							level.addParticle(ParticleTypes.LARGE_SMOKE,
									place.getX() + level.random.nextDouble(),
									place.getY() + level.random.nextDouble(),
									place.getZ() + level.random.nextDouble(),
									0.0D, 0.0D, 0.0D);
						}
					}
				}
				else
				{
					// 原版（1.7.10）の水源は「置いただけでは流体更新が積まれない」ため、
					// その場に静止したまま留まる（隣接ブロックが更新された時にはじめて流れ出す）。
					//
					// 1.20.1 はそうならない：LevelChunk.setBlockState は flag に関係なく
					// BlockState.onPlace を呼び（ガードは isClientSide と captureBlockSnapshots のみ）、
					// LiquidBlock.onPlace が必ず level.scheduleTick(pos, WATER, tickDelay) する。
					// → 設置した次の tick に流体が再計算され、周囲が空気だと即座に流れ出す
					//   （実測：flag を UPDATE_CLIENTS(2) に絞っても「普通の流水」になった）。
					//   1.20.1 の Block.UPDATE_* には「onPlace を飛ばす」高位フラグが存在しない。
					//
					// そこで設置直後にその予約を打ち消し、原版と同じ「静止した水源」を再現する。
					// 以降は隣接ブロックが更新された時点で LiquidBlock.neighborChanged が
					// 改めて tick を積むため、原版同様に「更新が入れば流れ出す」挙動になる。
					level.setBlock(place, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
					if(level instanceof ServerLevel serverLevel)
					{
						serverLevel.getFluidTicks().clearArea(BoundingBox.fromCorners(place, place));
					}
					itemStack.hurtAndBreak(2, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
				}
			}
		}

		return InteractionResultHolder.success(itemStack);
	}

	/** 原版 getMovingObjectPositionFromPlayer(world, player, flag) */
	private static BlockHitResult rayTrace(Level level, Player player, boolean includeLiquids)
	{
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.x * 5.0D, look.y * 5.0D, look.z * 5.0D);
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
				includeLiquids ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.BLOCK ? hit : null;
	}
}
