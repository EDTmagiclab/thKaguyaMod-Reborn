package thKaguyaMod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.spellcard.EntitySpellCard;

/**
 * 五つの難題MOD+用、汎用関数群（1.20.1 移植版，阶段 3d-2 裁剪版）
 *
 * 保留：spellCardDeclaration（符卡宣言核心，boss 用）、radTrans/degTrans。
 * 裁剪：itemEffectFollowUser/itemEffectFinish（依赖 EntityItem）、createSilverKnife（依赖 EntitySilverKnife）、
 * getPlayerPower（依赖物品背包 + metadata）、danmakuRemove（弹消，依赖未迁移）。
 */
public class THKaguyaLib
{
	/**
	 * スペルカードの宣言で宣言が可能だった場合の処理
	 */
	public static void spellCardDeclaration(Level world, LivingEntity user, LivingEntity target, int spellCardNumber, int needLevel, int level, boolean firstAttack)
	{
		EntitySpellCard entitySpellCard = new EntitySpellCard(world, user, target, spellCardNumber, level);

		if(!world.isClientSide)
		{
			world.addFreshEntity(entitySpellCard);
		}

		// プレイヤー関連（消息/音效/消耗物品）与 danmakuRemove 弹消：留待阶段 5/7 补全
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
