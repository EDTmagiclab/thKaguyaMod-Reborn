package thKaguyaMod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHLaser;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 楼観剣（楼观剑）
 *
 * 原版 ItemRoukanken extends ItemSword（妖夢の武器）：
 * - アンデッド特攻（殴った相手が UNDEAD なら +16 ダメージと enchantment critical）
 * - ガード不可、代わりに右クリック長押し→離すと前方ダッシュ（最大 20tick 分＝6.0 の勢い、空腹 1.5）
 * - 手に持って速度 3.0 以上で動くと、体当たり判定（移動ベクトル+1 格）内の Entity を斬る
 *     生きた Entity：アンデッド 16 / それ以外 8、LoS が通るときだけ
 *     弾（EntityTHShot、レーザーを除く）：持ち主の周りに白い弾を 3 発ランダムリングで撒いて弾を消す
 * - 破壊速度：蜘蛛の巣・葉 30F、羊毛 20F、その他は通常の 3 倍
 * - どんなブロックでも回収可、エンチャント不可
 *
 * 1.7.10 -> 1.20.1:
 *   getDigSpeed(ItemStack,Block,int) -> getDestroySpeed(ItemStack,BlockState)
 *   hitEntity                        -> hurtEnemy
 *   onItemRightClick                 -> use（startUsingItem）
 *   onPlayerStoppedUsing             -> releaseUsing
 *   onUpdate（常時）                 -> inventoryTick
 *   getItemUseAction(bow)            -> getUseAnimation() == UseAnim.BOW
 *   canHarvestBlock                  -> isCorrectToolForDrops
 *   getCreatureAttribute()==UNDEAD   -> getMobType()==MobType.UNDEAD
 *   attackEntityFrom(causeMobDamage) -> hurt(level.damageSources().mobAttack(...))
 *   onEnchantmentCritical            -> player.magicCrit(target)
 *   playSoundAtEntity(random.bow)    -> level.playSound(null, x, y, z, SoundEvents.ARROW_SHOOT, ...)
 *   addExhaustion                    -> causeFoodExhaustion
 *   canEntityBeSeen                  -> player.hasLineOfSight
 *   canBeCollidedWith                -> entity.isPickable
 *   shot.pos_Shot()                  -> shot.pos()
 *
 * 原版 toolMaterial_Roukanken（harvestLevel 0 / maxUses 251 / efficiency 4.0F / damage 3.0F /
 * enchantability 0、修繕アイテム無し）を Tier として再現している。
 * なお SwordItem の攻撃力は「1（素手）+ 3（SwordItem 既定）+ tier.getAttackDamageBonus()」
 * なので 3.0F を指定すると原版の 7.0 と一致する。
 */
public class ItemRoukanken extends SwordItem
{
	/** 原版 toolMaterial_Roukanken 相当（修繕不可） */
	public enum RoukankenTier implements Tier
	{
		INSTANCE;

		@Override
		public int getUses()
		{
			return 251;
		}

		@Override
		public float getSpeed()
		{
			return 4.0F;
		}

		@Override
		public float getAttackDamageBonus()
		{
			return 3.0F;
		}


		@Override
		public int getEnchantmentValue()
		{
			return 0;
		}

		/** 1.21.1 で Tier に追加された抽象メソッド。楼観剣は全ブロック採掘可（isCorrectToolForDrops 側で担保）。 */
		@Override
		public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops()
		{
			return net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL;
		}

		@Override
		public Ingredient getRepairIngredient()
		{
			return Ingredient.EMPTY;
		}
	}

	/** 楼観旋風刃も同じ素材性能を共有する */
	public ItemRoukanken()
	{
		super(RoukankenTier.INSTANCE, (new Item.Properties().stacksTo(1).durability(251)).attributes(SwordItem.createAttributes(RoukankenTier.INSTANCE, 3, -2.4F)));
	}

	/** ブロックを破壊する速度 */
	@Override
	public float getDestroySpeed(ItemStack itemStack, BlockState state)
	{
		// 蜘蛛の巣か葉ブロックなら破壊がかなり速くなる
		if(state.is(Blocks.COBWEB) || state.is(BlockTags.LEAVES))
		{
			return 30F;
		}
		// 羊毛でもそこそこ速い
		else if(state.is(BlockTags.WOOL))
		{
			return 20F;
		}
		// その他のブロックでも通常の３倍速い
		else
		{
			return super.getDestroySpeed(itemStack, state) * 3F;
		}
	}

	/** Entity に当たった時に呼び出される */
	@Override
	public boolean hurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker)
	{
		// 当たった Entity がアンデッドなら、ハート１０個分の追加ダメージ
		if(target.getType().is(EntityTypeTags.UNDEAD))
		{
			target.hurt(attacker.level().damageSources().mobAttack(attacker), 16.0F);
			if(attacker instanceof Player player)
			{
				player.magicCrit(target);
			}
		}

		// 耐久を１減らす
		itemStack.hurtAndBreak(1, attacker, net.minecraft.world.entity.LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));

		return true;
	}

	/** 右クリックを押した瞬間：使用を開始する（ダッシュの溜め） */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	/** 右クリックを押し終わったとき：地面にいれば前方へダッシュ */
	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		if(player.onGround())
		{
			// 右クリック時間でダッシュするパワーを決める（20 が限界）
			double power = (double)getUseDuration(itemStack, player) - timeLeft;
			if(power > 20.0D)
			{
				power = 20.0D;
			}
			power *= 0.3D;

			// プレイヤーを向いている方向へ前進させる（Y は原版同様そのまま）
			Vec3 motion = player.getDeltaMovement();
			player.setDeltaMovement(-Math.sin(Math.toRadians(player.getYRot())) * power,
					motion.y,
					Math.cos(Math.toRadians(player.getYRot())) * power);

			if(!player.isCreative())
			{
				player.causeFoodExhaustion(1.5F);
			}

			itemStack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
		}
	}

	/** 所持している限り常に呼ばれる：高速移動中の斬撃判定 */
	@Override
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slot, boolean selected)
	{
		if(!(entity instanceof Player player))
		{
			return;
		}

		// 手に持っているものがこの楼観剣でなければ何もしない
		if(player.getMainHandItem() != itemStack
				|| player.getMainHandItem().getItem() != THKaguyaItems.ROUKANKEN.get())
		{
			return;
		}

		// 1.20.1 の inventoryTick は Player.tick 内で LivingEntity.tick（＝移動処理）より先に呼ばれる
		// （javap 実測：Player.tick の offset 113 が Inventory.tick、140 が LivingEntity.tick）。
		// そのため getDeltaMovement() はまだ「前 tick の摩擦後」の値しか持たず、突進直後でも
		// 6.0 * 0.546 ≒ 3.28 程度に落ちている。原版 1.7.10 は移動後に走る onUpdate から
		// 移動前の motionX/Z を見ていたので、ここでは実際に前 tick で動いた距離（pos - xo）も見る。
		double movedX = player.getX() - player.xo;
		double movedY = player.getY() - player.yo;
		double movedZ = player.getZ() - player.zo;
		Vec3 motion = player.getDeltaMovement();

		// 移動量（水平）が 3.0 を越しているなら体当たり判定を取る
		if(Math.sqrt(movedX * movedX + movedZ * movedZ) <= 3.0D
				&& Math.sqrt(motion.x * motion.x + motion.z * motion.z) <= 3.0D)
		{
			return;
		}

		// 突進で通り抜けた範囲全体（前 tick の位置 → 現在位置 → 今 tick の移動先）を覆う。
		// 原版は「移動前の位置 + motion」で判定していたため、1.20.1 では実際の変位ぶんを
		// 両方向へ広げて同じ掃引範囲を作る（片方向だけだと突進中に置き去りにした敵を逃す）。
		List<Entity> list = level.getEntities(player,
				player.getBoundingBox()
						.move(-movedX, -movedY, -movedZ)
						.expandTowards(movedX * 2.0D, movedY * 2.0D, movedZ * 2.0D)
						.expandTowards(motion)
						.inflate(1.0D),
				e -> true);

		for(Entity target : list)
		{
			if(!target.isPickable())
			{
				continue;
			}

			boolean isShot = false;

			if(target instanceof LivingEntity living)
			{
				if(player.hasLineOfSight(living))
				{
					if(living.getType().is(EntityTypeTags.UNDEAD))
					{
						// アンデッドなら 16 ダメージを追加する
						living.hurt(level.damageSources().mobAttack(player), 16.0F);
						player.magicCrit(living);
					}
					else
					{
						// それ以外なら 8 ダメージ
						living.hurt(level.damageSources().mobAttack(player), 8F);
					}

					level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
							SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F,
							0.4F / (level.random.nextFloat() * 4F + 0.8F));
				}
			}
			else if(target instanceof EntityTHShot shot && !(target instanceof EntityTHLaser))
			{
				// 弾は斬り払って、持ち主の周りに白い弾を撒く
				if(shot.user != null)
				{
					THShotLib.createRandomRingShot(shot.user, shot.pos(), shot.angle, 0.0D, 0.4D, 0.03D,
							ShotData.shot(shot.getShotForm(), DanmakuConstants.WHITE,
									shot.getShotSize() / 2.0F, shot.shotDamage),
							3, 0.0D, 90F);
				}
				if(!level.isClientSide)
				{
					shot.discard();
				}
				isShot = true;
			}

			if(!isShot)
			{
				// アイテムの耐久を１減らす
				itemStack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
			}

			// 投げる動作をさせる
			player.swing(InteractionHand.MAIN_HAND);

			// 耐久が限界なら楼観剣は消滅する
			if(itemStack.isEmpty())
			{
				break;
			}
		}
	}

	/**
	 * 使用時間（原版 getMaxItemUseDuration は Item のデフォルト = 72000）
	 *
	 * 1.20.1 の Item#getUseDuration のデフォルトは 0 のため、明示的に上書きしないと
	 * startUsingItem が即座に終了してしまい、releaseUsing の溜め時間が常に 0 になる
	 * （＝ダッシュが一切出なくなる）。
	 */
	@Override
	public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
	{
		return 72000;
	}

	/** 右クリックを長押しした際の動作：弓の構え */
	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		return UseAnim.BOW;
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

	/** なんでも取得可能 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack itemStack, BlockState state)
	{
		return true;
	}

	/** エンチャントブックの使用を許可しない */
	@Override
	public boolean isBookEnchantable(ItemStack itemStack1, ItemStack itemStack2)
	{
		return false;
	}
}
