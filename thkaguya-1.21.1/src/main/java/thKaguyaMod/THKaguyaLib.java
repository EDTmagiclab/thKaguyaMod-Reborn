package thKaguyaMod;

import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.entity.spellcard.THSpellCard;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.registry.SpellCardRegistry;

/**
 * 五つの難題MOD+用、汎用関数群（1.20.1 移植版）
 *
 * 保留：checkSpellCardDeclaration（プレイヤーの符卡宣言）、spellCardDeclaration（符卡出現）、
 *       getPlayerPower、radTrans/degTrans、itemEffectFollowUser/itemEffectFinish。
 * 裁剪：createSilverKnife（依赖 EntitySilverKnife 的旧签名，符卡侧不走此处）。
 */
public class THKaguyaLib
{
	/**
	 * スペルカードの宣言で宣言が可能だった場合の処理
	 */
	public static void spellCardDeclaration(Level world, LivingEntity user, LivingEntity target, int spellCardNumber, int needLevel, int level, boolean firstAttack)
	{
		EntitySpellCard entitySpellCard = new EntitySpellCard(world, user, target, spellCardNumber, level);

		// スペルカード名（原版: StatCollector.translateToLocal("item.thSpellCard." + n + ".name")）
		String spellCardName = spellCardDecarationName(spellCardNumber);

		if(!world.isClientSide)
		{
			world.addFreshEntity(entitySpellCard);
		}

		// 宣言者がプレイヤーの場合
		if(user instanceof Player)
		{
			Player playerUser = (Player)user;

			if(!world.isClientSide)
			{
				// スペルカードを使ったというメッセージを出す
				playerUser.sendSystemMessage(Component.literal(spellCardName));
				// スペルカード宣言音を出す
				playSpellCardSound(world, playerUser);
			}

			// ボムアイテム（bomb_item）を必要レベル分まで消費し、足りない分は経験レベルを消費する
			int bomb = 0;

			while(bomb < needLevel && inventoryHas(playerUser, THKaguyaItems.BOMB_ITEM.get()))
			{
				inventoryConsume(playerUser, THKaguyaItems.BOMB_ITEM.get());
				bomb++;
			}

			// ボムアイテムで足りなかった分は、レベルを消費
			playerUser.giveExperienceLevels(bomb - needLevel);
		}

		// 宣言対象がプレイヤーの場合
		if(target instanceof Player)
		{
			// 継続スペルカードでなければ
			if(firstAttack && !world.isClientSide)
			{
				Player playerTarget = (Player)target;
				// 宣言対象にスペルカード宣言メッセージを出す
				playerTarget.sendSystemMessage(Component.literal(spellCardName));
				// スペルカード宣言音を出す
				playSpellCardSound(world, playerTarget);
			}
		}

		// 継続的に使っているスペルカードでなければ、周囲の敵弾を回収する
		if(firstAttack)
		{
			THShotLib.danmakuRemove(user, 40.0F, "Enemy", true);
		}
	}

	/** 符卡名（lang: item.thkaguya.spell_card.&lt;id&gt;）を返す */
	public static String spellCardDecarationName(int spellCardNumber)
	{
		return Component.translatable("item." + THKaguyaCore.MODID + ".spell_card." + spellCardNumber).getString();
	}

	/** 符卡宣言音（原版 "thkaguyamod:spellcard"） */
	private static void playSpellCardSound(Level world, Player player)
	{
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				thKaguyaMod.init.THKaguyaSounds.SPELL_CARD.get(),
				SoundSource.PLAYERS, THKaguyaConfig.SpellCardVol, 1.0F);
	}

	/** インベントリに指定アイテムがあるか */
	private static boolean inventoryHas(Player player, Item item)
	{
		for(ItemStack stack : player.getInventory().items)
		{
			if(!stack.isEmpty() && stack.is(item))
			{
				return true;
			}
		}
		return false;
	}

	/** インベントリから指定アイテムを1つ消費する（原版 consumeInventoryItem 相当） */
	private static void inventoryConsume(Player player, Item item)
	{
		for(int i = 0; i < player.getInventory().items.size(); i++)
		{
			ItemStack stack = player.getInventory().items.get(i);
			if(!stack.isEmpty() && stack.is(item))
			{
				stack.shrink(1);
				if(stack.isEmpty())
				{
					player.getInventory().items.set(i, ItemStack.EMPTY);
				}
				return;
			}
		}
	}

	/**
	 * プレイヤーの符卡宣言判定（原版 THKaguyaLib.checkSpellCardDeclaration）
	 *
	 * 非クリエイティブ時の消費判定（早苗の符卡の Charge、bomb_item 総数＋経験レベルが必要レベル以上か）を行い、
	 * 使用者の視線 32 ブロック以内で最も近い生物（Animal / Villager を除く）を対象として宣言する。
	 * 同一使用者の EntitySpellCard が既に 64 ブロック以内に存在するなら宣言できない。
	 *
	 * @param world           ワールド
	 * @param itemStack       使用したアイテム（符卡）
	 * @param user            使用者
	 * @param spellCardNumber 符卡 No
	 * @param level           符卡レベル（1:Easy 2:Normal 3:Hard 4:Lunatic、プレイヤー宣言は基本 2）
	 * @param firstAttack     true なら符卡名と宣言音を出す
	 * @return 宣言に成功したら true（原版同様、クライアント側では常に false）
	 */
	public static boolean checkSpellCardDeclaration(Level world, ItemStack itemStack, LivingEntity user, int spellCardNumber, int level, boolean firstAttack)
	{
		int needLevel = 0;

		// 宣言者がプレイヤーだった場合の処理
		if(user instanceof Player)
		{
			Player player = (Player)user;

			// プレイヤーがクリエイティブでないなら（クリエイティブは消費処理不要）
			if(!player.isCreative())
			{
				// 手に持っているのが早苗の符卡（14〜18）なら、NBT Charge が必要
				if(itemStack.getItem() == THKaguyaItems.SPELL_CARD.get())
				{
					// 1.21.1: maxDamage=0 のアイテムは damage が 0 に clamp されるため stack_tag から読む
					int damage = THStackData.getInt(itemStack, "Damage", 0);
					if(damage >= 14 && damage <= 18)
					{
						CompoundTag nbt = THStackData.get(itemStack);

						// NBT がない、または Charge が 0 以下なら宣言失敗
						if(nbt == null || nbt.getInt("Charge") <= 0)
						{
							return false;
						}
					}
				}

				// 符卡ごとの消費レベルを取得
				Class<? extends THSpellCard> spellcard = SpellCardRegistry.getSpellCardClass(spellCardNumber);
				if(spellcard != null)
				{
					THSpellCard useSpellCard = newSpellCard(spellcard);
					if(useSpellCard != null)
					{
						needLevel = useSpellCard.getNeedLevel();
					}
				}

				// インベントリ内の bomb_item 総数を数える
				int bombItemNum = 0;
				for(ItemStack stack : player.getInventory().items)
				{
					if(!stack.isEmpty() && stack.getItem() == THKaguyaItems.BOMB_ITEM.get())
					{
						bombItemNum += stack.getCount();
					}
				}

				// 経験レベル＋bomb_item 総数が必要レベルを下回るなら宣言失敗
				if((player.experienceLevel + bombItemNum) < needLevel)
				{
					return false;
				}
			}
		}

		// 視線方向（32 ブロック先まで）
		double angleX = -Math.sin(radTrans(user.getYRot())) * Math.cos(radTrans(user.getXRot()));
		double angleY = -Math.sin(radTrans(user.getXRot()));
		double angleZ =  Math.cos(radTrans(user.getYRot())) * Math.cos(radTrans(user.getXRot()));

		Vec3 vec3d = new Vec3(user.getX(), user.getY() + user.getEyeHeight(), user.getZ());
		Vec3 vec3d1 = new Vec3(user.getX() + angleX * 32.0D, user.getY() + angleY * 32.0D, user.getZ() + angleZ * 32.0D);

		// ブロックとの衝突を取得し、当たったら終点を衝突点に変更
		BlockHitResult blockHit = world.clip(new ClipContext(vec3d, vec3d1,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
		if(blockHit.getType() != HitResult.Type.MISS)
		{
			vec3d1 = blockHit.getLocation();
		}

		// 線分から範囲 1.0 ブロック分を加えた直方体と衝突する Entity から、最も近い生物を選ぶ
		AABB searchBox = user.getBoundingBox()
				.expandTowards(angleX * 32.0D, angleY * 32.0D, angleZ * 32.0D)
				.inflate(1.0D, 1.0D, 1.0D);

		Entity targetEntity = null;
		double nearDistance = 0.0D;

		List<Entity> list = world.getEntities(user, searchBox);
		for(Entity entity1 : list)
		{
			if(!(entity1 instanceof LivingEntity) || entity1 instanceof Animal || entity1 instanceof Villager)
			{
				continue;
			}

			float expand = 0.3F;//当たり判定の加算値
			AABB axisalignedbb = entity1.getBoundingBox().inflate(expand, expand, expand);
			Optional<Vec3> intercept = axisalignedbb.clip(vec3d, vec3d1);

			if(intercept.isPresent())
			{
				double distanceToHitEntity = vec3d.distanceTo(intercept.get());
				if(distanceToHitEntity < nearDistance || nearDistance == 0.0D)
				{
					targetEntity = entity1;
					nearDistance = distanceToHitEntity;
				}
			}
		}

		// 同一使用者のスペルカードが既に存在するなら宣言を無効化
		for(EntitySpellCard entitySpellCard : world.getEntitiesOfClass(EntitySpellCard.class,
				user.getBoundingBox().inflate(64.0D, 64.0D, 64.0D)))
		{
			if(entitySpellCard.user == user)
			{
				return false;
			}
		}

		if(targetEntity == null)
		{
			return false;
		}

		if(itemProssessionCheck(world, user, spellCardNumber) && targetEntity instanceof LivingEntity)
		{
			// 原版同様、宣言の実処理はサーバ側でのみ行い、クライアントは false を返す
			if(!world.isClientSide)
			{
				spellCardDeclaration(world, user, (LivingEntity)targetEntity, spellCardNumber, needLevel, level, firstAttack);
				return true;
			}
		}

		return false;
	}

	/** 原版 itemProssessionCheck（常に true） */
	private static boolean itemProssessionCheck(Level world, LivingEntity user, int number)
	{
		return true;
	}

	/** 符卡クラスを反射でインスタンス化する（原版 newInstance()） */
	private static THSpellCard newSpellCard(Class<? extends THSpellCard> spellcard)
	{
		try
		{
			return spellcard.getDeclaredConstructor().newInstance();
		}
		catch(ReflectiveOperationException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * プレイヤーのパワーを返す。最大 400（原版 THKaguyaLib.getPlayerPower）
	 * power_up_item_small 1 個 = 1、power_up_item_big 1 個 = 10。
	 */
	public static float getPlayerPower(Player player)
	{
		float power = 0.0F;

		for(ItemStack stack : player.getInventory().items)
		{
			if(stack.isEmpty())
			{
				continue;
			}

			if(stack.getItem() == THKaguyaItems.POWER_UP_ITEM_SMALL.get())
			{
				power += 1.0F * stack.getCount();
			}
			else if(stack.getItem() == THKaguyaItems.POWER_UP_ITEM_BIG.get())
			{
				power += 10.0F * stack.getCount();
			}
		}

		if(power > 400.0F)
		{
			power = 400.0F;
		}

		return power;
	}

	public static float radTrans(float deg)
	{
		return deg / 180F * (float)Math.PI;
	}

	public static float degTrans(float rad)
	{
		return rad / (float)Math.PI * 180F;
	}

	/* ==================== 阶段 3d-4：item 实体通用処理 ==================== */

	/**
	 * アイテムエフェクトを使用者に追従させる（1.20.1 移植版）
	 * 1.7.10 -> 1.20.1: posX->getX()、rotationYaw->getYRot()、motionX->getDeltaMovement()
	 */
	public static void itemEffectFollowUser(Entity itemEntity, Entity userEntity, double lengthToUser, float positionAngle, boolean yFollow, double yOffset)
	{
		if(userEntity != null)
		{
			double posX, posY, posZ;

			if(yFollow)
			{
				posX = userEntity.getX() - Math.sin((userEntity.getYRot() + positionAngle) / 180F * Math.PI) * Math.cos(userEntity.getXRot() / 180F * Math.PI) * lengthToUser;
				posZ = userEntity.getZ() + Math.cos((userEntity.getYRot() + positionAngle) / 180F * Math.PI) * Math.cos(userEntity.getXRot() / 180F * Math.PI) * lengthToUser;
				posY = userEntity.getY() - Math.sin(userEntity.getXRot() / 180F * Math.PI) * lengthToUser + (double)userEntity.getEyeHeight() - 0.5D;//若干目の高さより下に設置
			}
			else
			{
				posX = userEntity.getX() - Math.sin((userEntity.getYRot() + positionAngle) / 180F * Math.PI) * lengthToUser;
				posZ = userEntity.getZ() + Math.cos((userEntity.getYRot() + positionAngle) / 180F * Math.PI) * lengthToUser;
				posY = userEntity.getY() + yOffset;//若干目の高さより下に設置
			}
			Vec3 userMotion = userEntity.getDeltaMovement();
			itemEntity.setDeltaMovement(
					userMotion.x + (posX - itemEntity.getX()),
					userMotion.y + (posY - itemEntity.getY()),
					userMotion.z + (posZ - itemEntity.getZ()));
			itemEntity.setPos(posX, posY, posZ);
		}
	}

	public static void itemEffectFollowUser(Entity itemEntity, Entity userEntity, double lengthToUser, float positionAngle)
	{
		itemEffectFollowUser(itemEntity, userEntity, lengthToUser, positionAngle, true, 0.0D);
	}

	/**
	 * アイテムエフェクトの終了処理：使用者にアイテムを返す / その場にドロップ（1.20.1 移植版）
	 * 1.7.10 -> 1.20.1: EntityItem->ItemEntity、ItemStack(item,1,damage)->setDamageValue、
	 * capabilities.isCreativeMode->isCreative()、isDead->isDeadOrDying()
	 */
	public static boolean itemEffectFinish(Entity itemEntity, LivingEntity userEntity, Item item, int damage)
	{
		//使用者が存在するなら
		if(userEntity != null)
		{
			if(userEntity instanceof Player userPlayer)
			{
				if(userPlayer.isCreative())//使用者がクリエイティブモードなら
				{
					if(!itemEntity.level().isClientSide)
					{
						itemEntity.discard();//そのまま消滅させる
					}
					return true;
				}
				//アイテムをインベントリに加える
				ItemStack stack = new ItemStack(item, 1);
				if(damage > 0)
				{
					stack.setDamageValue(damage);
				}
				if(!userPlayer.getInventory().add(stack))
				{
					//インベントリが一杯なら使用者の目の前でアイテム化
					if(!itemEntity.level().isClientSide)
					{
						itemEntity.level().addFreshEntity(new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack));
						itemEntity.discard();
					}
					return true;
				}
			}
			if(userEntity.isDeadOrDying())//使用者が死んでいるなら
			{
				if(!itemEntity.level().isClientSide)
				{
					//アイテム化し、その場にドロップ
					itemEntity.level().addFreshEntity(new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), makeStack(item, damage)));
				}
			}
		}
		else
		{
			//使用者がいない場合（再起動した際など）にはその場にアイテム化
			if(!itemEntity.level().isClientSide)
			{
				itemEntity.level().addFreshEntity(new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), makeStack(item, damage)));
			}
		}
		if(!itemEntity.level().isClientSide)
		{
			itemEntity.discard();
		}
		return true;
	}

	/** ダメージ値のないアイテム版 */
	public static boolean itemEffectFinish(Entity itemEntity, LivingEntity userEntity, Item item)
	{
		return itemEffectFinish(itemEntity, userEntity, item, 0);
	}

	private static ItemStack makeStack(Item item, int damage)
	{
		ItemStack stack = new ItemStack(item, 1);
		if(damage > 0)
		{
			stack.setDamageValue(damage);
		}
		return stack;
	}
}
