package thKaguyaMod.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.THStackData;

/**
 * 弾アイテム（1.20.1 移植版）
 *
 * 原版 ItemTHShot は 1 個の Item + damage 値（0～18）で 19 種の弾を表していたが、
 * 1.20.1 では metadata が廃止されたため、19 種を独立 Item に分割し、
 * このクラスを type（0～18）付きで共有する。
 *
 * - form  : DanmakuConstants.FORM_*（弹的形状 ID。元の shotTypeTrans[] と同値）
 * - speed : 原版 speed[]（弹速）
 * - 右键试射：shootDanmaku() は原版 ItemTHShot.shootDanmaku の移植。
 *   NBT "Number"/"DanmakuForm"/"Speed"/"Color"/"Special"/"Gravity" を読む
 *   （弾幕工作台 ContainerDanmakuCrafting が書き込むキーと同じ）。
 * - クリエイティブ時は原版同様 NBT "Infinity" を立てて弾を消費しない。
 */
public class ItemTHShot extends Item
{
	/* 原版の damage 値（アイテム種別）*/
	public static final int SMALL		=  0;
	public static final int TINY		=  1;
	public static final int MEDIUM		=  2;
	public static final int BIG			=  3;
	public static final int STAR		=  4;
	public static final int SMALLSTAR	=  5;
	public static final int CIRCLE		=  6;
	public static final int SCALE		=  7;
	public static final int BUTTERFLY	=  8;
	public static final int LIGHT		=  9;
	public static final int KNIFE		= 10;
	public static final int HEART		= 11;
	public static final int KUNAI		= 12;
	public static final int TALISMAN	= 13;
	public static final int BIGLIGHT	= 14;
	public static final int RICE		= 15;
	public static final int OVAL		= 16;
	public static final int CRYSTAL		= 17;
	public static final int ARROW		= 18;

	/** 原版 ItemTHShot.form[]：アイテム種別 → 弹形状 ID */
	public static final int[] FORM =
	{
		DanmakuConstants.FORM_SMALL, DanmakuConstants.FORM_TINY, DanmakuConstants.FORM_MEDIUM, DanmakuConstants.FORM_BIG,
		DanmakuConstants.FORM_STAR, DanmakuConstants.FORM_SMALLSTAR, DanmakuConstants.FORM_CIRCLE, DanmakuConstants.FORM_SCALE,
		DanmakuConstants.FORM_BUTTERFLY, DanmakuConstants.FORM_LIGHT, DanmakuConstants.FORM_KNIFE, DanmakuConstants.FORM_HEART,
		DanmakuConstants.FORM_KUNAI, DanmakuConstants.FORM_TALISMAN, DanmakuConstants.FORM_BIGLIGHT, DanmakuConstants.FORM_RICE,
		DanmakuConstants.FORM_OVAL, DanmakuConstants.FORM_CRYSTAL, DanmakuConstants.FORM_ARROW
	};

	/** 原版 ItemTHShot.speed[]：アイテム種別 → 弹速 */
	public static final float[] SPEED =
	{
		0.50F, 0.50F, 0.40F, 0.25F,
		0.50F, 0.50F, 0.50F, 0.55F,
		0.30F, 0.50F, 0.65F, 0.40F,
		0.60F, 0.50F, 0.40F, 0.50F,
		0.40F, 0.45F, 0.50F
	};

	/** 弹幕形状名（原版 danmakuForm[]、lang キー thKaguya.danmakuForm.N 用） */
	public static final String[] DANMAKU_FORM =
	{
		"Point", "Random", "Sector", "Around", "Sphere", "Ring", "undefined"
	};

	/** 16 色染料 → 8 色弹幕色（原版 ItemTHShot.sellectbleColor） */
	public static final int[] SELECTABLE_COLOR =
	{
		7, 0, 2, 0, 1, 4, 5, 7, 7, 4, 2, 3, 5, 4, 6, 7
	};

	/** 原版 colorR/G/B[]（弹图标着色用） */
	public static final int[] COLOR_R = { 255,   0,   0, 255, 255,   0, 255, 255};
	public static final int[] COLOR_G = {   0,   0, 255, 255,   0, 255, 128, 255};
	public static final int[] COLOR_B = {   0, 255,   0,   0, 255, 255,   0, 255};

	private final int shotType;

	public ItemTHShot(int shotType)
	{
		super(new Item.Properties().stacksTo(64));
		this.shotType = shotType;
	}

	/** アイテム種別（0～18） */
	public int getShotType()
	{
		return shotType;
	}

	/** 弹形状 ID（DanmakuConstants.FORM_*） */
	public int getShotForm()
	{
		return FORM[shotType];
	}

	/** 原版の弹速 */
	public float getShotSpeed()
	{
		return SPEED[shotType];
	}

	/** NBT "Color"（0～7）から图标着色 RGB を返す。原版 getColorFromItemStack 相当 */
	public static int getTintColor(ItemStack stack)
	{
		int color = 7;

		CompoundTag nbt = THStackData.get(stack);
		if(nbt != null)
		{
			color = nbt.getByte("Color");

			if(color >= 8)
			{
				color = 7;
			}
		}

		// 1.21.1 の ItemRenderer.renderQuadList は tint の alpha を合成係数に使う
		// （1.20.1 の Forge 版は alpha を見ない）。alpha=0 のままだと layer0 が
		// 完全透明になり「空心圆环」になるため、必ず不透明にする。
		return 0xFF000000 | (COLOR_R[color] << 16) | (COLOR_G[color] << 8) | COLOR_B[color];
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		if(!level.isClientSide)
		{
			shootDanmaku(itemStack, player, player.isShiftKeyDown(),
					THShotLib.pos_Living(player), player.getLookAngle(),
					DanmakuConstants.SIZE[getShotForm()]);

			THShotLib.playShotSound(player);

			boolean isInfinity = false;
			if(THStackData.get(itemStack) != null)
			{
				isInfinity = THStackData.get(itemStack).getBoolean("Infinity");
			}

			if(!isInfinity)
			{
				itemStack.shrink(1);
			}
		}

		return InteractionResultHolder.success(itemStack);
	}

	/**
	 * 原版 ItemTHShot.shootDanmaku の移植。
	 * 弾幕工作台が NBT に書いた Number/DanmakuForm/Speed/Color/Special/Gravity を読んで発射する。
	 */
	public void shootDanmaku(ItemStack itemStack, LivingEntity living, boolean isSlowMode,
			Vec3 pos, Vec3 angle, double distance)
	{
		net.minecraft.util.RandomSource rand = living.level().random;
		int type2 = getShotForm();
		int color = rand.nextInt(7);
		int special = 0;

		int way = 1;
		double shotSpeed = SPEED[shotType];
		int danmakuForm = 0;
		double gravity = 0.0D;

		CompoundTag nbt = THStackData.get(itemStack);
		if(nbt != null)
		{
			way = nbt.getShort("Number");
			danmakuForm = nbt.getByte("DanmakuForm");
			shotSpeed = SPEED[shotType] * (1.0D + (double)nbt.getByte("Speed") * 0.03D);
			special = nbt.getInt("Special");
			gravity = (double)nbt.getByte("Gravity") * -0.003D;
			color = nbt.getByte("Color");
		}

		// 原版：クリエイティブなら Infinity を立てて消費しない
		if(living instanceof Player player && player.isCreative())
		{
			CompoundTag tag = THStackData.getOrCreate(itemStack);
			if(!tag.contains("Number"))
			{
				tag.putShort("Number", (short)1);
			}
			tag.putBoolean("Infinity", true);
			THStackData.set(itemStack, tag);
		}

		// config の最大弾数でクランプ
		if(way > THKaguyaConfig.shotMaxNumber)
		{
			way = THKaguyaConfig.shotMaxNumber;
		}

		float wide;
		float damagePlus = 0.0F;

		if(living instanceof Player player)
		{
			damagePlus += THKaguyaLib.getPlayerPower(player) / 400.0F;
		}

		ShotData shot = ShotData.shot(type2, color,
				DanmakuConstants.SIZE[type2], DanmakuConstants.DAMAGE[type2] + damagePlus, 0, 80, special);

		switch(danmakuForm)
		{
			case 0:// 一点
			{
				double shotSpeed2 = shotSpeed;
				Vec3 shootPos = new Vec3(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);

				for(int i = 1; i <= way; i++)
				{
					THShotLib.createShot(living, living, shootPos, angle, 0F,
							shotSpeed2, shotSpeed2, 0.0D, THShotLib.gravity(gravity), shot);
					shotSpeed2 = shotSpeed / (double)way * (double)i;
				}
				break;
			}
			case 1:// 前方ランダム
				wide = 120F;
				if(isSlowMode)
				{
					wide = 60F;
				}
				THShotLib.createRandomRingShot(living, living, pos, angle, 0F, 9999,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity(0.0D, gravity, 0.0D),
						shot, way, distance, wide);
				break;
			case 2:// 前方 n-way
				wide = way * 3F;
				if(isSlowMode)
				{
					wide = wide * 0.5F;
				}
				THShotLib.createWideShot(living, living, pos, angle,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity(0.0D, gravity, 0.0D),
						shot, way, wide, distance);
				break;
			case 3:// 全方位
				THShotLib.createCircleShot(living, living, pos, angle,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity(0.0D, gravity, 0.0D),
						shot, way, distance, 0F);
				break;
			case 4:// 球状
				THShotLib.createSphereShot(living, living, pos, angle, 0F,
						THShotLib.rotate_Default(), 0F, 99999,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(),
						shot, way, distance, 0F);
				break;
			case 5:// リング
				wide = 15F;
				if(isSlowMode)
				{
					wide *= 0.5F;
				}
				THShotLib.createRingShot(living, living, pos, angle, 0F, 9999,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity(0.0D, gravity, 0.0D),
						shot, way, distance, wide, rand.nextFloat() * 360F);
				break;
			default:
				break;
		}
	}

	/** 原版 hasEffect：NBT "Infinity" で発光 */
	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		CompoundTag nbt = THStackData.get(itemStack);
		return nbt != null && nbt.getBoolean("Infinity");
	}

	@Override
	public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(itemStack, context, tooltip, flag);

		short shotNum = 1;
		byte form = 0;
		float shotSpeed = SPEED[shotType];
		int color = 8;
		double gravity = 0.0D;
		byte bound = 0;
		boolean isInfinity = false;

		CompoundTag nbt = THStackData.get(itemStack);
		if(nbt != null)
		{
			shotNum = nbt.getShort("Number");
			form = nbt.getByte("DanmakuForm");
			shotSpeed = (float)(SPEED[shotType] * (1.0F + (float)nbt.getByte("Speed") * 0.03F));
			color = nbt.getByte("Color");
			gravity = (double)nbt.getByte("Gravity") * 0.003D;
			bound = (byte)(nbt.getInt("Special") - THShotLib.BOUND01 + 1);
			isInfinity = nbt.getBoolean("Infinity");
		}

		if(bound < 0)
		{
			bound = 0;
		}

		if(form >= DANMAKU_FORM.length)
		{
			form = (byte)(DANMAKU_FORM.length - 1);
		}

		tooltip.add(line("danmakuCrafting.damage", fmt(DanmakuConstants.DAMAGE[getShotForm()] / 2F)));
		tooltip.add(line("danmakuCrafting.number", String.valueOf(shotNum)));
		tooltip.add(Component.translatable("danmakuCrafting.form").append(" : ")
				.append(Component.translatable("thKaguya.danmakuForm." + form)));
		tooltip.add(line("danmakuCrafting.speed", fmt(shotSpeed)));

		if(gravity != 0.0D)
		{
			tooltip.add(line("danmakuCrafting.gravity", fmt(gravity)));
		}
		else
		{
			tooltip.add(lineTranslated("danmakuCrafting.gravity", "thKaguya.gravityFree"));
		}

		tooltip.add(Component.translatable("danmakuCrafting.color").append(" : ")
				.append(Component.translatable("thKaguya.color." + color)));

		if(bound >= 1 && bound <= 3)
		{
			tooltip.add(line("danmakuCrafting.bound", String.valueOf(bound)));
		}
		else if(bound == 4)
		{
			tooltip.add(lineTranslated("danmakuCrafting.bound", "Infinity"));
		}
		else
		{
			tooltip.add(line("danmakuCrafting.bound", "0"));
		}

		if(isInfinity)
		{
			tooltip.add(Component.translatable("Infinity").withStyle(ChatFormatting.AQUA));
		}
	}

	private static Component line(String key, String value)
	{
		return Component.translatable(key).append(" : ").append(value);
	}

	private static Component lineTranslated(String key, String valueKey)
	{
		return Component.translatable(key).append(" : ").append(Component.translatable(valueKey));
	}

	private static String fmt(float value)
	{
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	private static String fmt(double value)
	{
		return String.format(java.util.Locale.ROOT, "%.4f", value);
	}

	/** 旧存档：载入时把 1.20.1 的 tag 从 {@code minecraft:custom_data} 搬进 {@code thkaguya:stack_tag}。 */
	@Override
	public void verifyComponentsAfterLoad(ItemStack stack)
	{
		THStackData.migrate(stack);
	}
}
