package thKaguyaMod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
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
		public int getLevel()
		{
			return 0;
		}

		@Override
		public int getEnchantmentValue()
		{
			return 0;
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
		super(RoukankenTier.INSTANCE, 3, -2.4F, new Item.Properties().stacksTo(1).durability(251));
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
		if(target.getMobType() == MobType.UNDEAD)
		{
			target.hurt(attacker.level().damageSources().mobAttack(attacker), 16.0F);
			if(attacker instanceof Player player)
			{
				player.magicCrit(target);
			}
		}

		// 耐久を１減らす
		itemStack.hurtAndBreak(1, attacker, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));

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
			double power = (double)getUseDuration(itemStack) - timeLeft;
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

			itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
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

		Vec3 motion = player.getDeltaMovement();

		// 移動量（水平）が 3.0 を越しているなら体当たり判定を取る
		if(Math.sqrt(motion.x * motion.x + motion.z * motion.z) <= 3.0D)
		{
			return;
		}

		List<Entity> list = level.getEntities(player,
				player.getBoundingBox().expandTowards(motion).inflate(1.0D), e -> true);

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
					if(living.getMobType() == MobType.UNDEAD)
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
				itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
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
	public boolean isCorrectToolForDrops(BlockState state)
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
