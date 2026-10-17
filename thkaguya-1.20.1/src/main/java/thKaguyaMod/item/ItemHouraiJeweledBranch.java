package thKaguyaMod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 蓬莱の玉の枝（hourai_jeweled_branch，蓬莱玉枝）—— 原版 {@code thKaguyaMod.item.ItemHouraiJeweledBranch}（280 行，extends ItemBow）
 *
 * 原版行为（注意：耐久值被当成「充能」计数器使用，从不真正掉耐久）：
 * - 右键按住进入 {@code EnumAction.block}（格挡姿势），松手射出「真珠彩虹弹」
 * - 弹数：{@code shotNum = 32 + 72000 - usedTime}（held 越久越多），上限 72，再 {@code /3}
 * - 充能 {@code getCharge(NBT "Charge") <= 200} 才能发射；每次发射 {@code addCharge(-50)}
 * - 潜行：8 层不同初速（0.3 + i*0.2）的全方位弹（way = shotNum，baseAngle = i*3°，distance 0.2）
 * - 非潜行：3 层环弹（way = shotNum / shotNum/2 / shotNum/3，ringSpan 15/10/5，distance 0）
 * - 库存里时每 50 tick {@code addCharge(+50)}（即充能回落），< 0 则 {@code setCharge(0)}
 * - 6 档图标按充能切换（icons[0..5] = houraiEda, _4, _3, _2, _1, _0）
 *
 * 1.20.1 迁移：
 * - 不继承 {@code BowItem}（它强制要箭），改为 {@code Item} + {@code startUsingItem}/{@code releaseUsing}
 * - 必须显式覆写 {@code getUseDuration} = 72000（默认 0 会让 releaseUsing 立刻触发）
 * - 6 档图标改用 {@code CustomModelData}（在 {@code inventoryTick} 写入），模型用 overrides 分派
 */
public class ItemHouraiJeweledBranch extends Item
{
	/** 原版 NBT 键（short） */
	public static final String TAG_CHARGE = "Charge";
	/** 原版 setMaxDamage(301)：只是用来显示「充能」，实际从不扣耐久 */
	public static final int MAX_DAMAGE = 301;
	/** 充能上限：超过就不许发射（原版 getCharge(itemStack) <= 200） */
	public static final int CHARGE_LIMIT = 200;
	/** 每次发射增加的充能（原版 addCharge(itemStack, -50)） */
	public static final int CHARGE_PER_SHOT = 50;
	/** 恢复节奏：每 50 tick 回落 50 */
	public static final int CHARGE_REGEN_INTERVAL = 50;

	public ItemHouraiJeweledBranch()
	{
		super(new Item.Properties().stacksTo(1).durability(MAX_DAMAGE));
	}

	// ---------------------------------------------------------------- 充能

	public static int getCharge(ItemStack itemStack)
	{
		CompoundTag tag = itemStack.getTag();
		if(tag == null)
		{
			return 0;
		}
		return tag.getShort(TAG_CHARGE);
	}

	/** 原版 setCharge：只在当前值 >= 0 时写入 */
	public static void setCharge(ItemStack itemStack, int charge)
	{
		CompoundTag tag = itemStack.getTag();
		if(tag == null)
		{
			tag = new CompoundTag();
			itemStack.setTag(tag);
			tag.putShort(TAG_CHARGE, (short)charge);
		}
		else if(tag.getShort(TAG_CHARGE) >= 0)
		{
			tag.putShort(TAG_CHARGE, (short)charge);
		}
	}

	/** 原版 addCharge：charge 为负时是「增加」，初次创建时写 -charge */
	public static void addCharge(ItemStack itemStack, int charge)
	{
		CompoundTag tag = itemStack.getTag();
		if(tag == null)
		{
			tag = new CompoundTag();
			itemStack.setTag(tag);
			tag.putShort(TAG_CHARGE, (short)(-charge));
		}
		else if(tag.getShort(TAG_CHARGE) >= 0)
		{
			tag.putShort(TAG_CHARGE, (short)(tag.getShort(TAG_CHARGE) - charge));
		}
	}

	/** 原版图标档位：0 = 空(≦0)、1..5 = 50/100/150/200/200+ */
	public static int getIconTier(ItemStack itemStack)
	{
		int charge = getCharge(itemStack);
		if(charge <= 0)
		{
			return 0;
		}
		if(charge <= 50)
		{
			return 1;
		}
		if(charge <= 100)
		{
			return 2;
		}
		if(charge <= 150)
		{
			return 3;
		}
		if(charge <= 200)
		{
			return 4;
		}
		return 5;
	}

	// ---------------------------------------------------------------- 使用

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public int getUseDuration(ItemStack itemStack)
	{
		// 原版沿用 ItemBow 的 72000
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemStack)
	{
		// 原版 EnumAction.block
		return UseAnim.BLOCK;
	}

	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity living, int timeLeft)
	{
		if(!(living instanceof Player player))
		{
			return;
		}

		int usedTime = getUseDuration(itemStack) - timeLeft;

		// 原版：32 + 持续时间，上限 72，再 /3
		int shotNum = 32 + usedTime;
		if(shotNum > 72)
		{
			shotNum = 72;
		}
		shotNum /= 3;

		if(getCharge(itemStack) > CHARGE_LIMIT)
		{
			return;
		}

		if(level.isClientSide)
		{
			return;
		}

		ShotData shot = ShotData.shot(DanmakuConstants.FORM_PEARL, DanmakuConstants.RAINBOW, 0.3F, 5.0F, 1, 80);

		if(player.isShiftKeyDown())
		{
			// 潜行：弹速不同的全方位弹（way = shotNum，每层 +3°）
			for(int i = 0; i < shotNum; i += 3)
			{
				THShotLib.createCircleShot(player, player, THShotLib.pos_Living(player),
						THShotLib.angle(player.getYRot(), player.getXRot()),
						0.3D + (double)i * 0.2D, 2.0D, 0.03D, THShotLib.gravity_Zero(),
						shot, shotNum, 0.2D, i * 3F);
			}
		}
		else
		{
			// 非潜行：三层环弹，发射点前移半个方块
			Vec3 angle = THShotLib.angle(player.getYRot(), player.getXRot());
			Vec3 pos = THShotLib.pos(player.getX() + angle.x * 0.5D,
					THShotLib.getPosYFromEye(player) + angle.y * 0.5D,
					player.getZ() + angle.z * 0.5D);

			THShotLib.createRingShot(player, player, pos, angle, 0F, 9999,
					0.6D, 2.0D, 0.03D, THShotLib.gravity_Zero(),
					shot, shotNum, 0.0D, 15F, level.random.nextFloat() * 360F);

			shot = ShotData.shot(DanmakuConstants.FORM_PEARL, DanmakuConstants.RAINBOW, 0.3F, 5.0F, 1, 80);
			THShotLib.createRingShot(player, player, pos, angle, 0F, 9999,
					0.6D, 2.0D, 0.03D, THShotLib.gravity_Zero(),
					shot, shotNum / 2, 0.0D, 10F, level.random.nextFloat() * 360F);

			shot = ShotData.shot(DanmakuConstants.FORM_PEARL, DanmakuConstants.RAINBOW, 0.3F, 5.0F, 1, 80);
			THShotLib.createRingShot(player, player, pos, angle, 0F, 9999,
					0.6D, 2.0D, 0.03D, THShotLib.gravity_Zero(),
					shot, shotNum / 3, 0.0D, 5F, level.random.nextFloat() * 360F);
		}

		// 原版 "random.orb"
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.random.nextFloat() * 4F + 0.8F));

		addCharge(itemStack, -CHARGE_PER_SHOT);
	}

	@Override
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slot, boolean selected)
	{
		// 原版 onUpdate：每 50 tick 让充能回落 50
		if(getCharge(itemStack) > 0 && entity.tickCount % CHARGE_REGEN_INTERVAL == 0)
		{
			addCharge(itemStack, CHARGE_PER_SHOT);
			if(getCharge(itemStack) < 0)
			{
				setCharge(itemStack, 0);
			}
		}

		// 客户端渲染 6 档图标
		int tier = getIconTier(itemStack);
		CompoundTag tag = itemStack.getTag();
		int current = tag == null ? -1 : tag.getInt("CustomModelData");
		if(current != tier)
		{
			itemStack.getOrCreateTag().putInt("CustomModelData", tier);
		}
	}

	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		// 原版 hasEffect 恒 true
		return true;
	}

	@Override
	public int getEnchantmentValue()
	{
		return 0;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return false;
	}

	@Override
	public boolean isRepairable(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
	{
		if(level != null && level.isClientSide)
		{
			tooltip.add(Component.literal("Charge : " + getCharge(itemStack) + " / " + CHARGE_LIMIT)
					.withStyle(ChatFormatting.AQUA));
		}
	}
}
