package thKaguyaMod.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import thKaguyaMod.init.THKaguyaArmorMaterials;

/**
 * 火鼠の皮衣（Fire Rat Robe）
 *
 * 原版 extract/sources/java/thKaguyaMod/item/ItemFireRatRobe.java（123 行，extends ItemArmor）：
 * ・setMaxDamage(0) → 无限耐久
 * ・getArmorTexture → thkaguyamod:textures/armor/hinezumi_1.png（只有一张，不分内外层）
 * ・hitEntity → 目标 setFire(20)（1 秒）
 * ・右键 = 格挡姿势（EnumAction.block），使用时长 72000，按下的瞬间给自己灭火
 * ・onUpdate（在背包里就每 tick 跑）→ 没有附魔就补火焰保护 V
 * ・hasEffect 恒 true（一直带附魔光效），附魔台不可附，附魔书不可用
 * ・onArmorTick（穿在身上）→ 每 tick 灭火
 *
 * 1.20.1 对应：
 * ・ItemArmor → ArmorItem(material, Type.CHESTPLATE, props)，耐久来自 ArmorMaterial（返回 0 = 不可损坏）
 * ・getArmorTexture → Forge 的 IForgeItem#getArmorTexture，返回完整贴图路径
 * ・hitEntity → hurtEnemy；onUpdate → inventoryTick；onArmorTick 仍在 IForgeItem 上
 * ・EnumAction.block → UseAnim.BLOCK；setItemInUse → startUsingItem
 * ・extinguish() → clearFire()
 */
public class ItemFireRatRobe extends ArmorItem
{
	public ItemFireRatRobe()
	{
		super(THKaguyaArmorMaterials.HINEZUMI, ArmorItem.Type.CHESTPLATE, new Properties());
	}

	/** 原版固定返回 thkaguyamod:textures/armor/hinezumi_1.png（不区分内外层） */
	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type)
	{
		return "thkaguya:textures/models/armor/hinezumi_layer_1.png";
	}

	/** 原版 hitEntity：拿这件衣服打到谁就给谁点火 20 tick（1 秒） */
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
	{
		target.setSecondsOnFire(1);
		return true;
	}

	/** 原版 getItemUseAction = EnumAction.block：右键摆格挡架势 */
	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.BLOCK;
	}

	/** 原版 getMaxItemUseDuration = 72000 */
	@Override
	public int getUseDuration(ItemStack stack)
	{
		return 72000;
	}

	/** 原版 onItemRightClick：开始使用 + 立刻给自己灭火 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		if(!level.isClientSide)
		{
			player.clearFire();
		}
		return InteractionResultHolder.consume(stack);
	}

	/** 原版 onUpdate：只要物品在背包里、且没有附魔，就补火焰保护 V（砂轮洗掉后下一 tick 会回来） */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if(!level.isClientSide && !stack.isEnchanted())
		{
			stack.enchant(Enchantments.FIRE_PROTECTION, 5);
		}
	}

	/** 原版 onArmorTick：穿在身上每 tick 灭火 */
	@Override
	public void onArmorTick(ItemStack stack, Level level, Player player)
	{
		if(!level.isClientSide)
		{
			player.clearFire();
		}
	}

	/** 原版 hasEffect 恒 true */
	@Override
	public boolean isFoil(ItemStack stack)
	{
		return true;
	}

	/** 原版 isBookEnchantable = false（Forge 默认 true，必须显式关掉） */
	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return false;
	}

	/** 原版 getItemEnchantability = 0（ArmorItem 会转发到这里再转给材质，这里显式固定） */
	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}
}
