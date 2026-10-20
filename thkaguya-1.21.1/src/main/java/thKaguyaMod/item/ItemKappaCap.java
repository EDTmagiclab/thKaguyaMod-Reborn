package thKaguyaMod.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.init.THKaguyaArmorMaterials;

/**
 * 河童の帽子（Kappa's Cap）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemKappaCap.java（121 行，extends ItemArmor）：
 * ・armorMaterial_Hinezumi（防御 1/2/2/2 → 頭は 1）、type 0 = 頭、setMaxDamage(500)
 * ・getArmorTexture → textures/armor/hinezumi_1.png（移植版に同名アセットあり）
 * ・onArmorTick：
 *   - 水中：motionX/Z ×1.2、（上昇中なら）motionY ×1.2、その後 ±0.5 にクランプ（Y は上限のみ）
 *           さらに耐久が減っていれば 3 回復
 *   - 陸上で濡れている：耐久 1 回復
 *   - 陸上で乾いている：耐久 498 未満なら 1 減少（放置すると壊れる）
 *
 * 1.20.1 対応：
 * ・motionX/Y/Z → getDeltaMovement()/setDeltaMovement(Vec3)
 * ・damageItem(-3) → setDamageValue(max(0, damage-3))、damageItem(1) → setDamageValue(damage+1)
 * ・耐久 500 は Item#getMaxDamage の上書きで与える（素材 HINEZUMI は「火鼠の皮衣」用に耐久 0 = 無敵）
 */
public class ItemKappaCap extends ArmorItem
{
	/** 原版 setMaxDamage(500) */
	public static final int MAX_DURABILITY = 500;
	/** 原版 double maxSpeed = 0.5D */
	public static final double MAX_SPEED = 0.5D;
	/** 原版 水中スピード倍率 1.2 */
	public static final double WATER_SPEED_FACTOR = 1.2D;
	/** 原版 陸上で乾いているときに減る上限（getItemDamage() < 498） */
	public static final int DRY_DAMAGE_LIMIT = 498;

	public ItemKappaCap()
	{
		super(THKaguyaArmorMaterials.HINEZUMI, ArmorItem.Type.HELMET, new net.minecraft.world.item.Item.Properties().durability(MAX_DURABILITY));
	}

	@Override
	public int getMaxDamage(ItemStack stack)
	{
		return MAX_DURABILITY;
	}

	/** 原版 getItemEnchantability = 0 */
	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	/** 原版 isBookEnchantable = false */
	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return false;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if (!(entity instanceof Player player) || !isWorn(player, stack))
		{
			return;
		}
		if (player.isInWater())
		{
			Vec3 motion = player.getDeltaMovement();
			double motionX = motion.x * WATER_SPEED_FACTOR;
			double motionY = motion.y > 0.0D ? motion.y * WATER_SPEED_FACTOR : motion.y;
			double motionZ = motion.z * WATER_SPEED_FACTOR;

			// 原版：X/Z は ±maxSpeed でクランプ、Y は上限のみクランプ（負方向はそのまま）
			motionX = clampSymmetric(motionX);
			motionZ = clampSymmetric(motionZ);
			if (motionY > MAX_SPEED)
			{
				motionY = MAX_SPEED;
			}
			player.setDeltaMovement(motionX, motionY, motionZ);

			if (!level.isClientSide && stack.isDamaged())
			{
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - 3));
			}
		}
		else if (player.isInWaterRainOrBubble())
		{
			if (!level.isClientSide)
			{
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
			}
		}
		else if (!level.isClientSide && stack.getDamageValue() < DRY_DAMAGE_LIMIT)
		{
			stack.setDamageValue(stack.getDamageValue() + 1);
		}
	}

	private static double clampSymmetric(double value)
	{
		if (value > MAX_SPEED)
		{
			return MAX_SPEED;
		}
		if (value < -MAX_SPEED)
		{
			return -MAX_SPEED;
		}
		return value;
	}
	private static boolean isWorn(Player player, ItemStack stack)
	{
		for (ItemStack armor : player.getInventory().armor)
		{
			if (armor == stack)
			{
				return true;
			}
		}
		return false;
	}
}
