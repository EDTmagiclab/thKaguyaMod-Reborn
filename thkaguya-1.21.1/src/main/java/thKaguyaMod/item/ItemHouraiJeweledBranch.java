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
import thKaguyaMod.THStackData;

/**
 * 蓬莱の玉の枝（hourai_jeweled_branch，蓬莱玉枝）—— 原版 {@code thKaguyaMod.item.ItemHouraiJeweledBranch}（280 行，extends ItemBow）
 *
 * 原版行为（注意：耐久值被当成「充能」计数器使用，从不真正掉耐久）：
 * - 右键按住进入 {@code EnumAction.block}（格挡姿势），松手射出「真珠彩虹弹」
 * - 弹数：{@code shotNum = 32 + 72000 - usedTime}（held 越久越多），上限 72，再 {@code /3}
 * - 充能 {@code getCharge(NBT "Charge") <= 200} 才能发射；每次发射 {@code addCharge(-50)}
 * - 潜行：8 层水平 360° 全方位弹（弹速 0.3 + i*0.2，每层绕竖直轴转 3°，见 releaseUsing 里的说明）
 * - 非潜行：前方扇状弹（原版此处是 3 层环弹 ringSpan 15/10/5，实机上是一坨；本版取作者同段注释代码
 *   {@code createWideShot(..., shotNum, 30F, 0.2D)}，见 releaseUsing 里的说明）
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
		CompoundTag tag = THStackData.get(itemStack);
		if(tag == null)
		{
			return 0;
		}
		return tag.getShort(TAG_CHARGE);
	}

	/** 原版 setCharge：只在当前值 >= 0 时写入 */
	public static void setCharge(ItemStack itemStack, int charge)
	{
		CompoundTag tag = THStackData.get(itemStack);
		if(tag == null)
		{
			tag = new CompoundTag();
			THStackData.set(itemStack, tag);
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
		CompoundTag tag = THStackData.get(itemStack);
		if(tag == null)
		{
			tag = new CompoundTag();
			THStackData.set(itemStack, tag);
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
	public int getUseDuration(ItemStack itemStack, LivingEntity entity)
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

		int usedTime = getUseDuration(itemStack, player) - timeLeft;

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
			// 潜行：以玩家为圆心的水平 360° 全方位弹（前后左右都有弹）。
			// 原版的 createCircleShot / createRingShot 都是拿「视线」当旋转轴，在**垂直于视线**的
			// 平面里铺一圈——平视时那是一面竖着的圆墙，弹全都向前飞，身后没有弹；
			// 而 ringSpan = 0 更会退化成一条直线（baseVec 与轴重合，绕轴旋转无效）。
			// 这里改成绕竖直轴（Y）旋转，弹幕铺在水平面上，才是真正的全方位。
			Vec3 dir = THShotLib.angle(player.getYRot(), 0F);
			Vec3 axis = new Vec3(0.0D, 1.0D, 0.0D);
			Vec3 pos = THShotLib.pos_Living(player);
			float span = 360F / (float)shotNum;
			for(int i = 0; i < shotNum; i += 3)
			{
				float base = (float)i * 3F;
				double speed = 0.3D + (double)i * 0.2D;
				for(int k = 0; k < shotNum; k++)
				{
					Vec3 vec = THShotLib.getVectorFromRotation(axis, dir, base + (float)k * span);
					THShotLib.createShot(player, player,
							THShotLib.pos(pos.x + vec.x * 0.2D, pos.y + vec.y * 0.2D, pos.z + vec.z * 0.2D),
							vec, 0F, axis, 0F, 9999,
							speed, 2.0D, 0.03D, THShotLib.gravity_Zero(), shot);
				}
			}
		}
		else
		{
			// 非潜行：前方扇状弹幕。
			// 原版此处是三次 createRingShot（ringSpan 15/10/5）——那三个环都以视线为轴、
			// 只偏 7.5°/5°/2.5°，近距离看是「一坨」而不是扇形；作者在同段里保留的注释代码
			// createWideShot(..., shotNum, 30F, 0.2D) 才是扇形。按实机手感取扇形版本。
			Vec3 angle = THShotLib.angle(player.getYRot(), player.getXRot());
			Vec3 pos = THShotLib.pos(player.getX() + angle.x * 0.5D,
					THShotLib.getPosYFromEye(player) + angle.y * 0.5D,
					player.getZ() + angle.z * 0.5D);

			THShotLib.createWideShot(player, player, pos, angle,
					0.3D, 2.0D, 0.03D, THShotLib.gravity_Zero(),
					shot, shotNum, 30F, 0.2D);
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
		CompoundTag tag = THStackData.get(itemStack);
		int current = tag == null ? -1 : tag.getInt("CustomModelData");
		if(current != tier)
		{
			itemStack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(tier));
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
	public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		if(context.level() != null && context.level().isClientSide)
		{
			tooltip.add(Component.literal("Charge : " + getCharge(itemStack) + " / " + CHARGE_LIMIT)
					.withStyle(ChatFormatting.AQUA));
		}
	}

	/** 旧存档：载入时把 1.20.1 的 tag 从 {@code minecraft:custom_data} 搬进 {@code thkaguya:stack_tag}。 */
	@Override
	public void verifyComponentsAfterLoad(ItemStack stack)
	{
		THStackData.migrate(stack);
	}
}
